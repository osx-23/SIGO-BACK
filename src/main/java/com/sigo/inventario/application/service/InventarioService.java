package com.sigo.inventario.application.service;

import com.sigo.inventario.api.dto.request.AnularInventarioRequest;
import com.sigo.inventario.api.dto.request.GuardarConteoRequest;
import com.sigo.inventario.api.dto.response.InventarioDetalleItemResponse;
import com.sigo.inventario.api.dto.response.InventarioDetalleResponse;
import com.sigo.inventario.api.dto.response.InventarioResumenResponse;
import com.sigo.inventario.api.dto.response.ProductoInventarioResponse;
import com.sigo.inventario.infrastructure.persistence.entity.EstadoInventario;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteo;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteoDetalle;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProducto;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoDetalleRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioProductoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioRolRepository;
import com.sigo.inventario.application.security.InventarioAuthorizationService;
import com.sigo.inventario.application.security.InventarioUsuarioActual;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventarioService {

  private final InventarioConteoRepository conteos;
  private final InventarioConteoDetalleRepository detalles;
  private final InventarioProductoRepository productos;
  private final InventarioRolRepository roles;
  private final InventarioAuthorizationService auth;
  private final InventarioAuditoriaService auditoria;


  // ============================================================
  // INICIAR INVENTARIO
  // ============================================================

  @Transactional
  public InventarioResumenResponse iniciar(
          InventarioUsuarioActual usuario
  ) {

    auth.exigirPlazaAsignada(usuario);

    Page<InventarioConteo> abiertos =
            conteos.buscarEnProceso(
                    usuario.trabajadorId(),
                    PageRequest.of(0, 1)
            );

    if (abiertos.hasContent()) {

      Long inventarioId =
              abiertos.getContent()
                      .get(0)
                      .getId();

      throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "Ya existe un inventario EN_PROCESO: "
                      + inventarioId
      );
    }

    InventarioConteo inventario =
            new InventarioConteo();

    inventario.setPlaza(
            usuario.trabajador().getPlaza()
    );

    inventario.setResponsable(
            usuario.trabajador()
    );

    inventario.setRol(
            usuario.rol()
    );

    inventario.setEstado(
            EstadoInventario.EN_PROCESO
    );

    inventario =
            conteos.save(inventario);

    auditoria.registrar(
            usuario.trabajador(),
            "INVENTARIO_INICIADO",
            "INVENTARIO_CONTEO",
            inventario.getId(),
            Map.of(
                    "plaza",
                    inventario
                            .getPlaza()
                            .getCodigo(),
                    "rol",
                    usuario.rolCodigo()
            )
    );

    return resumen(inventario);
  }


  // ============================================================
  // PRODUCTOS PERMITIDOS
  // ============================================================

  @Transactional(readOnly = true)
  public List<ProductoInventarioResponse> productosPermitidos(
          InventarioUsuarioActual usuario,
          Long inventarioId
  ) {

    InventarioConteo inventario =
            obtener(inventarioId);

    auth.exigirPuedeModificarConteo(
            usuario,
            inventario
    );

    if (inventario.getEstado()
            != EstadoInventario.EN_PROCESO) {

      throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "Inventario cerrado"
      );
    }

    return productos
            .buscarPermitidos(
                    inventario
                            .getRol()
                            .getId(),

                    inventario
                            .getPlaza()
                            .getId()
            )
            .stream()
            .map(this::producto)
            .toList();
  }


  // ============================================================
  // GUARDAR CONTEO
  // ============================================================

  @Transactional
  public InventarioDetalleResponse guardarDetalle(
          InventarioUsuarioActual usuario,
          Long inventarioId,
          GuardarConteoRequest request
  ) {

    InventarioConteo inventario =
            obtener(inventarioId);

    auth.exigirPuedeModificarConteo(
            usuario,
            inventario
    );

    if (inventario.getEstado()
            != EstadoInventario.EN_PROCESO) {

      throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "Solo se puede editar un inventario EN_PROCESO"
      );
    }

    Set<Long> productosRecibidos =
            new HashSet<>();

    for (var item : request.productos()) {

      if (!productosRecibidos.add(
              item.productoId()
      )) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Producto repetido"
        );
      }

      auth.exigirProductoVisible(
              usuario,
              item.productoId()
      );

      if (item.cantidad() == null) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La cantidad es obligatoria"
        );
      }

      if (item.cantidad()
              .compareTo(BigDecimal.ZERO) < 0) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La cantidad no puede ser negativa"
        );
      }

      InventarioProducto producto =
              productos
                      .findById(
                              item.productoId()
                      )
                      .orElseThrow(
                              () ->
                                      new ResponseStatusException(
                                              HttpStatus.NOT_FOUND,
                                              "Producto no encontrado"
                                      )
                      );

      InventarioConteoDetalle detalle =
              detalles
                      .findByInventarioIdAndProductoId(
                              inventarioId,
                              item.productoId()
                      )
                      .orElseGet(
                              InventarioConteoDetalle::new
                      );

      detalle.setInventario(
              inventario
      );

      detalle.setProducto(
              producto
      );

      detalle.setCantidadEncontrada(
              item.cantidad()
      );

      detalle.setNombreProductoSnapshot(
              producto.getNombre()
      );

      detalle.setUnidadSnapshot(
              producto.getUnidadMedida()
      );

      detalle.setCategoriaSnapshot(
              producto.getCategoria() == null
                      ? null
                      : producto
                      .getCategoria()
                      .getNombre()
      );

      detalle.setAmbitoSnapshot(
              producto.getAmbito() == null
                      ? null
                      : producto
                      .getAmbito()
                      .getNombre()
      );

      detalles.save(detalle);
    }

    auditoria.registrar(
            usuario.trabajador(),
            "CONTEO_GUARDADO",
            "INVENTARIO_CONTEO",
            inventarioId,
            Map.of(
                    "productosRecibidos",
                    request.productos().size()
            )
    );

    return detalle(
            usuario,
            inventarioId
    );
  }


  // ============================================================
  // FINALIZAR INVENTARIO
  // ============================================================

  @Transactional
  public InventarioDetalleResponse finalizar(
          InventarioUsuarioActual usuario,
          Long inventarioId
  ) {

    InventarioConteo inventario =
            obtener(inventarioId);

    auth.exigirPuedeModificarConteo(
            usuario,
            inventario
    );

    if (inventario.getEstado()
            != EstadoInventario.EN_PROCESO) {

      throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "Inventario ya cerrado"
      );
    }

    List<InventarioProducto> permitidos =
            productos.buscarPermitidos(
                    inventario
                            .getRol()
                            .getId(),

                    inventario
                            .getPlaza()
                            .getId()
            );

    List<InventarioConteoDetalle> detallesInventario =
            detalles
                    .findByInventarioIdOrderByNombreProductoSnapshotAsc(
                            inventarioId
                    );

    Set<Long> productosContados =
            detallesInventario
                    .stream()
                    .map(
                            detalle ->
                                    detalle
                                            .getProducto()
                                            .getId()
                    )
                    .collect(
                            Collectors.toSet()
                    );

    List<String> faltantes =
            permitidos
                    .stream()
                    .filter(
                            producto ->
                                    !productosContados
                                            .contains(
                                                    producto.getId()
                                            )
                    )
                    .map(
                            InventarioProducto::getNombre
                    )
                    .toList();

    if (!faltantes.isEmpty()) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Faltan productos por contar: "
                      + String.join(
                      ", ",
                      faltantes
              )
      );
    }

    boolean cantidadInvalida =
            detallesInventario
                    .stream()
                    .anyMatch(
                            detalle ->
                                    detalle
                                            .getCantidadEncontrada()
                                            == null
                                            ||
                                            detalle
                                                    .getCantidadEncontrada()
                                                    .compareTo(
                                                            BigDecimal.ZERO
                                                    ) < 0
                    );

    if (cantidadInvalida) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Existe una cantidad inválida"
      );
    }

    inventario.setEstado(
            EstadoInventario.FINALIZADO
    );

    inventario.setFechaFinalizacion(
            OffsetDateTime.now()
    );

    conteos.save(inventario);

    auditoria.registrar(
            usuario.trabajador(),
            "INVENTARIO_FINALIZADO",
            "INVENTARIO_CONTEO",
            inventarioId,
            Map.of(
                    "productos",
                    detallesInventario.size()
            )
    );

    return toDetalle(
            inventario,
            detallesInventario
    );
  }


  // ============================================================
  // ANULAR INVENTARIO
  // ============================================================

  @Transactional
  public InventarioDetalleResponse anular(
          InventarioUsuarioActual usuario,
          Long inventarioId,
          AnularInventarioRequest request
  ) {

    InventarioConteo inventario =
            obtener(inventarioId);

    boolean supervisor =
            "SUPERVISOR"
                    .equalsIgnoreCase(
                            usuario.rolCodigo()
                    );

    boolean propietarioEnProceso =
            inventario
                    .getResponsable()
                    .getId()
                    .equals(
                            usuario.trabajadorId()
                    )
                    &&
                    inventario.getEstado()
                            == EstadoInventario.EN_PROCESO;

    if (!supervisor
            && !propietarioEnProceso) {

      throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "No puede anular este inventario"
      );
    }

    if (inventario.getEstado()
            == EstadoInventario.ANULADO) {

      throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "El inventario ya está anulado"
      );
    }

    if (request.motivo() == null
            || request.motivo()
            .trim()
            .isBlank()) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "El motivo de anulación es obligatorio"
      );
    }

    String motivo =
            request
                    .motivo()
                    .trim();

    inventario.setEstado(
            EstadoInventario.ANULADO
    );

    inventario.setAnuladoPor(
            usuario.trabajador()
    );

    inventario.setAnuladoEn(
            OffsetDateTime.now()
    );

    inventario.setMotivoAnulacion(
            motivo
    );

    conteos.save(inventario);

    auditoria.registrar(
            usuario.trabajador(),
            "INVENTARIO_ANULADO",
            "INVENTARIO_CONTEO",
            inventarioId,
            Map.of(
                    "motivo",
                    motivo
            )
    );

    return toDetalle(
            inventario,
            detalles
                    .findByInventarioIdOrderByNombreProductoSnapshotAsc(
                            inventarioId
                    )
    );
  }


  // ============================================================
  // DETALLE
  // ============================================================

  @Transactional(readOnly = true)
  public InventarioDetalleResponse detalle(
          InventarioUsuarioActual usuario,
          Long inventarioId
  ) {

    InventarioConteo inventario =
            obtener(inventarioId);

    auth.exigirPuedeConsultarPlaza(
            usuario,
            inventario
                    .getPlaza()
                    .getId()
    );

    List<InventarioConteoDetalle> listaDetalles =
            detalles
                    .findByInventarioIdOrderByNombreProductoSnapshotAsc(
                            inventarioId
                    );

    return toDetalle(
            inventario,
            listaDetalles
    );
  }


  // ============================================================
  // HISTORIAL
  // ============================================================

  @Transactional(readOnly = true)
  public Page<InventarioResumenResponse> historial(
          InventarioUsuarioActual usuario,
          Long plazaId,
          Long responsableId,
          String rol,
          EstadoInventario estado,
          LocalDate desde,
          LocalDate hasta,
          int page,
          int size
  ) {

    /*
     * Supervisor:
     * puede consultar cualquier plaza.
     *
     * Controlador / Agente:
     * quedan restringidos automáticamente
     * a su plaza asignada.
     */

    Long plazaFiltro =
            plazaId;

    if (!"SUPERVISOR"
            .equalsIgnoreCase(
                    usuario.rolCodigo()
            )) {

      auth.exigirPlazaAsignada(
              usuario
      );

      if (plazaId != null
              && !plazaId.equals(
              usuario.plazaId()
      )) {

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No puede consultar inventarios de otra plaza"
        );
      }

      plazaFiltro =
              usuario.plazaId();
    }


    // --------------------------------------------------------
    // Resolver rol
    // --------------------------------------------------------

    Long rolId =
            null;

    if (rol != null
            && !rol.isBlank()) {

      String codigoRol =
              rol
                      .trim()
                      .toUpperCase(
                              Locale.ROOT
                      );

      rolId =
              roles
                      .findByCodigoAndActivoTrue(
                              codigoRol
                      )
                      .map(
                              InventarioRol::getId
                      )
                      .orElseThrow(
                              () ->
                                      new ResponseStatusException(
                                              HttpStatus.BAD_REQUEST,
                                              "Rol inválido"
                                      )
                      );
    }


    // --------------------------------------------------------
    // Fechas Lima
    // --------------------------------------------------------

    ZoneId zonaLima =
            ZoneId.of(
                    "America/Lima"
            );

    OffsetDateTime fechaDesde =
            desde == null
                    ? null
                    : desde
                    .atStartOfDay(
                            zonaLima
                    )
                    .toOffsetDateTime();

    OffsetDateTime fechaHasta =
            hasta == null
                    ? null
                    : hasta
                    .plusDays(1)
                    .atStartOfDay(
                            zonaLima
                    )
                    .minusNanos(1)
                    .toOffsetDateTime();


    // --------------------------------------------------------
    // Specification dinámica
    // --------------------------------------------------------

    Specification<InventarioConteo> spec =
            Specification.where(null);


    if (plazaFiltro != null) {

      Long finalPlazaFiltro =
              plazaFiltro;

      spec =
              spec.and(
                      (
                              root,
                              query,
                              cb
                      ) ->
                              cb.equal(
                                      root
                                              .get("plaza")
                                              .get("id"),
                                      finalPlazaFiltro
                              )
              );
    }


    if (responsableId != null) {

      spec =
              spec.and(
                      (
                              root,
                              query,
                              cb
                      ) ->
                              cb.equal(
                                      root
                                              .get("responsable")
                                              .get("id"),
                                      responsableId
                              )
              );
    }


    if (rolId != null) {

      Long finalRolId =
              rolId;

      spec =
              spec.and(
                      (
                              root,
                              query,
                              cb
                      ) ->
                              cb.equal(
                                      root
                                              .get("rol")
                                              .get("id"),
                                      finalRolId
                              )
              );
    }


    if (estado != null) {

      spec =
              spec.and(
                      (
                              root,
                              query,
                              cb
                      ) ->
                              cb.equal(
                                      root.get("estado"),
                                      estado
                              )
              );
    }


    if (fechaDesde != null) {

      spec =
              spec.and(
                      (
                              root,
                              query,
                              cb
                      ) ->
                              cb.greaterThanOrEqualTo(
                                      root.get(
                                              "fechaInicio"
                                      ),
                                      fechaDesde
                              )
              );
    }


    if (fechaHasta != null) {

      spec =
              spec.and(
                      (
                              root,
                              query,
                              cb
                      ) ->
                              cb.lessThanOrEqualTo(
                                      root.get(
                                              "fechaInicio"
                                      ),
                                      fechaHasta
                              )
              );
    }


    // --------------------------------------------------------
    // Paginación
    // --------------------------------------------------------

    int paginaSegura =
            Math.max(
                    0,
                    page
            );

    int tamanioSeguro =
            Math.min(
                    100,
                    Math.max(
                            1,
                            size
                    )
            );

    Pageable pageable =
            PageRequest.of(
                    paginaSegura,
                    tamanioSeguro,
                    org.springframework.data.domain.Sort
                            .by(
                                    org.springframework.data.domain.Sort.Direction.DESC,
                                    "fechaInicio"
                            )
            );


    // --------------------------------------------------------
    // Consulta
    // --------------------------------------------------------

    return conteos
            .findAll(
                    spec,
                    pageable
            )
            .map(
                    this::resumen
            );
  }


  // ============================================================
  // OBTENER INVENTARIO
  // ============================================================

  private InventarioConteo obtener(
          Long inventarioId
  ) {

    return conteos
            .findConDetalleById(
                    inventarioId
            )
            .orElseThrow(
                    () ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Inventario no encontrado"
                            )
            );
  }


  // ============================================================
  // RESUMEN
  // ============================================================

  private InventarioResumenResponse resumen(
          InventarioConteo inventario
  ) {

    long cantidadProductos =
            detalles.countByInventarioId(
                    inventario.getId()
            );

    return new InventarioResumenResponse(
            inventario.getId(),

            inventario
                    .getPlaza()
                    .getId(),

            inventario
                    .getPlaza()
                    .getCodigo(),

            inventario
                    .getResponsable()
                    .getId(),

            inventario
                    .getResponsable()
                    .getCodigo(),

            inventario
                    .getResponsable()
                    .getNombreCompleto(),

            inventario
                    .getRol()
                    .getCodigo(),

            inventario
                    .getFechaInicio(),

            inventario
                    .getFechaFinalizacion(),

            inventario
                    .getEstado(),

            cantidadProductos
    );
  }


  // ============================================================
  // PRODUCTO RESPONSE
  // ============================================================

  private ProductoInventarioResponse producto(
          InventarioProducto producto
  ) {

    return new ProductoInventarioResponse(
            producto.getId(),
            producto.getCodigo(),
            producto.getNombre(),
            producto.getDescripcion(),
            producto.getUnidadMedida(),

            producto.getCategoria() == null
                    ? null
                    : producto
                    .getCategoria()
                    .getId(),

            producto.getCategoria() == null
                    ? null
                    : producto
                    .getCategoria()
                    .getNombre(),

            producto.getAmbito() == null
                    ? null
                    : producto
                    .getAmbito()
                    .getId(),

            producto.getAmbito() == null
                    ? null
                    : producto
                    .getAmbito()
                    .getNombre()
    );
  }


  // ============================================================
  // DETALLE RESPONSE
  // ============================================================

  private InventarioDetalleResponse toDetalle(
          InventarioConteo inventario,
          List<InventarioConteoDetalle> listaDetalles
  ) {

    List<InventarioDetalleItemResponse> items =
            listaDetalles
                    .stream()
                    .map(
                            detalle ->
                                    new InventarioDetalleItemResponse(

                                            detalle
                                                    .getProducto()
                                                    .getId(),

                                            detalle
                                                    .getNombreProductoSnapshot(),

                                            detalle
                                                    .getUnidadSnapshot(),

                                            detalle
                                                    .getCategoriaSnapshot(),

                                            detalle
                                                    .getAmbitoSnapshot(),

                                            detalle
                                                    .getCantidadEncontrada()
                                    )
                    )
                    .toList();

    return new InventarioDetalleResponse(
            inventario.getId(),

            inventario
                    .getPlaza()
                    .getId(),

            inventario
                    .getPlaza()
                    .getCodigo(),

            inventario
                    .getResponsable()
                    .getId(),

            inventario
                    .getResponsable()
                    .getCodigo(),

            inventario
                    .getResponsable()
                    .getNombreCompleto(),

            inventario
                    .getRol()
                    .getCodigo(),

            inventario
                    .getFechaInicio(),

            inventario
                    .getFechaFinalizacion(),

            inventario
                    .getEstado(),

            inventario
                    .getObservacion(),

            inventario
                    .getMotivoAnulacion(),

            items
    );
  }
}