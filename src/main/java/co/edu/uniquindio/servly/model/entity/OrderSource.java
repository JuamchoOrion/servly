package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "source_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class OrderSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}