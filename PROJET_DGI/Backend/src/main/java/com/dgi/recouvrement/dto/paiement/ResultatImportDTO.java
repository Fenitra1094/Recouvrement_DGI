package com.dgi.recouvrement.dto.paiement;

import java.util.List;

/** Bilan d'un import de relevé bancaire : chaque ligne est soit acceptée, soit rejetée avec sa raison. */
public record ResultatImportDTO(int lignesLues, int paiementsValides, int chequesConfirmes, int chequesRejetes,
                                List<LigneRejetee> lignesRejetees) {

    public record LigneRejetee(int numeroLigne, String contenu, String raison) {
    }
}
