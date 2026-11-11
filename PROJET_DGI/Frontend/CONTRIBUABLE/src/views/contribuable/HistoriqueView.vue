<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import ContribuableLayout from '@/layouts/ContribuableLayout.vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { formatDate, formatMontant, impot, libellePeriode, modePaiement, MOIS, typePeriode } from '@/utils/fiscal'

const auth = useAuthStore()

const historique = ref(null)
const chargement = ref(false)
const erreur = ref('')

onMounted(chargerHistorique)

async function chargerHistorique() {
  if (!auth.contribuableNif) return
  chargement.value = true
  erreur.value = ''
  try {
    const { data } = await api.get(`/contribuable/historique/${encodeURIComponent(auth.contribuableNif)}`)
    historique.value = data
  } catch (e) {
    erreur.value = 'Impossible de charger votre historique. Vérifiez que le serveur est démarré.'
  } finally {
    chargement.value = false
  }
}

// État du rapprochement déclaration / paiements
const ETATS = {
  SOLDEE:    { libelle: 'Soldée',          ton: 'ok',     icone: 'bi-check2-circle' },
  PARTIELLE: { libelle: 'Paiement partiel', ton: 'alerte', icone: 'bi-pie-chart' },
  NON_PAYEE: { libelle: 'Non payée',       ton: 'danger', icone: 'bi-x-circle' },
}
const etat = (code) => ETATS[code] || { libelle: code, ton: 'neutre', icone: 'bi-question-circle' }

// Taux de règlement : part du montant déclaré effectivement versée
const tauxReglement = computed(() => {
  const declare = Number(historique.value?.totalDeclare || 0)
  return declare ? Math.min(100, Math.round((Number(historique.value.totalPaye) / declare) * 100)) : 0
})
// Anneau SVG (rayon 42)
const CIRCONFERENCE = 2 * Math.PI * 42

// ---------- Vue et filtres ----------
const vue = ref('CHRONOLOGIE')
const NATURES = [
  { cle: 'TOUT',        libelle: 'Tout' },
  { cle: 'DECLARATION', libelle: 'Déclarations' },
  { cle: 'PAIEMENT',    libelle: 'Paiements' },
]
const FILTRES_DEFAUT = { recherche: '', impot: '', annee: '', nature: 'TOUT', etat: '' }
const filtres = reactive({ ...FILTRES_DEFAUT })

const dossiers = computed(() => historique.value?.dossiers || [])
const evenements = computed(() => historique.value?.evenements || [])
const impotsDisponibles = computed(() => [...new Set(dossiers.value.map((d) => d.impot))].sort())
const anneesDisponibles = computed(() =>
  [...new Set(dossiers.value.map((d) => d.annee).filter(Boolean))].sort((a, b) => b - a)
)

// Filtres communs aux deux vues : impôt, exercice, recherche libre
function correspond(x, reference = '') {
  const texte = filtres.recherche.trim().toLowerCase()
  return (!filtres.impot || x.impot === filtres.impot)
    && (!filtres.annee || x.annee === Number(filtres.annee))
    && (!texte || `${x.impot} ${libellePeriode(x)} ${reference}`.toLowerCase().includes(texte))
}

const evenementsFiltres = computed(() =>
  evenements.value.filter((e) =>
    correspond(e, e.reference || '') && (filtres.nature === 'TOUT' || e.type === filtres.nature)
  )
)
const compteNature = (cle) =>
  evenements.value.filter((e) => correspond(e, e.reference || '') && (cle === 'TOUT' || e.type === cle)).length

// Chronologie regroupée par mois (le plus récent en premier, l'API trie déjà)
const groupesParMois = computed(() => {
  const groupes = []
  for (const e of evenementsFiltres.value) {
    const d = new Date(e.date)
    const cle = `${d.getFullYear()}-${d.getMonth()}`
    let g = groupes[groupes.length - 1]
    if (!g || g.cle !== cle) {
      g = { cle, titre: `${MOIS[d.getMonth()]} ${d.getFullYear()}`, items: [] }
      groupes.push(g)
    }
    g.items.push(e)
  }
  return groupes
})

const dossiersFiltres = computed(() =>
  dossiers.value.filter((d) =>
    correspond(d, d.paiements.map((p) => p.reference).join(' ')) && (!filtres.etat || d.etatRapprochement === filtres.etat)
  )
)
const compteEtat = (cle) =>
  dossiers.value.filter((d) =>
    correspond(d, d.paiements.map((p) => p.reference).join(' ')) && (!cle || d.etatRapprochement === cle)
  ).length

const totauxDossiers = computed(() => {
  const somme = (champ) => dossiersFiltres.value.reduce((t, d) => t + Number(d[champ] || 0), 0)
  return { declare: somme('montantDeclare'), paye: somme('montantPaye'), ecart: somme('ecart') }
})

const filtresActifs = computed(() => Object.keys(FILTRES_DEFAUT).some((k) => filtres[k] !== FILTRES_DEFAUT[k]))
function reinitialiserFiltres() {
  Object.assign(filtres, FILTRES_DEFAUT)
}

// Lignes dépliées de la vue "par déclaration"
const ouverts = ref(new Set())
function basculer(id) {
  const s = new Set(ouverts.value)
  s.has(id) ? s.delete(id) : s.add(id)
  ouverts.value = s
}

function pctPaye(d) {
  const declare = Number(d.montantDeclare || 0)
  return declare ? Math.min(100, Math.round((Number(d.montantPaye) / declare) * 100)) : 0
}
</script>

<template>
  <ContribuableLayout>
    <div class="hist">

      <div v-if="erreur" class="message-erreur">
        <i class="bi bi-wifi-off"></i> {{ erreur }}
      </div>

      <div v-if="chargement && !historique" class="chargement">
        <span class="spinner-border spinner-border-sm"></span> Chargement de votre historique…
      </div>

      <template v-if="historique">

        <!-- ===== En-tête ===== -->
        <section class="hero">
          <div class="hero-motif"></div>
          <div class="hero-glow"></div>

          <div class="hero-gauche">
            <span class="eyebrow">ESPACE CONTRIBUABLE · HISTORIQUE</span>
            <h1 class="hero-titre">Vos déclarations <em>et vos paiements</em>, réunis</h1>
            <p class="hero-sous-titre">
              Chaque paiement est rattaché à la déclaration qu'il règle. Retrouvez ce que vous avez déclaré,
              ce que vous avez versé, et ce qui reste à régulariser.
            </p>
            <ul class="hero-chips">
              <li><i class="bi bi-file-earmark-text"></i> {{ historique.nombreDeclarations }} déclaration(s)</li>
              <li><i class="bi bi-cash-coin"></i> {{ historique.nombrePaiements }} paiement(s)</li>
              <li v-if="historique.declarationsHorsDelai" class="chip-alerte">
                <i class="bi bi-alarm"></i> {{ historique.declarationsHorsDelai }} déposée(s) hors délai
              </li>
              <li v-if="historique.paiementsHorsDelai" class="chip-alerte">
                <i class="bi bi-alarm"></i> {{ historique.paiementsHorsDelai }} paiement(s) hors délai
              </li>
            </ul>
          </div>

          <div class="hero-bilan">
            <div class="anneau">
              <svg viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="42" class="anneau-fond" />
                <circle cx="50" cy="50" r="42" class="anneau-valeur"
                        :stroke-dasharray="CIRCONFERENCE"
                        :stroke-dashoffset="CIRCONFERENCE * (1 - tauxReglement / 100)" />
              </svg>
              <div class="anneau-texte">
                <strong>{{ tauxReglement }}<small>%</small></strong>
                <span>réglé</span>
              </div>
            </div>
            <dl class="bilan">
              <div>
                <dt><i class="puce p-declare"></i> Total déclaré</dt>
                <dd>{{ formatMontant(historique.totalDeclare) }} <small>Ar</small></dd>
              </div>
              <div>
                <dt><i class="puce p-paye"></i> Total versé</dt>
                <dd>{{ formatMontant(historique.totalPaye) }} <small>Ar</small></dd>
              </div>
              <div>
                <dt><i class="puce p-ecart"></i> Principal non réglé</dt>
                <dd>{{ formatMontant(historique.totalEcart) }} <small>Ar</small></dd>
              </div>
            </dl>
          </div>
        </section>

        <!-- ===== Historique ===== -->
        <section class="carte">
          <header class="carte-entete">
            <div class="entete-titre">
              <span class="entete-icone"><i class="bi bi-clock-history"></i></span>
              <div>
                <h3>Historique unifié</h3>
                <p class="carte-sous-titre">
                  {{ vue === 'CHRONOLOGIE' ? 'Tous les événements, du plus récent au plus ancien' : 'Chaque déclaration avec les paiements qui la règlent' }}
                </p>
              </div>
            </div>
            <div class="bascule-vue">
              <button :class="{ actif: vue === 'CHRONOLOGIE' }" @click="vue = 'CHRONOLOGIE'">
                <i class="bi bi-list-ul"></i> Chronologie
              </button>
              <button :class="{ actif: vue === 'DOSSIERS' }" @click="vue = 'DOSSIERS'">
                <i class="bi bi-diagram-3"></i> Par déclaration
              </button>
            </div>
          </header>

          <!-- Filtres -->
          <div class="filtres">
            <div class="filtres-champs">
              <label class="champ champ-recherche">
                <span>Recherche</span>
                <div class="recherche">
                  <i class="bi bi-search"></i>
                  <input type="search" v-model="filtres.recherche" placeholder="Référence, impôt, période…" />
                </div>
              </label>
              <label class="champ">
                <span>Type d'impôt</span>
                <select v-model="filtres.impot">
                  <option value="">Tous</option>
                  <option v-for="c in impotsDisponibles" :key="c" :value="c">{{ c }} — {{ impot(c).libelle }}</option>
                </select>
              </label>
              <label class="champ">
                <span>Exercice</span>
                <select v-model="filtres.annee">
                  <option value="">Tous</option>
                  <option v-for="a in anneesDisponibles" :key="a" :value="a">{{ a }}</option>
                </select>
              </label>
              <button v-if="filtresActifs" type="button" class="btn-lien" @click="reinitialiserFiltres">
                <i class="bi bi-x-circle"></i> Réinitialiser
              </button>
            </div>

            <div v-if="vue === 'CHRONOLOGIE'" class="segments">
              <button v-for="n in NATURES" :key="n.cle" type="button"
                      :class="['segment', { actif: filtres.nature === n.cle }]" @click="filtres.nature = n.cle">
                {{ n.libelle }} <span class="segment-nb">{{ compteNature(n.cle) }}</span>
              </button>
            </div>
            <div v-else class="segments">
              <button type="button" :class="['segment', { actif: !filtres.etat }]" @click="filtres.etat = ''">
                Toutes <span class="segment-nb">{{ compteEtat('') }}</span>
              </button>
              <button v-for="(def, cle) in ETATS" :key="cle" type="button"
                      :class="['segment', { actif: filtres.etat === cle }]" @click="filtres.etat = cle">
                {{ def.libelle }} <span class="segment-nb">{{ compteEtat(cle) }}</span>
              </button>
            </div>
          </div>

          <!-- ===== Vue chronologique ===== -->
          <template v-if="vue === 'CHRONOLOGIE'">
            <div v-if="!groupesParMois.length" class="vide">
              <span class="vide-icone"><i class="bi bi-funnel"></i></span>
              <p>Aucun événement ne correspond à ces filtres.</p>
            </div>
            <div v-else class="chrono">
              <div v-for="g in groupesParMois" :key="g.cle" class="chrono-groupe">
                <div class="chrono-mois">
                  <span>{{ g.titre }}</span>
                  <small>{{ g.items.length }} événement(s)</small>
                </div>
                <ol class="chrono-liste">
                  <li v-for="(e, i) in g.items" :key="i" :class="['evt', e.type === 'PAIEMENT' ? 'evt-paiement' : 'evt-declaration']">
                    <span class="evt-noeud">
                      <i :class="['bi', e.type === 'PAIEMENT' ? 'bi-arrow-down-left' : 'bi-file-earmark-text']"></i>
                    </span>
                    <div class="evt-carte">
                      <div class="evt-date">
                        <strong>{{ formatDate(e.date, { day: '2-digit' }) }}</strong>
                        <small>{{ formatDate(e.date, { month: 'short' }) }}</small>
                      </div>
                      <div class="evt-corps">
                        <div class="evt-titre">
                          <span class="impot-pastille" :style="{ '--i': impot(e.impot).couleur }">{{ e.impot }}</span>
                          <span>{{ e.type === 'PAIEMENT' ? 'Paiement' : 'Déclaration' }} · {{ libellePeriode(e) }}</span>
                          <span v-if="e.horsDelai" class="tag tag-danger"><i class="bi bi-alarm"></i> Hors délai</span>
                        </div>
                        <div class="evt-meta">
                          <template v-if="e.type === 'PAIEMENT'">
                            <span><i :class="['bi', modePaiement(e.mode).icone]"></i> {{ modePaiement(e.mode).libelle }}</span>
                            <span class="mono">{{ e.reference }}</span>
                          </template>
                          <template v-else>
                            <span>{{ impot(e.impot).libelle }}</span>
                            <span>{{ typePeriode(e) }}</span>
                          </template>
                        </div>
                      </div>
                      <div class="evt-montant">
                        <strong>{{ e.type === 'PAIEMENT' ? '− ' : '' }}{{ formatMontant(e.montant) }} <small>Ar</small></strong>
                        <span>{{ e.type === 'PAIEMENT' ? 'versé' : 'déclaré' }}</span>
                      </div>
                    </div>
                  </li>
                </ol>
              </div>
            </div>
          </template>

          <!-- ===== Vue par déclaration ===== -->
          <template v-else>
            <div v-if="!dossiersFiltres.length" class="vide">
              <span class="vide-icone"><i class="bi bi-funnel"></i></span>
              <p>Aucune déclaration ne correspond à ces filtres.</p>
            </div>
            <div v-else class="table-responsive">
              <table class="tableau">
                <thead>
                  <tr>
                    <th class="col-chevron"></th>
                    <th>Impôt</th>
                    <th>Période</th>
                    <th>Déclarée le</th>
                    <th class="num">Montant déclaré</th>
                    <th>Versé</th>
                    <th class="num">Reste (principal)</th>
                    <th>État</th>
                  </tr>
                </thead>
                <tbody>
                  <template v-for="d in dossiersFiltres" :key="d.idDeclaration">
                    <tr :class="['ligne', `ton-${etat(d.etatRapprochement).ton}`, { ouvert: ouverts.has(d.idDeclaration) }]"
                        @click="basculer(d.idDeclaration)">
                      <td class="col-chevron"><i class="bi bi-chevron-right chevron"></i></td>
                      <td>
                        <div class="impot">
                          <span class="impot-pastille" :style="{ '--i': impot(d.impot).couleur }">{{ d.impot }}</span>
                          <span class="impot-nom">{{ impot(d.impot).libelle }}</span>
                        </div>
                      </td>
                      <td>
                        <span class="periode">{{ libellePeriode(d) }}</span>
                        <span class="sous-info">Limite : {{ formatDate(d.dateLimite) }}</span>
                      </td>
                      <td>
                        {{ formatDate(d.dateDeclaration) }}
                        <span v-if="d.declarationHorsDelai" class="retard"><i class="bi bi-alarm"></i> hors délai</span>
                      </td>
                      <td class="num">{{ formatMontant(d.montantDeclare) }}</td>
                      <td>
                        <div class="reglement">
                          <div class="reglement-barre"><span :style="{ width: pctPaye(d) + '%' }"></span></div>
                          <span class="sous-info">
                            {{ formatMontant(d.montantPaye) }} Ar · {{ d.paiements.length }} paiement(s)
                          </span>
                        </div>
                      </td>
                      <td :class="['num', Number(d.ecart) ? 'fort' : 'attenue']">
                        {{ Number(d.ecart) ? formatMontant(d.ecart) : '—' }}
                      </td>
                      <td>
                        <span :class="['etiquette', `ton-${etat(d.etatRapprochement).ton}`]">
                          <i :class="['bi', etat(d.etatRapprochement).icone]"></i> {{ etat(d.etatRapprochement).libelle }}
                        </span>
                      </td>
                    </tr>
                    <tr v-if="ouverts.has(d.idDeclaration)" class="detail">
                      <td></td>
                      <td colspan="7">
                        <div v-if="d.paiements.length" class="paiements">
                          <div class="paiements-titre">Paiements rattachés à cette déclaration</div>
                          <div v-for="p in d.paiements" :key="p.reference" class="paiement">
                            <span class="paiement-icone"><i :class="['bi', modePaiement(p.mode).icone]"></i></span>
                            <div class="paiement-info">
                              <strong>{{ formatDate(p.datePaiement, { day: 'numeric', month: 'long', year: 'numeric' }) }}</strong>
                              <span>{{ modePaiement(p.mode).libelle }} · <span class="mono">{{ p.reference }}</span></span>
                            </div>
                            <span v-if="p.horsDelai" class="tag tag-danger"><i class="bi bi-alarm"></i> Hors délai</span>
                            <span v-else class="tag tag-ok"><i class="bi bi-check2"></i> Dans les délais</span>
                            <strong class="paiement-montant">{{ formatMontant(p.montant) }} Ar</strong>
                          </div>
                        </div>
                        <p v-else class="aucun-paiement">
                          <i class="bi bi-info-circle"></i> Aucun paiement rattaché à cette déclaration.
                        </p>
                      </td>
                    </tr>
                  </template>
                </tbody>
                <tfoot>
                  <tr>
                    <td></td>
                    <td colspan="3">Total de la sélection</td>
                    <td class="num">{{ formatMontant(totauxDossiers.declare) }}</td>
                    <td>{{ formatMontant(totauxDossiers.paye) }} Ar versés</td>
                    <td class="num fort">{{ formatMontant(totauxDossiers.ecart) }} <small>Ar</small></td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>
            <p class="note-bas">
              <i class="bi bi-info-circle"></i>
              Le reste indiqué porte sur le principal déclaré. Les pénalités de retard (1 % par mois) sont calculées
              dans le tableau de bord.
            </p>
          </template>
        </section>
      </template>
    </div>
  </ContribuableLayout>
</template>

<style scoped>
.hist {
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

.ton-danger { --c: var(--danger); --c-fond: var(--danger-fond); --c-trait: var(--danger-trait); --c-vif: var(--danger-vif); }
.ton-alerte { --c: var(--alerte); --c-fond: var(--alerte-fond); --c-trait: var(--alerte-trait); --c-vif: var(--alerte-vif); }
.ton-ok     { --c: var(--ok);     --c-fond: var(--ok-fond);     --c-trait: var(--ok-trait);     --c-vif: var(--ok-vif); }
.ton-neutre { --c: var(--neutre); --c-fond: var(--neutre-fond); --c-trait: var(--neutre-trait); --c-vif: var(--neutre-vif); }

.message-erreur, .chargement { padding: 1rem 1.25rem; border-radius: 12px; margin-bottom: 1.5rem; }
.message-erreur { background: var(--alerte-fond); border: 1px solid var(--alerte-trait); color: var(--alerte); }
.chargement { text-align: center; color: var(--gris); }
.mono { font-family: ui-monospace, 'SFMono-Regular', Consolas, monospace; }

/* ===================== HERO ===================== */
.hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  gap: 2rem;
  align-items: center;
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
.hero-gauche, .hero-bilan { position: relative; z-index: 1; min-width: 0; }
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
.hero-sous-titre { margin: 0; max-width: 560px; font-size: 0.95rem; line-height: 1.55; color: #b8cdea; }
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
.hero-chips .chip-alerte { border-color: rgba(240, 68, 56, 0.55); background: rgba(240, 68, 56, 0.14); }
.hero-chips .chip-alerte .bi { color: #ff8a80; }

.hero-bilan {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1.4rem 1.5rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(156, 196, 255, 0.24);
  backdrop-filter: blur(8px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}
.anneau { position: relative; flex-shrink: 0; width: 118px; height: 118px; }
.anneau svg { width: 100%; height: 100%; transform: rotate(-90deg); }
.anneau circle { fill: none; stroke-width: 9; }
.anneau-fond { stroke: rgba(255, 255, 255, 0.12); }
.anneau-valeur { stroke: #5b9bff; stroke-linecap: round; transition: stroke-dashoffset 0.8s ease; filter: drop-shadow(0 0 6px rgba(91, 155, 255, 0.6)); }
.anneau-texte { position: absolute; inset: 0; display: grid; place-content: center; text-align: center; line-height: 1.1; }
.anneau-texte strong { font-size: 1.7rem; font-weight: 800; }
.anneau-texte strong small { font-size: 0.55em; color: #9cc4ff; }
.anneau-texte span { font-size: 0.68rem; letter-spacing: 0.12em; text-transform: uppercase; color: #9cc4ff; font-weight: 700; }
.bilan { flex: 1; margin: 0; display: flex; flex-direction: column; gap: 0.7rem; }
.bilan div { display: flex; flex-direction: column; }
.bilan dt { font-size: 0.72rem; font-weight: 600; color: #b8cdea; }
.bilan dd { margin: 0; font-size: 1.1rem; font-weight: 800; font-variant-numeric: tabular-nums; }
.bilan dd small { font-size: 0.65em; color: #9cc4ff; font-weight: 600; }
.puce { display: inline-block; width: 8px; height: 8px; border-radius: 2px; margin-right: 0.35rem; }
.p-declare { background: #9cc4ff; }
.p-paye { background: #5b9bff; }
.p-ecart { background: #f04438; }

/* ===================== Carte ===================== */
.carte {
  background: #fff;
  border: 1px solid var(--trait);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05), 0 12px 24px -20px rgba(15, 42, 74, 0.25);
}
.carte-entete { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; padding: 1rem 1.25rem; border-bottom: 1px solid var(--trait); }
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

.bascule-vue { display: inline-flex; padding: 4px; border-radius: 12px; background: var(--navy); gap: 2px; }
.bascule-vue button {
  border: none;
  background: transparent;
  color: #b8cdea;
  font-size: 0.82rem;
  font-weight: 600;
  padding: 0.45rem 0.9rem;
  border-radius: 9px;
  transition: all 0.15s ease;
}
.bascule-vue button:hover { color: #fff; }
.bascule-vue button.actif { background: var(--bleu); color: #fff; box-shadow: 0 4px 10px -4px rgba(37, 99, 235, 0.8); }
.bascule-vue .bi { margin-right: 0.3rem; }

/* Filtres */
.filtres { display: flex; flex-direction: column; gap: 0.9rem; padding: 1rem 1.25rem; background: linear-gradient(180deg, #f8fafc 0%, #fff 100%); border-bottom: 1px solid var(--trait); }
.filtres-champs { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 0.75rem; }
.champ { display: flex; flex-direction: column; gap: 0.3rem; margin: 0; }
.champ > span { font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--gris); }
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
.champ-recherche { flex: 1; min-width: 220px; }
.recherche { position: relative; }
.recherche .bi { position: absolute; left: 0.7rem; top: 50%; transform: translateY(-50%); color: var(--gris-clair); font-size: 0.85rem; }
.recherche input { width: 100%; padding-left: 2rem; }
.btn-lien { border: none; background: none; color: var(--bleu); font-size: 0.82rem; font-weight: 600; padding: 0.45rem 0.25rem; }
.btn-lien:hover { text-decoration: underline; }
.segments { display: inline-flex; flex-wrap: wrap; gap: 2px; align-self: flex-start; padding: 4px; border-radius: 12px; background: #eef2f7; }
.segment { border: none; background: transparent; color: var(--gris); font-size: 0.82rem; font-weight: 600; padding: 0.45rem 0.9rem; border-radius: 9px; transition: all 0.15s ease; }
.segment:hover { color: var(--navy); }
.segment.actif { background: #fff; color: var(--navy); box-shadow: 0 2px 6px rgba(16, 24, 40, 0.12); }
.segment-nb { display: inline-block; min-width: 1.4rem; margin-left: 0.3rem; padding: 0.05rem 0.4rem; border-radius: 999px; font-size: 0.7rem; background: #e2e8f0; color: var(--neutre); }
.segment.actif .segment-nb { background: var(--bleu); color: #fff; }

/* ===================== Chronologie ===================== */
.chrono { padding: 0.5rem 1.25rem 1.25rem; }
.chrono-groupe + .chrono-groupe { margin-top: 0.5rem; }
.chrono-mois {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: baseline;
  gap: 0.6rem;
  padding: 0.9rem 0 0.6rem;
  background: #fff;
}
.chrono-mois span { font-size: 0.95rem; font-weight: 800; color: var(--navy); }
.chrono-mois small { font-size: 0.75rem; color: var(--gris-clair); font-weight: 600; }
.chrono-liste { list-style: none; margin: 0; padding: 0 0 0 0.25rem; position: relative; }
.chrono-liste::before {
  content: '';
  position: absolute;
  left: calc(0.25rem + 17px);
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: linear-gradient(180deg, #dbe4f0, #eef2f7);
  border-radius: 1px;
}
.evt { position: relative; display: flex; align-items: center; gap: 1rem; padding: 0.4rem 0; }
.evt-declaration { --e: #2563eb; --e-fond: #eff6ff; }
.evt-paiement    { --e: #12b76a; --e-fond: #ecfdf3; }
.evt-noeud {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 0.95rem;
  background: var(--e);
  border: 3px solid #fff;
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--e) 30%, #fff), 0 6px 12px -6px var(--e);
}
.evt-carte {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--trait);
  border-radius: 14px;
  background: #fff;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}
.evt-carte:hover { transform: translateX(3px); border-color: color-mix(in srgb, var(--e) 35%, #fff); box-shadow: 0 10px 20px -16px rgba(15, 42, 74, 0.45); }
.evt-date { flex-shrink: 0; width: 46px; padding: 0.3rem 0; text-align: center; border-radius: 10px; background: var(--e-fond); line-height: 1.1; }
.evt-date strong { display: block; font-size: 1.05rem; color: var(--navy); }
.evt-date small { font-size: 0.62rem; text-transform: uppercase; font-weight: 700; color: var(--e); }
.evt-corps { flex: 1; min-width: 0; }
.evt-titre { display: flex; align-items: center; flex-wrap: wrap; gap: 0.5rem; font-weight: 600; color: var(--encre); font-size: 0.9rem; }
.evt-meta { display: flex; flex-wrap: wrap; gap: 0.25rem 1rem; margin-top: 0.25rem; font-size: 0.78rem; color: var(--gris); }
.evt-meta .bi { margin-right: 0.25rem; }
.evt-montant { flex-shrink: 0; text-align: right; line-height: 1.2; }
.evt-montant strong { display: block; font-size: 1rem; font-weight: 800; color: var(--navy); font-variant-numeric: tabular-nums; }
.evt-paiement .evt-montant strong { color: var(--ok); }
.evt-montant small { font-size: 0.7em; color: var(--gris); font-weight: 600; }
.evt-montant span { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--gris-clair); font-weight: 700; }

/* ===================== Tableau par déclaration ===================== */
.tableau { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
.tableau th {
  text-align: left;
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--gris);
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--trait);
  white-space: nowrap;
}
.tableau td { padding: 0.9rem 1rem; border-bottom: 1px solid #f1f4f8; vertical-align: middle; white-space: nowrap; }
.ligne { cursor: pointer; transition: background 0.15s; }
.ligne:hover, .ligne.ouvert { background: #f5f8ff; }
.ligne td:first-child { box-shadow: inset 3px 0 0 var(--c-vif); }
.col-chevron { width: 40px; padding-right: 0 !important; }
.chevron { display: inline-block; color: var(--gris-clair); transition: transform 0.2s ease; }
.ligne.ouvert .chevron { transform: rotate(90deg); color: var(--bleu); }
.tableau tfoot td { padding: 0.9rem 1rem; background: #f8fafc; border-top: 2px solid var(--trait); font-weight: 600; font-size: 0.82rem; color: var(--gris); white-space: nowrap; }
.num { text-align: right !important; font-variant-numeric: tabular-nums; }
.fort { font-weight: 800; color: var(--navy); }
.fort small { font-size: 0.7em; color: var(--gris); font-weight: 600; }
.attenue { color: var(--gris-clair); }

.impot { display: flex; align-items: center; gap: 0.6rem; }
.impot-pastille {
  min-width: 46px;
  text-align: center;
  font-weight: 800;
  font-size: 0.72rem;
  letter-spacing: 0.03em;
  color: var(--i);
  background: color-mix(in srgb, var(--i) 10%, #fff);
  border: 1px solid color-mix(in srgb, var(--i) 28%, #fff);
  padding: 0.25rem 0.45rem;
  border-radius: 8px;
}
.impot-nom { font-size: 0.78rem; color: var(--gris); white-space: normal; max-width: 140px; line-height: 1.25; }
.periode { display: block; font-weight: 600; color: var(--encre); }
.sous-info { display: block; font-size: 0.72rem; color: var(--gris-clair); }
.retard { display: block; font-size: 0.72rem; font-weight: 700; color: var(--danger); }
.reglement { min-width: 140px; }
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
.tag { display: inline-flex; align-items: center; gap: 0.25rem; padding: 0.1rem 0.5rem; border-radius: 999px; font-size: 0.68rem; font-weight: 700; white-space: nowrap; }
.tag-danger { color: var(--danger); background: var(--danger-fond); border: 1px solid var(--danger-trait); }
.tag-ok { color: var(--ok); background: var(--ok-fond); border: 1px solid var(--ok-trait); }

/* Détail des paiements d'une déclaration */
.detail td { background: #f8fafc; padding-top: 0.25rem; padding-bottom: 1rem; white-space: normal; }
.paiements { border: 1px solid var(--trait); border-radius: 14px; background: #fff; overflow: hidden; }
.paiements-titre { padding: 0.6rem 1rem; font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--gris); border-bottom: 1px solid var(--trait); }
.paiement { display: flex; align-items: center; gap: 0.85rem; padding: 0.7rem 1rem; }
.paiement + .paiement { border-top: 1px dashed var(--trait); }
.paiement-icone { flex-shrink: 0; width: 34px; height: 34px; border-radius: 10px; display: grid; place-items: center; color: var(--ok); background: var(--ok-fond); border: 1px solid var(--ok-trait); }
.paiement-info { flex: 1; min-width: 0; display: flex; flex-direction: column; font-size: 0.82rem; color: var(--gris); }
.paiement-info strong { color: var(--encre); font-size: 0.88rem; }
.paiement-montant { font-size: 0.95rem; font-weight: 800; color: var(--ok); font-variant-numeric: tabular-nums; white-space: nowrap; }
.aucun-paiement { margin: 0; padding: 0.75rem 1rem; border: 1px dashed var(--neutre-trait); border-radius: 12px; font-size: 0.85rem; color: var(--gris); background: #fff; }
.note-bas { margin: 0; padding: 0.8rem 1.25rem; font-size: 0.78rem; color: var(--gris); border-top: 1px solid var(--trait); }

.vide { text-align: center; padding: 2.75rem 1rem; color: var(--gris); }
.vide-icone { display: inline-grid; place-items: center; width: 56px; height: 56px; border-radius: 16px; font-size: 1.6rem; color: var(--bleu); background: #eff6ff; border: 1px solid #c7dbfe; }
.vide p { margin: 0.75rem 0 0; }

/* ===================== Responsive ===================== */
@media (max-width: 1199px) {
  .hero { grid-template-columns: 1fr; }
}
@media (max-width: 575px) {
  .hero { padding: 1.5rem 1.25rem; border-radius: 16px; }
  .hero-titre { font-size: 1.5rem; }
  .hero-bilan { flex-direction: column; align-items: stretch; }
  .anneau { align-self: center; }
  .evt-carte { flex-wrap: wrap; }
  .evt-montant { width: 100%; text-align: left; }
}
</style>
