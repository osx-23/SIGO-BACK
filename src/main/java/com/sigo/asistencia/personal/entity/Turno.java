package com.sigo.asistencia.personal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "turnos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "codigo",
            nullable = false,
            unique = true,
            length = 1
    )
    private String codigo;

    @Column(length = 30)
    private String nombre;
}
