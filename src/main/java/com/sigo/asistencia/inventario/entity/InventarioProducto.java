package com.sigo.asistencia.inventario.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="inventario_producto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioProducto {
  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="inventario_producto_seq")
  @SequenceGenerator(name="inventario_producto_seq",sequenceName="inventario_producto_id_seq",allocationSize=1)
  private Long id;
  @Column(nullable=false,unique=true,length=60) private String codigo;
  @Column(nullable=false,length=150) private String nombre;
  @Column(length=350) private String descripcion;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="categoria_id") private InventarioCategoria categoria;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="ambito_id") private InventarioAmbito ambito;
  @Column(name="unidad_medida",nullable=false,length=50) private String unidadMedida;
  @Column(nullable=false) private Boolean activo=true;
  @Column(name="creado_en",nullable=false) private OffsetDateTime creadoEn;
  @Column(name="actualizado_en",nullable=false) private OffsetDateTime actualizadoEn;
  @PrePersist void prePersist(){ var n=OffsetDateTime.now(); if(creadoEn==null) creadoEn=n; if(actualizadoEn==null) actualizadoEn=n; if(activo==null) activo=true; }
  @PreUpdate void preUpdate(){ actualizadoEn=OffsetDateTime.now(); }
}
