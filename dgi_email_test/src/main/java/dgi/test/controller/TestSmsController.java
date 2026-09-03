package dgi.test.controller;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestSmsController {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromNumber;

    @GetMapping("/api/test-sms")
    public String testerSms(@RequestParam String telephone) {
        try {
            Twilio.init(accountSid, authToken);

            String texteSms = "DGI MADAGASCAR : TEST RELANCE. Votre NIF 123456789 présente un impayé. Merci de régulariser.";

            Message message = Message.creator(
                    new PhoneNumber(telephone),
                    new PhoneNumber(fromNumber),
                    texteSms
            ).create();

            return "✅ SMS envoyé avec succès ! SID Twilio : " + message.getSid();
        } catch (Exception e) {
            return "❌ Échec de l'envoi du SMS : " + e.getMessage();
        }
    }
}