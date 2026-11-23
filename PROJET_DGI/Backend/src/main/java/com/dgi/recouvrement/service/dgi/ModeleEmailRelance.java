package com.dgi.recouvrement.service.dgi;

import com.dgi.recouvrement.entity.Contribuable;
import com.dgi.recouvrement.entity.Relance;
import com.dgi.recouvrement.service.declaration.CalculImpot;
import com.dgi.recouvrement.util.LibellesFiscaux;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Rédaction des relances : titre, texte de la notification et email HTML.
 * Styles en ligne : les messageries (Gmail, Outlook) ignorent les feuilles de style.
 */
@Component
public class ModeleEmailRelance {

    private final CalculImpot calculImpot;

    public ModeleEmailRelance(CalculImpot calculImpot) {
        this.calculImpot = calculImpot;
    }

    /** "2 obligations impayées et 3 déclarations manquantes", selon ce qui est relancé. */
    private static String objet(int impayees, int nonDeclarees) {
        List<String> parties = new ArrayList<>();
        if (impayees > 0) parties.add(impayees > 1 ? impayees + " obligations fiscales impayées" : "1 obligation fiscale impayée");
        if (nonDeclarees > 0) parties.add(nonDeclarees > 1 ? nonDeclarees + " déclarations manquantes" : "1 déclaration manquante");
        return String.join(" et ", parties);
    }

    public String titre(String niveau, int impayees, int nonDeclarees) {
        String objet = objet(impayees, nonDeclarees);
        return switch (niveau) {
            case "MISE_EN_DEMEURE" -> "Mise en demeure : " + objet;
            case "RELANCE_2" -> "Deuxième relance : " + objet;
            default -> "Relance : " + objet;
        };
    }

    public String message(String niveau, int impayees, int nonDeclarees, BigDecimal total) {
        StringBuilder constat = new StringBuilder();
        if (impayees > 0) {
            constat.append(impayees > 1 ? impayees + " obligations fiscales restent impayées" : "1 obligation fiscale reste impayée")
                    .append(" après la date limite, pour un total de ").append(LibellesFiscaux.montant(total))
                    .append(" (principal et pénalités de retard). ");
        }
        if (nonDeclarees > 0) {
            constat.append(nonDeclarees > 1 ? nonDeclarees + " déclarations attendues n'ont pas été déposées"
                            : "1 déclaration attendue n'a pas été déposée")
                    .append(" alors que la date limite est passée. Déposez-les depuis « Déclarations » : ")
                    .append("le montant dû sera calculé à la déclaration. ");
        }
        return constat + switch (niveau) {
            case "MISE_EN_DEMEURE" -> "Malgré nos relances précédentes, votre situation n'a pas été régularisée. "
                    + "Vous êtes mis en demeure de la régulariser dans un délai de 8 jours. À défaut, l'administration "
                    + "pourra engager des mesures de recouvrement forcé, notamment un avis à tiers détenteur (ATD).";
            case "RELANCE_2" -> "Malgré notre précédente relance, votre situation n'a pas été régularisée."
                    + (impayees > 0 ? " Les pénalités de retard continuent de s'appliquer chaque mois (1 % du principal)." : "");
            default -> "Nous vous invitons à régulariser votre situation dans les meilleurs délais."
                    + (impayees > 0 ? " Si vous ne pouvez pas payer en une fois, vous pouvez demander un échéancier depuis votre espace." : "");
        };
    }

    /** Date limite d'une relance : celle de la déclaration, ou calculée depuis l'impôt et la période si non déclarée. */
    public LocalDate dateLimite(Relance r) {
        if (r.getDette() != null) return r.getDette().getDeclaration().getDateLimitePaiement();
        return calculImpot.dateLimite(r.getTypeImpots(), r.getPeriode(), r.getAnnee());
    }

    public String sujet(String titre, Contribuable c) {
        return "[DGI Madagascar] " + titre + " - NIF " + c.getNif();
    }

    public String html(Contribuable c, String niveau, String titre, String message, List<Relance> relances,
                       BigDecimal total, String lienPlateforme) {
        String couleur = switch (niveau) {
            case "MISE_EN_DEMEURE" -> "#b42318";
            case "RELANCE_2" -> "#b54708";
            default -> "#1d4ed8";
        };

        StringBuilder lignes = new StringBuilder();
        for (Relance r : relances) {
            String impot = r.getTypeImpots() != null ? r.getTypeImpots().getCode() : "";
            String montant = r.estNonDeclaree()
                    ? "<span style=\"color:#b42318\">Non déclarée</span>"
                    : "<strong>" + LibellesFiscaux.montant(r.getMontantDu()) + "</strong>";
            lignes.append("<tr>")
                    .append(td(esc(impot) + "<br><span style=\"color:#64748b;font-size:12px\">"
                            + esc(LibellesFiscaux.periode(r.getPeriode(), r.getAnnee())) + "</span>", "left"))
                    .append(td(LibellesFiscaux.date(dateLimite(r)), "left"))
                    .append(td(r.getJoursRetard() + " j", "right"))
                    .append(td(montant, "right"))
                    .append("</tr>");
        }

        // Récapitulatif : un montant n'existe que pour ce qui est déclaré ; une déclaration manquante
        // n'a pas encore de montant (il sera calculé quand elle sera déposée), on ne l'affiche donc pas à 0 Ar.
        long nonDeclarees = relances.stream().filter(Relance::estNonDeclaree).count();
        long impayees = relances.size() - nonDeclarees;
        StringBuilder recap = new StringBuilder();
        if (impayees > 0) {
            recap.append("<tr><td colspan=\"3\" style=\"padding:12px 10px;font-weight:bold\">Total à payer")
                    .append(nonDeclarees > 0 ? " (déclarations déposées)" : "").append("</td>")
                    .append("<td align=\"right\" style=\"padding:12px 10px;font-weight:bold;font-size:16px;color:")
                    .append(couleur).append("\">").append(LibellesFiscaux.montant(total)).append("</td></tr>");
        }
        if (nonDeclarees > 0) {
            recap.append("<tr><td colspan=\"3\" style=\"padding:12px 10px;font-weight:bold\">Déclarations à déposer")
                    .append("<br><span style=\"font-weight:normal;color:#64748b;font-size:12px\">")
                    .append("Le montant dû sera calculé au dépôt de chaque déclaration, pénalités de retard comprises.</span></td>")
                    .append("<td align=\"right\" style=\"padding:12px 10px;font-weight:bold;font-size:16px;color:#b42318\">")
                    .append(nonDeclarees).append("</td></tr>");
        }

        return """
                <!doctype html>
                <html lang="fr"><body style="margin:0;padding:24px 0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;color:#1e293b">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e6eaf0">
                  <tr><td style="background:#0f2a4a;padding:20px 28px;color:#ffffff">
                    <div style="font-size:18px;font-weight:bold;letter-spacing:1px">HETRA-RINDRA</div>
                    <div style="font-size:12px;color:#9cc4ff">Direction Générale des Impôts - Madagascar</div>
                  </td></tr>
                  <tr><td style="padding:0 28px"><div style="height:4px;background:%s"></div></td></tr>
                  <tr><td style="padding:24px 28px 8px">
                    <div style="font-size:12px;font-weight:bold;letter-spacing:1px;color:%s;text-transform:uppercase">%s</div>
                    <p style="margin:16px 0 4px">Madame, Monsieur,</p>
                    <p style="margin:0 0 16px;color:#475467"><strong style="color:#1e293b">%s</strong> - NIF %s</p>
                    <p style="margin:0 0 20px;line-height:1.55">%s</p>
                  </td></tr>
                  <tr><td style="padding:0 28px">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;font-size:14px">
                      <tr style="background:#f8fafc;color:#64748b;font-size:12px;text-transform:uppercase">
                        <th align="left" style="padding:10px;border-bottom:1px solid #e6eaf0">Impôt</th>
                        <th align="left" style="padding:10px;border-bottom:1px solid #e6eaf0">Date limite</th>
                        <th align="right" style="padding:10px;border-bottom:1px solid #e6eaf0">Retard</th>
                        <th align="right" style="padding:10px;border-bottom:1px solid #e6eaf0">Montant dû</th>
                      </tr>
                      %s
                      %s
                    </table>
                  </td></tr>
                  <tr><td align="center" style="padding:24px 28px">
                    <a href="%s" style="display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;font-weight:bold;padding:12px 24px;border-radius:8px">Consulter mon espace contribuable</a>
                  </td></tr>
                  <tr><td style="padding:16px 28px;background:#f8fafc;color:#64748b;font-size:12px;line-height:1.5">
                    Les montants sont calculés à la date de cet envoi ; les pénalités de retard augmentent de 1 %% du principal par mois entamé.
                    Si vous avez déjà réglé ces sommes, merci de ne pas tenir compte de ce message.<br>
                    Message envoyé automatiquement, merci de ne pas y répondre.
                  </td></tr>
                </table>
                </td></tr></table>
                </body></html>
                """.formatted(couleur, couleur, esc(titre), esc(c.getRaisonSociale()), esc(c.getNif()), esc(message),
                lignes, recap, esc(lienPlateforme));
    }

    private static String td(String contenu, String alignement) {
        return "<td align=\"" + alignement + "\" style=\"padding:10px;border-bottom:1px solid #e6eaf0\">" + contenu + "</td>";
    }

    private static String esc(String texte) {
        return texte != null ? HtmlUtils.htmlEscape(texte, "UTF-8") : "";
    }
}
