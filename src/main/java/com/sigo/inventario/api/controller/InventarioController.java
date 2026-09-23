package com.sigo.inventario.api.controller;

import com.sigo.inventario.api.dto.request.AnularInventarioRequest;
import com.sigo.inventario.api.dto.request.GuardarConteoRequest;
import com.sigo.inventario.api.dto.response.InventarioDetalleItemResponse;
import com.sigo.inventario.api.dto.response.InventarioDetalleResponse;
import com.sigo.inventario.api.dto.response.InventarioResumenResponse;
import com.sigo.inventario.api.dto.response.ProductoInventarioResponse;
import com.sigo.inventario.application.port.in.GuardarConteoInventarioUseCase;
import com.sigo.inventario.application.port.in.IniciarInventarioUseCase;
import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.inventario.application.security.InventarioUsuarioContextService;
import com.sigo.inventario.application.service.InventarioService;
import com.sigo.inventario.domain.InventarioEstado;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inventarios")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService service;
    private final GuardarConteoInventarioUseCase guardarConteoUseCase;
    private final IniciarInventarioUseCase iniciarUseCase;
    private final InventarioConsultaUseCase consultaUseCase;
    private final InventarioUsuarioContextService usuarios;

    @PostMapping
    public InventarioResumenResponse iniciar() {
        InventarioUsuarioActual actual =
                usuarios.obtenerActual();

        IniciarInventarioUseCase.Resumen resumen =
                iniciarUseCase.iniciar(
                        new IniciarInventarioUseCase.Usuario(
                                actual.trabajadorId(),
                                actual.plazaId(),
                                actual.rolId(),
                                actual.rolCodigo()
                        )
                );

        return new InventarioResumenResponse(
                resumen.id(),
                resumen.plazaId(),
                resumen.plaza(),
                resumen.responsableId(),
                resumen.codigoResponsable(),
                resumen.responsable(),
                resumen.rol(),
                resumen.fechaInicio(),
                resumen.fechaFinalizacion(),
                resumen.estado(),
                resumen.productosRegistrados()
        );
    }

    @GetMapping("/{id}/productos")
    public List<ProductoInventarioResponse> productos(
            @PathVariable Long id
    ) {
        return consultaUseCase
                .productosPermitidos(
                        usuarioConsulta(),
                        id
                )
                .stream()
                .map(this::toProductoResponse)
                .toList();
    }

    @PutMapping("/{id}/detalle")
    public InventarioDetalleResponse guardar(
            @PathVariable Long id,
            @Valid @RequestBody GuardarConteoRequest request
    ) {
        InventarioUsuarioActual actual =
                usuarios.obtenerActual();

        var detalle = guardarConteoUseCase.guardar(
                new GuardarConteoInventarioUseCase.Usuario(
                        actual.trabajadorId(),
                        actual.plazaId(),
                        actual.rolId(),
                        actual.rolCodigo()
                ),
                id,
                request.productos()
                        .stream()
                        .map(item ->
                                new GuardarConteoInventarioUseCase.Item(
                                        item.productoId(),
                                        item.cantidad()
                                )
                        )
                        .toList()
        );

        return toDetalleResponse(detalle);
    }

    @PostMapping("/{id}/finalizar")
    public InventarioDetalleResponse finalizar(
            @PathVariable Long id
    ) {
        return service.finalizar(
                usuarios.obtenerActual(),
                id
        );
    }

    @PostMapping("/{id}/anular")
    public InventarioDetalleResponse anular(
            @PathVariable Long id,
            @Valid @RequestBody AnularInventarioRequest request
    ) {
        return service.anular(
                usuarios.obtenerActual(),
                id,
                request
        );
    }

    @GetMapping("/{id}")
    public InventarioDetalleResponse detalle(
            @PathVariable Long id
    ) {
        return toDetalleResponse(
                consultaUseCase.detalle(
                        usuarioConsulta(),
                        id
                )
        );
    }

    @GetMapping
    public Page<InventarioResumenResponse> historial(
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Long responsableId,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) InventarioEstado estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        InventarioUsuarioActual actual =
                usuarios.obtenerActual();

        validarConsultaHistorial(
                actual,
                plazaId,
                responsableId,
                rol,
                estado,
                desde,
                hasta,
                page,
                size
        );

        InventarioConsultaUseCase.Pagina<InventarioConsultaUseCase.Resumen>
                resultado =
                consultaUseCase.historial(
                        toUsuarioConsulta(actual),
                        plazaId,
                        responsableId,
                        rol,
                        estado,
                        desde,
                        hasta,
                        page,
                        size
                );

        List<InventarioResumenResponse> contenido =
                resultado.contenido()
                        .stream()
                        .map(this::toResumenResponse)
                        .toList();

        return new PageImpl<>(
                contenido,
                PageRequest.of(
                        resultado.pagina(),
                        resultado.tamanio()
                ),
                resultado.totalElementos()
        );
    }

    private InventarioConsultaUseCase.Usuario usuarioConsulta() {
        return toUsuarioConsulta(
                usuarios.obtenerActual()
        );
    }

    private InventarioConsultaUseCase.Usuario toUsuarioConsulta(
            InventarioUsuarioActual usuario
    ) {
        return new InventarioConsultaUseCase.Usuario(
                usuario.trabajadorId(),
                usuario.plazaId(),
                usuario.rolCodigo()
        );
    }

    private ProductoInventarioResponse toProductoResponse(
            InventarioConsultaUseCase.Producto producto
    ) {
        return new ProductoInventarioResponse(
                producto.id(),
                producto.codigo(),
                producto.nombre(),
                producto.descripcion(),
                producto.unidadMedida(),
                producto.categoriaId(),
                producto.categoria(),
                producto.ambitoId(),
                producto.ambito()
        );
    }

    private InventarioDetalleResponse toDetalleResponse(
            InventarioConsultaUseCase.Detalle detalle
    ) {
        return new InventarioDetalleResponse(
                detalle.id(),
                detalle.plazaId(),
                detalle.plaza(),
                detalle.responsableId(),
                detalle.codigoResponsable(),
                detalle.responsable(),
                detalle.rol(),
                detalle.fechaInicio(),
                detalle.fechaFinalizacion(),
                detalle.estado(),
                detalle.observacion(),
                detalle.motivoAnulacion(),
                detalle.productos()
                        .stream()
                        .map(item ->
                                new InventarioDetalleItemResponse(
                                        item.productoId(),
                                        item.nombre(),
                                        item.unidad(),
                                        item.categoria(),
                                        item.ambito(),
                                        item.cantidad()
                                )
                        )
                        .toList()
        );
    }

    private InventarioResumenResponse toResumenResponse(
            InventarioConsultaUseCase.Resumen resumen
    ) {
        return new InventarioResumenResponse(
                resumen.id(),
                resumen.plazaId(),
                resumen.plaza(),
                resumen.responsableId(),
                resumen.codigoResponsable(),
                resumen.responsable(),
                resumen.rol(),
                resumen.fechaInicio(),
                resumen.fechaFinalizacion(),
                resumen.estado(),
                resumen.productosRegistrados()
        );
    }

    private void validarConsultaHistorial(
            InventarioUsuarioActual actual,
            Long plazaId,
            Long responsableId,
            String rol,
            InventarioEstado estado,
            LocalDate desde,
            LocalDate hasta,
            int page,
            int size
    ) {
        RolSistema rolSistema =
                actual.trabajador().getRolSistema();

        if (rolSistema == RolSistema.CONTROLADOR
                || rolSistema == RolSistema.SUPERVISOR) {
            return;
        }

        boolean recuperacionPropia =
                rolSistema == RolSistema.OPERADOR
                        && responsableId != null
                        && responsableId.equals(
                                actual.trabajador().getId()
                        )
                        && estado == InventarioEstado.EN_PROCESO
                        && plazaId == null
                        && rol == null
                        && desde == null
                        && hasta == null
                        && page == 0
                        && size == 1;

        if (!recuperacionPropia) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo controladores y supervisores pueden consultar el historial de inventario"
            );
        }
    }
}
