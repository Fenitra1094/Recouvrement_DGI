<template>
  <ContribuableLayout>
    <div class="row">
      <div class="col-md-12">
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
            <h4 class="mb-0 fw-bold text-dark">🔔 Journal des Notifications</h4>
            <button class="btn btn-sm btn-outline-primary">Tout marquer comme lu</button>
          </div>
          
          <div class="card-body p-0">
            <div class="list-group list-group-flush">
              
              <!-- Notification 1 : Mise en demeure (Urgent) -->
              <div class="list-group-item p-4 bg-light border-start border-danger border-4">
                <div class="d-flex w-100 justify-content-between mb-2">
                  <h6 class="mb-0 fw-bold text-danger">⚠️ MISE EN DEMEURE - Impayé IS 2023</h6>
                  <small class="text-muted fw-bold">Aujourd'hui, 08:30</small>
                </div>
                <p class="mb-1 text-dark">
                  Malgré notre précédente relance, nous constatons que la somme de 750 000 Ar reste impayée. 
                  Veuillez régulariser votre situation sous 8 jours avant procédure de recouvrement forcé (ATD).
                </p>
                <span class="badge bg-danger rounded-pill">Non lu</span>
              </div>

              <!-- Notification 2 : Validation d'un paiement (Succès) -->
              <div class="list-group-item p-4 border-start border-success border-4">
                <div class="d-flex w-100 justify-content-between mb-2">
                  <h6 class="mb-0 fw-bold text-success">✅ Paiement Validé et Facture Disponible</h6>
                  <small class="text-muted">Hier, 14:15</small>
                </div>
                <p class="mb-1 text-secondary">
                  Votre paiement de 250 000 Ar par virement bancaire a été rapproché avec succès. 
                  Votre quittance N° QUITUS-2024-0899 est disponible en téléchargement.
                </p>
                <button class="btn btn-sm btn-link p-0 text-decoration-none fw-bold mt-2">📥 Télécharger le reçu</button>
              </div>

              <!-- Notification 3 : Relance simple -->
              <div class="list-group-item p-4 border-start border-warning border-4">
                <div class="d-flex w-100 justify-content-between mb-2">
                  <h6 class="mb-0 fw-bold text-dark">ℹ️ Rappel d'échéance proche : TVA Mai 2024</h6>
                  <small class="text-muted">12 Mai 2024</small>
                </div>
                <p class="mb-1 text-secondary">
                  Nous vous rappelons que la date limite de paiement pour la TVA de Mai 2024 est fixée au 15 Juin 2024.
                </p>
              </div>

            </div>
          </div>
        </div>
      </div>
    </div>
  </ContribuableLayout>
</template>

<script setup>
import ContribuableLayout from '@/layouts/ContribuableLayout.vue';
</script>