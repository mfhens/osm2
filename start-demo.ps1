# start-demo.ps1 — Start the OSM2 demo (taxable-person portal + authority portal + all backend services)
# Requires: Java 21, Maven, Docker (postgres/keycloak/observability via compose)
#
# Usage:  .\start-demo.ps1                         # start everything (demo profile, no auth)
#         .\start-demo.ps1 -SecurityDemo           # start with Keycloak OIDC auth enabled
#         .\start-demo.ps1 -Stop                   # stop all Java services (leaves Docker infra running)
#         .\start-demo.ps1 -Only taxable           # start only taxable-person portal + its backends
#         .\start-demo.ps1 -Only authority         # start only authority portal + its backends
#         .\start-demo.ps1 -SecurityDemo -Only authority

param(
    [switch]$Stop,
    [ValidateSet("all", "taxable", "authority")]
    [string]$Only = "all",
    [switch]$SecurityDemo
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$PidDir = Join-Path $ScriptDir ".demo-pids"
$LogDir = Join-Path $ScriptDir ".demo-logs"

$PgPort = 5432
$PgUser = "osm2_admin"
$PgPass = "osm2_admin"

$DockerInfraServices = @("postgres", "keycloak", "immudb", "otel-collector", "tempo", "loki", "prometheus", "grafana", "promtail")
$KeycloakIssuerUri = "http://localhost:8080/realms/osm2"

# Demo client secrets (from application-demo.yml)
$TaxablePortalClientSecret  = "demo-secret-taxable"
$AuthorityPortalClientSecret = "demo-secret-authority"

function Write-Status($msg) { Write-Host $msg -ForegroundColor Yellow }
function Write-Ok($msg)     { Write-Host $msg -ForegroundColor Green  }
function Write-Err($msg)    { Write-Host $msg -ForegroundColor Red    }

function Wait-ForUrl {
    param([string]$Url, [int]$TimeoutSec = 90)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 -ErrorAction SilentlyContinue
            if ($r.StatusCode -lt 400) { return $true }
        } catch {}
        Start-Sleep -Seconds 2
    }
    return $false
}

function Wait-ForTcpPort {
    param([int]$Port, [int]$TimeoutSec = 30)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $async = $tcp.BeginConnect("localhost", $Port, $null, $null)
            if ($async.AsyncWaitHandle.WaitOne(2000, $false)) {
                $tcp.EndConnect($async)
                $tcp.Close()
                return $true
            }
            $tcp.Close()
        } catch {}
        Start-Sleep -Seconds 2
    }
    return $false
}

function Get-RunningInfraServices {
    $appCompose = Join-Path $ScriptDir "docker-compose.yml"
    $obsCompose = Join-Path $ScriptDir "docker-compose.observability.yml"
    $running = & docker compose -f $appCompose -f $obsCompose ps --status running --services 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $running) { return @() }
    return @($running)
}

function Ensure-DockerInfra {
    Write-Status "Ensuring Docker infra is running (postgres, keycloak, immudb, observability)..."

    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Err "  Docker CLI not found. Install/start Docker Desktop first."
        exit 1
    }

    $runningServices = Get-RunningInfraServices
    $missingServices = @($DockerInfraServices | Where-Object { $_ -notin $runningServices })

    if ($missingServices.Count -eq 0) {
        Write-Ok "  Docker infra already running."
        return
    }

    Write-Host "  Starting missing infra services: $($missingServices -join ', ')"

    $appCompose = Join-Path $ScriptDir "docker-compose.yml"
    $obsCompose = Join-Path $ScriptDir "docker-compose.observability.yml"

    $composeArgs = @("-f", $appCompose, "-f", $obsCompose, "up", "-d") + $DockerInfraServices
    & docker compose @composeArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Err "  Failed to start Docker infra services."
        exit 1
    }

    Write-Host "  Waiting for PostgreSQL on port $PgPort..."
    if (-not (Wait-ForTcpPort $PgPort 30)) {
        Write-Err "  PostgreSQL did not open port $PgPort in time."
        exit 1
    }

    Write-Host "  Waiting for Keycloak on port 8080..."
    if (-not (Wait-ForUrl "http://localhost:8080/health/ready" 120)) {
        Write-Err "  Keycloak did not become ready in time."
        exit 1
    }

    Write-Host "  Waiting for immudb metrics on port 9497..."
    if (-not (Wait-ForUrl "http://localhost:9497/metrics" 30)) {
        Write-Err "  immudb did not become ready in time."
        exit 1
    }

    Write-Ok "  Docker infra ready."
}

# ---------------------------------------------------------------------------
# Stop all Java services
# ---------------------------------------------------------------------------
function Stop-JavaServices {
    Write-Status "Stopping Java services..."
    if (Test-Path $PidDir) {
        Get-ChildItem "$PidDir\*.pid" -ErrorAction SilentlyContinue | ForEach-Object {
            $procId = [int](Get-Content $_.FullName)
            $name   = $_.BaseName
            try {
                $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
                if ($proc) {
                    Write-Host "  Stopping $name (PID $procId)"
                    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                }
            } catch {}
            Remove-Item $_.FullName -Force
        }
    }
    Write-Ok "Java services stopped."
}

if ($Stop) {
    Stop-JavaServices
    exit 0
}

# =========================================================================
# Determine which services to start
# =========================================================================
$startTaxable   = ($Only -eq "all" -or $Only -eq "taxable")
$startAuthority = ($Only -eq "all" -or $Only -eq "authority")

$backendProfile = if ($SecurityDemo) { "docker" } else { "demo" }
$portalProfile  = if ($SecurityDemo) { "demo" }   else { "demo" }

if ($SecurityDemo) {
    Write-Status "Security demo mode: Keycloak OIDC login required."
}

# =========================================================================
# Determine Maven modules to build
# =========================================================================
$modules = [System.Collections.ArrayList]::new()

# Shared libraries
[void]$modules.Add("osm2-common")

# Core backends (always needed)
[void]$modules.Add("osm2-scheme-service")
[void]$modules.Add("osm2-registration-service")

if ($startTaxable) {
    [void]$modules.Add("osm2-return-service")
    [void]$modules.Add("osm2-taxable-person-portal")
}

if ($startAuthority) {
    [void]$modules.Add("osm2-return-service")
    [void]$modules.Add("osm2-payment-service")
    [void]$modules.Add("osm2-records-service")
    [void]$modules.Add("osm2-authority-portal")
}

# Deduplicate (return-service may appear twice)
$modules = @($modules | Select-Object -Unique)

$totalSteps = 5
$step = 0

# =========================================================================
# Main
# =========================================================================
try { Stop-JavaServices } catch {}
New-Item -ItemType Directory -Path $PidDir -Force | Out-Null
New-Item -ItemType Directory -Path $LogDir -Force  | Out-Null

# --- Step 1: Docker infra ---
$step++
Write-Status "[$step/$totalSteps] Checking/starting Docker infra..."
Ensure-DockerInfra

# --- Activate mise (if available) ---
$miseCmd = Get-Command mise -ErrorAction SilentlyContinue
if ($miseCmd) {
    Write-Host "  Activating mise environment..."
    $miseEnv = & $miseCmd.Source env --shell pwsh 2>$null
    if ($miseEnv) { $miseEnv | Invoke-Expression }
}

# --- Step 2: Build JARs ---
$step++
$moduleList = $modules -join ","
Write-Status "[$step/$totalSteps] Building JARs ($moduleList)..."
mvn package -pl $moduleList -am -B -DskipTests -q
if ($LASTEXITCODE -ne 0) { Write-Err "Build failed!"; exit 1 }
Write-Ok "  Build complete."

# --- Step 3: Start backend services ---
$step++
Write-Status "[$step/$totalSteps] Starting backend services..."

$DbUrl = "jdbc:postgresql://localhost:${PgPort}"

function Start-Service {
    param(
        [string]$Name,
        [string]$JarPattern,
        [string]$Profile,
        [string]$DbName,
        [hashtable]$ExtraArgs = @{}
    )
    $jar = Get-ChildItem $JarPattern -Exclude "*.original" -ErrorAction SilentlyContinue |
           Select-Object -First 1
    if (-not $jar) {
        Write-Err "  JAR not found: $JarPattern"
        return $null
    }
    $jvmArgs = @("-jar", $jar.FullName, "--spring.profiles.active=$Profile")
    if ($DbName) {
        $jvmArgs += "--spring.datasource.url=${DbUrl}/${DbName}"
        $jvmArgs += "--spring.datasource.username=$PgUser"
        $jvmArgs += "--spring.datasource.password=$PgPass"
    }
    # Wire OTel traces → collector on localhost:4318 (OTLP/HTTP)
    $jvmArgs = @("-DLOKI_PUSH_URL=http://localhost:3100") + $jvmArgs
    foreach ($key in $ExtraArgs.Keys) {
        $jvmArgs += "--${key}=$($ExtraArgs[$key])"
    }
    $proc = Start-Process -FilePath java -ArgumentList $jvmArgs `
        -RedirectStandardOutput "$LogDir\$Name.log" `
        -RedirectStandardError  "$LogDir\$Name-err.log" `
        -PassThru -WindowStyle Hidden
    Set-Content -Path "$PidDir\$Name.pid" -Value $proc.Id
    Write-Host ("  {0,-28} PID {1}" -f $Name, $proc.Id)
    return $proc
}

# --- scheme-service (always) ---
Start-Service -Name "scheme-service" `
    -JarPattern "osm2-scheme-service\target\osm2-scheme-service-*.jar" `
    -Profile $backendProfile -DbName "osm2_scheme" `
    -ExtraArgs @{
        "KEYCLOAK_ISSUER_URI" = $KeycloakIssuerUri
    }

# --- registration-service (always) ---
Start-Service -Name "registration-service" `
    -JarPattern "osm2-registration-service\target\osm2-registration-service-*.jar" `
    -Profile $backendProfile -DbName "osm2_registration" `
    -ExtraArgs @{
        "KEYCLOAK_ISSUER_URI" = $KeycloakIssuerUri
    }

if ($startTaxable -or $startAuthority) {
    Start-Service -Name "return-service" `
        -JarPattern "osm2-return-service\target\osm2-return-service-*.jar" `
        -Profile $backendProfile -DbName "osm2_return" `
        -ExtraArgs @{
            "KEYCLOAK_ISSUER_URI" = $KeycloakIssuerUri
        }
}

if ($startAuthority) {
    Start-Service -Name "payment-service" `
        -JarPattern "osm2-payment-service\target\osm2-payment-service-*.jar" `
        -Profile $backendProfile -DbName "osm2_payment" `
        -ExtraArgs @{
            "KEYCLOAK_ISSUER_URI"   = $KeycloakIssuerUri
            "IMMUDB_HOST"          = "localhost"
            "IMMUDB_GRPC_PORT"     = "3322"
        }

    Start-Service -Name "records-service" `
        -JarPattern "osm2-records-service\target\osm2-records-service-*.jar" `
        -Profile $backendProfile -DbName "osm2_records" `
        -ExtraArgs @{
            "KEYCLOAK_ISSUER_URI" = $KeycloakIssuerUri
        }
}

# --- Step 4: Wait for backend health ---
$step++
Write-Status "[$step/$totalSteps] Waiting for backend services to become healthy..."

$ok = Wait-ForUrl "http://localhost:8081/actuator/health"
if (-not $ok) { Write-Err "  scheme-service failed! See .demo-logs\scheme-service-err.log"; exit 1 }
Write-Ok "  scheme-service ready."

$ok = Wait-ForUrl "http://localhost:8082/actuator/health"
if (-not $ok) { Write-Err "  registration-service failed! See .demo-logs\registration-service-err.log"; exit 1 }
Write-Ok "  registration-service ready."

if ($startTaxable -or $startAuthority) {
    $ok = Wait-ForUrl "http://localhost:8083/actuator/health"
    if (-not $ok) { Write-Err "  return-service failed! See .demo-logs\return-service-err.log"; exit 1 }
    Write-Ok "  return-service ready."
}

if ($startAuthority) {
    $ok = Wait-ForUrl "http://localhost:8084/actuator/health"
    if (-not $ok) { Write-Err "  payment-service failed! See .demo-logs\payment-service-err.log"; exit 1 }
    Write-Ok "  payment-service ready."

    $ok = Wait-ForUrl "http://localhost:8085/actuator/health"
    if (-not $ok) { Write-Err "  records-service failed! See .demo-logs\records-service-err.log"; exit 1 }
    Write-Ok "  records-service ready."
}

# --- Step 5: Start portals ---
$step++
Write-Status "[$step/$totalSteps] Starting portal(s)..."

if ($startTaxable) {
    Start-Service -Name "taxable-person-portal" `
        -JarPattern "osm2-taxable-person-portal\target\osm2-taxable-person-portal-*.jar" `
        -Profile $portalProfile -DbName $null `
        -ExtraArgs @{
            "OIDC_CLIENT_SECRET"        = $TaxablePortalClientSecret
            "SCHEME_SERVICE_URL"        = "http://localhost:8081"
            "REGISTRATION_SERVICE_URL"  = "http://localhost:8082"
            "RETURN_SERVICE_URL"        = "http://localhost:8083"
        }

    $ok = Wait-ForUrl "http://localhost:8090/actuator/health" 60
    if (-not $ok) { Write-Err "  taxable-person-portal failed! See .demo-logs\taxable-person-portal-err.log"; exit 1 }
    Write-Ok "  taxable-person-portal ready."
}

if ($startAuthority) {
    Start-Service -Name "authority-portal" `
        -JarPattern "osm2-authority-portal\target\osm2-authority-portal-*.jar" `
        -Profile $portalProfile -DbName $null `
        -ExtraArgs @{
            "OIDC_CLIENT_SECRET"        = $AuthorityPortalClientSecret
            "SCHEME_SERVICE_URL"        = "http://localhost:8081"
            "REGISTRATION_SERVICE_URL"  = "http://localhost:8082"
            "RETURN_SERVICE_URL"        = "http://localhost:8083"
            "PAYMENT_SERVICE_URL"       = "http://localhost:8084"
            "RECORDS_SERVICE_URL"       = "http://localhost:8085"
        }

    $ok = Wait-ForUrl "http://localhost:8091/actuator/health" 60
    if (-not $ok) { Write-Err "  authority-portal failed! See .demo-logs\authority-portal-err.log"; exit 1 }
    Write-Ok "  authority-portal ready."
}

# =========================================================================
# Summary
# =========================================================================
Write-Host ""
Write-Ok "============================================="
Write-Ok "  OSM2 demo is running!"
Write-Ok "============================================="
Write-Host ""

if ($startTaxable) {
    Write-Host "  Taxable Person Portal:  http://localhost:8090"
}
if ($startAuthority) {
    Write-Host "  Authority Portal:       http://localhost:8091"
}

Write-Host ""
Write-Host "  Backend APIs (Swagger UI):"
Write-Host "    Scheme service:        http://localhost:8081/swagger-ui.html"
Write-Host "    Registration service:  http://localhost:8082/swagger-ui.html"

if ($startTaxable -or $startAuthority) {
    Write-Host "    Return service:        http://localhost:8083/swagger-ui.html"
}
if ($startAuthority) {
    Write-Host "    Payment service:       http://localhost:8084/swagger-ui.html"
    Write-Host "    Records service:       http://localhost:8085/swagger-ui.html"
}

Write-Host ""
Write-Host "  Demo users (Keycloak):"
Write-Host "    demo-taxable       / (see realm config)  — taxable person"
Write-Host "    demo-caseworker    / (see realm config)  — SKAT caseworker"
Write-Host "    demo-supervisor    / (see realm config)  — SKAT supervisor"
Write-Host "    demo-intermediary  / (see realm config)  — intermediary/agent"
Write-Host "  Keycloak admin:         http://localhost:8080/admin/  (admin / admin)"

Write-Host ""
Write-Host "  Observability:"
Write-Host "    Grafana:           http://localhost:3000"
Write-Host "    Prometheus:        http://localhost:9090"
Write-Host "    (Traces via OTLP -> Tempo; logs via Promtail -> Loki)"

Write-Host ""
Write-Host "  Stop with:              .\start-demo.ps1 -Stop"
Write-Host "  Logs in:                .demo-logs\"
Write-Host "  Note: Docker infra keeps running after -Stop."
Write-Host ""
