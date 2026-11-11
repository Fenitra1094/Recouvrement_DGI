import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8085/api',
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 15000
})

// Intercepteur pour injecter automatiquement le token dans chaque requête
api.interceptors.request.use(
  (config) => {
    const authData = localStorage.getItem('dgi_contribuable_auth')
    if (authData) {
      try {
        const parsed = JSON.parse(authData)
        if (parsed.token) {
          config.headers.Authorization = `Bearer ${parsed.token}`
        }
      } catch (e) {
        console.error('Erreur lecture session:', e)
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Intercepteur en cas d'erreur 401 (Session expirée / Non autorisé)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('dgi_contribuable_auth')
      if (window.location.pathname !== '/connexion') {
        window.location.href = '/connexion'
      }
    }
    return Promise.reject(error)
  }
)

export default api