package co.edu.uniquindio.servly.config;

import co.edu.uniquindio.servly.repository.RevokedTokenRepository;
import co.edu.uniquindio.servly.repository.TableSessionRepository;
import co.edu.uniquindio.servly.repository.VerificationCodeRepository;
import co.edu.uniquindio.servly.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final VerificationCodeRepository codeRepository;
    private final TableSessionRepository     tableSessionRepository;
    private final RevokedTokenRepository     revokedTokenRepository;
    private final AuditService               auditService;

    /** Limpia códigos OTP expirados o usados — cada hora. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpiredCodes() {
        codeRepository.deleteExpiredAndUsed(LocalDateTime.now());
        log.debug("Limpieza de códigos OTP ejecutada");
    }

    /** Cierra sesiones de mesa cuyo tiempo expiró — cada 15 minutos. */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void closeExpiredTableSessions() {
        tableSessionRepository.closeExpiredSessions(LocalDateTime.now());
        log.debug("Cierre de sesiones de mesa expiradas ejecutado");
    }

    /** Limpia tokens revocados expirados de la blacklist — diariamente a las 3 AM. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredRevokedTokens() {
        revokedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.debug("Limpieza de tokens revocados expirados ejecutada");
    }

    /** Limpia logs de auditoría antiguos (más de 90 días) — semanalmente los domingos a las 4 AM. */
    @Scheduled(cron = "0 0 4 * * SUN")
    @Transactional
    public void cleanOldAuditLogs() {
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        long deletedCount = auditService.deleteLogsOlderThan(ninetyDaysAgo);
        log.info("Limpieza de logs de auditoría ejecutada. Se eliminaron {} registros antiguos", deletedCount);
    }
}