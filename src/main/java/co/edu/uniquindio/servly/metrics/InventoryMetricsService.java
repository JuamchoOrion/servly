package co.edu.uniquindio.servly.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para registrar métricas de inventario.
 * Registra creación de items, categorías, operaciones de toggle, y consultas paginadas.
 *
 * Umbrales ISO/IEC 25010:
 * - inventory.item.creation.duration: < 500 ms (p95)
 * - inventory.item.creation.success: >= 95%
 * - inventory.category.creation.duration: < 300 ms (p95)
 * - inventory.category.creation.success: >= 90%
 * - inventory.category.duplicate.errors: < 10% of total
 * - inventory.category.toggle.success: >= 98%
 * - inventory.items.paginated.query.duration: < 150 ms/page (p95)
 * - inventory.crud.endpoints.tested: = 100%
 *
 * @author Servly Backend Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class InventoryMetricsService {

    private final MeterRegistry meterRegistry;
    private final Timer inventoryItemCreationDurationTimer;
    private final Counter inventoryItemCreationSuccessCounter;
    private final Counter inventoryItemCreationTotalCounter;
    private final Counter inventoryItemCreationFailureCounter;
    private final Timer inventoryCategoryCreationDurationTimer;
    private final Counter inventoryCategoryCreationSuccessCounter;
    private final Counter inventoryCategoryCreationTotalCounter;
    private final Counter inventoryCategoryCreationFailureCounter;
    private final Counter inventoryCategoryDuplicateErrorsCounter;
    private final Counter inventoryCategoryToggleSuccessCounter;
    private final Counter inventoryCategoryToggleTotalCounter;
    private final Timer inventoryPaginatedQueryDurationTimer;

    /**
     * Constructor con inyección de dependencias.
     */
    public InventoryMetricsService(
        MeterRegistry meterRegistry,
        Timer inventoryItemCreationDurationTimer,
        Counter inventoryItemCreationSuccessCounter,
        Counter inventoryItemCreationTotalCounter,
        Counter inventoryItemCreationFailureCounter,
        Timer inventoryCategoryCreationDurationTimer,
        Counter inventoryCategoryCreationSuccessCounter,
        Counter inventoryCategoryCreationTotalCounter,
        Counter inventoryCategoryCreationFailureCounter,
        Counter inventoryCategoryDuplicateErrorsCounter,
        Counter inventoryCategoryToggleSuccessCounter,
        Counter inventoryCategoryToggleTotalCounter,
        Timer inventoryPaginatedQueryDurationTimer
    ) {
        this.meterRegistry = meterRegistry;
        this.inventoryItemCreationDurationTimer = inventoryItemCreationDurationTimer;
        this.inventoryItemCreationSuccessCounter = inventoryItemCreationSuccessCounter;
        this.inventoryItemCreationTotalCounter = inventoryItemCreationTotalCounter;
        this.inventoryItemCreationFailureCounter = inventoryItemCreationFailureCounter;
        this.inventoryCategoryCreationDurationTimer = inventoryCategoryCreationDurationTimer;
        this.inventoryCategoryCreationSuccessCounter = inventoryCategoryCreationSuccessCounter;
        this.inventoryCategoryCreationTotalCounter = inventoryCategoryCreationTotalCounter;
        this.inventoryCategoryCreationFailureCounter = inventoryCategoryCreationFailureCounter;
        this.inventoryCategoryDuplicateErrorsCounter = inventoryCategoryDuplicateErrorsCounter;
        this.inventoryCategoryToggleSuccessCounter = inventoryCategoryToggleSuccessCounter;
        this.inventoryCategoryToggleTotalCounter = inventoryCategoryToggleTotalCounter;
        this.inventoryPaginatedQueryDurationTimer = inventoryPaginatedQueryDurationTimer;
    }

    /**
     * Registra la creación de un item con su duración y resultado.
     * Umbral: duration < 500 ms (p95), success >= 95%
     *
     * @param success true si la creación fue exitosa, false en caso contrario
     * @param durationMs Duración de la operación en milisegundos
     */
    public void recordItemCreation(boolean success, long durationMs) {
        // Record duration
        inventoryItemCreationDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);

        // Record counters
        inventoryItemCreationTotalCounter.increment();
        if (success) {
            inventoryItemCreationSuccessCounter.increment();
        } else {
            inventoryItemCreationFailureCounter.increment();
        }

        log.info("✅ METRIC RECORDED: inventory.item.creation | success={} | duration={}ms | total={} | success_rate={:.1f}%",
            success, durationMs,
            (int)inventoryItemCreationTotalCounter.count(),
            getItemCreationSuccessRate());
    }

    /**
     * Registra la creación de una categoría con su duración y resultado.
     * Umbral: duration < 300 ms (p95), success >= 90%
     *
     * @param success true si la creación fue exitosa, false en caso contrario
     * @param durationMs Duración de la operación en milisegundos
     */
    public void recordCategoryCreation(boolean success, long durationMs) {
        // Record duration
        inventoryCategoryCreationDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);

        // Record counters
        inventoryCategoryCreationTotalCounter.increment();
        if (success) {
            inventoryCategoryCreationSuccessCounter.increment();
        } else {
            inventoryCategoryCreationFailureCounter.increment();
        }

        log.info("✅ METRIC RECORDED: inventory.category.creation | success={} | duration={}ms | total={} | success_rate={:.1f}%",
            success, durationMs,
            (int)inventoryCategoryCreationTotalCounter.count(),
            getCategoryCreationSuccessRate());
    }

    /**
     * Registra un error de categoría duplicada.
     * Umbral: < 10% de los intentos totales de creación
     */
    public void recordCategoryDuplicateError() {
        inventoryCategoryDuplicateErrorsCounter.increment();
    }

    /**
     * Registra una operación de toggle de categoría.
     * Umbral: success >= 98%
     *
     * @param success true si el toggle fue exitoso, false en caso contrario
     */
    public void recordCategoryToggle(boolean success) {
        inventoryCategoryToggleTotalCounter.increment();
        if (success) {
            inventoryCategoryToggleSuccessCounter.increment();
        }
    }

    /**
     * Registra la duración de una consulta paginada de items.
     * Umbral: duration < 150 ms/page (p95)
     *
     * @param durationMs Duración de la consulta en milisegundos
     */
    public void recordPaginatedQuery(long durationMs) {
        inventoryPaginatedQueryDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
        log.info("✅ METRIC RECORDED: inventory.items.paginated.query | duration={}ms", durationMs);
    }

    /**
     * Obtiene la tasa actual de éxito de creación de items.
     * @return Porcentaje de éxito (0-100)
     */
    public double getItemCreationSuccessRate() {
        double total = inventoryItemCreationTotalCounter.count();
        if (total == 0) return 0.0;
        return (inventoryItemCreationSuccessCounter.count() / total) * 100;
    }

    /**
     * Obtiene la tasa actual de éxito de creación de categorías.
     * @return Porcentaje de éxito (0-100)
     */
    public double getCategoryCreationSuccessRate() {
        double total = inventoryCategoryCreationTotalCounter.count();
        if (total == 0) return 0.0;
        return (inventoryCategoryCreationSuccessCounter.count() / total) * 100;
    }

    /**
     * Obtiene la tasa actual de duplicación de categorías.
     * @return Porcentaje de duplicación (0-100)
     */
    public double getCategoryDuplicateErrorRate() {
        double total = inventoryCategoryCreationTotalCounter.count();
        if (total == 0) return 0.0;
        return (inventoryCategoryDuplicateErrorsCounter.count() / total) * 100;
    }

    /**
     * Obtiene la tasa actual de éxito de toggle de categorías.
     * @return Porcentaje de éxito (0-100)
     */
    public double getCategoryToggleSuccessRate() {
        double total = inventoryCategoryToggleTotalCounter.count();
        if (total == 0) return 0.0;
        return (inventoryCategoryToggleSuccessCounter.count() / total) * 100;
    }
}

