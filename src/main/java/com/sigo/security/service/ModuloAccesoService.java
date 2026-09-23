package com.sigo.security.service;

import com.sigo.personal.entity.RolSistema;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ModuloAccesoService {
    public List<String> modulosPara(RolSistema rol) {
        return switch (rol) {
            case SUPERVISOR -> List.of(
                    "DASHBOARD","RELEVOS","ASISTENCIA","INVENTARIO",
                    "ADMIN_PRODUCTOS","TRABAJADORES","CHAT",
                    "PROGRAMACION","DISTRIBUCION","MI_HORARIO"
            );
            case CONTROLADOR -> List.of(
                    "DASHBOARD","RELEVOS","ASISTENCIA","INVENTARIO",
                    "ADMIN_PRODUCTOS","CHAT","DISTRIBUCION","MI_HORARIO"
            );
            case OPERADOR -> List.of(
                    "RELEVOS","INVENTARIO","MI_HORARIO"
            );
        };
    }
}
