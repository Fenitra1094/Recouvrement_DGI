import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import DashboardView from '@/views/contribuable/DashboardView.vue'
import DemandeEcheancierView from '@/views/contribuable/DemandeEcheancierView.vue'
import NotificationsView from '@/views/contribuable/NotificationsView.vue'
import HistoriqueView from '@/views/contribuable/HistoriqueView.vue'
import DemandeVeilleuseView from '@/views/contribuable/DemandeVeilleuseView.vue'
import LoginView from '@/views/auth/LoginView.vue'
import RegisterView from '@/views/auth/RegisterView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
      {
    path: '/',
    redirect: '/connexion'
  },
  {
    path: '/connexion',
    name: 'Login',
    component: LoginView,
    meta: { requiresGuest: true }
  },
  {
    path: '/inscription',
    name: 'Register',
    component: RegisterView,
    meta: { requiresGuest: true }
  },

    // Pages contribuable (protégées)
    { path: '/contribuable/dashboard', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
    { path: '/contribuable/echeancier', component: DemandeEcheancierView, meta: { requiresAuth: true } },
    { path: '/contribuable/notifications', component: NotificationsView, meta: { requiresAuth: true } },
    { path: '/contribuable/historique', name: 'historique', component: HistoriqueView, meta: { requiresAuth: true } },
    { path: '/contribuable/DemandeVeilleuse', name: 'DemandeVeilleuse', component: DemandeVeilleuseView, meta: { requiresAuth: true } },

    // Fallback
    { path: '/:pathMatch(.*)*', redirect: '/connexion' },
  ],
})

// Garde de navigation : redirige vers /connexion si non authentifié
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { path: '/connexion', query: { redirect: to.fullPath } }
  }
  // Déjà connecté : pas besoin de revoir connexion/inscription
  if ((to.path === '/connexion' || to.path === '/inscription') && auth.isAuthenticated) {
    return '/contribuable/dashboard'
  }
})

export default router
