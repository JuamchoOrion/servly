package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Representa una mesa física del restaurante.
 *
 * Una mesa puede tener múltiples sesiones a lo largo del tiempo,
 * pero solo UNA sesión activa simultaneamente.
 *
 * Flujo:
 *  1. Admin crea mesas en el sistema (RestaurantTable)
 *  2. Cliente escanea QR de la mesa → se crea TableSession
 *  3. TableSession referencia a RestaurantTable
 *  4. Cuando hay una orden desde mesa → OrderSource.TABLE referencia a RestaurantTable
 */
@Entity
@Table(
        name = "restaurant_tables",
        indexes = {
                @Index(name = "idx_restaurant_table_number", columnList = "table_number", unique = true),
                @Index(name = "idx_restaurant_table_status", columnList = "status")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "table_number", nullable = false, unique = true)
    private Integer tableNumber;

    @Column(nullable = false)
    private Integer capacity; // Cantidad de personas que puede acomodar

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(length = 255)
    private String location; // Ejemplo: "Piso 1, Esquina", "Terraza", etc.

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Estados posibles de una mesa
     */
    public enum TableStatus {
        AVAILABLE,      // Mesa disponible
        OCCUPIED,       // Hay clientes usando la mesa
        MAINTENANCE,    // Mesa en mantenimiento
        RESERVED        // Mesa reservada
    }
}

