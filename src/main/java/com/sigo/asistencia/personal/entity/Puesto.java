package com.sigo.asistencia.personal.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="puestos") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Puesto { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true,length=60) private String nombre; }
