<template>
  <ContribuableLayout>
    <div class="row">
      
      <!-- Carte Résumé de la Dette -->
      <div class="col-md-12 mb-4">
        <div class="card border-0 shadow-sm border-start border-danger border-5">
          <div class="card-body py-4">
            <h5 class="text-muted text-uppercase fw-bold mb-1">Solde Fiscal Restant Dû</h5>
            <h1 class="text-danger fw-bolder mb-2">{{ soldeGlobal.toLocaleString() }} Ar</h1>
            <div class="text-secondary">
              <span class="me-4"><strong>Principal :</strong> {{ principal.toLocaleString() }} Ar</span>
              <span><strong>Pénalités de retard :</strong> {{ penalites.toLocaleString() }} Ar</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Tableau des Impayés -->
      <div class="col-md-12">
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white py-3">
            <h5 class="mb-0 fw-bold">🧾 Détail de vos impôts en attente de paiement</h5>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                  <tr>
                    <th>Type d'Impôt</th>
                    <th>Période</th>
                    <th>Date Limite</th>
                    <th>Montant Dû</th>
                    <th>Statut</th>
                    <th class="text-end">Action</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="dette in listeDettes" :key="dette.id">
                    <td class="fw-bold">{{ dette.type }}</td>
                    <td>{{ dette.periode }}</td>
                    <td>{{ dette.dateLimite }}</td>
                    <td class="fw-bold fs-5">{{ dette.montant.toLocaleString() }} Ar</td>
                    <td>
                      <span :class="['badge px-3 py-2', dette.statut === 'EN RETARD' ? 'bg-danger' : 'bg-warning text-dark']">
                        {{ dette.statut }}
                      </span>
                    </td>
                    <td class="text-end">
                      <button class="btn btn-sm btn-primary fw-bold px-3" @click="simulerPaiement(dette)">
                        💳 Payer
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Boutons Globaux d'Action -->
      <div class="col-md-12 mt-4 text-end">
        <button class="btn btn-outline-secondary me-3 px-4 py-2 fw-bold">
          📄 Télécharger le relevé PDF
        </button>
        <button class="btn btn-warning px-4 py-2 fw-bold text-dark shadow-sm">
          🗓️ Demander un échéancier de paiement
        </button>
      </div>

    </div>
  </ContribuableLayout>
</template>

<script setup>
import { ref } from 'vue';
import ContribuableLayout from '@/layouts/ContribuableLayout.vue';

// 🎭 DONNÉES FICTIVES (Mock) pour le Cahier des Charges
const soldeGlobal = ref(1250000);
const principal = ref(1100000);
const penalites = ref(150000);

const listeDettes = ref([
  {
    id: 1,
    type: 'IS (Impôt Synthétique)',
    periode: 'Année 2023',
    dateLimite: '15/05/2024',
    montant: 750000,
    statut: 'EN RETARD'
  },
  {
    id: 2,
    type: 'TVA',
    periode: 'Mai 2024',
    dateLimite: '15/06/2024',
    montant: 500000,
    statut: 'A PAYER'
  }
]);

// Fonction "Fausse" pour la démo
function simulerPaiement(dette) {
  alert(`Simulation : Redirection vers e-Hetraphone pour payer ${dette.montant} Ar.`);
}
</script>

<style scoped>
.table th, .table td {
  padding: 1rem;
}
</style>