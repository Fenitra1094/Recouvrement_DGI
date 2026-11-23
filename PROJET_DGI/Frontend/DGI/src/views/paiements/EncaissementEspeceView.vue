<template>
  <DgiLayout>
    <!-- En-tête -->
    <div class="entete-page">
      <div class="d-flex align-items-center gap-3">
        <span class="entete-icone"><i class="bi bi-bank2"></i></span>
        <div>
          <h3 class="fw-bold mb-0">Guichet</h3>
          <p class="text-muted mb-0 small">Encaissement des bordereaux, déclaration pour un contribuable et suivi des chèques remis.</p>
        </div>
      </div>
      <ul class="nav nav-pills onglets">
        <li v-for="o in ONGLETS" :key="o.cle" class="nav-item">
          <button :class="['nav-link', { active: onglet === o.cle }]" @click="onglet = o.cle">
            <i :class="['bi me-1', o.icone]"></i>{{ o.libelle }}
            <span v-if="o.cle === 'CHEQUES' && cheques.length" class="badge bg-warning text-dark ms-1">{{ cheques.length }}</span>
          </button>
        </li>
      </ul>
    </div>

    <!-- ===================================================== -->
    <!-- ENCAISSER                                              -->
    <!-- ===================================================== -->
    <div v-if="onglet === 'ENCAISSER'" class="row g-4">
      <div class="col-xl-5">
        <!-- Recherche : référence du bordereau ou NIF (bordereau perdu) -->
        <div class="carte">
          <div class="carte-titre"><i class="bi bi-search"></i> Rechercher un bordereau</div>
          <form class="d-flex gap-2" @submit.prevent="rechercher">
            <input v-model.trim="saisie" class="form-control font-monospace"
                   placeholder="BV-2026-000001 ou NIF" required />
            <button class="btn btn-primary fw-semibold px-3" :disabled="recherche">
              <span v-if="recherche" class="spinner-border spinner-border-sm"></span>
              <span v-else>Rechercher</span>
            </button>
          </form>
          <div class="aide">
            <i class="bi bi-info-circle me-1"></i>
            Bordereau perdu ? Saisissez le <strong>NIF</strong> : les déclarations à payer s'affichent et le bordereau en cours est retrouvé.
          </div>
          <div v-if="erreurBv" class="alert alert-warning small mt-3 mb-0">{{ erreurBv }}</div>
        </div>

        <!-- Déclarations à payer d'un NIF -->
        <div v-if="aPayer" class="carte mt-4">
          <div class="carte-titre"><i class="bi bi-list-check"></i> À payer — NIF {{ nifRecherche }}</div>
          <p v-if="!aPayer.length" class="text-muted small mb-0">Aucune déclaration à payer pour ce contribuable.</p>
          <ul v-else class="liste-a-payer">
            <li v-for="e in aPayer" :key="e.idDeclaration" :class="{ actif: bv && bv.idDeclaration === e.idDeclaration }">
              <span class="pastille">{{ e.impot }}</span>
              <div class="flex-grow-1">
                <div class="fw-semibold">{{ libellePeriode(e) }}</div>
                <div class="small text-muted">
                  Limite {{ formatDate(e.dateLimite) }}
                  <span v-if="e.joursRetard > 0" class="text-danger"> · {{ e.joursRetard }} j de retard</span>
                </div>
              </div>
              <div class="text-end">
                <div class="fw-bold text-nowrap">{{ formatMontant(e.resteAPayer) }} Ar</div>
                <button class="btn btn-link btn-sm p-0" :disabled="ouverture === e.idDeclaration" @click="ouvrirBordereau(e)">
                  <span v-if="ouverture === e.idDeclaration" class="spinner-border spinner-border-sm"></span>
                  <template v-else><i class="bi bi-receipt me-1"></i>Bordereau</template>
                </button>
              </div>
            </li>
          </ul>
        </div>

        <!-- Encaissement du bordereau trouvé -->
        <div v-if="bv && !quittance" class="carte mt-4">
          <div class="d-flex justify-content-between align-items-start mb-3">
            <div>
              <div class="fw-bold">{{ bv.raisonSociale }}</div>
              <div class="small text-muted">NIF {{ bv.nif }} · {{ bv.impot }} {{ libellePeriode(bv) }}</div>
            </div>
            <span :class="['badge', STATUTS_BV[bv.statut]?.classe || 'bg-secondary']">{{ STATUTS_BV[bv.statut]?.libelle || bv.statut }}</span>
          </div>

          <div v-if="bv.referenceRemplacee" class="alert alert-info small">
            <i class="bi bi-arrow-repeat me-1"></i>
            Le bordereau <strong>{{ bv.referenceRemplacee }}</strong> avait expiré : il est remplacé par
            <strong>{{ bv.reference }}</strong>, au montant du jour. Remettez ce nouveau bordereau au contribuable.
          </div>

          <div class="montant-encaisser">
            <span>Montant à encaisser</span>
            <strong>{{ formatMontant(bv.montant) }} Ar</strong>
          </div>

          <form v-if="bv.statut === 'A_PAYER'" @submit.prevent="encaisser">
            <div class="btn-group w-100 mb-3" role="group">
              <button type="button" :class="['btn', paiement.mode === 'ESPECES' ? 'btn-dark' : 'btn-outline-secondary']"
                      @click="paiement.mode = 'ESPECES'"><i class="bi bi-cash-stack me-1"></i>Espèces</button>
              <button type="button" :class="['btn', paiement.mode === 'CHEQUE' ? 'btn-dark' : 'btn-outline-secondary']"
                      @click="paiement.mode = 'CHEQUE'"><i class="bi bi-journal-check me-1"></i>Chèque</button>
            </div>

            <label class="form-label small fw-semibold">Montant reçu (Ar)</label>
            <input v-model.number="paiement.montantRecu" type="number" min="0" step="1" class="form-control form-control-lg mb-1" required />
            <div :class="['small mb-3', montantExact ? 'text-success' : 'text-danger']">
              <i :class="['bi me-1', montantExact ? 'bi-check-circle' : 'bi-exclamation-circle']"></i>
              {{ montantExact ? 'Montant exact.' : `Le montant doit être exactement ${formatMontant(bv.montant)} Ar : pas de paiement partiel.` }}
            </div>

            <div v-if="paiement.mode === 'CHEQUE'" class="row g-2 mb-3">
              <div class="col-md-6">
                <label class="form-label small fw-semibold">Numéro du chèque</label>
                <input v-model.trim="paiement.numeroCheque" class="form-control" required />
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-semibold">Banque</label>
                <input v-model.trim="paiement.banque" class="form-control" placeholder="BNI, BOA, BFV…" required />
              </div>
              <div class="col-12 small text-muted">
                <i class="bi bi-info-circle me-1"></i>
                Le chèque reste en attente jusqu'à la confirmation de la banque. Les pénalités s'arrêtent aujourd'hui, date de remise.
              </div>
            </div>

            <div v-if="erreurEncaissement" class="alert alert-danger small">{{ erreurEncaissement }}</div>
            <button class="btn btn-success fw-semibold w-100 py-2" :disabled="encaissement || !montantExact">
              <span v-if="encaissement" class="spinner-border spinner-border-sm me-2"></span>
              Encaisser {{ formatMontant(paiement.montantRecu) }} Ar
            </button>
          </form>
          <div v-else class="alert alert-secondary small mb-0">
            <i class="bi bi-info-circle me-1"></i>Ce bordereau n'est pas à encaisser ({{ STATUTS_BV[bv.statut]?.libelle || bv.statut }}).
          </div>
        </div>

        <!-- Après encaissement -->
        <div v-if="quittance" class="carte mt-4">
          <div class="d-flex align-items-center gap-2 mb-2">
            <i :class="['bi fs-4', quittance.statut === 'VALIDE' ? 'bi-patch-check-fill text-success' : 'bi-hourglass-split text-warning']"></i>
            <div class="fw-bold">{{ quittance.statut === 'VALIDE' ? 'Paiement enregistré' : 'Chèque remis, en attente de la banque' }}</div>
          </div>
          <p class="small text-muted">
            {{ quittance.statut === 'VALIDE' ? 'La déclaration est soldée.' : 'La quittance sera acquise à la confirmation du chèque.' }}
            Remettez le document au contribuable.
          </p>
          <button class="btn btn-outline-secondary w-100" @click="nouvelEncaissement">
            <i class="bi bi-plus-lg me-1"></i>Nouvel encaissement
          </button>
        </div>
      </div>

      <!-- Document : bordereau officiel ou quittance -->
      <div class="col-xl-7">
        <div class="carte document">
          <div class="document-barre">
            <div class="fw-semibold">
              <template v-if="quittance">{{ quittance.statut === 'VALIDE' ? 'Quittance' : 'Reçu de remise de chèque' }} {{ quittance.referencePaiement }}</template>
              <template v-else-if="bv">Bordereau de versement {{ bv.reference }}</template>
              <template v-else>Document</template>
            </div>
            <button v-if="bv || quittance" class="btn btn-primary btn-sm fw-semibold" :disabled="telechargement"
                    @click="telecharger(quittance ? 'quittance' : 'bordereau')">
              <span v-if="telechargement" class="spinner-border spinner-border-sm me-1"></span>
              <i v-else class="bi bi-download me-1"></i>Télécharger (PDF)
            </button>
          </div>
          <div class="document-fond">
            <QuittanceOfficielle v-if="quittance" ref="docQuittance" :q="quittance" :agent-par-defaut="auth.user?.matricule" />
            <BordereauOfficiel v-else-if="bv" ref="docBordereau" :bv="bv" />
            <div v-else class="vide">
              <i class="bi bi-receipt"></i>
              <p>Recherchez un bordereau par sa référence ou par le NIF du contribuable.</p>
              <small>S'il a expiré, un nouveau bordereau est émis automatiquement, au montant du jour.</small>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===================================================== -->
    <!-- DÉCLARER POUR UN CONTRIBUABLE                          -->
    <!-- ===================================================== -->
    <div v-else-if="onglet === 'DECLARER'" class="row g-4">
      <div class="col-xl-5">
        <div class="carte">
          <div class="carte-titre"><i class="bi bi-file-earmark-plus"></i> Déclaration au guichet</div>

          <label class="form-label small fw-semibold">NIF du contribuable</label>
          <form class="d-flex gap-2 mb-3" @submit.prevent="chargerImpots">
            <input v-model.trim="decl.nif" class="form-control font-monospace" placeholder="300XXXXXXX" maxlength="10" required />
            <button class="btn btn-outline-primary fw-semibold px-3 text-nowrap" :disabled="chargementImpots">
              <span v-if="chargementImpots" class="spinner-border spinner-border-sm"></span>
              <span v-else>Valider</span>
            </button>
          </form>
          <div v-if="erreurImpots" class="alert alert-warning small">{{ erreurImpots }}</div>

          <form v-if="impots.length" @submit.prevent="simuler">
            <label class="form-label small fw-semibold">Impôt</label>
            <select v-model="decl.codeImpot" class="form-select mb-3" required>
              <option v-for="i in impots" :key="i.code" :value="i.code">{{ i.code }} — {{ i.libelle }} ({{ i.periodicite?.toLowerCase() }})</option>
            </select>

            <div class="row g-2 mb-3">
              <div v-if="mensuel" class="col-6">
                <label class="form-label small fw-semibold">Mois</label>
                <select v-model.number="decl.mois" class="form-select" required>
                  <option v-for="(m, i) in MOIS" :key="m" :value="i + 1">{{ m }}</option>
                </select>
              </div>
              <div :class="mensuel ? 'col-6' : 'col-12'">
                <label class="form-label small fw-semibold">{{ mensuel ? 'Année' : 'Exercice' }}</label>
                <select v-model.number="decl.annee" class="form-select" required>
                  <option v-for="a in annees" :key="a" :value="a">{{ a }}</option>
                </select>
              </div>
            </div>

            <label class="form-label small fw-semibold">{{ impotChoisi?.libelleBase || 'Base imposable' }} (Ar)</label>
            <input v-model.number="decl.base" type="number" min="0" step="1" class="form-control mb-3" required />

            <template v-if="impotChoisi?.avecTvaDeductible">
              <label class="form-label small fw-semibold">TVA déductible du mois (Ar)</label>
              <input v-model.number="decl.tvaDeductible" type="number" min="0" step="1" class="form-control mb-3" />
            </template>

            <div v-if="erreurDecl" class="alert alert-danger small">{{ erreurDecl }}</div>

            <div v-if="simulation" :class="['calcul', { retard: simulation.enRetard }]">
              <div class="d-flex justify-content-between align-items-baseline">
                <span class="small fw-semibold text-uppercase">Impôt calculé</span>
                <span class="fs-4 fw-bold">{{ formatMontant(simulation.montantImpot) }} Ar</span>
              </div>
              <div class="small">{{ simulation.impot }} · {{ libellePeriode(simulation) }} · date limite {{ formatDate(simulation.dateLimite) }}</div>
              <div v-if="simulation.enRetard" class="small fw-semibold mt-1">
                <i class="bi bi-alarm me-1"></i>Date limite dépassée : les pénalités s'ajoutent au bordereau.
              </div>
            </div>

            <div class="d-flex gap-2 justify-content-end">
              <button v-if="!simulation" class="btn btn-primary fw-semibold" :disabled="calcul">
                <span v-if="calcul" class="spinner-border spinner-border-sm me-1"></span>
                <i v-else class="bi bi-calculator me-1"></i>Calculer
              </button>
              <template v-else>
                <button type="button" class="btn btn-outline-secondary" @click="simulation = null">Modifier</button>
                <button type="button" class="btn btn-success fw-semibold" :disabled="envoiDecl" @click="declarer">
                  <span v-if="envoiDecl" class="spinner-border spinner-border-sm me-1"></span>
                  Enregistrer la déclaration
                </button>
              </template>
            </div>
          </form>
        </div>
      </div>

      <div class="col-xl-7">
        <div class="carte document">
          <div class="document-barre">
            <div class="fw-semibold">
              <template v-if="resultatDecl">
                <i class="bi bi-check-circle-fill text-success me-1"></i>Déclaration enregistrée
                <span v-if="resultatDecl.bordereau" class="text-muted fw-normal"> — {{ resultatDecl.bordereau.reference }}</span>
              </template>
              <template v-else>Bordereau de versement</template>
            </div>
            <div v-if="resultatDecl?.bordereau" class="d-flex gap-2">
              <button class="btn btn-primary btn-sm fw-semibold" :disabled="telechargement" @click="telecharger('declaration')">
                <span v-if="telechargement" class="spinner-border spinner-border-sm me-1"></span>
                <i v-else class="bi bi-download me-1"></i>Télécharger (PDF)
              </button>
              <button class="btn btn-success btn-sm fw-semibold" @click="encaisserMaintenant">
                <i class="bi bi-cash-stack me-1"></i>Encaisser maintenant
              </button>
            </div>
          </div>
          <div class="document-fond">
            <BordereauOfficiel v-if="resultatDecl?.bordereau" ref="docDeclaration" :bv="resultatDecl.bordereau" />
            <div v-else-if="resultatDecl" class="vide">
              <i class="bi bi-check2-circle"></i>
              <p>L'impôt calculé est nul : la déclaration est enregistrée comme réglée, sans bordereau.</p>
            </div>
            <div v-else class="vide">
              <i class="bi bi-person-vcard"></i>
              <p>Saisissez le NIF du contribuable : seuls les impôts de son régime fiscal sont proposés.</p>
              <small>La déclaration est enregistrée à votre nom ({{ auth.user?.matricule }}). Le bordereau s'affiche ici.</small>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===================================================== -->
    <!-- CHÈQUES EN ATTENTE                                     -->
    <!-- ===================================================== -->
    <div v-else class="carte p-0">
      <div class="document-barre">
        <div class="fw-semibold"><i class="bi bi-journal-check me-1"></i>Chèques remis, en attente de la banque</div>
        <button class="btn btn-outline-secondary btn-sm" :disabled="chargementCheques" @click="chargerCheques">
          <i class="bi bi-arrow-clockwise me-1"></i>Actualiser
        </button>
      </div>
      <div v-if="erreurCheques" class="alert alert-warning m-3">{{ erreurCheques }}</div>
      <div v-if="!cheques.length" class="vide">
        <i class="bi bi-inbox"></i>
        <p>Aucun chèque en attente. Les chèques sont normalement confirmés par l'import du relevé bancaire.</p>
      </div>
      <div v-else class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Remis le</th>
              <th>Contribuable</th>
              <th>Impôt</th>
              <th>Chèque</th>
              <th class="text-end">Montant</th>
              <th class="text-end">Décision manuelle</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in cheques" :key="c.idPaiement">
              <td class="small text-nowrap">{{ formatDate(c.datePaiement) }}</td>
              <td>
                <div class="fw-semibold">{{ c.raisonSociale }}</div>
                <small class="text-muted">NIF {{ c.nif }} · {{ c.referenceBv }}</small>
              </td>
              <td><span class="pastille">{{ c.impot }}</span> <small>{{ libellePeriode(c) }}</small></td>
              <td class="small">n° {{ c.numeroCheque }}<br /><span class="text-muted">{{ c.banque }}</span></td>
              <td class="text-end fw-bold text-nowrap">{{ formatMontant(c.montant) }} Ar</td>
              <td class="text-end text-nowrap">
                <button class="btn btn-sm btn-success me-1" :disabled="decision === c.idPaiement" @click="confirmer(c)">
                  <i class="bi bi-check-lg"></i> Payé
                </button>
                <button class="btn btn-sm btn-outline-danger" :disabled="decision === c.idPaiement" @click="rejeter(c)">
                  <i class="bi bi-x-lg"></i> Impayé
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pied small text-muted">
        <i class="bi bi-info-circle me-1"></i>
        Un chèque impayé annule le bordereau : la dette reprend avec ses pénalités depuis la date limite, et la relance automatique la reprend.
      </div>
    </div>
  </DgiLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import DgiLayout from '@/layouts/DgiLayout.vue'
import BordereauOfficiel from '@/components/BordereauOfficiel.vue'
import QuittanceOfficielle from '@/components/QuittanceOfficielle.vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { telechargerPdf } from '@/utils/pdf'

const auth = useAuthStore()
const messageServeur = (e, defaut) => e.response?.data?.message || defaut

const ONGLETS = [
  { cle: 'ENCAISSER', libelle: 'Encaisser', icone: 'bi-cash-stack' },
  { cle: 'DECLARER', libelle: 'Déclarer', icone: 'bi-file-earmark-plus' },
  { cle: 'CHEQUES', libelle: 'Chèques en attente', icone: 'bi-journal-check' },
]
const onglet = ref('ENCAISSER')

const STATUTS_BV = {
  A_PAYER: { libelle: 'À payer', classe: 'bg-primary' },
  EN_ATTENTE_ENCAISSEMENT: { libelle: 'Chèque en attente', classe: 'bg-warning text-dark' },
  PAYE: { libelle: 'Payé', classe: 'bg-success' },
  ANNULE: { libelle: 'Annulé', classe: 'bg-secondary' },
}

// ---------- Encaisser ----------
const saisie = ref('')
const bv = ref(null)
const aPayer = ref(null)
const nifRecherche = ref('')
const recherche = ref(false)
const ouverture = ref(null)
const erreurBv = ref('')
const paiement = reactive({ mode: 'ESPECES', montantRecu: null, numeroCheque: '', banque: '' })
const encaissement = ref(false)
const erreurEncaissement = ref('')
const quittance = ref(null)

const montantExact = computed(() => bv.value && Number(paiement.montantRecu) === Number(bv.value.montant))

/** Une référence de bordereau commence par BV- ; sinon la saisie est un NIF. */
async function rechercher() {
  erreurBv.value = ''
  quittance.value = null
  bv.value = null
  aPayer.value = null
  const valeur = saisie.value.trim()
  if (/^BV-/i.test(valeur)) {
    await chercherBordereau(valeur)
  } else {
    await chercherParNif(valeur)
  }
}

async function chercherBordereau(reference) {
  recherche.value = true
  try {
    const { data } = await api.get(`/dgi/guichet/bordereaux/${encodeURIComponent(reference.toUpperCase())}`)
    afficherBordereau(data)
    saisie.value = data.reference
  } catch (e) {
    erreurBv.value = messageServeur(e, 'Bordereau introuvable ou serveur indisponible.')
  } finally {
    recherche.value = false
  }
}

async function chercherParNif(nif) {
  recherche.value = true
  try {
    const { data } = await api.get(`/dgi/guichet/contribuables/${encodeURIComponent(nif)}/a-payer`)
    nifRecherche.value = nif
    aPayer.value = data
    if (data.length === 1) await ouvrirBordereau(data[0])
  } catch (e) {
    erreurBv.value = messageServeur(e, 'Contribuable introuvable ou serveur indisponible.')
  } finally {
    recherche.value = false
  }
}

/** Bordereau en cours de la déclaration : le même tant qu'il est valable, jamais un doublon. */
async function ouvrirBordereau(e) {
  erreurBv.value = ''
  quittance.value = null
  ouverture.value = e.idDeclaration
  try {
    const { data } = await api.get(`/dgi/guichet/contribuables/${encodeURIComponent(nifRecherche.value)}/declarations/${e.idDeclaration}/bordereau`)
    afficherBordereau(data)
  } catch (err) {
    erreurBv.value = messageServeur(err, 'Impossible d\'obtenir le bordereau.')
  } finally {
    ouverture.value = null
  }
}

function afficherBordereau(data) {
  bv.value = data
  Object.assign(paiement, { mode: 'ESPECES', montantRecu: Number(data.montant), numeroCheque: '', banque: '' })
  erreurEncaissement.value = ''
}

async function encaisser() {
  const libelle = paiement.mode === 'CHEQUE' ? 'la remise du chèque' : 'l\'encaissement en espèces'
  if (!confirm(`Confirmer ${libelle} de ${formatMontant(paiement.montantRecu)} Ar ?`)) return
  erreurEncaissement.value = ''
  encaissement.value = true
  try {
    const { data } = await api.post('/dgi/guichet/encaissements', {
      referenceBv: bv.value.reference,
      modePaiement: paiement.mode,
      montantRecu: paiement.montantRecu,
      numeroCheque: paiement.mode === 'CHEQUE' ? paiement.numeroCheque : null,
      banque: paiement.mode === 'CHEQUE' ? paiement.banque : null,
      matriculeAgent: auth.user?.matricule,
    })
    quittance.value = data
    aPayer.value = null
    if (data.modePaiement === 'CHEQUE') chargerCheques()
  } catch (e) {
    erreurEncaissement.value = messageServeur(e, 'L\'encaissement a échoué.')
  } finally {
    encaissement.value = false
  }
}

function nouvelEncaissement() {
  quittance.value = null
  bv.value = null
  aPayer.value = null
  saisie.value = ''
}

// ---------- Déclarer ----------
const aujourdHui = new Date()
const moisPrecedent = new Date(aujourdHui.getFullYear(), aujourdHui.getMonth() - 1, 1)
const MOIS = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre']

const decl = reactive({ nif: '', codeImpot: '', annee: null, mois: null, base: null, tvaDeductible: null })
const impots = ref([])
const chargementImpots = ref(false)
const erreurImpots = ref('')
const simulation = ref(null)
const calcul = ref(false)
const envoiDecl = ref(false)
const erreurDecl = ref('')
const resultatDecl = ref(null)

const impotChoisi = computed(() => impots.value.find((i) => i.code === decl.codeImpot))
const mensuel = computed(() => impotChoisi.value?.periodicite?.toUpperCase() === 'MENSUELLE')
const annees = computed(() => {
  const derniere = mensuel.value ? aujourdHui.getFullYear() : aujourdHui.getFullYear() - 1
  return Array.from({ length: 5 }, (_, i) => derniere - i)
})

watch(mensuel, (m) => {
  decl.annee = m ? moisPrecedent.getFullYear() : aujourdHui.getFullYear() - 1
  decl.mois = m ? moisPrecedent.getMonth() + 1 : null
}, { immediate: true })

// Toute modification de la saisie invalide le calcul affiché
watch(() => [decl.codeImpot, decl.annee, decl.mois, decl.base, decl.tvaDeductible], () => {
  simulation.value = null
  erreurDecl.value = ''
})

async function chargerImpots() {
  erreurImpots.value = ''
  impots.value = []
  resultatDecl.value = null
  chargementImpots.value = true
  try {
    const { data } = await api.get(`/dgi/guichet/contribuables/${encodeURIComponent(decl.nif)}/impots-declarables`)
    impots.value = data
    decl.codeImpot = data[0]?.code || ''
    if (!data.length) erreurImpots.value = 'Ce contribuable n\'est assujetti à aucun impôt.'
  } catch (e) {
    erreurImpots.value = messageServeur(e, 'Contribuable introuvable.')
  } finally {
    chargementImpots.value = false
  }
}

function requeteDecl() {
  return {
    nif: decl.nif,
    codeImpot: decl.codeImpot,
    annee: decl.annee,
    mois: mensuel.value ? decl.mois : null,
    base: decl.base,
    tvaDeductible: impotChoisi.value?.avecTvaDeductible ? (decl.tvaDeductible || 0) : null,
    matriculeAgent: auth.user?.matricule,
  }
}

async function simuler() {
  erreurDecl.value = ''
  calcul.value = true
  try {
    const { data } = await api.post('/dgi/guichet/declarations/simulation', requeteDecl())
    simulation.value = data
  } catch (e) {
    erreurDecl.value = messageServeur(e, 'Le calcul a échoué.')
  } finally {
    calcul.value = false
  }
}

async function declarer() {
  envoiDecl.value = true
  erreurDecl.value = ''
  try {
    const { data } = await api.post('/dgi/guichet/declarations', requeteDecl())
    resultatDecl.value = data
    decl.base = null
    decl.tvaDeductible = null
  } catch (e) {
    erreurDecl.value = messageServeur(e, 'La déclaration a échoué.')
  } finally {
    envoiDecl.value = false
  }
}

// Le contribuable paie tout de suite : on passe à l'encaissement avec son bordereau
async function encaisserMaintenant() {
  saisie.value = resultatDecl.value.bordereau.reference
  onglet.value = 'ENCAISSER'
  await rechercher()
}

// ---------- Téléchargement PDF ----------
const docBordereau = ref(null)
const docQuittance = ref(null)
const docDeclaration = ref(null)
const telechargement = ref(false)

async function telecharger(quoi) {
  const docs = {
    bordereau: [docBordereau, bv.value?.reference],
    quittance: [docQuittance, quittance.value?.referencePaiement],
    declaration: [docDeclaration, resultatDecl.value?.bordereau?.reference],
  }
  const [doc, nom] = docs[quoi]
  telechargement.value = true
  try {
    await telechargerPdf(doc.value?.feuille, nom || 'document')
  } finally {
    telechargement.value = false
  }
}

// ---------- Chèques en attente ----------
const cheques = ref([])
const chargementCheques = ref(false)
const erreurCheques = ref('')
const decision = ref(null)

onMounted(chargerCheques)

async function chargerCheques() {
  chargementCheques.value = true
  erreurCheques.value = ''
  try {
    const { data } = await api.get('/dgi/guichet/cheques-en-attente')
    cheques.value = data
  } catch {
    erreurCheques.value = 'Impossible de charger les chèques en attente.'
  } finally {
    chargementCheques.value = false
  }
}

async function confirmer(c) {
  if (!confirm(`La banque a payé le chèque n° ${c.numeroCheque} de ${c.raisonSociale} ?`)) return
  await decider(c, 'confirmer')
}

async function rejeter(c) {
  if (!confirm(`Le chèque n° ${c.numeroCheque} de ${c.raisonSociale} est impayé ? La dette reprendra avec ses pénalités.`)) return
  await decider(c, 'rejeter')
}

async function decider(c, action) {
  decision.value = c.idPaiement
  erreurCheques.value = ''
  try {
    await api.post(`/dgi/guichet/cheques/${c.idPaiement}/${action}`)
    await chargerCheques()
  } catch (e) {
    erreurCheques.value = messageServeur(e, 'L\'opération a échoué.')
  } finally {
    decision.value = null
  }
}

// ---------- Commun ----------
function libellePeriode(l) {
  const p = l.periode || ''
  let m
  if ((m = p.match(/^M(\d{1,2})$/))) return `${MOIS[Number(m[1]) - 1]} ${l.annee ?? ''}`.trim()
  if ((m = p.match(/^A(\d{4})$/))) return `Exercice ${m[1]}`
  return `${p} ${l.annee ?? ''}`.trim()
}
const formatMontant = (v) => Number(v || 0).toLocaleString('fr-FR', { maximumFractionDigits: 0 })
const formatDate = (iso) => (iso ? new Date(iso).toLocaleDateString('fr-FR') : '—')
</script>

<style scoped>
.entete-page { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
.entete-icone { width: 46px; height: 46px; border-radius: 12px; background: #0f2a4a; color: #fff; display: grid; place-items: center; font-size: 1.35rem; }
.onglets { background: #fff; padding: 4px; border-radius: 10px; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.06); border: 1px solid #e6eaf0; }
.onglets .nav-link { color: #475467; border-radius: 7px; padding: 0.45rem 0.95rem; font-size: 0.88rem; font-weight: 600; }
.onglets .nav-link.active { background: #0f2a4a; color: #fff; }

.carte { background: #fff; border: 1px solid #e6eaf0; border-radius: 12px; padding: 1.25rem; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04); }
.carte-titre { font-weight: 700; font-size: 1rem; margin-bottom: 0.9rem; color: #101828; }
.carte-titre .bi { color: #2563eb; margin-right: 0.35rem; }
.aide { font-size: 0.8rem; color: #667085; margin-top: 0.6rem; }

.liste-a-payer { list-style: none; padding: 0; margin: 0; }
.liste-a-payer li { display: flex; align-items: center; gap: 0.75rem; padding: 0.7rem 0.5rem; border-bottom: 1px solid #f0f2f5; border-radius: 8px; }
.liste-a-payer li:last-child { border-bottom: 0; }
.liste-a-payer li.actif { background: #eff4ff; }
.pastille { display: inline-block; min-width: 46px; text-align: center; font-size: 0.72rem; font-weight: 700; padding: 0.25rem 0.4rem; border-radius: 6px; background: #f2f4f7; color: #344054; border: 1px solid #e4e7ec; }

.montant-encaisser { display: flex; justify-content: space-between; align-items: baseline; background: #f8fafc; border: 1px solid #e6eaf0; border-radius: 10px; padding: 0.8rem 1rem; margin-bottom: 1rem; }
.montant-encaisser span { font-size: 0.8rem; color: #667085; text-transform: uppercase; letter-spacing: 0.4px; font-weight: 600; }
.montant-encaisser strong { font-size: 1.5rem; color: #0f2a4a; }

.calcul { border: 1px solid #c7d7fe; background: #eff4ff; border-radius: 10px; padding: 0.8rem 1rem; margin-bottom: 1rem; color: #1d3a8a; }
.calcul.retard { border-color: #fedf89; background: #fffaeb; color: #93370d; }

.document { padding: 0; overflow: hidden; }
.document-barre { display: flex; justify-content: space-between; align-items: center; gap: 0.75rem; flex-wrap: wrap; padding: 0.85rem 1.25rem; border-bottom: 1px solid #e6eaf0; background: #fff; }
.document-fond { background: #eef1f5; padding: 1.25rem; min-height: 420px; }
.document-fond :deep(.bvo-feuille), .document-fond :deep(.qo-feuille) { box-shadow: 0 4px 18px rgba(16, 24, 40, 0.12); }

.vide { text-align: center; color: #667085; padding: 4rem 1.5rem; }
.vide .bi { font-size: 2.4rem; display: block; margin-bottom: 0.6rem; color: #98a2b3; }
.vide p { margin-bottom: 0.3rem; }
.pied { padding: 0.85rem 1.25rem; border-top: 1px solid #e6eaf0; }
</style>
