package com.sigo.inventario.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@Entity
@Table(name="inventario_conteo_detalle",uniqueConstraints=@UniqueConstraint(name="inventario_conteo_detalle_uniq",columnNames={"inventario_id","producto_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioConteoDetalle {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="inventario_id",nullable=false) private InventarioConteo inventario;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="producto_id",nullable=false) private InventarioProducto producto;
  @Column(name="cantidad_encontrada",nullable=false,precision=14,scale=2) private BigDecimal cantidadEncontrada;
  @Column(name="nombre_producto_snapshot",nullable=false,length=150) private String nombreProductoSnapshot;
  @Column(name="unidad_snapshot",nullable=false,length=50) private String unidadSnapshot;
  @Column(name="categoria_snapshot",length=120) private String categoriaSnapshot;
  @Column(name="ambito_snapshot",length=120) private String ambitoSnapshot;
  @Column(name="creado_en",nullable=false) private OffsetDateTime creadoEn;
  @PrePersist void prePersist(){ if(creadoEn==null) creadoEn=OffsetDateTime.now(); }
}
