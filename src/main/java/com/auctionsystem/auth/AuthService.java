package com.auctionsystem.auth;

import com.auctionsystem.auth.dto.ForgotPasswordRequest;
import com.auctionsystem.auth.dto.LoginRequest;
import com.auctionsystem.auth.dto.LoginResponse;
import com.auctionsystem.auth.dto.CurrentUserResponse;
import com.auctionsystem.auth.dto.MedioPagoResponse;
import com.auctionsystem.auth.dto.PaymentMethodRequest;
import com.auctionsystem.auth.dto.RegisterPaymentMethodsRequest;
import com.auctionsystem.auth.dto.ResetPasswordRequest;
import com.auctionsystem.auth.dto.SendEmailVerificationCodeRequest;
import com.auctionsystem.auth.dto.Stage1RegistrationRequest;
import com.auctionsystem.auth.dto.Stage2RegistrationRequest;
import com.auctionsystem.auth.dto.VerifyEmailCodeRequest;
import com.auctionsystem.entities.Cliente;
import com.auctionsystem.entities.Empleado;
import com.auctionsystem.entities.Persona;
import com.auctionsystem.repositories.ClienteRepository;
import com.auctionsystem.repositories.EmpleadoRepository;
import com.auctionsystem.repositories.PersonaRepository;
import com.auctionsystem.services.MailService;
import com.auctionsystem.verification.PersonVerificationService;
import com.auctionsystem.verification.VerificationResult;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
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

        private static final int EMAIL_CODE_EXPIRATION_MINUTES = 10;
        private static final int EMAIL_CODE_MAX_ATTEMPTS = 5;
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();
        private static final String TYPE_EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
        private static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final RolRepository rolRepository;
    private final RegistroPostorRepository registroPostorRepository;
    private final MedioPagoRepository medioPagoRepository;
        private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final PersonaRepository personaRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
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

        Persona persona = Persona.builder()
                .documento(request.documento())
                                .nombre(request.nombre())
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
        registro.setDomicilioLegal(request.domicilioLegal());
        registro.setPaisOrigen(request.paisOrigen());
        registroPostorRepository.save(registro);

        mailService.sendPlainText(
                usuario.getEmail(),
                "Registro etapa 1 completado",
                "Tu registro etapa 1 fue recibido. Completa la etapa 2 cargando documentacion e identidad."
        );
    }

    @Transactional
    public void completeStage2(String email, Stage2RegistrationRequest request) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        RegistroPostor registro = registroPostorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("No existe etapa 1 para este usuario"));

        Persona persona = personaRepository.findById(usuario.getPersonaId())
                .orElseThrow(() -> new IllegalArgumentException("Persona asociada no encontrada"));

        VerificationResult verification = personVerificationService.verify(
                persona.getDocumento(),
                request.numeroTramite(),
                persona.getNombre(),
                registro.getPaisOrigen()
        );
        if (!verification.approved()) {
                throw new IllegalArgumentException("No se pudo validar identidad con numero de tramite: " + verification.detail());
        }

        registro.setDocFrenteUrl(request.docFrenteUrl());
        registro.setDocDorsoUrl(request.docDorsoUrl());
        registro.setNumeroTramite(request.numeroTramite());
        registro.setEtapa("ETAPA_2_COMPLETA");
        registro.setVerificacionExternaEstado("APROBADA");
        registro.setVerificacionExternaFuente(verification.source());
        registro.setVerificacionExternaDetalle(verification.detail());
        registro.setVerificadoPor("SISTEMA_" + verification.source());
        registro.setVerificadoAt(LocalDateTime.now());
                registro.setMotivoRechazo(null);
        registroPostorRepository.save(registro);

                usuario.setEstado("PENDIENTE_VERIFICACION_EMAIL");
        usuario.setUpdatedAt(LocalDateTime.now());
        usuarioAuthRepository.save(usuario);

                sendEmailVerificationCodeInternal(usuario);

        mailService.sendPlainText(
                usuario.getEmail(),
                                "Registro etapa 2 completado",
                                "Te enviamos un codigo a tu correo para confirmar email. Una vez validado, tu cuenta quedara en revision de la empresa."
        );
    }

        @Transactional
        public void sendEmailVerificationCode(SendEmailVerificationCodeRequest request) {
                UsuarioAuth usuario = usuarioAuthRepository.findByEmail(request.email().toLowerCase(Locale.ROOT))
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                if (!"PENDIENTE_VERIFICACION_EMAIL".equalsIgnoreCase(usuario.getEstado())) {
                        throw new IllegalArgumentException("El usuario no requiere verificacion de correo en este estado");
                }

                sendEmailVerificationCodeInternal(usuario);
        }

        @Transactional
        public void verifyEmailCode(VerifyEmailCodeRequest request) {
                String normalizedEmail = request.email().toLowerCase(Locale.ROOT);
                UsuarioAuth usuario = usuarioAuthRepository.findByEmail(normalizedEmail)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                if (!"PENDIENTE_VERIFICACION_EMAIL".equalsIgnoreCase(usuario.getEstado())) {
                        throw new IllegalArgumentException("El usuario no se encuentra pendiente de verificacion de correo");
                }

                EmailVerificationCode verificationCode = emailVerificationCodeRepository
                                .findTopByEmailAndTypeOrderByCreatedAtDesc(normalizedEmail, TYPE_EMAIL_VERIFICATION)
                                .orElseThrow(() -> new IllegalArgumentException("No existe codigo activo para este usuario"));

                validateCode(verificationCode, request.code());

                RegistroPostor registro = registroPostorRepository.findByUsuarioId(usuario.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Registro de postor no encontrado"));
                registro.setEtapa("PENDIENTE_REVISION_ADMIN");
                registroPostorRepository.save(registro);

                usuario.setEstado("PENDIENTE_REVISION");
                usuario.setUpdatedAt(LocalDateTime.now());
                usuarioAuthRepository.save(usuario);

                mailService.sendPlainText(
                                usuario.getEmail(),
                                "Correo verificado",
                                "Correo validado correctamente. Ahora estamos verificando tus datos para aprobar tu cuenta."
                );
        }

        @Transactional
        public void forgotPassword(ForgotPasswordRequest request) {
                String normalizedEmail = request.email().toLowerCase(Locale.ROOT);
                // Si no existe el usuario, respondemos igual para no revelar si el email está registrado
                if (!usuarioAuthRepository.existsByEmail(normalizedEmail)) {
                        return;
                }

                for (EmailVerificationCode previous : emailVerificationCodeRepository
                                .findByEmailAndTypeAndUsedFalse(normalizedEmail, TYPE_PASSWORD_RESET)) {
                        previous.setUsed(true);
                        emailVerificationCodeRepository.save(previous);
                }

                String generatedCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
                EmailVerificationCode resetCode = new EmailVerificationCode();
                resetCode.setEmail(normalizedEmail);
                resetCode.setType(TYPE_PASSWORD_RESET);
                resetCode.setCode(generatedCode);
                resetCode.setCreatedAt(LocalDateTime.now());
                resetCode.setExpiresAt(LocalDateTime.now().plusMinutes(EMAIL_CODE_EXPIRATION_MINUTES));
                resetCode.setAttempts(0);
                resetCode.setUsed(false);
                emailVerificationCodeRepository.save(resetCode);

                mailService.sendPlainText(
                                normalizedEmail,
                                "Restablecer contrasena",
                                "Tu codigo para restablecer la contrasena es: " + generatedCode
                                + ". Vence en " + EMAIL_CODE_EXPIRATION_MINUTES + " minutos."
                );
        }

        @Transactional
        public void resetPassword(ResetPasswordRequest request) {
                String normalizedEmail = request.email().toLowerCase(Locale.ROOT);
                UsuarioAuth usuario = usuarioAuthRepository.findByEmail(normalizedEmail)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                EmailVerificationCode resetCode = emailVerificationCodeRepository
                                .findTopByEmailAndTypeOrderByCreatedAtDesc(normalizedEmail, TYPE_PASSWORD_RESET)
                                .orElseThrow(() -> new IllegalArgumentException("No existe un codigo de restablecimiento activo"));

                validateCode(resetCode, request.code());

                usuario.setPasswordHash(passwordEncoder.encode(request.newPassword()));
                usuario.setUpdatedAt(LocalDateTime.now());
                usuarioAuthRepository.save(usuario);

                mailService.sendPlainText(
                                normalizedEmail,
                                "Contrasena actualizada",
                                "Tu contrasena fue restablecida correctamente."
                );
        }

        private void validateCode(EmailVerificationCode verificationCode, String inputCode) {
                if (verificationCode.isUsed()) {
                        throw new IllegalArgumentException("El codigo ya fue utilizado");
                }

                if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
                        verificationCode.setUsed(true);
                        emailVerificationCodeRepository.save(verificationCode);
                        throw new IllegalArgumentException("El codigo expiro. Solicita uno nuevo");
                }

                if (!verificationCode.getCode().equals(inputCode)) {
                        int attempts = verificationCode.getAttempts() + 1;
                        verificationCode.setAttempts(attempts);
                        if (attempts >= EMAIL_CODE_MAX_ATTEMPTS) {
                                verificationCode.setUsed(true);
                        }
                        emailVerificationCodeRepository.save(verificationCode);
                        throw new IllegalArgumentException("Codigo invalido");
                }

                verificationCode.setUsed(true);
                emailVerificationCodeRepository.save(verificationCode);
        }

        @Transactional
        public void registerPaymentMethods(String email, RegisterPaymentMethodsRequest request) {
                UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                if (!"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
                                throw new IllegalArgumentException("Solo usuarios aprobados pueden registrar medios de pago");
                }

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
        }

        private void sendEmailVerificationCodeInternal(UsuarioAuth usuario) {
                String normalizedEmail = usuario.getEmail().toLowerCase(Locale.ROOT);
                for (EmailVerificationCode previousCode : emailVerificationCodeRepository
                                .findByEmailAndTypeAndUsedFalse(normalizedEmail, TYPE_EMAIL_VERIFICATION)) {
                        previousCode.setUsed(true);
                        emailVerificationCodeRepository.save(previousCode);
                }

                String generatedCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
                EmailVerificationCode verificationCode = new EmailVerificationCode();
                verificationCode.setEmail(normalizedEmail);
                verificationCode.setType(TYPE_EMAIL_VERIFICATION);
                verificationCode.setCode(generatedCode);
                verificationCode.setCreatedAt(LocalDateTime.now());
                verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(EMAIL_CODE_EXPIRATION_MINUTES));
                verificationCode.setAttempts(0);
                verificationCode.setUsed(false);
                emailVerificationCodeRepository.save(verificationCode);

                mailService.sendPlainText(
                                normalizedEmail,
                                "Codigo de verificacion",
                                "Tu codigo de verificacion es: " + generatedCode + ". Vence en " + EMAIL_CODE_EXPIRATION_MINUTES + " minutos."
                );
        }

        @Transactional
        public void aprobarUsuario(String email, String categoria) {
                UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                RegistroPostor registro = registroPostorRepository.findByUsuarioId(usuario.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Registro de postor no encontrado"));

                String categoriaNormalizada = categoria.toUpperCase(Locale.ROOT);
                registro.setCategoria(categoriaNormalizada);
                registro.setEtapa("APROBADO");
                registro.setVerificadoAt(LocalDateTime.now());
                registroPostorRepository.save(registro);

                usuario.setEstado("ACTIVO");
                usuario.setUpdatedAt(LocalDateTime.now());
                usuarioAuthRepository.save(usuario);

                // El circuito de pujas valida la categoria sobre Cliente, no sobre RegistroPostor.
                // Al aprobar, creamos/actualizamos el Cliente para que el postor pueda participar.
                sincronizarClientePostor(usuario, categoriaNormalizada);

                mailService.sendPlainText(
                                usuario.getEmail(),
                                "Cuenta aprobada",
                                "Tu cuenta fue verificada y aprobada. Ya podes participar segun tu categoria."
                );
        }

        private void sincronizarClientePostor(UsuarioAuth usuario, String categoria) {
                if (usuario.getPersonaId() == null) {
                        return;
                }

                Persona persona = personaRepository.findById(usuario.getPersonaId())
                                .orElseThrow(() -> new IllegalStateException("Persona del usuario no encontrada"));

                Empleado verificador = empleadoRepository.findAll().stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                        "No existe ningun empleado en el sistema para asignar como verificador"));

                Cliente cliente = clienteRepository.findByPersonaId(persona.getId())
                                .orElseGet(() -> Cliente.builder().persona(persona).build());
                cliente.setPersona(persona);
                cliente.setCategoria(categoria);
                cliente.setAdmitido("si");
                cliente.setVerificador(verificador);
                clienteRepository.save(cliente);
        }

        @Transactional(readOnly = true)
        public List<MedioPagoResponse> listarMediosPago(String email) {
                UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                return medioPagoRepository.findByUsuarioId(usuario.getId()).stream()
                                .map(m -> new MedioPagoResponse(
                                                m.getId(),
                                                m.getTipo(),
                                                m.getAliasDescripcion(),
                                                m.getMoneda(),
                                                m.getMontoGarantia(),
                                                Boolean.TRUE.equals(m.getVerificado()),
                                                Boolean.TRUE.equals(m.getActivo())
                                ))
                                .toList();
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

        @Transactional(readOnly = true)
        public CurrentUserResponse currentUser(String email) {
                UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                Set<String> roles = usuario.getRoles().stream()
                                .map(Rol::getNombre)
                                .collect(Collectors.toSet());

                String categoria = null;
                if (usuario.getPersonaId() != null) {
                        categoria = clienteRepository.findByPersonaId(usuario.getPersonaId())
                                        .map(Cliente::getCategoria)
                                        .orElse(null);
                }

                return new CurrentUserResponse(
                                usuario.getId(),
                                usuario.getEmail(),
                                usuario.getEstado(),
                                usuario.getPersonaId(),
                                roles,
                                categoria
                );
        }
}
