package com.auctionsystem.auth;

import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevAdminBootstrap implements CommandLineRunner {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap-admin.email:admin@auction.com}")
    private String bootstrapEmail;

    @Value("${app.bootstrap-admin.password:Admin123!}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            return;
        }

        String normalizedEmail = bootstrapEmail.toLowerCase(Locale.ROOT);
        if (usuarioAuthRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        Rol adminRol = rolRepository.findByNombre("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN no encontrado"));

        UsuarioAuth admin = new UsuarioAuth();
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setEstado("ACTIVO");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        admin.getRoles().add(adminRol);

        usuarioAuthRepository.save(admin);
    }
}
