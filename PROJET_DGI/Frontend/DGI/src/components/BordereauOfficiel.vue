<template>
  <!--
    Bordereau de versement au format des imprimés DGI (feuille A4).
    Même fichier dans les deux frontends : Frontend/CONTRIBUABLE et Frontend/DGI (src/components/BordereauOfficiel.vue).
    Le PDF est produit à partir de cet élément (utils/pdf.js), il doit donc rester autonome : pas de style externe.
  -->
  <!-- Aperçu réduit à la largeur disponible ; le PDF est fait sur la feuille, à taille réelle -->
  <div ref="apercu" class="bvo-apercu" :style="{ height: hauteurApercu }">
  <div class="bvo-echelle" :style="{ transform: `scale(${echelle})` }">
  <div ref="feuille" class="bvo-feuille">
    <!-- En-tête administratif -->
    <header class="bvo-entete">
      <div class="bvo-entete-gauche">
        <div>MINISTÈRE DE L'ÉCONOMIE ET DES FINANCES</div>
        <div class="bvo-tirets">--------------------</div>
        <div>DIRECTION GÉNÉRALE DES IMPÔTS</div>
        <div class="bvo-tirets">--------------------</div>
        <div>{{ (bv.centreFiscal || 'Centre fiscal').toUpperCase() }}</div>
      </div>
      <div class="bvo-entete-centre">
        <div class="bvo-republique">REPOBLIKAN'I MADAGASIKARA</div>
        <div class="bvo-devise">Fitiavana – Tanindrazana – Fandrosoana</div>
      </div>
      <div class="bvo-entete-droite">
        <div class="bvo-numero-libelle">N°</div>
        <div class="bvo-numero">{{ bv.reference }}</div>
        <div class="bvo-emis">Émis le {{ date(bv.dateEmission) }}</div>
      </div>
    </header>

    <div class="bvo-titre">
      <h1>BORDEREAU DE VERSEMENT</h1>
      <h2>{{ (bv.impotLibelle || bv.impot || '').toUpperCase() }}</h2>
      <div class="bvo-periode">{{ libellePeriode }}</div>
    </div>

    <!-- Identification -->
    <section class="bvo-cadre">
      <div class="bvo-cadre-titre">IDENTIFICATION DU CONTRIBUABLE</div>
      <div class="bvo-ligne">
        <span class="bvo-libelle">N° d'identification fiscale (NIF) :</span>
        <span class="bvo-cases">
          <span v-for="(c, i) in casesNif" :key="i" class="bvo-case">{{ c }}</span>
        </span>
      </div>
      <div class="bvo-ligne"><span class="bvo-libelle">Raison sociale :</span><span class="bvo-valeur">{{ bv.raisonSociale }}</span></div>
      <div class="bvo-ligne"><span class="bvo-libelle">Adresse :</span><span class="bvo-valeur">{{ bv.adresse || '—' }}</span></div>
      <div class="bvo-ligne"><span class="bvo-libelle">Téléphone :</span><span class="bvo-valeur">{{ bv.telephone || '—' }}</span></div>
    </section>

    <!-- Déclaration -->
    <section class="bvo-cadre">
      <div class="bvo-cadre-titre">DÉCLARATION</div>
      <div class="bvo-grille">
        <div class="bvo-ligne"><span class="bvo-libelle">Impôt :</span><span class="bvo-valeur">{{ bv.impot }} — {{ bv.impotLibelle }}</span></div>
        <div class="bvo-ligne"><span class="bvo-libelle">Période :</span><span class="bvo-valeur">{{ libellePeriode }}</span></div>
        <div class="bvo-ligne"><span class="bvo-libelle">Déclarée le :</span><span class="bvo-valeur">{{ date(bv.dateDeclaration) }} {{ canal }}</span></div>
        <div class="bvo-ligne"><span class="bvo-libelle">Date limite de paiement :</span><span class="bvo-valeur">{{ date(bv.dateLimite) }}</span></div>
      </div>
    </section>

    <!-- Calcul -->
    <table class="bvo-calcul">
      <thead>
        <tr><th colspan="2">CALCUL DU MONTANT À VERSER</th><th class="bvo-montant">Montant (Ar)</th></tr>
      </thead>
      <tbody>
        <tr v-for="l in lignesCalcul" :key="l.lettre" :class="{ 'bvo-total': l.total }">
          <td class="bvo-lettre">{{ l.lettre }}.</td>
          <td>{{ l.libelle }}<span v-if="l.detail" class="bvo-detail"> {{ l.detail }}</span></td>
          <td class="bvo-montant">{{ montant(l.valeur) }}</td>
        </tr>
      </tbody>
    </table>
    <div class="bvo-lettres">
      Arrêté le présent bordereau à la somme de : <strong>{{ enLettres }}</strong>
    </div>

    <div class="bvo-validite">
      Bordereau valable jusqu'au <strong>{{ date(bv.dateEcheance) }}</strong>. Paiement en totalité, au guichet
      (espèces ou chèque) ou par virement en indiquant la référence <strong>{{ bv.reference }}</strong>.
      Après cette date, le montant change : un nouveau bordereau est émis.
    </div>

    <!-- Cadre réservé à l'administration -->
    <section class="bvo-admin">
      <div class="bvo-admin-titre">CADRE RÉSERVÉ À L'ADMINISTRATION</div>
      <table class="bvo-admin-table">
        <thead>
          <tr><th>Net à payer</th><th>Pénalités</th><th>Total versé</th><th>Reste à recouvrer</th><th>N° récépissé</th></tr>
        </thead>
        <tbody>
          <tr><td>{{ montant(bv.montant) }}</td><td>{{ montant(bv.penalites) }}</td><td></td><td></td><td></td></tr>
        </tbody>
      </table>
      <div class="bvo-admin-bas">
        <div class="bvo-modes">
          <div class="bvo-mode-titre">Mode de paiement</div>
          <div><span class="bvo-coche"></span> Espèces</div>
          <div><span class="bvo-coche"></span> Chèque N° ............................ Banque ............................</div>
          <div><span class="bvo-coche"></span> Virement bancaire</div>
        </div>
        <div class="bvo-signature">
          <div>A ............................, le ....................</div>
          <div class="bvo-cachet">Cachet, signature et titre</div>
        </div>
      </div>
    </section>
  </div>
  </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  /** BordereauDTO renvoyé par l'API (référence, montants, contribuable, déclaration) */
  bv: { type: Object, required: true },
})

/** L'élément de la feuille, pour le téléchargement PDF (utils/pdf.js). */
const feuille = ref(null)
defineExpose({ feuille })

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

const MOIS = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre']
// Libellé de la base imposable selon l'impôt (même vocabulaire que l'écran de déclaration)
const BASES = {
  IS: "Chiffre d'affaires annuel",
  IR: 'Revenu net imposable de l\'exercice',
  TVA: "Chiffre d'affaires hors taxes du mois",
  IRSA: 'Salaires imposables du mois',
}

const libellePeriode = computed(() => {
  const p = props.bv.periode || ''
  let m
  if ((m = p.match(/^M(\d{1,2})$/))) return `Mois de ${MOIS[Number(m[1]) - 1].toLowerCase()} ${props.bv.annee ?? ''}`.trim()
  if ((m = p.match(/^A(\d{4})$/))) return `Exercice ${m[1]}`
  return `${p} ${props.bv.annee ?? ''}`.trim()
})

const canal = computed(() => {
  if (props.bv.canal === 'EN_LIGNE') return '(en ligne)'
  if (props.bv.canal === 'GUICHET') return '(au guichet)'
  return ''
})

const casesNif = computed(() => {
  const nif = String(props.bv.nif || '')
  return Array.from({ length: Math.max(10, nif.length) }, (_, i) => nif[i] || '')
})

/** Lignes A, B, C… comme sur les imprimés : seules les lignes utiles sont affichées. */
const lignesCalcul = computed(() => {
  const b = props.bv
  const lignes = []
  const ajouter = (libelle, valeur, detail, total = false) =>
    lignes.push({ lettre: String.fromCharCode(65 + lignes.length), libelle, valeur, detail, total })

  if (b.baseImposable != null) ajouter(BASES[b.impot] || 'Base imposable', b.baseImposable)
  if (b.tvaDeductible != null && Number(b.tvaDeductible) > 0) ajouter('TVA déductible du mois', b.tvaDeductible)
  ajouter('Impôt dû', b.impotDu)
  const penalites = Number(b.penalitesTotales || 0)
  if (penalites > 0 || Number(b.joursRetard) > 0) {
    ajouter('Pénalités de retard', b.penalitesTotales, b.joursRetard ? `(${b.joursRetard} jours de retard)` : '')
  }
  if (Number(b.dejaVerse || 0) > 0) ajouter('À déduire : versements déjà effectués', b.dejaVerse)
  ajouter('NET À PAYER', b.montant, '', true)
  return lignes
})

const enLettres = computed(() => `${nombreEnLettres(Math.round(Number(props.bv.montant || 0)))} ariary`.replace(/^./, (c) => c.toUpperCase()))

function montant(v) {
  return Number(v || 0).toLocaleString('fr-FR', { maximumFractionDigits: 0 })
}
function date(iso) {
  return iso ? new Date(iso).toLocaleDateString('fr-FR') : '—'
}

// ---------- Montant en lettres (règles du français : vingt, cent, mille, million, milliard) ----------
const UNITES = ['zéro', 'un', 'deux', 'trois', 'quatre', 'cinq', 'six', 'sept', 'huit', 'neuf', 'dix',
  'onze', 'douze', 'treize', 'quatorze', 'quinze', 'seize', 'dix-sept', 'dix-huit', 'dix-neuf']
const DIZAINES = ['', '', 'vingt', 'trente', 'quarante', 'cinquante', 'soixante', 'soixante', 'quatre-vingt', 'quatre-vingt']

function moinsDeCent(n) {
  if (n < 20) return UNITES[n]
  const d = Math.floor(n / 10)
  let u = n % 10
  if (d === 7 || d === 9) u += 10 // soixante-dix, quatre-vingt-dix
  let mot = DIZAINES[d]
  if (u === 0) return d === 8 ? 'quatre-vingts' : mot
  if ((u === 1 || u === 11) && d < 8) return `${mot} et ${UNITES[u]}`
  return `${mot}-${UNITES[u]}`
}

function moinsDeMille(n, final) {
  const c = Math.floor(n / 100)
  const r = n % 100
  let mot = ''
  if (c > 0) mot = c === 1 ? 'cent' : `${UNITES[c]} cent${r === 0 && final ? 's' : ''}`
  if (r > 0) mot += (mot ? ' ' : '') + moinsDeCent(r)
  return mot
}

function nombreEnLettres(n) {
  if (!n) return 'zéro'
  const parties = []
  const echelles = [[1e9, 'milliard'], [1e6, 'million'], [1e3, 'mille']]
  for (const [valeur, nom] of echelles) {
    const q = Math.floor(n / valeur)
    if (q > 0) {
      if (nom === 'mille') parties.push(q === 1 ? 'mille' : `${moinsDeMille(q, false)} mille`)
      else parties.push(`${moinsDeMille(q, false)} ${nom}${q > 1 ? 's' : ''}`)
      n %= valeur
    }
  }
  if (n > 0) parties.push(moinsDeMille(n, true))
  return parties.join(' ')
}
</script>

<style scoped>
.bvo-apercu { width: 100%; overflow: hidden; }
.bvo-echelle { width: 794px; transform-origin: top left; }
.bvo-feuille {
  width: 794px; /* A4 à 96 ppp */
  min-height: 1060px;
  margin: 0 auto;
  padding: 40px 44px;
  background: #fff;
  color: #111;
  font-family: Arial, Helvetica, sans-serif;
  font-size: 12.5px;
  line-height: 1.45;
  box-sizing: border-box;
}
.bvo-entete { display: grid; grid-template-columns: 1fr 1.1fr 1fr; gap: 12px; align-items: start; font-size: 10.5px; }
.bvo-entete-gauche { text-align: center; font-weight: 700; letter-spacing: 0.2px; }
.bvo-tirets { font-weight: 400; color: #555; line-height: 1; }
.bvo-entete-centre { text-align: center; }
.bvo-republique { font-weight: 700; font-size: 12px; letter-spacing: 0.5px; }
.bvo-devise { font-style: italic; color: #333; }
.bvo-entete-droite { text-align: right; }
.bvo-numero-libelle { color: #555; }
.bvo-numero { font-family: 'Courier New', monospace; font-weight: 700; font-size: 14px; border: 1.5px solid #111; padding: 3px 8px; display: inline-block; }
.bvo-emis { margin-top: 4px; color: #333; }

.bvo-titre { text-align: center; margin: 26px 0 18px; }
.bvo-titre h1 { font-size: 19px; font-weight: 800; letter-spacing: 1px; margin: 0; }
.bvo-titre h2 { font-size: 14px; font-weight: 700; margin: 4px 0 0; }
.bvo-periode { margin-top: 4px; font-size: 13px; }

.bvo-cadre { border: 1.2px solid #111; padding: 10px 14px 8px; margin-bottom: 12px; }
.bvo-cadre-titre { text-align: center; font-weight: 700; text-decoration: underline; margin-bottom: 8px; }
.bvo-grille { display: grid; grid-template-columns: 1fr 1fr; column-gap: 18px; }
.bvo-ligne { display: flex; align-items: baseline; gap: 6px; margin-bottom: 5px; }
.bvo-libelle { white-space: nowrap; }
.bvo-valeur { flex: 1; border-bottom: 1px dotted #777; font-weight: 600; min-height: 17px; }
.bvo-cases { display: inline-flex; }
.bvo-case { width: 22px; height: 22px; border: 1px solid #111; margin-left: -1px; text-align: center; line-height: 21px; font-family: 'Courier New', monospace; font-weight: 700; font-size: 13px; }

.bvo-calcul { width: 100%; border-collapse: collapse; margin-top: 4px; }
.bvo-calcul th, .bvo-calcul td { border: 1.2px solid #111; padding: 6px 10px; }
.bvo-calcul th { background: #f1f1f1; text-align: left; font-weight: 700; }
.bvo-lettre { width: 34px; text-align: center; font-weight: 700; }
.bvo-montant { width: 170px; text-align: right; font-family: 'Courier New', monospace; font-weight: 700; white-space: nowrap; }
.bvo-detail { color: #444; font-size: 11.5px; }
.bvo-total td { font-weight: 800; font-size: 14px; background: #f7f7f7; }
.bvo-lettres { margin: 8px 0 12px; }
.bvo-validite { border-left: 3px solid #111; padding: 6px 10px; background: #fafafa; font-size: 11.5px; }

.bvo-admin { margin-top: 26px; }
.bvo-admin-titre { text-align: center; font-weight: 700; margin-bottom: 6px; }
.bvo-admin-table { width: 100%; border-collapse: collapse; text-align: center; }
.bvo-admin-table th, .bvo-admin-table td { border: 1.2px solid #111; padding: 5px 6px; font-size: 11.5px; }
.bvo-admin-table th { font-weight: 600; }
.bvo-admin-table td { height: 30px; font-family: 'Courier New', monospace; font-weight: 700; }
.bvo-admin-bas { display: flex; justify-content: space-between; gap: 20px; margin-top: 14px; }
.bvo-mode-titre { font-weight: 700; margin-bottom: 4px; }
.bvo-modes div { margin-bottom: 4px; }
.bvo-coche { display: inline-block; width: 11px; height: 11px; border: 1px solid #111; margin-right: 6px; vertical-align: -1px; }
.bvo-signature { text-align: right; align-self: flex-end; }
.bvo-cachet { font-style: italic; margin-top: 4px; color: #333; }
</style>
