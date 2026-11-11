<template>
  <div class="min-vh-100" style="background-color: #f4f6f9;">
    <!-- En-tête officiel DGI (Bleu Marine Ministère) -->
    <nav class="navbar navbar-expand-lg navbar-dark shadow-sm" style="background-color: #0f2a4a;">
      <div class="container">
        <a class="navbar-brand fw-bold d-flex align-items-center gap-2" href="#">
          <span class="logo-dgi"><i class="bi bi-bank2"></i></span>
          <div>
            <span class="d-block lh-1 fw-extrabold tracking-wide">HETRA-RINDRA</span>
            <small class="fs-8 text-white-50 fw-normal">Direction Générale des Impôts</small>
          </div>
        </a>
        <div class="d-flex align-items-center text-white">
          <div class="bg-white bg-opacity-10 px-3 py-1 rounded-pill me-3 border border-light border-opacity-25 small">
            <i class="bi bi-person-circle me-1"></i> {{ auth.user?.raisonSociale || 'Invité' }}
            <span class="text-white-50 ms-2">NIF {{ auth.user?.nif || '—' }}</span>
          </div>
          <button class="btn btn-outline-light btn-sm fw-bold px-3" @click="deconnexion">
            Déconnexion
          </button>
        </div>
      </div>
    </nav>

    <!-- Menu Secondaire (Pills Bleus) -->
    <div class="bg-white shadow-sm mb-4 border-bottom">
      <div class="container">
        <ul class="nav nav-pills py-2 flex-wrap gap-1">
          <li class="nav-item">
            <router-link
              to="/contribuable/dashboard"
              class="nav-link fw-semibold text-secondary"
              active-class="active text-white bg-blue-dgi"
            >
              <i class="bi bi-grid-1x2 me-2"></i>Tableau de bord
            </router-link>
          </li>
          <li class="nav-item">
            <router-link
              to="/contribuable/echeancier"
              class="nav-link fw-semibold text-secondary"
              active-class="active text-white bg-blue-dgi"
            >
              <i class="bi bi-calendar2-range me-2"></i>Demande d'échéancier
            </router-link>
          </li>
          <li class="nav-item">
            <router-link
              to="/contribuable/DemandeVeilleuse"
              class="nav-link fw-semibold text-secondary"
              active-class="active text-white bg-blue-dgi"
            >
              <i class="bi bi-pause-circle me-2"></i>Mise en veilleuse
            </router-link>
          </li>
          <li class="nav-item">
            <router-link
              to="/contribuable/notifications"
              class="nav-link fw-semibold text-secondary"
              active-class="active text-white bg-blue-dgi"
            >
              <i class="bi bi-bell me-2"></i>Notifications
              <span v-if="notifications.nonLues" class="badge bg-danger ms-1">{{ notifications.nonLues }}</span>
            </router-link>
          </li>
          <li class="nav-item">
            <router-link
              to="/contribuable/historique"
              class="nav-link fw-semibold text-secondary"
              active-class="active text-white bg-blue-dgi"
            >
              <i class="bi bi-clock-history me-2"></i>Historique
            </router-link>
          </li>
        </ul>
      </div>
    </div>

    <!-- Contenu de la page -->
    <div class="container pb-5">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationsStore } from '@/stores/notifications'

const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationsStore()

onMounted(() => notifications.chargerNonLues())

function deconnexion() {
  auth.logout()
  router.push('/connexion')
}
</script>

<style scoped>
.logo-dgi {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  font-size: 1.15rem;
}
.fs-8 {
  font-size: 0.725rem;
}
.tracking-wide {
  letter-spacing: 0.05em;
}

.nav-link {
  border-radius: 8px;
  padding: 0.5rem 1rem;
  transition: all 0.2s ease;
}

.nav-link:hover:not(.active) {
  background-color: #f1f5f9;
  color: #0f2a4a !important;
}

/* Classe de fond actif personnalisée Bleu Ministère */
.bg-blue-dgi {
  background-color: #2563eb !important; /* Bleu Roi moderne */
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.25);
}
</style>
