# Petition: OSS-01 — Juridisk grundlag, skemavurdering og definitioner
# Legal basis: ML §§ 66, 66a, 66d, 66m / MSD artikler 358, 358a, 369a, 369l
# Gennemførelsesforordning 282/2011 artikel 57a
# Direktiv 2017/2455 / Direktiv 2019/1995 / Lov nr. 810 af 9. juni 2020

Feature: Regelhierarki og juridisk sporbarhed
  Som Skatteforvaltningens OSS-system
  Skal systemet respektere det korrekte regelhierarki
  og ledsage alle afgørelser med verificerbar juridisk hjemmel
  i henhold til DA16.3.1.3 og ML §§ 66-66u

  Scenario: Momslovens almindelige regler finder sekundær anvendelse
    Given en afgiftspligtig person tilsluttet Ikke-EU-ordningen
    And en situation er ikke udtømmende reguleret af OSS-særordningens specifikke regler
    When systemet fastslår hvilke regler der finder anvendelse
    Then finder Momslovens almindelige regler sekundært anvendelse
    And hjemmel angives som "DA16.3.1.3 — ML almindelige regler (sekundær)"

  Scenario: Opkrævningslovens regler finder tertiær anvendelse for ikke-EU-virksomheder
    Given en afgiftspligtig person etableret uden for EU tilsluttet en OSS-ordning
    And en procedure er ikke reguleret af ML eller OSS-specifik lovgivning
    When systemet fastslår hvilke regler der finder anvendelse
    Then finder Opkrævningslovens regler tertiært anvendelse
    And hjemmel angives som "DA16.3.1.3 — Opkrævningsloven (tertiær)"

  Scenario: Klassifikationsafgørelse ledsages altid af juridisk hjemmel
    Given en afgiftspligtig person vurderet af systemet
    When systemet returnerer en klassifikationsafgørelse
    Then indeholder afgørelsen reference til den relevante ML-paragraf
    And indeholder afgørelsen reference til den relevante MSD-artikel
