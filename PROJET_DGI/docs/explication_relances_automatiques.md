# Relances automatiques et notifications : tout ce qui a été fait

> Les numéros de ligne correspondent à l'état du code au 10/11/2026 (après le passage aux gros volumes : vue SQL, lots, file d'attente des emails). Si un fichier est modifié, ils peuvent se décaler : cherche alors le nom de la fonction.

---

## 1. En une phrase

Chaque samedi à 8h (réglable dans un fichier), le backend demande à la base, via la vue `v_obligations_a_relancer`, quels contribuables sont en retard. Il leur crée **une notification** dans leur espace et met **un email** en file d'attente ; un expéditeur envoie ces emails quelques secondes plus tard. La DGI suit le tout dans la page **« Relances automatiques »**. Aucun agent n'intervient (décision de l'encadreur).

---

## 2. Le flux complet

### 2.1 Envoi d'une relance (côté serveur)

```
 ┌──────────────────────────────────────┐        ┌────────────────────────────────────────┐
 │ config/relance.properties  (ligne 24)│        │ Front DGI : bouton "Lancer maintenant" │
 │ relance.planification=0 0 8 * * SAT  │        │ JournalRelancesView.vue  (ligne 290)   │
 └──────────────────┬───────────────────┘        └───────────────────┬────────────────────┘
                    │ chaque samedi 8h                               │ POST /api/dgi/relances/lancer
                    ▼                                                ▼
 ┌──────────────────────────────────────┐        ┌────────────────────────────────────────┐
 │ RelanceScheduler.java  (ligne 40-47) │        │ RelanceController.java  (ligne 27)     │
 │ @Scheduled -> relanceAutomatique()   │        │ lancer()                               │
 └──────────────────┬───────────────────┘        └───────────────────┬────────────────────┘
                    └──────────────────────┬─────────────────────────┘
                                           ▼
 ┌─────────────────────────────────────────────────────────────────────────────────────┐
 │ RelanceService.lancerCampagne()                                        (ligne 113)  │
 │  1. la base donne les contribuables en retard :                                     │
 │     DeclarationRepository.findIdsContribuablesARelancer()  -> vue SQL               │
 │  2. découpage en lots de relance.taille-lot (500), 1 transaction par lot (l. 127)   │
 │                                                                                     │
 │  traiterLot()                                                          (ligne 143)  │
 │   3. obligations du lot : findObligationsARelancer()  -> vue SQL       (ligne 145)  │
 │      (+ nb de relances déjà faites, + "relancée récemment ?")                       │
 │   4. déclarations, dettes, contribuables du lot : 3 requêtes groupées (IN ...)      │
 │   5. CalculEcheance.calculer() -> mêmes montants que le tableau de bord (l. 161)    │
 │   6. meriteRelance() ?                                                 (ligne 209)  │
 │   7. relancée il y a moins de 7 jours ? -> ignorée (anti-spam)        (ligne 163)  │
 │                                                                                     │
 │  relancer()  (un contribuable)                                         (ligne 175)  │
 │   8. dette existante, sinon constaterDette()                           (ligne 228)  │
 │   9. niveauSuivant() : 1re / 2e / mise en demeure                      (ligne 219)  │
 │  10. 1 notification pour toutes ses dettes                             (ligne 199)  │
 │  11. 1 ligne relance par dette                                         (ligne 202)  │
 │  12. mettreEmailEnAttente() : envoi_notification au statut EN_ATTENTE  (ligne 243)  │
 └───────────────────────────────────────────┬─────────────────────────────────────────┘
                                             │ écrit en base
                                             ▼
   ┌────────────────────────────────────────────────────────┐
   │ notification       (1 par contribuable)                │
   │ relance            (1 par dette)                       │
   │ envoi_notification (1 par canal, statut EN_ATTENTE)    │
   │ dettes             (si constatée)                      │
   └───────────────────────────┬────────────────────────────┘
                               │ toutes les 30 s (relance.email.intervalle-expedition-secondes)
                               ▼
 ┌─────────────────────────────────────────────────────────────────────────────────────┐
 │ RelanceScheduler.expedierEmails()  (ligne 34-37)                                    │
 │   -> ExpediteurEmails.expedier()   (ligne 55)                                       │
 │      prend au plus 20 envois EN_ATTENTE (relance.email.envois-par-passage)          │
 │      ModeleEmailRelance (titre / html) + EmailService.envoyerHtml() -> SMTP Gmail   │
 │      statut -> ENVOYE ou ECHEC (+ message d'erreur), date_envoi = heure réelle      │
 └─────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Pourquoi une vue, des lots et une file d'attente (gros volumes)

L'ancienne version parcourait **tous** les contribuables et recalculait le tableau de bord de chacun : plusieurs requêtes par contribuable, même s'il était à jour. Avec des centaines de milliers de contribuables, cela fait des millions de requêtes, dans une seule transaction, avec l'envoi SMTP au milieu (environ 1 seconde par email). La nouvelle version règle ces trois problèmes :

| Problème | Solution | Où |
|---|---|---|
| On parcourait tous les contribuables | La **vue `v_obligations_a_relancer`** ne renvoie que les déclarations non soldées dont la date limite est dépassée. La base fait le tri grâce aux index | `data.sql` lignes 167-198 |
| Plusieurs requêtes par contribuable (N+1) | Par lot, **3 requêtes groupées** (`IN (...)`) chargent déclarations, dettes et contribuables. Le nombre de relances et l'anti-spam viennent de la vue | `RelanceService.traiterLot()` |
| Une seule transaction géante | **Une transaction par lot** de 500 contribuables. Une erreur n'annule que son lot ; les autres sont relancés et le bilan indique `lotsEnErreur` | `RelanceService.lancerCampagne()` ligne 127 |
| Emails envoyés pendant la transaction | La campagne met les emails **EN_ATTENTE** ; l'**expéditeur** les envoie ensuite par paquets | `ExpediteurEmails.java` |

**Ce que contient la vue :** `id_declaration`, `id_contribuables`, `id_dettes` (vide si la dette n'est pas encore constatée), `statut_dette`, `date_limite_paiement`, `jours_retard`, `derniere_relance`, `nb_relances`.

**Ce que la vue ne contient pas :** les montants. La formule des pénalités (1 % par mois entamé, paiements imputés d'abord sur les pénalités) n'existe qu'à un seul endroit, `service/contribuable/CalculEcheance.java`, utilisé à la fois par le tableau de bord contribuable et par la relance. Si la formule était aussi écrite en SQL, il faudrait penser à changer les deux le jour où le taux change.

**Vue simple, pas matérialisée :** une vue simple n'est qu'une requête enregistrée, toujours à jour. Une vue matérialisée est une copie qu'il faut rafraîchir ; elle pourrait annoncer un retard déjà payé.

**La consulter toi-même :**
```
docker exec -it ssif-db psql -U dgi_user -d recouvrement_db
SELECT * FROM v_obligations_a_relancer;
```

### 2.3 Lecture par le contribuable

```
 Connexion contribuable
        │
        ▼
 ContribuableLayout.vue (ligne 96) ── onMounted ──► stores/notifications.js chargerNonLues()
        │                                              │ GET /api/contribuable/notifications/{nif}/non-lues
        │                                              ▼
        │                                ContribuableController (ligne 58) -> NotificationService.compterNonLues()
        │                                              │
        │◄──────────── badge rouge "3" (ligne 63) ─────┘
        ▼
 NotificationsView.vue charger() (ligne 116)
        │ GET /api/contribuable/notifications/{nif}
        ▼
 ContribuableController (ligne 52) -> NotificationService.getNotificationsByNif() -> versDTO() (ligne 70)
        │  (notification + ses relances + ses envois)
        ▼
 Le contribuable clique sur une notification : ouvrir() (ligne 150)
        │ PUT /api/contribuable/notifications/{id}/lue
        ▼
 ContribuableController (ligne 63) -> NotificationService.marquerLue() (ligne 51)
        │  est_lue = true, date_lecture = maintenant (seulement la 1re fois)
        ▼
 Le badge baisse ; côté DGI le journal affiche "Lue le …"
```

### 2.4 Consultation par la DGI

```
 Menu "Relances automatiques" (DgiLayout.vue ligne 48) -> route /dgi/relances (router/index.js ligne 17)
        │
        ▼
 JournalRelancesView.vue charger() (ligne 277) ── GET /api/dgi/relances
        ▼
 RelanceController.getJournal() (ligne 21) -> RelanceService.getJournal() (ligne 227)
        │  + configuration() (ligne 256) : lit relance.properties, calcule la prochaine exécution
        │  + decrirePlanification() (ligne 282) : "0 0 8 * * SAT" -> "Chaque samedi à 08:00"
        ▼
 Affiche : planification, critères, compte email, chiffres clés, journal (email envoyé ? lue ?)
```

---

## 3. Changer la date de relance (sans toucher au code ni à la base)

**Fichier :** `PROJET_DGI/config/relance.properties`

| Ligne | Paramètre | Valeur normale | Rôle |
|---|---|---|---|
| 14 | `relance.active` | `true` | `false` coupe la relance planifiée (le bouton DGI marche toujours) |
| **24** | **`relance.planification`** | **`0 0 8 * * SAT`** | **Quand relancer** |
| 25 | `relance.fuseau-horaire` | `Indian/Antananarivo` | Heure de Madagascar |
| 31 | `relance.jours-retard-minimum` | `1` | Retard minimum avant relance |
| 33 | `relance.montant-minimum` | `0` | Reste dû minimum (Ar) |
| 35 | `relance.delai-entre-deux-relances-jours` | `7` | Anti-spam : pas deux relances de la même dette dans ce délai (`0` seulement pour les tests) |
| 37 | `relance.inclure-echeanciers` | `true` | Relancer aussi les échéanciers en retard |
| 44 | `relance.mise-en-demeure-apres` | `2` | Après 2 relances, on passe en mise en demeure |
| 51 | `relance.taille-lot` | `500` | Nombre de contribuables traités par transaction (voir 2.2) |
| 60 | `relance.email.actif` | `true` | Envoyer l'email ou non. Les destinataires viennent de la base (`contribuables.email`), jamais de ce fichier |
| 62 | `relance.email.lien-plateforme` | `http://localhost:3000/…` | Lien du bouton dans l'email |
| 66 | `relance.email.envois-par-passage` | `20` | Emails envoyés au maximum à chaque passage de l'expéditeur |
| 67 | `relance.email.intervalle-expedition-secondes` | `30` | Temps entre deux passages de l'expéditeur |

**Débit des emails :** 20 emails toutes les 30 secondes font 2 400 emails par heure au maximum. Un compte Gmail personnel est limité à environ 500 emails par jour : au-delà, Gmail refuse et l'envoi passe en ECHEC. En production, il faudra le serveur mail de la DGI.

**Format de la planification :** `seconde minute heure jour mois jour-de-la-semaine`

| Je veux… | J'écris ligne 24 |
|---|---|
| Chaque samedi à 8h | `relance.planification=0 0 8 * * SAT` |
| Le 22 de chaque mois à 8h | `relance.planification=0 0 8 22 * *` |
| Le 22, tous les 3 mois (janv., avril, juil., oct.) | `relance.planification=0 0 8 22 1/3 *` |
| Lundi et jeudi à 9h30 | `relance.planification=0 30 9 * * MON,THU` |
| Chaque jour à 8h | `relance.planification=0 0 8 * * *` |

**Puis :** `docker restart ssif-backend`. ⚠️ `docker compose up -d --build` **ne suffit pas** : si le code n'a pas changé, Docker laisse tourner le conteneur (« Running ») et le fichier n'est pas relu. Pas de rebuild : le dossier `config` est monté dans le conteneur (`docker-compose.yml` ligne 79). La page DGI affiche aussitôt la nouvelle planification en français et la prochaine date. C'est testé : avec `0 0 8 22 1/3 *`, la page a affiché « Le 22, tous les 3 mois à 08:00 », prochaine exécution le 22/01/2027.

⚠️ Pas de commentaire en fin de ligne : `relance.active=true # oui` ferait lire la valeur `true # oui`. Un commentaire va toujours sur sa propre ligne.

**Le mot de passe de l'email n'est PAS dans ce fichier.** Il est dans `PROJET_DGI/.env`, lignes 6-7 (`MAIL_USERNAME`, `MAIL_PASSWORD`). Ce fichier est ignoré par git (`.gitignore`). Après l'avoir modifié, lancer `docker compose up -d backend` : un simple restart ne relit pas `.env`.

---

## 4. Question : la dette, c'est quoi exactement ?

### 4.1 La règle

```
 Montant déclaré (principal)            ← ne change pas : c'est l'impôt déclaré
 + Pénalités de retard                  ← AUGMENTENT : 1 % du principal par mois de retard entamé
 − Paiements déjà faits                 ← imputés d'abord sur les pénalités, puis sur le principal
 ─────────────────────────────────────
 = Reste à payer  (ce que le contribuable doit aujourd'hui)
```

Donc :
- **la partie qui augmente**, ce sont les **pénalités** ;
- **ce qui devrait être payé**, c'est le **principal** ;
- **le total réclamé** dans la relance, c'est le **reste à payer** = principal restant + pénalités restantes.

C'est calculé dans `DashboardService.calculerEcheance()` (ligne 146) : nombre de mois entamés ligne 173, pénalités ligne 174, imputation des paiements ligne 176.

### 4.2 Deux exemples réels (calculés le 09/11/2026)

**Harimalala, IS 2024 :** date limite 15/05/2025, 543 jours de retard, soit 19 mois entamés.

| | Montant |
|---|---|
| Principal déclaré | 45 000 000 Ar |
| Pénalités : 45 000 000 × 1 % × 19 mois | + 8 550 000 Ar |
| Déjà payé | − 0 Ar |
| **Reste à payer = montant de la relance** | **53 550 000 Ar** |

Le mois prochain, les pénalités passent à 20 mois (9 000 000 Ar) et le total à 54 000 000 Ar.

**Carene, ISYN 2025 :** en échéancier, date limite 31/03/2026, 223 jours de retard, soit 8 mois entamés.

| | Montant |
|---|---|
| Principal déclaré | 3 600 000 Ar |
| Pénalités : 3 600 000 × 1 % × 8 mois | + 288 000 Ar |
| Déjà payé : 1 000 000 Ar, imputé d'abord sur les pénalités | − 288 000 Ar sur les pénalités, − 712 000 Ar sur le principal |
| **Reste à payer** | **2 888 000 Ar** (principal restant, pénalités soldées) |

### 4.3 Dans la base de données

| Table / colonne | Contenu |
|---|---|
| `declaration.montant_principal` | Le montant déclaré (ne change pas) |
| `dettes.montant_initial` | Le principal au moment où la dette est constatée |
| `dettes.montant_penalites` | Les pénalités **enregistrées** |
| `dettes.montant_paye` | Ce qui a été payé |
| `dettes.solde_restant` | `montant_initial + montant_penalites − montant_paye` |
| `relance.montant_du` | **Photographie** du reste à payer **le jour de la relance** (preuve de ce qui a été réclamé) |

⚠️ **Point à connaître (limite actuelle)** : `dettes.montant_penalites` et `dettes.solde_restant` ne sont **pas mis à jour chaque mois**. Ce sont les valeurs saisies dans `data.sql`. Pour Harimalala, la base indique 6 750 000 Ar de pénalités, alors que le calcul du jour donne 8 550 000 Ar.

Le tableau de bord, l'historique et les relances **n'utilisent pas** ces deux colonnes : ils recalculent toujours en direct. Les chiffres affichés sont donc justes. Pour que la base reste à jour aussi, il faudrait une tâche planifiée qui réécrit ces colonnes chaque mois, comme la relance. C'est à décider ensemble.

---

## 5. Tous les fichiers

### 5.1 Fichiers créés

**Configuration**

| Fichier | Ce qu'il fait |
|---|---|
| `config/relance.properties` | Tous les réglages de la relance (voir section 3) |
| `.gitignore` | Empêche `.env` (mot de passe) d'aller sur GitHub |
| `Backend/…/config/RelanceProperties.java` | Lit les clés `relance.*` du fichier ; **aucune valeur par défaut dans le code** : si une clé manque dans le fichier, le backend refuse de démarrer et nomme la clé manquante (`APPLICATION FAILED TO START … Property: relance.delaiEntreDeuxRelancesJours`, testé) |
| `Backend/…/config/RelanceScheduler.java` | Les deux réveils. Lignes 34-37 : `expedierEmails()`, toutes les `relance.email.intervalle-expedition-secondes`, appelle l'expéditeur. Lignes 40-47 : `@Scheduled(cron = "${relance.planification}", zone = "${relance.fuseau-horaire}")`, valeurs lues dans le fichier, sans valeur de secours ; `relanceAutomatique()` appelle `lancerCampagne("AUTOMATIQUE")` si `relance.active=true`. Les deux réveils partagent le même fil : ils ne tournent jamais en même temps |

**Tables (entités JPA)**

| Fichier | Table | Contenu |
|---|---|---|
| `entity/Relance.java` | `relance` | date_relance, jours_retard, montant_du, → types_relance, → dettes, → notification |
| `entity/Notification.java` | `notification` | titre, message, date_envoi, **est_lue**, **date_lecture**, → types_notification, → contribuables |
| `entity/EnvoiNotification.java` | `envoi_notification` | canal (EMAIL/SMS), destinataire, date_envoi, message_erreur, → statut_envoi, → notification |
| `entity/TypeRelance.java` | `types_relance` | RELANCE_1, RELANCE_2, MISE_EN_DEMEURE |
| `entity/TypeNotification.java` | `types_notification` | RELANCE, RAPPEL_ECHEANCE, PAIEMENT_VALIDE, INFORMATION |
| `entity/StatutEnvoi.java` | `statut_envoi` | EN_ATTENTE, ENVOYE, DELIVRE, ECHEC |

**Repositories :** `RelanceRepository`, `NotificationRepository`, `EnvoiNotificationRepository`, `TypeRelanceRepository`, `TypeNotificationRepository` et `StatutEnvoiRepository`. Ce sont des requêtes Spring Data dérivées du nom des méthodes. `EnvoiNotificationRepository.findByCanalAndStatutEnvoiLibelleOrderByIdAsc()` donne la file d'attente de l'expéditeur, par paquet.

**Lecture de la vue SQL**

| Fichier | Rôle |
|---|---|
| `repository/DeclarationRepository.java` | Ligne 27 : `findIdsContribuablesARelancer()`. Ligne 39 : `findObligationsARelancer(ids, limiteAntiSpam)`. Seules requêtes SQL écrites à la main (`nativeQuery`), car une vue n'est pas une entité JPA |
| `repository/ObligationARelancer.java` | Une ligne de la vue telle que Java la reçoit : déclaration, contribuable, dette, nombre de relances, « relancée récemment ? » |

Pourquoi la vue n'est pas une entité JPA : avec `ddl-auto=update`, Hibernate créerait une **table** du même nom au démarrage, avant `data.sql`, et la création de la vue échouerait.

**Services**

| Fichier | Fonction | Ligne | Rôle |
|---|---|---|---|
| `service/dgi/RelanceService.java` | `lancerCampagne()` | 113 | Le moteur : contribuables en retard (vue), découpage en lots, une transaction par lot, bilan |
| | `traiterLot()` | 143 | Un lot : obligations de la vue, 3 requêtes groupées, calcul, critères, anti-spam |
| | `relancer()` | 175 | Un contribuable : relances, notification, email en file d'attente |
| | `meriteRelance()` | 209 | En retard ? Assez de jours ? Assez de montant ? |
| | `niveauSuivant()` | 219 | 0 relance → 1re ; 1 → 2e ; ≥ 2 → mise en demeure |
| | `constaterDette()` | 228 | Crée la ligne `dettes` si elle n'existe pas encore |
| | `mettreEmailEnAttente()` | 243 | Crée l'`envoi_notification` EN_ATTENTE vers `contribuables.email` |
| | `getJournal()` | 259 | Données de la page DGI |
| | `configuration()` | 288 | Réglages et prochaine exécution |
| | `decrirePlanification()` | 314 | Traduit le cron en français |
| `service/dgi/ExpediteurEmails.java` | `expedier()` | 55 | Prend un paquet d'envois EN_ATTENTE, envoie, passe chacun en ENVOYE ou ECHEC |
| | `envoyer()` | 83 | Un email ; compte configuré ? niveau et total recalculés depuis les relances de la notification |
| `service/contribuable/CalculEcheance.java` | `calculer()` | 27 | **Seule** formule des pénalités et du reste à payer, partagée par le tableau de bord et la relance |
| `service/dgi/ModeleEmailRelance.java` | `titre()` / `message()` / `sujet()` / `html()` | 20 / 29 / 44 / 48 | Textes de la relance selon le niveau ; email HTML aux couleurs DGI |
| `service/EmailService.java` | `estConfigure()` / `envoyerHtml()` | 26 / 34 | Envoi SMTP Gmail |
| `service/contribuable/NotificationService.java` | `getNotificationsByNif()` | 41 | Liste des notifications |
| | `compterNonLues()` | 45 | Badge |
| | `marquerLue()` | 51 | Lecture (date conservée à la 1re ouverture) |
| | `toutMarquerLu()` | 61 | Bouton « Tout marquer comme lu » |
| | `versDTO()` | 70 | Assemble notification + relances + envois (utilisé aussi par la DGI) |
| `util/LibellesFiscaux.java` | `periode()` / `montant()` / `date()` | | « M12 » → « Décembre 2025 », « 12 500 000 Ar » |

**DTO et contrôleur**

| Fichier | Rôle |
|---|---|
| `dto/NotificationDTO.java` | Ce que reçoivent les deux fronts |
| `dto/dgi/JournalRelancesDTO.java` | Journal DGI |
| `dto/dgi/ResultatCampagneDTO.java` | Bilan de « Lancer maintenant » : contribuables relancés, relances créées, ignorées (délai), emails en file d'attente, lots en erreur |
| `controller/dgi/RelanceController.java` | Ligne 21 : `GET /api/dgi/relances`. Ligne 27 : `POST /api/dgi/relances/lancer` |

**Front**

| Fichier | Rôle |
|---|---|
| Front DGI `views/relances/JournalRelancesView.vue` | Page « Relances automatiques ». `charger()` ligne 277, `lancerMaintenant()` ligne 290 |
| Front contribuable `stores/notifications.js` | Nombre de non lues, partagé entre le menu et la page |
| `docs/explication_relances_automatiques.md` | Ce document |

### 5.2 Fichiers modifiés

| Fichier | Lignes | Modification |
|---|---|---|
| `Backend/pom.xml` | 46-50 | Dépendance `spring-boot-starter-mail` |
| `Backend/…/application.properties` | 42 | `spring.config.import` : charge `config/relance.properties` |
| | 46-54 | Serveur SMTP Gmail ; identifiants lus depuis `.env` |
| `docker-compose.yml` | 71 | Fuseau horaire de Madagascar pour le backend |
| | 74-75 | Passe `MAIL_USERNAME` / `MAIL_PASSWORD` depuis `.env` |
| | 79 | Monte `./config` dans le conteneur |
| `.env` | 6-7 | `MAIL_USERNAME=bakomalalafenitra@gmail.com`, `MAIL_PASSWORD=` (**à remplir par toi**) |
| `data.sql` | 54+ | Contribuables de test renommés avec les vrais emails et téléphones ; Diana (50) et Njara (51) ajoutés |
| | 79 | TRUNCATE inclut `envoi_notification`, `relance`, `notification` |
| | 144-166 | Valeurs de `types_relance`, `statut_envoi`, `types_notification` |
| | 167-193 | **Vue `v_obligations_a_relancer`** (`CREATE OR REPLACE VIEW`, recréée à chaque démarrage) |
| | 195-198 | **Index** : `declaration(date_limite_paiement)`, `declaration(id_contribuables)`, `relance(id_dettes, date_relance)`, `envoi_notification(id_statut_envoi, canal)` |
| | fin | `setval` des nouvelles séquences |
| `service/contribuable/DashboardService.java` | | La formule des pénalités est sortie dans `CalculEcheance` ; le tableau de bord l'appelle. Montants inchangés (vérifié : Rochel 10 670 000 Ar en retard des deux côtés) |
| `config/RelanceProperties.java` | | Nouvelles clés `taille-lot`, `email.envois-par-passage`, `email.intervalle-expedition-secondes` (obligatoires, sans valeur par défaut) |
| Front DGI `views/relances/JournalRelancesView.vue` | 23-38 | Bilan : « N email(s) en file d'attente » et lots en erreur, au lieu de « N envoyé(s) » |
| `controller/contribuable/ContribuableController.java` | 52 | `GET /notifications/{nif}` |
| | 58 | `GET /notifications/{nif}/non-lues` |
| | 63 | `PUT /notifications/{id}/lue` |
| | 69 | `PUT /notifications/{nif}/tout-lire` |
| Front contribuable `views/contribuable/NotificationsView.vue` | | **Même design**, données réelles. `charger()` l. 116, `ouvrir()` l. 150 (marque lue), `toutMarquerLu()` l. 167, détail des obligations dépliable |
| Front contribuable `layouts/ContribuableLayout.vue` | 63, 96 | Badge réel (au lieu du « 2 » écrit en dur) |
| Front DGI `layouts/DgiLayout.vue` | 48 | Lien « Relances automatiques » pour tous les rôles ; liens « Proposer une relance » et « Validations Relances » retirés |
| Front DGI `router/index.js` | 5, 17 | Route `/dgi/relances` |

### 5.3 Fichiers supprimés

Ces pages correspondaient à l'ancienne idée d'une relance validée par un agent :
- `ValidationRelancesView.vue` : maquette avec des données inventées ;
- `ProposerRelanceView.vue` : fichier vide ;
- `RelanceCard.vue` : fichier vide.

---

## 6. Changements à reporter dans le MCD Looping

```
relance
  - date_proposition, date_validation, motif_rejet   (supprimés : plus de validation par un agent)
  - id_statut_envoi                                  (déplacé dans envoi_notification)
  + date_relance, jours_retard, montant_du, id_notification

notification
  + est_lue, date_lecture, id_types_notification

envoi_notification   (nouvelle)   id, canal, destinataire, date_envoi, message_erreur,
                                  id_statut_envoi, id_notification
types_notification   (nouvelle)   id, code, libelle

v_obligations_a_relancer   (VUE, pas une table : à mettre dans le MLD, pas dans le MCD)
  id_declaration, id_contribuables, id_dettes, statut_dette, date_limite_paiement,
  jours_retard, derniere_relance, nb_relances
  source : declaration ⟕ dettes ⟕ relance (agrégée par dette)

index : declaration(date_limite_paiement), declaration(id_contribuables),
        relance(id_dettes, date_relance), envoi_notification(id_statut_envoi, canal)
```

**Pourquoi ces choix :**
- **Une notification pour plusieurs relances :** Harimalala a 6 dettes en retard ; il reçoit **un** email récapitulatif, pas 6.
- **Le statut d'envoi est dans `envoi_notification`, pas dans `relance` :** une même notification partira par email **et** par SMS. Chaque canal a son propre statut : l'email peut partir alors que le SMS échoue.
- **`montant_du` et `jours_retard` dans `relance` :** c'est une preuve de ce qui a été réclamé ce jour-là, même si le solde change ensuite.

---

## 7. Ce qui a été testé (09/11/2026)

| Test | Résultat |
|---|---|
| Lancer maintenant | 3 contribuables, 10 obligations relancées |
| Montant Rochel dans la relance comparé à son tableau de bord | 10 670 000 Ar des deux côtés |
| 2e lancement juste après | 0 relance, 10 ignorées (anti-spam 7 jours) |
| Escalade (anti-spam mis à 0 pour le test) | Carene : Relance → Deuxième relance → Mise en demeure |
| Changement de planification dans le fichier + restart | Affiché « Le 22, tous les 3 mois à 08:00 » |
| Ouvrir une notification | `est_lue = true`, badge 1 → 0 |
| Email sans mot de passe dans `.env` | Noté « Échec : compte d'envoi non configuré », aucun email parti |

**Testé le 10/11/2026, après le passage à la vue et aux lots :**

| Test | Résultat |
|---|---|
| `SELECT * FROM v_obligations_a_relancer` | 10 obligations (Rochel 3, Harimalala 6, Carene 1), les mêmes qu'avant |
| Lancer maintenant | 3 contribuables, 10 relances, 3 emails EN_ATTENTE, 0 lot en erreur |
| Expéditeur | Moins d'une minute après, les 3 envois sont ENVOYE (log « Expédition des relances : 3 email(s) envoyé(s) / 0 échec(s) ») |
| Montants | Rochel 10 670 000 Ar, Harimalala 146 785 000 Ar, Carene 2 888 000 Ar : identiques à avant ; tableau de bord de Rochel inchangé |

**Non testé :**
- un vrai gros volume (il n'y a que 5 contribuables de test) ;
- l'affichage des pages dans le navigateur.

---

## 8. Pour tester toi-même

1. Remplir `MAIL_PASSWORD` dans `.env` (mot de passe d'application Google, voir section 3), puis lancer `docker compose up -d backend`.
2. Front DGI http://localhost:3001 → `agent.dgi@impots.mg` / `password123` → **Relances automatiques** → l'encadré « Envoi par email » doit afficher l'expéditeur en vert.
3. Cliquer sur **Lancer la relance maintenant**. Le bilan indique « 3 email(s) en file d'attente ».
4. Attendre environ 30 secondes, recharger la page : les envois passent à « Envoyé ». Vérifier les boîtes mail de Rochel, Harimalala et Carene (et les Spams).
5. Front contribuable http://localhost:3000 → `rochelrandriamanantena@gmail.com` / `password123` → badge rouge sur Notifications → ouvrir la notification.
6. Revenir sur la page DGI → la ligne de Rochel affiche « Lue le … ».

**Suivre les logs en direct :** `docker logs -f ssif-backend 2>&1 | findstr /i "Relance Expédition"` (Ctrl+C pour quitter). Une campagne écrit « Relance AUTOMATIQUE : … » ou « Relance MANUEL : … », l'expéditeur écrit « Expédition des relances : … ».

**Rappel :** chaque redémarrage du backend remet les relances et notifications à zéro, à cause de `data.sql`.

---

## 9. Limites et suite

- **SMS** : à brancher comme 2e canal dans `envoi_notification`, avec la passerelle Android (téléphone émetteur 0383817421).
- **Pénalités en base** non actualisées chaque mois (voir 4.3).
- **Sécurité** : endpoints encore en `permitAll`, comme le reste de l'application.
- **Tableau de bord DGI** : la carte « Relances en attente » est encore une maquette. Elle pourra lire la vue `v_obligations_a_relancer`.
- **Journal DGI** : `getJournal()` charge encore **tout** l'historique des relances. Avec de gros volumes, il faudra une pagination (page DGI comprise).
- **Envois en ECHEC** : ils ne sont pas retentés automatiquement. Il faudrait un compteur de tentatives dans `envoi_notification`.
- **Gmail** : limite d'environ 500 emails par jour pour un compte personnel (voir section 3).
