package co.edu.uniquindio.servly.util;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

public class CookieUtil {
    /**
     * Agrega una cookie HTTP-only con el token JWT
     * @param response HttpServletResponse para agregar el header Set-Cookie
     * @param token El token JWT a guardar
     * @param maxAgeSeconds Tiempo de vida en segundos (ej: 86400 = 24h)
     */
    public void addJwtCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        response.addHeader("Set-Cookie", String.format(
                "accessToken=%s; Path=/; Max-Age=%d; HttpOnly; Secure=false; SameSite=Lax",
                token,
                maxAgeSeconds
        ));
    }

    /**
     * Agrega una cookie para el refresh token
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        response.addHeader("Set-Cookie", String.format(
                "refreshToken=%s; Path=/; Max-Age=%d; HttpOnly; Secure=false; SameSite=Lax",
                token,
                maxAgeSeconds
        ));
    }
    /**
     * Elimina las cookies de los tokens
     */
    public void removeJwtCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", "accessToken=; Path=/; Max-Age=0; HttpOnly; Secure=false; SameSite=Lax");
        response.addHeader("Set-Cookie", "refreshToken=; Path=/; Max-Age=0; HttpOnly; Secure=false; SameSite=Lax");
    }

}
