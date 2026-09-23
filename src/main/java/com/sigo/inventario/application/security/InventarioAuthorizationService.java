package com.sigo.inventario.application.security;

import com.sigo.inventario.application.port.out.InventarioProductoVisibilidadPort;
import com.sigo.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventarioAuthorizationService {

  private final InventarioProductoVisibilidadPort visibilidadPort;


  public void exigirPlazaAsignada(
          InventarioUsuarioActual u
  ) {

    if (u.plazaId() == null) {

      throw new ForbiddenException("Trabajador sin plaza asignada"
      );
    }
  }


  /**
   * Pueden administrar productos:
   *
   * CONTROLADOR:
   * - solamente de su propia plaza
   *
   * SUPERVISOR:
   * - todas las plazas
   */
  public void exigirAdministrador(
          InventarioUsuarioActual u
  ) {

    String rol = u.rolCodigo();

    boolean permitido =
            "CONTROLADOR".equalsIgnoreCase(rol)
                    ||
                    "SUPERVISOR".equalsIgnoreCase(rol);

    if (!permitido) {

      throw new ForbiddenException("No tiene permisos para administrar productos"
      );
    }

    /*
     * El controlador necesita necesariamente
     * tener una plaza asignada.
     */
    if ("CONTROLADOR".equalsIgnoreCase(rol)) {
      exigirPlazaAsignada(u);
    }
  }


  public boolean esSupervisor(
          InventarioUsuarioActual u
  ) {

    return "SUPERVISOR"
            .equalsIgnoreCase(
                    u.rolCodigo()
            );
  }


  public boolean esControlador(
          InventarioUsuarioActual u
  ) {

    return "CONTROLADOR"
            .equalsIgnoreCase(
                    u.rolCodigo()
            );
  }


  public void exigirPuedeModificarConteo(
          InventarioUsuarioActual u,
          Long responsableId
  ) {

    if (!responsableId.equals(
            u.trabajadorId()
    )) {

      throw new ForbiddenException("El inventario pertenece a otro trabajador"
      );
    }
  }


  public void exigirProductoVisible(
          InventarioUsuarioActual u,
          Long productoId
  ) {

    exigirPlazaAsignada(u);

    boolean visible =
            visibilidadPort.esVisiblePara(
                    productoId,
                    u.rolId(),
                    u.plazaId()
            );

    if (!visible) {

      throw new ForbiddenException("Producto no autorizado para el rol o plaza"
      );
    }
  }


  public void exigirPuedeConsultarPlaza(
          InventarioUsuarioActual u,
          Long plazaId
  ) {

    if (esSupervisor(u)) {
      return;
    }

    exigirPlazaAsignada(u);

    if (!u.plazaId()
            .equals(
                    plazaId
            )) {

      throw new ForbiddenException("No puede consultar otra plaza"
      );
    }
  }


  /**
   * Evita que un controlador intente trabajar
   * con una plaza distinta a la que tiene asignada.
   */
  public void exigirPuedeAdministrarPlaza(
          InventarioUsuarioActual u,
          Long plazaId
  ) {

    exigirAdministrador(u);

    if (esSupervisor(u)) {
      return;
    }

    exigirPlazaAsignada(u);

    if (!u.plazaId().equals(plazaId)) {

      throw new ForbiddenException("El controlador solo puede administrar productos de su propia plaza"
      );
    }
  }
}