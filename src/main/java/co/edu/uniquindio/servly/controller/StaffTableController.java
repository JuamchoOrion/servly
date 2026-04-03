package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.RestaurantTableDTO;
import co.edu.uniquindio.servly.service.RestaurantTableStaffService;
import co.edu.uniquindio.servly.service.TableSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para operaciones del staff sobre las mesas.
 *
 * WAITER/CASHIER:
 *  - GET    /api/staff/tables                    → Cargar todas las mesas
 *  - GET    /api/staff/tables/{tableNumber}      → Obtener una mesa específica
 *  - DELETE /api/staff/tables/{tableNumber}/session  → Cerrar sesión
 *  - GET    /api/staff/tables/{tableNumber}/session/status → Ver si mesa está activa
 *
 * ADMIN:
 *  - POST   /api/staff/tables                    → Crear mesa
 *  - PATCH  /api/staff/tables/{tableNumber}/status → Cambiar estado
 *  - DELETE /api/staff/tables/{tableNumber}      → Eliminar mesa
 */
@RestController
@RequestMapping("/api/staff/tables")
@RequiredArgsConstructor
public class StaffTableController {

    private final TableSessionService tableSessionService;
    private final RestaurantTableStaffService restaurantTableStaffService;

    // ============ LISTAR Y OBTENER MESAS ============

    /**
     * WAITER/CASHIER: Obtener todas las mesas
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
    public ResponseEntity<List<RestaurantTableDTO>> getAllTables() {
        List<RestaurantTableDTO> dtos = restaurantTableStaffService.getAllTablesForStaff();
        return ResponseEntity.ok(dtos);
    }

    /**
     * WAITER/CASHIER: Obtener una mesa específica
     */
    @GetMapping("/{tableNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
    public ResponseEntity<RestaurantTableDTO> getTable(@PathVariable Integer tableNumber) {
        RestaurantTableDTO dto = restaurantTableStaffService.getTableByNumberForStaff(tableNumber);
        return ResponseEntity.ok(dto);
    }

    // ============ OPERACIONES DE SESIÓN ============

    /**
     * WAITER/CASHIER: Cerrar sesión de una mesa
     */
    @DeleteMapping("/{tableNumber}/session")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
    public ResponseEntity<MessageResponse> closeTableSession(@PathVariable Integer tableNumber) {
        MessageResponse response = tableSessionService.closeSession(tableNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * WAITER/CASHIER: Ver si la mesa está activa
     */
    @GetMapping("/{tableNumber}/session/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
    public ResponseEntity<MessageResponse> getTableStatus(@PathVariable Integer tableNumber) {
        boolean isActive = tableSessionService.isTableActive(tableNumber);
        return ResponseEntity.ok(new MessageResponse(isActive ? "true" : "false"));
    }
}