package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.DTO.RestaurantTableDTO;
import co.edu.uniquindio.servly.model.entity.RestaurantTable;
import co.edu.uniquindio.servly.service.RestaurantTableService;
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
    private final RestaurantTableService restaurantTableService;

    // ============ LISTAR Y OBTENER MESAS ============

    /**
     * WAITER/CASHIER: Obtener todas las mesas
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
    public ResponseEntity<List<RestaurantTableDTO>> getAllTables() {
        List<RestaurantTable> tables = restaurantTableService.getAllTables();
        List<RestaurantTableDTO> dtos = tables.stream()
                .map(RestaurantTableDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * WAITER/CASHIER: Obtener una mesa específica
     */
    @GetMapping("/{tableNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
    public ResponseEntity<RestaurantTableDTO> getTable(@PathVariable Integer tableNumber) {
        RestaurantTable table = restaurantTableService.getTableByNumber(tableNumber);
        return ResponseEntity.ok(RestaurantTableDTO.fromEntity(table));
    }

    // ============ OPERACIONES DE SESIÓN ============

    /**
     * WAITER/CASHIER: Cerrar sesión de una mesa
     */
    @DeleteMapping("/{tableNumber}/session")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
    public MessageResponse closeTableSession(@PathVariable Integer tableNumber) {
        return tableSessionService.closeSession(tableNumber);
    }

    /**
     * WAITER/CASHIER: Ver si la mesa está activa
     */
    @GetMapping("/{tableNumber}/session/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
    public MessageResponse getTableStatus(@PathVariable Integer tableNumber) {
        return new MessageResponse(
                tableSessionService.isTableActive(tableNumber) ? "true" : "false");
    }

    // ============ OPERACIONES DE ADMINISTRACIÓN (ADMIN ONLY) ============

    /**
     * ADMIN: Crear una nueva mesa
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantTableDTO> createTable(
            @RequestParam Integer tableNumber,
            @RequestParam Integer capacity,
            @RequestParam(required = false) String location) {

        RestaurantTable table = restaurantTableService.createTable(tableNumber, capacity, location);
        return ResponseEntity.ok(RestaurantTableDTO.fromEntity(table));
    }

    /**
     * ADMIN: Actualizar estado de una mesa
     */
    @PatchMapping("/{tableNumber}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantTableDTO> updateTableStatus(
            @PathVariable Integer tableNumber,
            @RequestParam RestaurantTable.TableStatus status) {

        RestaurantTable table = restaurantTableService.updateTableStatus(tableNumber, status);
        return ResponseEntity.ok(RestaurantTableDTO.fromEntity(table));
    }

    /**
     * ADMIN: Obtener mesas por estado
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RestaurantTableDTO>> getTablesByStatus(
            @PathVariable RestaurantTable.TableStatus status) {

        List<RestaurantTable> tables = restaurantTableService.getTablesByStatus(status);
        List<RestaurantTableDTO> dtos = tables.stream()
                .map(RestaurantTableDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * ADMIN: Eliminar una mesa
     */
    @DeleteMapping("/{tableNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteTable(
            @PathVariable Integer tableNumber) {

        restaurantTableService.deleteTable(tableNumber);
        return ResponseEntity.ok(new MessageResponse("Mesa " + tableNumber + " eliminada correctamente"));
    }
}