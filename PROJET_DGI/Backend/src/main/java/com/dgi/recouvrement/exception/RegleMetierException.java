package com.dgi.recouvrement.exception;

/** Règle métier non respectée (déclaration en double, bordereau déjà payé…) : renvoyée au client en HTTP 400. */
public class RegleMetierException extends RuntimeException {
    public RegleMetierException(String message) {
        super(message);
    }
}
