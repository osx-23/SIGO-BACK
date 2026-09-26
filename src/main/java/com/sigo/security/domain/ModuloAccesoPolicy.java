package com.sigo.security.domain;

import java.util.List;

public class ModuloAccesoPolicy {

    public List<String> modulosPara(RolSeguridad rol) {
        return switch (rol) {
            case SUPERVISOR -> List.of(
                    "DASHBOARD",
                    "RELEVOS",
                    "INCIDENCIAS",
                    "ASISTENCIA",
                    "INVENTARIO",
                    "ADMIN_PRODUCTOS",
                    "TRABAJADORES",
                    "CHAT",
                    "PROGRAMACION",
                    "DISTRIBUCION",
                    "MI_HORARIO"
            );

            case CONTROLADOR -> List.of(
                    "DASHBOARD",
                    "RELEVOS",
                    "ASISTENCIA",
                    "INVENTARIO",
                    "ADMIN_PRODUCTOS",
                    "CHAT",
                    "DISTRIBUCION",
                    "MI_HORARIO"
            );

            case OPERADOR -> List.of(
                    "RELEVOS",
                    "INCIDENCIAS",
                    "INVENTARIO",
                    "MI_HORARIO"
            );
        };
    }
}
