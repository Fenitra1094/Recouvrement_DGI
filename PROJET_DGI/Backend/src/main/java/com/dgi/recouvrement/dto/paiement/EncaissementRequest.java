package com.dgi.recouvrement.dto.paiement;

import java.math.BigDecimal;

/**
 * Encaissement au guichet d'un bordereau, payé en totalité.
 * @param modePaiement   ESPECES ou CHEQUE
 * @param numeroCheque   chèque uniquement
 * @param banque         chèque uniquement
 */
public record EncaissementRequest(String referenceBv, String modePaiement, BigDecimal montantRecu,
                                  String numeroCheque, String banque, String matriculeAgent) {
}
