package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("TABLE")
@Getter
@Setter
public class TableSource extends OrderSource {
    @Column(nullable = false, unique = true)
    private Integer tableNumber;
}