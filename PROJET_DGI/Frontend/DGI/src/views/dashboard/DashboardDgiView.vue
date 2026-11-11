<template>
  <DgiLayout v-slot="{ role }">
    
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h3 class="fw-bold mb-0">🏠 Tableau de Bord de Recouvrement</h3>
        <p class="text-muted mb-0 small">
          Vue d'ensemble {{ role === 'CHEF_CENTRE' ? 'MACPâscopique de pilotage du Centre' : 'Microscopique des opérations quotidiennes' }}
        </p>
      </div>
      <span class="badge bg-white text-dark shadow-sm border p-2">{{ dateDuJour }}</span>
    </div>

    <!-- ======================================================== -->
    <!-- 📊 STATISTIQUES CHEF DE CENTRE                           -->
    <!-- ======================================================== -->
    <div v-if="role === 'CHEF_CENTRE'" class="row g-3 mb-4">
      <div class="col-md-4">
        <div class="card border-0 shadow-sm bg-primary text-white">
          <div class="card-body py-4">
            <div class="text-white-50 text-uppercase fw-bold fs-7 mb-1">Reste à Recouvrer Total</div>
            <h2 class="fw-bolder mb-0">45 500 000 Ar</h2>
            <small class="text-white-50">124 Contribuables défaillants</small>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card border-0 shadow-sm bg-warning text-dark">
          <div class="card-body py-4">
            <div class="text-dark-50 text-uppercase fw-bold fs-7 mb-1">Relances en Attente</div>
            <h2 class="fw-bolder mb-0">4 Relances</h2>
            <small class="text-dark-50">À valider ce jour</small>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card border-0 shadow-sm bg-danger text-white">
          <div class="card-body py-4">
            <div class="text-white-50 text-uppercase fw-bold fs-7 mb-1">Procédures ATD Actives</div>
            <h2 class="fw-bolder mb-0">3 Saisies</h2>
            <small class="text-white-50">Comptes bancaires bloqués</small>
          </div>
        </div>
      </div>
    </div>

    <!-- ======================================================== -->
    <!-- 📊 STATISTIQUES AGENT GUICHET                            -->
    <!-- ======================================================== -->
    <div v-if="role === 'AGENT'" class="row g-3 mb-4">
      <div class="col-md-6">
        <div class="card border-0 shadow-sm border-start border-success border-5">
          <div class="card-body py-4">
            <div class="text-muted text-uppercase fw-bold fs-7 mb-1">Encaissements Caisse (Aujourd'hui)</div>
            <h2 class="fw-bolder text-success mb-0">1 250 000 Ar</h2>
            <small class="text-muted">5 Transactions effectuées</small>
          </div>
        </div>
      </div>
      <div class="col-md-6">
        <div class="card border-0 shadow-sm border-start border-primary border-5">
          <div class="card-body py-4">
            <div class="text-muted text-uppercase fw-bold fs-7 mb-1">Propositions de Relances transmises</div>
            <h2 class="fw-bolder text-primary mb-0">3 Dossiers</h2>
            <small class="text-muted">Envoyées au Chef de Centre</small>
          </div>
        </div>
      </div>
    </div>

    <!-- ======================================================== -->
    <!-- 📋 TABLEAU DES DERNIÈRES OPÉRATIONS DGI                 -->
    <!-- ======================================================== -->
    <div class="card border-0 shadow-sm">
      <div class="card-header bg-white py-3">
        <h5 class="mb-0 fw-bold">📉 Journal des Derniers Actes de Recouvrement</h5>
      </div>
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>NIF & Entreprise</th>
                <th>Acte / Opération</th>
                <th>Agent Initiateur</th>
                <th>Montant</th>
                <th>Statut</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <div class="fw-bold">123456789</div>
                  <small class="text-muted">Boulangerie Mamy</small>
                </td>
                <td>Paiement Caisse (Espèces) - IS 2023</td>
                <td>Agent RAKOTO</td>
                <td class="fw-bold">150 000 Ar</td>
                <td><span class="badge bg-success">Validé (Quitus Émis)</span></td>
              </tr>
              <tr>
                <td>
                  <div class="fw-bold">987654321</div>
                  <small class="text-muted">Hôtel Les Mangroves</small>
                </td>
                <td>Proposition Mise en Demeure</td>
                <td>Agent RANDRIA</td>
                <td class="fw-bold">750 000 Ar</td>
                <td><span class="badge bg-warning text-dark">En attente Chef</span></td>
              </tr>
              <tr>
                <td>
                  <div class="fw-bold">456123789</div>
                  <small class="text-muted">Société BTP Sarl</small>
                </td>
                <td>Demande de Mise en Veilleuse</td>
                <td>Contribuable</td>
                <td class="fw-bold">-</td>
                <td><span class="badge bg-warning text-dark">En attente Chef</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

  </DgiLayout>
</template>

<script setup>
import DgiLayout from '@/layouts/DgiLayout.vue';

const dateDuJour = new Date().toLocaleDateString('fr-FR', {
  weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
});
</script>