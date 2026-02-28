package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.service.TableSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Operaciones del staff sobre las mesas.
 *
 * DELETE /api/staff/tables/{n}/session         → Cerrar sesión de mesa
 * GET    /api/staff/tables/{n}/session/status  → Ver si la mesa está activa
 *
 * Acceso: ADMIN, CASHIER, WAITER
 */
@RestController
@RequestMapping("/api/staff/tables")
@RequiredArgsConstructor
public class StaffTableController {

    private final TableSessionService tableSessionService;

    @DeleteMapping("/{tableNumber}/session")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
    public MessageResponse closeTableSession(@PathVariable Integer tableNumber) {
        return tableSessionService.closeSession(tableNumber);
    }

    @GetMapping("/{tableNumber}/session/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAITER')")
    public MessageResponse getTableStatus(@PathVariable Integer tableNumber) {
        return new MessageResponse(
                tableSessionService.isTableActive(tableNumber) ? "true" : "false");
    }
}