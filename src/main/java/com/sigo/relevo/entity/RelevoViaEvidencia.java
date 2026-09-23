package com.sigo.relevo.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="relevo_via_evidencias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RelevoViaEvidencia {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="relevo_via_id",nullable=false) private RelevoVia relevoVia;
 @Column(name="url_archivo",nullable=false,length=500) private String urlArchivo;
 @Column(name="public_id",length=255) private String publicId;
 @Column(nullable=false,length=20) private String tipo="foto";
 @Column(name="created_at",insertable=false,updatable=false) private OffsetDateTime createdAt;
}
