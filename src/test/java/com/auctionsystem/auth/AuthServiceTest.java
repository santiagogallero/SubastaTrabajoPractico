package com.auctionsystem.auth;

import com.auctionsystem.auth.dto.LoginRequest;
import com.auctionsystem.auth.dto.LoginResponse;
import com.auctionsystem.auth.dto.Stage1RegistrationRequest;
import com.auctionsystem.entities.Persona;
import com.auctionsystem.repositories.ClienteRepository;
import com.auctionsystem.repositories.PersonaRepository;
import com.auctionsystem.services.MailService;
import com.auctionsystem.verification.PersonVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioAuthRepository usuarioAuthRepository;
    @Mock private RolRepository rolRepository;
    @Mock private RegistroPostorRepository registroPostorRepository;
    @Mock private MedioPagoRepository medioPagoRepository;
    @Mock private EmailVerificationCodeRepository emailVerificationCodeRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private MailService mailService;
    @Mock private PersonVerificationService personVerificationService;

    @InjectMocks
    private AuthService authService;

    private Stage1RegistrationRequest requestValido;

    @BeforeEach
    void setUp() {
        requestValido = new Stage1RegistrationRequest(
                "juan@test.com", "12345678",
                "Juan Perez", "Av. Siempre Viva 123", "ARGENTINA"
        );
    }

    @Test
    void registerStage1_guardaUsuarioPendienteYEnviaEmail() {
        Rol rolPostor = new Rol();
        rolPostor.setNombre("POSTOR");

        Persona personaGuardada = Persona.builder()
                .id(1).documento("12345678").nombre("Juan Perez")
                .direccion("Av. Siempre Viva 123").estado("PENDIENTE").build();

        when(usuarioAuthRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(personaRepository.existsByDocumento("12345678")).thenReturn(false);
        when(personaRepository.save(any())).thenReturn(personaGuardada);
        when(rolRepository.findByNombre("POSTOR")).thenReturn(Optional.of(rolPostor));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioAuthRepository.save(any())).thenAnswer(inv -> {
            UsuarioAuth u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(registroPostorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(emailVerificationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.registerStage1(requestValido);

        verify(usuarioAuthRepository).save(argThat(u ->
                "juan@test.com".equals(u.getEmail()) && "PENDIENTE_VERIFICACION_EMAIL".equals(u.getEstado())
        ));
        verify(emailVerificationCodeRepository).save(any());
        verify(mailService).sendPlainText(eq("juan@test.com"), eq("Codigo de verificacion"), contains("Tu codigo de verificacion es:"));
    }

    @Test
    void registerStage1_lanzaExcepcionSiEmailYaExiste() {
        when(usuarioAuthRepository.existsByEmail("juan@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerStage1(requestValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya esta registrado");

        verify(personaRepository, never()).save(any());
        verify(usuarioAuthRepository, never()).save(any());
    }

    @Test
    void registerStage1_lanzaExcepcionSiDocumentoYaExiste() {
        when(usuarioAuthRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(personaRepository.existsByDocumento("12345678")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerStage1(requestValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya esta registrado");

        verify(personaRepository, never()).save(any());
    }

    @Test
    void login_retornaTokenCuandoCredencialesValidas() {
        var userDetails = User.withUsername("juan@test.com")
                .password("hashed")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_POSTOR")))
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt.token.aqui");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);

        LoginResponse response = authService.login(new LoginRequest("juan@test.com", "Password123!"));

        assertThat(response.token()).isEqualTo("jwt.token.aqui");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(7200L);
        assertThat(response.roles()).contains("POSTOR");
    }
}
