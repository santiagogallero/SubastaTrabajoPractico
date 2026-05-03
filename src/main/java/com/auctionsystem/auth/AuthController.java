package com.auctionsystem.auth;

import com.auctionsystem.auth.dto.LoginRequest;
import com.auctionsystem.auth.dto.LoginResponse;
import com.auctionsystem.auth.dto.CurrentUserResponse;
import com.auctionsystem.auth.dto.AdminApprovalRequest;
import com.auctionsystem.auth.dto.PaymentVerificationRequest;
import com.auctionsystem.auth.dto.RegisterPaymentMethodsRequest;
import com.auctionsystem.auth.dto.SendEmailVerificationCodeRequest;
import com.auctionsystem.auth.dto.Stage1RegistrationRequest;
import com.auctionsystem.auth.dto.Stage2RegistrationRequest;
import com.auctionsystem.auth.dto.VerifyEmailCodeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/stage1")
    public ResponseEntity<String> registerStage1(@Valid @RequestBody Stage1RegistrationRequest request) {
        authService.registerStage1(request);
        return ResponseEntity.ok("Registro etapa 1 completado");
    }

    @PostMapping("/register/stage2")
    public ResponseEntity<String> registerStage2(@Valid @RequestBody Stage2Payload payload) {
        authService.completeStage2(payload.email(), payload.request());
        return ResponseEntity.ok("Registro etapa 2 completado");
    }

    @PostMapping("/email/send-code")
    public ResponseEntity<String> sendEmailCode(@Valid @RequestBody SendEmailVerificationCodeRequest request) {
        authService.sendEmailVerificationCode(request);
        return ResponseEntity.ok("Codigo enviado");
    }

    @PostMapping("/email/verify-code")
    public ResponseEntity<String> verifyEmailCode(@Valid @RequestBody VerifyEmailCodeRequest request) {
        authService.verifyEmailCode(request);
        return ResponseEntity.ok("Correo verificado. Tu cuenta queda en revision.");
    }

    @PostMapping("/payment-methods")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> registerPaymentMethods(
            @Valid @RequestBody RegisterPaymentMethodsRequest request,
            Principal principal
    ) {
        authService.registerPaymentMethods(principal.getName(), request);
        return ResponseEntity.ok("Medios de pago registrados");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> me(Principal principal) {
        return ResponseEntity.ok(authService.currentUser(principal.getName()));
    }

    @PostMapping("/admin/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<String> aprobarUsuario(@Valid @RequestBody AdminApprovalRequest request) {
        authService.aprobarUsuario(request.email(), request.categoria());
        return ResponseEntity.ok("Usuario aprobado");
    }

    @PostMapping("/admin/verificar-medio-pago")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<String> verificarMedioPago(@Valid @RequestBody PaymentVerificationRequest request) {
        authService.verificarMedioPago(request.medioPagoId(), request.verificado());
        return ResponseEntity.ok("Medio de pago actualizado");
    }

    public record Stage2Payload(
            @NotBlank @Email String email,
            @NotNull @Valid Stage2RegistrationRequest request
    ) {
    }
}
