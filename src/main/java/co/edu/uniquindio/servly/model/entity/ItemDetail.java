package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "itema_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 255)
    private String annotation;

    @Column(nullable = false)
    private Boolean isOptional;

    // Variaciones de cantidad si isOptional = true
    @Column
    private Integer minQuantity; // Mínimo que puede seleccionar (default = 0 si opcional)

    @Column
    private Integer maxQuantity; // Máximo que puede seleccionar

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;
}