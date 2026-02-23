package co.edu.uniquindio.servly.model.enums;

/**
 * Proveedor de autenticación del usuario.
 *
 * LOCAL  → registro con email y contraseña
 * GOOGLE → login mediante OAuth2 con Google
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}

