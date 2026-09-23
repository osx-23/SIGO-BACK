package com.sigo.inventario.infrastructure.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="inventario_rol")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioRol {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false,unique=true,length=40) private String codigo;
  @Column(nullable=false,length=100) private String nombre;
  @Column(nullable=false) private Boolean activo=true;
}
