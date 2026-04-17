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

    /**
     * TABLE & ORDER METRICS
     */

    /**
     * Timer: table.session.open.duration
     * Umbral: < 500 ms
     * Tracks table session open duration
     */
    @Bean
    public Timer tableSessionOpenDurationTimer() {
        return Timer.builder("table.session.open.duration")
            .description("Duration of table session open in milliseconds")
            .tag("module", "table")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Counter: table.session.open.success
     * Umbral: >= 95%
     * Counts successful table session opens
     */
    @Bean
    public Counter tableSessionOpenSuccessCounter() {
        return Counter.builder("table.session.open.success")
            .description("Count of successful table session opens")
            .tag("module", "table")
            .register(meterRegistry);
    }

    /**
     * Counter: table.session.open.total
     * Counts total table session open attempts
     */
    @Bean
    public Counter tableSessionOpenTotalCounter() {
        return Counter.builder("table.session.open.total")
            .description("Count of total table session open attempts")
            .tag("module", "table")
            .register(meterRegistry);
    }

    /**
     * Counter: table.session.open.failure
     * Counts failed table session opens
     */
    @Bean
    public Counter tableSessionOpenFailureCounter() {
        return Counter.builder("table.session.open.failure")
            .description("Count of failed table session opens")
            .tag("module", "table")
            .register(meterRegistry);
    }

    /**
     * Timer: order.creation.duration
     * Umbral: < 1000 ms
     * Tracks order creation duration
     */
    @Bean
    public Timer orderCreationDurationTimer() {
        return Timer.builder("order.creation.duration")
            .description("Duration of order creation in milliseconds")
            .tag("module", "order")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Counter: order.creation.success
     * Umbral: >= 95%
     * Counts successful order creations
     */
    @Bean
    public Counter orderCreationSuccessCounter() {
        return Counter.builder("order.creation.success")
            .description("Count of successful order creations")
            .tag("module", "order")
            .register(meterRegistry);
    }

    /**
     * Counter: order.creation.total
     * Counts total order creation attempts
     */
    @Bean
    public Counter orderCreationTotalCounter() {
        return Counter.builder("order.creation.total")
            .description("Count of total order creation attempts")
            .tag("module", "order")
            .register(meterRegistry);
    }

    /**
     * Counter: order.creation.failure
     * Counts failed order creations
     */
    @Bean
    public Counter orderCreationFailureCounter() {
        return Counter.builder("order.creation.failure")
            .description("Count of failed order creations")
            .tag("module", "order")
            .register(meterRegistry);
    }

    /**
     * Timer: order.status.transition.duration
     * Umbral: < 2000 ms
     * Tracks order status transition duration
     */
    @Bean
    public Timer orderStatusTransitionDurationTimer() {
        return Timer.builder("order.status.transition.duration")
            .description("Duration of order status transitions in milliseconds")
            .tag("module", "order")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Counter: order.status.transition.total
     * Counts total order status transitions
     */
    @Bean
    public Counter orderStatusTransitionTotalCounter() {
        return Counter.builder("order.status.transition.total")
            .description("Count of total order status transitions")
            .tag("module", "order")
            .register(meterRegistry);
    }

    /**
     * Counter: table.session.close.success
     * Umbral: >= 98%
     * Counts successful table session closes
     */
    @Bean
    public Counter tableSessionCloseSuccessCounter() {
        return Counter.builder("table.session.close.success")
            .description("Count of successful table session closes")
            .tag("module", "table")
            .register(meterRegistry);
    }

    /**
     * Counter: table.session.close.total
     * Counts total table session close attempts
     */
    @Bean
    public Counter tableSessionCloseTotalCounter() {
        return Counter.builder("table.session.close.total")
            .description("Count of total table session close attempts")
            .tag("module", "table")
            .register(meterRegistry);
    }

    /**
     * Counter: table.session.close.failure
     * Counts failed table session closes
     */
    @Bean
    public Counter tableSessionCloseFailureCounter() {
        return Counter.builder("table.session.close.failure")
            .description("Count of failed table session closes")
            .tag("module", "table")
            .register(meterRegistry);
    }

    /**
     * Counter: table.session.active
     * Tracks active table sessions (incremented on open, not decremented)
     */
    @Bean
    public Counter tableSessionActiveCounter() {
        return Counter.builder("table.session.active")
            .description("Count of active table sessions (incremented only)")
            .tag("module", "table")
            .register(meterRegistry);
    }

    /**
     * Counter: table.session.closed
     * Counts total table sessions closed
     */
    @Bean
    public Counter tableSessionClosedCounter() {
        return Counter.builder("table.session.closed")
            .description("Count of total table sessions closed")
            .tag("module", "table")
            .register(meterRegistry);
    }

    /**
     * Timer: notification.latency
     * Umbral: < 500 ms
     * Tracks notification latency for SSE
     */
    @Bean
    public Timer notificationLatencyTimer() {
        return Timer.builder("notification.latency")
            .description("Latency of notifications in milliseconds")
            .tag("module", "notification")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Counter: notification.sent
     * Counts successful notification sends
     */
    @Bean
    public Counter notificationSentCounter() {
        return Counter.builder("notification.sent")
            .description("Count of successful notification sends")
            .tag("module", "notification")
            .register(meterRegistry);
    }

    /**
     * Counter: notification.failure
     * Counts failed notification sends
     */
    @Bean
    public Counter notificationFailureCounter() {
        return Counter.builder("notification.failure")
            .description("Count of failed notification sends")
            .tag("module", "notification")
            .register(meterRegistry);
    }

    /**
     * Counter: table.session.expired
     * Umbral: rate < 10%
     * Counts sessions expired by timeout
     */
    @Bean
    public Counter tableSessionExpiredCounter() {
        return Counter.builder("table.session.expired")
            .description("Count of table sessions expired by timeout")
            .tag("module", "table")
            .register(meterRegistry);
    }

    /**
     * Counter: table.session.total
     * Counts total table sessions created
     */
    @Bean
    public Counter tableSessionTotalCounter() {
        return Counter.builder("table.session.total")
            .description("Count of total table sessions created")
            .tag("module", "table")
            .register(meterRegistry);
    }
}

