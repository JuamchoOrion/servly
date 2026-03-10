package co.edu.uniquindio.servly.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para registrar métricas de autenticación.
 * Registra intentos de login, recuperación de contraseñas, 2FA, y sesiones.
 *
 * Umbrales ISO/IEC 25010:
 * - auth.login.duration: < 2000 ms (p95)
 * - auth.login.success: > 95%
 * - auth.password.recovery.duration: < 5 min
 * - auth.2fa.verification.duration: < 60 sec
 * - auth.2fa.codes.expired/generated rate: < 10%
 * - auth.session.duration: ≈ shift duration
 *
 * @author Servly Backend Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class AuthMetricsService {

    private final MeterRegistry meterRegistry;
    private final Timer authLoginDurationTimer;
    private final Counter authLoginSuccessCounter;
    private final Counter authLoginTotalCounter;
    private final Counter authLoginFailureCounter;
    private final Timer authPasswordRecoveryDurationTimer;
    private final Timer auth2FAVerificationDurationTimer;
    private final Counter auth2FACodesGeneratedCounter;
    private final Counter auth2FACodesExpiredCounter;
    private final Timer authSessionDurationTimer;

    /**
     * Constructor con inyección de dependencias.
     */
    public AuthMetricsService(
        MeterRegistry meterRegistry,
        Timer authLoginDurationTimer,
        Counter authLoginSuccessCounter,
        Counter authLoginTotalCounter,
        Counter authLoginFailureCounter,
        Timer authPasswordRecoveryDurationTimer,
        Timer auth2FAVerificationDurationTimer,
        Counter auth2FACodesGeneratedCounter,
        Counter auth2FACodesExpiredCounter,
        Timer authSessionDurationTimer
    ) {
        this.meterRegistry = meterRegistry;
        this.authLoginDurationTimer = authLoginDurationTimer;
        this.authLoginSuccessCounter = authLoginSuccessCounter;
        this.authLoginTotalCounter = authLoginTotalCounter;
        this.authLoginFailureCounter = authLoginFailureCounter;
        this.authPasswordRecoveryDurationTimer = authPasswordRecoveryDurationTimer;
        this.auth2FAVerificationDurationTimer = auth2FAVerificationDurationTimer;
        this.auth2FACodesGeneratedCounter = auth2FACodesGeneratedCounter;
        this.auth2FACodesExpiredCounter = auth2FACodesExpiredCounter;
        this.authSessionDurationTimer = authSessionDurationTimer;
    }

    /**
     * Registra un intento de login con su duración y resultado.
     * Actualiza counters totales, éxitos y fallos, además de timer.
     * Umbral: duration < 2000ms (p95)
     *
     * @param role Rol del usuario (ADMIN, MESERO, COCINA, CLIENTE)
     * @param success true si el login fue exitoso, false en caso contrario
     * @param durationMs Duración del intento en milisegundos
     */
    public void recordLoginAttempt(String role, boolean success, long durationMs) {
        // Record duration
        authLoginDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);

        // Record counters with role tag
        authLoginTotalCounter.increment();
        if (success) {
            authLoginSuccessCounter.increment();
        } else {
            authLoginFailureCounter.increment();
        }

        // Create tagged counters per role
        Counter.builder("auth.login.by_role")
            .tag("role", role != null ? role : "UNKNOWN")
            .tag("result", success ? "SUCCESS" : "FAILURE")
            .register(meterRegistry)
            .increment();

        log.info("✅ METRIC RECORDED: auth.login.attempt | role={} | success={} | duration={}ms | total={} | success_rate={:.1f}%",
            role, success, durationMs,
            (int)authLoginTotalCounter.count(),
            getLoginSuccessRate());
    }

    /**
     * Registra el tiempo de recuperación de contraseña.
     * Umbral: duration < 300000 ms (5 minutos)
     *
     * @param durationMs Duración del proceso en milisegundos
     */
    public void recordPasswordRecovery(long durationMs) {
        authPasswordRecoveryDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Registra el tiempo de verificación de código 2FA.
     * Umbral: duration < 60000 ms (60 segundos)
     *
     * @param durationMs Duración de la verificación en milisegundos
     */
    public void record2FAVerification(long durationMs) {
        auth2FAVerificationDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Registra la generación de un código 2FA.
     * Usado para calcular la tasa de expiración.
     */
    public void record2FACodeGenerated() {
        auth2FACodesGeneratedCounter.increment();
    }

    /**
     * Registra la expiración de un código 2FA.
     * Umbral: tasa < 10% (expired / generated)
     */
    public void record2FACodeExpired() {
        auth2FACodesExpiredCounter.increment();
    }

    /**
     * Registra la duración de una sesión de usuario.
     * Umbral: ≈ shift duration (depende del rol)
     *
     * @param role Rol del usuario
     * @param durationMs Duración de la sesión en milisegundos
     */
    public void recordSessionDuration(String role, long durationMs) {
        authSessionDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);

        // Create tagged gauge per role
        Counter.builder("auth.session.by_role")
            .tag("role", role != null ? role : "UNKNOWN")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Obtiene la tasa actual de éxito de login.
     * @return Porcentaje de éxito (0-100)
     */
    public double getLoginSuccessRate() {
        double total = authLoginTotalCounter.count();
        if (total == 0) return 0.0;
        return (authLoginSuccessCounter.count() / total) * 100;
    }

    /**
     * Obtiene la tasa actual de expiración de códigos 2FA.
     * @return Porcentaje de expiración (0-100)
     */
    public double get2FAExpirationRate() {
        double generated = auth2FACodesGeneratedCounter.count();
        if (generated == 0) return 0.0;
        return (auth2FACodesExpiredCounter.count() / generated) * 100;
    }
}

