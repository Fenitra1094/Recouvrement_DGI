import { defineStore } from 'pinia'
import api from '@/services/api'

export const useAuthStore = defineStore('auth', {
  state: () => {
    const savedAuth = localStorage.getItem('dgi_contribuable_auth')
    return {
      user: savedAuth ? JSON.parse(savedAuth).user : null,
      token: savedAuth ? JSON.parse(savedAuth).token : null,
    }
  },

  getters: {
    isAuthenticated: (state) => !!state.user,
    contribuableNif: (state) => state.user?.nif || '',
  },

  actions: {
    async loginContribuable(identifiantOrObject, motDePasseParam) {
      let id = identifiantOrObject
      let pwd = motDePasseParam

      // Support si l'appelant passe un objet { identifiant, motDePasse }
      if (typeof identifiantOrObject === 'object' && identifiantOrObject !== null) {
        id = identifiantOrObject.identifiant || identifiantOrObject.email
        pwd = identifiantOrObject.motDePasse || identifiantOrObject.password
      }

      // Envoi du JSON propre au backend Spring Boot
      const response = await api.post('/auth/contribuable/login', { 
        identifiant: id, 
        email: id, 
        motDePasse: pwd 
      })
      const data = response.data

      this.user = data
      this.token = data.token || 'session_active'

      localStorage.setItem('dgi_contribuable_auth', JSON.stringify({
        user: this.user,
        token: this.token
      }))

      return data
    },

    async register(payload) {
      const response = await api.post('/auth/contribuable/register', payload)
      return response.data
    },

    logout() {
      this.user = null
      this.token = null
      localStorage.removeItem('dgi_contribuable_auth')
    }
  }
})