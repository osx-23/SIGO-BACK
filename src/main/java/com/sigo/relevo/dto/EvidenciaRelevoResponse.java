package com.sigo.relevo.dto;
import java.time.OffsetDateTime;
public record EvidenciaRelevoResponse(Long id,String urlArchivo,String publicId,String tipo,OffsetDateTime createdAt) {}
