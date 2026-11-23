<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import ContribuableLayout from '@/layouts/ContribuableLayout.vue'
import BordereauOfficiel from '@/components/BordereauOfficiel.vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { formatDate, formatMontant, impot, libellePeriode, MOIS } from '@/utils/fiscal'
import { telechargerPdf } from '@/utils/pdf'

const auth = useAuthStore()
const nif = computed(() => encodeURIComponent(auth.contribuableNif))

// Message d'une règle métier refusée par le serveur (HTTP 400 {"message"})
const messageServeur = (e, defaut) => e.response?.data?.message || defaut

// ---------- Impôts déclarables et obligations à payer ----------
const impots = ref([])
const echeances = ref([])
const chargement = ref(false)
const erreurChargement = ref('')

onMounted(charger)

async function charger() {
  if (!auth.contribuableNif) return
  chargement.value = true
  erreurChargement.value = ''
  try {
    const [declarables, resume] = await Promise.all([
      api.get(`/contribuable/impots-declarables/${nif.value}`),
      api.get(`/contribuable/dashboard-summary/${nif.value}`),
    ])
    impots.value = declarables.data
    echeances.value = resume.data.echeances || []
    if (!form.codeImpot && impots.value.length) form.codeImpot = impots.value[0].code
  } catch {
    erreurChargement.value = 'Impossible de charger vos impôts. Vérifiez que le serveur est démarré.'
  } finally {
    chargement.value = false
  }
}

const aPayer = computed(() => echeances.value.filter((e) => Number(e.resteAPayer) > 0))

// ---------- Formulaire ----------
const aujourdHui = new Date()
const moisPrecedent = new Date(aujourdHui.getFullYear(), aujourdHui.getMonth() - 1, 1)

const form = reactive({ codeImpot: '', annee: null, mois: null, base: null, tvaDeductible: null })
const impotChoisi = computed(() => impots.value.find((i) => i.code === form.codeImpot))
const mensuel = computed(() => impotChoisi.value?.periodicite?.toUpperCase() === 'MENSUELLE')

// Période proposée par défaut : le dernier mois terminé, ou le dernier exercice terminé
watch(mensuel, (m) => {
  form.annee = m ? moisPrecedent.getFullYear() : aujourdHui.getFullYear() - 1
  form.mois = m ? moisPrecedent.getMonth() + 1 : null
}, { immediate: true })

const annees = computed(() => {
  const derniere = mensuel.value ? aujourdHui.getFullYear() : aujourdHui.getFullYear() - 1
  return Array.from({ length: 5 }, (_, i) => derniere - i)
})

const simulation = ref(null)
const erreur = ref('')
const calcul = ref(false)
const envoi = ref(false)

// Toute modification de la saisie invalide le calcul affiché
watch(form, () => { simulation.value = null; erreur.value = '' })

function requete() {
  return {
    codeImpot: form.codeImpot,
    annee: form.annee,
    mois: mensuel.value ? form.mois : null,
    base: form.base,
    tvaDeductible: impotChoisi.value?.avecTvaDeductible ? (form.tvaDeductible || 0) : null,
  }
}

async function calculer() {
  erreur.value = ''
  calcul.value = true
  try {
    const { data } = await api.post(`/contribuable/declarations/${nif.value}/simulation`, requete())
    simulation.value = data
  } catch (e) {
    erreur.value = messageServeur(e, 'Le calcul a échoué. Vérifiez que le serveur est démarré.')
  } finally {
    calcul.value = false
  }
}

async function valider() {
  if (!confirm('Valider cette déclaration ? Une période ne se déclare qu\'une seule fois.')) return
  erreur.value = ''
  envoi.value = true
  try {
    const { data } = await api.post(`/contribuable/declarations/${nif.value}`, requete())
    resultat.value = data
    bordereau.value = data.bordereau
    form.base = null
    form.tvaDeductible = null
    await charger()
  } catch (e) {
    erreur.value = messageServeur(e, 'La déclaration a échoué. Vérifiez que le serveur est démarré.')
  } finally {
    envoi.value = false
  }
}

// ---------- Bordereau ----------
const resultat = ref(null)
const bordereau = ref(null)
const ouverture = ref(null)
const erreurBordereau = ref('')

async function afficherBordereau(e) {
  erreurBordereau.value = ''
  resultat.value = null
  ouverture.value = e.idDeclaration
  try {
    const { data } = await api.get(`/contribuable/declarations/${nif.value}/${e.idDeclaration}/bordereau`)
    bordereau.value = data
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (err) {
    erreurBordereau.value = messageServeur(err, 'Impossible d\'obtenir le bordereau.')
  } finally {
    ouverture.value = null
  }
}

function fermerBordereau() {
  bordereau.value = null
  resultat.value = null
}

// Téléchargement du bordereau officiel en PDF (pas besoin d'imprimante)
const docBordereau = ref(null)
const telechargement = ref(false)
async function telecharger() {
  telechargement.value = true
  try {
    await telechargerPdf(docBordereau.value?.feuille, bordereau.value?.reference || 'bordereau')
  } finally {
    telechargement.value = false
  }
}
</script>

<template>
  <ContribuableLayout>
    <div class="decl">

      <div v-if="erreurChargement" class="message-erreur">
        <i class="bi bi-wifi-off"></i> {{ erreurChargement }}
      </div>

      <!-- ===== En-tête ===== -->
      <section class="hero">
        <div class="hero-motif"></div>
        <div class="hero-glow"></div>
        <div class="hero-contenu">
          <span class="eyebrow">ESPACE CONTRIBUABLE · DÉCLARATION</span>
          <h1 class="hero-titre">Déclarez en ligne, <em>payez au guichet</em></h1>
          <p class="hero-sous-titre">
            Saisissez la base de votre impôt : le montant est calculé automatiquement. Vous obtenez un bordereau
            de versement à présenter au guichet de votre centre fiscal, ou à indiquer sur votre virement.
          </p>
          <ol class="etapes">
            <li><span>1</span> Saisir la base</li>
            <li><span>2</span> Vérifier le calcul</li>
            <li><span>3</span> Télécharger le bordereau</li>
            <li><span>4</span> Payer la totalité</li>
          </ol>
        </div>
      </section>

      <!-- ===== Bordereau émis ou consulté ===== -->
      <section v-if="resultat && !resultat.bordereau" class="carte carte-succes">
        <div class="succes">
          <span class="succes-icone"><i class="bi bi-check2-circle"></i></span>
          <div>
            <strong>Déclaration enregistrée : rien à payer.</strong>
            <p>{{ resultat.impot }} · {{ libellePeriode(resultat) }} — l'impôt calculé est nul
              (par exemple un crédit de TVA). Aucun bordereau n'est nécessaire.</p>
          </div>
          <button class="btn-lien" @click="fermerBordereau"><i class="bi bi-x-lg"></i></button>
        </div>
      </section>

      <section v-if="bordereau" class="carte bv-zone">
        <header class="carte-entete">
          <div class="entete-titre">
            <span class="entete-icone"><i class="bi bi-receipt"></i></span>
            <div>
              <h3>{{ resultat ? 'Déclaration enregistrée : votre bordereau' : 'Votre bordereau de versement' }}</h3>
              <p class="carte-sous-titre">Téléchargez-le ou notez sa référence avant de payer.</p>
            </div>
          </div>
          <div class="actions-bv">
            <button class="btn-principal" :disabled="telechargement" @click="telecharger">
              <span v-if="telechargement" class="spinner-border spinner-border-sm"></span>
              <i v-else class="bi bi-download"></i> Télécharger (PDF)
            </button>
            <button class="btn-secondaire" @click="fermerBordereau">Fermer</button>
          </div>
        </header>

        <div v-if="bordereau.referenceRemplacee" class="bandeau-info">
          <i class="bi bi-arrow-repeat"></i>
          Le bordereau {{ bordereau.referenceRemplacee }} avait expiré : il est remplacé par celui-ci, au montant du jour.
        </div>

        <!-- Bordereau au format officiel ; c'est cette feuille qui est téléchargée en PDF -->
        <div class="bv-document">
          <BordereauOfficiel ref="docBordereau" :bv="bordereau" />
        </div>
      </section>

      <div class="grille">
        <!-- ===== Nouvelle déclaration ===== -->
        <section class="carte">
          <header class="carte-entete">
            <div class="entete-titre">
              <span class="entete-icone"><i class="bi bi-file-earmark-plus"></i></span>
              <div>
                <h3>Nouvelle déclaration</h3>
                <p class="carte-sous-titre">Seuls les impôts de votre régime fiscal sont proposés</p>
              </div>
            </div>
          </header>

          <div v-if="chargement && !impots.length" class="chargement">
            <span class="spinner-border spinner-border-sm"></span> Chargement…
          </div>

          <div v-else-if="!impots.length && !erreurChargement" class="vide">
            <span class="vide-icone"><i class="bi bi-slash-circle"></i></span>
            <p>Aucun impôt n'est rattaché à votre compte. Contactez votre centre fiscal.</p>
          </div>

          <form v-else class="formulaire" @submit.prevent="calculer">
            <div class="champ">
              <span>Impôt</span>
              <div class="choix-impots">
                <button v-for="i in impots" :key="i.code" type="button"
                        :class="['choix', { actif: form.codeImpot === i.code }]"
                        :style="{ '--i': impot(i.code).couleur }" @click="form.codeImpot = i.code">
                  <strong>{{ i.code }}</strong>
                  <small>{{ impot(i.code).libelle }}</small>
                  <em>{{ i.periodicite?.toLowerCase() }}</em>
                </button>
              </div>
            </div>

            <div class="ligne-champs">
              <label v-if="mensuel" class="champ">
                <span>Mois</span>
                <select v-model.number="form.mois" required>
                  <option v-for="(m, i) in MOIS" :key="m" :value="i + 1">{{ m }}</option>
                </select>
              </label>
              <label class="champ">
                <span>{{ mensuel ? 'Année' : 'Exercice' }}</span>
                <select v-model.number="form.annee" required>
                  <option v-for="a in annees" :key="a" :value="a">{{ a }}</option>
                </select>
              </label>
            </div>

            <label class="champ">
              <span>{{ impotChoisi?.libelleBase || 'Base imposable' }} (Ar)</span>
              <input v-model.number="form.base" type="number" min="0" step="1" required placeholder="Ex : 10000000" />
            </label>

            <label v-if="impotChoisi?.avecTvaDeductible" class="champ">
              <span>TVA déductible du mois (Ar)</span>
              <input v-model.number="form.tvaDeductible" type="number" min="0" step="1" placeholder="0" />
            </label>

            <div v-if="erreur" class="message-erreur message-form">
              <i class="bi bi-exclamation-circle"></i> {{ erreur }}
            </div>

            <!-- Résultat du calcul -->
            <div v-if="simulation" :class="['calcul', { 'calcul-retard': simulation.enRetard }]">
              <div class="calcul-ligne">
                <span>Impôt calculé</span>
                <strong>{{ formatMontant(simulation.montantImpot) }} <small>Ar</small></strong>
              </div>
              <div class="calcul-detail">
                {{ simulation.impot }} · {{ libellePeriode(simulation) }} · date limite {{ formatDate(simulation.dateLimite) }}
              </div>
              <div v-if="simulation.enRetard" class="calcul-alerte">
                <i class="bi bi-alarm"></i> La date limite est dépassée : des pénalités de retard s'ajouteront au bordereau.
              </div>
              <div v-if="!Number(simulation.montantImpot)" class="calcul-detail">
                Rien à payer : la déclaration sera enregistrée sans bordereau.
              </div>
            </div>

            <div class="actions">
              <button v-if="!simulation" type="submit" class="btn-principal" :disabled="calcul || !form.codeImpot">
                <span v-if="calcul" class="spinner-border spinner-border-sm"></span>
                <i v-else class="bi bi-calculator"></i> Calculer l'impôt
              </button>
              <template v-else>
                <button type="button" class="btn-secondaire" @click="simulation = null">Modifier</button>
                <button type="button" class="btn-principal" :disabled="envoi" @click="valider">
                  <span v-if="envoi" class="spinner-border spinner-border-sm"></span>
                  <i v-else class="bi bi-check2-circle"></i> Valider la déclaration
                </button>
              </template>
            </div>
          </form>
        </section>

        <!-- ===== À payer ===== -->
        <section class="carte">
          <header class="carte-entete">
            <div class="entete-titre">
              <span class="entete-icone"><i class="bi bi-cash-coin"></i></span>
              <div>
                <h3>À payer</h3>
                <p class="carte-sous-titre">Le bordereau est recalculé au montant du jour</p>
              </div>
            </div>
          </header>

          <div v-if="erreurBordereau" class="message-erreur message-form">
            <i class="bi bi-exclamation-circle"></i> {{ erreurBordereau }}
          </div>

          <div v-if="!aPayer.length" class="vide">
            <span class="vide-icone"><i class="bi bi-check2-all"></i></span>
            <p>Vous n'avez rien à payer pour le moment.</p>
          </div>

          <ul v-else class="liste-a-payer">
            <li v-for="e in aPayer" :key="e.idDeclaration">
              <span class="impot-pastille" :style="{ '--i': impot(e.impot).couleur }">{{ e.impot }}</span>
              <div class="a-payer-info">
                <strong>{{ libellePeriode(e) }}</strong>
                <span>
                  Limite {{ formatDate(e.dateLimite) }}
                  <template v-if="e.joursRetard > 0"> · <em class="retard">{{ e.joursRetard }} j de retard</em></template>
                </span>
              </div>
              <div class="a-payer-montant">{{ formatMontant(e.resteAPayer) }} <small>Ar</small></div>
              <span v-if="e.statut === 'EN_ATTENTE_ENCAISSEMENT'" class="tag-neutre">
                <i class="bi bi-journal-check"></i> Chèque en cours
              </span>
              <button v-else class="btn-bv" :disabled="ouverture === e.idDeclaration" @click="afficherBordereau(e)">
                <span v-if="ouverture === e.idDeclaration" class="spinner-border spinner-border-sm"></span>
                <template v-else><i class="bi bi-receipt"></i> Bordereau</template>
              </button>
            </li>
          </ul>
        </section>
      </div>
    </div>
  </ContribuableLayout>
</template>

<style scoped>
.decl {
  --navy: #0f2a4a;
  --encre: #1e293b;
  --gris: #64748b;
  --gris-clair: #94a3b8;
  --trait: #e6eaf0;
  --bleu: #2563eb;
  --danger: #b42318;  --danger-fond: #fef3f2;  --danger-trait: #fecdca;
  --alerte: #b54708;  --alerte-fond: #fffaeb;  --alerte-trait: #fedf89;
  --ok: #067647;      --ok-fond: #ecfdf3;      --ok-trait: #abefc6;
  --neutre: #475467;  --neutre-fond: #f2f4f7;  --neutre-trait: #d0d5dd;
  color: var(--encre);
  padding-top: 0.25rem;
}
.mono { font-family: ui-monospace, 'SFMono-Regular', Consolas, monospace; }
.message-erreur { padding: 0.85rem 1.1rem; border-radius: 12px; margin-bottom: 1.25rem; background: var(--alerte-fond); border: 1px solid var(--alerte-trait); color: var(--alerte); font-size: 0.88rem; }
.message-form { margin: 0 0 1rem; }
.chargement { padding: 2rem; text-align: center; color: var(--gris); }

/* ===== Hero (même langage que l'historique) ===== */
.hero {
  position: relative;
  overflow: hidden;
  padding: 2rem 2.25rem;
  margin-bottom: 1.5rem;
  border-radius: 20px;
  color: #fff;
  background: radial-gradient(ellipse at 30% 0%, #0f3f7f 0%, #0a2a55 50%, #06152f 100%);
  box-shadow: 0 24px 48px -28px rgba(6, 21, 47, 0.7);
}
.hero-motif {
  position: absolute; inset: 0; pointer-events: none;
  background-image: radial-gradient(rgba(156, 196, 255, 0.16) 1px, transparent 1.4px);
  background-size: 26px 26px;
  -webkit-mask-image: linear-gradient(180deg, #000 0%, transparent 85%);
  mask-image: linear-gradient(180deg, #000 0%, transparent 85%);
}
.hero-glow {
  position: absolute; width: 560px; height: 560px; top: -240px; right: -140px; border-radius: 50%; pointer-events: none;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.38) 0%, transparent 65%);
}
.hero-contenu { position: relative; z-index: 1; }
.eyebrow { display: inline-block; padding: 5px 12px; border: 1px solid rgba(156, 196, 255, 0.4); border-radius: 999px; font-size: 11px; font-weight: 700; letter-spacing: 2px; color: #9cc4ff; }
.hero-titre { margin: 1rem 0 0.5rem; font-size: 2rem; line-height: 1.15; font-weight: 700; }
.hero-titre em { font-style: normal; color: #9cc4ff; }
.hero-sous-titre { margin: 0; max-width: 640px; font-size: 0.95rem; line-height: 1.55; color: #b8cdea; }
.etapes { list-style: none; margin: 1.25rem 0 0; padding: 0; display: flex; flex-wrap: wrap; gap: 0.6rem; }
.etapes li {
  display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.4rem 0.85rem 0.4rem 0.4rem;
  border-radius: 999px; font-size: 0.8rem; font-weight: 600; color: #e6f0ff;
  background: rgba(255, 255, 255, 0.08); border: 1px solid rgba(156, 196, 255, 0.22);
}
.etapes span { width: 22px; height: 22px; border-radius: 50%; display: grid; place-items: center; font-size: 0.72rem; font-weight: 800; background: #2563eb; color: #fff; }

/* ===== Cartes ===== */
.grille { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr); gap: 1.5rem; align-items: start; }
.carte {
  background: #fff; border: 1px solid var(--trait); border-radius: 18px; overflow: hidden; margin-bottom: 1.5rem;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05), 0 12px 24px -20px rgba(15, 42, 74, 0.25);
}
.carte-entete { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; padding: 1rem 1.25rem; border-bottom: 1px solid var(--trait); }
.entete-titre { display: flex; align-items: center; gap: 0.75rem; }
.entete-icone {
  flex-shrink: 0; width: 38px; height: 38px; border-radius: 11px; display: grid; place-items: center; font-size: 1.05rem;
  color: var(--bleu); background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%); border: 1px solid #c7dbfe;
}
.carte-entete h3 { font-size: 1rem; font-weight: 700; margin: 0; color: var(--navy); }
.carte-sous-titre { margin: 0.1rem 0 0; font-size: 0.8rem; color: var(--gris); }

/* ===== Formulaire ===== */
.formulaire { padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; }
.ligne-champs { display: flex; gap: 0.75rem; flex-wrap: wrap; }
.ligne-champs .champ { flex: 1; min-width: 140px; }
.champ { display: flex; flex-direction: column; gap: 0.35rem; margin: 0; }
.champ > span { font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--gris); }
.champ select, .champ input {
  width: 100%; padding: 0.6rem 0.75rem; font-size: 0.92rem; color: var(--encre); background: #fff;
  border: 1px solid #cbd5e1; border-radius: 10px; transition: border-color 0.2s, box-shadow 0.2s;
}
.champ select:focus, .champ input:focus { outline: none; border-color: var(--bleu); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12); }

.choix-impots { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 0.6rem; }
.choix {
  display: flex; flex-direction: column; align-items: flex-start; gap: 0.1rem; padding: 0.7rem 0.85rem; text-align: left;
  border: 1.5px solid var(--trait); border-radius: 12px; background: #fff; transition: all 0.15s ease;
}
.choix strong { font-size: 0.95rem; color: var(--i); }
.choix small { font-size: 0.75rem; color: var(--gris); line-height: 1.25; }
.choix em { font-style: normal; font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: var(--gris-clair); }
.choix:hover { border-color: color-mix(in srgb, var(--i) 40%, #fff); }
.choix.actif { border-color: var(--i); background: color-mix(in srgb, var(--i) 7%, #fff); box-shadow: 0 0 0 3px color-mix(in srgb, var(--i) 14%, transparent); }

.calcul { padding: 1rem 1.1rem; border-radius: 14px; background: #eff6ff; border: 1px solid #c7dbfe; }
.calcul-retard { background: var(--alerte-fond); border-color: var(--alerte-trait); }
.calcul-ligne { display: flex; justify-content: space-between; align-items: baseline; gap: 1rem; }
.calcul-ligne span { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--gris); }
.calcul-ligne strong { font-size: 1.6rem; font-weight: 800; color: var(--navy); font-variant-numeric: tabular-nums; }
.calcul-ligne small { font-size: 0.55em; color: var(--gris); }
.calcul-detail { font-size: 0.82rem; color: var(--gris); margin-top: 0.2rem; }
.calcul-alerte { margin-top: 0.5rem; font-size: 0.82rem; font-weight: 600; color: var(--alerte); }

.actions { display: flex; justify-content: flex-end; gap: 0.6rem; }
.btn-principal, .btn-secondaire, .btn-bv {
  display: inline-flex; align-items: center; gap: 0.45rem; border-radius: 10px; font-weight: 700; font-size: 0.88rem;
  padding: 0.6rem 1.1rem; transition: all 0.15s ease;
}
.btn-principal { border: none; color: #fff; background: var(--bleu); box-shadow: 0 6px 14px -8px rgba(37, 99, 235, 0.9); }
.btn-principal:hover:not(:disabled) { background: #1d4ed8; }
.btn-principal:disabled { opacity: 0.6; }
.btn-secondaire { border: 1.5px solid #cbd5e1; background: #fff; color: #334155; }
.btn-secondaire:hover { background: #f1f5f9; }
.btn-lien { border: none; background: none; color: var(--gris); font-size: 1rem; padding: 0.25rem; }

/* ===== À payer ===== */
.liste-a-payer { list-style: none; margin: 0; padding: 0.5rem 1.25rem 1rem; }
.liste-a-payer li { display: flex; align-items: center; gap: 0.85rem; padding: 0.8rem 0; }
.liste-a-payer li + li { border-top: 1px dashed var(--trait); }
.impot-pastille {
  min-width: 46px; text-align: center; font-weight: 800; font-size: 0.72rem; letter-spacing: 0.03em; color: var(--i);
  background: color-mix(in srgb, var(--i) 10%, #fff); border: 1px solid color-mix(in srgb, var(--i) 28%, #fff);
  padding: 0.25rem 0.45rem; border-radius: 8px;
}
.a-payer-info { flex: 1; min-width: 0; display: flex; flex-direction: column; font-size: 0.78rem; color: var(--gris); }
.a-payer-info strong { font-size: 0.9rem; color: var(--encre); }
.retard { font-style: normal; font-weight: 700; color: var(--danger); }
.a-payer-montant { font-weight: 800; color: var(--navy); font-variant-numeric: tabular-nums; white-space: nowrap; }
.a-payer-montant small { font-size: 0.7em; color: var(--gris); }
.btn-bv { border: 1px solid #c7dbfe; background: #eff6ff; color: var(--bleu); padding: 0.4rem 0.75rem; font-size: 0.8rem; white-space: nowrap; }
.btn-bv:hover:not(:disabled) { background: #dbeafe; }
.tag-neutre { display: inline-flex; align-items: center; gap: 0.3rem; padding: 0.25rem 0.6rem; border-radius: 999px; font-size: 0.72rem; font-weight: 700; white-space: nowrap; color: var(--neutre); background: var(--neutre-fond); border: 1px solid var(--neutre-trait); }

.vide { text-align: center; padding: 2.5rem 1rem; color: var(--gris); }
.vide-icone { display: inline-grid; place-items: center; width: 52px; height: 52px; border-radius: 16px; font-size: 1.5rem; color: var(--bleu); background: #eff6ff; border: 1px solid #c7dbfe; }
.vide p { margin: 0.75rem 0 0; }

/* ===== Succès sans bordereau ===== */
.carte-succes { border-color: var(--ok-trait); }
.succes { display: flex; align-items: flex-start; gap: 0.85rem; padding: 1rem 1.25rem; background: var(--ok-fond); }
.succes-icone { font-size: 1.5rem; color: var(--ok); }
.succes strong { color: var(--ok); }
.succes p { margin: 0.2rem 0 0; font-size: 0.85rem; color: var(--gris); }
.succes .btn-lien { margin-left: auto; }

/* ===== Bordereau ===== */
.actions-bv { display: flex; gap: 0.5rem; }
.bandeau-info { padding: 0.7rem 1.25rem; font-size: 0.85rem; color: var(--alerte); background: var(--alerte-fond); border-bottom: 1px solid var(--alerte-trait); }
.bv-document { padding: 1.25rem; background: #eef1f5; }
.bv-document :deep(.bvo-feuille) { box-shadow: 0 4px 18px rgba(16, 24, 40, 0.12); }

.num { text-align: right; font-variant-numeric: tabular-nums; }

@media (max-width: 991px) {
  .grille { grid-template-columns: 1fr; }
}
@media (max-width: 575px) {
  .hero { padding: 1.5rem 1.25rem; border-radius: 16px; }
  .hero-titre { font-size: 1.5rem; }
  .liste-a-payer li { flex-wrap: wrap; }
}
</style>

