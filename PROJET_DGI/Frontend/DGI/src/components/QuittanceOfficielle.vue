<template>
  <!--
    Quittance (paiement validé) ou reçu de remise de chèque, au même format que le bordereau officiel.
    Le PDF est produit à partir de la feuille (utils/pdf.js) ; l'aperçu est seulement réduit à l'écran.
  -->
  <div ref="apercu" class="qo-apercu" :style="{ height: hauteurApercu }">
    <div class="qo-echelle" :style="{ transform: `scale(${echelle})` }">
      <div ref="feuille" class="qo-feuille">
        <header class="qo-entete">
          <div class="qo-entete-gauche">
            <div>MINISTÈRE DE L'ÉCONOMIE ET DES FINANCES</div>
            <div class="qo-tirets">--------------------</div>
            <div>DIRECTION GÉNÉRALE DES IMPÔTS</div>
          </div>
          <div class="qo-entete-centre">
            <div class="qo-republique">REPOBLIKAN'I MADAGASIKARA</div>
            <div class="qo-devise">Fitiavana – Tanindrazana – Fandrosoana</div>
          </div>
          <div class="qo-entete-droite">
            <div class="qo-numero-libelle">N° récépissé</div>
            <div class="qo-numero">{{ q.referencePaiement }}</div>
          </div>
        </header>

        <div class="qo-titre">
          <h1>{{ valide ? 'QUITTANCE DE PAIEMENT' : 'REÇU DE REMISE DE CHÈQUE' }}</h1>
          <div v-if="!valide" class="qo-mention">Ce reçu ne vaut pas quittance : la quittance est acquise à la confirmation du chèque par la banque.</div>
        </div>

        <table class="qo-table">
          <tbody>
            <tr><th>Contribuable</th><td>{{ q.raisonSociale }}</td></tr>
            <tr><th>NIF</th><td class="qo-mono">{{ q.nif }}</td></tr>
            <tr><th>Impôt / période</th><td>{{ q.impot }} — {{ libellePeriode }}</td></tr>
            <tr><th>Bordereau de versement</th><td class="qo-mono">{{ q.referenceBv }}</td></tr>
            <tr><th>Date</th><td>{{ date(q.datePaiement) }}</td></tr>
            <tr>
              <th>Mode de paiement</th>
              <td>{{ q.modePaiement === 'CHEQUE' ? `Chèque n° ${q.numeroCheque} — ${q.banque}` : q.modePaiement === 'VIREMENT' ? 'Virement bancaire' : 'Espèces' }}</td>
            </tr>
            <tr><th>Principal</th><td class="qo-mono qo-droite">{{ montant(q.principal) }} Ar</td></tr>
            <tr><th>Pénalités de retard</th><td class="qo-mono qo-droite">{{ montant(q.penalites) }} Ar</td></tr>
            <tr class="qo-total"><th>MONTANT {{ valide ? 'ENCAISSÉ' : 'REMIS' }}</th><td class="qo-mono qo-droite">{{ montant(q.montant) }} Ar</td></tr>
          </tbody>
        </table>

        <div class="qo-signature">
          <div>Agent : {{ agent || '—' }}</div>
          <div class="qo-cachet">Cachet et signature</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  /** QuittanceDTO renvoyé par l'API */
  q: { type: Object, required: true },
  /** Agent affiché si la quittance n'en porte pas */
  agentParDefaut: { type: String, default: '' },
})

const feuille = ref(null)
defineExpose({ feuille })

const MOIS = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre']
const valide = computed(() => props.q.statut === 'VALIDE')
const agent = computed(() => props.q.agent || props.agentParDefaut)
const libellePeriode = computed(() => {
  const p = props.q.periode || ''
  let m
  if ((m = p.match(/^M(\d{1,2})$/))) return `mois de ${MOIS[Number(m[1]) - 1]} ${props.q.annee ?? ''}`.trim()
  if ((m = p.match(/^A(\d{4})$/))) return `exercice ${m[1]}`
  return `${p} ${props.q.annee ?? ''}`.trim()
})
const montant = (v) => Number(v || 0).toLocaleString('fr-FR', { maximumFractionDigits: 0 })
const date = (iso) => (iso ? new Date(iso).toLocaleDateString('fr-FR') : '—')

// Réduction de l'aperçu à la largeur du conteneur (jamais agrandi)
const apercu = ref(null)
const echelle = ref(1)
const hauteurApercu = ref('auto')
let observateur = null
function ajuster() {
  if (!apercu.value || !feuille.value) return
  echelle.value = Math.min(1, apercu.value.clientWidth / feuille.value.offsetWidth)
  hauteurApercu.value = `${feuille.value.offsetHeight * echelle.value}px`
}
onMounted(() => {
  ajuster()
  observateur = new ResizeObserver(ajuster)
  observateur.observe(apercu.value)
  observateur.observe(feuille.value)
})
onBeforeUnmount(() => observateur?.disconnect())
</script>

<style scoped>
.qo-apercu { width: 100%; overflow: hidden; }
.qo-echelle { width: 794px; transform-origin: top left; }
.qo-feuille {
  width: 794px; padding: 40px 44px; background: #fff; color: #111; box-sizing: border-box;
  font-family: Arial, Helvetica, sans-serif; font-size: 12.5px; line-height: 1.45;
}
.qo-entete { display: grid; grid-template-columns: 1fr 1.1fr 1fr; gap: 12px; font-size: 10.5px; }
.qo-entete-gauche { text-align: center; font-weight: 700; }
.qo-tirets { font-weight: 400; color: #555; line-height: 1; }
.qo-entete-centre { text-align: center; }
.qo-republique { font-weight: 700; font-size: 12px; letter-spacing: 0.5px; }
.qo-devise { font-style: italic; color: #333; }
.qo-entete-droite { text-align: right; }
.qo-numero-libelle { color: #555; }
.qo-numero { font-family: 'Courier New', monospace; font-weight: 700; font-size: 14px; border: 1.5px solid #111; padding: 3px 8px; display: inline-block; }
.qo-titre { text-align: center; margin: 26px 0 18px; }
.qo-titre h1 { font-size: 19px; font-weight: 800; letter-spacing: 1px; margin: 0; }
.qo-mention { margin-top: 6px; font-style: italic; font-size: 11.5px; }
.qo-table { width: 100%; border-collapse: collapse; }
.qo-table th, .qo-table td { border: 1.2px solid #111; padding: 7px 10px; }
.qo-table th { width: 38%; background: #f4f4f4; text-align: left; font-weight: 600; }
.qo-mono { font-family: 'Courier New', monospace; font-weight: 700; }
.qo-droite { text-align: right; }
.qo-total th, .qo-total td { font-weight: 800; font-size: 14px; }
.qo-signature { margin-top: 36px; text-align: right; }
.qo-cachet { font-style: italic; margin-top: 4px; color: #333; min-height: 60px; }
</style>
