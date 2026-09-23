package com.sigo.inventario.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="inventario_categoria")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioCategoria {
  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="inventario_categoria_seq")
  @SequenceGenerator(name="inventario_categoria_seq",sequenceName="inventario_categoria_id_seq",allocationSize=1)
  private Long id;
  @Column(nullable=false,unique=true,length=120) private String nombre;
  @Column(length=250) private String descripcion;
  @Column(nullable=false) private Boolean activo=true;
}
