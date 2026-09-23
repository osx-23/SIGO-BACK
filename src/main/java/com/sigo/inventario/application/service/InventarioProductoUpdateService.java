package com.sigo.inventario.application.service;

import com.sigo.inventario.api.dto.request.ProductoGuardarRequest;
import com.sigo.inventario.api.dto.request.ProductoPlazaRequest;
import com.sigo.inventario.api.dto.response.ProductoAdminResponse;
import com.sigo.inventario.application.security.InventarioAuthorizationService;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventarioProductoUpdateService {

  private final NamedParameterJdbcTemplate jdbc;
  private final InventarioAuthorizationService auth;
  private final InventarioAuditoriaService auditoria;

  @Transactional
  public ProductoAdminResponse actualizar(
      InventarioUsuarioActual usuario,
      Long id,
      ProductoGuardarRequest request
  ) {
    auth.exigirAdministrador(usuario);

    String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
    String nombre = request.nombre().trim();
    String unidad = request.unidadMedida().trim();
    String descripcion = request.descripcion() == null || request.descripcion().isBlank()
        ? null
        : request.descripcion().trim();
    boolean activo = request.activo() == null || request.activo();

    exigirProductoExiste(id);
    exigirCodigoDisponible(id, codigo);

    if (auth.esControlador(usuario)) {
      exigirProductoDePlaza(id, usuario.plazaId());
    }

    Catalogo categoria = obtenerCatalogoActivo("inventario_categoria", request.categoriaId(), "Categoría inválida");
    Catalogo ambito = obtenerCatalogoActivo("inventario_ambito", request.ambitoId(), "Ámbito inválido");

    Set<String> codigosRoles = normalizarRoles(usuario, request.roles());
    Map<String, Long> rolesPorCodigo = obtenerRolesActivos(codigosRoles);
    if (rolesPorCodigo.size() != codigosRoles.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más roles son inválidos");
    }

    List<PlazaConfig> configuracionPlazas = auth.esControlador(usuario)
        ? configurarPlazaControlador(usuario, request)
        : configurarPlazasSupervisor(request.plazas());

    var updateParams = new MapSqlParameterSource()
        .addValue("id", id)
        .addValue("codigo", codigo)
        .addValue("nombre", nombre)
        .addValue("descripcion", descripcion)
        .addValue("categoriaId", request.categoriaId())
        .addValue("ambitoId", request.ambitoId())
        .addValue("unidad", unidad)
        .addValue("activo", activo);

    jdbc.update("""
        UPDATE inventario_producto
        SET codigo = :codigo,
            nombre = :nombre,
            descripcion = :descripcion,
            categoria_id = :categoriaId,
            ambito_id = :ambitoId,
            unidad_medida = :unidad,
            activo = :activo,
            actualizado_en = NOW()
        WHERE id = :id
        """, updateParams);

    jdbc.update("DELETE FROM inventario_producto_rol WHERE producto_id = :id",
        new MapSqlParameterSource("id", id));
    jdbc.update("DELETE FROM inventario_producto_plaza WHERE producto_id = :id",
        new MapSqlParameterSource("id", id));

    SqlParameterSource[] roleBatch = codigosRoles.stream()
        .map(rol -> new MapSqlParameterSource()
            .addValue("productoId", id)
            .addValue("rolId", rolesPorCodigo.get(rol)))
        .toArray(SqlParameterSource[]::new);

    jdbc.batchUpdate("""
        INSERT INTO inventario_producto_rol(producto_id, rol_id)
        VALUES (:productoId, :rolId)
        """, roleBatch);

    SqlParameterSource[] plazaBatch = configuracionPlazas.stream()
        .map(config -> new MapSqlParameterSource()
            .addValue("productoId", id)
            .addValue("plazaId", config.id())
            .addValue("stockMinimo", config.stockMinimo()))
        .toArray(SqlParameterSource[]::new);

    jdbc.batchUpdate("""
        INSERT INTO inventario_producto_plaza(producto_id, plaza_id, stock_minimo)
        VALUES (:productoId, :plazaId, :stockMinimo)
        """, plazaBatch);

    auditoria.registrar(
        usuario.trabajador(),
        "PRODUCTO_ACTUALIZADO",
        "INVENTARIO_PRODUCTO",
        id,
        Map.of("codigo", codigo)
    );

    List<ProductoAdminResponse.ProductoPlazaConfigResponse> plazasResponse = configuracionPlazas.stream()
        .map(p -> new ProductoAdminResponse.ProductoPlazaConfigResponse(p.id(), p.codigo(), p.stockMinimo()))
        .toList();

    return new ProductoAdminResponse(
        id,
        codigo,
        nombre,
        descripcion,
        request.categoriaId(),
        categoria.nombre(),
        request.ambitoId(),
        ambito.nombre(),
        unidad,
        activo,
        new LinkedHashSet<>(codigosRoles),
        plazasResponse
    );
  }

  private void exigirProductoExiste(Long id) {
    Integer existe = jdbc.queryForObject(
        "SELECT COUNT(*) FROM inventario_producto WHERE id = :id",
        new MapSqlParameterSource("id", id),
        Integer.class
    );
    if (existe == null || existe == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
    }
  }

  private void exigirCodigoDisponible(Long id, String codigo) {
    Integer duplicados = jdbc.queryForObject(
        """
        SELECT COUNT(*)
        FROM inventario_producto
        WHERE UPPER(codigo) = UPPER(:codigo) AND id <> :id
        """,
        new MapSqlParameterSource().addValue("codigo", codigo).addValue("id", id),
        Integer.class
    );
    if (duplicados != null && duplicados > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Código de producto duplicado");
    }
  }

  private void exigirProductoDePlaza(Long productoId, Long plazaId) {
    Integer existe = jdbc.queryForObject(
        """
        SELECT COUNT(*)
        FROM inventario_producto_plaza
        WHERE producto_id = :productoId AND plaza_id = :plazaId
        """,
        new MapSqlParameterSource()
            .addValue("productoId", productoId)
            .addValue("plazaId", plazaId),
        Integer.class
    );
    if (existe == null || existe == 0) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede modificar productos de otra plaza");
    }
  }

  private Catalogo obtenerCatalogoActivo(String tabla, Long id, String mensaje) {
    if (!Set.of("inventario_categoria", "inventario_ambito").contains(tabla)) {
      throw new IllegalArgumentException("Catálogo no permitido");
    }
    try {
      return jdbc.queryForObject(
          "SELECT id, nombre FROM " + tabla + " WHERE id = :id AND activo = true",
          new MapSqlParameterSource("id", id),
          (rs, rowNum) -> new Catalogo(rs.getLong("id"), rs.getString("nombre"))
      );
    } catch (EmptyResultDataAccessException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
  }

  private Set<String> normalizarRoles(InventarioUsuarioActual usuario, Set<String> rolesRequest) {
    Set<String> resultado = new LinkedHashSet<>();
    for (String rol : rolesRequest) {
      String codigo = rol.trim().toUpperCase(Locale.ROOT);
      if (auth.esControlador(usuario) && "SUPERVISOR".equals(codigo)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El controlador no puede asignar el rol SUPERVISOR");
      }
      resultado.add(codigo);
    }
    return resultado;
  }

  private Map<String, Long> obtenerRolesActivos(Set<String> codigos) {
    if (codigos.isEmpty()) return Map.of();
    Map<String, Long> resultado = new LinkedHashMap<>();

    RowCallbackHandler handler = rs ->
        resultado.put(rs.getString("codigo"), rs.getLong("id"));

    jdbc.query(
        """
        SELECT id, UPPER(codigo) AS codigo
        FROM inventario_rol
        WHERE activo = true AND UPPER(codigo) IN (:codigos)
        """,
        new MapSqlParameterSource("codigos", codigos),
        handler
    );
    return resultado;
  }

  private List<PlazaConfig> configurarPlazaControlador(
      InventarioUsuarioActual usuario,
      ProductoGuardarRequest request
  ) {
    Long plazaId = usuario.plazaId();
    PlazaConfig plaza = obtenerPlazasActivas(Set.of(plazaId)).get(plazaId);
    if (plaza == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plaza del controlador inválida");
    }

    int stockMinimo = request.plazas().stream()
        .filter(p -> plazaId.equals(p.plazaId()))
        .findFirst()
        .map(p -> p.stockMinimo() == null ? 0 : p.stockMinimo())
        .orElse(0);

    if (stockMinimo < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El stock mínimo no puede ser negativo");
    }
    return List.of(new PlazaConfig(plaza.id(), plaza.codigo(), stockMinimo));
  }

  private List<PlazaConfig> configurarPlazasSupervisor(List<ProductoPlazaRequest> requestPlazas) {
    Set<Long> ids = new LinkedHashSet<>();
    for (ProductoPlazaRequest config : requestPlazas) {
      if (!ids.add(config.plazaId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plaza repetida");
      }
      if (config.stockMinimo() != null && config.stockMinimo() < 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El stock mínimo no puede ser negativo");
      }
    }

    Map<Long, PlazaConfig> plazasActivas = obtenerPlazasActivas(ids);
    if (plazasActivas.size() != ids.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una o más plazas son inválidas");
    }

    List<PlazaConfig> resultado = new ArrayList<>();
    for (ProductoPlazaRequest config : requestPlazas) {
      PlazaConfig plaza = plazasActivas.get(config.plazaId());
      resultado.add(new PlazaConfig(
          plaza.id(),
          plaza.codigo(),
          config.stockMinimo() == null ? 0 : config.stockMinimo()
      ));
    }
    return resultado;
  }

  private Map<Long, PlazaConfig> obtenerPlazasActivas(Set<Long> ids) {
    if (ids.isEmpty()) return Map.of();
    Map<Long, PlazaConfig> resultado = new LinkedHashMap<>();

    RowCallbackHandler handler = rs -> {
      long id = rs.getLong("id");
      resultado.put(id, new PlazaConfig(id, rs.getString("codigo"), 0));
    };

    jdbc.query(
        """
        SELECT id, codigo
        FROM plazas
        WHERE activo = true AND id IN (:ids)
        """,
        new MapSqlParameterSource("ids", ids),
        handler
    );
    return resultado;
  }

  private record Catalogo(Long id, String nombre) {}
  private record PlazaConfig(Long id, String codigo, Integer stockMinimo) {}
}
