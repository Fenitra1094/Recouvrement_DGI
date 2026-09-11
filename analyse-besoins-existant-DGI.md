# Analyse de l'Existant et des Besoins
## Système de Suivi de Recouvrement des Déclarations — DGI

---

## 1. Contexte et problématique

Le DGI dispose déjà d'outils de déclaration en ligne et d'inscription en ligne pour les contribuables. Cependant, une fois la déclaration effectuée, le suivi du recouvrement (paiement, relance, gestion des retards) souffre d'un manque de visibilité et d'automatisation. Plusieurs milliards d'Ariary restent non recouvrés faute d'un pilotage efficace du processus.

Le projet vise à concevoir un système de **Suivi de Déclaration et Recouvrement**, centré sur trois fonctions clés : la **relance**, la **demande d'échéance**, et la **mise en veilleuse** des dossiers, avec gestion de différents types de paiement.

---

## 2. Acteurs concernés

| Acteur | Rôle |
|---|---|
| **Agent de recouvrement / Inspecteur** | Suit les dossiers, envoie les relances, instruit les demandes d'échéance |
| **Chef de Centre Fiscal** | Valide les relances et décisions sensibles (échéance, mise en veilleuse, ATD), a besoin d'une vue globale |
| **Contribuable (particulier/société)** | Déclare, paie, peut demander un délai de paiement |
| **Trésor Public / Banques / Opérateurs Mobile Money** | Interviennent dans la chaîne de paiement |

---

## 3. Analyse de l'existant (AS-IS)

### 3.1 Système d'information actuel : un existant hybride

Le DGI dispose de deux systèmes officiels :
- **SAFI** (Système d'Administration Fiscale Intégré) : censé centraliser les informations de tous les contribuables et l'historique de leurs paiements.
- **HetraOnline** : plateforme de déclaration et paiement en ligne.

Dans la pratique, au niveau des Centres Fiscaux / DRI / DRE, une grande partie des agents et inspecteurs continue de travailler sur **Excel**, en local, pour le suivi et la relance. Cet usage est confirmé par un échange direct avec un contact opérationnel au sein du DGI.

**Pourquoi ce retour à Excel malgré l'existence de SAFI ?**
- SAFI est perçu comme lent, avec une connexion/serveur souvent instable
- Il est difficile d'en extraire rapidement des états statistiques exploitables
- Chaque service (Recouvrement, Contrôle, Assiette) a donc développé son propre fichier Excel

**Conséquence :** des incohérences apparaissent entre les fichiers Excel des agents et les données réelles dans SAFI — la même information peut différer selon l'endroit où on la consulte.

### 3.2 Circuits de paiement existants

Le mode de paiement détermine un circuit différent, avec des délais variables avant que le paiement soit visible dans SAFI :

| Mode de paiement | Circuit de traitement | Point de friction |
|---|---|---|
| **Virement bancaire / HetraOnline** | Bordereau de versement → réconciliation banque/Trésor Public → validation DGI/Trésor → crédit dans SAFI | Délai de réconciliation bancaire |
| **Mobile Money (e-Hetraphone : Mvola, Orange Money, Airtel Money)** | Passage par l'API de l'opérateur → plateforme e-Hetraphone → doit se refléter dans SAFI | Le reflet dans SAFI n'est pas toujours immédiat |
| **Espèces / Chèque en Caisse ou au Trésor** | Remise d'une quittance de paiement papier → saisie manuelle dans SAFI ou dans un registre/journal de caisse | Risque d'oubli ou d'erreur de saisie manuelle |

**Problème central identifié :** le rapprochement bancaire est souvent tardif. Un contribuable qui a réellement payé peut se voir indiqué comme **« reste à payer »** dans SAFI, simplement parce que la mise à jour n'a pas encore été effectuée. Ce n'est pas seulement un problème de lenteur technique — c'est un problème de **fiabilité de la donnée qui affecte directement le contribuable**, avec un risque réel d'être relancé ou pénalisé à tort.

### 3.3 Processus de relance existant

Le processus de relance est **entièrement manuel**, avec une escalade progressive :

1. **Lettre de mise en demeure** : l'agent rédige la lettre (Word/Excel ou SAFI), elle est signée par le Chef de Centre, puis envoyée par la poste ou remise directement par un porteur/huissier.
2. **Appel téléphonique** : réservé principalement aux grandes et moyennes entreprises (DGE) — l'inspecteur en charge du dossier appelle directement.
3. **ATD — Avis à Tiers Détenteur** : en dernier recours, si le contribuable ne répond pas, le DGI envoie une notification à toutes les banques pour bloquer le compte du contribuable jusqu'au paiement de l'impôt dû.

**Concernant le SMS :** seul e-Hetraphone envoie un SMS, mais uniquement une **confirmation de paiement reçu**. Il n'existe aujourd'hui aucun système de rappel automatique par SMS ou email avant ou après échéance (ex : « il vous reste 5 jours pour payer votre TVA » ou « vous avez une dette de 200 000 Ar »).

**Point de vigilance pour la conception :** la relance vers une société ne part pas directement de l'agent — elle passe par une **validation hiérarchique** avant envoi. Le futur système devra donc intégrer une étape de validation, et non un envoi automatique sans contrôle humain.

---

## 4. Synthèse des dysfonctionnements

| Constat | Impact concret |
|---|---|
| Coexistence SAFI / HetraOnline / Excel non synchronisés | Données incohérentes, pas de source unique fiable |
| Rapprochement bancaire tardif ou manuel | Contribuables affichés à tort comme défaillants ; perte de confiance envers l'administration |
| Relance 100% manuelle (courrier, porteur, appel) | Lenteur, pas de priorisation, charge de travail élevée pour les agents |
| Aucun rappel automatique avant échéance | Retards évitables, pénalités qui s'accumulent inutilement |
| Décision d'échéance sans critère objectif | Risque d'inégalité de traitement entre contribuables |
| Absence de vue d'ensemble en temps réel pour le Chef de Centre | Pilotage à l'aveugle, dossiers oubliés jusqu'à l'ATD |

---

## 5. Analyse des besoins

### 5.1 Besoins fonctionnels (déduits directement de l'existant)

| Constat de l'existant | Besoin | Fonctionnalité correspondante |
|---|---|---|
| Pas de rappel avant échéance | Communication proactive et automatisée | **Plateforme de relance multicanal** (SMS / WhatsApp / Email), déclenchée J-5 et J-1 avant échéance, puis J+1 et J+7 après retard, avec pénalités calculées automatiquement — sous validation hiérarchique avant envoi |
| Chef de Centre sans vue d'ensemble | Pilotage centralisé en temps réel | **Dashboard de suivi** : reste à recouvrer, statut des relances envoyées/sans réponse, dossiers à faire remonter en ATD |
| Réconciliation bancaire tardive/manuelle | Fiabilisation des données de paiement | **Module de rapprochement** entre les fichiers banque/mobile money et le fichier des contribuables |
| Décision d'échéance sans critère | Aide à la décision objective | **Module de demande/gestion d'échéance** avec critères (montant, ancienneté, historique du contribuable) et workflow de validation |
| Dossiers à suspendre temporairement | Continuité du suivi sans perte d'information | **Mise en veilleuse** d'un dossier, avec motif et durée |
| Plusieurs canaux de paiement à enregistrer | Traçabilité complète | **Enregistrement multi-types de paiement** avec historique par contribuable |

### 5.2 Contraintes et points de vigilance

- La relance automatique doit rester **soumise à validation hiérarchique**, conformément au fonctionnement actuel constaté sur le terrain.
- Le **rapprochement bancaire automatique** avec de vraies API bancaires est un périmètre ambitieux pour un projet de fin d'études — à cadrer clairement dans le cahier des charges (import de fichier simulé plutôt que connexion réelle aux banques, par exemple).
- Le système doit coexister avec l'usage actuel d'Excel — prévoir une phase de transition ou un export/import compatible pour faciliter l'adoption par les agents.

---

## 6. Objectifs du futur système

- Offrir une **vue en temps réel** du statut de chaque contribuable (actif / défaillant / en retard / en échéance)
- **Automatiser et prioriser** les relances selon des critères objectifs
- **Fiabiliser** les données de paiement pour éviter les erreurs de statut
- **Encadrer** les décisions d'échéance et de mise en veilleuse avec un processus de validation clair
- Réduire le délai entre le retard de paiement et l'action corrective (relance, ATD)

---

## 7. Prochaines étapes

1. Formaliser les processus (AS-IS et TO-BE) sous forme de diagrammes (acteurs, swimlane du processus de relance)
2. Rédiger les cas d'utilisation (UML) pour chaque module
3. Passer à la maquette (wireframes) du dashboard et du module de relance
4. Conception détaillée (base de données, architecture)
