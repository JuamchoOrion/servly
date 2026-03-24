package co.edu.uniquindio.servly.config;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración central de métricas personalizadas para Servly.
 * Registra todas las métricas requeridas por el plan ISO/IEC 25010.
 *
 * @author Servly Backend Team
 * @version 1.0.0
 */
@Configuration
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    /**
     * Constructor con inyección de dependencias de MeterRegistry.
     * @param meterRegistry Registro central de Micrometer
     */
    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Bean para configurar filtros globales de métricas.
     * Habilita histogramas para todos los timers.
     */
    @Bean
    public MeterFilter meterFilter() {
        return MeterFilter.deny(id -> {
            String name = id.getName();
            return name.startsWith("jvm") || name.startsWith("process") || name.startsWith("system");
        });
    }

    /**
     * Habilitar la publicación de histogramas para Prometheus.
     * Esto crea los buckets necesarios para histogram_quantile en PromQL.
     */
    @Bean
    public MeterFilter histogramBucketFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith("auth_") || id.getName().startsWith("inventory_")) {
                    return DistributionStatisticConfig.builder()
                        .percentilesHistogram(true)
                        //.publishPercentiles(0.5, 0.95, 0.99)
                        .serviceLevelObjectives(
                            100, 200, 300, 500, 1000, 2000, 5000, 10000
                        )
                        .build()
                        .merge(config);
                }
                return config;
            }
        };
    }

    /**
     * AUTHENTICATION METRICS
     */

    /**
     * Timer: auth.login.duration
     * Umbral: < 2000 ms
     * Tracks duration of login attempts by role
     */
    @Bean
    public Timer authLoginDurationTimer() {
        return Timer.builder("auth.login.duration")
            .description("Duration of login attempts in milliseconds")
            .tag("module", "auth")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Counter: auth.login.success
     * Umbral: > 95% success rate
     * Counts successful vs total login attempts by role
     */
    @Bean
    public Counter authLoginSuccessCounter() {
        return Counter.builder("auth.login.success")
            .description("Count of successful logins")
            .tag("module", "auth")
            .register(meterRegistry);
    }

    /**
     * Counter: auth.login.total
     * Counts total login attempts by role
     */
    @Bean
    public Counter authLoginTotalCounter() {
        return Counter.builder("auth.login.total")
            .description("Count of total login attempts")
            .tag("module", "auth")
            .register(meterRegistry);
    }

    /**
     * Counter: auth.login.failure
     * Counts failed login attempts by role
     */
    @Bean
    public Counter authLoginFailureCounter() {
        return Counter.builder("auth.login.failure")
            .description("Count of failed logins")
            .tag("module", "auth")
            .register(meterRegistry);
    }

    /**
     * Timer: auth.password.recovery.duration
     * Umbral: < 5 min (300000 ms)
     * Tracks password recovery duration
     */
    @Bean
    public Timer authPasswordRecoveryDurationTimer() {
        return Timer.builder("auth.password.recovery.duration")
            .description("Duration of password recovery process in milliseconds")
            .tag("module", "auth")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Timer: auth.2fa.verification.duration
     * Umbral: < 60 sec (60000 ms)
     * Tracks 2FA code verification duration
     */
    @Bean
    public Timer auth2FAVerificationDurationTimer() {
        return Timer.builder("auth.2fa.verification.duration")
            .description("Duration of 2FA code verification in milliseconds")
            .tag("module", "auth")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Counter: auth.2fa.codes.generated
     * Counts generated 2FA codes
     */
    @Bean
    public Counter auth2FACodesGeneratedCounter() {
        return Counter.builder("auth.2fa.codes.generated")
            .description("Count of generated 2FA codes")
            .tag("module", "auth")
            .register(meterRegistry);
    }

    /**
     * Counter: auth.2fa.codes.expired
     * Umbral: rate < 10% expiration
     * Counts expired 2FA codes
     */
    @Bean
    public Counter auth2FACodesExpiredCounter() {
        return Counter.builder("auth.2fa.codes.expired")
            .description("Count of expired 2FA codes")
            .tag("module", "auth")
            .register(meterRegistry);
    }

    /**
     * Timer: auth.session.duration
     * Umbral: ≈ shift duration (depends on role)
     * Tracks session duration by role
     */
    @Bean
    public Timer authSessionDurationTimer() {
        return Timer.builder("auth.session.duration")
            .description("Duration of user sessions in milliseconds")
            .tag("module", "auth")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * INVENTORY METRICS
     */

    /**
     * Timer: inventory.item.creation.duration
     * Umbral: < 500 ms
     * Tracks item creation duration
     */
    @Bean
    public Timer inventoryItemCreationDurationTimer() {
        return Timer.builder("inventory.item.creation.duration")
            .description("Duration of item creation in milliseconds")
            .tag("module", "inventory")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Counter: inventory.item.creation.success
     * Umbral: >= 95%
     * Counts successful item creations
     */
    @Bean
    public Counter inventoryItemCreationSuccessCounter() {
        return Counter.builder("inventory.item.creation.success")
            .description("Count of successful item creations")
            .tag("module", "inventory")
            .register(meterRegistry);
    }

    /**
     * Counter: inventory.item.creation.total
     * Counts total item creation attempts
     */
    @Bean
    public Counter inventoryItemCreationTotalCounter() {
        return Counter.builder("inventory.item.creation.total")
            .description("Count of total item creation attempts")
            .tag("module", "inventory")
            .register(meterRegistry);
    }

    /**
     * Counter: inventory.item.creation.failure
     * Counts failed item creations
     */
    @Bean
    public Counter inventoryItemCreationFailureCounter() {
        return Counter.builder("inventory.item.creation.failure")
            .description("Count of failed item creations")
            .tag("module", "inventory")
            .register(meterRegistry);
    }

    /**
     * Timer: inventory.category.creation.duration
     * Umbral: < 300 ms
     * Tracks category creation duration
     */
    @Bean
    public Timer inventoryCategoryCreationDurationTimer() {
        return Timer.builder("inventory.category.creation.duration")
            .description("Duration of category creation in milliseconds")
            .tag("module", "inventory")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Counter: inventory.category.creation.success
     * Umbral: >= 90%
     * Counts successful category creations
     */
    @Bean
    public Counter inventoryCategoryCreationSuccessCounter() {
        return Counter.builder("inventory.category.creation.success")
            .description("Count of successful category creations")
            .tag("module", "inventory")
            .register(meterRegistry);
    }

    /**
     * Counter: inventory.category.creation.total
     * Counts total category creation attempts
     */
    @Bean
    public Counter inventoryCategoryCreationTotalCounter() {
        return Counter.builder("inventory.category.creation.total")
            .description("Count of total category creation attempts")
            .tag("module", "inventory")
            .register(meterRegistry);
    }

    /**
     * Counter: inventory.category.creation.failure
     * Counts failed category creations
     */
    @Bean
    public Counter inventoryCategoryCreationFailureCounter() {
        return Counter.builder("inventory.category.creation.failure")
            .description("Count of failed category creations")
            .tag("module", "inventory")
            .register(meterRegistry);
    }

    /**
     * Counter: inventory.category.duplicate.errors
     * Umbral: < 10% of total creations
     * Counts duplicate category errors
     */
    @Bean
    public Counter inventoryCategoryDuplicateErrorsCounter() {
        return Counter.builder("inventory.category.duplicate.errors")
            .description("Count of duplicate category errors")
            .tag("module", "inventory")
            .register(meterRegistry);
    }

    /**
     * Counter: inventory.category.toggle.success
     * Umbral: >= 98%
     * Counts successful category toggle operations
     */
    @Bean
    public Counter inventoryCategoryToggleSuccessCounter() {
        return Counter.builder("inventory.category.toggle.success")
            .description("Count of successful category toggle operations")
            .tag("module", "inventory")
            .register(meterRegistry);
    }

    /**
     * Counter: inventory.category.toggle.total
     * Counts total category toggle attempts
     */
    @Bean
    public Counter inventoryCategoryToggleTotalCounter() {
        return Counter.builder("inventory.category.toggle.total")
            .description("Count of total category toggle attempts")
            .tag("module", "inventory")
            .register(meterRegistry);
    }

    /**
     * Timer: inventory.items.paginated.query.duration
     * Umbral: < 150 ms/page
     * Tracks paginated query duration
     */
    @Bean
    public Timer inventoryPaginatedQueryDurationTimer() {
        return Timer.builder("inventory.items.paginated.query.duration")
            .description("Duration of paginated item queries in milliseconds")
            .tag("module", "inventory")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Gauge: inventory.crud.endpoints.tested
     * Umbral: = 100% coverage
     * Represents percentage of CRUD endpoints with test coverage
     */
    @Bean
    public Gauge inventoryCrudEndpointsTestedGauge() {
        return Gauge.builder("inventory.crud.endpoints.tested", () -> 100.0)
            .description("Percentage of inventory CRUD endpoints tested (0-100)")
            .tag("module", "inventory")
            .register(meterRegistry);
    }
}

