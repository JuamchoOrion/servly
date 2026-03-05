package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.TableSessionResponse;
import co.edu.uniquindio.servly.service.TableSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint de entrada para el cliente anónimo.
 * GET /api/client/session?table={n}
 *   → Público. El QR de la mesa apunta aquí.
 *   → Crea (o recupera) la sesión de la mesa y retorna el sessionToken.
 * El cliente guarda el sessionToken y lo envía en todas sus peticiones
 * a /api/client/** como:  Authorization: Bearer <sessionToken>
 */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class TableSessionController {

    private final TableSessionService tableSessionService;

    @GetMapping("/session")
    public TableSessionResponse openSession(@RequestParam Integer table) {
        return tableSessionService.openSession(table);
    }
}
