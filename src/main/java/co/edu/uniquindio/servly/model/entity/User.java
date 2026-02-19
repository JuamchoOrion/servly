package co.edu.uniquindio.servly.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String last_name;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    private String address;

}
