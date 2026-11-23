<template>
  <DgiLayout>
    <div class="mb-4">
      <h3 class="fw-bold mb-0">📥 Import du relevé bancaire</h3>
      <p class="text-muted mb-0 small">
        Les virements payent leur bordereau ; les chèques remis au guichet sont confirmés ou rejetés.
        Chaque ligne est traitée seule : une ligne refusée n'empêche pas les autres.
      </p>
    </div>

    <div class="row g-4">
      <div class="col-lg-5">
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body">
            <h5 class="fw-bold mb-3"><i class="bi bi-file-earmark-spreadsheet me-2 text-primary"></i>Fichier CSV ou Excel</h5>

            <label
              :class="['depot', { survol, choisi: fichier }]"
              @dragover.prevent="survol = true"
              @dragleave.prevent="survol = false"
              @drop.prevent="deposer"
            >
              <input type="file" accept=".csv,.xlsx,.xls" class="d-none" @change="choisir" />
              <i :class="['bi fs-1', fichier ? 'bi-file-earmark-check text-success' : 'bi-cloud-arrow-up text-primary']"></i>
              <div v-if="fichier" class="fw-bold mt-2">{{ fichier.name }}</div>
              <div v-else class="fw-bold mt-2">Glissez le relevé ici ou cliquez pour le choisir</div>
              <small class="text-muted">{{ fichier ? `${(fichier.size / 1024).toFixed(1)} Ko` : 'CSV (séparateur ;) ou Excel (.xlsx, .xls)' }}</small>
            </label>

            <div v-if="erreur" class="alert alert-danger small mt-3 mb-0">{{ erreur }}</div>

            <button class="btn btn-primary fw-bold w-100 mt-3" :disabled="!fichier || envoi" @click="importer">
              <span v-if="envoi" class="spinner-border spinner-border-sm me-2"></span>
              <i v-else class="bi bi-upload me-2"></i>Importer le relevé
            </button>
          </div>
        </div>

        <div class="card border-0 shadow-sm">
          <div class="card-body small">
            <div class="text-uppercase text-muted fw-bold mb-2"><i class="bi bi-info-circle me-1"></i>Format attendu</div>
            <pre class="bg-light border rounded p-2 mb-2 format">date_operation;reference_banque;reference_bv;montant;type;numero_cheque
2026-11-12;OP-000101;BV-2026-000001;1590000;CHEQUE_PAYE;0012345
15/10/2026;OP-000102;BV-2026-000003;2 385 000;VIREMENT;</pre>
            <ul class="mb-2 ps-3">
              <li><strong>VIREMENT</strong> : paie le bordereau, qui devait être valable à la date de l'opération.</li>
              <li><strong>CHEQUE_PAYE</strong> : confirme un chèque remis au guichet (daté du jour de remise).</li>
              <li><strong>CHEQUE_IMPAYE</strong> : rejette ce chèque ; la dette reprend.</li>
            </ul>
            <div class="text-muted">
              Dates <code>AAAA-MM-JJ</code> ou <code>JJ/MM/AAAA</code>. Une même <code>reference_banque</code>
              n'est jamais importée deux fois.
              En Excel : mêmes colonnes sur la première feuille ; saisissez <code>numero_cheque</code> en texte
              pour garder les zéros de tête. Les relevés PDF ne sont pas acceptés.
            </div>
            <button class="btn btn-link btn-sm px-0 mt-1" @click="telechargerModele">
              <i class="bi bi-download me-1"></i>Télécharger un modèle
            </button>
          </div>
        </div>
      </div>

      <div class="col-lg-7">
        <template v-if="resultat">
          <div class="row g-3 mb-4">
            <div v-for="k in indicateurs" :key="k.libelle" class="col-6 col-md-3">
              <div class="card border-0 shadow-sm h-100">
                <div class="card-body py-3">
                  <div class="text-uppercase text-muted fw-bold small mb-1">{{ k.libelle }}</div>
                  <div :class="['fs-3 fw-bolder', k.classe]">{{ k.valeur }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="card border-0 shadow-sm">
            <div class="card-header bg-white py-3">
              <h5 class="mb-0 fw-bold">
                <template v-if="resultat.lignesRejetees.length">⚠️ Lignes refusées ({{ resultat.lignesRejetees.length }})</template>
                <template v-else>✅ Toutes les lignes ont été acceptées</template>
              </h5>
            </div>
            <div v-if="resultat.lignesRejetees.length" class="table-responsive">
              <table class="table align-middle mb-0">
                <thead class="table-light">
                  <tr><th>Ligne</th><th>Contenu</th><th>Raison</th></tr>
                </thead>
                <tbody>
                  <tr v-for="l in resultat.lignesRejetees" :key="l.numeroLigne">
                    <td class="fw-bold">{{ l.numeroLigne }}</td>
                    <td><code class="small text-break">{{ l.contenu }}</code></td>
                    <td class="small text-danger">{{ l.raison }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="card-footer bg-white small text-muted">
              Corrigez les lignes refusées dans un nouveau fichier : les lignes déjà acceptées ne sont pas reprises en double.
            </div>
          </div>
        </template>

        <div v-else class="card border-0 shadow-sm">
          <div class="card-body text-center text-muted p-5">
            <i class="bi bi-bank fs-1 d-block mb-2"></i>
            Le bilan de l'import s'affichera ici : paiements validés, chèques confirmés ou rejetés,
            et chaque ligne refusée avec sa raison.
          </div>
        </div>
      </div>
    </div>
  </DgiLayout>
</template>

<script setup>
import { computed, ref } from 'vue'
import DgiLayout from '@/layouts/DgiLayout.vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const fichier = ref(null)
const survol = ref(false)
const envoi = ref(false)
const erreur = ref('')
const resultat = ref(null)

function choisir(e) {
  fichier.value = e.target.files[0] || null
  resultat.value = null
  erreur.value = ''
}

function deposer(e) {
  survol.value = false
  fichier.value = e.dataTransfer.files[0] || null
  resultat.value = null
  erreur.value = ''
}

async function importer() {
  erreur.value = ''
  envoi.value = true
  const donnees = new FormData()
  donnees.append('fichier', fichier.value)
  donnees.append('matriculeAgent', auth.user?.matricule || '')
  try {
    // multipart : sans cet en-tête, axios convertirait le formulaire en JSON (en-tête par défaut de l'instance)
    const { data } = await api.post('/dgi/guichet/import-banque', donnees, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    })
    resultat.value = data
  } catch (e) {
    erreur.value = e.response?.data?.message || 'L\'import a échoué. Vérifiez le fichier et que le serveur est démarré.'
  } finally {
    envoi.value = false
  }
}

const indicateurs = computed(() => [
  { libelle: 'Lignes lues', valeur: resultat.value.lignesLues, classe: 'text-dark' },
  { libelle: 'Virements validés', valeur: resultat.value.paiementsValides, classe: 'text-success' },
  { libelle: 'Chèques confirmés', valeur: resultat.value.chequesConfirmes, classe: 'text-primary' },
  { libelle: 'Chèques rejetés', valeur: resultat.value.chequesRejetes, classe: 'text-danger' },
])

function telechargerModele() {
  const contenu = 'date_operation;reference_banque;reference_bv;montant;type;numero_cheque\n'
  const lien = document.createElement('a')
  lien.href = URL.createObjectURL(new Blob([contenu], { type: 'text/csv;charset=utf-8' }))
  lien.download = 'releve_banque_modele.csv'
  lien.click()
  URL.revokeObjectURL(lien.href)
}
</script>

<style scoped>
.depot {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2rem 1rem;
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s ease;
}
.depot:hover, .depot.survol { border-color: #2563eb; background: #eff6ff; }
.depot.choisi { border-style: solid; border-color: #12b76a; background: #ecfdf3; }
.format { font-size: 0.72rem; white-space: pre; overflow-x: auto; }
</style>
