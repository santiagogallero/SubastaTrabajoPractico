package com.auctionsystem.payout;

import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.entities.Duenio;
import com.auctionsystem.entities.Persona;
import com.auctionsystem.payout.dto.CuentaCobroRequest;
import com.auctionsystem.payout.dto.CuentaCobroResponse;
import com.auctionsystem.repositories.DuenioRepository;
import com.auctionsystem.repositories.PersonaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private final CuentaCobroRepository cuentaCobroRepository;
    private final DuenioRepository duenioRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;

    @Transactional(readOnly = true)
    public List<CuentaCobroResponse> listar(String email) {
        Duenio duenio = duenioDe(email);
        if (duenio == null) {
            return List.of();
        }
        return cuentaCobroRepository.findByDuenioIdAndActivoTrueOrderByCreatedAtDesc(duenio.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CuentaCobroResponse crear(String email, CuentaCobroRequest request) {
        Duenio duenio = obtenerOCrearDuenio(email);
        validarRequest(request);

        CuentaCobro cuenta = new CuentaCobro();
        cuenta.setDuenio(duenio);
        cuenta.setAlias(request.alias().trim());
        cuenta.setMoneda(request.currency().trim().toUpperCase(Locale.ROOT));
        cuenta.setExtranjera(Boolean.TRUE.equals(request.foreignAccount()));
        cuenta.setBanco(request.bankName().trim());
        cuenta.setNumeroCuenta(request.accountNumber().trim());
        cuenta.setSwiftCode(blankToNull(request.swiftCode()));
        cuenta.setTitular(request.holderName().trim());
        cuenta.setActivo(true);
        cuenta.setCreatedAt(LocalDateTime.now());
        return toDto(cuentaCobroRepository.save(cuenta));
    }

    @Transactional
    public CuentaCobroResponse actualizar(String email, Long id, CuentaCobroRequest request) {
        Duenio duenio = duenioDe(email);
        if (duenio == null) {
            throw new IllegalArgumentException("No existe perfil de vendedor para este usuario");
        }
        CuentaCobro cuenta = cuentaCobroRepository.findByIdAndDuenioId(id, duenio.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta de cobro no encontrada"));
        validarRequest(request);

        cuenta.setAlias(request.alias().trim());
        cuenta.setMoneda(request.currency().trim().toUpperCase(Locale.ROOT));
        cuenta.setExtranjera(Boolean.TRUE.equals(request.foreignAccount()));
        cuenta.setBanco(request.bankName().trim());
        cuenta.setNumeroCuenta(request.accountNumber().trim());
        cuenta.setSwiftCode(blankToNull(request.swiftCode()));
        cuenta.setTitular(request.holderName().trim());
        return toDto(cuentaCobroRepository.save(cuenta));
    }

    @Transactional
    public void eliminar(String email, Long id) {
        Duenio duenio = duenioDe(email);
        if (duenio == null) {
            throw new IllegalArgumentException("No existe perfil de vendedor para este usuario");
        }
        CuentaCobro cuenta = cuentaCobroRepository.findByIdAndDuenioId(id, duenio.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta de cobro no encontrada"));
        cuenta.setActivo(false);
        cuentaCobroRepository.save(cuenta);
    }

    private Duenio duenioDe(String email) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT)).orElse(null);
        if (usuario == null || usuario.getPersonaId() == null) {
            return null;
        }
        return duenioRepository.findById(usuario.getPersonaId()).orElse(null);
    }

    private Duenio obtenerOCrearDuenio(String email) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (usuario.getPersonaId() == null) {
            throw new IllegalArgumentException("El usuario no tiene persona asociada");
        }
        Persona persona = personaRepository.findById(usuario.getPersonaId())
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        return duenioRepository.findById(persona.getId())
                .orElseGet(() -> duenioRepository.save(Duenio.builder().persona(persona).build()));
    }

    private void validarRequest(CuentaCobroRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Datos de cuenta invalidos");
        }
        if (Boolean.TRUE.equals(request.foreignAccount())
                && (request.swiftCode() == null || request.swiftCode().isBlank())) {
            throw new IllegalArgumentException("swiftCode es obligatorio para cuentas del exterior");
        }
    }

    private CuentaCobroResponse toDto(CuentaCobro cuenta) {
        return new CuentaCobroResponse(
                cuenta.getId(),
                cuenta.getAlias(),
                cuenta.getMoneda(),
                Boolean.TRUE.equals(cuenta.getExtranjera()),
                cuenta.getBanco(),
                enmascarar(cuenta.getNumeroCuenta()),
                cuenta.getSwiftCode(),
                cuenta.getTitular(),
                Boolean.TRUE.equals(cuenta.getActivo())
        );
    }

    static String enmascarar(String numero) {
        if (numero == null || numero.isBlank()) {
            return "****";
        }
        String limpio = numero.replaceAll("\\s", "");
        if (limpio.length() <= 4) {
            return "****" + limpio;
        }
        return "****" + limpio.substring(limpio.length() - 4);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
