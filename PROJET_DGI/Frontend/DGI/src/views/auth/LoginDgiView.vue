<template>
  <div class="dgi-portal-wrapper">

    <!-- Réseau de nœuds vectoriels (Côté Gauche) -->
    <div class="network-nodes left-nodes d-none d-lg-block">
      <svg width="220" height="500" viewBox="0 0 220 500" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M50 80 L120 150 L60 240 L130 330 L70 420" stroke="#a3bfdb" stroke-width="1.5" stroke-dasharray="3 3"/>
        <path d="M120 150 L190 200 L130 330" stroke="#a3bfdb" stroke-width="1.5"/>
        
        <!-- Nœud 1 : Graphique -->
        <g transform="translate(50, 80)">
          <circle r="22" fill="#ffffff" stroke="#2563eb" stroke-width="1.5"/>
          <path d="M-8 6 V-4 M-2 6 V-10 M4 6 V-1 M10 6 V-7" stroke="#2563eb" stroke-width="2" stroke-linecap="round"/>
        </g>
        <!-- Nœud 2 : Calendrier -->
        <g transform="translate(120, 150)">
          <circle r="22" fill="#ffffff" stroke="#2563eb" stroke-width="1.5"/>
          <path d="M-7 -6 H7 V8 H-7 Z M-4 -9 V-6 M4 -9 V-6 M-7 -1 H7" stroke="#2563eb" stroke-width="1.5"/>
        </g>
        <!-- Nœud 3 : Liste / Menu -->
        <g transform="translate(60, 240)">
          <circle r="22" fill="#ffffff" stroke="#2563eb" stroke-width="1.5"/>
          <path d="M-8 -5 H8 M-8 0 H8 M-8 5 H8" stroke="#2563eb" stroke-width="2" stroke-linecap="round"/>
        </g>
        <!-- Nœud 4 : Tendance / Courbe -->
        <g transform="translate(130, 330)">
          <circle r="22" fill="#ffffff" stroke="#2563eb" stroke-width="1.5"/>
          <path d="M-8 4 L-2 -3 L3 1 L8 -5" stroke="#2563eb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </g>
        <!-- Nœud 5 : Sécurité / Bouclier -->
        <g transform="translate(70, 420)">
          <circle r="22" fill="#ffffff" stroke="#2563eb" stroke-width="1.5"/>
          <path d="M0 -8 L7 -4 V2 C7 7 0 10 0 10 C0 10 -7 7 -7 2 V-4 Z" fill="none" stroke="#2563eb" stroke-width="1.5"/>
        </g>
      </svg>
    </div>

    <!-- Réseau de nœuds vectoriels (Côté Droit) -->
    <div class="network-nodes right-nodes d-none d-lg-block">
      <svg width="220" height="500" viewBox="0 0 220 500" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M170 90 L110 180 L180 270 L120 370 L170 460" stroke="#a3bfdb" stroke-width="1.5" stroke-dasharray="3 3"/>
        <path d="M110 180 L40 240 L120 370" stroke="#a3bfdb" stroke-width="1.5"/>

        <!-- Nœud 1 : Analytics -->
        <g transform="translate(170, 90)">
          <circle r="22" fill="#ffffff" stroke="#2563eb" stroke-width="1.5"/>
          <path d="M-6 6 V-2 M0 6 V-8 M6 6 V2" stroke="#2563eb" stroke-width="2" stroke-linecap="round"/>
        </g>
        <!-- Nœud 2 : Monnaie Ariary (Ar) -->
        <g transform="translate(180, 270)">
          <circle r="22" fill="#ffffff" stroke="#2563eb" stroke-width="1.5"/>
          <text x="0" y="5" text-anchor="middle" font-size="14" font-weight="bold" fill="#2563eb">Ar</text>
        </g>
        <!-- Nœud 3 : Carte / Paiement -->
        <g transform="translate(120, 370)">
          <circle r="22" fill="#ffffff" stroke="#2563eb" stroke-width="1.5"/>
          <rect x="-9" y="-6" width="18" height="12" rx="2" stroke="#2563eb" stroke-width="1.5" fill="none"/>
          <line x1="-9" y1="-2" x2="9" y2="-2" stroke="#2563eb" stroke-width="1.5"/>
        </g>
        <!-- Nœud 4 : Égalité / Quittance -->
        <g transform="translate(170, 460)">
          <circle r="22" fill="#ffffff" stroke="#2563eb" stroke-width="1.5"/>
          <path d="M-7 -3 H7 M-7 3 H7" stroke="#2563eb" stroke-width="2" stroke-linecap="round"/>
        </g>
      </svg>
    </div>

    <!-- Carte du portail -->
    <div class="portal-card-container">

      <!-- Bannière bleu nuit à quadrillage -->
      <header class="portal-header-banner">
        <div class="dgi-badge">DGI</div>
        <h1 class="portal-title">Espace Agent</h1>
        <p class="portal-subtitle">PORTAIL D'AUTHENTIFICATION SÉCURISÉ</p>
      </header>

      <!-- Corps du formulaire -->
      <div class="portal-body">

        <!-- Alerte d'erreur -->
        <div v-if="erreur" class="error-box" role="alert">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3l10 18H2z"/><path d="M12 10v5M12 18h.01"/></svg>
          <span>{{ erreur }}</span>
        </div>

        <form @submit.prevent="handleLogin" novalidate>


          <!-- Adresse e-mail -->
          <div class="form-group">
            <label class="form-label" for="email">Adresse e-mail</label>
            <div class="field">
              <span class="field-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/></svg></span>
              <input
                id="email"
                type="email"
                inputmode="email"
                class="form-control custom-input has-icon"
                v-model.trim="email"
                placeholder="agent.dgi@impots.mg"
                autocomplete="email"
                autocapitalize="off"
                spellcheck="false"
                required
              >
            </div>
            <p class="field-hint">Votre adresse e-mail professionnelle.</p>
          </div>

          <!-- Mot de passe + œil -->
          <div class="form-group">
            <label class="form-label" for="password">Mot de passe</label>
            <div class="field">
              <span class="field-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V8a4 4 0 018 0v3"/></svg></span>
              <input
                id="password"
                :type="showPassword ? 'text' : 'password'"
                class="form-control custom-input has-icon custom-input-password"
                v-model="password"
                placeholder="••••••••"
                autocomplete="current-password"
                required
              >
              <button type="button" class="toggle-password" @click="showPassword = !showPassword"
                      :aria-label="showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
                      :aria-pressed="showPassword">
                <svg v-if="!showPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-6.5 0-10-7-10-7a18.5 18.5 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c6.5 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19"/><path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/><line x1="2" y1="2" x2="22" y2="22"/></svg>
              </button>
            </div>
          </div>

         

          <!-- Bouton Se connecter -->
          <button type="submit" class="submit-btn" :disabled="chargement">
            <span v-if="chargement" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
            <span>{{ chargement ? 'Vérification en cours…' : 'Se connecter' }}</span>
            <span v-if="!chargement" class="btn-arrow"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6"/></svg></span>
          </button>
        </form>

        <!-- Compte de démonstration : un clic remplit le formulaire -->
        <div class="demo-box">
          <div class="demo-head">
            <strong>Compte de démonstration</strong>
            <span class="demo-badge">Test</span>
          </div>
          <div class="demo-line">E-mail : <code>faly.bako@dgi.mg</code></div>
          <div class="demo-line">Mot de passe : <code>123456</code></div>
          <button type="button" class="demo-fill" @click="remplirDemo">Remplir automatiquement</button>
        </div>

        <!-- Pied de page -->
        <footer class="portal-footer">
          <p class="secure-note"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3l8 3v6c0 4.5-3.2 8.2-8 9-4.8-.8-8-4.5-8-9V6l8-3z"/><path d="M9 12l2 2 4-4"/></svg><span>Connexion sécurisée · Accès réservé aux agents habilités</span></p>
          <p class="official">République de Madagascar &mdash; Ministère des Finances &middot; DGI</p>
        </footer>

      </div>

      <!-- Liseré aux couleurs du drapeau -->
      <div class="flag-strip" aria-hidden="true"><span></span><span></span><span></span></div>
    </div>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const erreur = ref('')
const chargement = ref(false)
const showPassword = ref(false)

function remplirDemo() {
  email.value = 'faly.bako@dgi.mg'
  password.value = '123456'
  erreur.value = ''
}

async function handleLogin() {
  erreur.value = ''
  if (!email.value || !password.value) {
    erreur.value = 'Veuillez saisir votre adresse e-mail et votre mot de passe.'
    return
  }
  if (!/^\S+@\S+\.\S+$/.test(email.value)) {
    erreur.value = 'Adresse e-mail invalide.'
    return
  }

  chargement.value = true
  try {
    await auth.loginDgi(email.value, password.value)
    router.push('/dgi/dashboard')
  } catch (e) {
    erreur.value = extraireErreur(e) || 'E-mail ou mot de passe incorrect.'
  } finally {
    chargement.value = false
  }
}

function extraireErreur(e) {
  if (e.response) {
    if (typeof e.response.data === 'string') return e.response.data
    if (e.response.data?.message) return e.response.data.message
  }
  return null
}
</script>

<!-- CSS séparé : src/assets/css/connexion-agent.css -->
<style scoped src="@/assets/css/connexion-agent.css"></style>