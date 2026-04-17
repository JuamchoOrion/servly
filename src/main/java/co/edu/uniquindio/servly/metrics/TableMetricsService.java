package co.edu.uniquindio.servly.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Servicio para registrar métricas de gestión de mesas y pedidos.
 * Registra apertura de sesiones, creación de pedidos, transiciones de estado, cierres de sesión.
 *
 * Umbrales ISO/IEC 25010:
 * - table.session.open.duration: < 500 ms (p95)
 * - table.session.open.success: >= 95%
 * - order.creation.duration: < 1000 ms (p95)
 * - order.creation.success: >= 95%
 * - order.status.transition.duration: < 2000 ms (p95)
 * - table.session.close.success: >= 98%
 * - table.session.concurrent: monitoreo en tiempo real
 * - notification.latency: < 500 ms (p95)
 * - table.session.expired.rate: < 10%
 *
 * @author Servly Backend Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class TableMetricsService {

    private final MeterRegistry meterRegistry;

    // Apertura de sesiones de mesa
    private final Timer tableSessionOpenDurationTimer;
    private final Counter tableSessionOpenSuccessCounter;
    private final Counter tableSessionOpenTotalCounter;
    private final Counter tableSessionOpenFailureCounter;

    // Creación de pedidos
    private final Timer orderCreationDurationTimer;
    private final Counter orderCreationSuccessCounter;
    private final Counter orderCreationTotalCounter;
    private final Counter orderCreationFailureCounter;

    // Transiciones de estado de pedidos
    private final Timer orderStatusTransitionDurationTimer;
    private final Counter orderStatusTransitionTotalCounter;

    // Cierre de sesiones de mesa
    private final Counter tableSessionCloseSuccessCounter;
    private final Counter tableSessionCloseTotalCounter;
    private final Counter tableSessionCloseFailureCounter;

    // Sesiones concurrentes activas (gauge)
    private final Counter tableSessionActiveCounter;
    private final Counter tableSessionClosedCounter;

    // Latencia de notificaciones SSE
    private final Timer notificationLatencyTimer;
    private final Counter notificationSentCounter;
    private final Counter notificationFailureCounter;

    // Sesiones expiradas por timeout
    private final Counter tableSessionExpiredCounter;
    private final Counter tableSessionTotalCounter;

    // Mapa para tracking de tiempos de transición entre estados
    private final Map<Long, Long> orderStatusChangeTimes = new ConcurrentHashMap<>();

    /**
     * Constructor con inyección de dependencias.
     */
    public TableMetricsService(
        MeterRegistry meterRegistry,
        Timer tableSessionOpenDurationTimer,
        Counter tableSessionOpenSuccessCounter,
        Counter tableSessionOpenTotalCounter,
        Counter tableSessionOpenFailureCounter,
        Timer orderCreationDurationTimer,
        Counter orderCreationSuccessCounter,
        Counter orderCreationTotalCounter,
        Counter orderCreationFailureCounter,
        Timer orderStatusTransitionDurationTimer,
        Counter orderStatusTransitionTotalCounter,
        Counter tableSessionCloseSuccessCounter,
        Counter tableSessionCloseTotalCounter,
        Counter tableSessionCloseFailureCounter,
        Counter tableSessionActiveCounter,
        Counter tableSessionClosedCounter,
        Timer notificationLatencyTimer,
        Counter notificationSentCounter,
        Counter notificationFailureCounter,
        Counter tableSessionExpiredCounter,
        Counter tableSessionTotalCounter
    ) {
        this.meterRegistry = meterRegistry;
        this.tableSessionOpenDurationTimer = tableSessionOpenDurationTimer;
        this.tableSessionOpenSuccessCounter = tableSessionOpenSuccessCounter;
        this.tableSessionOpenTotalCounter = tableSessionOpenTotalCounter;
        this.tableSessionOpenFailureCounter = tableSessionOpenFailureCounter;
        this.orderCreationDurationTimer = orderCreationDurationTimer;
        this.orderCreationSuccessCounter = orderCreationSuccessCounter;
        this.orderCreationTotalCounter = orderCreationTotalCounter;
        this.orderCreationFailureCounter = orderCreationFailureCounter;
        this.orderStatusTransitionDurationTimer = orderStatusTransitionDurationTimer;
        this.orderStatusTransitionTotalCounter = orderStatusTransitionTotalCounter;
        this.tableSessionCloseSuccessCounter = tableSessionCloseSuccessCounter;
        this.tableSessionCloseTotalCounter = tableSessionCloseTotalCounter;
        this.tableSessionCloseFailureCounter = tableSessionCloseFailureCounter;
        this.tableSessionActiveCounter = tableSessionActiveCounter;
        this.tableSessionClosedCounter = tableSessionClosedCounter;
        this.notificationLatencyTimer = notificationLatencyTimer;
        this.notificationSentCounter = notificationSentCounter;
        this.notificationFailureCounter = notificationFailureCounter;
        this.tableSessionExpiredCounter = tableSessionExpiredCounter;
        this.tableSessionTotalCounter = tableSessionTotalCounter;

        // Registrar gauge para sesiones concurrentes activas
        Gauge.builder("table.session.concurrent", this, TableMetricsService::getActiveSessionCount)
            .description("Número de sesiones de mesa activas simultáneamente")
            .register(meterRegistry);
    }

    /**
     * Registra la apertura de una sesión de mesa con su duración y resultado.
     * Umbral: duration < 500 ms (p95), success >= 95%
     *
     * @param success true si la apertura fue exitosa, false en caso contrario
     * @param durationMs Duración de la operación en milisegundos
     */
    public void recordSessionOpen(boolean success, long durationMs) {
        tableSessionOpenDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
        tableSessionOpenTotalCounter.increment();
        tableSessionTotalCounter.increment();

        if (success) {
            tableSessionOpenSuccessCounter.increment();
            tableSessionActiveCounter.increment();
        } else {
            tableSessionOpenFailureCounter.increment();
        }

        log.info("✅ METRIC RECORDED: table.session.open | success={} | duration={}ms | total={} | success_rate={:.1f}%",
            success, durationMs,
            (int)tableSessionOpenTotalCounter.count(),
            getSessionOpenSuccessRate());
    }

    /**
     * Registra la creación de un pedido con su duración y resultado.
     * Umbral: duration < 1000 ms (p95), success >= 95%
     *
     * @param success true si la creación fue exitosa, false en caso contrario
     * @param durationMs Duración de la operación en milisegundos
     */
    public void recordOrderCreation(boolean success, long durationMs) {
        orderCreationDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
        orderCreationTotalCounter.increment();

        if (success) {
            orderCreationSuccessCounter.increment();
        } else {
            orderCreationFailureCounter.increment();
        }

        log.info("✅ METRIC RECORDED: order.creation | success={} | duration={}ms | total={} | success_rate={:.1f}%",
            success, durationMs,
            (int)orderCreationTotalCounter.count(),
            getOrderCreationSuccessRate());
    }

    /**
     * Registra el inicio de una transición de estado de pedido.
     * Se debe llamar antes de cambiar el estado.
     *
     * @param orderId ID de la orden
     */
    public void startOrderStatusTransition(Long orderId) {
        orderStatusChangeTimes.put(orderId, System.currentTimeMillis());
    }

    /**
     * Registra el final de una transición de estado de pedido.
     * Calcula la duración y la agrega al timer.
     *
     * @param orderId ID de la orden
     */
    public void endOrderStatusTransition(Long orderId) {
        Long startTime = orderStatusChangeTimes.remove(orderId);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            orderStatusTransitionDurationTimer.record(duration, TimeUnit.MILLISECONDS);
            orderStatusTransitionTotalCounter.increment();

            log.info("✅ METRIC RECORDED: order.status.transition | orderId={} | duration={}ms | total_transitions={}",
                orderId, duration, (int)orderStatusTransitionTotalCounter.count());
        }
    }

    /**
     * Registra el cierre de una sesión de mesa con su resultado.
     * Umbral: success >= 98%
     *
     * @param success true si el cierre fue exitoso, false en caso contrario
     */
    public void recordSessionClose(boolean success) {
        tableSessionCloseTotalCounter.increment();
        tableSessionClosedCounter.increment();

        if (success) {
            tableSessionCloseSuccessCounter.increment();
        } else {
            tableSessionCloseFailureCounter.increment();
        }

        log.info("✅ METRIC RECORDED: table.session.close | success={} | total={} | success_rate={:.1f}%",
            success,
            (int)tableSessionCloseTotalCounter.count(),
            getSessionCloseSuccessRate());
    }

    /**
     * Registra una notificación SSE enviada con su latencia.
     * Umbral: latency < 500 ms (p95)
     *
     * @param success true si la notificación fue enviada exitosamente, false en caso contrario
     * @param latencyMs Latencia de la notificación en milisegundos
     */
    public void recordNotification(boolean success, long latencyMs) {
        notificationLatencyTimer.record(latencyMs, TimeUnit.MILLISECONDS);

        if (success) {
            notificationSentCounter.increment();
        } else {
            notificationFailureCounter.increment();
        }

        log.info("✅ METRIC RECORDED: notification.latency | success={} | latency={}ms | total_sent={}",
            success, latencyMs, (int)notificationSentCounter.count());
    }

    /**
     * Registra una sesión expirada por timeout.
     * Umbral: expired_rate < 10% (expired / total)
     */
    public void recordSessionExpired() {
        tableSessionExpiredCounter.increment();
    }

    /**
     * Obtiene la tasa actual de éxito de apertura de sesiones.
     * @return Porcentaje de éxito (0-100)
     */
    public double getSessionOpenSuccessRate() {
        double total = tableSessionOpenTotalCounter.count();
        if (total == 0) return 0.0;
        return (tableSessionOpenSuccessCounter.count() / total) * 100;
    }

    /**
     * Obtiene la tasa actual de éxito de creación de pedidos.
     * @return Porcentaje de éxito (0-100)
     */
    public double getOrderCreationSuccessRate() {
        double total = orderCreationTotalCounter.count();
        if (total == 0) return 0.0;
        return (orderCreationSuccessCounter.count() / total) * 100;
    }

    /**
     * Obtiene la tasa actual de éxito de cierre de sesiones.
     * @return Porcentaje de éxito (0-100)
     */
    public double getSessionCloseSuccessRate() {
        double total = tableSessionCloseTotalCounter.count();
        if (total == 0) return 0.0;
        return (tableSessionCloseSuccessCounter.count() / total) * 100;
    }

    /**
     * Obtiene la tasa actual de expiración de sesiones.
     * @return Porcentaje de expiración (0-100)
     */
    public double getSessionExpirationRate() {
        double total = tableSessionTotalCounter.count();
        if (total == 0) return 0.0;
        return (tableSessionExpiredCounter.count() / total) * 100;
    }

    /**
     * Obtiene el número de sesiones concurrentes activas.
     * @return Número de sesiones activas
     */
    public int getActiveSessionCount() {
        return (int) tableSessionActiveCounter.count();
    }

    /**
     * Obtiene el tiempo promedio de apertura de sesiones.
     * @return Tiempo promedio en milisegundos
     */
    public double getAverageSessionOpenDuration() {
        return tableSessionOpenDurationTimer.mean(TimeUnit.MILLISECONDS);
    }

    /**
     * Obtiene el tiempo promedio de creación de pedidos.
     * @return Tiempo promedio en milisegundos
     */
    public double getAverageOrderCreationDuration() {
        return orderCreationDurationTimer.mean(TimeUnit.MILLISECONDS);
    }

    /**
     * Obtiene el tiempo promedio de transición de estado de pedidos.
     * @return Tiempo promedio en milisegundos
     */
    public double getAverageOrderStatusTransitionDuration() {
        return orderStatusTransitionDurationTimer.mean(TimeUnit.MILLISECONDS);
    }

    /**
     * Obtiene la latencia promedio de notificaciones.
     * @return Latencia promedio en milisegundos
     */
    public double getAverageNotificationLatency() {
        return notificationLatencyTimer.mean(TimeUnit.MILLISECONDS);
    }
}
