import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/contribuable/DashboardView.vue'
import DemandeEcheancierView from '../views/contribuable/DemandeEcheancierView.vue'
import NotificationsView from '../views/contribuable/NotificationsView.vue'
import HistoriqueView from '../views/contribuable/HistoriqueView.vue'
import DemandeVeilleuseView from '../views/contribuable/DemandeVeilleuseView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/contribuable/dashboard' },
    { path: '/contribuable/dashboard', component: DashboardView },
    { path: '/contribuable/echeancier', component: DemandeEcheancierView },
    { path: '/contribuable/notifications', component: NotificationsView },
    { 
      path: '/contribuable/historique', 
      name: 'historique', 
      component: HistoriqueView 
    },
    { 
      path: '/contribuable/DemandeVeilleuse', 
      name: 'DemandeVeilleuse', 
      component: DemandeVeilleuseView 
    }

    
  ]
})

export default router