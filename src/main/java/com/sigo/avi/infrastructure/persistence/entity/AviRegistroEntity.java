package com.sigo.avi.infrastructure.persistence.entity;

import com.sigo.avi.domain.AviAccion;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "avi_registros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AviRegistroEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Trabajador usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plaza_id", nullable = false)
    private Plaza plaza;

    @Column(nullable = false, length = 15)
    private String placa;

    @Column(nullable = false)
    private Integer via;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AviAccion accion;

    @Column(name = "fecha_hora_evento", nullable = false)
    private OffsetDateTime fechaHoraEvento;

    @Column(
            name = "fecha_hora_recepcion",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "timestamp with time zone default now()"
    )
    private OffsetDateTime fechaHoraRecepcion;

    @Column(name = "texto_reconocido", columnDefinition = "text")
    private String textoReconocido;
}
