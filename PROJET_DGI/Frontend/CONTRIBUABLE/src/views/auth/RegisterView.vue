<template>
  <AuthLayout>
    <!-- Panneau gauche : textes hero -->
    <template #hero>
      <span class="eyebrow">HETRA-RINDRA</span>
      <h2 class="hero-title">Créez votre <em>compte</em></h2>
      <p class="hero-subtitle">
        Quelques informations suffisent pour accéder à votre espace contribuable
        et gérer vos démarches en ligne.
      </p>
      <ul class="chips">
        <li>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>
          </svg>
          3 étapes simples
        </li>
        <li>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M12 3l8 3v6c0 4.5-3.2 8.2-8 9-4.8-.8-8-4.5-8-9V6l8-3z"/><path d="M9 12l2 2 4-4"/>
          </svg>
          Données protégées
        </li>
        <li>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/>
          </svg>
          Démarches en ligne
        </li>
      </ul>
    </template>

    <!-- Panneau droit : formulaire 3 étapes -->
    <h1 class="card-title">Créer mon compte</h1>

    <ol class="steps" aria-label="Progression de l'inscription">
      <li
        v-for="(label, i) in stepLabels"
        :key="label"
        class="step"
        :class="{ 'is-active': step === i + 1, 'is-done': step > i + 1 }"
        :aria-current="step === i + 1 ? 'step' : undefined"
      >
        <span class="dot">
          <template v-if="step > i + 1">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M5 12l5 5L20 7"/>
            </svg>
          </template>
          <template v-else>{{ i + 1 }}</template>
        </span>
        <span class="step-label">{{ label }}</span>
      </li>
    </ol>

    <div v-if="errorMessage" class="error-alert" role="alert">{{ errorMessage }}</div>

    <form @submit.prevent="handleSubmit" class="form-content">

      <!-- Étape 1 : Identité fiscale -->
      <template v-if="step === 1">
        <div class="form-group">
          <label class="form-label" for="nif">NIF</label>
          <input
            id="nif"
            type="text"
            v-model.trim="form.nif"
            placeholder="300XXXXXXX"
            maxlength="10"
            pattern="300[0-9]{7}"
            title="Le NIF doit commencer par 300 et contenir 10 chiffres"
            required
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="raisonSociale">Raison sociale / Nom</label>
          <input
            id="raisonSociale"
            type="text"
            v-model.trim="form.raisonSociale"
            placeholder="Nom complet ou raison sociale"
            autocomplete="organization"
            required
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="ca">Chiffre d'affaires annuel (Ar)</label>
          <input
            id="ca"
            type="number"
            v-model.number="form.chiffreAffairesAnnuel"
            placeholder="Ex : 120000000"
            min="0"
            step="1"
            required
            class="form-input"
          />
        </div>
      </template>

      <!-- Étape 2 : Coordonnées -->
      <template v-else-if="step === 2">
        <div class="form-group">
          <label class="form-label" for="telephone">Téléphone</label>
          <input
            id="telephone"
            type="tel"
            v-model.trim="form.telephone"
            placeholder="+261 3X XX XXX XX"
            autocomplete="tel"
            required
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="email">Adresse e-mail</label>
          <input
            id="email"
            type="email"
            v-model.trim="form.email"
            placeholder="exemple@email.com"
            autocomplete="email"
            required
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="adresse">Adresse</label>
          <input
            id="adresse"
            type="text"
            v-model.trim="form.adresse"
            placeholder="Quartier, commune"
            autocomplete="street-address"
            required
            class="form-input"
          />
        </div>
      </template>

      <!-- Étape 3 : Sécurité -->
      <template v-else>
        <div class="form-group">
          <label class="form-label" for="motDePasse">Mot de passe</label>
          <div class="password-wrapper">
            <input
              id="motDePasse"
              :type="showPassword ? 'text' : 'password'"
              v-model="form.motDePasse"
              placeholder="8 caractères minimum"
              autocomplete="new-password"
              minlength="8"
              required
              class="form-input form-input-password"
            />
            <button
              type="button"
              class="toggle-password"
              @click="showPassword = !showPassword"
              :aria-label="showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
              :aria-pressed="showPassword"
            >
              <svg v-if="!showPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-6.5 0-10-7-10-7a18.5 18.5 0 0 1 5.06-5.94"/>
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c6.5 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19"/>
                <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/>
                <line x1="2" y1="2" x2="22" y2="22"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label" for="confirmation">Confirmer le mot de passe</label>
          <div class="password-wrapper">
            <input
              id="confirmation"
              :type="showConfirm ? 'text' : 'password'"
              v-model="form.confirmation"
              placeholder="Retapez le mot de passe"
              autocomplete="new-password"
              minlength="8"
              required
              class="form-input form-input-password"
            />
            <button
              type="button"
              class="toggle-password"
              @click="showConfirm = !showConfirm"
              :aria-label="showConfirm ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
              :aria-pressed="showConfirm"
            >
              <svg v-if="!showConfirm" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-6.5 0-10-7-10-7a18.5 18.5 0 0 1 5.06-5.94"/>
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c6.5 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19"/>
                <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/>
                <line x1="2" y1="2" x2="22" y2="22"/>
              </svg>
            </button>
          </div>
        </div>
      </template>

      <div class="nav" :class="{ 'nav-single': step === 1 }">
        <button v-if="step > 1" type="button" class="back-button" @click="prevStep">
          Retour
        </button>
        <button type="submit" class="submit-button" :disabled="loading">
          <span v-if="loading">Création en cours...</span>
          <span v-else-if="step < 3">Continuer</span>
          <span v-else>Créer mon compte</span>
        </button>
      </div>
    </form>

    <div class="login-line">
      Déjà un compte ?
      <router-link to="/connexion" class="login-link">Se connecter</router-link>
    </div>

  </AuthLayout>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()
const authStore = useAuthStore()

const stepLabels = ['Identité', 'Coordonnées', 'Sécurité']
const step = ref(1)

const form = reactive({
  nif: '',
  raisonSociale: '',
  telephone: '',
  email: '',
  adresse: '',
  chiffreAffairesAnnuel: null,
  motDePasse: '',
  confirmation: ''
})

const errorMessage = ref('')
const loading = ref(false)
const showPassword = ref(false)
const showConfirm = ref(false)

const prevStep = () => {
  errorMessage.value = ''
  step.value--
}

const handleSubmit = async () => {
  errorMessage.value = ''

  // Validation NIF à l'étape 1
  if (step.value === 1) {
    if (!/^300\d{7}$/.test(form.nif)) {
      errorMessage.value = 'NIF invalide. Format attendu : 300XXXXXXX (10 chiffres).'
      return
    }
    step.value++
    return
  }

  // Étape 2 → 3
  if (step.value === 2) {
    step.value++
    return
  }

  // Étape 3 : contrôle des mots de passe + envoi API
  if (form.motDePasse !== form.confirmation) {
    errorMessage.value = 'Les deux mots de passe ne sont pas identiques.'
    return
  }

  loading.value = true
  try {
    await authStore.register({
      nif: form.nif,
      raisonSociale: form.raisonSociale,
      telephone: form.telephone,
      email: form.email,
      adresse: form.adresse,
      chiffreAffairesAnnuel: form.chiffreAffairesAnnuel,
      motDePasse: form.motDePasse
    })
    router.push('/connexion')
  } catch (err) {
    errorMessage.value =
      err.response?.data || err.message || 'Impossible de créer le compte.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped src="@/assets/css/inscription.css"></style>