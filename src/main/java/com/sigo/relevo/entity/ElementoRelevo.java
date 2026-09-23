package com.sigo.relevo.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="elementos_relevo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ElementoRelevo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=30) private String codigo;
 @Column(nullable=false,length=100) private String nombre;
 @Column(nullable=false,length=30) private String categoria;
 @Column(name="requiere_cantidad",nullable=false) private Boolean requiereCantidad=false;
 @Column(nullable=false) private Boolean activo=true;
 @Column(nullable=false) private Integer orden=0;
 @Column(name="created_at",insertable=false,updatable=false) private OffsetDateTime createdAt;
}
