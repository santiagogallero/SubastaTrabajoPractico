package com.auctionsystem.integration;

import com.auctionsystem.auth.Rol;
import com.auctionsystem.auth.RolRepository;
import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RolRepository rolRepository;
    @Autowired private UsuarioAuthRepository usuarioAuthRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // Evita que Spring intente conectarse a un servidor SMTP real
    @MockBean private JavaMailSender javaMailSender;

    @BeforeEach
    void seedRoles() {
        for (String nombre : Set.of("ADMIN", "EMPLEADO", "POSTOR", "DUENIO", "SUBASTADOR")) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                Rol rol = new Rol();
                rol.setNombre(nombre);
                rolRepository.save(rol);
            }
        }
    }

    // ── register/stage1 ──────────────────────────────────────────────────────

    @Test
    void registerStage1_retorna200ConBodyValido() throws Exception {
        var body = Map.of(
                "email", "nuevo@test.com",
                "password", "Password123!",
                "documento", "30111222",
                "nombre", "Maria Lopez",
                "domicilioLegal", "Calle Falsa 123",
                "paisOrigen", "ARGENTINA"
        );

        mockMvc.perform(post("/api/auth/register/stage1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("codigo de verificacion")));
    }

    @Test
    void registerStage1_retorna400SiFaltaCampo() throws Exception {
        var body = Map.of(
                "email", "invalido@test.com"
                // faltan password, documento, nombre, domicilioLegal, paisOrigen
        );

        mockMvc.perform(post("/api/auth/register/stage1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerStage1_retorna400SiEmailInvalido() throws Exception {
        var body = Map.of(
                "email", "no-es-un-email",
                "password", "Password123!",
                "documento", "30111333",
                "nombre", "Pedro Gomez",
                "domicilioLegal", "Av. Test 1",
                "paisOrigen", "ARGENTINA"
        );

        mockMvc.perform(post("/api/auth/register/stage1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerStage1_retorna400SiEmailDuplicado() throws Exception {
        var body = Map.of(
                "email", "duplicado@test.com",
                "password", "Password123!",
                "documento", "30111444",
                "nombre", "Ana Garcia",
                "domicilioLegal", "Calle Real 5",
                "paisOrigen", "ARGENTINA"
        );
        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/api/auth/register/stage1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // segundo intento con el mismo email → debe fallar
        var body2 = Map.of(
                "email", "duplicado@test.com",
                "password", "OtroPass!",
                "documento", "99999999",
                "nombre", "Otro Usuario",
                "domicilioLegal", "Otra Dir",
                "paisOrigen", "ARGENTINA"
        );
        mockMvc.perform(post("/api/auth/register/stage1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isBadRequest());
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_retornaTokenConUsuarioActivo() throws Exception {
        crearUsuarioActivo("activo@test.com", "Password123!");

        var body = Map.of("email", "activo@test.com", "password", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.roles", hasItem("POSTOR")));
    }

    @Test
    void login_retorna401ConPasswordIncorrecto() throws Exception {
        crearUsuarioActivo("activo2@test.com", "Password123!");

        var body = Map.of("email", "activo2@test.com", "password", "wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_retorna403UsuarioPendiente() throws Exception {
        // DisabledException → el ApiExceptionHandler lo mapea a 403 Forbidden
        crearUsuarioPendiente("pendiente@test.com", "Password123!");

        var body = Map.of("email", "pendiente@test.com", "password", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Usuario pendiente de aprobacion"));
    }

    // ── /me ─────────────────────────────────────────────────────────────────

    @Test
    void me_retornaDatosConTokenValido() throws Exception {
        crearUsuarioActivo("yo@test.com", "Password123!");

        String token = obtenerToken("yo@test.com", "Password123!");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("yo@test.com"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void me_retorna401SinToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void crearUsuarioActivo(String email, String password) {
        crearUsuario(email, password, "ACTIVO");
    }

    private void crearUsuarioPendiente(String email, String password) {
        crearUsuario(email, password, "PENDIENTE");
    }

    private void crearUsuario(String email, String password, String estado) {
        Rol rolPostor = rolRepository.findByNombre("POSTOR")
                .orElseThrow(() -> new IllegalStateException("Rol POSTOR no encontrado en test"));

        UsuarioAuth usuario = new UsuarioAuth();
        usuario.setEmail(email.toLowerCase());
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setEstado(estado);
        usuario.setCreatedAt(LocalDateTime.now());
        usuario.setUpdatedAt(LocalDateTime.now());
        usuario.getRoles().add(rolPostor);
        usuarioAuthRepository.save(usuario);
    }

    private String obtenerToken(String email, String password) throws Exception {
        var body = Map.of("email", email, "password", password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }
}
