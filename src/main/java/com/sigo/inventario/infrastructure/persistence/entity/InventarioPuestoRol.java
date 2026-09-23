package com.sigo.inventario.infrastructure.persistence.entity;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="inventario_puesto_rol")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioPuestoRol {
  @Id @Column(name="puesto_id") private Long puestoId;
  @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="puesto_id",insertable=false,updatable=false) private Puesto puesto;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="rol_id",nullable=false) private InventarioRol rol;
}
