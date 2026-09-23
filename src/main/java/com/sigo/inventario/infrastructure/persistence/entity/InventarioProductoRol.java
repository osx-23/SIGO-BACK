package com.sigo.inventario.infrastructure.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="inventario_producto_rol")
@IdClass(InventarioProductoRolId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioProductoRol {
  @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="producto_id",nullable=false) private InventarioProducto producto;
  @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="rol_id",nullable=false) private InventarioRol rol;
}
