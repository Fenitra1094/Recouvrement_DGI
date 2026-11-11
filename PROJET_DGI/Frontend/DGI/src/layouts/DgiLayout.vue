<template>
  <div class="d-flex min-vh-100">

    <!-- ========================================================= -->
    <!-- SIDEBAR DGI (Barre latérale dynamique selon le Rôle)       -->
    <!-- ========================================================= -->
    <div class="bg-dgi-dark text-white p-3 d-flex flex-column flex-shrink-0" style="width: 270px;">

      <!-- En-tête Sidebar -->
      <div class="d-flex align-items-center mb-4 px-2">
        <span class="fs-4 fw-bold text-white">
          🦅 SIG-SSIF
          <small class="d-block fs-7 text-secondary fw-normal">Direction Générale des Impôts</small>
        </span>
      </div>

      <!-- Informations utilisateur connecté -->
      <div class="p-2 mb-3 bg-dark rounded-3 border border-secondary border-opacity-25">
        <div class="d-flex align-items-center">
          <div class="badge bg-primary p-2 me-2">
            <i class="bi bi-person-fill fs-5"></i>
          </div>
          <div>
            <div class="fw-bold fs-7">{{ auth.user?.nomComplet || 'Agent' }}</div>
            <div class="small text-secondary">{{ auth.user?.matricule }}</div>
            <div class="badge" :class="roleActuel === 'CHEF_CENTRE' ? 'bg-success' : 'bg-info'">
              {{ roleActuel === 'CHEF_CENTRE' ? 'Chef de Centre' : 'Agent de Guichet' }}
            </div>
          </div>
        </div>
      </div>

      <!-- Liens de Navigation -->
      <div class="nav flex-column gap-1 mb-auto">

        <!-- Menu Général -->
        <div class="text-uppercase fs-8 text-secondary fw-bold px-2 mt-2 mb-1">Général</div>

        <router-link to="/dgi/dashboard" class="sidebar-link" active-class="active">
          <i class="bi bi-speedometer2"></i> Tableau de Bord
        </router-link>

        <router-link to="/dgi/contribuables" class="sidebar-link" active-class="active">
          <i class="bi bi-people"></i> Fiches Contribuables
        </router-link>

        <!-- Relance automatique : consultable par l'agent comme par le chef de centre -->
        <router-link to="/dgi/relances" class="sidebar-link" active-class="active">
          <i class="bi bi-send-exclamation"></i> Relances automatiques
        </router-link>

        <!-- Menu AGENT -->
        <template v-if="roleActuel === 'AGENT'">
          <div class="text-uppercase fs-8 text-secondary fw-bold px-2 mt-3 mb-1">Opérations Guichet</div>

          <router-link to="/dgi/paiements-especes" class="sidebar-link" active-class="active">
            <i class="bi bi-cash-stack"></i> Saisie Caisse (Espèces)
          </router-link>
        </template>

        <!-- Menu CHEF DE CENTRE -->
        <template v-if="roleActuel === 'CHEF_CENTRE'">
          <div class="text-uppercase fs-8 text-secondary fw-bold px-2 mt-3 mb-1">Validations & Pilotage</div>

          <router-link to="/dgi/import-banque" class="sidebar-link" active-class="active">
            <i class="bi bi-file-earmark-spreadsheet"></i> Import Fichier Banque
          </router-link>

          <router-link to="/dgi/validations-echeanciers" class="sidebar-link" active-class="active">
            <i class="bi bi-people"></i> Validations échéanciers
          </router-link>

          <router-link to="/dgi/validations-veilleuse" class="sidebar-link" active-class="active">
            <i class="bi bi-moon-stars"></i> Mises en Veilleuse
            <span class="badge bg-warning text-dark ms-auto">2</span>
          </router-link>

          <router-link to="/dgi/atd" class="sidebar-link" active-class="active">
            <i class="bi bi-shield-lock"></i> Saisies ATD
          </router-link>
        </template>

      </div>

      <!-- Pied de Sidebar -->
      <div class="pt-3 border-top border-secondary border-opacity-25">
        <button class="btn btn-outline-light btn-sm w-100" @click="deconnexion">
          <i class="bi bi-box-arrow-right"></i> Déconnexion
        </button>
      </div>

    </div>

    <!-- ========================================================= -->
    <!-- CONTENU PRINCIPAL                                          -->
    <!-- ========================================================= -->
    <div class="flex-grow-1 overflow-auto">

      <!-- Topbar / Sélecteur de rôle pour démonstration -->
      <header class="bg-white shadow-sm px-4 py-2 d-flex justify-content-between align-items-center border-bottom">
        <div class="fw-bold text-secondary">
          🏛️ {{ auth.user?.centreFiscal || 'Centre fiscal' }}
        </div>

        <!-- 🧪 Sélecteur de Rôle (pour le Cahier des charges) -->
        <div class="d-flex align-items-center bg-light p-1 rounded border">
          <span class="small fw-bold me-2 ps-2">Simuler le Rôle :</span>
          <div class="btn-group btn-group-sm">
            <button
              class="btn"
              :class="roleActuel === 'AGENT' ? 'btn-primary' : 'btn-outline-secondary'"
              @click="changerRole('AGENT')"
            >
              Agent Guichet
            </button>
            <button
              class="btn"
              :class="roleActuel === 'CHEF_CENTRE' ? 'btn-success' : 'btn-outline-secondary'"
              @click="changerRole('CHEF_CENTRE')"
            >
              Chef de Centre
            </button>
          </div>
        </div>
      </header>

      <!-- Emplacement du contenu des pages -->
      <main class="p-4">
        <slot :role="roleActuel"></slot>
      </main>

    </div>

  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

// Rôle initial = rôle du compte connecté (modifiable via le simulateur)
const roleActuel = ref(auth.role || localStorage.getItem('dgi_role') || 'AGENT')

watch(() => auth.user, () => {
  roleActuel.value = auth.role || 'AGENT'
})

function changerRole(nouveauRole) {
  roleActuel.value = nouveauRole
  localStorage.setItem('dgi_role', nouveauRole)
}

function deconnexion() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.bg-dgi-dark {
  background: linear-gradient(180deg, #0f2a4a 0%, #16324f 100%);
}
.fs-7 { font-size: 0.85rem; }
.fs-8 { font-size: 0.75rem; }

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.6rem 0.8rem;
  border-radius: 8px;
  color: #cbd5e1;
  text-decoration: none;
  font-weight: 500;
  font-size: 0.9rem;
  transition: all 0.2s ease;
}

.sidebar-link:hover:not(.active) {
  background-color: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.sidebar-link.active {
  background-color: #2563eb;
  color: #ffffff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}
</style>
