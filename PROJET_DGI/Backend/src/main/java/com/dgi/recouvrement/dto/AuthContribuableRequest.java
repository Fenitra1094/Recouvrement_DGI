package mg.ssif.recouvrement.dto;

import lombok.Data;

@Data
public class AuthContribuableRequest {
    private String identifiant; // Peut être le NIF (ex: 3000510971) ou l'email
    private String motDePasse;
}