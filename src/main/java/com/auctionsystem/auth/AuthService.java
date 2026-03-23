package com.auctionsystem.auth;

import com.auctionsystem.auth.dto.LoginRequest;
import com.auctionsystem.auth.dto.LoginResponse;
import com.auctionsystem.auth.dto.PaymentMethodRequest;
import com.auctionsystem.auth.dto.Stage1RegistrationRequest;
import com.auctionsystem.auth.dto.Stage2RegistrationRequest;
import com.auctionsystem.entities.Persona;
import com.auctionsystem.repositories.PersonaRepository;
import com.auctionsystem.services.MailService;
import com.auctionsystem.verification.PersonVerificationService;
import com.auctionsystem.verification.VerificationResult;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final RolRepository rolRepository;
    private final RegistroPostorRepository registroPostorRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MailService mailService;
        private final PersonVerificationService personVerificationService;

    @Transactional
    public void registerStage1(Stage1RegistrationRequest request) {
        if (usuarioAuthRepository.existsByEmail(request.email().toLowerCase(Locale.ROOT))) {
                        throw new IllegalArgumentException("Este usuario ya esta registrado");
                }

                if (personaRepository.existsByDocumento(request.documento())) {
                        throw new IllegalArgumentException("Este usuario ya esta registrado");
        }

                VerificationResult verification = personVerificationService.verify(
                                request.documento(),
                                request.numeroTramite(),
                                request.nombre(),
                                request.paisOrigen()
                );
                if (!verification.approved()) {
                        throw new IllegalArgumentException("No se pudo validar identidad con numero de tramite: " + verification.detail());
                }

        Persona persona = Persona.builder()
                .documento(request.documento())
                                .nombre(verification.canonicalName() != null ? verification.canonicalName() : request.nombre())
                .direccion(request.domicilioLegal())
                .estado("PENDIENTE")
                .foto(null)
                .build();
        persona = personaRepository.save(persona);

        UsuarioAuth usuario = new UsuarioAuth();
        usuario.setEmail(request.email().toLowerCase(Locale.ROOT));
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setEstado("PENDIENTE");
        usuario.setPersonaId(persona.getId());
        usuario.setCreatedAt(LocalDateTime.now());
        usuario.setUpdatedAt(LocalDateTime.now());
        usuario.getRoles().add(rolRepository.findByNombre("POSTOR")
                .orElseThrow(() -> new IllegalStateException("Rol POSTOR no encontrado")));
        usuario = usuarioAuthRepository.save(usuario);

        RegistroPostor registro = new RegistroPostor();
        registro.setUsuario(usuario);
        registro.setEtapa("ETAPA_1_COMPLETA");
        registro.setCategoria("COMUN");
        registro.setDocFrenteUrl(request.docFrenteUrl());
        registro.setDocDorsoUrl(request.docDorsoUrl());
        registro.setDomicilioLegal(request.domicilioLegal());
        registro.setPaisOrigen(verification.canonicalCountry() != null ? verification.canonicalCountry() : request.paisOrigen());
        registro.setNumeroTramite(request.numeroTramite());
        registro.setVerificacionExternaEstado("APROBADA");
        registro.setVerificacionExternaFuente(verification.source());
        registro.setVerificacionExternaDetalle(verification.detail());
        registro.setVerificadoPor("SISTEMA_" + verification.source());
        registro.setVerificadoAt(LocalDateTime.now());
        registroPostorRepository.save(registro);

        mailService.sendPlainText(
                usuario.getEmail(),
                "Registro etapa 1 completado",
                "Tu registro etapa 1 fue recibido. Completa la etapa 2 cargando un medio de pago."
        );
    }

    @Transactional
    public void completeStage2(String email, Stage2RegistrationRequest request) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        RegistroPostor registro = registroPostorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("No existe etapa 1 para este usuario"));

        for (PaymentMethodRequest medio : request.mediosPago()) {
            MedioPago medioPago = new MedioPago();
            medioPago.setUsuario(usuario);
            medioPago.setTipo(medio.tipo().toUpperCase(Locale.ROOT));
            medioPago.setAliasDescripcion(medio.aliasDescripcion());
            medioPago.setMoneda(medio.moneda().toUpperCase(Locale.ROOT));
            medioPago.setMontoGarantia(medio.montoGarantia());
                        medioPago.setVerificado(false);
            medioPago.setActivo(true);
            medioPagoRepository.save(medioPago);
        }

                if (medioPagoRepository.findByUsuarioId(usuario.getId()).isEmpty()) {
                        throw new IllegalArgumentException("Debe registrar al menos un medio de pago");
        }

        registro.setEtapa("ETAPA_2_COMPLETA");
        registro.setCategoria(request.categoria().toUpperCase(Locale.ROOT));
        registroPostorRepository.save(registro);

                usuario.setEstado("PENDIENTE_REVISION");
        usuario.setUpdatedAt(LocalDateTime.now());
        usuarioAuthRepository.save(usuario);

        mailService.sendPlainText(
                usuario.getEmail(),
                                "Registro recibido",
                                "Tu registro etapa 2 fue recibido. La casa de subastas debe verificar datos y medios de pago."
        );
    }

        @Transactional
        public void aprobarUsuario(String email, String categoria) {
                UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                RegistroPostor registro = registroPostorRepository.findByUsuarioId(usuario.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Registro de postor no encontrado"));

                registro.setCategoria(categoria.toUpperCase(Locale.ROOT));
                registro.setEtapa("APROBADO");
                registro.setVerificadoAt(LocalDateTime.now());
                registroPostorRepository.save(registro);

                usuario.setEstado("ACTIVO");
                usuario.setUpdatedAt(LocalDateTime.now());
                usuarioAuthRepository.save(usuario);

                mailService.sendPlainText(
                                usuario.getEmail(),
                                "Cuenta aprobada",
                                "Tu cuenta fue verificada y aprobada. Ya podes participar segun tu categoria."
                );
        }

        @Transactional
        public void verificarMedioPago(Long medioPagoId, boolean verificado) {
                MedioPago medioPago = medioPagoRepository.findById(medioPagoId)
                                .orElseThrow(() -> new IllegalArgumentException("Medio de pago no encontrado"));
                medioPago.setVerificado(verificado);
                medioPagoRepository.save(medioPago);
        }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());

        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), roles);
    }
}
