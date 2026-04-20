package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.TableSessionResponse;
import co.edu.uniquindio.servly.service.TableSessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint de entrada para el cliente anónimo.
 * GET /api/client/session?table={n}
 *   → Público. El QR de la mesa apunta aquí.
 *   → Crea (o recupera) la sesión de la mesa.
 *   → Envía el sessionToken como Cookie HttpOnly (automático, sin localStorage)
 */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class TableSessionController {

    private final TableSessionService tableSessionService;

    @GetMapping("/session")
    public ResponseEntity<TableSessionResponse> openSession(
            @RequestParam Integer table,
            HttpServletResponse response) {

        TableSessionResponse sessionResponse = tableSessionService.openSession(table);

        // Crear cookie HttpOnly para sessionToken
        Cookie sessionCookie = new Cookie("sessionToken", sessionResponse.getSessionToken());
        sessionCookie.setHttpOnly(true);      // Protege contra XSS
        sessionCookie.setSecure(true);        // Solo HTTPS en producción
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(14400);       // 4 horas (mismo que el JWT)
        response.addCookie(sessionCookie);

        // Crear cookie con número de mesa (accesible desde JS si lo necesita)
        Cookie tableCookie = new Cookie("tableNumber", sessionResponse.getTableNumber().toString());
        tableCookie.setSecure(true);
        tableCookie.setPath("/");
        tableCookie.setMaxAge(14400);
        response.addCookie(tableCookie);

        return ResponseEntity.ok(sessionResponse);
    }
}
