<template>
  <ContribuableLayout>
    <div class="row justify-content-center">
      <div class="col-md-10">
        
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-header bg-white py-3">
            <h4 class="mb-0 fw-bold text-dark">🗓️ Demande d'Échéancier de Paiement (Moratoire)</h4>
            <p class="text-muted mb-0 mt-1">Soumettez une demande pour payer vos dettes en plusieurs fois.</p>
          </div>
          
          <div class="card-body p-4">
            <form @submit.prevent="soumettreDemande">
              
              <!-- Sélection de la dette -->
              <div class="mb-4">
                <label class="form-label fw-bold">1. Dette concernée</label>
                <select class="form-select form-select-lg" v-model="detteSelectionnee">
                  <option value="750000">IS (Impôt Synthétique) - Année 2023 - Reste : 750 000 Ar</option>
                  <option value="500000">TVA - Mai 2024 - Reste : 500 000 Ar</option>
                </select>
              </div>

              <!-- Motif de la demande -->
              <div class="mb-4">
                <label class="form-label fw-bold">2. Motif de votre demande</label>
                <textarea class="form-control" rows="3" placeholder="Expliquez brièvement vos difficultés de trésorerie..." required></textarea>
              </div>

              <!-- Nombre de mensualités -->
              <div class="mb-4">
                <label class="form-label fw-bold">3. Nombre d'échéances souhaitées</label>
                <select class="form-select" v-model="nombreMois">
                  <option value="2">2 mois</option>
                  <option value="3">3 mois</option>
                  <option value="4">4 mois</option>
                  <option value="6">6 mois</option>
                </select>
              </div>

              <hr class="my-4">

              <!-- Simulation automatique du plan de remboursement -->
              <div class="bg-light p-4 rounded-3 border mb-4">
                <h5 class="fw-bold mb-3">📊 Simulation de votre plan de remboursement</h5>
                <div class="table-responsive">
                  <table class="table table-sm table-borderless mb-0">
                    <tbody>
                      <tr v-for="mois in Number(nombreMois)" :key="mois">
                        <td class="text-secondary">Échéance N°{{ mois }}</td>
                        <td class="fw-bold text-end">{{ (detteSelectionnee / nombreMois).toLocaleString(undefined, { maximumFractionDigits: 0 }) }} Ar</td>
                      </tr>
                      <tr class="border-top border-2">
                        <td class="fw-bold pt-2">TOTAL</td>
                        <td class="fw-bold text-end text-danger pt-2">{{ Number(detteSelectionnee).toLocaleString() }} Ar</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <div class="text-end">
                <button type="button" class="btn btn-outline-secondary me-2 px-4">Annuler</button>
                <button type="submit" class="btn btn-warning fw-bold px-4 text-dark shadow-sm">🚀 Soumettre la demande à la DGI</button>
              </div>

            </form>
          </div>
        </div>

      </div>
    </div>
  </ContribuableLayout>
</template>

<script setup>
import { ref } from 'vue';
import ContribuableLayout from '@/layouts/ContribuableLayout.vue';

// Variables réactives pour la simulation en direct
const detteSelectionnee = ref(750000); // Par défaut IS 750 000
const nombreMois = ref(3); // Par défaut 3 mois

function soumettreDemande() {
  alert("Demande envoyée avec succès ! Elle est en attente de validation par le Chef de Centre.");
}
</script>