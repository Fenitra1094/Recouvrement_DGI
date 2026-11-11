# Historique unifié — Espace contribuable

> Tâches du todo : **16** (affichage), **17** (rapprochement déclaration / paiement), **18** (intégration base de données)
> Cahier des charges : *« Historique unifié : consolidation des déclarations et des paiements en une seule vue. »*

---

## 1. À quoi sert cette page

Le contribuable voit **en un seul endroit** :
- tout ce qu'il a **déclaré** (les déclarations) ;
- tout ce qu'il a **payé** (les paiements) ;
- et **quel paiement règle quelle déclaration** (le rapprochement).

Différence avec le tableau de bord :

| Tableau de bord | Historique |
|---|---|
| « Où j'en suis **aujourd'hui** ? » | « Qu'est-ce que j'ai fait **avant** ? » |
| Ce qui reste à payer + pénalités | Toutes les déclarations et tous les paiements, depuis le début |
| État calculé | Registre / preuves (références de transaction) |

---

## 2. Les tables utilisées

Il n'existe **pas** de table « historique ». L'historique est **calculé** en assemblant les tables de la conception (MCD).

| Table | Rôle | Colonnes lues |
|---|---|---|
| `declaration` | Les **déclarations** | `id_declaration`, `periode`, `annee`, `date_declaration`, `date_limite_paiement`, `montant_principal`, `id_types_impots`, `id_contribuables` |
| `paiements` | Les **paiements** | `id_paiements`, `montant_payes`, `date_paiement`, `reference_transaction`, `id_modes_paiement`, `id_statut_paiement`, `id_declaration` |
| `modes_paiement` | Comment le contribuable a payé | `code` (VIREMENT, MOBILE_MONEY, ESPECES, CHEQUE), `libelle` |
| `statut_paiement` | Le paiement est-il validé ? | `libelle` (VALIDE, EN_ATTENTE, REJETE) |
| `types_impots` | Nom de l'impôt | `code`, `libelle` |
| `contribuables` | Retrouver le contribuable par son NIF | `nif`, `raison_sociale` |

**Le lien clé :** `paiements.id_declaration` → `declaration.id_declaration`.
C'est ce lien qui permet de dire « ce paiement règle cette déclaration ».

La table `dettes` n'est **pas** utilisée ici (elle sert au tableau de bord).

### Pourquoi la table `paiements` et pas `bordereaux_versement`

Le code contenait déjà une table `bordereaux_versement`, mais elle **n'est pas dans la conception**. La conception prévoit `paiements`, `modes_paiement` et `statut_paiement` : c'est donc elles qu'on utilise.
Ces 3 tables sont créées automatiquement par Hibernate (`ddl-auto=update`) à partir des classes Java, au démarrage du backend.

### Équivalent SQL de ce que fait le backend

```sql
SELECT d.id_declaration, t.code AS impot, d.periode, d.annee,
       d.date_declaration, d.date_limite_paiement, d.montant_principal,
       p.reference_transaction, p.date_paiement, p.montant_payes, m.code AS mode
FROM declaration d
JOIN contribuables c ON c.id_contribuables = d.id_contribuables
JOIN types_impots t  ON t.id_types_impots  = d.id_types_impots
LEFT JOIN paiements p
       ON p.id_declaration = d.id_declaration
      AND p.id_statut_paiement = (SELECT id_statut_paiement FROM statut_paiement WHERE libelle = 'VALIDE')
LEFT JOIN modes_paiement m ON m.id_modes_paiement = p.id_modes_paiement
WHERE c.nif = '3001000001'
ORDER BY d.date_limite_paiement DESC;
```

`LEFT JOIN` : on garde aussi les déclarations **sans aucun paiement** (elles seront « Non payée »).

---

## 3. Les fichiers touchés

### Backend (Spring Boot)

| Fichier | Nouveau / modifié | Ce qui a été fait |
|---|---|---|
| `Backend/src/main/java/com/dgi/recouvrement/entity/Paiement.java` | **nouveau** | Table `paiements` : montant, date, référence, liens vers mode, statut et déclaration |
| `Backend/src/main/java/com/dgi/recouvrement/entity/ModePaiement.java` | **nouveau** | Table `modes_paiement` |
| `Backend/src/main/java/com/dgi/recouvrement/entity/StatutPaiement.java` | **nouveau** | Table `statut_paiement` |
| `Backend/src/main/java/com/dgi/recouvrement/repository/PaiementRepository.java` | **nouveau** | `findByDeclarationContribuableNif(String nif)` : les paiements d'un contribuable, via leur déclaration |
| `Backend/src/main/java/com/dgi/recouvrement/dto/contribuable/HistoriqueDTO.java` | **nouveau** | Forme de la réponse JSON (voir §5) |
| `Backend/src/main/java/com/dgi/recouvrement/service/contribuable/HistoriqueService.java` | **nouveau** | Toute la logique : lecture, rapprochement, calculs, tri |
| `Backend/src/main/java/com/dgi/recouvrement/controller/contribuable/ContribuableController.java` | modifié | Nouvel endpoint `GET /api/contribuable/historique/{nif}` |
| `Backend/src/main/resources/data.sql` | modifié | Déclaration 200 (FY, IS 2023 payé) + 4 modes de paiement + 3 statuts de paiement + 7 paiements de test + resynchronisation des séquences |

### Frontend (Vue 3)

| Fichier | Nouveau / modifié | Ce qui a été fait |
|---|---|---|
| `Frontend/CONTRIBUABLE/src/views/contribuable/HistoriqueView.vue` | réécrit | Les données fictives en dur sont remplacées par l'appel API ; nouvelle mise en page |
| `Frontend/CONTRIBUABLE/src/utils/fiscal.js` | **nouveau** | Fonctions communes au tableau de bord et à l'historique |
| `Frontend/CONTRIBUABLE/src/views/contribuable/DashboardView.vue` | modifié | Utilise maintenant `utils/fiscal.js` au lieu de ses propres copies |

---

## 4. Les fonctions

### `HistoriqueService.getHistoriqueByNif(nif)` — le cœur

1. `contribuableRepository.findByNif(nif)` → retrouve le contribuable (erreur s'il n'existe pas).
2. `declarationRepository.findByContribuableNif(nif)` → toutes ses déclarations.
3. `paiementRepository.findByDeclarationContribuableNif(nif)` → tous ses paiements, puis :
   - on **garde seulement** ceux dont le statut est `VALIDE` (un paiement en attente ou rejeté ne règle rien) ;
   - on les **regroupe par déclaration** (`groupingBy(id_declaration)`).
4. Pour chaque déclaration :
   - liste de ses paiements, triés par date ;
   - `montantPaye` = somme des paiements ;
   - `ecart` = montant déclaré − montant payé (jamais négatif) ;
   - `etatRapprochement` (voir ci-dessous) ;
   - `declarationHorsDelai` = déposée **après** la date limite ;
   - chaque paiement : `horsDelai` = payé **après** la date limite ;
   - on ajoute 1 événement `DECLARATION` + 1 événement `PAIEMENT` par paiement dans la chronologie.
5. Totaux : total déclaré, total payé, total de l'écart, nombre de déclarations / paiements hors délai.
6. Tri : chronologie du plus récent au plus ancien ; dossiers par date limite décroissante.

### `etatRapprochement(declare, paye)` — la règle métier (tâche 17)

| Condition | État |
|---|---|
| payé = 0 | `NON_PAYEE` |
| 0 < payé < déclaré | `PARTIELLE` |
| payé ≥ déclaré | `SOLDEE` |

### `estApres(date, limite)`
Renvoie `true` si `date` est strictement après `limite`. Sert pour « déposée hors délai » et « payé hors délai ».

### Côté frontend (`HistoriqueView.vue`)

| Fonction / calcul | Rôle |
|---|---|
| `chargerHistorique()` | Appelle `GET /contribuable/historique/{nif}` avec le NIF du contribuable connecté |
| `tauxReglement` | % réglé = total payé ÷ total déclaré (l'anneau dans l'en-tête) |
| `correspond(x)` | Filtres communs : type d'impôt, exercice, recherche texte (référence, impôt, période) |
| `evenementsFiltres` / `groupesParMois` | Chronologie filtrée puis regroupée par mois |
| `dossiersFiltres` / `totauxDossiers` | Vue « par déclaration » filtrée + ligne de total |
| `basculer(id)` | Déplie / replie une déclaration pour voir ses paiements |
| `pctPaye(d)` | Barre de progression du règlement d'une déclaration |

### `utils/fiscal.js`

| Fonction | Exemple |
|---|---|
| `libellePeriode(e)` | `M12` + 2025 → « Décembre 2025 » ; `T4` → « 4e trimestre 2025 » ; `A2024` → « Exercice 2024 » |
| `typePeriode(e)` | Mensuelle / Trimestrielle / Annuelle |
| `impot(code)` | `TVA` → « Taxe sur la valeur ajoutée » + couleur |
| `modePaiement(code)` | `MOBILE_MONEY` → « Mobile money » + icône |
| `formatMontant(v)`, `formatDate(iso)` | 6100000 → « 6 100 000 » ; 2026-01-15 → « 15/01/2026 » |

---

## 5. Le flux complet

```
 Navigateur (contribuable connecté, NIF dans le store Pinia)
        │
        │  clic sur « Historique »
        ▼
 HistoriqueView.vue ── chargerHistorique()
        │
        │  GET http://localhost:8085/api/contribuable/historique/{nif}
        ▼
 ContribuableController.getHistorique(nif)
        │
        ▼
 HistoriqueService.getHistoriqueByNif(nif)
        │
        ├── ContribuableRepository.findByNif            → table contribuables
        ├── DeclarationRepository.findByContribuableNif  → table declaration (+ types_impots)
        └── PaiementRepository.findByDeclarationContribuableNif → tables paiements (+ modes_paiement, statut_paiement)
        │
        │  rapprochement : paiements groupés par id_declaration,
        │  somme, écart, état, hors délai, totaux, tri
        ▼
 HistoriqueDTO  (JSON)
        │
        ▼
 HistoriqueView.vue
        ├── En-tête : % réglé, total déclaré / versé / non réglé, alertes hors délai
        ├── Vue « Chronologie »    ← evenements
        └── Vue « Par déclaration » ← dossiers (+ paiements dépliables)
```

### Forme de la réponse JSON (`HistoriqueDTO`)

```json
{
  "nif": "3005557777",
  "raisonSociale": "DIAMONDRA ET FILS",
  "nombreDeclarations": 2,
  "nombrePaiements": 3,
  "totalDeclare": 4800000.00,
  "totalPaye": 2200000.00,
  "totalEcart": 2600000.00,
  "declarationsHorsDelai": 0,
  "paiementsHorsDelai": 0,
  "dossiers": [
    {
      "idDeclaration": 302, "impot": "ISYN", "periode": "A2025", "annee": 2025,
      "dateDeclaration": "2026-01-20", "dateLimite": "2026-03-31",
      "montantDeclare": 3600000.00, "montantPaye": 1000000.00, "ecart": 2600000.00,
      "etatRapprochement": "PARTIELLE", "declarationHorsDelai": false,
      "paiements": [
        { "reference": "MM-2026-000302A", "datePaiement": "2026-02-27", "montant": 500000.00, "mode": "MOBILE_MONEY", "horsDelai": false },
        { "reference": "ESP-2026-000302B", "datePaiement": "2026-03-30", "montant": 500000.00, "mode": "ESPECES", "horsDelai": false }
      ]
    }
  ],
  "evenements": [
    { "date": "2026-03-30", "type": "PAIEMENT", "impot": "ISYN", "periode": "A2025", "montant": 500000.00, "reference": "ESP-2026-000302B", "mode": "ESPECES", "horsDelai": false }
  ]
}
```

---

## 6. Les données de test (et leur histoire)

| Contribuable | Paiements ajoutés | Ce que ça montre |
|---|---|---|
| **BAKO SARL** (quincaillerie, IR) | IR 2025 : 12 500 000 le 29/04/2025 (virement) ; TVA déc. 2025 : 6 100 000 le 15/01/2026 (virement) ; IRSA déc. 2025 : 1 800 000 le **20/01/2026** (chèque) | Un paiement **hors délai** (IRSA, 5 jours après la limite du 15/01) |
| **FY SA** (import-distribution, IS) | IS 2023 : 38 000 000 le 14/05/2024 (virement) — déclaration 200 ajoutée | Un bon payeur en 2024, qui a ensuite cessé de payer : tout le reste est « Non payée » |
| **DIAMONDRA ET FILS** (épicerie, ISYN) | ISYN 2024 : 1 200 000 le 30/03/2025 (mobile money) ; ISYN 2025 : 500 000 le 27/02/2026 (mobile money) + 500 000 le 30/03/2026 (espèces) | Un **paiement partiel** en plusieurs versements (échéancier) |

Les montants sont **cohérents avec le tableau de bord** : par exemple BAKO a 15 075 000 Ar de principal non réglé sur les deux pages, et DIAMONDRA 1 000 000 Ar payé sur l'ISYN 2025 (= `dettes.montant_paye`).

Mot de passe de tous les comptes de test : `password123`.

---

## 7. Comment tester

1. Reconstruire : `docker compose up -d --build backend frontend-contribuable`
2. Tester l'API directement : `http://localhost:8085/api/contribuable/historique/3001000001`
3. Ouvrir `http://localhost:3000`, se connecter, cliquer sur **Historique** (Ctrl + F5 si l'ancienne version s'affiche).
4. Vérifier les tables dans psql :
   ```
   docker exec -it ssif-db psql -U dgi_user -d recouvrement_db
   SELECT reference_transaction, montant_payes, date_paiement, id_modes_paiement, id_statut_paiement, id_declaration FROM paiements;
   ```

---

## 8. Limites connues

- Les paiements viennent **uniquement de `data.sql`**. L'enregistrement d'un vrai paiement (saisie puis passage de `EN_ATTENTE` à `VALIDE` par un agent DGI) est une autre tâche, côté DGI.
- Le « reste » de l'historique porte sur le **principal déclaré**. Les pénalités (1 % par mois) sont calculées dans le tableau de bord, pas ici.
- Pas d'export PDF / Excel ni de quittance téléchargeable (non demandé dans les tâches 16-18).
- L'endpoint n'est pas encore protégé (tout est en `permitAll`) : n'importe qui connaissant un NIF peut appeler l'API. À traiter dans la tâche de sécurisation.
