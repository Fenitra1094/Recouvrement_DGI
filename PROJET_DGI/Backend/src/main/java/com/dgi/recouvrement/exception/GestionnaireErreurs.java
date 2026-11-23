package com.dgi.recouvrement.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Les règles métier refusées sont renvoyées en 400 avec un message lisible, au lieu d'une erreur 500. */
@RestControllerAdvice
public class GestionnaireErreurs {

    @ExceptionHandler(RegleMetierException.class)
    public ResponseEntity<Map<String, String>> regleMetier(RegleMetierException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
