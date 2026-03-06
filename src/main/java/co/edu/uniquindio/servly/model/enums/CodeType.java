package co.edu.uniquindio.servly.model.enums;

/**
 * Tipo de código OTP almacenado en la base de datos.
 *
 * TWO_FACTOR     → código enviado al correo para completar el login con 2FA
 * PASSWORD_RESET → código enviado al correo para recuperar la contraseña
 */
public enum CodeType {
    TWO_FACTOR,
    PASSWORD_RESET
}
