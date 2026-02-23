package co.edu.uniquindio.servly.config;

import co.edu.uniquindio.servly.repository.TableSessionRepository;
import co.edu.uniquindio.servly.repository.VerificationCodeRepository;
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
}