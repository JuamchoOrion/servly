package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.VerificationCode;
import co.edu.uniquindio.servly.model.enums.CodeType;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private final VerificationCodeRepository codeRepository;
    private final PasswordEncoder            passwordEncoder;

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
        log.debug("Código {} generado para: {}", type, email);
        return plainCode;
    }

    @Transactional
    public void verifyCode(String email, String plainCode, CodeType type) {
        VerificationCode stored = codeRepository
                .findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> new AuthException("No hay un código de verificación activo"));

        if (stored.isExpiredOrUsed()) {
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