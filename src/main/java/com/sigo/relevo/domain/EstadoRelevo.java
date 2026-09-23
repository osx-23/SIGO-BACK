package com.sigo.relevo.domain;

public enum EstadoRelevo {
    OPERATIVO,
    OBSERVADO,
    NO_OPERATIVO;

    public boolean requiereDetalle() {
        return this == OBSERVADO || this == NO_OPERATIVO;
    }
}
