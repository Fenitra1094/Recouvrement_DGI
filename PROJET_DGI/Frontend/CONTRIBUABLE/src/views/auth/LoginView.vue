<template>
  <AuthLayout>
    <!-- Contenu spécifique au panneau visuel (injecté dans le slot name="hero") -->
    <template #hero>
      <span class="eyebrow">HETRA-RINDRA</span>
      <h2 class="hero-title">Votre espace <em>contribuable</em></h2>
      <p class="hero-subtitle">Consultez votre situation fiscale et effectuez vos démarches en ligne, simplement et en toute sécurité.</p>
      <ul class="chips">
        <li><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/></svg>Paiement en ligne</li>
        <li><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8z"/><path d="M14 3v5h5M9 13h6M9 17h6"/></svg>Suivi de dossier</li>
        <li><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3l8 3v6c0 4.5-3.2 8.2-8 9-4.8-.8-8-4.5-8-9V6l8-3z"/><path d="M9 12l2 2 4-4"/></svg>Accès sécurisé</li>
      </ul>
    </template>

    <!-- Contenu du formulaire (injecté dans le slot par défaut) -->
    <h1 class="card-title">Connexion</h1>

    <div v-if="errorMessage" class="error-alert" role="alert">
      {{ errorMessage }}
    </div>

    <form @submit.prevent="handleLogin" class="form-content">
      <div class="form-group">
        <label class="form-label" for="identifiant">Identifiant</label>
        <input id="identifiant" type="text" v-model="identifiant" placeholder="NIF ou email" autocomplete="username" required class="form-input" />
      </div>

      <div class="form-group">
        <label class="form-label" for="motDePasse">Mot de passe</label>
        <div class="password-wrapper">
          <input id="motDePasse" :type="showPassword ? 'text' : 'password'" v-model="motDePasse" placeholder="••••••••" autocomplete="current-password" required class="form-input form-input-password" />
          <button type="button" class="toggle-password" @click="showPassword = !showPassword" :aria-label="showPassword ? 'Masquer' : 'Afficher'">
            <svg v-if="!showPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>
            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-6.5 0-10-7-10-7a18.5 18.5 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c6.5 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19"/><path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/><line x1="2" y1="2" x2="22" y2="22"/></svg>
          </button>
        </div>
      </div>

  

      <button type="submit" class="submit-button" :disabled="loading">
        <span v-if="!loading">Se connecter</span>
        <span v-else>Connexion en cours...</span>
      </button>
    </form>

    <div class="register-wrapper">
      <div class="register-divider"><span>Nouveau contribuable ?</span></div>
      <router-link to="/inscription" class="register-button">S'inscrire (Nouveau membre)</router-link>
    </div>

  </AuthLayout>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import AuthLayout from '@/layouts/AuthLayout.vue';

const router = useRouter();
const authStore = useAuthStore();

const identifiant = ref('');
const motDePasse = ref('');
const errorMessage = ref('');
const loading = ref(false);
const showPassword = ref(false);

const handleLogin = async () => {
  errorMessage.value = '';
  loading.value = true;
  try {
    await authStore.loginContribuable(identifiant.value, motDePasse.value);
    router.push('/contribuable/dashboard');
  } catch (err) {
    errorMessage.value = err.response?.data || err.message || "NIF / Email ou mot de passe incorrect.";
  } finally {
    loading.value = false;
  }
};
</script>