package com.sigo.inventario.entity;
import com.sigo.personal.entity.Plaza;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="inventario_producto_plaza")
@IdClass(InventarioProductoPlazaId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioProductoPlaza {
  @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="producto_id",nullable=false) private InventarioProducto producto;
  @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="plaza_id",nullable=false) private Plaza plaza;
  @Column(name="stock_minimo",nullable=false) private Integer stockMinimo=0;
}
