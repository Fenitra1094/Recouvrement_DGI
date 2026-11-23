// Téléchargement d'un document (bordereau, quittance) en PDF A4, sans imprimante.
// Même fichier dans Frontend/CONTRIBUABLE et Frontend/DGI (src/utils/pdf.js).
// La librairie n'est chargée qu'au premier téléchargement (elle est lourde).

export async function telechargerPdf(element, nomFichier) {
  if (!element) return
  const { default: html2pdf } = await import('html2pdf.js')
  await html2pdf()
    .set({
      margin: 0,
      filename: nomFichier.endsWith('.pdf') ? nomFichier : `${nomFichier}.pdf`,
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: { scale: 2, useCORS: true, backgroundColor: '#ffffff' },
      jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
      pagebreak: { mode: ['avoid-all'] },
    })
    .from(element)
    .save()
}
