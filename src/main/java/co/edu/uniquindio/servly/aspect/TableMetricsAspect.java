package co.edu.uniquindio.servly.aspect;

import co.edu.uniquindio.servly.metrics.TableMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspecto AOP para interceptar y registrar automáticamente métricas en métodos críticos
 * de gestión de mesas y pedidos.
 *
 * Intercepta:
 * - Métodos en TableSessionService: registra apertura y cierre de sesiones
 * - Métodos en OrderService: registra creación de pedidos y transiciones de estado
 *
 * Automáticamente captura:
 * - Duración de ejecución
 * - Resultado (éxito/fallo)
 *
 * @author Servly Backend Team
 * @version 1.0.0
 */
@Aspect
@Component
@Slf4j
public class TableMetricsAspect {

    private final TableMetricsService tableMetricsService;

    /**
     * Constructor con inyección de dependencias.
     */
    public TableMetricsAspect(TableMetricsService tableMetricsService) {
        this.tableMetricsService = tableMetricsService;
    }

    /**
     * Intercepts public methods in TableSessionService (openSession, closeSession).
     * Records duration and success/failure automatically.
     * Threshold: open duration < 500ms (p95), close success >= 98%
     *
     * Pointcut: Cualquier método público en la clase TableSessionService
     */
    @Around("execution(public * co.edu.uniquindio.servly.service.TableSessionService.*(..))")
    public Object recordTableSessionMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = false;

        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Exception e) {
            success = false;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String methodName = joinPoint.getSignature().getName();

            if ("openSession".equals(methodName)) {
                tableMetricsService.recordSessionOpen(success, duration);
                log.info("📊 Table session open recorded: success={}, duration={}ms", success, duration);
            } else if ("closeSession".equals(methodName)) {
                tableMetricsService.recordSessionClose(success);
                log.info("📊 Table session close recorded: success={}", success);
            }
        }
    }

    /**
     * Intercepts public methods in OrderService (createTableOrder, updateOrderStatus, confirmPayment).
     * Records duration and success/failure automatically.
     * Threshold: creation duration < 1000ms (p95), success >= 95%
     *
     * Pointcut: Cualquier método público en la clase OrderService
     */
    @Around("execution(public * co.edu.uniquindio.servly.service.OrderService.*(..))")
    public Object recordOrderServiceMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = false;

        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Exception e) {
            success = false;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String methodName = joinPoint.getSignature().getName();

            if ("createTableOrder".equals(methodName) || "createTableOrderFromClient".equals(methodName) ||
                "createDeliveryOrder".equals(methodName)) {
                tableMetricsService.recordOrderCreation(success, duration);
                log.info("📊 Order creation recorded: success={}, duration={}ms", success, duration);
            } else if ("updateOrderStatus".equals(methodName) || "confirmPayment".equals(methodName)) {
                // Extraer orderId del resultado si es posible
                if (result != null) {
                    try {
                        Long orderId = (Long) result.getClass().getMethod("getId").invoke(result);
                        tableMetricsService.startOrderStatusTransition(orderId);
                        tableMetricsService.endOrderStatusTransition(orderId);
                        log.info("📊 Order status transition recorded: orderId={}", orderId);
                    } catch (Exception e) {
                        log.debug("No se pudo extraer orderId del resultado: {}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Intercepts public methods in OrderNotificationService (notifyOrderStatusChange).
     * Records notification latency automatically.
     * Threshold: latency < 500ms (p95)
     *
     * Pointcut: Cualquier método público en la clase OrderNotificationService
     */
    @Around("execution(public * co.edu.uniquindio.servly.service.OrderNotificationService.*(..))")
    public Object recordNotificationMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = false;

        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Exception e) {
            success = false;
            throw e;
        } finally {
            long latency = System.currentTimeMillis() - startTime;
            tableMetricsService.recordNotification(success, latency);
            log.info("📊 Notification latency recorded: success={}, latency={}ms", success, latency);
        }
    }
}
