package com.sigo.asistencia.api.dto; import java.math.BigDecimal; public record ResumenAsistenciaResponse(Long registros,Long presentes,Long programados,Long ausentes,BigDecimal porcentajeGeneral){}
