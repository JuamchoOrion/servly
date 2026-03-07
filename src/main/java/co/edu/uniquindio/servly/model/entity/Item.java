package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
    //hacerlo escalable con otra clase puede ser
    @Column(nullable = false)
    private String unitOfMeasurement; // kg, unit, liters

    private Integer expirationDays;

    // Categoría del ítem (p. ej. alimentos, limpieza, oficina)
    @Column(nullable = false)
    private String category;
}
