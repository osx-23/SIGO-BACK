package com.sigo.programacion.domain;

public enum ProgramacionEstado {
    A, B, C, D, V, COM, DM, LIC;

    public boolean esOperativo() {
        return this == A || this == B || this == C;
    }
}
