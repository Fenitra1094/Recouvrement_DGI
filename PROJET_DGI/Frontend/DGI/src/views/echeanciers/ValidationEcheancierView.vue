<template>
  <DgiLayout v-slot="{ role }">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h3 class="fw-bold mb-0">🗓️ Instruction & Validation des Échéanciers</h3>
        <p class="text-muted mb-0 small">
          Demandes de paiement échelonné (moratoire) — instruction agent + décision Chef de Centre.
        </p>
      </div>
    </div>

    <!-- ===================== LISTE DES DEMANDES ===================== -->
    <div class="card border-0 shadow-sm mb-4">
      <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
        <h5 class="mb-0 fw-bold">📋 Demandes en attente ({{ demandes.length }})</h5>
        <span class="badge bg-warning text-dark">Critères objectifs CPF / SSIF</span>
      </div>
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>Contribuable</th>
                <th>Dette concernée</th>
                <th>Demande</th>
                <th>Statut</th>
                <th class="text-end">Action</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="d in demandes"
                :key="d.id"
                :class="{ 'table-active': demandeActive?.id === d.id }"
                style="cursor: pointer"
                @click="ouvrirDossier(d)"
              >
                <td>
                  <div class="fw-bold">{{ d.raisonSociale }}</div>
                  <small class="text-muted">NIF : {{ d.nif }}</small>
                </td>
                <td>
                  <span class="badge bg-light text-dark border">{{ d.typeImpot }}</span>
                  <div class="small text-muted">{{ d.periode }}</div>
                  <div class="fw-bold text-danger">{{ d.montant.toLocaleString() }} Ar</div>
                </td>
                <td>
                  <div class="fw-bold">{{ d.nbMois }} mensualités</div>
                  <small class="text-muted">Soumis le {{ d.dateDemande }}</small>
                </td>
                <td>
                  <span :class="['badge', statutBadge(d.statut)]">{{ d.statut }}</span>
                </td>
                <td class="text-end">
                  <button class="btn btn-sm btn-outline-primary fw-bold" @click.stop="ouvrirDossier(d)">
                    📂 Instruire
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ===================== DOSSIER D'INSTRUCTION ===================== -->
    <div v-if="demandeActive" class="card border-0 shadow-sm border-top border-primary border-4">
      <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
        <div>
          <h5 class="mb-0 fw-bold">Dossier : {{ demandeActive.raisonSociale }}</h5>
          <small class="text-muted">NIF {{ demandeActive.nif }} — Demande n° {{ demandeActive.reference }}</small>
        </div>
         <router-link
            class="btn btn-sm btn-outline-dark fw-bold"
            :to="`/dgi/contribuables/${demandeActive.nif}`"
          >
            📂 Ouvrir la fiche complète (historique)
          </router-link>
        <button class="btn btn-sm btn-outline-secondary" @click="demandeActive = null">Fermer</button>
      </div>

      <div class="card-body">
        <div class="row g-4">
          <!-- Colonne gauche : infos + motif -->
          <div class="col-md-5">
            <h6 class="fw-bold text-uppercase text-muted mb-3">1. Détail de la demande</h6>

            <div class="mb-3 p-3 bg-light rounded-3">
              <div class="d-flex justify-content-between mb-2">
                <span class="text-muted">Impôt</span>
                <strong>{{ demandeActive.typeImpot }} ({{ demandeActive.periode }})</strong>
              </div>
              <div class="d-flex justify-content-between mb-2">
                <span class="text-muted">Montant dû</span>
                <strong class="text-danger">{{ demandeActive.montant.toLocaleString() }} Ar</strong>
              </div>
              <div class="d-flex justify-content-between mb-2">
                <span class="text-muted">Mensualités demandées</span>
                <strong>{{ demandeActive.nbMois }} mois</strong>
              </div>
              <div class="d-flex justify-content-between">
                <span class="text-muted">Mensualité simulée</span>
                <strong class="text-primary">
                  {{ Math.round(demandeActive.montant / demandeActive.nbMois).toLocaleString() }} Ar / mois
                </strong>
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label fw-bold">Motif du contribuable</label>
              <div class="border rounded-3 p-3 bg-white small">
                {{ demandeActive.motif }}
              </div>
            </div>

            <!-- Plan d'échéances simulé -->
            <h6 class="fw-bold text-uppercase text-muted mb-2">Plan d'échéances proposé</h6>
            <table class="table table-sm table-bordered mb-0">
              <thead class="table-light">
                <tr>
                  <th>N°</th>
                  <th>Échéance</th>
                  <th class="text-end">Montant</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="n in demandeActive.nbMois" :key="n">
                  <td>{{ n }}</td>
                  <td>Mois +{{ n }}</td>
                  <td class="text-end fw-bold">
                    {{ Math.round(demandeActive.montant / demandeActive.nbMois).toLocaleString() }} Ar
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Colonne droite : critères objectifs + décision -->
          <div class="col-md-7">
            <h6 class="fw-bold text-uppercase text-muted mb-3">2. Grille de critères objectifs</h6>

            <div class="list-group mb-4">
              <label
                v-for="c in criteres"
                :key="c.id"
                class="list-group-item list-group-item-action d-flex align-items-start gap-3"
              >
                <input
                  type="checkbox"
                  class="form-check-input mt-1"
                  v-model="c.ok"
                  :disabled="role === 'AGENT' && c.reserveChef"
                />
                <div class="flex-grow-1">
                  <div class="fw-bold">{{ c.libelle }}</div>
                  <small class="text-muted">{{ c.description }}</small>
                  <div v-if="c.auto" class="mt-1">
                    <span class="badge" :class="c.ok ? 'bg-success' : 'bg-danger'">
                      {{ c.ok ? '✓ Contrôle système OK' : '✗ Non conforme' }}
                    </span>
                  </div>
                </div>
              </label>
            </div>

            <!-- Score -->
            <div class="alert mb-4" :class="tousCriteresOk ? 'alert-success' : 'alert-warning'">
              <strong>Score critères :</strong>
              {{ criteresOkCount }} / {{ criteres.length }}
              <span v-if="tousCriteresOk"> — Éligible à la validation</span>
              <span v-else> — Au moins un critère bloque la validation</span>
            </div>

            <!-- Note d'instruction (Agent) -->
            <div class="mb-3" v-if="role === 'AGENT' || role === 'CHEF_CENTRE'">
              <label class="form-label fw-bold">3. Note d'instruction (Agent)</label>
              <textarea
                class="form-control"
                rows="2"
                v-model="noteInstruction"
                placeholder="Ex: Dossier complet, pas d'ATD en cours, capacité de remboursement plausible..."
              ></textarea>
            </div>

            <!-- Décision Chef -->
            <div v-if="role === 'CHEF_CENTRE'" class="border-top pt-3">
              <label class="form-label fw-bold">4. Décision du Chef de Centre</label>
              <div class="d-flex flex-wrap gap-2">
                <button
                  class="btn btn-success fw-bold px-4"
                  :disabled="!tousCriteresOk"
                  @click="valider"
                >
                  ✅ Valider l'échéancier
                </button>
                <button class="btn btn-outline-danger fw-bold px-4" @click="rejeter">
                  ❌ Rejeter
                </button>
                <button class="btn btn-outline-secondary" @click="demandeActive = null">
                  Plus tard
                </button>
              </div>
              <div v-if="!tousCriteresOk" class="form-text text-danger mt-2">
                La validation est bloquée tant que tous les critères objectifs ne sont pas satisfaits.
              </div>
            </div>

            <!-- Action Agent : transmettre au Chef -->
            <div v-if="role === 'AGENT'" class="border-top pt-3">
              <button class="btn btn-primary fw-bold px-4" @click="transmettreAuChef">
                📩 Transmettre au Chef de Centre (instruction terminée)
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Message si aucune sélection -->
    <div v-else class="text-center text-muted py-5">
      <div class="fs-1 mb-2">📂</div>
      <p class="mb-0">Sélectionnez une demande dans le tableau pour l'instruire.</p>
    </div>
  </DgiLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import DgiLayout from '@/layouts/DgiLayout.vue'

const demandeActive = ref(null)
const noteInstruction = ref('')

const demandes = ref([
  {
    id: 1,
    reference: 'ECH-2024-0012',
    nif: '123456789',
    raisonSociale: 'Boulangerie Mamy Sarl',
    typeImpot: 'IS (Impôt Synthétique)',
    periode: 'Année 2023',
    montant: 750000,
    nbMois: 3,
    dateDemande: '20/05/2024',
    statut: 'SOUMIS',
    motif:
      'Baisse temporaire du chiffre d\'affaires suite aux travaux de voirie devant le local. Demande de fractionnement en 3 mois.'
  },
  {
    id: 2,
    reference: 'ECH-2024-0015',
    nif: '987654321',
    raisonSociale: 'Société Transport Express',
    typeImpot: 'TVA',
    periode: 'Mai 2024',
    montant: 1200000,
    nbMois: 4,
    dateDemande: '22/05/2024',
    statut: 'EN_INSTRUCTION',
    motif: 'Retard de paiement clients institutionnels. Trésorerie temporairement tendue.'
  }
])

// Critères objectifs (certains pré-cochés = contrôle automatique système)
const criteres = ref([
  {
    id: 1,
    libelle: 'Pas de procédure ATD en cours',
    description: 'Le contribuable ne fait pas l\'objet d\'un avis à tiers détenteur actif.',
    ok: true,
    auto: true,
    reserveChef: false
  },
  {
    id: 2,
    libelle: 'Pas d\'échéancier déjà en cours sur cette dette',
    description: 'Une seule plan de moratoire par dette à la fois.',
    ok: true,
    auto: true,
    reserveChef: false
  },
  {
    id: 3,
    libelle: 'Montant ≥ seuil minimal (200 000 Ar)',
    description: 'Les petits soldes se règlent en une fois.',
    ok: true,
    auto: true,
    reserveChef: false
  },
  {
    id: 4,
    libelle: 'Nombre de mois ≤ plafond (6 mois)',
    description: 'Conformément à la pratique du centre / circulaire interne.',
    ok: true,
    auto: true,
    reserveChef: false
  },
  {
    id: 5,
    libelle: 'Compte contribuable éligible (Actif, non radié)',
    description: 'Statut fiscal permettant un arrangement amiable.',
    ok: true,
    auto: true,
    reserveChef: false
  },
  {
    id: 6,
    libelle: 'Motif et capacité de remboursement jugés recevables',
    description: 'Appréciation de l\'instructeur / Chef de Centre.',
    ok: false,
    auto: false,
    reserveChef: false
  }
])

const criteresOkCount = computed(() => criteres.value.filter((c) => c.ok).length)
const tousCriteresOk = computed(() => criteres.value.every((c) => c.ok))

function statutBadge(s) {
  if (s === 'SOUMIS') return 'bg-secondary'
  if (s === 'EN_INSTRUCTION') return 'bg-info text-dark'
  if (s === 'VALIDE') return 'bg-success'
  if (s === 'REJETE') return 'bg-danger'
  return 'bg-light text-dark'
}

function ouvrirDossier(d) {
  demandeActive.value = d
  noteInstruction.value = ''
  // Reset critère "humain" à false à chaque ouverture (simulation)
  const humain = criteres.value.find((c) => c.id === 6)
  if (humain) humain.ok = false
}

function transmettreAuChef() {
  if (!noteInstruction.value.trim()) {
    alert('Merci de renseigner la note d\'instruction avant transmission.')
    return
  }
  demandeActive.value.statut = 'EN_INSTRUCTION'
  alert(
    `Instruction transmise au Chef de Centre pour ${demandeActive.value.raisonSociale}.\nNote : ${noteInstruction.value}`
  )
}

function valider() {
  if (!tousCriteresOk.value) return
  demandeActive.value.statut = 'VALIDE'
  alert(
    `✅ Échéancier VALIDÉ pour ${demandeActive.value.raisonSociale}.\n` +
      `Plan en ${demandeActive.value.nbMois} mois généré.\n` +
      `Notification envoyée sur l'espace contribuable.`
  )
  demandeActive.value = null
}

function rejeter() {
  const motif = prompt('Motif du rejet (obligatoire) :')
  if (!motif) return
  demandeActive.value.statut = 'REJETE'
  alert(`❌ Demande rejetée.\nMotif : ${motif}\nNotification envoyée au contribuable.`)
  demandeActive.value = null
}
</script>