<template>
  <DgiLayout v-slot="{ role }">
    <!-- En-tête -->
    <div class="d-flex justify-content-between align-items-start mb-4 flex-wrap gap-3">
      <div>
        <button class="btn btn-sm btn-outline-secondary mb-2" @click="$router.back()">
          ← Retour
        </button>
        <h3 class="fw-bold mb-1">👤 Fiche Contribuable</h3>
        <p class="text-muted mb-0 small">
          Dossier de décision — instruction échéancier / recouvrement
        </p>
      </div>
      <div class="text-end">
        <span :class="['badge px-3 py-2 fs-6', badgeStatut(contribuable.statut)]">
          {{ contribuable.statut }}
        </span>
        <div class="mt-2">
          <span :class="['badge px-3 py-2', badgeRisque(scoreRisque)]">
            Risque : {{ scoreRisque }}
          </span>
        </div>
      </div>
    </div>

    <!-- ===================== IDENTITÉ ===================== -->
    <div class="row g-3 mb-4">
      <div class="col-md-8">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body">
            <div class="row">
              <div class="col-md-6">
                <div class="text-muted small text-uppercase fw-bold mb-1">Raison sociale</div>
                <div class="fw-bold fs-5">{{ contribuable.raisonSociale }}</div>
                <div class="text-muted">NIF : <code>{{ contribuable.nif }}</code></div>
              </div>
              <div class="col-md-6">
                <div class="text-muted small text-uppercase fw-bold mb-1">Régime fiscal</div>
                <div class="fw-bold">{{ contribuable.regime }}</div>
                <div class="text-muted small">{{ contribuable.adresse }}</div>
                <div class="text-muted small">Tél : {{ contribuable.telephone }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card border-0 shadow-sm h-100 border-start border-danger border-4">
          <div class="card-body">
            <div class="text-muted small text-uppercase fw-bold mb-1">Solde total restant dû</div>
            <h2 class="text-danger fw-bolder mb-1">
              {{ soldeTotal.toLocaleString() }} Ar
            </h2>
            <small class="text-muted">
              {{ dettes.length }} dette(s) · {{ dettesEnRetard }} en retard
            </small>
          </div>
        </div>
      </div>
    </div>

    <!-- ===================== INDICATEURS DÉCISION ===================== -->
    <div class="card border-0 shadow-sm mb-4">
      <div class="card-header bg-white py-3">
        <h5 class="mb-0 fw-bold">🎯 Indicateurs pour décision d'échéancier</h5>
      </div>
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-3" v-for="ind in indicateurs" :key="ind.label">
            <div class="p-3 rounded-3 border h-100" :class="ind.ok ? 'border-success bg-success bg-opacity-10' : 'border-danger bg-danger bg-opacity-10'">
              <div class="fw-bold">{{ ind.ok ? '✅' : '❌' }} {{ ind.label }}</div>
              <small class="text-muted">{{ ind.detail }}</small>
            </div>
          </div>
        </div>
        <div class="alert mt-3 mb-0" :class="scoreRisque === 'FAIBLE' ? 'alert-success' : scoreRisque === 'MOYEN' ? 'alert-warning' : 'alert-danger'">
          <strong>Recommandation système :</strong> {{ recommandation }}
        </div>
      </div>
    </div>

    <!-- ===================== ONGLETS ===================== -->
    <ul class="nav nav-tabs mb-3">
      <li class="nav-item" v-for="tab in tabs" :key="tab.id">
        <button
          class="nav-link"
          :class="{ active: onglet === tab.id }"
          @click="onglet = tab.id"
        >
          {{ tab.label }}
        </button>
      </li>
    </ul>

    <!-- DETTES -->
    <div v-show="onglet === 'dettes'" class="card border-0 shadow-sm mb-4">
      <div class="card-body p-0">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Impôt / Période</th>
              <th>Date limite</th>
              <th>Principal</th>
              <th>Pénalités</th>
              <th>Payé</th>
              <th>Solde</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in dettes" :key="d.id">
              <td>
                <div class="fw-bold">{{ d.type }}</div>
                <small class="text-muted">{{ d.periode }}</small>
              </td>
              <td>{{ d.dateLimite }}</td>
              <td>{{ d.principal.toLocaleString() }} Ar</td>
              <td class="text-danger">{{ d.penalites.toLocaleString() }} Ar</td>
              <td class="text-success">{{ d.paye.toLocaleString() }} Ar</td>
              <td class="fw-bold text-danger">{{ d.solde.toLocaleString() }} Ar</td>
              <td>
                <span :class="['badge', d.statut === 'EN RETARD' ? 'bg-danger' : d.statut === 'ECHELONNE' ? 'bg-info text-dark' : 'bg-secondary']">
                  {{ d.statut }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- HISTORIQUE PAIEMENTS -->
    <div v-show="onglet === 'paiements'" class="card border-0 shadow-sm mb-4">
      <div class="card-body p-0">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Date</th>
              <th>Réf. BV / Transaction</th>
              <th>Mode</th>
              <th>Montant</th>
              <th>Impôt lié</th>
              <th>Statut</th>
              <th>Quitus</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in paiements" :key="p.id">
              <td>{{ p.date }}</td>
              <td><code>{{ p.reference }}</code></td>
              <td><span class="badge bg-light text-dark border">{{ p.mode }}</span></td>
              <td class="fw-bold">{{ p.montant.toLocaleString() }} Ar</td>
              <td>{{ p.impot }}</td>
              <td>
                <span :class="['badge', p.statut === 'VALIDE' ? 'bg-success' : 'bg-warning text-dark']">
                  {{ p.statut }}
                </span>
              </td>
              <td>
                <button v-if="p.quitus" class="btn btn-sm btn-outline-success" @click="alert('Téléchargement quitus ' + p.quitus)">
                  📄 PDF
                </button>
                <span v-else class="text-muted">—</span>
              </td>
            </tr>
            <tr v-if="!paiements.length">
              <td colspan="7" class="text-center text-muted py-4">Aucun paiement enregistré</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- HISTORIQUE RELANCES -->
    <div v-show="onglet === 'relances'" class="card border-0 shadow-sm mb-4">
      <div class="card-body p-0">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Date</th>
              <th>Type d'acte</th>
              <th>Impôt</th>
              <th>Montant</th>
              <th>Signé par</th>
              <th>Statut envoi</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in relances" :key="r.id">
              <td>{{ r.date }}</td>
              <td>
                <span :class="['badge', r.type === 'MISE EN DEMEURE' ? 'bg-warning text-dark' : r.type === 'SOMMATION' ? 'bg-danger' : 'bg-info text-dark']">
                  {{ r.type }}
                </span>
              </td>
              <td>{{ r.impot }}</td>
              <td>{{ r.montant.toLocaleString() }} Ar</td>
              <td>{{ r.chef }}</td>
              <td>
                <span class="badge bg-success">{{ r.statut }}</span>
              </td>
            </tr>
            <tr v-if="!relances.length">
              <td colspan="6" class="text-center text-muted py-4">Aucune relance émise</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ANCIENS ÉCHÉANCIERS -->
    <div v-show="onglet === 'echeanciers'" class="card border-0 shadow-sm mb-4">
      <div class="card-body p-0">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Référence</th>
              <th>Période / Impôt</th>
              <th>Plan</th>
              <th>Respecté ?</th>
              <th>Décision</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in echeanciers" :key="e.id">
              <td><code>{{ e.ref }}</code></td>
              <td>{{ e.impot }}</td>
              <td>{{ e.nbMois }} mois — {{ e.montant.toLocaleString() }} Ar</td>
              <td>
                <span :class="['badge', e.respecte === 'OUI' ? 'bg-success' : e.respecte === 'NON' ? 'bg-danger' : 'bg-secondary']">
                  {{ e.respecte }}
                </span>
              </td>
              <td>{{ e.decision }}</td>
              <td>{{ e.date }}</td>
            </tr>
            <tr v-if="!echeanciers.length">
              <td colspan="6" class="text-center text-muted py-4">
                Aucun échéancier antérieur — premier moratoire demandé
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="echeanciers.some(e => e.respecte === 'NON')" class="card-footer bg-danger bg-opacity-10 text-danger small">
        ⚠️ Ce contribuable a déjà <strong>rompu un échéancier</strong> par le passé — critère défavorable.
      </div>
      <div v-else-if="echeanciers.some(e => e.respecte === 'OUI')" class="card-footer bg-success bg-opacity-10 text-success small">
        ✅ A déjà respecté un échéancier — critère favorable.
      </div>
    </div>

    <!-- ACTIONS RAPIDES (selon rôle) -->
    <div class="card border-0 shadow-sm">
      <div class="card-header bg-white py-3">
        <h5 class="mb-0 fw-bold">⚡ Actions rapides</h5>
      </div>
      <div class="card-body d-flex flex-wrap gap-2">
        <router-link
          class="btn btn-warning fw-bold text-dark"
          :to="'/dgi/validations-echeanciers'"
        >
          🗓️ Voir / Instruire la demande d'échéancier
        </router-link>
        <button class="btn btn-outline-primary fw-bold" @click="alert('Ouverture génération BV / quitus')">
          📄 Documents fiscaux
        </button>
        <button
          v-if="role === 'CHEF_CENTRE'"
          class="btn btn-outline-danger fw-bold"
          @click="alert('Escalade ATD — écran ATD')"
        >
          🛑 Escalader vers ATD
        </button>
      </div>
    </div>
  </DgiLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import DgiLayout from '@/layouts/DgiLayout.vue'

const route = useRoute()
// Plus tard : charger via API avec route.params.nif
const nifParam = route.params.nif || '123456789'

const onglet = ref('dettes')
const tabs = [
  { id: 'dettes', label: '💰 Dettes' },
  { id: 'paiements', label: '💳 Paiements' },
  { id: 'relances', label: '📨 Relances' },
  { id: 'echeanciers', label: '🗓️ Échéanciers passés' }
]

// ========== DONNÉES FICTIVES (maquette) ==========
const contribuable = ref({
  nif: nifParam,
  raisonSociale: 'Boulangerie Mamy Sarl',
  regime: 'Impôt Synthétique',
  adresse: 'Lot II M 45 Antaninarenina, Antananarivo',
  telephone: '034 00 000 00',
  statut: 'ACTIF' // ACTIF | VEILLEUSE | ATD | RADIE
})

const dettes = ref([
  {
    id: 1,
    type: 'IS (Impôt Synthétique)',
    periode: 'Année 2023',
    dateLimite: '15/05/2024',
    principal: 700000,
    penalites: 50000,
    paye: 0,
    solde: 750000,
    statut: 'EN RETARD'
  },
  {
    id: 2,
    type: 'TVA',
    periode: 'Mai 2024',
    dateLimite: '15/06/2024',
    principal: 500000,
    penalites: 0,
    paye: 0,
    solde: 500000,
    statut: 'A PAYER'
  }
])

const paiements = ref([
  {
    id: 1,
    date: '12/05/2024',
    reference: 'BV-2024-0412 / VIR-BOA-8891',
    mode: 'Virement Bancaire',
    montant: 450000,
    impot: 'TVA Avril 2024',
    statut: 'VALIDE',
    quitus: 'QUITUS-2024-0412'
  },
  {
    id: 2,
    date: '10/03/2024',
    reference: 'BV-2024-0201 / CAISSE-001',
    mode: 'Espèces (Caisse DGI)',
    montant: 200000,
    impot: 'IRSA Fév 2024',
    statut: 'VALIDE',
    quitus: 'QUITUS-2024-0201'
  }
])

const relances = ref([
  {
    id: 1,
    date: '01/06/2024',
    type: 'RELANCE SIMPLE',
    impot: 'IS 2023',
    montant: 750000,
    chef: 'Chef ANDRIA',
    statut: 'ENVOYÉ'
  }
])

const echeanciers = ref([
  // Cas A : jamais eu d'échéancier → tableau vide = favorable "premier moratoire"
  // Cas B : exemple d'un ancien respecté (décommente pour tester)
  // {
  //   id: 1,
  //   ref: 'ECH-2022-008',
  //   impot: 'TVA 2022',
  //   nbMois: 3,
  //   montant: 300000,
  //   respecte: 'OUI',
  //   decision: 'VALIDÉ',
  //   date: '15/08/2022'
  // }
])

// ========== CALCULS DÉCISION ==========
const soldeTotal = computed(() => dettes.value.reduce((s, d) => s + d.solde, 0))
const dettesEnRetard = computed(() => dettes.value.filter((d) => d.statut === 'EN RETARD').length)

const aDejaRompuEcheancier = computed(() =>
  echeanciers.value.some((e) => e.respecte === 'NON')
)
const aDejaRespecteEcheancier = computed(() =>
  echeanciers.value.some((e) => e.respecte === 'OUI')
)
const aPaiementsRecents = computed(() => paiements.value.length >= 1)
const pasATD = computed(() => contribuable.value.statut !== 'ATD')
const compteActif = computed(() => contribuable.value.statut === 'ACTIF')

const indicateurs = computed(() => [
  {
    label: 'Compte actif',
    detail: contribuable.value.statut,
    ok: compteActif.value
  },
  {
    label: 'Pas d\'ATD en cours',
    detail: pasATD.value ? 'Aucune saisie tiers' : 'ATD actif — refus probable',
    ok: pasATD.value
  },
  {
    label: 'Historique de paiement',
    detail: aPaiementsRecents.value
      ? `${paiements.value.length} paiement(s) validé(s)`
      : 'Aucun paiement trouvé',
    ok: aPaiementsRecents.value
  },
  {
    label: 'Échéanciers passés',
    detail: aDejaRompuEcheancier.value
      ? 'A déjà rompu un plan'
      : aDejaRespecteEcheancier.value
        ? 'A déjà respecté un plan'
        : 'Premier moratoire demandé',
    ok: !aDejaRompuEcheancier.value
  }
])

const scoreRisque = computed(() => {
  let score = 0
  if (!compteActif.value) score += 3
  if (!pasATD.value) score += 3
  if (!aPaiementsRecents.value) score += 2
  if (aDejaRompuEcheancier.value) score += 3
  if (dettesEnRetard.value >= 2) score += 1
  if (soldeTotal.value > 2000000) score += 1

  if (score <= 1) return 'FAIBLE'
  if (score <= 3) return 'MOYEN'
  return 'ÉLEVÉ'
})

const recommandation = computed(() => {
  if (scoreRisque.value === 'FAIBLE') {
    return 'Profil favorable — échéancier recommandable sous réserve des plafonds (durée / montant).'
  }
  if (scoreRisque.value === 'MOYEN') {
    return 'Profil mitigé — valider uniquement avec plan court (2–3 mois) et suivi renforcé.'
  }
  return 'Profil à risque — refus recommandé, ou orientation vers autre mesure (relance / ATD).'
})

function badgeStatut(s) {
  if (s === 'ACTIF') return 'bg-success'
  if (s === 'VEILLEUSE') return 'bg-secondary'
  if (s === 'ATD') return 'bg-danger'
  return 'bg-dark'
}

function badgeRisque(r) {
  if (r === 'FAIBLE') return 'bg-success'
  if (r === 'MOYEN') return 'bg-warning text-dark'
  return 'bg-danger'
}
</script>   