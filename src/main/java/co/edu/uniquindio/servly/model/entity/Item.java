package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String unitOfMeasurement; // kg, unit, liters

    private Integer expirationDays;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // Relación con ItemCategory
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_category_id", nullable = false)
    private ItemCategory itemCategory;

    // Nuevo campo: ideal stock (no nulleable)
    @Column(name = "ideal_stock", nullable = false)
    @Builder.Default
    private Integer idealStock = 0;
}
