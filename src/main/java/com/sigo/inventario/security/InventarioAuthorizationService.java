package com.sigo.inventario.security;

import com.sigo.inventario.entity.InventarioConteo;
import com.sigo.inventario.repository.InventarioProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InventarioAuthorizationService {

  private final InventarioProductoRepository productoRepository;


  public void exigirPlazaAsignada(
          InventarioUsuarioActual u
  ) {

    if (u.plazaId() == null) {

      throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "Trabajador sin plaza asignada"
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

      throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "No tiene permisos para administrar productos"
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
          InventarioConteo i
  ) {

    if (!i.getResponsable()
            .getId()
            .equals(
                    u.trabajadorId()
            )) {

      throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "El inventario pertenece a otro trabajador"
      );
    }
  }


  public void exigirProductoVisible(
          InventarioUsuarioActual u,
          Long productoId
  ) {

    exigirPlazaAsignada(u);

    boolean visible =
            productoRepository.esVisiblePara(
                    productoId,
                    u.rol().getId(),
                    u.plazaId()
            );

    if (!visible) {

      throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "Producto no autorizado para el rol o plaza"
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

      throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "No puede consultar otra plaza"
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

      throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "El controlador solo puede administrar productos de su propia plaza"
      );
    }
  }
}