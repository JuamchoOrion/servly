package co.edu.uniquindio.servly.exception;

/**
 * Excepción lanzada cuando se intenta abrir una sesión en una mesa que está ocupada.
 * HTTP Status: 409 Conflict
 */
public class TableOccupiedException extends RuntimeException {

    private final Integer tableNumber;
    private final String status;

    public TableOccupiedException(Integer tableNumber, String status) {
        super(String.format("La mesa número %d no está disponible (estado: %s). No se puede abrir una sesión.", tableNumber, status));
        this.tableNumber = tableNumber;
        this.status = status;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public String getStatus() {
        return status;
    }
}

