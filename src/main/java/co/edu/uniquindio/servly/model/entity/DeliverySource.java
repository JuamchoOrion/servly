package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("DELIVERY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySource extends OrderSource {

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String clientName;
}