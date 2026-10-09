package com.sigo.relevo.application.port.out;

import com.sigo.relevo.application.port.in.ConfigurarViasAviUseCase;
import com.sigo.relevo.application.port.in.ListarViasUseCase;

import java.util.List;

public interface ViaConsultaPort {

    boolean existePlaza(Long plazaId);

    List<ListarViasUseCase.Via> listarActivas(Long plazaId);

    List<ListarViasUseCase.Via> listarActivasVisiblesAvi(Long plazaId);

    List<ConfigurarViasAviUseCase.ViaConfig> listarActivasParaConfigAvi(Long plazaId);

    void actualizarVisibilidadAvi(
            Long plazaId,
            Long viaId,
            boolean visible
    );
}
