package com.auctionsystem.auth;

import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void run(String... args) {
        if (!bootstrapEnabled) {
            return;
        }

        String normalizedEmail = bootstrapEmail.toLowerCase(Locale.ROOT);
        Rol adminRol = rolRepository.findByNombre("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN no encontrado"));

        UsuarioAuth admin = usuarioAuthRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            UsuarioAuth newAdmin = new UsuarioAuth();
            newAdmin.setEmail(normalizedEmail);
            newAdmin.setCreatedAt(LocalDateTime.now());
            return newAdmin;
        });

        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setEstado("ACTIVO");
        admin.setUpdatedAt(LocalDateTime.now());
        if (admin.getRoles().stream().noneMatch(r -> "ADMIN".equals(r.getNombre()))) {
            admin.getRoles().add(adminRol);
        }

        usuarioAuthRepository.save(admin);
    }
}
