# Petition: OSS-01 — Juridisk grundlag, skemavurdering og definitioner
# Legal basis: ML §§ 66, 66a, 66d, 66m / MSD artikler 358, 358a, 369a, 369l
# Gennemførelsesforordning 282/2011 artikel 57a
# Direktiv 2017/2455 / Direktiv 2019/1995 / Lov nr. 810 af 9. juni 2020

Feature: Håndtering af utilstrækkelige oplysninger
  Som Skatteforvaltningens OSS-system
  Skal systemet afvise klassifikation og anmode om supplerende oplysninger
  når de nødvendige grundoplysninger mangler

  Scenario: Systemet kan ikke klassificere uden etableringsoplysninger
    Given en afgiftspligtig person uden angivelse af etableringsland eller etableringstype
    When systemet vurderer berettigelsen til OSS-særordning
    Then returnerer systemet "Utilstrækkelige oplysninger — etableringsland eller etableringstype mangler"
    And foretages ingen klassifikationsafgørelse

  Scenario: Systemet kan ikke klassificere Importordning uden angivet forsendelsesværdi
    Given fjernsalg af varer indført fra et land uden for EU
    And forsendelsens reelle værdi er ikke angivet
    When systemet vurderer berettigelsen til Importordningen
    Then returnerer systemet "Utilstrækkelige oplysninger — reel forsendelsesværdi skal angives"
    And foretages ingen klassifikationsafgørelse for Importordningen
