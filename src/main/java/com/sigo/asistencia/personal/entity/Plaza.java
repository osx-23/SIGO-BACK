package com.sigo.asistencia.personal.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="plazas") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Plaza { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true,length=20) private String codigo; @Column(length=100) private String descripcion; @Column(nullable=false) private Boolean activo=true; }
