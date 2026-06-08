package com.auctionsystem.auth;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class DevTestDataBootstrap implements CommandLineRunner {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // Caso 1: login exitoso
        crearUsuario("test.ok@prueba.com",        "test1234", "POSTOR", "ACTIVO");
        // Caso 4a: usuario pendiente de aprobación
        crearUsuario("test.pendiente@prueba.com", "test1234", "POSTOR", "PENDIENTE");
        // Caso 4b: usuario en revisión de admin
        crearUsuario("test.revision@prueba.com",  "test1234", "POSTOR", "PENDIENTE_REVISION");
        // Caso 4c: usuario pendiente de verificación de email
        crearUsuario("test.email@prueba.com",     "test1234", "POSTOR", "PENDIENTE_VERIFICACION_EMAIL");
        // Casos 2 y 3 (password incorrecto / email no existe) no requieren usuario especial
    }

    private void crearUsuario(String email, String password, String rolNombre, String estado) {
        if (usuarioAuthRepository.existsByEmail(email)) {
            return;
        }
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("Rol " + rolNombre + " no encontrado"));

        UsuarioAuth usuario = new UsuarioAuth();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setEstado(estado);
        usuario.setCreatedAt(LocalDateTime.now());
        usuario.setUpdatedAt(LocalDateTime.now());
        usuario.getRoles().add(rol);
        usuarioAuthRepository.save(usuario);
        log.info("[DEV] Usuario de prueba creado: {} / {} (estado: {})", email, password, estado);
    }
}
