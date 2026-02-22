package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("TABLE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableSource extends OrderSource {

    @Column(nullable = false)
    private boolean occupied;

    private LocalDateTime occupiedSince;
}