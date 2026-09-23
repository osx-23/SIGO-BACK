package com.sigo.personal.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trabajadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trabajador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer codigo;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plaza_id")
    private Plaza plaza;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_sistema", nullable = false, length = 30)
    private RolSistema rolSistema = RolSistema.OPERADOR;

    @JsonIgnore
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "requiere_cambio_password", nullable = false)
    private Boolean requiereCambioPassword = true;

    @Column(nullable = false)
    private Boolean activo = true;
}
