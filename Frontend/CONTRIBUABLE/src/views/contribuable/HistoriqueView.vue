<template>
  <ContribuableLayout>
    <div class="row">
      <div class="col-md-12">
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
            <div>
              <h4 class="mb-0 fw-bold text-dark">📜 Historique Unifié (Déclarations & Paiements)</h4>
              <p class="text-muted mb-0 small">Vue consolidée de l'ensemble de vos engagements fiscaux et règlements.</p>
            </div>
            <!-- Filtre rapide -->
            <div class="d-flex gap-2">
              <select class="form-select form-select-sm" v-model="filtreImpot">
                <option value="TOUS">Tous les impôts</option>
                <option value="TVA">TVA</option>
                <option value="IS">IS</option>
                <option value="IRSA">IRSA</option>
              </select>
            </div>
          </div>

          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                  <tr>
                    <th>Période / Impôt</th>
                    <th>Montant Déclaré</th>
                    <th>Montant Payé</th>
                    <th>Mode & Réf. Paiement</th>
                    <th>Statut Dossier</th>
                    <th class="text-end">Document / Action</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in historiqueFiltre" :key="item.id">
                    <!-- Déclaration -->
                    <td>
                      <div class="fw-bold text-dark">{{ item.typeImpot }}</div>
                      <small class="text-muted">Période : {{ item.periode }} | Déclaré le {{ item.dateDeclaration }}</small>
                    </td>

                    <!-- Montant Déclaré -->
                    <td class="fw-semibold">
                      {{ item.montantDeclare.toLocaleString() }} Ar
                    </td>

                    <!-- Montant Payé -->
                    <td :class="['fw-bold', item.montantPaye >= item.montantDeclare ? 'text-success' : 'text-danger']">
                      {{ item.montantPaye.toLocaleString() }} Ar
                    </td>

                    <!-- Info Paiement -->
                    <td>
                      <div v-if="item.referencePaiement" class="small">
                        <span class="badge bg-light text-dark border me-1">{{ item.modePaiement }}</span>
                        <code>{{ item.referencePaiement }}</code>
                        <div class="text-muted fs-7">Payé le {{ item.datePaiement }}</div>
                      </div>
                      <span v-else class="text-muted fst-italic small">Aucun paiement enregistré</span>
                    </td>

                    <!-- Statut consolidé -->
                    <td>
                      <span :class="['badge px-2 py-1', getBadgeClass(item.statut)]">
                        {{ item.statut }}
                      </span>
                    </td>

                    <!-- Action / Téléchargement Facture Quitus -->
                    <td class="text-end">
                      <button 
                        v-if="item.statut === 'SOLDE'" 
                        class="btn btn-sm btn-outline-success fw-bold"
                        @click="telechargerQuitus(item)"
                      >
                        📄 Quitus (PDF)
                      </button>
                      <button 
                        v-else-if="item.statut === 'EN ATTENTE VALIDATION'" 
                        class="btn btn-sm btn-outline-warning text-dark disabled"
                      >
                        ⏳ En cours de contrôle
                      </button>
                      <button 
                        v-else 
                        class="btn btn-sm btn-primary fw-bold"
                        @click="payerMaintenant(item)"
                      >
                        💳 Régulariser
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </ContribuableLayout>
</template>

<script setup>
import { ref, computed } from 'vue';
import ContribuableLayout from '@/layouts/ContribuableLayout.vue';

const filtreImpot = ref('TOUS');

// 🎭 DONNÉES FICTIVES UNIFIÉES (Déclaration + Paiement mariés ensemble)
const listeHistorique = ref([
  {
    id: 1,
    typeImpot: 'TVA - Taxe sur la Valeur Ajoutée',
    periode: 'Avril 2024',
    dateDeclaration: '10/05/2024',
    montantDeclare: 450000,
    montantPaye: 450000,
    modePaiement: 'e-Hetraphone (MVola)',
    referencePaiement: 'MVOLA-889921',
    datePaiement: '12/05/2024',
    statut: 'SOLDE'
  },
  {
    id: 2,
    typeImpot: 'IRSA - Impôt Salarial',
    periode: 'Mai 2024',
    dateDeclaration: '05/06/2024',
    montantDeclare: 200000,
    montantPaye: 200000,
    modePaiement: 'Virement Bancaire BOA',
    referencePaiement: 'VIR-2024-0012',
    datePaiement: '14/06/2024',
    statut: 'EN ATTENTE VALIDATION'
  },
  {
    id: 3,
    typeImpot: 'IS - Impôt Synthétique',
    periode: 'Année 2023',
    dateDeclaration: '15/03/2024',
    montantDeclare: 750000,
    montantPaye: 0,
    modePaiement: null,
    referencePaiement: null,
    datePaiement: null,
    statut: 'EN RETARD'
  }
]);

// Filtrage dynamique
const historiqueFiltre = computed(() => {
  if (filtreImpot.value === 'TOUS') return listeHistorique.value;
  return listeHistorique.value.filter(item => item.typeImpot.includes(filtreImpot.value));
});

function getBadgeClass(statut) {
  switch (statut) {
    case 'SOLDE': return 'bg-success';
    case 'EN ATTENTE VALIDATION': return 'bg-warning text-dark';
    case 'EN RETARD': return 'bg-danger';
    default: return 'bg-secondary';
  }
}

function telechargerQuitus(item) {
  alert(`Téléchargement de la Facture / Quitus Officiel PDF pour ${item.typeImpot} (${item.referencePaiement})`);
}

function payerMaintenant(item) {
  alert(`Redirection vers le paiement pour ${item.typeImpot}`);
}
</script>