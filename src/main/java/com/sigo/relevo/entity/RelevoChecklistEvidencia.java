package com.sigo.relevo.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="relevo_checklist_evidencias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RelevoChecklistEvidencia {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="checklist_id",nullable=false) private RelevoChecklist checklist;
 @Column(name="url_archivo",nullable=false,length=500) private String urlArchivo;
 @Column(name="public_id",length=255) private String publicId;
 @Column(nullable=false,length=20) private String tipo="foto";
 @Column(name="created_at",insertable=false,updatable=false) private OffsetDateTime createdAt;
}
