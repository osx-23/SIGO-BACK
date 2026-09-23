package com.sigo.asistencia.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="motivos_ausencia") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MotivoAusencia { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true,length=60) private String nombre; }
