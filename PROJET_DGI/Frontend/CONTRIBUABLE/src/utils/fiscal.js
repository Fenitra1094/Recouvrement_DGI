// Libellés et formats partagés par les pages de l'espace contribuable

// Nom complet et couleur d'identification de chaque type d'impôt
export const IMPOTS = {
  ISYN: { libelle: 'Impôt synthétique',               couleur: '#7c3aed' },
  IR:   { libelle: 'Impôt sur le revenu',             couleur: '#0e7490' },
  IS:   { libelle: 'Impôt sur les sociétés',          couleur: '#1d4ed8' },
  TVA:  { libelle: 'Taxe sur la valeur ajoutée',      couleur: '#c2410c' },
  IRSA: { libelle: 'Impôt sur les revenus salariaux', couleur: '#be185d' },
}
export const impot = (code) => IMPOTS[code] || { libelle: code, couleur: '#475467' }

export const MOIS = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août',
  'Septembre', 'Octobre', 'Novembre', 'Décembre']

// "M12" -> "Décembre 2025", "T4" -> "4e trimestre 2025", "A2024" -> "Exercice 2024"
export function libellePeriode(e) {
  const p = e.periode || ''
  let m
  if ((m = p.match(/^M(\d{1,2})$/))) return `${MOIS[Number(m[1]) - 1]} ${e.annee ?? ''}`.trim()
  if ((m = p.match(/^T([1-4])$/))) return `${m[1]}${m[1] === '1' ? 'er' : 'e'} trimestre ${e.annee ?? ''}`.trim()
  if ((m = p.match(/^A(\d{4})$/))) return `Exercice ${m[1]}`
  return `${p} ${e.annee ?? ''}`.trim()
}

export function typePeriode(e) {
  const p = e.periode || ''
  if (p.startsWith('M')) return 'Mensuelle'
  if (p.startsWith('T')) return 'Trimestrielle'
  if (p.startsWith('A')) return 'Annuelle'
  return ''
}

export const MODES_PAIEMENT = {
  VIREMENT:     { libelle: 'Virement bancaire', icone: 'bi-bank' },
  MOBILE_MONEY: { libelle: 'Mobile money',      icone: 'bi-phone' },
  ESPECES:      { libelle: 'Espèces (guichet)', icone: 'bi-cash-stack' },
  CHEQUE:       { libelle: 'Chèque',            icone: 'bi-journal-check' },
}
export const modePaiement = (code) => MODES_PAIEMENT[code] || { libelle: code || '—', icone: 'bi-credit-card' }

export function formatMontant(v) {
  return Number(v || 0).toLocaleString('fr-FR', { maximumFractionDigits: 0 })
}

export function formatDate(iso, options = { day: '2-digit', month: '2-digit', year: 'numeric' }) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('fr-FR', options)
}
