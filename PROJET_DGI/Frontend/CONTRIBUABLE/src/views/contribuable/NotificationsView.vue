<template>
  <ContribuableLayout>
    <div class="row">
      <div class="col-md-12">
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
            <h4 class="mb-0 fw-bold text-dark">🔔 Journal des Notifications</h4>
            <button class="btn btn-sm btn-outline-primary" :disabled="!nonLues" @click="toutMarquerLu">
              Tout marquer comme lu
            </button>
          </div>

          <div class="card-body p-0">
            <div v-if="erreur" class="alert alert-warning m-4 mb-4">{{ erreur }}</div>
            <div v-else-if="chargement" class="text-center text-muted p-5">Chargement…</div>

            <div v-else-if="!notifications.length" class="text-center text-muted p-5">
              <i class="bi bi-bell-slash fs-2 d-block mb-2"></i>
              Aucune notification pour le moment.
            </div>

            <div v-else class="list-group list-group-flush">
              <div
                v-for="n in notifications"
                :key="n.id"
                :class="['list-group-item p-4 border-start border-4', 'border-' + style(n).couleur, { 'bg-light': !n.estLue }]"
                role="button"
                @click="ouvrir(n)"
              >
                <div class="d-flex w-100 justify-content-between mb-2 gap-3">
                  <h6 :class="['mb-0 fw-bold', style(n).titre]">{{ style(n).icone }} {{ n.titre }}</h6>
                  <small :class="['text-muted text-nowrap', { 'fw-bold': !n.estLue }]">{{ dateRelative(n.dateEnvoi) }}</small>
                </div>
                <p :class="['mb-1', n.estLue ? 'text-secondary' : 'text-dark']">{{ n.message }}</p>

                <div class="d-flex align-items-center flex-wrap gap-2 mt-2">
                  <span v-if="!n.estLue" class="badge bg-danger rounded-pill">Non lu</span>
                  <span v-for="e in n.envois.filter((e) => e.statut === 'ENVOYE')" :key="e.canal" class="small text-muted">
                    <i :class="['bi', e.canal === 'SMS' ? 'bi-phone' : 'bi-envelope']"></i>
                    Également envoyé par {{ e.canal === 'SMS' ? 'SMS' : 'email' }} à {{ e.destinataire }}
                  </span>
                  <button
                    v-if="n.lignes.length"
                    class="btn btn-sm btn-link p-0 text-decoration-none fw-bold ms-auto"
                    @click.stop="basculer(n)"
                  >
                    {{ ouverts.has(n.id) ? 'Masquer le détail' : `Voir le détail (${n.lignes.length})` }}
                    <i :class="['bi', ouverts.has(n.id) ? 'bi-chevron-up' : 'bi-chevron-down']"></i>
                  </button>
                </div>

                <!-- Détail des obligations relancées -->
                <div v-if="ouverts.has(n.id)" class="table-responsive mt-3" @click.stop>
                  <table class="table table-sm align-middle mb-2 bg-white border">
                    <thead class="table-light small text-uppercase text-muted">
                      <tr>
                        <th>Impôt</th>
                        <th>Période</th>
                        <th>Date limite</th>
                        <th class="text-end">Retard</th>
                        <th class="text-end">Montant dû</th>
                        <th>Niveau</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="l in n.lignes" :key="l.idDeclaration">
                        <td><span class="badge bg-light text-dark border">{{ l.impot }}</span></td>
                        <td>{{ libellePeriode(l) }}</td>
                        <td>{{ formatDate(l.dateLimite) }}</td>
                        <td class="text-end">{{ l.joursRetard }} j</td>
                        <td class="text-end fw-bold">{{ formatMontant(l.montantDu) }} Ar</td>
                        <td><small>{{ l.typeRelanceLibelle }}</small></td>
                      </tr>
                    </tbody>
                    <tfoot>
                      <tr>
                        <td colspan="4" class="fw-bold">Total à la date de la relance</td>
                        <td class="text-end fw-bold text-danger">{{ formatMontant(n.montantTotal) }} Ar</td>
                        <td></td>
                      </tr>
                    </tfoot>
                  </table>
                  <router-link to="/contribuable/dashboard" class="small fw-bold text-decoration-none">
                    Voir ma situation à jour dans le tableau de bord →
                  </router-link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </ContribuableLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import ContribuableLayout from '@/layouts/ContribuableLayout.vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useNotificationsStore } from '@/stores/notifications'
import { formatDate, formatMontant, libellePeriode } from '@/utils/fiscal'

const auth = useAuthStore()
const badge = useNotificationsStore()

const notifications = ref([])
const chargement = ref(false)
const erreur = ref('')
const ouverts = reactive(new Set())

const nonLues = computed(() => notifications.value.filter((n) => !n.estLue).length)

onMounted(charger)

async function charger() {
  if (!auth.contribuableNif) return
  chargement.value = true
  erreur.value = ''
  try {
    const { data } = await api.get(`/contribuable/notifications/${encodeURIComponent(auth.contribuableNif)}`)
    notifications.value = data
  } catch {
    erreur.value = 'Impossible de charger vos notifications. Vérifiez que le serveur est démarré.'
  } finally {
    chargement.value = false
  }
}

// Couleur et icône selon le niveau de relance (ou le type pour les autres notifications)
const STYLES = {
  MISE_EN_DEMEURE: { couleur: 'danger', titre: 'text-danger', icone: '⚠️' },
  RELANCE_2: { couleur: 'warning', titre: 'text-dark', icone: '⏰' },
  RELANCE_1: { couleur: 'primary', titre: 'text-dark', icone: '📩' },
  PAIEMENT_VALIDE: { couleur: 'success', titre: 'text-success', icone: '✅' },
  RAPPEL_ECHEANCE: { couleur: 'warning', titre: 'text-dark', icone: 'ℹ️' },
}
const style = (n) => STYLES[n.niveau] || STYLES[n.type] || { couleur: 'secondary', titre: 'text-dark', icone: 'ℹ️' }

function dateRelative(iso) {
  const d = new Date(iso)
  const heure = d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
  const jours = Math.round((new Date().setHours(0, 0, 0, 0) - new Date(iso).setHours(0, 0, 0, 0)) / 86400000)
  if (jours === 0) return `Aujourd'hui, ${heure}`
  if (jours === 1) return `Hier, ${heure}`
  return `${d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}, ${heure}`
}

// Ouvrir une notification l'enregistre comme lue (seule lecture fiable, contrairement à l'email ou au SMS)
async function ouvrir(n) {
  if (n.lignes.length && !ouverts.has(n.id)) ouverts.add(n.id)
  if (n.estLue) return
  try {
    await api.put(`/contribuable/notifications/${n.id}/lue`)
    n.estLue = true
    badge.nonLues = nonLues.value
  } catch {
    // La notification reste "non lue", elle sera marquée au prochain clic
  }
}

function basculer(n) {
  if (ouverts.has(n.id)) ouverts.delete(n.id)
  else ouvrir(n)
}

async function toutMarquerLu() {
  try {
    await api.put(`/contribuable/notifications/${encodeURIComponent(auth.contribuableNif)}/tout-lire`)
    notifications.value.forEach((n) => (n.estLue = true))
    badge.nonLues = 0
  } catch {
    erreur.value = 'Impossible de marquer les notifications comme lues.'
  }
}
</script>
