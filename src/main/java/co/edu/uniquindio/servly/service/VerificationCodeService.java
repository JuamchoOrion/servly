package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.AuditLog;
import co.edu.uniquindio.servly.model.entity.VerificationCode;
import co.edu.uniquindio.servly.model.entity.User;
import co.edu.uniquindio.servly.model.enums.CodeType;
import co.edu.uniquindio.servly.repository.UserRepository;
import co.edu.uniquindio.servly.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Gestiona la generación, almacenamiento y validación de códigos OTP.
 *
 * Seguridad:
 *  - Generados con SecureRandom (criptográficamente seguro)
 *  - Almacenados hasheados con BCrypt
 *  - Cada nuevo código invalida los anteriores del mismo tipo
 *  - Un código solo puede usarse una vez
 *
 * Auditoría:
 *  - Registra evento CODE_EXPIRED cuando un código expira antes de usarse
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private final VerificationCodeRepository codeRepository;
    private final PasswordEncoder            passwordEncoder;
    private final UserRepository             userRepository;
    private final AuditService               auditService;

    @Value("${app.two-factor.code-expiration-minutes}")
    private int twoFactorExpiration;

    @Value("${app.password-reset.code-expiration-minutes}")
    private int passwordResetExpiration;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public String generateAndSave(String email, CodeType type) {
        codeRepository.deleteByEmailAndType(email, type);

        String plainCode = generateSixDigitCode();
        int minutes = (type == CodeType.TWO_FACTOR) ? twoFactorExpiration : passwordResetExpiration;

        VerificationCode code = VerificationCode.builder()
                .email(email)
                .code(passwordEncoder.encode(plainCode))
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(minutes))
                .used(false)
                .build();

        codeRepository.save(code);

        // Registrar que se generó un código 2FA
        if (type == CodeType.TWO_FACTOR) {
            authMetricsService.record2FACodeGenerated();
            log.info("📊 2FA Code Generated Metric Recorded for: {}", email);
        }

        log.debug("Código {} generado para: {}", type, email);
        return plainCode;
    }

    @Transactional
    public void verifyCode(String email, String plainCode, CodeType type) {
        VerificationCode stored = codeRepository
                .findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> new AuthException("No hay un código de verificación activo"));

        if (stored.isExpiredOrUsed()) {
            // Registrar evento de código expirado para métricas
            String role = userRepository.findByEmail(email)
                    .map(u -> u.getRole().name())
                    .orElse(null);

            auditService.logEvent(AuditLog.EVENT_CODE_EXPIRED, email, role,
                    false, "Código expirado antes de ser utilizado", null, null);

            log.warn("Código expirado para: {}", email);
            // Registrar que un código expiró (solo para 2FA)
            if (type == CodeType.TWO_FACTOR) {
                authMetricsService.record2FACodeExpired();
                log.info("📊 2FA Code Expired Metric Recorded for: {}", email);
            }
            throw new AuthException("El código ha expirado. Por favor solicita uno nuevo.");
        }

        if (!passwordEncoder.matches(plainCode, stored.getCode())) {
            throw new AuthException("Código de verificación incorrecto");
        }

        stored.setUsed(true);
        codeRepository.save(stored);
        log.debug("Código {} validado exitosamente para: {}", type, email);
    }

    private String generateSixDigitCode() {
        return String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
    }
}