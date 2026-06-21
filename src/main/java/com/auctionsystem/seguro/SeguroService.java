package com.auctionsystem.seguro;

import com.auctionsystem.auth.UsuarioAuth;
import com.auctionsystem.auth.UsuarioAuthRepository;
import com.auctionsystem.entities.Producto;
import com.auctionsystem.entities.Seguro;
import com.auctionsystem.repositories.ProductoRepository;
import com.auctionsystem.seguro.dto.SeguroPolizaResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeguroService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<SeguroPolizaResponse> misPolizas(String email) {
        Integer personaId = personaIdDe(email);
        return productoRepository.findByDuenioIdAndSeguroIsNotNullOrderByIdDesc(personaId).stream()
                .map(this::toResponse)
                .toList();
    }

    private SeguroPolizaResponse toResponse(Producto producto) {
        Seguro seguro = producto.getSeguro();
        return new SeguroPolizaResponse(
                producto.getId(),
                producto.getTitulo() != null ? producto.getTitulo() : producto.getDescripcionCatalogo(),
                seguro.getNroPoliza(),
                seguro.getCompania(),
                seguro.getImporte(),
                "si".equalsIgnoreCase(seguro.getPolizaCombinada()),
                seguro.getContactoTelefono(),
                seguro.getContactoEmail()
        );
    }

    private Integer personaIdDe(String email) {
        UsuarioAuth usuario = usuarioAuthRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no vinculado"));
        if (usuario.getPersonaId() == null) {
            throw new IllegalArgumentException("El usuario no tiene una persona asociada");
        }
        return usuario.getPersonaId();
    }
}
