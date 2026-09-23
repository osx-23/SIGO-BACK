package com.sigo.asistencia.programacion.entity;

public enum EstadoProgramacion {
    A, B, C, D, V, COM, DM, LIC;

    public boolean esOperativo() {
        return this == A || this == B || this == C;
    }
}
