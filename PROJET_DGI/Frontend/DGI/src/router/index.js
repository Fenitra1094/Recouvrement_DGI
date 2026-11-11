import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginDgiView from '../views/auth/LoginDgiView.vue'
import DashboardDgiView from '../views/dashboard/DashboardDgiView.vue'
import JournalRelancesView from '../views/relances/JournalRelancesView.vue'
import FicheContribuableView from '../views/contribuables/FicheContribuableView.vue'
import ValidationEcheancierView from '../views/echeanciers/ValidationEcheancierView.vue'
import ValidationVeilleuseView from '../views/veilleuse/ValidationVeilleuseView.vue'
import GestionAtdView from '../views/atd/GestionAtdView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: LoginDgiView },
    { path: '/dgi/dashboard', name: 'dashboard-dgi', component: DashboardDgiView, meta: { requiresAuth: true } },
    { path: '/dgi/relances', name: 'relances', component: JournalRelancesView, meta: { requiresAuth: true } },
    { path: '/dgi/contribuables/:nif?', name: 'fiche-contribuable', component: FicheContribuableView, meta: { requiresAuth: true } },
    { path: '/dgi/validations-echeanciers', name: 'validations-echeanciers', component: ValidationEcheancierView, meta: { requiresAuth: true } },
    { path: '/dgi/validations-veilleuse', name: 'validations-veilleuse', component: ValidationVeilleuseView, meta: { requiresAuth: true } },
    { path: '/dgi/atd', name: 'atd-dgi', component: GestionAtdView, meta: { requiresAuth: true } },

    // Secours
    { path: '/dgi/contribuables', component: DashboardDgiView, meta: { requiresAuth: true } },
    { path: '/dgi/paiements-especes', component: DashboardDgiView, meta: { requiresAuth: true } },
    { path: '/dgi/import-banque', component: DashboardDgiView, meta: { requiresAuth: true } },

    // Fallback
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

// Garde de navigation : login obligatoire
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && auth.isLoggedIn) {
    return '/dgi/dashboard'
  }
})

export default router
