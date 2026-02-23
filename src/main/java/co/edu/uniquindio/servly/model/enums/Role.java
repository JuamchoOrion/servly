package co.edu.uniquindio.servly.model.enums;
/**
 * Roles del sistema Servly.
 *
 * Roles con autenticación JWT:
 *  - ADMIN       → acceso total
 *  - CASHIER     → pagos y facturación
 *  - WAITER     → pedidos en sala
 *  - KITCHEN     → visualización y estado de pedidos
 *  - STOREKEEPER → inventario y bodega
 *
 * Rol sin autenticación:
 *  - CLIENT → accede escaneando el QR de su mesa (sesión anónima de mesa).
 *              No tiene cuenta en la tabla users, no requiere login.
 */
public enum Role {
    ADMIN,
    WAITER,
    CASHIER,
    KITCHEN,
    STOREKEEPER,
    CLIENT
}
