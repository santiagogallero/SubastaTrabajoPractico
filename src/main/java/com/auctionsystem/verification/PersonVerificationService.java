package com.auctionsystem.verification;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PersonVerificationService {

    private final String provider;
    private final String renaperBaseUrl;
    private final String renaperToken;
    private final boolean strictNameMatch;

    public PersonVerificationService(
            @Value("${app.verification.provider:mock}") String provider,
            @Value("${app.verification.renaper.base-url:}") String renaperBaseUrl,
            @Value("${app.verification.renaper.token:}") String renaperToken,
            @Value("${app.verification.strict-name-match:true}") boolean strictNameMatch
    ) {
        this.provider = provider.toLowerCase(Locale.ROOT);
        this.renaperBaseUrl = renaperBaseUrl;
        this.renaperToken = renaperToken;
        this.strictNameMatch = strictNameMatch;
    }

    public VerificationResult verify(String documento, String numeroTramite, String nombre, String paisOrigen) {
        return switch (provider) {
            case "manual" -> new VerificationResult(
                    true,
                    "MANUAL",
                    "Verificacion manual pendiente por operador",
                    nombre,
                    paisOrigen
            );
            case "renaper" -> verifyAgainstRenaper(documento, numeroTramite, nombre, paisOrigen);
            default -> verifyMock(documento, numeroTramite, nombre, paisOrigen);
        };
    }

    private VerificationResult verifyMock(String documento, String numeroTramite, String nombre, String paisOrigen) {
        if (!"ARGENTINA".equalsIgnoreCase(paisOrigen) && !"AR".equalsIgnoreCase(paisOrigen)) {
            return new VerificationResult(false, "MOCK_AR", "La validacion de numero de tramite mock aplica solo AR", nombre, paisOrigen);
        }
        if (numeroTramite == null || numeroTramite.length() < 8) {
            return new VerificationResult(false, "MOCK_AR", "Numero de tramite invalido", nombre, paisOrigen);
        }
        return new VerificationResult(true, "MOCK_AR", "Validacion mock aprobada", nombre, "ARGENTINA");
    }

    private VerificationResult verifyAgainstRenaper(String documento, String numeroTramite, String nombre, String paisOrigen) {
        if (renaperBaseUrl == null || renaperBaseUrl.isBlank() || renaperToken == null || renaperToken.isBlank()) {
            return new VerificationResult(false, "RENAPER", "Configuracion RENAPER incompleta", nombre, paisOrigen);
        }

        RenaperResponse response;
        try {
            response = RestClient.builder().build().post()
                    .uri(renaperBaseUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + renaperToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RenaperRequest(documento, numeroTramite))
                    .retrieve()
                    .body(RenaperResponse.class);
        } catch (Exception ex) {
            return new VerificationResult(false, "RENAPER", "Error consultando RENAPER: " + ex.getMessage(), nombre, paisOrigen);
        }

        if (response == null || !Boolean.TRUE.equals(response.ok())) {
            return new VerificationResult(false, "RENAPER", "RENAPER no pudo validar identidad", nombre, paisOrigen);
        }

        String canonicalName = response.nombreCompleto() != null ? response.nombreCompleto() : nombre;
        boolean nameMatches = !strictNameMatch || normalize(nombre).equals(normalize(canonicalName));
        if (!nameMatches) {
            return new VerificationResult(false, "RENAPER", "Nombre no coincide con registro oficial", canonicalName, "ARGENTINA");
        }

        return new VerificationResult(true, "RENAPER", "Identidad validada por servicio oficial", canonicalName, "ARGENTINA");
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }

    private record RenaperRequest(String documento, String numeroTramite) {
    }

    private record RenaperResponse(Boolean ok, String nombreCompleto) {
    }
}
