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
        crearUsuario("postor@test.com", "test1234", "POSTOR");
        crearUsuario("empleado@test.com", "test1234", "EMPLEADO");
    }

    private void crearUsuario(String email, String password, String rolNombre) {
        if (usuarioAuthRepository.existsByEmail(email)) {
            return;
        }
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("Rol " + rolNombre + " no encontrado"));

        UsuarioAuth usuario = new UsuarioAuth();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setEstado("ACTIVO");
        usuario.setCreatedAt(LocalDateTime.now());
        usuario.setUpdatedAt(LocalDateTime.now());
        usuario.getRoles().add(rol);
        usuarioAuthRepository.save(usuario);
        log.info("[DEV] Usuario de prueba creado: {} / {}", email, password);
    }
}
