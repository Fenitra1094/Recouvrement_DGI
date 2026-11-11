<template>
  <ContribuableLayout>
    <div class="row justify-content-center">
      <div class="col-md-9">
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white py-3">
            <h4 class="mb-0 fw-bold text-dark">💤 Demande de Mise en Veilleuse (Mise en Sommeil)</h4>
            <p class="text-muted mb-0 small">Demandez la suspension temporaire de vos obligations fiscales en cas d'arrêt d'activité.</p>
          </div>
          <div class="card-body p-4">
            
            <form @submit.prevent="soumettreVeilleuse">
              <div class="mb-3">
                <label class="form-label fw-bold">1. Date de début de suspension souhaitée</label>
                <input type="date" class="form-control" required>
              </div>

              <div class="mb-3">
                <label class="form-label fw-bold">2. Date de fin prévue (Reprise d'activité)</label>
                <input type="date" class="form-control" required>
              </div>

              <div class="mb-3">
                <label class="form-label fw-bold">3. Motif de la suspension d'activité</label>
                <textarea class="form-control" rows="3" placeholder="Ex: Travaux de rénovation, fermeture saisonnière..." required></textarea>
              </div>

              <div class="mb-4">
                <label class="form-label fw-bold">4. Justificatif officiel (PV d'Assemblée Générale, Acte...)</label>
                <input type="file" class="form-control" accept=".pdf, .png, .jpg" required>
                <div class="form-text">Format PDF ou image requis.</div>
              </div>

              <div class="alert alert-warning border-0 bg-light-warning">
                <i class="bi bi-exclamation-triangle-fill text-warning me-2"></i>
                <strong>Attention :</strong> La mise en veilleuse suspend les déclarations futures mais ne supprime pas les dettes fiscales antérieures à la date de début.
              </div>

              <div class="text-end">
                <button type="submit" class="btn btn-warning fw-bold px-4 text-dark shadow-sm">
                  🚀 Transmettre la demande au Chef de Centre DGI
                </button>
              </div>
            </form>

          </div>
        </div>
      </div>
    </div>
  </ContribuableLayout>
</template>

<script setup>
import ContribuableLayout from '@/layouts/ContribuableLayout.vue';

function soumettreVeilleuse() {
  alert("Demande transmise avec succès ! Elle est en attente d'instruction et de validation par le Chef de Centre.");
}
</script>