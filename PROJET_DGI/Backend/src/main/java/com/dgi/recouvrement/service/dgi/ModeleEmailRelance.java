package com.dgi.recouvrement.service.dgi;

import com.dgi.recouvrement.entity.Contribuable;
import com.dgi.recouvrement.entity.Declaration;
import com.dgi.recouvrement.entity.Relance;
import com.dgi.recouvrement.util.LibellesFiscaux;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rédaction des relances : titre, texte de la notification et email HTML.
 * Styles en ligne : les messageries (Gmail, Outlook) ignorent les feuilles de style.
 */
@Component
public class ModeleEmailRelance {

    public String titre(String niveau, int nombre) {
        String obligations = nombre > 1 ? nombre + " obligations fiscales impayées" : "1 obligation fiscale impayée";
        return switch (niveau) {
            case "MISE_EN_DEMEURE" -> "Mise en demeure de payer : " + obligations;
            case "RELANCE_2" -> "Deuxième relance : " + obligations;
            default -> "Relance : " + obligations;
        };
    }

    public String message(String niveau, int nombre, BigDecimal total) {
        String constat = (nombre > 1 ? nombre + " obligations fiscales restent impayées" : "1 obligation fiscale reste impayée")
                + " après la date limite, pour un total de " + LibellesFiscaux.montant(total)
                + " (principal et pénalités de retard). ";
        return constat + switch (niveau) {
            case "MISE_EN_DEMEURE" -> "Malgré nos relances précédentes, ces sommes n'ont pas été réglées. "
                    + "Vous êtes mis en demeure de les payer dans un délai de 8 jours. À défaut, l'administration "
                    + "pourra engager des mesures de recouvrement forcé, notamment un avis à tiers détenteur (ATD).";
            case "RELANCE_2" -> "Malgré notre précédente relance, ces montants restent dus. Les pénalités de retard "
                    + "continuent de s'appliquer chaque mois (1 % du principal).";
            default -> "Nous vous invitons à régulariser votre situation dans les meilleurs délais. "
                    + "Si vous ne pouvez pas payer en une fois, vous pouvez demander un échéancier depuis votre espace.";
        };
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
            Declaration d = r.getDette().getDeclaration();
            String impot = d.getTypeImpots() != null ? d.getTypeImpots().getCode() : "";
            lignes.append("<tr>")
                    .append(td(esc(impot) + "<br><span style=\"color:#64748b;font-size:12px\">"
                            + esc(LibellesFiscaux.periode(d.getPeriode(), d.getAnnee())) + "</span>", "left"))
                    .append(td(LibellesFiscaux.date(d.getDateLimitePaiement()), "left"))
                    .append(td(r.getJoursRetard() + " j", "right"))
                    .append(td("<strong>" + LibellesFiscaux.montant(r.getMontantDu()) + "</strong>", "right"))
                    .append("</tr>");
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
                      <tr><td colspan="3" style="padding:12px 10px;font-weight:bold">Total à régler</td>
                          <td align="right" style="padding:12px 10px;font-weight:bold;font-size:16px;color:%s">%s</td></tr>
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
                lignes, couleur, LibellesFiscaux.montant(total), esc(lienPlateforme));
    }

    private static String td(String contenu, String alignement) {
        return "<td align=\"" + alignement + "\" style=\"padding:10px;border-bottom:1px solid #e6eaf0\">" + contenu + "</td>";
    }

    private static String esc(String texte) {
        return texte != null ? HtmlUtils.htmlEscape(texte, "UTF-8") : "";
    }
}
