package com.sigo.relevo.infrastructure.persistence.entity;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="vias", uniqueConstraints=@UniqueConstraint(columnNames={"plaza_id","numero"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Via {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.EAGER, optional=false) @JoinColumn(name="plaza_id",nullable=false) private Plaza plaza;
 @Column(nullable=false) private Integer numero;
 @Column(length=50) private String nombre;
 @Column(nullable=false) private Boolean activa=true;
 @Column(name="avi_visible", nullable=false, columnDefinition="boolean default true") private Boolean aviVisible=true;
 @Column(nullable=false) private Integer orden=0;
 @Column(name="created_at",insertable=false,updatable=false) private OffsetDateTime createdAt;

 public Via(
         Long id,
         Plaza plaza,
         Integer numero,
         String nombre,
         Boolean activa,
         Integer orden,
         OffsetDateTime createdAt
 ) {
  this.id = id;
  this.plaza = plaza;
  this.numero = numero;
  this.nombre = nombre;
  this.activa = activa;
  this.aviVisible = true;
  this.orden = orden;
  this.createdAt = createdAt;
 }
}
