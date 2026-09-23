package com.sigo.asistencia.relevo.entity;
import com.sigo.asistencia.personal.entity.Plaza;
import com.sigo.asistencia.personal.entity.Trabajador;
import com.sigo.asistencia.personal.entity.Turno;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name="relevos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Relevo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.EAGER, optional=false) @JoinColumn(name="plaza_id",nullable=false) private Plaza plaza;
 @ManyToOne(fetch=FetchType.EAGER, optional=false) @JoinColumn(name="turno_id",nullable=false) private Turno turno;
 @ManyToOne(fetch=FetchType.EAGER, optional=false) @JoinColumn(name="operador_id",nullable=false) private Trabajador operador;
 @Column(nullable=false) private LocalDate fecha;
 @Column(nullable=false) private LocalTime hora;
 @Column(columnDefinition="text") private String observaciones;
 @Column(columnDefinition="text") private String resumen;
 @Column(name="created_at",insertable=false,updatable=false) private OffsetDateTime createdAt;
 @Column(name="updated_at",insertable=false,updatable=false) private OffsetDateTime updatedAt;
}
