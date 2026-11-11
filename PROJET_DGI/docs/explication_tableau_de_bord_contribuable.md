# Tableau de bord contribuable — Explication complète

> Tâches concernées dans `todo_DGI_recouvrement.xlsx` : lignes **13, 14 et 15** (Espace Contribuable / dashboard).
> Chiffres calculés le **3 novembre 2026**. Les jours de retard augmentent chaque jour, donc les montants changent avec le temps : c'est voulu.

---

## 1. À quoi sert cette page ?

Le tableau de bord est une **page d'information**. Le contribuable s'y connecte pour savoir :

1. **Combien il doit** à l'État, pénalités comprises.
2. **S'il est en règle** : statut vert, orange ou rouge.
3. **Quelles sont ses prochaines dates limites** : le calendrier.

**On ne paie pas sur cette page.** Le paiement se fait ailleurs : HetraOnline, Mobile Money (e-Hetraphone), ou à la caisse du centre fiscal. La seule action proposée est de **demander un échéancier** quand le contribuable est en retard.

---

## 2. Quelques mots de vocabulaire

| Mot | Signification simple |
|---|---|
| **Déclaration** | Le contribuable dit à la DGI : « pour telle période, je dois tant d'impôt ». Elle se fait sur HetraOnline. |
| **Principal** | Le montant de l'impôt lui-même, celui qui a été déclaré. |
| **Date limite** | Le dernier jour pour payer sans sanction. |
| **Pénalité de retard** | La somme ajoutée au principal, qui **grossit chaque mois** tant que la dette n'est pas payée. |
| **Reste dû** | Principal + pénalité de retard − ce qui a déjà été payé. |
| **Échéancier** | Un accord avec la DGI pour payer la dette en plusieurs fois. |
| **NIF** | Le numéro d'identification fiscale du contribuable. |

---

## 3. Les types d'impôts du projet : qui paie quoi ?

Les impôts sont dans la table `types_impots` et les régimes dans `regime_fiscaux` (fichier `Backend/src/main/resources/data.sql`, à partir de la ligne 12).

### 3.1 Le régime : il dépend du chiffre d'affaires

Le **chiffre d'affaires (CA)** est le total des ventes de l'année, **avant** de retirer les dépenses. C'est lui qui décide dans quelle catégorie tombe une entreprise. Ces seuils sont ceux écrits dans `data.sql` :

| Régime | Code | Chiffre d'affaires annuel | Pour qui ? |
|---|---|---|---|
| Impôt Synthétique | `ISYN` | moins de 100 millions Ar | Petits commerces, artisans, épiceries |
| Impôt sur le Revenu | `IR` | de 100 millions à 5 milliards Ar | Entreprises moyennes |
| Impôt sur les Sociétés | `IS` | 5 milliards Ar et plus | Grandes entreprises |

> ⚠️ Ces seuils et ces noms sont **ceux de notre projet**. Le Code Général des Impôts malgache réel peut être différent : par exemple, l'impôt sur les bénéfices des sociétés y est rattaché à l'IR. Fais-les valider par ton encadreur.

### 3.2 Les impôts eux-mêmes

| Impôt | Code | Fréquence | Sur quoi ? | Qui le paie ? |
|---|---|---|---|---|
| **Impôt Synthétique** | `ISYN` | 1 fois par an | Un montant calculé à partir du chiffre d'affaires | Les petites entreprises. En principe, il **remplace** l'IR et la TVA. |
| **Impôt sur le Revenu** | `IR` | 1 fois par an + des **acomptes** trimestriels | Le **bénéfice** (ventes − dépenses) | Les entreprises moyennes |
| **Impôt sur les Sociétés** | `IS` | 1 fois par an + des **acomptes** trimestriels | Le **bénéfice** | Les grandes entreprises |
| **TVA** | `TVA` | Chaque mois | La taxe ajoutée au prix de vente (taux de 20 % dans les exemples ci-dessous) | Toute entreprise soumise à l'IR ou à l'IS. Le client paie la TVA à l'entreprise, qui la reverse à l'État. |
| **IRSA** | `IRSA` | Chaque mois | Les **salaires** des employés | Tout **employeur**. Il retient l'impôt sur la fiche de paie et le reverse à l'État. |

**Pourquoi telle entreprise paie tel impôt ?**
- Elle vend avec TVA → elle déclare la **TVA** chaque mois.
- Elle a des employés → elle déclare l'**IRSA** chaque mois.
- Selon son chiffre d'affaires → **ISYN**, **IR** ou **IS** chaque année.

**L'acompte trimestriel (T1, T2, T3, T4).** L'entreprise n'attend pas la fin de l'année pour payer son IR ou son IS. Chaque trimestre, elle verse **un quart de l'impôt de l'année précédente**. Les deux exemples ci-dessous le montrent.

---

## 4. La formule de calcul et d'où vient le pourcentage

### 4.1 L'origine du taux

Le taux vient de l'**encadreur**. Sa réponse à la question sur le retard :

> « Misy pénalité de retard ny Non Dépôt : **1 % par mois de retard**. »

Le projet applique donc **une seule majoration : 1 % du principal par mois de retard**.

> Historique : la première version du code suivait le cahier des charges (Module 3), c'est-à-dire une pénalité de 10 % plus 1 % d'intérêts par mois. La pénalité de 10 % a été **supprimée** pour suivre la règle de l'encadreur.

> 💬 **Point encore à confirmer.** L'encadreur parle du **non-dépôt** (déclaration faite en retard). Le projet applique ce même 1 % par mois au **retard de paiement** : on compte depuis la date limite jusqu'au paiement. Il faut lui demander si cette lecture est correcte.

### 4.2 Le fichier qui fait le calcul

**`Backend/src/main/java/com/dgi/recouvrement/service/contribuable/DashboardService.java`**

Les taux sont écrits tout en haut du fichier. Pour les changer, il suffit de modifier ces lignes :

| Ligne | Code | Signification |
|---|---|---|
| 30 | `TAUX_PENALITE_MENSUEL = 0.01` | Pénalité de retard de **1 % du principal par mois** de retard |
| 32 | `SEUIL_ECHEANCE_PROCHE_JOURS = 7` | Une échéance est « proche » si elle tombe dans **7 jours ou moins** |

Le calcul lui-même se trouve dans la méthode `calculerEcheance` (ligne 133).

### 4.3 La formule étape par étape

Pour **chaque déclaration non payée** :

```
1. Jours de retard   = aujourd'hui − date limite        (0 si la date n'est pas passée)

2. Mois entamés      = jours de retard ÷ 30, arrondi AU-DESSUS
                       (exemple : 261 jours → 8,7 → 9 mois)

3. Pénalité          = principal × 1 % × mois entamés

4. Total brut        = principal + pénalité

5. On retire ce qui a déjà été payé (colonne dettes.montant_paye),
   dans cet ordre :  d'abord la pénalité → puis le principal

6. Reste dû          = ce qui reste après l'étape 5
```

**Pourquoi « arrondi au-dessus » ?** Un mois commencé compte comme un mois entier. Avec un jour de retard, la pénalité de 1 % s'applique déjà.

**Pourquoi payer d'abord la pénalité ?** Le principal est remboursé en dernier, donc tant que la dette n'est pas soldée, la pénalité continue de se calculer sur la totalité du principal. Cela pousse à payer vite.

### 4.4 Le statut de chaque déclaration

Méthode `determinerStatutEcheance` (ligne 175). Les questions sont posées **dans cet ordre** :

| Question | Si oui, statut = |
|---|---|
| Le reste dû vaut-il 0 ? | `PAYEE` (Réglée) |
| La dette a-t-elle le statut `EN_ATTENTE_TRAITEMENT` ? | En attente de traitement |
| La dette a-t-elle le statut `EN_ECHEANCIER` ? | En échéancier |
| La date limite est-elle dépassée ? | `EN_RETARD` 🔴 |
| La date limite est-elle dans 7 jours ou moins ? | `ECHEANCE_PROCHE` 🟠 |
| Sinon | `A_PAYER` (À échoir) |

### 4.5 Le statut global du dossier (la couleur)

Méthode `determinerStatutGlobal` (ligne 197). On garde **la situation la plus grave** parmi toutes les déclarations :

```
EN_RETARD  >  EN_ATTENTE_TRAITEMENT  >  ECHEANCE_PROCHE  >  EN_ECHEANCIER  >  A_JOUR
   🔴                 🟠                       🟠                 🟠            🟢
```

Exemple : si une seule déclaration sur dix est en retard, tout le dossier est rouge.

---

## 5. Trois contribuables racontés

Ces trois contribuables sont dans `data.sql`. Pour se connecter, l'email ou le NIF fonctionne, avec le mot de passe **`password123`**.

---

### 5.1 SOCIETE BAKO SARL — une entreprise moyenne en difficulté

**Sa vie.** BAKO SARL est une quincaillerie et un dépôt de matériaux de construction à Tsaralalana, à Antananarivo. Elle vend du ciment, des tôles et des fers à béton à des particuliers et à des petits chantiers. Elle emploie une quinzaine de personnes : vendeurs, magasiniers, chauffeurs et une comptable.

**Pourquoi ces impôts ?**
- Chiffre d'affaires : **450 000 000 Ar par an** (environ 37 millions par mois). C'est entre 100 millions et 5 milliards, donc régime **IR**. Elle dépend du **Service Régional des Entreprises Analamanga**.
- Elle vend avec TVA → **TVA** chaque mois.
- Elle paie des salaires → **IRSA** chaque mois.

**D'où viennent les montants ?**
- **IR 2025 = 12 500 000 Ar.** Son bénéfice 2025 était d'environ 62,5 millions, imposé à 20 %. Elle l'a **payé** à temps.
- **IR T1 2026 = 3 125 000 Ar.** C'est un acompte : 12 500 000 ÷ 4 = **3 125 000**. ✔
- **TVA de janvier 2026 = 4 800 000 Ar.** Exemple de calcul : elle a vendu pour 38 millions HT, soit 7,6 millions de TVA encaissés auprès de ses clients. Elle a acheté pour 14 millions HT, soit 2,8 millions de TVA payés à ses fournisseurs. Elle reverse la différence : 7,6 − 2,8 = **4,8 millions**.
- **IRSA de janvier 2026 = 1 950 000 Ar** : le total de l'impôt retenu sur les salaires de ses employés.

**Ce qui s'est passé.** BAKO a bien payé tout ce qui concernait 2025. Début 2026, un gros client chantier ne l'a pas payée, et sa trésorerie s'est vidée. Depuis février, elle ne règle plus rien.

**Le calcul au 03/11/2026 :**

| Déclaration | Date limite | Retard | Principal | Pénalité (1 % × mois) | Reste dû | Statut |
|---|---|---|---|---|---|---|
| IR 2025 | 30/04/2025 | — | — | — | 0 | Réglée |
| TVA déc. 2025 | 15/01/2026 | — | — | — | 0 | Réglée |
| IRSA déc. 2025 | 15/01/2026 | — | — | — | 0 | Réglée |
| **TVA janv. 2026** | 15/02/2026 | 261 j → **9 mois** | 4 800 000 | 4 800 000 × 1 % × 9 = 432 000 | **5 232 000** | 🔴 En retard |
| **IRSA janv. 2026** | 15/02/2026 | 261 j → **9 mois** | 1 950 000 | 1 950 000 × 1 % × 9 = 175 500 | **2 125 500** | 🔴 En retard |
| **IR T1 2026** | 15/05/2026 | 172 j → **6 mois** | 3 125 000 | 3 125 000 × 1 % × 6 = 187 500 | **3 312 500** | 🔴 En retard |
| **TVA sept. 2026** | 06/11/2026 | dans 3 jours | 5 200 000 | 0 | **5 200 000** | 🟠 Échéance proche |
| **TOTAL** | | | **15 075 000** | **795 000** | **15 870 000 Ar** | **🔴 En retard** |

**Ce que voit BAKO.** Un bandeau rouge « Dossier en retard », 3 alertes rouges et 1 alerte orange (la TVA à déclarer dans 3 jours), et un bouton « Demander un échéancier ».

> La TVA de septembre 2026 a une date limite **calculée** : `CURRENT_DATE + 3` (`data.sql`, ligne 75), recalculée à chaque redémarrage du backend. Ainsi, il y a toujours une « échéance proche » à montrer pendant la démonstration.

---

### 5.2 SOCIETE FY SA — un gros débiteur

**Sa vie.** FY SA est une grande société d'import-distribution de produits alimentaires (riz, huile, sucre) qui fournit les supermarchés de tout le pays. Elle a plusieurs centaines d'employés et des entrepôts à Antananarivo et à Toamasina.

**Pourquoi ces impôts ?**
- Chiffre d'affaires : **12 500 000 000 Ar** (12,5 milliards). C'est plus de 5 milliards, donc régime **IS**. Elle dépend de la **Direction des Grandes Entreprises (DGE)**.
- Elle vend avec TVA → **TVA** chaque mois, avec de gros montants.
- Elle a beaucoup d'employés → **IRSA** chaque mois.

**D'où viennent les montants ?**
- **IS 2024 = 45 000 000 Ar.** Son bénéfice 2024 était d'environ 225 millions, imposé à 20 %.
- **IS T4 2025 = 11 250 000 Ar.** Acompte : 45 000 000 ÷ 4 = **11 250 000**. ✔
- **TVA de novembre 2025, décembre 2025 et janvier 2026** : 22, 25 et 21 millions.

**Ce qui s'est passé.** FY conteste un redressement fiscal et a cessé de payer pour faire pression. **Rien n'a été payé depuis mai 2025.** Pour un agent de recouvrement, c'est le profil typique d'un dossier à traiter en priorité : gros montants et retards anciens.

**Le calcul au 03/11/2026 :**

| Déclaration | Date limite | Retard | Principal | Pénalité (1 % × mois) | Reste dû |
|---|---|---|---|---|---|
| IS 2024 | 15/05/2025 | 537 j → **18 mois** | 45 000 000 | 45 M × 1 % × 18 = 8 100 000 | **53 100 000** |
| TVA nov. 2025 | 15/12/2025 | 323 j → **11 mois** | 22 000 000 | 22 M × 1 % × 11 = 2 420 000 | **24 420 000** |
| IS T4 2025 | 15/01/2026 | 292 j → **10 mois** | 11 250 000 | 1 125 000 | **12 375 000** |
| TVA déc. 2025 | 15/01/2026 | 292 j → **10 mois** | 25 000 000 | 2 500 000 | **27 500 000** |
| IRSA déc. 2025 | 15/01/2026 | 292 j → **10 mois** | 5 500 000 | 550 000 | **6 050 000** |
| TVA janv. 2026 | 15/02/2026 | 261 j → **9 mois** | 21 000 000 | 1 890 000 | **22 890 000** |
| **TOTAL** | | | **129 750 000** | **16 585 000** | **146 335 000 Ar** 🔴 |

**À remarquer.** Sur l'IS 2024, après 18 mois de retard, la pénalité représente déjà **18 % du principal** (8,1 millions). Chaque mois supplémentaire ajoute encore 450 000 Ar sur cette seule ligne.

---

### 5.3 DIAMONDRA ET FILS — une petite entreprise familiale en échéancier

**Sa vie.** Madame Diamondra tient une épicerie de quartier à Analakely avec ses deux fils. Elle vend des produits de première nécessité (riz, savon, pétrole lampant, crédit téléphonique). C'est une petite structure familiale, sans employé déclaré.

**Pourquoi ces impôts ?**
- Chiffre d'affaires : **65 000 000 Ar par an**. C'est moins de 100 millions, donc régime **ISYN** (Impôt Synthétique). Elle dépend du **Centre Fiscal de proximité Antananarivo I**.
- L'ISYN **remplace** l'IR et la TVA : elle n'a **pas de TVA**.
- Elle n'a pas de salarié : **pas d'IRSA**.
- Elle ne déclare donc **qu'un seul impôt par an**.

**Ce qui s'est passé.**
- **ISYN 2024 = 1 200 000 Ar** : payé à temps.
- **ISYN 2025 = 3 600 000 Ar** : l'épicerie a bien marché, l'impôt a augmenté. Mais Madame Diamondra n'a pas pu tout payer avant le 31/03/2026.
- Elle a **demandé un échéancier** au centre fiscal. Il a été **accepté** : la dette porte le statut `EN_ECHEANCIER` dans la table `dettes`.
- Elle a déjà versé **1 000 000 Ar**.

**Le calcul au 03/11/2026, avec l'imputation du paiement :**

```
Retard            : 31/03/2026 → 03/11/2026 = 217 jours → 217 ÷ 30 = 7,2 → 8 mois entamés

Principal         :                                   3 600 000
Pénalité          : 3 600 000 × 1 % × 8          =      288 000
                                                   -----------
Total brut                                       =    3 888 000

Paiement de 1 000 000, imputé dans l'ordre :
  1) sur la pénalité :  288 000 → pénalité = 0        (il reste 712 000 à imputer)
  2) sur le principal : 3 600 000 − 712 000 = 2 888 000

Reste dû = 0 + 2 888 000 = 2 888 000 Ar     Statut : 🟠 En échéancier
```

**Ce que voit Diamondra.** Un bandeau orange « Dossier en échéancier », qui lui rappelle que **2 mensualités manquées font rompre l'échéancier**.

> 💬 **Point à discuter avec l'encadreur.** Aujourd'hui, la pénalité continue de courir même quand un échéancier est accordé. En pratique, un échéancier accepté **gèle** souvent les pénalités. C'est une règle métier à trancher avant de coder le module échéancier.

---

## 6. Comment les données circulent

```
 Base PostgreSQL                Backend Spring Boot                                Front Vue.js
┌──────────────────┐   ┌─────────────────────────────────────────────┐   ┌──────────────────────────┐
│ contribuables    │   │ DeclarationRepository / DetteRepository     │   │ DashboardView.vue        │
│ declaration      │──▶│   (lisent les tables)                       │   │                          │
│ dettes           │   │            │                                │   │ appelle :                │
│ types_impots     │   │            ▼                                │   │ GET /api/contribuable/   │
└──────────────────┘   │ DashboardService.java   ◀── LE CALCUL       │   │  dashboard-summary/{NIF} │
                       │            │                                │   │                          │
                       │            ▼                                │   │ et affiche : bandeau,    │
                       │ ContribuableController.java  ── JSON ───────┼──▶│ indicateurs, alertes,    │
                       │   GET /dashboard-summary/{nif}              │   │ tableau, calendrier      │
                       └─────────────────────────────────────────────┘   └──────────────────────────┘
```

**Le front ne calcule rien.** Il reçoit des montants déjà calculés et les met en forme. Toute la logique métier est dans `DashboardService.java`.

---

## 7. Liste des fichiers modifiés

| Fichier | Ce qui a changé | Pourquoi |
|---|---|---|
| `Backend/.../service/contribuable/DashboardService.java` | **Réécrit.** Calcule la pénalité de retard (1 % par mois), l'imputation des paiements, les statuts et les alertes. | Avant, il lisait les pénalités telles quelles dans la base, sans calcul, et ignorait les paiements. |
| `Backend/.../dto/contribuable/DashboardSummaryDTO.java` | Ajout de champs : couleur, alertes, et pour chaque échéance le détail principal / pénalité / payé / reste. | Pour envoyer au front tout ce qu'il doit afficher. |
| `Backend/.../entity/Dette.java` (ligne 30) | Nouvelle colonne `statut` : `OUVERTE`, `EN_ATTENTE_TRAITEMENT`, `EN_ECHEANCIER`, `SOLDEE`. | Pour savoir si une dette est en échéancier ou en attente d'une décision. |
| `Backend/.../repository/DeclarationRepository.java` | `findByContribuableId` renommé en `findByContribuableIdContribuables`. | Le nom ne correspondait pas au champ de l'entité, et le backend plantait au démarrage. |
| `Backend/src/main/resources/data.sql` | Colonnes inexistantes retirées, statut des dettes ajouté, déclaration 107 (échéance proche) ajoutée, mot de passe `password123` corrigé. | Les données de test n'entraient pas dans la base, et personne ne pouvait se connecter. |
| `Frontend/CONTRIBUABLE/src/views/contribuable/DashboardView.vue` | **Réécrit.** Appelle la bonne API, affiche le bandeau, les indicateurs, les alertes, le tableau et le calendrier. Plus de bouton « Payer ». | Avant, il appelait une adresse qui n'existait pas, donc la page affichait toujours une erreur. |
| `Frontend/CONTRIBUABLE/src/layouts/ContribuableLayout.vue` | Emojis remplacés par des icônes. « Demande de Moratoire » devient « Demande d'échéancier ». | Rendu plus professionnel, et vocabulaire aligné sur le cahier des charges. |
| `Frontend/CONTRIBUABLE/src/router/index.js` | `auth.isLoggedIn` remplacé par `auth.isAuthenticated`. | `isLoggedIn` n'existait pas. |

> **Colonnes qui ne servent plus au calcul :** `declaration.montant_penalites`, `dettes.montant_penalites` et `dettes.solde_restant`. Ces valeurs étaient tapées à la main et ne bougeaient jamais. Elles sont maintenant **recalculées** à chaque affichage. Seuls `dettes.montant_paye` et `dettes.statut` sont encore lus.

---

## 8. Ce qu'il te reste à faire

1. **Relancer** : `docker compose up -d --build`. **Toujours** avec `--build` après une modification du code.
2. **Vérifier l'écran** sur http://localhost:3000 avec `rochelrandriamanntena@gmail.com` / `password123`. Tu dois retrouver les chiffres du tableau de la section 5.1.
3. **Confirmer avec l'encadreur** :
   - le 1 % par mois s'applique-t-il aussi au **retard de paiement**, et pas seulement au non-dépôt ? (section 4.1)
   - la pénalité pendant un échéancier : continue-t-elle ou est-elle gelée ? (section 5.3)
   - les seuils ISYN / IR / IS (section 3.1)
4. **Cocher les lignes 13, 14 et 15** dans la todo.
5. **Pour changer le taux** : modifier la ligne 30 de `DashboardService.java`, puis relancer avec `--build`.

**Ce qui n'est PAS dans cette tâche** (et viendra plus tard) :
- Mettre une dette en `EN_ATTENTE_TRAITEMENT` quand le contribuable dépose une demande d'échéancier → **ligne 22**.
- Empêcher un contribuable de voir le tableau de bord d'un autre NIF : aujourd'hui, tout le monde a accès à tout → **ligne 43 (sécurisation)**.
