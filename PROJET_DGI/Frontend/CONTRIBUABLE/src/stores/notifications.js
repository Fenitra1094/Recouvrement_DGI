import { defineStore } from 'pinia'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'

// Nombre de notifications non lues, partagé entre le menu (badge) et la page Notifications
export const useNotificationsStore = defineStore('notifications', {
  state: () => ({
    nonLues: 0,
  }),

  actions: {
    async chargerNonLues() {
      const auth = useAuthStore()
      if (!auth.contribuableNif) return
      try {
        const { data } = await api.get(`/contribuable/notifications/${encodeURIComponent(auth.contribuableNif)}/non-lues`)
        this.nonLues = data.nonLues
      } catch {
        // Badge non critique : on garde la dernière valeur connue
      }
    },
  },
})
