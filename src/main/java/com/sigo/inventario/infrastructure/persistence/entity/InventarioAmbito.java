package com.sigo.inventario.infrastructure.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="inventario_ambito")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioAmbito {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false,unique=true,length=60) private String codigo;
  @Column(nullable=false,length=120) private String nombre;
  @Column(length=250) private String descripcion;
  @Column(nullable=false) private Boolean activo=true;
}
