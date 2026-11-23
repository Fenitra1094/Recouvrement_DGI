# Déclaration, bordereau et paiement

État au 13/11/2026. Ce document décrit le circuit **déclaration → bordereau → paiement**, du backend aux écrans.

| Écran | Où | Rôle |
|---|---|---|
| Inscription | espace contribuable, étape 1 | le CA et « Option IR ? » fixent le régime |
| Déclarations | espace contribuable, `/contribuable/declarer` | déclarer, voir l'impôt calculé, imprimer le bordereau ; liste « À payer » |
| Guichet | DGI (agent), `/dgi/paiements-especes` | encaisser un bordereau (espèces, chèque), déclarer pour un contribuable, chèques en attente |
| Import du relevé | DGI (chef de centre), `/dgi/import-banque` | importer le CSV de la banque, voir les lignes refusées |

## 1. Le circuit

```
1. RÉGIME        à l'inscription : CA > seuil ? option IR ?  →  impôts dus (assujettissements)
2. DÉCLARATION   en ligne ou au guichet : on saisit la BASE, le système calcule l'impôt et la date limite
3. BORDEREAU     BV-2026-000001, montant = principal + pénalités du jour
4. PAIEMENT      espèces (validé tout de suite), chèque (attend la banque), virement (import du relevé)
5. SOLDE         bordereau PAYE, déclaration PAYEE, dette SOLDEE ; plus aucune relance
```

Règle de départ : **chaque contribuable paie la totalité de ce qu'il doit**. Il n'y a pas de paiement partiel.

## 2. Régime fiscal

```
CA > seuil ? ── Oui → IR + TVA            (régime du réel)
   └ Non → Option IR ? ── Oui → IR + TVA
              └ Non → IS                  (Impôt Synthétique)
```

- Le seuil (400 000 000 Ar) est dans `config/impots.properties`.
- **L'IRSA n'est pas un régime.** C'est l'impôt de l'employeur : il le retient sur les salaires, puis le déclare et le paie. Le salarié ne déclare rien. L'IRSA est donné par l'assujettissement : pour l'instant, il est créé dans les données de départ (ROCHEL et HARIMALALA), pas à l'inscription. L'ajout de l'IRSA lors d'une embauche relève du futur module « changement de situation fiscale » (`database/nouveau.sql`, partie 2).
- **IS veut dire Impôt Synthétique.** L'ancien code `ISYN` et l'ancien « Impôt sur les Sociétés » ont été supprimés. Les sociétés paient l'IR.
- À l'inscription, le régime est calculé (`CalculImpot.regime`) et les assujettissements sont créés. On ne peut déclarer **que** les impôts de son régime.

## 3. Calcul de l'impôt

Le contribuable ne saisit pas le montant de l'impôt. Il saisit la **base**, et le système calcule l'impôt (`CalculImpot.calculer`).

| Impôt | Périodicité | Base saisie | Calcul |
|---|---|---|---|
| IS | annuelle | chiffre d'affaires de l'année | base × 5 % |
| IR | annuelle | revenu net imposable de l'année | base × 20 % |
| TVA | mensuelle | CA HT du mois + TVA déductible | base × 20 % − déductible ; jamais négatif |
| IRSA | mensuelle | salaire imposable du mois | barème par tranches, minimum 3 000 Ar |

**Barème IRSA :**

| Tranche du salaire | Taux |
|---|---|
| jusqu'à 350 000 | 0 % |
| 350 000 à 400 000 | 5 % |
| 400 000 à 500 000 | 10 % |
| 500 000 à 600 000 | 15 % |
| au-delà de 600 000 | 20 % |

Exemple : pour un salaire de 1 000 000, l'IRSA vaut 2 500 + 10 000 + 15 000 + 80 000 = **107 500 Ar**.

**Dates limites :**
- impôts mensuels : le 15 du mois qui suit la période ;
- impôts annuels : le 15 mai de l'année qui suit l'exercice.

**Contrôles à la déclaration :**
- l'impôt doit être un impôt du régime du contribuable ;
- la période doit être terminée ;
- une période ne peut être déclarée qu'une seule fois.

Si l'impôt calculé vaut 0 (crédit de TVA, salaire sous le seuil), la déclaration est enregistrée comme réglée, sans bordereau.

## 4. Bordereau toujours à jour

- **Montant** : le reste à payer au jour de l'émission, calculé par `CalculEcheance`, la même formule que le tableau de bord et la relance.
- **Validité** : `bordereau.validite-jours` (30), mais **jamais au-delà de la veille d'une hausse de pénalité**. Son montant reste donc juste pendant toute sa validité.
- **Expiré** : il est annulé (statut ANNULE) et un nouveau est émis au montant du jour. Ça se fait automatiquement quand le guichet ou le contribuable le consulte.

Exemples réels (tests du 11/11/2026) :

| Cas | Date limite | Retard | Montant du BV | Valable jusqu'au | Pourquoi |
|---|---|---|---|---|---|
| Diana IS 2025 | 15/05/2026 | 180 j | 1 500 000 + 90 000 (6 mois) | 11/11/2026 | le 7ᵉ mois commence le 12/11 |
| Rochel TVA 10/2026 | 15/11/2026 | 0 | 1 500 000 | 15/11/2026 | la pénalité commence le 16/11 |
| Carene IS 2025 | 31/03/2026 | 225 j | 2 888 000 | 26/11/2026 | le 9ᵉ mois commence le 27/11 |

**Statuts du bordereau :** A_PAYER → EN_ATTENTE_ENCAISSEMENT (chèque remis) → PAYE ; ou ANNULE (expiré, ou chèque rejeté).

## 5. Paiement

| Mode | Où | Effet |
|---|---|---|
| Espèces | guichet | paiement VALIDE ; bordereau, déclaration et dette soldés tout de suite |
| Chèque | guichet | paiement EN_ATTENTE ; **pénalités figées à la date de remise** ; plus de relance pendant l'attente |
| Virement | import du relevé | paiement VALIDE, daté du jour de l'opération bancaire |

Le montant reçu doit être **exactement** celui du bordereau.

**Chèque :**
- **confirmé** par la banque : il devient VALIDE, à la date de remise. Le contribuable a payé ce jour-là ; le délai de la banque n'est pas de sa faute.
- **rejeté** (sans provision) : il devient REJETE et le bordereau est ANNULE. La déclaration repasse EN_RETARD, les pénalités reprennent depuis la date limite et la relance la reprend.

**Numérotation :**
- bordereaux : `BV-2026-000001` ;
- paiements : `ESP-…`, `CHQ-…`, `VIR-…`.

Les numéros viennent de deux séquences Postgres (`seq_reference_bv`, `seq_reference_paiement`). Elles ne repartent jamais à zéro, donc il n'y a jamais de doublon.

## 6. Import du relevé bancaire

Fichier CSV, séparateur `;`, encodage UTF-8, première ligne = en-tête :

```
date_operation;reference_banque;reference_bv;montant;type;numero_cheque
2026-11-12;OP-000101;BV-2026-000001;1590000;CHEQUE_PAYE;0012345
15/10/2026;OP-000102;BV-2026-000003;2 385 000;VIREMENT;
2026-11-12;OP-000105;BV-2026-000007;3000000;CHEQUE_IMPAYE;0099887
```

| type | Signification |
|---|---|
| VIREMENT | paiement d'un bordereau par virement ; le bordereau devait être valable à la date de l'opération |
| CHEQUE_PAYE | la banque a encaissé un chèque remis au guichet |
| CHEQUE_IMPAYE | la banque a rejeté ce chèque |

**Format accepté :**
- date : `AAAA-MM-JJ` ou `JJ/MM/AAAA` ;
- montant : avec ou sans espaces.

**Règles d'import :**
- chaque ligne est traitée seule ;
- une ligne refusée n'empêche pas les autres et revient avec sa raison ;
- une même `reference_banque` n'est jamais importée deux fois.

Résultat du test du 11/11/2026 : 5 lignes lues, 1 virement et 1 chèque validés, 3 lignes refusées :
- la déclaration était déjà payée ;
- l'opération avait déjà été importée ;
- le bordereau était introuvable.

## 7. API

**Espace contribuable :**

| Méthode | URL | Rôle |
|---|---|---|
| GET | `/api/contribuable/impots-declarables/{nif}` | impôts que le contribuable peut déclarer |
| POST | `/api/contribuable/declarations/{nif}/simulation` | impôt calculé avant validation (rien n'est enregistré) |
| POST | `/api/contribuable/declarations/{nif}` | déclarer en ligne |
| GET | `/api/contribuable/declarations/{nif}/{idDeclaration}/bordereau` | bordereau à jour |

**Guichet DGI :**

| Méthode | URL | Rôle |
|---|---|---|
| GET | `/api/dgi/guichet/contribuables/{nif}/impots-declarables` | impôts du contribuable |
| POST | `/api/dgi/guichet/declarations/simulation` | impôt calculé avant validation (rien n'est enregistré) |
| POST | `/api/dgi/guichet/declarations` | déclarer pour un contribuable (avec `matriculeAgent`) |
| GET | `/api/dgi/guichet/bordereaux/{reference}` | rechercher un bordereau, remplacé s'il a expiré |
| POST | `/api/dgi/guichet/encaissements` | encaisser en espèces ou par chèque |
| GET | `/api/dgi/guichet/cheques-en-attente` | chèques remis, pas encore confirmés |
| POST | `/api/dgi/guichet/cheques/{id}/confirmer` | confirmer un chèque à la main |
| POST | `/api/dgi/guichet/cheques/{id}/rejeter` | rejeter un chèque à la main |
| POST | `/api/dgi/guichet/import-banque` | importer un relevé (multipart : `fichier`, `matriculeAgent`) |

Exemple de déclaration :

```json
{ "codeImpot": "TVA", "annee": 2026, "mois": 10, "base": 10000000, "tvaDeductible": 500000 }
```

Une règle métier refusée renvoie HTTP 400 avec `{"message": "..."}`.

## 8. Configuration

| Fichier | Clés |
|---|---|
| `config/` | seuil du régime, taux IS / IR / TVA, tranches et minimum IRSA, dates limites |
| `config/bordereau.properties` | `bordereau.validite-jours` |
| `config/penalites.properties` | taux mensuel de pénalité, mois entamés ou complets |

Après une modification : `docker restart ssif-backend`. Une clé manquante empêche le démarrage, et le message nomme la clé.

## 9. Changements pour le MCD / MLD

| Table | Changement |
|---|---|
| `DECLARATION` | + `base_imposable`, `tva_deductible`, `canal` (EN_LIGNE / GUICHET), lien facultatif vers `AGENTS_DGI` (agent de saisie) |
| `PAIEMENTS` | + lien vers `BORDEREAUX_VERSEMENT`, `numero_cheque`, `banque`, `reference_banque`, lien facultatif vers `AGENTS_DGI` (agent d'encaissement ou d'import) |
| `BORDEREAUX_VERSEMENT` | statuts : A_PAYER, EN_ATTENTE_ENCAISSEMENT, PAYE, ANNULE ; plusieurs bordereaux possibles par déclaration (un seul A_PAYER à la fois) |
| `DECLARATION.statut_paiement` | + EN_ATTENTE_ENCAISSEMENT |
| `REGIME_FISCAUX` | IS (Impôt Synthétique), IR (régime du réel) ; l'ancien régime IRSA « Salarié » est supprimé |
| `TYPES_IMPOTS` | IS = Impôt Synthétique ; « Impôt sur les Sociétés » supprimé |
| Séquences | `seq_reference_bv`, `seq_reference_paiement` |
| Vue `v_obligations_a_relancer` | exclut aussi EN_ATTENTE_ENCAISSEMENT (pas de relance pendant qu'un chèque attend) |

## 10. Fichiers

| Fichier | Rôle |
|---|---|
| `service/declaration/CalculImpot.java` | régime, calcul de l'impôt, date limite |
| `service/declaration/DeclarationService.java` | déclaration en ligne et au guichet |
| `service/paiement/BordereauService.java` | émission, validité, remplacement |
| `service/paiement/EncaissementService.java` | espèces, chèque, confirmation, rejet |
| `service/paiement/ImportBanqueService.java` | import du relevé CSV |
| `service/contribuable/CalculEcheance.java` | pénalités ; prochaine hausse ; pénalités figées pendant un chèque |
| `controller/contribuable/DeclarationController.java` | API contribuable |
| `controller/dgi/GuichetController.java` | API guichet |
| `config/ImpotProperties.java`, `config/BordereauProperties.java` | lecture des fichiers de config |
| `exception/RegleMetierException.java`, `exception/GestionnaireErreurs.java` | erreurs métier en HTTP 400 |
| `Frontend/CONTRIBUABLE/src/views/contribuable/DeclarationView.vue` | écran « Déclarations » |
| `Frontend/CONTRIBUABLE/src/views/auth/RegisterView.vue` | question « option IR » |
| `Frontend/DGI/src/views/paiements/EncaissementEspeceView.vue` | écran du guichet |
| `Frontend/DGI/src/views/paiements/ImportBanqueView.vue` | écran d'import du relevé |

## 11. Limites connues

- **Taux et barème** (`config/impots.properties`) : à faire valider par l'encadreur.
- **Quittance** : imprimée depuis le navigateur, pas de PDF généré par le serveur.
- **IR** : pas de minimum de perception. **IRSA** : pas de réduction pour personnes à charge.
- **IRSA d'un employeur** : la déclaration prend un seul salaire et lui applique le barème. Pour plusieurs employés, le barème doit s'appliquer salaire par salaire. Le choix A / B / C reste ouvert (voir `database/nouveau.sql`).
- **Crédit de TVA** : non reporté sur le mois suivant.
- **Déclaration rectificative** : non prévue (une période se déclare une fois).
- **Contribuable 36 (3008777698)**, inscrit avant ce changement : il garde ses anciens assujettissements (IR, TVA, IRSA).
