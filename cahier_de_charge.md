# 📘 PROJET 1 : PLATEFORME DE GESTION INTELLIGENTE DU
RECOUVREMENT FISCAL


---


## 1. 🎯 Le Titre officiel de Soutenance


> ***« Conception et développement d’une plateforme intelligente de gestion
et de suivi du recouvrement des Restes À Recouvrer (RAR), avec relances
automatisées, gestion des échéanciers de paiement et cartographie SIG – Cas de
la DGI / SSIF »***


---


## 2. ❓ Le Problème (Problématique)


Actuellement à la DGI, on constate que :


- Un contribuable peut **déclarer** un impôt (grâce à Hetra Online),

- Mais **ne pas le payer** dans les délais légaux.

- Le suivi des **impayés (RAR)** est souvent :

- fait manuellement sur Excel,

- dispersé entre plusieurs agents,

- sans priorisation claire,

- sans relances automatiques.

👉 Résultat : **L'État perd des
milliards** chaque année en recettes déclarées mais jamais encaissées.


**Question centrale du mémoire :**


> ***Comment améliorer l’efficacité du recouvrement fiscal à la DGI en
automatisant le suivi, les relances, les échéanciers et en aidant les agents à
prioriser les dossiers les plus rentables ?***


---


## 3. 🎯 Les Objectifs du projet


### Objectif général


Concevoir et réaliser une plateforme web pour la DGI qui automatise et optimise
le processus de recouvrement des impayés fiscaux.


### Objectifs spécifiques


1. Centraliser toutes les dettes fiscales dans un seul système.

2. Calculer automatiquement les pénalités et intérêts de retard.

3. Automatiser les relances (amiable, mise en demeure, contentieux).

4. Gérer les échéanciers de paiement (plans de paiement échelonné).

5. Prioriser les dossiers avec un score de recouvrabilité.

6. Fournir des tableaux de bord de pilotage à la direction.

7. Cartographier les zones à fort reste à recouvrer avec Leaflet.


---


## 4. 🧩 Les Modules de l’application


Voici le découpage complet.


### 🔹 Module 1 : Authentification & Gestion des
utilisateurs


- Connexion sécurisée (login/mot de passe).

- Rôles :

- **Admin** : gère tout le système.

- **Chef de service** : voit tous les dossiers, valide les échéanciers.

- **Agent de recouvrement** : traite ses dossiers assignés.

- **Caissier** : enregistre les paiements et génère les quittances.

- Journal des actions (traçabilité).


---


### 🔹 Module 2 : Gestion des contribuables et de leurs
dettes (CRUD léger)


- Fiche contribuable (NIF, nom, activité, adresse, téléphone, GPS).

- Import Excel/CSV des rôles de dettes (venant d'Hetra Online ou saisi
manuellement).

- Chaque dette est rattachée à :

- un impôt (TVA, IR, IS, IRSA, DE...),

- une période (mois/année),

- un montant principal,

- une date d’exigibilité.


> ***⚠️ C’est \~10% du projet, pas le cœur.***


---


### 🔹 Module 3 : Moteur de calcul du RAR (Cœur métier
n°1)


Pour chaque dette, l’application calcule automatiquement :


**Formule :**


```

svgtextsvgsvg

```


```

RAR = Montant Principal + Pénalités + Intérêts de retard - Paiements effectués

```


**Détails :**


- **Pénalités** : par exemple 10% du principal si retard > 1 mois.

- **Intérêts de retard** : par exemple 1% par mois de retard.

- **Ventilation des paiements** : quand un contribuable paye, on impute
d’abord sur :

1. les pénalités,

2. puis les intérêts,

3. puis le principal.


**Situation fiscale globale du contribuable :**


- Somme de toutes ses dettes.

- Somme de tous ses paiements.

- Solde total dû.


---


### 🔹 Module 4 : Workflow de recouvrement (Cœur métier
n°2)


Chaque dette suit un cycle automatique :


| **ÉtapeDéclencheurDocument généré** | | |

| ----------------------------------- | --------------------------- |
------------------------ |

| 1. Détection impayé | Date d’exigibilité dépassée | Aucune |

| 2. Relance amiable | J+15 après exigibilité | Lettre de relance PDF |

| 3. Mise en demeure | J+30 sans réponse | Mise en demeure PDF |

| 4. Proposition échéancier | Sur demande contribuable | Contrat d’échéancier
PDF |

| 5. Recouvrement forcé | J+60 sans réponse | Ordre de poursuite |

| 6. Clôture | Payé / Abandon | Attestation de paiement |


Chaque étape a :


- un statut,

- un agent responsable,

- une date,

- un historique.


---


### 🔹 Module 5 : Gestion des Échéanciers de paiement
(Cœur métier n°3)


Un contribuable qui ne peut pas tout payer d’un coup peut demander
un **plan de paiement échelonné**.


Fonctionnalités :


- Création d’un échéancier (3, 6, 12 mois).

- Calcul automatique des mensualités.

- Dépôt initial obligatoire (ex: 20% du total).

- Suivi automatique :

- échéance à jour ✅

- échéance manquée ⚠️

- Alerte si une échéance est en retard.

- **Rupture automatique** de l’échéancier après 2 impayés → retour en
recouvrement forcé.

- Génération PDF du contrat d’échéancier signé.


---


### 🔹 Module 6 : Relances automatisées intelligentes
(Cœur métier n°4)


- Génération automatique des lettres de relance à envoyer chaque jour.

- File d'attente "à traiter aujourd’hui" pour chaque agent.

- Règles de relance :

- Ne pas relancer si un paiement a été fait dans les 7 derniers jours.

- Escalade automatique (relance 1 → relance 2 → mise en demeure).

- Simulation d’envoi SMS/Email (dans le projet, on simule sans vraie API pour
rester dans les 3 mois).


---


### 🔹 Module 7 : Scoring de recouvrabilité (Cœur métier
n°5)


Chaque dette reçoit un **score de recouvrabilité de 0 à 100** :


| **CritèrePoids** | |

| --------------------------------------- | ------ |

| Montant de la dette | 20 pts |

| Ancienneté (récente = mieux) | 20 pts |

| Existence d’un contact valide | 15 pts |

| Historique de paiements du contribuable | 15 pts |

| Échéancier en cours ou pas | 10 pts |

| Type de contribuable (actif/inactif) | 10 pts |

| Localisation accessible | 10 pts |


Résultat :


- 🟢 Score 70–100 : facilement recouvrable → à traiter
en priorité.

- 🟠 Score 40–69 : moyennement recouvrable.

- 🔴 Score 0–39 : difficilement recouvrable.


Le chef de service peut dire :


> ***« Traite d’abord les 50 dossiers verts, tu vas ramener plus d’argent en
moins de temps. »***


---


### 🔹 Module 8 : Paiements & Quittances


- Enregistrement d’un paiement par le caissier.

- Ventilation automatique sur les dettes.

- Génération immédiate d’une **quittance PDF** avec numéro unique.

- Mise à jour automatique du RAR du contribuable.


---


### 🔹 Module 9 : Tableaux de bord et pilotage


Pour la direction, en temps réel :


- Stock total du RAR (en Ariary).

- RAR par impôt (TVA, IR, IS...).

- RAR par ancienneté (0–30j, 31–90j, 90j+).

- Taux de recouvrement mensuel.

- Top 20 des plus gros débiteurs.

- Performance par agent (combien il a recouvré).

- Évolution des encaissements sur 12 mois (graphique Chart.js).

- Taux de respect des échéanciers.

- Export Excel/PDF des rapports.


---


### 🔹 Module 10 : Cartographie SIG avec Leaflet


Petit module (\~10%) mais très utile visuellement.


- Chaque contribuable a des coordonnées GPS (latitude/longitude).

- Carte OpenStreetMap avec :

- 🟢 vert : contribuable à jour

- 🟠 orange : échéancier en cours

- 🔴 rouge : gros débiteur / mise en demeure

- **Heatmap** : zones géographiques à fort reste à recouvrer.

- Filtres : par impôt, par montant, par ancienneté.

- Utilité concrète : aide à organiser les **tournées de
recouvrement** par quartier (envoyer un agent une fois pour visiter 10
débiteurs proches).


---


## 8. 🎓 Plan du rapport de soutenance


1. **Introduction générale**

2. **Chapitre 1 : Présentation de la DGI et du SSIF**

3. **Chapitre 2 : Contexte général sur la fiscalité et le recouvrement**

4. **Chapitre 3 : Problématique et étude de l’existant**

5. **Chapitre 4 : Analyse et conception (UML, MERISE)**

6. **Chapitre 5 : Réalisation et implémentation**

7. **Chapitre 6 : Présentation des résultats + captures d’écran**

8. **Conclusion et perspectives**
  voilale rendu finale , de mon projet , renvoie moi le cahier de charges ( on parle de jour pas semaine comme cela) , en .md , pour le vente en ligne , je vais encore en parler avec l'encadreur 