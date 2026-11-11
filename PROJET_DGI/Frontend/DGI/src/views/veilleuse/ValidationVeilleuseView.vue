<template>
  <DgiLayout v-slot="{ role }">
    <!-- En-tête de page -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h3 class="fw-bold mb-0">🌙 Validation des Mises en Veilleuse Dossier</h3>
        <p class="text-muted mb-0 small">
          Instruction des demandes de suspension temporaire d'activité et gel des poursuites (Art. CPF).
        </p>
      </div>
      <div class="d-flex gap-2">
        <span class="badge bg-warning text-dark px-3 py-2 fs-6">
          {{ demandesAttente.length }} demande(s) en attente
        </span>
      </div>
    </div>

    <!-- Carte synthétique d'impact SSIF -->
    <div class="row g-3 mb-4">
      <div class="col-md-4">
        <div class="card border-0 shadow-sm border-start border-warning border-4">
          <div class="card-body py-3">
            <div class="text-muted small text-uppercase fw-bold">Demandes à instruire</div>
            <div class="fs-4 fw-bold text-dark">{{ demandesAttente.length }} dossier(s)</div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card border-0 shadow-sm border-start border-success border-4">
          <div class="card-body py-3">
            <div class="text-muted small text-uppercase fw-bold">Dossiers actuellement en Veilleuse</div>
            <div class="fs-4 fw-bold text-success">14 contribuables</div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card border-0 shadow-sm border-start border-secondary border-4">
          <div class="card-body py-3">
            <div class="text-muted small text-uppercase fw-bold">Effet sur le moteur fiscal</div>
            <div class="small text-muted fw-bold">Relances & Pénalités gelées pendant la période</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tableau de validation -->
    <div class="card border-0 shadow-sm">
      <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
        <h5 class="mb-0 fw-bold">📋 Demandes soumises par les contribuables</h5>
        <span class="badge bg-light text-dark border">Décision réservée au Chef de Centre</span>
      </div>

      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>Contribuable (NIF)</th>
                <th>Motif & Explication</th>
                <th>Durée sollicitée</th>
                <th>Justificatif joint</th>
                <th>Statut</th>
                <th class="text-end">Actions DGI</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in demandes" :key="item.id">
                <!-- NIF & Raison Sociale -->
                <td>
                  <div class="fw-bold text-dark">{{ item.raisonSociale }}</div>
                  <small class="text-muted">NIF : <code>{{ item.nif }}</code></small>
                </td>

                <!-- Motif -->
                <td>
                  <span class="badge bg-secondary mb-1">{{ libelleMotif(item.typeMotif) }}</span>
                  <div class="small text-muted text-truncate" style="max-width: 280px;" :title="item.explication">
                    {{ item.explication }}
                  </div>
                </td>

                <!-- Période et Durée -->
                <td>
                  <div class="fw-bold text-dark">{{ item.dureeMois }} mois</div>
                  <div class="small text-muted">Du {{ item.dateDebut }} au {{ item.dateFin }}</div>
                </td>

                <!-- Fichier PDF -->
                <td>
                  <button 
                    class="btn btn-sm btn-outline-danger fw-bold" 
                    @click="ouvrirPieceJointe(item)"
                  >
                    📄 {{ item.fichier }}
                  </button>
                </td>

                <!-- Statut -->
                <td>
                  <span :class="['badge px-3 py-2', badgeStatut(item.statut)]">
                    {{ item.statut }}
                  </span>
                </td>

                <!-- Actions réservées au Chef de Centre -->
                <td class="text-end">
                  <div class="d-flex justify-content-end align-items-center gap-1">
                    <!-- Bouton Fiche Dossier -->
                    <router-link
                      class="btn btn-sm btn-outline-dark fw-bold me-1"
                      :to="`/dgi/contribuables/${item.nif}`"
                      title="Consulter l'historique et la dette du contribuable"
                    >
                      👤 Dossier
                    </router-link>

                    <!-- Décisions du Chef -->
                    <template v-if="item.statut === 'EN_ATTENTE'">
                      <button 
                        v-if="role === 'CHEF_CENTRE'"
                        class="btn btn-sm btn-success fw-bold" 
                        @click="approuverVeilleuse(item)"
                      >
                        ✅ Accorder
                      </button>
                      <button 
                        v-if="role === 'CHEF_CENTRE'"
                        class="btn btn-sm btn-outline-danger" 
                        @click="rejeterVeilleuse(item)"
                      >
                        ❌ Rejeter
                      </button>
                      <span v-if="role === 'AGENT'" class="badge bg-light text-muted border small ms-1">
                        Lecture seule (Agent)
                      </span>
                    </template>
                    <template v-else>
                      <small class="text-muted">Traité</small>
                    </template>
                  </div>
                </td>
              </tr>
              <tr v-if="!demandes.length">
                <td colspan="6" class="text-center text-muted py-4">
                  Aucune demande de mise en veilleuse enregistrée.
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

// Données fictives de demandes soumises par les contribuables
const demandes = ref([
  {
    id: 1,
    nif: '123456789',
    raisonSociale: 'Boulangerie Mamy Sarl',
    typeMotif: 'CESSATION_TEMPORAIRE',
    explication: 'Arrêt de production pour travaux de mise aux normes sanitaires et réfection du local.',
    dateDebut: '01/06/2024',
    dateFin: '30/11/2024',
    dureeMois: 6,
    fichier: 'PV_AG_Cessation.pdf',
    statut: 'EN_ATTENTE'
  },
  {
    id: 2,
    nif: '987654321',
    raisonSociale: 'Société Transport Express',
    typeMotif: 'SINISTRE_FORCE_MAJEURE',
    explication: 'Inondation majeure des entrepôts — destruction des véhicules de transport.',
    dateDebut: '15/05/2024',
    dateFin: '15/08/2024',
    dureeMois: 3,
    fichier: 'Constat_Huissier_Sinistre.pdf',
    statut: 'EN_ATTENTE'
  },
  {
    id: 3,
    nif: '456789123',
    raisonSociale: 'Hôtel Les Palmiers',
    typeMotif: 'PROCEDURE_COLLECTIVE',
    explication: 'Ouverture d\'une procédure de redressement judiciaire par le Tribunal de Commerce.',
    dateDebut: '01/01/2024',
    dateFin: '31/12/2024',
    dureeMois: 12,
    fichier: 'Jugement_Tribunal_Commerce.pdf',
    statut: 'APPROUVE'
  }
]);

const demandesAttente = computed(() => demandes.value.filter(d => d.statut === 'EN_ATTENTE'));

function libelleMotif(type) {
  if (type === 'CESSATION_TEMPORAIRE') return 'Cessation temporaire';
  if (type === 'SINISTRE_FORCE_MAJEURE') return 'Sinistre / Force majeure';
  if (type === 'PROCEDURE_COLLECTIVE') return 'Procédure collective';
  return 'Autre motif';
}

function badgeStatut(statut) {
  if (statut === 'EN_ATTENTE') return 'bg-warning text-dark';
  if (statut === 'APPROUVE') return 'bg-success';
  if (statut === 'REJETE') return 'bg-danger';
  return 'bg-secondary';
}

function ouvrirPieceJointe(item) {
  alert(`📄 Consultation du justificatif fourni par le contribuable :\n\nFichier : ${item.fichier}\nContribuable : ${item.raisonSociale} (NIF: ${item.nif})`);
}

function approuverVeilleuse(item) {
  if (confirm(`Confirmer la mise en veilleuse pour "${item.raisonSociale}" pour une durée de ${item.dureeMois} mois ?\n\nCela va geler l'émission des relances et le calcul des pénalités.`)) {
    item.statut = 'APPROUVE';
    alert(
      `✅ Mise en veilleuse ACCORDÉE pour ${item.raisonSociale}.\n` +
      `• Statut contribuable ➔ VEILLEUSE\n` +
      `• Moteur de relance ➔ SUSPENDU jusqu'au ${item.dateFin}\n` +
      `• Notification web transmise à l'usager.`
    );
  }
}

function rejeterVeilleuse(item) {
  const motif = prompt("Saisir le motif officiel du rejet :");
  if (!motif) return;
  
  item.statut = 'REJETE';
  alert(`❌ Demande de mise en veilleuse REJETÉE.\nMotif : ${motif}\nNotification transmise à l'usager.`);
}
</script>