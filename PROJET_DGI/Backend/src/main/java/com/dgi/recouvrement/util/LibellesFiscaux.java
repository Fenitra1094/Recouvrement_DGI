package com.dgi.recouvrement.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Libellés lisibles pour les messages envoyés au contribuable (mêmes règles que utils/fiscal.js côté front). */
public final class LibellesFiscaux {

    private static final String[] MOIS = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LibellesFiscaux() {}

    /** M12 -> "Décembre 2025", T4 -> "4e trimestre 2025", A2024 -> "Exercice 2024". */
    public static String periode(String code, Integer annee) {
        if (code == null || code.isBlank()) return annee != null ? String.valueOf(annee) : "";
        String suffixe = annee != null ? " " + annee : "";
        try {
            switch (code.charAt(0)) {
                case 'M' -> { return MOIS[Integer.parseInt(code.substring(1)) - 1] + suffixe; }
                case 'T' -> {
                    int t = Integer.parseInt(code.substring(1));
                    return (t == 1 ? "1er" : t + "e") + " trimestre" + suffixe;
                }
                case 'A' -> { return "Exercice " + code.substring(1); }
                default -> { }
            }
        } catch (RuntimeException ignore) {
            // code de période non standard : affiché tel quel
        }
        return code + suffixe;
    }

    /** 12500000 -> "12 500 000 Ar" */
    public static String montant(BigDecimal v) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0", symboles).format(v != null ? v : BigDecimal.ZERO) + " Ar";
    }

    public static String date(LocalDate d) {
        return d != null ? d.format(FORMAT_DATE) : "";
    }
}
