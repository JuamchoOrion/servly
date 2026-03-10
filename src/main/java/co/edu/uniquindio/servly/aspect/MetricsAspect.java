package co.edu.uniquindio.servly.aspect;

import co.edu.uniquindio.servly.metrics.AuthMetricsService;
import co.edu.uniquindio.servly.metrics.InventoryMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Aspecto AOP para interceptar y registrar automáticamente métricas en métodos críticos.
 *
 * Intercepta:
 * - Métodos en AuthService: registra duración de login y otras operaciones de autenticación
 * - Métodos en ItemService: registra duración de creación y CRUD
 * - Métodos en CategoryService: registra duración de creación, toggle y CRUD
 *
 * Automáticamente captura:
 * - Duración de ejecución
 * - Resultado (éxito/fallo)
 * - Rol del usuario desde SecurityContextHolder
 *
 * @author Servly Backend Team
 * @version 1.0.0
 */
@Aspect
@Component
@Slf4j
public class MetricsAspect {

    private final AuthMetricsService authMetricsService;
    private final InventoryMetricsService inventoryMetricsService;

    /**
     * Constructor con inyección de dependencias.
     */
    public MetricsAspect(AuthMetricsService authMetricsService,
                        InventoryMetricsService inventoryMetricsService) {
        this.authMetricsService = authMetricsService;
        this.inventoryMetricsService = inventoryMetricsService;
    }

    /**
     * Intercepts public methods in AuthService (e.g., login, register, resetPassword).
     * Records duration and success/failure automatically.
     * Threshold: < 2000ms (p95)
     *
     * Pointcut: Cualquier método público en la clase AuthService
     */
    @Around("execution(public * co.edu.uniquindio.servly.service.AuthService.*(..))")
    public Object recordAuthServiceMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
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
            String role = extractRoleFromContext();

            // Record based on method name
            if ("login".equals(methodName) || "authenticate".equals(methodName)) {
                authMetricsService.recordLoginAttempt(role, success, duration);
                log.info("📊 Auth login attempt recorded: role={}, success={}, duration={}ms",
                    role, success, duration);
            } else if ("resetPassword".equals(methodName) || "forgotPassword".equals(methodName)) {
                authMetricsService.recordPasswordRecovery(duration);
                log.info("📊 Auth password recovery recorded: duration={}ms", duration);
            } else if ("verify2FA".equals(methodName) || "validateTwoFACode".equals(methodName)) {
                authMetricsService.record2FAVerification(duration);
                log.info("📊 Auth 2FA verification recorded: duration={}ms", duration);
            }
        }
    }

    /**
     * Intercepts public methods in ItemService (e.g., create, update, delete).
     * Records duration and success/failure automatically.
     * Threshold: < 500ms (p95)
     *
     * Pointcut: Cualquier método público en la clase ItemService
     */
    @Around("execution(public * co.edu.uniquindio.servly.service.ItemService.*(..))")
    public Object recordItemServiceMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
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

            // Record item creation/CRUD metrics
            if ("create".equals(methodName) || "save".equals(methodName) ||
                "createItem".equals(methodName) || "saveItem".equals(methodName)) {
                inventoryMetricsService.recordItemCreation(success, duration);
                log.info("📊 Inventory item creation recorded: success={}, duration={}ms",
                    success, duration);
            } else if ("findAll".equals(methodName) || "getAll".equals(methodName) ||
                       "getAllPaginated".equals(methodName) || "findAllPaginated".equals(methodName) ||
                       "getAllItemsPaginated".equals(methodName) || "getItemsByCategory".equals(methodName)) {
                inventoryMetricsService.recordPaginatedQuery(duration);
                log.info("📊 Inventory paginated query recorded: method={}, duration={}ms", methodName, duration);
            }
        }
    }

    /**
     * Intercepts public methods in CategoryService (e.g., create, toggle, update).
     * Records duration and success/failure automatically.
     * Threshold: < 300ms (p95)
     *
     * Pointcut: Cualquier método público en la clase CategoryService
     */
    @Around("execution(public * co.edu.uniquindio.servly.service.ItemCategoryService.*(..))")
    public Object recordCategoryServiceMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = false;

        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Exception e) {
            // Check if it's a duplicate error
            if (e.getMessage() != null && (
                e.getMessage().contains("duplicate") ||
                e.getMessage().contains("already exists") ||
                e.getMessage().contains("unique constraint"))) {
                inventoryMetricsService.recordCategoryDuplicateError();
                log.debug("Inventory category duplicate error recorded");
            }
            success = false;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String methodName = joinPoint.getSignature().getName();

            // Record category creation/CRUD metrics
            if ("create".equals(methodName) || "save".equals(methodName) ||
                "createCategory".equals(methodName) || "saveCategory".equals(methodName)) {
                inventoryMetricsService.recordCategoryCreation(success, duration);
                log.info("📊 Inventory category creation recorded: success={}, duration={}ms",
                    success, duration);
            } else if ("toggleActive".equals(methodName) || "toggle".equals(methodName) ||
                       "toggleCategory".equals(methodName)) {
                inventoryMetricsService.recordCategoryToggle(success);
                log.info("📊 Inventory category toggle recorded: success={}, method={}",
                    success, methodName);
            } else if ("findAll".equals(methodName) || "getAll".equals(methodName) ||
                       "getAllPaginated".equals(methodName) || "findAllPaginated".equals(methodName) ||
                       "getAllCategories".equals(methodName)) {
                inventoryMetricsService.recordPaginatedQuery(duration);
                log.info("📊 Inventory paginated query recorded: duration={}ms", duration);
            }
        }
    }

    /**
     * Extrae el rol del usuario del SecurityContextHolder.
     * @return Nombre del rol (ADMIN, MESERO, COCINA, CLIENTE) o "ANONYMOUS" si no está autenticado
     */
    private String extractRoleFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("UNKNOWN");
        }

        return "ANONYMOUS";
    }
}

