<template>
  <DgiLayout>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-4">
      <div>
        <h3 class="fw-bold mb-0">📨 Relances automatiques</h3>
        <p class="text-muted mb-0 small">
          Le système relance seul les contribuables en retard, selon la planification de
          <code>config/relance.properties</code>. Aucune validation manuelle n'est nécessaire.
        </p>
      </div>
      <button class="btn btn-primary fw-bold px-3 shadow-sm" :disabled="lancement" @click="lancerMaintenant">
        <span v-if="lancement" class="spinner-border spinner-border-sm me-2"></span>
        <i v-else class="bi bi-send-fill me-2"></i>Lancer la relance maintenant
      </button>
    </div>

    <div v-if="erreur" class="alert alert-warning">{{ erreur }}</div>

    <!-- Bilan de la dernière exécution manuelle -->
    <div v-if="resultat" class="alert alert-success d-flex justify-content-between align-items-start">
      <div>
        <strong>Relance exécutée.</strong>
        <template v-if="resultat.relancesCreees">
          {{ resultat.contribuablesRelances }} contribuable(s) relancé(s) pour {{ resultat.relancesCreees }} obligation(s).
          <template v-if="resultat.emailsEnAttente">
            {{ resultat.emailsEnAttente }} email(s) en file d'attente : ils partent dans les secondes qui suivent
            (rechargez la page pour voir leur statut).
          </template>
        </template>
        <template v-else>Aucun nouveau contribuable à relancer.</template>
        <span v-if="resultat.dettesIgnoreesDelai" class="d-block small">
          {{ resultat.dettesIgnoreesDelai }} obligation(s) déjà relancée(s) il y a moins de
          {{ journal?.configuration.delaiEntreDeuxRelancesJours }} jours, ignorée(s) pour éviter les doublons.
        </span>
        <span v-if="resultat.lotsEnErreur" class="d-block small text-danger">
          {{ resultat.lotsEnErreur }} lot(s) de contribuables en erreur (voir les logs du backend), les autres ont été relancés.
        </span>
      </div>
      <button class="btn-close" @click="resultat = null"></button>
    </div>

    <template v-if="journal">
      <!-- ===== Paramètres (lecture seule : ils se modifient dans le fichier) ===== -->
      <div class="row g-3 mb-4">
        <div class="col-lg-4">
          <div class="card border-0 shadow-sm h-100">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-uppercase text-muted fw-bold small"><i class="bi bi-calendar-event me-1"></i>Planification</span>
                <span :class="['badge', config.active ? 'bg-success' : 'bg-secondary']">{{ config.active ? 'Active' : 'Désactivée' }}</span>
              </div>
              <div class="fs-5 fw-bold">{{ config.planificationLibelle }}</div>
              <div class="small text-muted mb-3"><code>{{ config.planification }}</code></div>
              <div class="small">
                <i class="bi bi-hourglass-split me-1 text-primary"></i>
                Prochaine exécution :
                <strong>{{ config.prochaineExecution ? formatDateHeure(config.prochaineExecution) : '—' }}</strong>
              </div>
              <div class="small text-muted mt-1">
                Dernière relance : {{ stats.derniereExecution ? formatDateHeure(stats.derniereExecution) : 'aucune' }}
              </div>
            </div>
          </div>
        </div>

        <div class="col-lg-4">
          <div class="card border-0 shadow-sm h-100">
            <div class="card-body">
              <div class="text-uppercase text-muted fw-bold small mb-2"><i class="bi bi-funnel me-1"></i>Qui est relancé ?</div>
              <ul class="list-unstyled small mb-0 criteres">
                <li>Retard d'au moins <strong>{{ config.joursRetardMinimum }} jour(s)</strong> après la date limite</li>
                <li>Reste dû d'au moins <strong>{{ formatMontant(config.montantMinimum) }} Ar</strong></li>
                <li>Pas de nouvelle relance avant <strong>{{ config.delaiEntreDeuxRelancesJours }} jours</strong></li>
                <li>Échéanciers en retard : <strong>{{ config.inclureEcheanciers ? 'inclus' : 'exclus' }}</strong></li>
                <li>Mise en demeure après <strong>{{ config.miseEnDemeureApres }} relance(s)</strong></li>
              </ul>
            </div>
          </div>
        </div>

        <div class="col-lg-4">
          <div class="card border-0 shadow-sm h-100">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-uppercase text-muted fw-bold small"><i class="bi bi-envelope me-1"></i>Envoi par email</span>
                <span :class="['badge', config.emailActif ? 'bg-success' : 'bg-secondary']">{{ config.emailActif ? 'Actif' : 'Désactivé' }}</span>
              </div>
              <div v-if="config.compteEnvoiConfigure" class="small mb-2">
                <i class="bi bi-check-circle-fill text-success me-1"></i>Expéditeur : <strong>{{ config.compteEnvoi }}</strong>
              </div>
              <div v-else class="small mb-2 text-danger">
                <i class="bi bi-exclamation-triangle-fill me-1"></i>Compte d'envoi non configuré
                (<code>MAIL_USERNAME</code> / <code>MAIL_PASSWORD</code> dans <code>.env</code>)
              </div>
              <div class="small text-muted">Destinataires : l'adresse email enregistrée de chaque contribuable relancé.</div>
              <div class="small text-muted mt-2"><i class="bi bi-phone me-1"></i>SMS : prochaine étape (passerelle Android)</div>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== Chiffres clés ===== -->
      <div class="row g-3 mb-4">
        <div v-for="k in indicateurs" :key="k.libelle" class="col-6 col-lg-3">
          <div class="card border-0 shadow-sm h-100">
            <div class="card-body py-3">
              <div class="text-uppercase text-muted fw-bold small mb-1">{{ k.libelle }}</div>
              <div :class="['fs-3 fw-bolder', k.classe]">{{ k.valeur }}</div>
              <small class="text-muted">{{ k.detail }}</small>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== Journal ===== -->
      <div class="card border-0 shadow-sm">
        <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center flex-wrap gap-2">
          <h5 class="mb-0 fw-bold">📋 Journal des relances envoyées</h5>
          <div class="d-flex gap-2 flex-wrap">
            <input v-model="filtres.recherche" type="search" class="form-control form-control-sm" style="width: 220px"
                   placeholder="Contribuable ou NIF…" />
            <select v-model="filtres.niveau" class="form-select form-select-sm" style="width: auto">
              <option value="">Tous les niveaux</option>
              <option v-for="(def, code) in NIVEAUX" :key="code" :value="code">{{ def.libelle }}</option>
            </select>
            <select v-model="filtres.lecture" class="form-select form-select-sm" style="width: auto">
              <option value="">Lues et non lues</option>
              <option value="LUE">Lues</option>
              <option value="NON_LUE">Non lues</option>
            </select>
          </div>
        </div>
        <div class="card-body p-0">
          <div v-if="!journal.relances.length" class="text-center text-muted p-5">
            <i class="bi bi-inbox fs-2 d-block mb-2"></i>
            Aucune relance envoyée pour l'instant. Elle partira automatiquement à la prochaine exécution,
            ou cliquez sur « Lancer la relance maintenant ».
          </div>
          <div v-else class="table-responsive">
            <table class="table table-hover align-middle mb-0">
              <thead class="table-light">
                <tr>
                  <th>Date</th>
                  <th>Contribuable</th>
                  <th>Niveau</th>
                  <th class="text-end">Montant relancé</th>
                  <th>Email</th>
                  <th>Lecture (espace)</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <template v-for="r in relancesFiltrees" :key="r.notification.id">
                  <tr role="button" @click="basculer(r.notification.id)">
                    <td class="small text-nowrap">{{ formatDateHeure(r.notification.dateEnvoi) }}</td>
                    <td>
                      <div class="fw-bold text-dark">{{ r.raisonSociale }}</div>
                      <small class="text-muted">NIF : {{ r.nif }}</small>
                    </td>
                    <td>
                      <span :class="['badge px-3 py-2', niveau(r.notification.niveau).classe]">
                        {{ niveau(r.notification.niveau).libelle }}
                      </span>
                      <div class="small text-muted mt-1">{{ r.notification.lignes.length }} obligation(s)</div>
                    </td>
                    <td class="text-end fw-bold text-danger text-nowrap">{{ formatMontant(r.notification.montantTotal) }} Ar</td>
                    <td>
                      <template v-if="email(r)">
                        <span :class="['badge', STATUTS_ENVOI[email(r).statut]?.classe || 'bg-secondary']"
                              :title="email(r).messageErreur || ''">
                          {{ STATUTS_ENVOI[email(r).statut]?.libelle || email(r).statut }}
                        </span>
                        <div class="small text-muted text-truncate" style="max-width: 220px">{{ email(r).destinataire }}</div>
                      </template>
                      <span v-else class="small text-muted">—</span>
                    </td>
                    <td>
                      <span v-if="r.notification.estLue" class="badge bg-success-subtle text-success border border-success-subtle">
                        <i class="bi bi-eye-fill me-1"></i>Lue le {{ formatDateHeure(r.notification.dateLecture) }}
                      </span>
                      <span v-else class="badge bg-light text-muted border"><i class="bi bi-eye-slash me-1"></i>Non lue</span>
                    </td>
                    <td class="text-end">
                      <i :class="['bi', ouverts.has(r.notification.id) ? 'bi-chevron-up' : 'bi-chevron-down']"></i>
                    </td>
                  </tr>
                  <tr v-if="ouverts.has(r.notification.id)" class="table-light">
                    <td colspan="7" class="px-4 py-3">
                      <div class="small fw-bold mb-1">{{ r.notification.titre }}</div>
                      <p class="small text-secondary mb-2">{{ r.notification.message }}</p>
                      <div v-if="email(r)?.messageErreur" class="small text-danger mb-2">
                        <i class="bi bi-exclamation-circle me-1"></i>{{ email(r).messageErreur }}
                      </div>
                      <table class="table table-sm mb-0 bg-white border">
                        <thead class="small text-uppercase text-muted">
                          <tr>
                            <th>Impôt</th><th>Période</th><th>Date limite</th>
                            <th class="text-end">Retard</th><th class="text-end">Montant dû</th><th>Niveau</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="l in r.notification.lignes" :key="l.idDeclaration">
                            <td><span class="badge bg-light text-dark border">{{ l.impot }}</span></td>
                            <td>{{ libellePeriode(l) }}</td>
                            <td>{{ formatDate(l.dateLimite) }}</td>
                            <td class="text-end">{{ l.joursRetard }} j</td>
                            <td class="text-end fw-bold">{{ formatMontant(l.montantDu) }} Ar</td>
                            <td class="small">{{ l.typeRelanceLibelle }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </td>
                  </tr>
                </template>
                <tr v-if="!relancesFiltrees.length">
                  <td colspan="7" class="text-center text-muted py-4">Aucune relance ne correspond aux filtres.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </template>

    <div v-else-if="chargement" class="text-center text-muted p-5">Chargement…</div>
  </DgiLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import DgiLayout from '@/layouts/DgiLayout.vue'
import api from '@/services/api'

const journal = ref(null)
const chargement = ref(false)
const lancement = ref(false)
const erreur = ref('')
const resultat = ref(null)
const ouverts = reactive(new Set())
const filtres = reactive({ recherche: '', niveau: '', lecture: '' })

const config = computed(() => journal.value?.configuration || {})
const stats = computed(() => journal.value?.statistiques || {})

const NIVEAUX = {
  RELANCE_1: { libelle: 'Première relance', classe: 'bg-info text-dark' },
  RELANCE_2: { libelle: 'Deuxième relance', classe: 'bg-warning text-dark' },
  MISE_EN_DEMEURE: { libelle: 'Mise en demeure', classe: 'bg-danger' },
}
const niveau = (code) => NIVEAUX[code] || { libelle: code || '—', classe: 'bg-secondary' }

const STATUTS_ENVOI = {
  ENVOYE: { libelle: 'Envoyé', classe: 'bg-success' },
  DELIVRE: { libelle: 'Délivré', classe: 'bg-success' },
  EN_ATTENTE: { libelle: 'En attente', classe: 'bg-secondary' },
  ECHEC: { libelle: 'Échec', classe: 'bg-danger' },
}
const email = (r) => r.notification.envois.find((e) => e.canal === 'EMAIL')

const indicateurs = computed(() => [
  { libelle: 'Contribuables relancés', valeur: stats.value.contribuablesRelances, detail: `${stats.value.relances} obligation(s) relancée(s)`, classe: 'text-dark' },
  { libelle: 'Mises en demeure', valeur: stats.value.misesEnDemeure, detail: 'après relances restées sans effet', classe: 'text-danger' },
  { libelle: 'Emails envoyés', valeur: stats.value.emailsEnvoyes, detail: stats.value.emailsEchec ? `${stats.value.emailsEchec} en échec` : 'aucun échec', classe: 'text-primary' },
  { libelle: 'Lues dans l\'espace', valeur: `${stats.value.notificationsLues} / ${stats.value.notifications}`, detail: 'seule lecture vérifiable', classe: 'text-success' },
])

const relancesFiltrees = computed(() => {
  const q = filtres.recherche.trim().toLowerCase()
  return (journal.value?.relances || []).filter((r) =>
    (!q || r.raisonSociale.toLowerCase().includes(q) || r.nif.includes(q))
    && (!filtres.niveau || r.notification.niveau === filtres.niveau)
    && (!filtres.lecture || (filtres.lecture === 'LUE') === r.notification.estLue))
})

onMounted(charger)

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    const { data } = await api.get('/dgi/relances')
    journal.value = data
  } catch {
    erreur.value = 'Impossible de charger le journal des relances. Vérifiez que le serveur est démarré.'
  } finally {
    chargement.value = false
  }
}

async function lancerMaintenant() {
  if (!confirm('Lancer la relance maintenant ? Les contribuables en retard recevront une notification et un email.')) return
  lancement.value = true
  erreur.value = ''
  try {
    // Avec beaucoup de contribuables en retard, la campagne peut durer (les emails partent ensuite, à part)
    const { data } = await api.post('/dgi/relances/lancer', null, { timeout: 120000 })
    resultat.value = data
    await charger()
  } catch {
    erreur.value = 'La relance a échoué. Consultez les logs du serveur (docker logs ssif-backend).'
  } finally {
    lancement.value = false
  }
}

function basculer(id) {
  if (ouverts.has(id)) ouverts.delete(id)
  else ouverts.add(id)
}

const MOIS = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre']
function libellePeriode(l) {
  const p = l.periode || ''
  let m
  if ((m = p.match(/^M(\d{1,2})$/))) return `${MOIS[Number(m[1]) - 1]} ${l.annee ?? ''}`.trim()
  if ((m = p.match(/^T([1-4])$/))) return `${m[1]}${m[1] === '1' ? 'er' : 'e'} trimestre ${l.annee ?? ''}`.trim()
  if ((m = p.match(/^A(\d{4})$/))) return `Exercice ${m[1]}`
  return `${p} ${l.annee ?? ''}`.trim()
}
const formatMontant = (v) => Number(v || 0).toLocaleString('fr-FR', { maximumFractionDigits: 0 })
const formatDate = (iso) => (iso ? new Date(iso).toLocaleDateString('fr-FR') : '—')
const formatDateHeure = (iso) => (iso
  ? new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  : '—')
</script>

<style scoped>
.criteres li { padding: 0.2rem 0; border-bottom: 1px dashed #e6eaf0; }
.criteres li:last-child { border-bottom: 0; }
</style>
