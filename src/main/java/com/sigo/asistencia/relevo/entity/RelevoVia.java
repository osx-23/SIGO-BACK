package com.sigo.asistencia.relevo.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="relevo_vias", uniqueConstraints=@UniqueConstraint(columnNames={"relevo_id","via_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RelevoVia {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="relevo_id",nullable=false) private Relevo relevo;
 @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="via_id",nullable=false) private Via via;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EstadoOperativo estado;
 @Column(columnDefinition="text") private String detalle;
 @Column(name="created_at",insertable=false,updatable=false) private OffsetDateTime createdAt;
}
