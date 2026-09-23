package com.sigo.asistencia.asistencia.service;

import com.sigo.asistencia.asistencia.dto.AusenciaMotivoResponse;
import com.sigo.asistencia.asistencia.dto.DashboardPuntoResponse;
import com.sigo.asistencia.asistencia.dto.ResumenAsistenciaResponse;

import com.sigo.asistencia.asistencia.repository.AsistenciaAusenciaRepository;
import com.sigo.asistencia.asistencia.repository.AsistenciaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardAsistenciaService {

 private final AsistenciaRepository asistenciaRepository;
 private final AsistenciaAusenciaRepository ausenciaRepository;

 /*
  * ============================================================
  * ASISTENCIA DIARIA
  * ============================================================
  */

 @Transactional(readOnly = true)
 public List<DashboardPuntoResponse> diario(
         int anio,
         int mes,
         Long plazaId,
         Long turnoId
 ) {

  validarAnio(anio);
  validarMes(mes);

  YearMonth periodo =
          YearMonth.of(
                  anio,
                  mes
          );

  LocalDate inicio =
          periodo.atDay(1);

  LocalDate fin =
          periodo.atEndOfMonth();

  List<Object[]> resultados =
          asistenciaRepository.obtenerDiario(
                  inicio,
                  fin,
                  plazaId,
                  turnoId
          );

  return resultados
          .stream()
          .map(this::convertirPunto)
          .toList();
 }

 /*
  * ============================================================
  * ASISTENCIA ANUAL
  * ============================================================
  */

 @Transactional(readOnly = true)
 public List<DashboardPuntoResponse> anual(
         int anio,
         Long plazaId,
         Long turnoId
 ) {

  validarAnio(anio);

  LocalDate inicio =
          LocalDate.of(
                  anio,
                  1,
                  1
          );

  LocalDate fin =
          LocalDate.of(
                  anio,
                  12,
                  31
          );

  List<Object[]> resultados =
          asistenciaRepository.obtenerAnual(
                  inicio,
                  fin,
                  plazaId,
                  turnoId
          );

  return resultados
          .stream()
          .map(this::convertirPunto)
          .toList();
 }

 /*
  * ============================================================
  * AUSENCIAS POR MOTIVO
  * ============================================================
  */

 @Transactional(readOnly = true)
 public List<AusenciaMotivoResponse> motivos(
         int anio,
         Integer mes,
         Long plazaId,
         Long turnoId
 ) {

  validarAnio(anio);

  LocalDate inicio;
  LocalDate fin;

  /*
   * Si viene mes:
   * consultamos solamente ese mes.
   *
   * Si no viene mes:
   * consultamos todo el año.
   */
  if (mes != null) {

   validarMes(mes);

   YearMonth periodo =
           YearMonth.of(
                   anio,
                   mes
           );

   inicio =
           periodo.atDay(1);

   fin =
           periodo.atEndOfMonth();

  } else {

   inicio =
           LocalDate.of(
                   anio,
                   1,
                   1
           );

   fin =
           LocalDate.of(
                   anio,
                   12,
                   31
           );
  }

  List<Object[]> resultados =
          ausenciaRepository.contarPorMotivo(
                  inicio,
                  fin,
                  plazaId,
                  turnoId
          );

  return resultados
          .stream()
          .map(
                  r ->
                          new AusenciaMotivoResponse(
                                  String.valueOf(
                                          r[0]
                                  ),
                                  numero(
                                          r[1]
                                  ).longValue()
                          )
          )
          .toList();
 }

 /*
  * ============================================================
  * RESUMEN
  * ============================================================
  *
  * Esta es la corrección importante para el error 500.
  *
  * El repository devuelve List<Object[]>.
  * Tomamos explícitamente la primera fila.
  */

 @Transactional(readOnly = true)
 public ResumenAsistenciaResponse resumen(
         LocalDate inicio,
         LocalDate fin,
         Long plazaId,
         Long turnoId
 ) {

  validarRango(
          inicio,
          fin
  );

  List<Object[]> resultado =
          asistenciaRepository.obtenerResumen(
                  inicio,
                  fin,
                  plazaId,
                  turnoId
          );

  /*
   * Aunque una consulta con COUNT/SUM
   * debería devolver siempre una fila,
   * dejamos esta validación por seguridad.
   */
  if (
          resultado == null ||
                  resultado.isEmpty()
  ) {

   return new ResumenAsistenciaResponse(
           0L,
           0L,
           0L,
           0L,
           BigDecimal.ZERO
   );
  }

  Object[] r =
          resultado.get(0);

  long registros =
          numero(
                  valor(
                          r,
                          0
                  )
          ).longValue();

  long presentes =
          numero(
                  valor(
                          r,
                          1
                  )
          ).longValue();

  long programados =
          numero(
                  valor(
                          r,
                          2
                  )
          ).longValue();

  long ausentes =
          numero(
                  valor(
                          r,
                          3
                  )
          ).longValue();

  BigDecimal porcentaje =
          calcularPorcentaje(
                  presentes,
                  programados
          );

  return new ResumenAsistenciaResponse(
          registros,
          presentes,
          programados,
          ausentes,
          porcentaje
  );
 }

 /*
  * ============================================================
  * CONVERTIR RESULTADO SQL A DTO
  * ============================================================
  */

 private DashboardPuntoResponse convertirPunto(
         Object[] r
 ) {

  int periodo =
          numero(
                  valor(
                          r,
                          0
                  )
          ).intValue();

  long presentes =
          numero(
                  valor(
                          r,
                          1
                  )
          ).longValue();

  long programados =
          numero(
                  valor(
                          r,
                          2
                  )
          ).longValue();

  BigDecimal porcentaje =
          decimal(
                  valor(
                          r,
                          3
                  )
          );

  return new DashboardPuntoResponse(
          periodo,
          presentes,
          programados,
          porcentaje
  );
 }

 /*
  * ============================================================
  * PORCENTAJE
  * ============================================================
  */

 private BigDecimal calcularPorcentaje(
         long presentes,
         long programados
 ) {

  if (programados <= 0) {

   return BigDecimal.ZERO;
  }

  return BigDecimal
          .valueOf(
                  presentes
          )
          .multiply(
                  BigDecimal.valueOf(
                          100
                  )
          )
          .divide(
                  BigDecimal.valueOf(
                          programados
                  ),
                  2,
                  RoundingMode.HALF_UP
          );
 }

 /*
  * ============================================================
  * UTILIDADES PARA RESULTADOS SQL
  * ============================================================
  */

 private Object valor(
         Object[] fila,
         int indice
 ) {

  if (
          fila == null ||
                  indice < 0 ||
                  indice >= fila.length
  ) {

   return null;
  }

  return fila[indice];
 }

 private Number numero(
         Object valor
 ) {

  if (valor == null) {

   return 0;
  }

  if (
          valor instanceof Number numero
  ) {

   return numero;
  }

  try {

   return new BigDecimal(
           valor.toString()
   );

  } catch (
          NumberFormatException e
  ) {

   return 0;
  }
 }

 private BigDecimal decimal(
         Object valor
 ) {

  if (valor == null) {

   return BigDecimal.ZERO;
  }

  if (
          valor instanceof BigDecimal decimal
  ) {

   return decimal;
  }

  if (
          valor instanceof Number numero
  ) {

   return new BigDecimal(
           numero.toString()
   );
  }

  try {

   return new BigDecimal(
           valor.toString()
   );

  } catch (
          NumberFormatException e
  ) {

   return BigDecimal.ZERO;
  }
 }

 /*
  * ============================================================
  * VALIDACIONES
  * ============================================================
  */

 private void validarMes(
         int mes
 ) {

  if (
          mes < 1 ||
                  mes > 12
  ) {

   throw new IllegalArgumentException(
           "El mes debe estar entre 1 y 12"
   );
  }
 }

 private void validarAnio(
         int anio
 ) {

  if (
          anio < 2000 ||
                  anio > 2100
  ) {

   throw new IllegalArgumentException(
           "El año indicado no es válido"
   );
  }
 }

 private void validarRango(
         LocalDate inicio,
         LocalDate fin
 ) {

  if (
          inicio == null ||
                  fin == null
  ) {

   throw new IllegalArgumentException(
           "Las fechas inicio y fin son obligatorias"
   );
  }

  if (
          inicio.isAfter(
                  fin
          )
  ) {

   throw new IllegalArgumentException(
           "La fecha de inicio no puede ser posterior a la fecha fin"
   );
  }
 }
}