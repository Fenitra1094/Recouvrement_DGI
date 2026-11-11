<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import ContribuableLayout from '@/layouts/ContribuableLayout.vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { formatDate, formatMontant, impot, libellePeriode, typePeriode } from '@/utils/fiscal'

const auth = useAuthStore()

const resume = ref(null)
const chargement = ref(false)
const erreur = ref('')

// Libellé, ton visuel et icône de chaque statut renvoyé par l'API
const STATUTS = {
  A_JOUR:                { libelle: 'À jour',                   ton: 'ok',     icone: 'bi-shield-check' },
  A_PAYER:               { libelle: 'À échoir',                 ton: 'neutre', icone: 'bi-clock' },
  PAYEE:                 { libelle: 'Réglée',                   ton: 'neutre', icone: 'bi-check2' },
  ECHEANCE_PROCHE:       { libelle: 'Échéance proche',          ton: 'alerte', icone: 'bi-hourglass-split' },
  EN_ECHEANCIER:         { libelle: 'En échéancier',            ton: 'alerte', icone: 'bi-calendar2-range' },
  EN_ATTENTE_TRAITEMENT: { libelle: 'En attente de traitement', ton: 'alerte', icone: 'bi-inbox' },
  EN_RETARD:             { libelle: 'En retard',                ton: 'danger', icone: 'bi-exclamation-octagon' },
}
const TON_NIVEAU = { ROUGE: 'danger', ORANGE: 'alerte', VERT: 'ok' }

// Message principal du bandeau selon le statut global du dossier
const BANDEAUX = {
  A_JOUR:                { titre: 'Votre dossier est à jour', texte: 'Aucune obligation fiscale en retard. Continuez à respecter vos dates limites.' },
  ECHEANCE_PROCHE:       { titre: 'Une échéance approche', texte: 'Une ou plusieurs obligations arrivent à leur date limite dans les prochains jours.' },
  EN_ECHEANCIER:         { titre: 'Dossier en échéancier', texte: 'Un plan de paiement est en cours. Deux mensualités manquées entraînent la rupture de l\'échéancier.' },
  EN_ATTENTE_TRAITEMENT: { titre: 'Demande en cours d\'examen', texte: 'Votre demande a été transmise à votre centre fiscal et attend une décision.' },
  EN_RETARD:             { titre: 'Dossier en retard', texte: 'Des obligations ont dépassé leur date limite. La pénalité de retard augmente de 1 % du principal chaque mois.' },
}

const statut = (code) => STATUTS[code] || { libelle: code, ton: 'neutre', icone: 'bi-question-circle' }

const tonGlobal = computed(() => statut(resume.value?.statutGlobal).ton)
const bandeau = computed(() => BANDEAUX[resume.value?.statutGlobal] || BANDEAUX.A_JOUR)
const echeancesOuvertes = computed(() => (resume.value?.echeances || []).filter((e) => e.statut !== 'PAYEE'))
const majorations = computed(() => Number(resume.value?.soldePenalites || 0))
// Alertes à afficher dans le bandeau (le message "à jour" est déjà le titre du bandeau)
const alertesBandeau = computed(() => (resume.value?.alertes || []).filter((a) => a.niveau !== 'VERT'))

// ---------- Filtres des obligations ----------
const SITUATIONS = [
  { cle: 'NON_REGLEES', libelle: 'Non réglées', test: (e) => e.statut !== 'PAYEE' },
  { cle: 'EN_RETARD',   libelle: 'En retard',   test: (e) => e.statut === 'EN_RETARD' },
  { cle: 'REGLEES',     libelle: 'Réglées',     test: (e) => e.statut === 'PAYEE' },
  { cle: 'TOUTES',      libelle: 'Toutes',      test: () => true },
]
const FILTRES_DEFAUT = { situation: 'NON_REGLEES', impot: '', annee: '', du: '', au: '' }
const filtres = reactive({ ...FILTRES_DEFAUT })

const toutesEcheances = computed(() => resume.value?.echeances || [])
const impotsDisponibles = computed(() => [...new Set(toutesEcheances.value.map((e) => e.impot))].sort())
const anneesDisponibles = computed(() =>
  [...new Set(toutesEcheances.value.map((e) => e.annee).filter(Boolean))].sort((a, b) => b - a)
)
const compteSituation = (cle) => toutesEcheances.value.filter(SITUATIONS.find((s) => s.cle === cle).test).length

const echeancesFiltrees = computed(() => {
  const test = SITUATIONS.find((s) => s.cle === filtres.situation).test
  return toutesEcheances.value.filter((e) =>
    test(e)
    && (!filtres.impot || e.impot === filtres.impot)
    && (!filtres.annee || e.annee === Number(filtres.annee))
    && (!filtres.du || (e.dateLimite && e.dateLimite >= filtres.du))
    && (!filtres.au || (e.dateLimite && e.dateLimite <= filtres.au))
  )
})
const filtresActifs = computed(() =>
  Object.keys(FILTRES_DEFAUT).some((k) => k !== 'situation' && filtres[k])
)
const totauxFiltres = computed(() => {
  const somme = (champ) => echeancesFiltrees.value.reduce((t, e) => t + Number(e[champ] || 0), 0)
  return { initial: somme('montantInitial'), penalites: somme('penalites'), paye: somme('montantPaye'), reste: somme('resteAPayer') }
})
function reinitialiserFiltres() {
  Object.assign(filtres, FILTRES_DEFAUT)
}
// Part déjà payée sur le total exigible (principal + pénalités)
function pctPaye(e) {
  if (e.statut === 'PAYEE') return 100
  const paye = Number(e.montantPaye || 0)
  const total = paye + Number(e.resteAPayer || 0)
  return total ? Math.round((paye / total) * 100) : 0
}

// Hausse de la dette au prochain mois entamé si rien n'est réglé : 1 % du principal restant de chaque obligation en retard
const hausseMoisProchain = computed(() =>
  (resume.value?.echeances || [])
    .filter((e) => e.statut === 'EN_RETARD')
    .reduce((t, e) => t + Number(e.montantPrincipal || 0) * 0.01, 0)
)

const prochaineEcheance = computed(() => {
  const today = cleDate(new Date())
  return (resume.value?.echeances || []).find((e) => e.statut !== 'PAYEE' && e.dateLimite >= today) || null
})

// Répartition du solde pour la barre de composition
const repartition = computed(() => {
  const total = Number(resume.value?.soldeTotal || 0)
  if (!total) return []
  return [
    { cle: 'principal', libelle: 'Principal', valeur: Number(resume.value.soldePrincipal) },
    { cle: 'penalites', libelle: 'Pénalités de retard', valeur: Number(resume.value.soldePenalites) },
  ].map((r) => ({ ...r, pct: (r.valeur / total) * 100 }))
})

const dateDuJour = new Date().toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })

onMounted(chargerDashboard)

async function chargerDashboard() {
  if (!auth.contribuableNif) return
  chargement.value = true
  erreur.value = ''
  try {
    const { data } = await api.get(`/contribuable/dashboard-summary/${encodeURIComponent(auth.contribuableNif)}`)
    resume.value = data
  } catch (e) {
    erreur.value = 'Impossible de charger votre tableau de bord. Vérifiez que le serveur est démarré.'
  } finally {
    chargement.value = false
  }
}

// ---------- Calendrier des échéances ----------
const JOURS = ['L', 'M', 'M', 'J', 'V', 'S', 'D']
const aujourdHui = new Date()
const moisAffiche = ref(new Date(aujourdHui.getFullYear(), aujourdHui.getMonth(), 1))

const titreMois = computed(() =>
  moisAffiche.value.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })
)

function cleDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const echeancesParJour = computed(() => {
  const map = {}
  for (const e of resume.value?.echeances || []) {
    if (!e.dateLimite) continue
    ;(map[e.dateLimite] ||= []).push(e)
  }
  return map
})

// Grille de 6 semaines commençant un lundi
const cases = computed(() => {
  const premier = moisAffiche.value
  const decalage = (premier.getDay() + 6) % 7
  const debut = new Date(premier.getFullYear(), premier.getMonth(), 1 - decalage)
  return Array.from({ length: 42 }, (_, i) => {
    const d = new Date(debut.getFullYear(), debut.getMonth(), debut.getDate() + i)
    const cle = cleDate(d)
    const echeances = echeancesParJour.value[cle] || []
    return {
      cle,
      jour: d.getDate(),
      horsMois: d.getMonth() !== premier.getMonth(),
      estAujourdHui: cle === cleDate(aujourdHui),
      echeances,
      ton: tonLePlusGrave(echeances),
    }
  })
})

// Échéances du mois affiché, listées sous le calendrier
const echeancesDuMois = computed(() => {
  const m = moisAffiche.value
  const prefixe = `${m.getFullYear()}-${String(m.getMonth() + 1).padStart(2, '0')}`
  return (resume.value?.echeances || []).filter((e) => e.dateLimite?.startsWith(prefixe))
})

function changerMois(delta) {
  const m = moisAffiche.value
  moisAffiche.value = new Date(m.getFullYear(), m.getMonth() + delta, 1)
}

function tonLePlusGrave(echeances) {
  const ordre = ['danger', 'alerte', 'ok', 'neutre']
  return echeances.map((e) => statut(e.statut).ton).sort((a, b) => ordre.indexOf(a) - ordre.indexOf(b))[0]
}

function infobulle(echeances) {
  return echeances.map((e) => `${e.impot} ${libellePeriode(e)} — ${statut(e.statut).libelle}`).join('\n')
}

function joursAvant(iso) {
  const diff = Math.round((new Date(iso) - new Date(cleDate(aujourdHui))) / 86400000)
  return diff === 0 ? "aujourd'hui" : `dans ${diff} jour${diff > 1 ? 's' : ''}`
}
</script>

<template>
  <ContribuableLayout>
    <div class="tdb">

      <div v-if="erreur" class="message-erreur">
        <i class="bi bi-wifi-off"></i> {{ erreur }}
      </div>

      <div v-if="chargement && !resume" class="chargement">
        <span class="spinner-border spinner-border-sm"></span> Chargement de votre situation…
      </div>

      <template v-if="resume">

        <!-- ===== En-tête : identité + solde (même univers visuel que la page de connexion) ===== -->
        <section :class="['hero', `ton-${tonGlobal}`]">
          <div class="hero-motif"></div>
          <div class="hero-glow"></div>

          <div class="hero-gauche">
            <span class="eyebrow">ESPACE CONTRIBUABLE · {{ dateDuJour.toUpperCase() }}</span>
            <h1 class="hero-titre">Bonjour, <em>{{ resume.raisonSociale }}</em></h1>
            <p class="hero-sous-titre">{{ bandeau.texte }}</p>

            <ul class="hero-chips">
              <li><i class="bi bi-person-vcard"></i> NIF {{ resume.nif }}</li>
              <li><i class="bi bi-briefcase"></i> Régime {{ resume.regimeFiscal }}</li>
              <li><i class="bi bi-geo-alt"></i> {{ resume.centreFiscal }}</li>
            </ul>

            <ul v-if="alertesBandeau.length" class="hero-alertes">
              <li v-for="(a, i) in alertesBandeau" :key="i" :class="`ton-${TON_NIVEAU[a.niveau] || 'neutre'}`">
                <span class="hero-alerte-icone">
                  <i :class="['bi', a.niveau === 'ROUGE' ? 'bi-exclamation-lg' : 'bi-clock']"></i>
                </span>
                <span>{{ a.message }}</span>
              </li>
            </ul>
          </div>

          <div class="hero-solde">
            <div class="solde-entete">
              <span>Solde restant dû</span>
              <button class="btn-actualiser" @click="chargerDashboard" :disabled="chargement" title="Actualiser">
                <i :class="['bi bi-arrow-clockwise', { tourne: chargement }]"></i>
              </button>
            </div>
            <div class="solde-valeur">{{ formatMontant(resume.soldeTotal) }} <small>Ar</small></div>
            <span class="statut-pill">
              <i :class="['bi', statut(resume.statutGlobal).icone]"></i> {{ bandeau.titre }}
            </span>

            <div v-if="repartition.length" class="solde-compo">
              <div class="solde-barre">
                <span v-for="r in repartition" :key="r.cle" :class="`seg-${r.cle}`" :style="{ width: r.pct + '%' }"></span>
              </div>
              <ul class="solde-legende">
                <li v-for="r in repartition" :key="r.cle">
                  <i :class="`puce seg-${r.cle}`"></i>{{ r.libelle }}
                  <strong>{{ formatMontant(r.valeur) }} Ar</strong>
                </li>
              </ul>
            </div>
            <p v-else class="solde-ok"><i class="bi bi-patch-check-fill"></i> Rien à régler pour le moment</p>
          </div>
        </section>

        <!-- ===== Indicateurs ===== -->
        <section class="kpis">
          <article class="kpi k-danger">
            <div class="kpi-icone"><i class="bi bi-exclamation-octagon"></i></div>
            <div class="kpi-corps">
              <span class="kpi-libelle">Obligations en retard</span>
              <span class="kpi-valeur">{{ resume.nombreDeclarationsEnRetard }}</span>
              <span class="kpi-note">sur {{ echeancesOuvertes.length }} non réglée(s)</span>
            </div>
          </article>

          <article class="kpi k-alerte">
            <div class="kpi-icone"><i class="bi bi-graph-up-arrow"></i></div>
            <div class="kpi-corps">
              <span class="kpi-libelle">Pénalités de retard</span>
              <span class="kpi-valeur">{{ formatMontant(majorations) }} <small>Ar</small></span>
              <span class="kpi-note">1 % du principal par mois</span>
            </div>
          </article>

          <article class="kpi k-violet">
            <div class="kpi-icone"><i class="bi bi-arrow-up-right-circle"></i></div>
            <div class="kpi-corps">
              <span class="kpi-libelle">Hausse le mois prochain</span>
              <span class="kpi-valeur">+ {{ formatMontant(hausseMoisProchain) }} <small>Ar</small></span>
              <span class="kpi-note">{{ hausseMoisProchain ? 'si les retards ne sont pas réglés' : 'aucun retard, aucune hausse' }}</span>
            </div>
          </article>


          <article class="kpi k-bleu">
            <div class="kpi-icone"><i class="bi bi-calendar-event"></i></div>
            <div class="kpi-corps">
              <span class="kpi-libelle">Prochaine échéance</span>
              <template v-if="prochaineEcheance">
                <span class="kpi-valeur kpi-date">
                  {{ formatDate(prochaineEcheance.dateLimite, { day: 'numeric', month: 'short' }) }}
                </span>
                <span class="kpi-note">
                  {{ prochaineEcheance.impot }} · {{ joursAvant(prochaineEcheance.dateLimite) }}
                </span>
              </template>
              <template v-else>
                <span class="kpi-valeur kpi-date">—</span>
                <span class="kpi-note">aucune échéance à venir</span>
              </template>
            </div>
          </article>
        </section>

        <div class="grille">
          <!-- ===== Obligations fiscales ===== -->
          <section class="carte">
            <header class="carte-entete">
              <div class="entete-titre">
                <span class="entete-icone"><i class="bi bi-journal-text"></i></span>
                <div>
                  <h3>Mes obligations fiscales</h3>
                  <p class="carte-sous-titre">Déclarations, montants dus et état du règlement</p>
                </div>
              </div>
              <span class="compteur">{{ echeancesFiltrees.length }} ligne(s)</span>
            </header>

            <div class="filtres">
              <div class="segments" role="tablist">
                <button v-for="s in SITUATIONS" :key="s.cle" type="button"
                        :class="['segment', { actif: filtres.situation === s.cle }]"
                        @click="filtres.situation = s.cle">
                  {{ s.libelle }} <span class="segment-nb">{{ compteSituation(s.cle) }}</span>
                </button>
              </div>
              <div class="filtres-champs">
                <label class="champ">
                  <span>Type d'impôt</span>
                  <select v-model="filtres.impot">
                    <option value="">Tous</option>
                    <option v-for="c in impotsDisponibles" :key="c" :value="c">{{ c }} — {{ impot(c).libelle }}</option>
                  </select>
                </label>
                <label class="champ">
                  <span>Année</span>
                  <select v-model="filtres.annee">
                    <option value="">Toutes</option>
                    <option v-for="a in anneesDisponibles" :key="a" :value="a">{{ a }}</option>
                  </select>
                </label>
                <label class="champ">
                  <span>Date limite du</span>
                  <input type="date" v-model="filtres.du" />
                </label>
                <label class="champ">
                  <span>au</span>
                  <input type="date" v-model="filtres.au" />
                </label>
                <button v-if="filtresActifs || filtres.situation !== 'NON_REGLEES'" type="button"
                        class="btn-lien" @click="reinitialiserFiltres">
                  <i class="bi bi-x-circle"></i> Réinitialiser
                </button>
              </div>
            </div>

            <div v-if="echeancesFiltrees.length === 0" class="vide">
              <span class="vide-icone">
                <i :class="['bi', filtres.situation === 'NON_REGLEES' && !filtresActifs ? 'bi-shield-check' : 'bi-funnel']"></i>
              </span>
              <p v-if="filtres.situation === 'NON_REGLEES' && !filtresActifs">
                Aucune obligation en attente. Votre situation est régulière.
              </p>
              <p v-else>Aucune obligation ne correspond à ces filtres.</p>
            </div>
            <div v-else class="table-responsive">
              <table class="tableau">
                <thead>
                  <tr>
                    <th>Impôt</th>
                    <th>Période</th>
                    <th>Date limite</th>
                    <th class="num">Montant déclaré</th>
                    <th class="num">Pénalités</th>
                    <th>Règlement</th>
                    <th class="num">Reste dû</th>
                    <th>Statut</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="e in echeancesFiltrees" :key="e.idDeclaration" :class="`ligne ton-${statut(e.statut).ton}`">
                    <td>
                      <div class="impot">
                        <span class="impot-pastille" :style="{ '--i': impot(e.impot).couleur }">{{ e.impot }}</span>
                        <span class="impot-nom">{{ impot(e.impot).libelle }}</span>
                      </div>
                    </td>
                    <td>
                      <span class="periode">{{ libellePeriode(e) }}</span>
                      <span class="sous-info">{{ typePeriode(e) }}</span>
                    </td>
                    <td>
                      {{ formatDate(e.dateLimite) }}
                      <span v-if="e.joursRetard > 0" class="retard">
                        <i class="bi bi-alarm"></i> {{ e.joursRetard }} j de retard
                      </span>
                    </td>
                    <td class="num">{{ formatMontant(e.montantInitial) }}</td>
                    <td :class="['num', Number(e.penalites) ? 'txt-danger' : 'attenue']">
                      {{ Number(e.penalites) ? '+ ' + formatMontant(e.penalites) : '—' }}
                    </td>
                    <td>
                      <div class="reglement">
                        <div class="reglement-barre"><span :style="{ width: pctPaye(e) + '%' }"></span></div>
                        <span class="sous-info">{{ pctPaye(e) }} % · {{ formatMontant(e.montantPaye) }} Ar</span>
                      </div>
                    </td>
                    <td class="num fort">{{ formatMontant(e.resteAPayer) }} <small>Ar</small></td>
                    <td>
                      <span :class="['etiquette', `ton-${statut(e.statut).ton}`]">
                        <i :class="['bi', statut(e.statut).icone]"></i> {{ statut(e.statut).libelle }}
                      </span>
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <td colspan="3">Total de la sélection</td>
                    <td class="num">{{ formatMontant(totauxFiltres.initial) }}</td>
                    <td class="num txt-danger">{{ totauxFiltres.penalites ? '+ ' + formatMontant(totauxFiltres.penalites) : '—' }}</td>
                    <td>{{ formatMontant(totauxFiltres.paye) }} Ar payés</td>
                    <td class="num fort">{{ formatMontant(totauxFiltres.reste) }} <small>Ar</small></td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </section>

          <aside class="col-laterale">
            <!-- ===== Calendrier ===== -->
            <section class="carte">
              <header class="carte-entete">
                <div class="entete-titre">
                  <span class="entete-icone"><i class="bi bi-calendar3"></i></span>
                  <h3>Calendrier des échéances</h3>
                </div>
              </header>
              <div class="cal-nav">
                <button @click="changerMois(-1)" aria-label="Mois précédent"><i class="bi bi-chevron-left"></i></button>
                <span>{{ titreMois }}</span>
                <button @click="changerMois(1)" aria-label="Mois suivant"><i class="bi bi-chevron-right"></i></button>
              </div>
              <div class="calendrier">
                <div v-for="(j, i) in JOURS" :key="i" class="cal-jour-nom">{{ j }}</div>
                <div v-for="c in cases" :key="c.cle"
                     :class="['cal-case', c.ton && `marque ton-${c.ton}`, { 'hors-mois': c.horsMois, 'aujourd-hui': c.estAujourdHui }]"
                     :title="c.echeances.length ? infobulle(c.echeances) : ''">
                  {{ c.jour }}
                </div>
              </div>
              <ul class="cal-legende">
                <li><i class="puce ton-danger"></i> En retard</li>
                <li><i class="puce ton-alerte"></i> Proche / en cours</li>
                <li><i class="puce ton-neutre"></i> À échoir / réglée</li>
              </ul>
              <ul v-if="echeancesDuMois.length" class="cal-liste">
                <li v-for="e in echeancesDuMois" :key="e.idDeclaration">
                  <span class="cal-tuile">
                    <strong>{{ formatDate(e.dateLimite, { day: '2-digit' }) }}</strong>
                    <small>{{ formatDate(e.dateLimite, { month: 'short' }) }}</small>
                  </span>
                  <span class="cal-liste-libelle">
                    <strong>{{ e.impot }}</strong>
                    <small>{{ libellePeriode(e) }}</small>
                  </span>
                  <span :class="['etiquette petite', `ton-${statut(e.statut).ton}`]">{{ statut(e.statut).libelle }}</span>
                </li>
              </ul>
              <p v-else class="cal-vide"><i class="bi bi-calendar-check"></i> Aucune échéance ce mois-ci.</p>
            </section>

          </aside>
        </div>
      </template>
    </div>
  </ContribuableLayout>
</template>

<style scoped>
.tdb {
  --navy: #0f2a4a;
  --encre: #1e293b;
  --gris: #64748b;
  --gris-clair: #94a3b8;
  --trait: #e6eaf0;
  --fond-doux: #f8fafc;
  --bleu: #2563eb;

  --danger: #b42318;  --danger-fond: #fef3f2;  --danger-trait: #fecdca;  --danger-vif: #f04438;
  --alerte: #b54708;  --alerte-fond: #fffaeb;  --alerte-trait: #fedf89;  --alerte-vif: #f79009;
  --ok: #067647;      --ok-fond: #ecfdf3;      --ok-trait: #abefc6;      --ok-vif: #12b76a;
  --neutre: #475467;  --neutre-fond: #f2f4f7;  --neutre-trait: #d0d5dd;  --neutre-vif: #98a2b3;

  color: var(--encre);
  padding-top: 0.25rem;
}

/* Tons communs */
.ton-danger { --c: var(--danger); --c-fond: var(--danger-fond); --c-trait: var(--danger-trait); --c-vif: var(--danger-vif); }
.ton-alerte { --c: var(--alerte); --c-fond: var(--alerte-fond); --c-trait: var(--alerte-trait); --c-vif: var(--alerte-vif); }
.ton-ok     { --c: var(--ok);     --c-fond: var(--ok-fond);     --c-trait: var(--ok-trait);     --c-vif: var(--ok-vif); }
.ton-neutre { --c: var(--neutre); --c-fond: var(--neutre-fond); --c-trait: var(--neutre-trait); --c-vif: var(--neutre-vif); }
.txt-danger { color: var(--danger); }

.message-erreur, .chargement { padding: 1rem 1.25rem; border-radius: 12px; margin-bottom: 1.5rem; }
.message-erreur { background: var(--alerte-fond); border: 1px solid var(--alerte-trait); color: var(--alerte); }
.chargement { text-align: center; color: var(--gris); }
.tourne { display: inline-block; animation: tourner 0.8s linear infinite; }
@keyframes tourner { to { transform: rotate(360deg); } }

/* ===================== HERO ===================== */
.hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 2rem;
  padding: 2rem 2.25rem;
  margin-bottom: 1.5rem;
  border-radius: 20px;
  color: #fff;
  background: radial-gradient(ellipse at 30% 0%, #0f3f7f 0%, #0a2a55 50%, #06152f 100%);
  box-shadow: 0 24px 48px -28px rgba(6, 21, 47, 0.7);
}
.hero-motif {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: radial-gradient(rgba(156, 196, 255, 0.16) 1px, transparent 1.4px);
  background-size: 26px 26px;
  -webkit-mask-image: linear-gradient(180deg, #000 0%, transparent 85%);
  mask-image: linear-gradient(180deg, #000 0%, transparent 85%);
}
.hero-glow {
  position: absolute;
  width: 560px;
  height: 560px;
  top: -240px;
  right: -140px;
  border-radius: 50%;
  pointer-events: none;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.38) 0%, transparent 65%);
}
.hero-gauche, .hero-solde { position: relative; z-index: 1; min-width: 0; }

.eyebrow {
  display: inline-block;
  padding: 5px 12px;
  border: 1px solid rgba(156, 196, 255, 0.4);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #9cc4ff;
}
.hero-titre { margin: 1rem 0 0.5rem; font-size: 2rem; line-height: 1.15; font-weight: 700; }
.hero-titre em { font-style: normal; color: #9cc4ff; }
.hero-sous-titre { margin: 0; max-width: 540px; font-size: 0.95rem; line-height: 1.55; color: #b8cdea; }

.hero-chips { list-style: none; margin: 1.25rem 0 0; padding: 0; display: flex; flex-wrap: wrap; gap: 0.6rem; }
.hero-chips li {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.45rem 0.85rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #e6f0ff;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(156, 196, 255, 0.22);
  backdrop-filter: blur(4px);
}
.hero-chips .bi { color: #7fb0ff; }

.hero-alertes { list-style: none; margin: 1.25rem 0 0; padding: 0; display: flex; flex-direction: column; gap: 0.5rem; }
.hero-alertes li {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.6rem 0.85rem;
  border-radius: 12px;
  font-size: 0.85rem;
  color: #e6f0ff;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(156, 196, 255, 0.18);
  border-left: 3px solid var(--c-vif);
}
.hero-alerte-icone {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 0.9rem;
  color: #fff;
  background: var(--c-vif);
  box-shadow: 0 4px 12px -4px var(--c-vif);
}

/* Carte du solde (verre dépoli) */
.hero-solde {
  align-self: start;
  padding: 1.4rem 1.5rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(156, 196, 255, 0.24);
  backdrop-filter: blur(8px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}
.solde-entete {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #9cc4ff;
}
.btn-actualiser {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  border: 1px solid rgba(156, 196, 255, 0.3);
  background: rgba(255, 255, 255, 0.06);
  color: #e6f0ff;
  transition: background 0.2s;
}
.btn-actualiser:hover { background: rgba(255, 255, 255, 0.16); }
.solde-valeur {
  margin: 0.55rem 0 0.8rem;
  font-size: 2.5rem;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}
.solde-valeur small { font-size: 0.42em; font-weight: 600; color: #9cc4ff; letter-spacing: 0; }
.statut-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.8rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  color: #fff;
  background: color-mix(in srgb, var(--c-vif) 22%, transparent);
  border: 1px solid color-mix(in srgb, var(--c-vif) 60%, transparent);
}
.statut-pill .bi { color: var(--c-vif); }
.solde-compo { margin-top: 1.3rem; }
.solde-barre { display: flex; gap: 3px; height: 8px; border-radius: 4px; overflow: hidden; background: rgba(255, 255, 255, 0.1); }
.solde-barre span { border-radius: 4px; }
.solde-legende { list-style: none; margin: 0.8rem 0 0; padding: 0; display: flex; flex-direction: column; gap: 0.4rem; font-size: 0.82rem; color: #b8cdea; }
.solde-legende li { display: flex; align-items: center; }
.solde-legende strong { margin-left: auto; color: #fff; font-variant-numeric: tabular-nums; }
.seg-principal { background: #5b9bff; }
.seg-penalites { background: #f04438; }
.solde-ok { margin: 1.2rem 0 0; font-size: 0.85rem; color: #b8cdea; }
.solde-ok .bi { color: var(--ok-vif); }

.puce { display: inline-block; width: 8px; height: 8px; border-radius: 2px; margin-right: 0.45rem; }
.puce.ton-danger, .puce.ton-alerte, .puce.ton-neutre { background: var(--c-vif); }

/* ===================== KPI ===================== */
.kpis { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }
.kpi {
  position: relative;
  overflow: hidden;
  display: flex;
  gap: 0.9rem;
  align-items: flex-start;
  padding: 1.15rem 1.2rem;
  background: #fff;
  border: 1px solid var(--trait);
  border-radius: 16px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.kpi:hover { transform: translateY(-3px); box-shadow: 0 16px 28px -16px rgba(15, 42, 74, 0.3); }
.kpi::after {
  content: '';
  position: absolute;
  top: -36px;
  right: -36px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--k);
  opacity: 0.08;
}
.k-danger { --k: #f04438; --k2: #b42318; }
.k-violet { --k: #8b5cf6; --k2: #6d28d9; }
.k-alerte { --k: #f79009; --k2: #b54708; }
.k-bleu   { --k: #3b82f6; --k2: #1d4ed8; }
.kpi-icone {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  font-size: 1.25rem;
  color: #fff;
  background: linear-gradient(135deg, var(--k) 0%, var(--k2) 100%);
  box-shadow: 0 10px 18px -10px var(--k);
}
.kpi-corps { display: flex; flex-direction: column; gap: 0.15rem; min-width: 0; }
.kpi-libelle { font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--gris); }
.kpi-valeur { font-size: 1.5rem; font-weight: 800; color: var(--navy); line-height: 1.2; font-variant-numeric: tabular-nums; letter-spacing: -0.01em; }
.kpi-valeur small { font-size: 0.55em; font-weight: 600; color: var(--gris); }
.kpi-date { text-transform: capitalize; }
.kpi-note { font-size: 0.78rem; color: var(--gris); }

/* ===================== Cartes ===================== */
.grille { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 1.5rem; align-items: start; }
.col-laterale { display: flex; flex-direction: column; gap: 1.5rem; }
.carte {
  background: #fff;
  border: 1px solid var(--trait);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05), 0 12px 24px -20px rgba(15, 42, 74, 0.25);
}
.carte-entete { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 1rem 1.25rem; border-bottom: 1px solid var(--trait); }
.entete-titre { display: flex; align-items: center; gap: 0.75rem; }
.entete-icone {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  font-size: 1.05rem;
  color: var(--bleu);
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #c7dbfe;
}
.carte-entete h3 { font-size: 1rem; font-weight: 700; margin: 0; color: var(--navy); }
.carte-sous-titre { margin: 0.1rem 0 0; font-size: 0.8rem; color: var(--gris); }
.compteur { font-size: 0.75rem; font-weight: 700; background: #eff6ff; color: var(--bleu); border-radius: 999px; padding: 0.25rem 0.7rem; white-space: nowrap; }

/* Filtres */
.filtres { display: flex; flex-direction: column; gap: 0.9rem; padding: 1rem 1.25rem; background: linear-gradient(180deg, #f8fafc 0%, #fff 100%); border-bottom: 1px solid var(--trait); }
.segments { display: inline-flex; flex-wrap: wrap; gap: 2px; align-self: flex-start; padding: 4px; border-radius: 12px; background: #eef2f7; }
.segment { border: none; background: transparent; color: var(--gris); font-size: 0.82rem; font-weight: 600; padding: 0.45rem 0.9rem; border-radius: 9px; transition: all 0.15s ease; }
.segment:hover { color: var(--navy); }
.segment.actif { background: #fff; color: var(--navy); box-shadow: 0 2px 6px rgba(16, 24, 40, 0.12); }
.segment-nb { display: inline-block; min-width: 1.4rem; margin-left: 0.3rem; padding: 0.05rem 0.4rem; border-radius: 999px; font-size: 0.7rem; background: #e2e8f0; color: var(--neutre); }
.segment.actif .segment-nb { background: var(--bleu); color: #fff; }
.filtres-champs { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 0.75rem; }
.champ { display: flex; flex-direction: column; gap: 0.3rem; margin: 0; }
.champ span { font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--gris); }
.champ select, .champ input {
  min-width: 150px;
  padding: 0.45rem 0.65rem;
  font-size: 0.85rem;
  color: var(--encre);
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.champ select:focus, .champ input:focus { outline: none; border-color: var(--bleu); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12); }
.btn-lien { border: none; background: none; color: var(--bleu); font-size: 0.82rem; font-weight: 600; padding: 0.45rem 0.25rem; }
.btn-lien:hover { text-decoration: underline; }

/* Tableau */
.tableau { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
.tableau th {
  text-align: left;
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--gris);
  padding: 0.75rem 1.1rem;
  border-bottom: 1px solid var(--trait);
  white-space: nowrap;
}
.tableau td { padding: 0.9rem 1.1rem; border-bottom: 1px solid #f1f4f8; vertical-align: middle; white-space: nowrap; }
.tableau tbody tr { transition: background 0.15s; }
.tableau tbody tr:hover { background: #f5f8ff; }
.ligne td:first-child { box-shadow: inset 3px 0 0 var(--c-vif); }
.ligne.ton-neutre td:first-child { box-shadow: none; }
.tableau tfoot td { padding: 0.9rem 1.1rem; background: #f8fafc; border-top: 2px solid var(--trait); font-weight: 600; font-size: 0.82rem; color: var(--gris); white-space: nowrap; }
.tableau tfoot .fort { font-size: 0.95rem; }
.num { text-align: right !important; font-variant-numeric: tabular-nums; }
.fort { font-weight: 800; color: var(--navy); }
.fort small { font-size: 0.7em; color: var(--gris); font-weight: 600; }
.attenue { color: var(--gris-clair); }

.impot { display: flex; align-items: center; gap: 0.6rem; }
.impot-pastille {
  min-width: 48px;
  text-align: center;
  font-weight: 800;
  font-size: 0.72rem;
  letter-spacing: 0.03em;
  color: var(--i);
  background: color-mix(in srgb, var(--i) 10%, #fff);
  border: 1px solid color-mix(in srgb, var(--i) 28%, #fff);
  padding: 0.3rem 0.45rem;
  border-radius: 8px;
}
.impot-nom { font-size: 0.78rem; color: var(--gris); white-space: normal; max-width: 140px; line-height: 1.25; }
.periode { display: block; font-weight: 600; color: var(--encre); }
.sous-info { display: block; font-size: 0.72rem; color: var(--gris-clair); }
.retard { display: block; font-size: 0.72rem; font-weight: 700; color: var(--danger); }
.reglement { min-width: 120px; }
.reglement-barre { height: 6px; margin-bottom: 0.3rem; border-radius: 3px; overflow: hidden; background: #edf1f6; }
.reglement-barre span { display: block; height: 100%; border-radius: 3px; background: linear-gradient(90deg, #12b76a, #067647); }

.etiquette {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.65rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--c);
  background: var(--c-fond);
  border: 1px solid var(--c-trait);
  border-radius: 999px;
  white-space: nowrap;
}
.etiquette.petite { font-size: 0.66rem; padding: 0.15rem 0.5rem; }

.vide { text-align: center; padding: 2.75rem 1rem; color: var(--gris); }
.vide-icone { display: inline-grid; place-items: center; width: 56px; height: 56px; border-radius: 16px; font-size: 1.6rem; color: var(--ok); background: var(--ok-fond); border: 1px solid var(--ok-trait); }
.vide p { margin: 0.75rem 0 0; }

/* Calendrier */
.cal-nav { display: flex; justify-content: space-between; align-items: center; padding: 0.9rem 1.25rem 0.25rem; font-weight: 700; text-transform: capitalize; color: var(--navy); }
.cal-nav button { width: 32px; height: 32px; border: 1px solid var(--trait); border-radius: 10px; background: #fff; color: var(--gris); transition: all 0.15s; }
.cal-nav button:hover { background: var(--navy); border-color: var(--navy); color: #fff; }
.calendrier { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; padding: 0.5rem 1.1rem; }
.cal-jour-nom { text-align: center; font-size: 0.68rem; font-weight: 700; color: var(--gris-clair); padding: 0.25rem 0; }
.cal-case { aspect-ratio: 1; display: grid; place-items: center; font-size: 0.8rem; border-radius: 10px; color: var(--encre); font-variant-numeric: tabular-nums; transition: background 0.15s; }
.cal-case:hover { background: var(--fond-doux); }
.cal-case.hors-mois { color: var(--gris-clair); opacity: 0.45; }
.cal-case.aujourd-hui { background: var(--navy); color: #fff; font-weight: 700; }
.cal-case.marque { background: var(--c-vif); color: #fff; font-weight: 700; cursor: help; box-shadow: 0 6px 12px -6px var(--c-vif); }
.cal-case.marque.ton-neutre { background: #e4e9f1; color: var(--navy); box-shadow: none; }
.cal-case.marque.aujourd-hui { outline: 2px solid var(--navy); outline-offset: 2px; }
.cal-legende { list-style: none; display: flex; flex-wrap: wrap; gap: 0.25rem 1rem; margin: 0; padding: 0.4rem 1.25rem 0.9rem; font-size: 0.74rem; color: var(--gris); }
.cal-liste { list-style: none; margin: 0; padding: 0.4rem 0; border-top: 1px solid var(--trait); }
.cal-liste li { display: flex; align-items: center; gap: 0.75rem; padding: 0.55rem 1.25rem; }
.cal-tuile { flex-shrink: 0; width: 44px; padding: 0.3rem 0; text-align: center; border-radius: 10px; background: var(--fond-doux); border: 1px solid var(--trait); line-height: 1.1; }
.cal-tuile strong { display: block; font-size: 1rem; color: var(--navy); }
.cal-tuile small { font-size: 0.62rem; text-transform: uppercase; color: var(--gris); font-weight: 600; }
.cal-liste-libelle { flex: 1; min-width: 0; line-height: 1.25; }
.cal-liste-libelle strong { display: block; font-size: 0.85rem; color: var(--navy); }
.cal-liste-libelle small { font-size: 0.75rem; color: var(--gris); }
.cal-vide { margin: 0; padding: 0.9rem 1.25rem; border-top: 1px solid var(--trait); font-size: 0.85rem; color: var(--gris); }

.mono { font-family: ui-monospace, 'SFMono-Regular', Consolas, monospace; }

/* ===================== Responsive ===================== */
@media (max-width: 1199px) {
  .hero { grid-template-columns: 1fr; }
  .kpis { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .grille { grid-template-columns: 1fr; }
}
@media (max-width: 575px) {
  .hero { padding: 1.5rem 1.25rem; border-radius: 16px; }
  .hero-titre { font-size: 1.5rem; }
  .solde-valeur { font-size: 2rem; }
  .kpis { grid-template-columns: 1fr; }
}
</style>
