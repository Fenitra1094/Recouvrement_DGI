<template>
  <DgiLayout v-slot="{ role }">
    <!-- En-tête -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h3 class="fw-bold mb-0">🛑 Saisies & Avis à Tiers Détenteur (ATD)</h3>
        <p class="text-muted mb-0 small">
          Procédure exécutoire de recouvrement forcé — Blocage des comptes bancaires et saisies arrêt.
        </p>
      </div>
      <button 
        v-if="role === 'CHEF_CENTRE'" 
        class="btn btn-danger fw-bold shadow-sm"
        @click="nouvelAtd"
      >
        ⚡ Engager un nouvel ATD
      </button>
    </div>

    <!-- Statistiques Macroscopiques ATD -->
    <div class="row g-3 mb-4">
      <div class="col-md-3">
        <div class="card border-0 shadow-sm border-start border-danger border-4">
          <div class="card-body py-3">
            <div class="text-muted small text-uppercase fw-bold">ATD Actifs en cours</div>
            <div class="fs-4 fw-bold text-danger">{{ atdEnCours.length }} dossier(s)</div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card border-0 shadow-sm border-start border-warning border-4">
          <div class="card-body py-3">
            <div class="text-muted small text-uppercase fw-bold">Montant total à saisir</div>
            <div class="fs-4 fw-bold text-dark">{{ totalMontantASaisir.toLocaleString() }} Ar</div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card border-0 shadow-sm border-start border-success border-4">
          <div class="card-body py-3">
            <div class="text-muted small text-uppercase fw-bold">Recouvré via ATD (Mois)</div>
            <div class="fs-4 fw-bold text-success">4 500 000 Ar</div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card border-0 shadow-sm border-start border-info border-4">
          <div class="card-body py-3">
            <div class="text-muted small text-uppercase fw-bold">Mainlevées délivrées</div>
            <div class="fs-4 fw-bold text-info">8 dossiers éteints</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tableau de suivi des ATD -->
    <div class="card border-0 shadow-sm">
      <div class="card-header bg-white py-3">
        <h5 class="mb-0 fw-bold">📋 Registre des Avis à Tiers Détenteur émis</h5>
      </div>
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>Réf. ATD</th>
                <th>Contribuable (NIF)</th>
                <th>Dette visée</th>
                <th>Tiers notifiés (Banques)</th>
                <th>Date d'émission</th>
                <th>Statut</th>
                <th class="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in listeAtd" :key="item.id">
                <td><code>{{ item.reference }}</code></td>
                <td>
                  <div class="fw-bold text-dark">{{ item.raisonSociale }}</div>
                  <small class="text-muted">NIF : {{ item.nif }}</small>
                </td>
                <td>
                  <div class="fw-bold text-danger">{{ item.montantDette.toLocaleString() }} Ar</div>
                  <small class="text-muted">{{ item.typeImpot }}</small>
                </td>
                <td>
                  <div class="d-flex flex-wrap gap-1">
                    <span v-for="b in item.banques" :key="b" class="badge bg-light text-dark border">
                      🏦 {{ b }}
                    </span>
                  </div>
                </td>
                <td>{{ item.dateEmission }}</td>
                <td>
                  <span :class="['badge px-3 py-2', badgeStatut(item.statut)]">
                    {{ item.statut }}
                  </span>
                </td>
                <td class="text-end">
                  <div class="d-flex justify-content-end gap-1">
                    <!-- Fiche Contribuable -->
                    <router-link
                      class="btn btn-sm btn-outline-dark fw-bold"
                      :to="`/dgi/contribuables/${item.nif}`"
                    >
                      👤 Dossier
                    </router-link>

                    <!-- Télécharger l'acte d'ATD -->
                    <button 
                      class="btn btn-sm btn-outline-danger" 
                      @click="alert('Impression de l\'acte officiel d\'ATD N° ' + item.reference)"
                      title="Télécharger l'acte officiel adressé aux banques"
                    >
                      📄 Acte ATD
                    </button>

                    <!-- Accorder la Mainlevée si payé -->
                    <button 
                      v-if="role === 'CHEF_CENTRE' && item.statut === 'ACTIF'"
                      class="btn btn-sm btn-success fw-bold" 
                      @click="delivrerMainlevee(item)"
                      title="Annuler la saisie bancaire après règlement"
                    >
                      🔓 Mainlevée
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </DgiLayout>
</template>

<script setup>
import { ref, computed } from 'vue';
import DgiLayout from '@/layouts/DgiLayout.vue';

const listeAtd = ref([
  {
    id: 1,
    reference: 'ATD-2024-0089',
    nif: '987654321',
    raisonSociale: 'Société Transport Express',
    montantDette: 2400000,
    typeImpot: 'TVA + IS Cumulés',
    banques: ['BOA Madagascar', 'BNI Madagascar', 'BFV-SG'],
    dateEmission: '10/05/2024',
    statut: 'ACTIF'
  },
  {
    id: 2,
    reference: 'ATD-2024-0045',
    nif: '554433221',
    raisonSociale: 'Quincaillerie du Centre',
    montantDette: 1150000,
    typeImpot: 'IRSA 2023',
    banques: ['BMOI', 'BOA Madagascar'],
    dateEmission: '18/02/2024',
    statut: 'MAINLEVEE'
  }
]);

const atdEnCours = computed(() => listeAtd.value.filter(a => a.statut === 'ACTIF'));
const totalMontantASaisir = computed(() => atdEnCours.value.reduce((s, a) => s + a.montantDette, 0));

function badgeStatut(statut) {
  if (statut === 'ACTIF') return 'bg-danger';
  if (statut === 'MAINLEVEE') return 'bg-success';
  return 'bg-secondary';
}

function nouvelAtd() {
  const nif = prompt("Saisir le NIF du contribuable récalcitrant :");
  if (!nif) return;
  alert(`⚡ Procédure ATD engagée pour le NIF ${nif}.\nL'acte de saisie est généré et notifié aux établissements bancaires.`);
}

function delivrerMainlevee(item) {
  if (confirm(`Confirmer la délivrance de la MAINLEVÉE pour ${item.raisonSociale} ?\n\nCela atteste que la dette de ${item.montantDette.toLocaleString()} Ar a été éteinte. Les banques seront notifiées pour débloquer les comptes.`)) {
    item.statut = 'MAINLEVEE';
    alert(`🔓 MAINLEVÉE ACCORDÉE.\nLes comptes bancaires de ${item.raisonSociale} sont libérés de toute saisie.`);
  }
}
</script>