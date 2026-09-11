<template>
  <div class="min-vh-100" style="background-color: #f4f6f9;">
    <!-- En-tête officiel DGI -->
    <nav class="navbar navbar-expand-lg navbar-dark" style="background-color: #005f36;"> <!-- Vert DGI -->
      <div class="container">
        <a class="navbar-brand fw-bold" href="#">
          🏛️ DGI | HETRA-ONLINE V2
        </a>
        <div class="d-flex align-items-center text-white">
          <span class="me-3 fw-medium">👤 M. RAKOTO Jean (NIF: 123456789)</span>
          <button class="btn btn-outline-light btn-sm">Déconnexion</button>
        </div>
      </div>
    </nav>

    <!-- Menu Secondaire -->
    <div class="bg-white shadow-sm mb-4 border-bottom">
      <div class="container">
        <ul class="nav nav-pills py-3">
            <li class="nav-item">
                <router-link to="/contribuable/dashboard" class="nav-link fw-bold text-dark" active-class="active text-white bg-success">
                📊 Mon Tableau de Bord
                </router-link>
            </li>
            <li class="nav-item">
                <router-link to="/contribuable/echeancier" class="nav-link fw-bold text-dark" active-class="active text-white bg-success">
                🗓️ Demande de Moratoire
                </router-link>
            </li>
            <li class="nav-item">
                <router-link to="/contribuable/notifications" class="nav-link fw-bold text-dark" active-class="active text-white bg-success">
                🔔 Notifications <span class="badge bg-danger">2</span>
                </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/contribuable/historique" class="nav-link text-dark" active-class="active text-white bg-success">
                📜 Historique Unifié
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/contribuable/DemandeVeilleuse" class="nav-link text-dark" active-class="active text-white bg-success">
                📜 DemandeVeilleuse
              </router-link>
            </li>
            </ul>
      </div>
    </div>

    <!-- Le contenu de la page va s'afficher ici -->
    <div class="container pb-5">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.nav-link {
  border-radius: 8px;
  margin-right: 10px;
}
</style>