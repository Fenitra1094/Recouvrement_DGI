package com.dgi.recouvrement.service.paiement;

import com.dgi.recouvrement.dto.paiement.ResultatImportDTO;
import com.dgi.recouvrement.dto.paiement.ResultatImportDTO.LigneRejetee;
import com.dgi.recouvrement.entity.*;
import com.dgi.recouvrement.exception.RegleMetierException;
import com.dgi.recouvrement.repository.BordereauVersementRepository;
import com.dgi.recouvrement.repository.ModePaiementRepository;
import com.dgi.recouvrement.repository.PaiementRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Import du relevé bancaire, CSV (séparateur ";", UTF-8) ou Excel (.xlsx/.xls, première feuille),
 * première ligne = en-tête, colonnes dans cet ordre. Un PDF est refusé (montants non lisibles de façon fiable) :
 * <pre>
 * date_operation;reference_banque;reference_bv;montant;type;numero_cheque
 * 2026-11-12;OP-778899;BV-2026-000001;5000000;VIREMENT;
 * 2026-11-12;OP-778900;BV-2026-000002;3000000;CHEQUE_PAYE;0012345
 * 2026-11-12;OP-778901;BV-2026-000003;3000000;CHEQUE_IMPAYE;0012346
 * </pre>
 * - VIREMENT : paiement d'un bordereau par virement. Le bordereau devait être valable à la date de l'opération
 *   et le montant doit être celui du bordereau. Date de paiement = date de l'opération.
 * - CHEQUE_PAYE / CHEQUE_IMPAYE : réponse de la banque pour un chèque remis au guichet.
 * Chaque ligne est traitée dans sa propre transaction : une ligne refusée n'empêche pas les autres,
 * et elle est listée avec sa raison. Une même référence bancaire n'est jamais importée deux fois.
 */
@Service
public class ImportBanqueService {

    private static final DateTimeFormatter FORMAT_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EncaissementService encaissementService;
    private final BordereauService bordereauService;
    private final BordereauVersementRepository bordereauRepository;
    private final PaiementRepository paiementRepository;
    private final ModePaiementRepository modePaiementRepository;
    private final TransactionTemplate transaction;

    public ImportBanqueService(EncaissementService encaissementService, BordereauService bordereauService,
                               BordereauVersementRepository bordereauRepository, PaiementRepository paiementRepository,
                               ModePaiementRepository modePaiementRepository, PlatformTransactionManager transactionManager) {
        this.encaissementService = encaissementService;
        this.bordereauService = bordereauService;
        this.bordereauRepository = bordereauRepository;
        this.paiementRepository = paiementRepository;
        this.modePaiementRepository = modePaiementRepository;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    private enum Resultat { VIREMENT, CHEQUE_PAYE, CHEQUE_IMPAYE }

    public ResultatImportDTO importer(InputStream fichier, String nomFichier, String matriculeAgent) throws IOException {
        AgentDgi agent = encaissementService.agent(matriculeAgent);
        List<String[]> lignes = lire(fichier, nomFichier);
        int lues = 0, virements = 0, chequesPayes = 0, chequesImpayes = 0;
        List<LigneRejetee> rejetees = new ArrayList<>();

        for (int i = 1; i < lignes.size(); i++) { // ligne 0 = en-tête
            String[] c = lignes.get(i);
            if (String.join("", c).isBlank()) continue; // lignes vides
            lues++;
            try {
                Resultat r = transaction.execute(t -> traiterLigne(c, agent));
                switch (r) {
                    case VIREMENT -> virements++;
                    case CHEQUE_PAYE -> chequesPayes++;
                    case CHEQUE_IMPAYE -> chequesImpayes++;
                }
            } catch (RegleMetierException e) {
                rejetees.add(new LigneRejetee(i + 1, String.join(";", c), e.getMessage()));
            }
        }
        return new ResultatImportDTO(lues, virements, chequesPayes, chequesImpayes, rejetees);
    }

    /** Lignes du relevé, colonnes en texte, quel que soit le format (CSV ou Excel). */
    private static List<String[]> lire(InputStream fichier, String nomFichier) throws IOException {
        String nom = nomFichier == null ? "" : nomFichier.toLowerCase();
        if (nom.endsWith(".csv")) return lireCsv(fichier);
        if (nom.endsWith(".xlsx") || nom.endsWith(".xls")) return lireExcel(fichier);
        if (nom.endsWith(".pdf")) {
            throw new RegleMetierException("Un relevé PDF ne peut pas être importé : ses montants ne sont pas lisibles "
                    + "de façon fiable. Demandez à la banque l'export CSV ou Excel du relevé.");
        }
        throw new RegleMetierException("Format non pris en charge : fichier CSV (.csv) ou Excel (.xlsx, .xls) attendu.");
    }

    private static List<String[]> lireCsv(InputStream fichier) throws IOException {
        List<String[]> lignes = new ArrayList<>();
        try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(fichier, StandardCharsets.UTF_8))) {
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                if (lignes.isEmpty() && ligne.startsWith("﻿")) ligne = ligne.substring(1); // BOM (CSV enregistré par Excel)
                lignes.add(ligne.split(";", -1));
            }
        }
        return lignes;
    }

    /**
     * Première feuille du classeur, mêmes colonnes que le CSV. Les dates saisies comme dates Excel sont converties
     * en AAAA-MM-JJ et les nombres en entiers sans séparateur ; le reste est lu tel qu'affiché.
     */
    private static List<String[]> lireExcel(InputStream fichier) throws IOException {
        List<String[]> lignes = new ArrayList<>();
        DataFormatter affichage = new DataFormatter(Locale.FRANCE);
        try (Workbook classeur = WorkbookFactory.create(fichier)) {
            Sheet feuille = classeur.getSheetAt(0);
            for (int r = 0; r <= feuille.getLastRowNum(); r++) {
                Row row = feuille.getRow(r);
                String[] c = new String[6];
                for (int col = 0; col < 6; col++) {
                    Cell cell = row == null ? null : row.getCell(col);
                    c[col] = texte(cell, affichage);
                }
                lignes.add(c);
            }
        } catch (org.apache.poi.EmptyFileException | org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException
                 | org.apache.poi.poifs.filesystem.NotOLE2FileException e) {
            throw new RegleMetierException("Le fichier n'est pas un classeur Excel valide.");
        }
        return lignes;
    }

    private static String texte(Cell cell, DataFormatter affichage) {
        if (cell == null) return "";
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (type == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return affichage.formatCellValue(cell).trim();
    }

    private Resultat traiterLigne(String[] c, AgentDgi agent) {
        if (c.length < 5) throw new RegleMetierException("Ligne incomplète : 5 colonnes attendues au minimum.");
        LocalDate dateOperation = date(c[0].trim());
        String referenceBanque = obligatoire(c[1], "référence bancaire");
        String referenceBv = obligatoire(c[2], "référence du bordereau").toUpperCase();
        BigDecimal montant = montant(c[3]);
        String type = obligatoire(c[4], "type").toUpperCase();
        String numeroCheque = c.length > 5 ? c[5].trim() : "";

        if (paiementRepository.existsByReferenceBanque(referenceBanque)) {
            throw new RegleMetierException("Opération " + referenceBanque + " déjà importée.");
        }
        return switch (type) {
            case "VIREMENT" -> virement(dateOperation, referenceBanque, referenceBv, montant, agent);
            case "CHEQUE_PAYE", "CHEQUE_IMPAYE" -> {
                if (numeroCheque.isEmpty()) throw new RegleMetierException("Numéro de chèque manquant.");
                Paiement p = paiementRepository
                        .findFirstByBordereauReferenceBvAndNumeroChequeAndStatutPaiementLibelle(referenceBv, numeroCheque, "EN_ATTENTE")
                        .orElseThrow(() -> new RegleMetierException("Aucun chèque n° " + numeroCheque
                                + " en attente pour le bordereau " + referenceBv + "."));
                if (p.getMontantPayes().compareTo(montant) != 0) {
                    throw new RegleMetierException("Montant différent de celui du chèque remis ("
                            + p.getMontantPayes().toPlainString() + " Ar).");
                }
                p.setReferenceBanque(referenceBanque);
                if ("CHEQUE_PAYE".equals(type)) {
                    encaissementService.confirmerCheque(p.getId());
                    yield Resultat.CHEQUE_PAYE;
                }
                encaissementService.rejeterCheque(p.getId());
                yield Resultat.CHEQUE_IMPAYE;
            }
            default -> throw new RegleMetierException("Type inconnu : " + type + " (VIREMENT, CHEQUE_PAYE ou CHEQUE_IMPAYE).");
        };
    }

    private Resultat virement(LocalDate dateOperation, String referenceBanque, String referenceBv,
                              BigDecimal montant, AgentDgi agent) {
        BordereauVersement bv = bordereauRepository.findByReferenceBv(referenceBv)
                .orElseThrow(() -> new RegleMetierException("Bordereau introuvable : " + referenceBv + "."));
        Declaration d = bv.getDeclaration();
        bordereauService.verifierPayable(d);
        // Le bordereau devait être valable le jour du virement (il a pu être remplacé depuis, faute de paiement connu)
        boolean valableCeJour = ("A_PAYER".equals(bv.getStatut()) || "ANNULE".equals(bv.getStatut()))
                && !dateOperation.isBefore(bv.getDateEmission()) && !dateOperation.isAfter(bv.getDateEcheance());
        if (!valableCeJour) {
            throw new RegleMetierException("Le bordereau " + referenceBv + " n'était pas valable le "
                    + dateOperation.format(FORMAT_FR) + ".");
        }
        if (montant.compareTo(bv.getMontantTotal()) != 0) {
            throw new RegleMetierException("Le virement doit couvrir la totalité du bordereau ("
                    + bv.getMontantTotal().toPlainString() + " Ar).");
        }
        // Bordereau de remplacement éventuellement émis depuis : il n'a plus lieu d'être
        bordereauRepository.findByDeclarationIdOrderByIdDesc(d.getId()).stream()
                .filter(autre -> !autre.getId().equals(bv.getId()) && "A_PAYER".equals(autre.getStatut()))
                .forEach(autre -> {
                    autre.setStatut("ANNULE");
                    bordereauRepository.save(autre);
                });

        Paiement p = new Paiement();
        p.setMontantPayes(montant);
        p.setDatePaiement(dateOperation);
        p.setReferenceTransaction(encaissementService.reference("VIR", dateOperation));
        p.setReferenceBanque(referenceBanque);
        p.setModePaiement(modePaiementRepository.findByCode("VIREMENT").orElseThrow());
        p.setStatutPaiement(encaissementService.statut("VALIDE"));
        p.setBordereau(bv);
        p.setAgent(agent);
        p = paiementRepository.save(p);
        encaissementService.solder(p, bordereauService.penalitesBrutes(d, dateOperation));
        return Resultat.VIREMENT;
    }

    private static LocalDate date(String texte) {
        try {
            return texte.contains("/") ? LocalDate.parse(texte, FORMAT_FR) : LocalDate.parse(texte);
        } catch (DateTimeParseException e) {
            throw new RegleMetierException("Date invalide : " + texte + " (AAAA-MM-JJ ou JJ/MM/AAAA).");
        }
    }

    private static BigDecimal montant(String texte) {
        try {
            BigDecimal m = new BigDecimal(texte.replace(" ", "").replace(" ", "").replace(",", "."));
            if (m.signum() <= 0) throw new NumberFormatException();
            return m;
        } catch (NumberFormatException e) {
            throw new RegleMetierException("Montant invalide : " + texte + ".");
        }
    }

    private static String obligatoire(String valeur, String nom) {
        if (valeur == null || valeur.isBlank()) throw new RegleMetierException("Colonne vide : " + nom + ".");
        return valeur.trim();
    }
}
