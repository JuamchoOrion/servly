package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("TABLE")
@Getter
@Setter
public class TableSource extends OrderSource {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "restaurant_table_id", nullable = false)
    private RestaurantTable restaurantTable;

    @Column(name = "table_number", nullable = false)
    private Integer tableNumber;
}