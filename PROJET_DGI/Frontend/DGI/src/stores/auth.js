import { defineStore } from 'pinia'
import api from '../services/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('dgi_auth') || 'null'),
  }),

  getters: {
    isLoggedIn: (state) => !!state.user,
    // 'AGENT' ou 'CHEF_CENTRE' (sans le préfixe ROLE_)
    role: (state) => (state.user?.role || '').replace('ROLE_', '') || null,
  },

  actions: {
    _save(user) {
      this.user = user
      localStorage.setItem('dgi_auth', JSON.stringify(user))
      // Compatibilité avec le sélecteur de rôle du layout
      localStorage.setItem('dgi_role', this.role || 'AGENT')
    },

    /**
     * Connexion d'un agent DGI.
     * `identifiant` accepte l'email OU le matricule.
     */
    async loginDgi(identifiant, motDePasse) {
      const { data } = await api.post('/auth/dgi/login', {
        email: identifiant,
        motDePasse,
      })
      this._save({
        matricule: data.nif,
        email: data.email,
        nomComplet: data.raisonSociale,
        role: data.role,
        centreFiscal: data.centreFiscal,
      })
      return data
    },

    logout() {
      this.user = null
      localStorage.removeItem('dgi_auth')
      localStorage.removeItem('dgi_role')
    },
  },
})
