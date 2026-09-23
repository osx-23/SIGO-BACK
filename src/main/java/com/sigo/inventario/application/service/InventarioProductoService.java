package com.sigo.inventario.application.service;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.inventario.api.dto.request.ProductoGuardarRequest;
import com.sigo.inventario.api.dto.request.ProductoPlazaRequest;
import com.sigo.inventario.api.dto.response.ProductoAdminResponse;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioAmbito;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioCategoria;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProducto;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProductoPlaza;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProductoRol;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioAmbitoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioCategoriaRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioProductoPlazaRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioProductoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioProductoRolRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioRolRepository;
import com.sigo.inventario.application.security.InventarioAuthorizationService;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventarioProductoService {

  private final InventarioProductoRepository productos;
  private final InventarioCategoriaRepository categorias;
  private final InventarioAmbitoRepository ambitos;
  private final InventarioRolRepository roles;
  private final InventarioProductoRolRepository productoRoles;
  private final InventarioProductoPlazaRepository productoPlazas;
  private final PlazaRepository plazas;
  private final InventarioAuthorizationService auth;
  private final InventarioAuditoriaService auditoria;


  // ============================================================
  // LISTAR
  // ============================================================

  @Transactional(readOnly = true)
  public List<ProductoAdminResponse> listar(
          InventarioUsuarioActual usuario
  ) {

    auth.exigirAdministrador(usuario);

    /*
     * Antes se cargaban todos los productos y luego se ejecutaba
     * una consulta adicional por producto para comprobar la plaza.
     * Ahora el filtro se hace directamente en PostgreSQL.
     */
    List<InventarioProducto> lista = auth.esSupervisor(usuario)
            ? productos.listarAdministracion()
            : productos.listarAdministracionPorPlaza(usuario.plazaId());

    if (lista.isEmpty()) {
      return List.of();
    }

    List<Long> productoIds = lista.stream()
            .map(InventarioProducto::getId)
            .toList();

    /*
     * Cargamos TODOS los roles en una sola consulta.
     */
    Map<Long, Set<String>> rolesPorProducto = productoRoles
            .findAllByProductoIdsConRol(productoIds)
            .stream()
            .collect(Collectors.groupingBy(
                    x -> x.getProducto().getId(),
                    Collectors.mapping(
                            x -> x.getRol().getCodigo(),
                            Collectors.toCollection(LinkedHashSet::new)
                    )
            ));

    /*
     * Cargamos TODAS las plazas en una sola consulta.
     */
    Map<Long, List<ProductoAdminResponse.ProductoPlazaConfigResponse>> plazasPorProducto =
            productoPlazas
                    .findAllByProductoIdsConPlaza(productoIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            x -> x.getProducto().getId(),
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    items -> items.stream()
                                            .sorted(Comparator.comparing(
                                                    x -> x.getPlaza().getCodigo()
                                            ))
                                            .map(x -> new ProductoAdminResponse.ProductoPlazaConfigResponse(
                                                    x.getPlaza().getId(),
                                                    x.getPlaza().getCodigo(),
                                                    x.getStockMinimo()
                                            ))
                                            .toList()
                            )
                    ));

    return lista.stream()
            .map(producto -> toResponse(
                    producto,
                    rolesPorProducto.getOrDefault(producto.getId(), Set.of()),
                    plazasPorProducto.getOrDefault(producto.getId(), List.of())
            ))
            .toList();
  }


  // ============================================================
  // CREAR PRODUCTO
  // ============================================================

  @Transactional
  public ProductoAdminResponse crear(
          InventarioUsuarioActual usuario,
          ProductoGuardarRequest request
  ) {

    auth.exigirAdministrador(usuario);

    validarRequest(request);

    String codigo =
            request.codigo()
                    .trim()
                    .toUpperCase(
                            Locale.ROOT
                    );

    productos
            .findByCodigoIgnoreCase(codigo)
            .ifPresent(
                    producto -> {

                      throw new ResponseStatusException(
                              HttpStatus.CONFLICT,
                              "Código de producto duplicado"
                      );
                    }
            );

    InventarioProducto producto =
            new InventarioProducto();

    aplicar(
            producto,
            request
    );

    producto =
            productos.save(producto);


    /*
     * Supervisor:
     * usa las plazas enviadas desde frontend.
     *
     * Controlador:
     * forzamos únicamente su plaza.
     */
    guardarConfiguracion(
            usuario,
            producto,
            request
    );


    auditoria.registrar(
            usuario.trabajadorId(),
            "PRODUCTO_CREADO",
            "INVENTARIO_PRODUCTO",
            producto.getId(),
            Map.of(
                    "codigo",
                    producto.getCodigo(),
                    "nombre",
                    producto.getNombre()
            )
    );

    return toResponse(producto);
  }


  // ============================================================
  // ACTUALIZAR PRODUCTO
  // ============================================================

  @Transactional
  public ProductoAdminResponse actualizar(
          InventarioUsuarioActual usuario,
          Long id,
          ProductoGuardarRequest request
  ) {

    auth.exigirAdministrador(usuario);

    validarRequest(request);

    InventarioProducto producto =
            productos
                    .findById(id)
                    .orElseThrow(
                            () ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Producto no encontrado"
                                    )
                    );


    /*
     * Un controlador solo puede modificar
     * productos asociados a su propia plaza.
     */
    if (auth.esControlador(usuario)) {

      boolean perteneceAPlaza =
              productoPlazas
                      .findByProductoIdAndPlazaId(
                              id,
                              usuario.plazaId()
                      )
                      .isPresent();

      if (!perteneceAPlaza) {

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No puede modificar productos de otra plaza"
        );
      }
    }


    String codigo =
            request.codigo()
                    .trim()
                    .toUpperCase(
                            Locale.ROOT
                    );

    productos
            .findByCodigoIgnoreCase(codigo)
            .filter(
                    existente ->
                            !existente
                                    .getId()
                                    .equals(id)
            )
            .ifPresent(
                    existente -> {

                      throw new ResponseStatusException(
                              HttpStatus.CONFLICT,
                              "Código de producto duplicado"
                      );
                    }
            );


    aplicar(
            producto,
            request
    );

    productos.save(producto);


    /*
     * Eliminamos configuración anterior.
     */
    productoRoles.deleteByProductoId(id);
    productoPlazas.deleteByProductoId(id);

    productoRoles.flush();
    productoPlazas.flush();


    guardarConfiguracion(
            usuario,
            producto,
            request
    );


    auditoria.registrar(
            usuario.trabajadorId(),
            "PRODUCTO_ACTUALIZADO",
            "INVENTARIO_PRODUCTO",
            producto.getId(),
            Map.of(
                    "codigo",
                    producto.getCodigo()
            )
    );

    return toResponse(producto);
  }


  // ============================================================
  // ACTIVAR / DESACTIVAR
  // ============================================================

  @Transactional
  public ProductoAdminResponse cambiarEstado(
          InventarioUsuarioActual usuario,
          Long id,
          boolean activo
  ) {

    auth.exigirAdministrador(usuario);

    InventarioProducto producto =
            productos
                    .findById(id)
                    .orElseThrow(
                            () ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Producto no encontrado"
                                    )
                    );


    /*
     * Controlador:
     * solamente productos de su plaza.
     */
    if (auth.esControlador(usuario)) {

      boolean perteneceAPlaza =
              productoPlazas
                      .findByProductoIdAndPlazaId(
                              id,
                              usuario.plazaId()
                      )
                      .isPresent();

      if (!perteneceAPlaza) {

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No puede modificar productos de otra plaza"
        );
      }
    }


    producto.setActivo(activo);

    productos.save(producto);


    auditoria.registrar(
            usuario.trabajadorId(),
            activo
                    ? "PRODUCTO_ACTIVADO"
                    : "PRODUCTO_DESACTIVADO",
            "INVENTARIO_PRODUCTO",
            id,
            Map.of(
                    "activo",
                    activo
            )
    );

    return toResponse(producto);
  }


  // ============================================================
  // APLICAR DATOS GENERALES
  // ============================================================

  private void aplicar(
          InventarioProducto producto,
          ProductoGuardarRequest request
  ) {

    InventarioCategoria categoria =
            categorias
                    .findById(
                            request.categoriaId()
                    )
                    .filter(
                            c ->
                                    Boolean.TRUE.equals(
                                            c.getActivo()
                                    )
                    )
                    .orElseThrow(
                            () ->
                                    new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST,
                                            "Categoría inválida"
                                    )
                    );


    InventarioAmbito ambito =
            ambitos
                    .findById(
                            request.ambitoId()
                    )
                    .filter(
                            a ->
                                    Boolean.TRUE.equals(
                                            a.getActivo()
                                    )
                    )
                    .orElseThrow(
                            () ->
                                    new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST,
                                            "Ámbito inválido"
                                    )
                    );


    producto.setCodigo(
            request.codigo()
                    .trim()
                    .toUpperCase(
                            Locale.ROOT
                    )
    );

    producto.setNombre(
            request.nombre()
                    .trim()
    );

    producto.setDescripcion(
            request.descripcion() == null
                    ||
                    request.descripcion()
                            .isBlank()

                    ? null
                    : request.descripcion()
                    .trim()
    );

    producto.setCategoria(
            categoria
    );

    producto.setAmbito(
            ambito
    );

    producto.setUnidadMedida(
            request.unidadMedida()
                    .trim()
    );

    producto.setActivo(
            request.activo() == null
                    ? Boolean.TRUE
                    : request.activo()
    );
  }


  // ============================================================
  // CONFIGURAR ROLES Y PLAZAS
  // ============================================================

  private void guardarConfiguracion(
          InventarioUsuarioActual usuario,
          InventarioProducto producto,
          ProductoGuardarRequest request
  ) {

    guardarRoles(
            usuario,
            producto,
            request.roles()
    );


    if (auth.esControlador(usuario)) {

      /*
       * Un controlador no puede mandar otras plazas.
       * El backend ignora completamente cualquier plaza
       * recibida desde Angular.
       */

      Long plazaId =
              usuario.plazaId();

      Plaza plaza =
              plazas
                      .findById(plazaId)
                      .filter(
                              p ->
                                      Boolean.TRUE.equals(
                                              p.getActivo()
                                      )
                      )
                      .orElseThrow(
                              () ->
                                      new ResponseStatusException(
                                              HttpStatus.BAD_REQUEST,
                                              "Plaza del controlador inválida"
                                      )
                      );


      Integer stockMinimo =
              obtenerStockMinimoControlador(
                      request,
                      plazaId
              );


      productoPlazas.save(
              new InventarioProductoPlaza(
                      producto,
                      plaza,
                      stockMinimo
              )
      );

      return;
    }


    /*
     * SUPERVISOR
     */
    Set<Long> plazasUsadas =
            new HashSet<>();


    for (ProductoPlazaRequest config :
            request.plazas()) {

      if (!plazasUsadas.add(
              config.plazaId()
      )) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Plaza repetida"
        );
      }


      Plaza plaza =
              plazas
                      .findById(
                              config.plazaId()
                      )
                      .filter(
                              p ->
                                      Boolean.TRUE.equals(
                                              p.getActivo()
                                      )
                      )
                      .orElseThrow(
                              () ->
                                      new ResponseStatusException(
                                              HttpStatus.BAD_REQUEST,
                                              "Plaza inválida: "
                                                      + config.plazaId()
                                      )
                      );


      int stockMinimo =
              config.stockMinimo() == null
                      ? 0
                      : config.stockMinimo();


      if (stockMinimo < 0) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El stock mínimo no puede ser negativo"
        );
      }


      productoPlazas.save(
              new InventarioProductoPlaza(
                      producto,
                      plaza,
                      stockMinimo
              )
      );
    }
  }


  // ============================================================
  // GUARDAR ROLES
  // ============================================================

  private void guardarRoles(
          InventarioUsuarioActual usuario,
          InventarioProducto producto,
          Set<String> rolesRequest
  ) {

    Set<String> usados =
            new HashSet<>();


    for (String codigoRequest :
            rolesRequest) {

      String codigoRol =
              codigoRequest
                      .trim()
                      .toUpperCase(
                              Locale.ROOT
                      );


      /*
       * El controlador no puede configurar
       * visibilidad para SUPERVISOR.
       */
      if (auth.esControlador(usuario)
              &&
              "SUPERVISOR"
                      .equals(
                              codigoRol
                      )) {

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El controlador no puede asignar el rol SUPERVISOR"
        );
      }


      if (!usados.add(
              codigoRol
      )) {

        continue;
      }


      InventarioRol rol =
              roles
                      .findByCodigoAndActivoTrue(
                              codigoRol
                      )
                      .orElseThrow(
                              () ->
                                      new ResponseStatusException(
                                              HttpStatus.BAD_REQUEST,
                                              "Rol inválido: "
                                                      + codigoRol
                                      )
                      );


      productoRoles.save(
              new InventarioProductoRol(
                      producto,
                      rol
              )
      );
    }
  }


  // ============================================================
  // STOCK MÍNIMO DEL CONTROLADOR
  // ============================================================

  private Integer obtenerStockMinimoControlador(
          ProductoGuardarRequest request,
          Long plazaId
  ) {

    if (request.plazas() == null) {
      return 0;
    }


    return request.plazas()
            .stream()
            .filter(
                    p ->
                            plazaId.equals(
                                    p.plazaId()
                            )
            )
            .findFirst()
            .map(
                    p ->
                            p.stockMinimo() == null
                                    ? 0
                                    : p.stockMinimo()
            )
            .map(
                    stock -> {

                      if (stock < 0) {

                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "El stock mínimo no puede ser negativo"
                        );
                      }

                      return stock;
                    }
            )
            .orElse(0);
  }


  // ============================================================
  // VALIDACIONES DEL REQUEST
  // ============================================================

  private void validarRequest(
          ProductoGuardarRequest request
  ) {

    if (request == null) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Datos del producto obligatorios"
      );
    }


    if (request.codigo() == null
            ||
            request.codigo()
                    .isBlank()) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Código obligatorio"
      );
    }


    if (request.nombre() == null
            ||
            request.nombre()
                    .isBlank()) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Nombre obligatorio"
      );
    }


    if (request.unidadMedida() == null
            ||
            request.unidadMedida()
                    .isBlank()) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Unidad de medida obligatoria"
      );
    }


    if (request.categoriaId() == null) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Categoría obligatoria"
      );
    }


    if (request.ambitoId() == null) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Ámbito obligatorio"
      );
    }


    if (request.roles() == null
            ||
            request.roles()
                    .isEmpty()) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Debe seleccionar al menos un rol"
      );
    }


    if (request.plazas() == null) {

      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Configuración de plazas obligatoria"
      );
    }
  }


  // ============================================================
  // RESPONSE
  // ============================================================

  private ProductoAdminResponse toResponse(
          InventarioProducto producto,
          Set<String> rolesResponse,
          List<ProductoAdminResponse.ProductoPlazaConfigResponse> plazasResponse
  ) {

    return new ProductoAdminResponse(
            producto.getId(),
            producto.getCodigo(),
            producto.getNombre(),
            producto.getDescripcion(),
            producto.getCategoria() == null ? null : producto.getCategoria().getId(),
            producto.getCategoria() == null ? null : producto.getCategoria().getNombre(),
            producto.getAmbito() == null ? null : producto.getAmbito().getId(),
            producto.getAmbito() == null ? null : producto.getAmbito().getNombre(),
            producto.getUnidadMedida(),
            producto.getActivo(),
            rolesResponse,
            plazasResponse
    );
  }


  private ProductoAdminResponse toResponse(
          InventarioProducto producto
  ) {

    Set<String> rolesResponse =
            productoRoles
                    .findByProductoId(
                            producto.getId()
                    )
                    .stream()
                    .map(
                            x ->
                                    x.getRol()
                                            .getCodigo()
                    )
                    .collect(
                            Collectors.toCollection(
                                    LinkedHashSet::new
                            )
                    );


    var plazasResponse =
            productoPlazas
                    .findByProductoId(
                            producto.getId()
                    )
                    .stream()
                    .sorted(
                            Comparator.comparing(
                                    x ->
                                            x.getPlaza()
                                                    .getCodigo()
                            )
                    )
                    .map(
                            x ->
                                    new ProductoAdminResponse
                                            .ProductoPlazaConfigResponse(

                                            x.getPlaza()
                                                    .getId(),

                                            x.getPlaza()
                                                    .getCodigo(),

                                            x.getStockMinimo()
                                    )
                    )
                    .toList();


    return new ProductoAdminResponse(

            producto.getId(),

            producto.getCodigo(),

            producto.getNombre(),

            producto.getDescripcion(),

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
                    .getNombre(),

            producto.getUnidadMedida(),

            producto.getActivo(),

            rolesResponse,

            plazasResponse
    );
  }
}