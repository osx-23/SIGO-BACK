package com.sigo.relevo.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.relevo.application.port.in.GestionarEvidenciaRelevoUseCase;
import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.out.RelevoStoragePort;
import com.sigo.relevo.application.service.GestionarEvidenciaRelevoService;
import com.sigo.relevo.application.service.GestionarRelevoService;
import com.sigo.relevo.application.service.RelevoConsultaService;
import com.sigo.relevo.domain.EstadoRelevo;
import com.sigo.relevo.infrastructure.persistence.entity.ElementoRelevo;
import com.sigo.relevo.infrastructure.persistence.entity.Via;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoChecklistEvidenciaRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoChecklistRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoViaEvidenciaRepository;
import com.sigo.relevo.infrastructure.persistence.repository.RelevoViaRepository;
import com.sigo.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Import({
        RelevoGestionJpaAdapter.class,
        RelevoConsultaJpaAdapter.class,
        RelevoEvidenciaJpaAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
class RelevoFlujoJpaIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_relevo_test")
                    .withUsername("sigo")
                    .withPassword("sigo");

    @DynamicPropertySource
    static void datasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );
    }

    @Autowired
    private RelevoGestionJpaAdapter gestionAdapter;

    @Autowired
    private RelevoConsultaJpaAdapter consultaAdapter;

    @Autowired
    private RelevoEvidenciaJpaAdapter evidenciaAdapter;

    @Autowired
    private RelevoRepository relevoRepository;

    @Autowired
    private RelevoChecklistRepository checklistRepository;

    @Autowired
    private RelevoViaRepository relevoViaRepository;

    @Autowired
    private RelevoChecklistEvidenciaRepository checklistEvidenciaRepository;

    @Autowired
    private RelevoViaEvidenciaRepository viaEvidenciaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Plaza plaza;
    private Plaza otraPlaza;
    private Turno turno;
    private Trabajador operador;
    private ElementoRelevo banos;
    private ElementoRelevo conos;
    private Via via1;
    private Via viaOtraPlaza;

    private RelevoConsultaService consultaService;
    private GestionarRelevoService gestionarService;

    @BeforeEach
    void prepararDatos() {
        plaza = entityManager.persistAndFlush(
                new Plaza(null, "P4", "Plaza 4", true)
        );

        otraPlaza = entityManager.persistAndFlush(
                new Plaza(null, "P5", "Plaza 5", true)
        );

        turno = entityManager.persistAndFlush(
                new Turno(null, "A", "Turno A")
        );

        Puesto puesto = entityManager.persistAndFlush(
                new Puesto(null, "Agente de Recaudación")
        );

        operador = new Trabajador();
        operador.setCodigo(9501);
        operador.setNombreCompleto("Operador Relevo");
        operador.setPuesto(puesto);
        operador.setPlaza(plaza);
        operador.setRolSistema(RolSistema.OPERADOR);
        operador.setRequiereCambioPassword(false);
        operador.setActivo(true);
        operador = entityManager.persistAndFlush(operador);

        banos = entityManager.persistAndFlush(
                new ElementoRelevo(
                        null,
                        "BANOS",
                        "Baños",
                        "BASE",
                        false,
                        true,
                        1,
                        null
                )
        );

        conos = entityManager.persistAndFlush(
                new ElementoRelevo(
                        null,
                        "CONOS",
                        "Conos",
                        "PLAZA",
                        true,
                        true,
                        2,
                        null
                )
        );

        via1 = entityManager.persistAndFlush(
                new Via(
                        null,
                        plaza,
                        1,
                        "Vía 1",
                        true,
                        1,
                        null
                )
        );

        viaOtraPlaza = entityManager.persistAndFlush(
                new Via(
                        null,
                        otraPlaza,
                        1,
                        "Vía 1 P5",
                        true,
                        1,
                        null
                )
        );

        consultaService =
                new RelevoConsultaService(consultaAdapter);

        gestionarService =
                new GestionarRelevoService(
                        gestionAdapter,
                        consultaService
                );
    }

    @Test
    void registraRelevoCompletoYLoConsulta() {
        var registrado = gestionarService.registrar(
                command(
                        List.of(
                                new GestionarRelevoUseCase.ChecklistItem(
                                        banos.getId(),
                                        EstadoRelevo.OPERATIVO,
                                        null,
                                        null
                                ),
                                new GestionarRelevoUseCase.ChecklistItem(
                                        conos.getId(),
                                        EstadoRelevo.OBSERVADO,
                                        "Faltan dos conos",
                                        8
                                )
                        ),
                        List.of(
                                new GestionarRelevoUseCase.ViaItem(
                                        via1.getId(),
                                        EstadoRelevo.OBSERVADO,
                                        "Lector intermitente"
                                )
                        )
                )
        );

        assertNotNull(registrado.id());
        assertEquals(plaza.getId(), registrado.plazaId());
        assertEquals(2, registrado.checklist().size());
        assertEquals(1, registrado.vias().size());

        assertEquals(1, relevoRepository.count());
        assertEquals(
                2,
                checklistRepository
                        .findByRelevoIdOrderByElementoCategoriaAscElementoOrdenAsc(
                                registrado.id()
                        )
                        .size()
        );
        assertEquals(
                1,
                relevoViaRepository
                        .findByRelevoIdOrderByViaNumeroAsc(
                                registrado.id()
                        )
                        .size()
        );

        var listado = consultaService.listar(
                LocalDate.of(2026, 9, 23),
                LocalDate.of(2026, 9, 23)
        );

        assertEquals(1, listado.size());
        assertEquals(registrado.id(), listado.get(0).id());
    }

    @Test
    void actualizaChecklistYViaSinDuplicarRegistros() {
        var inicial = gestionarService.registrar(
                command(
                        checklistOperativo(),
                        List.of(
                                new GestionarRelevoUseCase.ViaItem(
                                        via1.getId(),
                                        EstadoRelevo.OPERATIVO,
                                        null
                                )
                        )
                )
        );

        var actualizado = gestionarService.actualizar(
                inicial.id(),
                command(
                        List.of(
                                new GestionarRelevoUseCase.ChecklistItem(
                                        banos.getId(),
                                        EstadoRelevo.OBSERVADO,
                                        "Lavadero con fuga",
                                        null
                                ),
                                new GestionarRelevoUseCase.ChecklistItem(
                                        conos.getId(),
                                        EstadoRelevo.OPERATIVO,
                                        null,
                                        10
                                )
                        ),
                        List.of(
                                new GestionarRelevoUseCase.ViaItem(
                                        via1.getId(),
                                        EstadoRelevo.NO_OPERATIVO,
                                        "Vía cerrada"
                                )
                        )
                )
        );

        assertEquals(inicial.id(), actualizado.id());
        assertEquals(1, relevoRepository.count());
        assertEquals(
                2,
                checklistRepository
                        .findByRelevoIdOrderByElementoCategoriaAscElementoOrdenAsc(
                                inicial.id()
                        )
                        .size()
        );
        assertEquals(
                1,
                relevoViaRepository
                        .findByRelevoIdOrderByViaNumeroAsc(
                                inicial.id()
                        )
                        .size()
        );

        assertEquals(
                EstadoRelevo.OBSERVADO,
                actualizado.checklist()
                        .stream()
                        .filter(item -> item.elementoId().equals(banos.getId()))
                        .findFirst()
                        .orElseThrow()
                        .estado()
        );

        assertEquals(
                EstadoRelevo.NO_OPERATIVO,
                actualizado.vias().get(0).estado()
        );
    }

    @Test
    void actualizarSinViasEliminaReporteDeViaAnterior() {
        var inicial = gestionarService.registrar(
                command(
                        checklistOperativo(),
                        List.of(
                                new GestionarRelevoUseCase.ViaItem(
                                        via1.getId(),
                                        EstadoRelevo.OPERATIVO,
                                        null
                                )
                        )
                )
        );

        assertEquals(1, inicial.vias().size());

        var actualizado = gestionarService.actualizar(
                inicial.id(),
                command(
                        checklistOperativo(),
                        List.of()
                )
        );

        assertTrue(actualizado.vias().isEmpty());
        assertTrue(
                relevoViaRepository
                        .findByRelevoIdOrderByViaNumeroAsc(
                                inicial.id()
                        )
                        .isEmpty()
        );
    }

    @Test
    void actualizarDespuesDeDesactivarElementoEliminaChecklistObsoleto() {
        var inicial = gestionarService.registrar(
                command(
                        checklistOperativo(),
                        List.of()
                )
        );

        assertEquals(2, inicial.checklist().size());

        conos.setActivo(false);
        entityManager.persistAndFlush(conos);

        var actualizado = gestionarService.actualizar(
                inicial.id(),
                command(
                        List.of(
                                new GestionarRelevoUseCase.ChecklistItem(
                                        banos.getId(),
                                        EstadoRelevo.OPERATIVO,
                                        null,
                                        null
                                )
                        ),
                        List.of()
                )
        );

        assertEquals(1, actualizado.checklist().size());
        assertEquals(
                banos.getId(),
                actualizado.checklist().get(0).elementoId()
        );

        assertEquals(
                1,
                checklistRepository
                        .findByRelevoIdOrderByElementoCategoriaAscElementoOrdenAsc(
                                inicial.id()
                        )
                        .size()
        );
    }

    @Test
    void rechazaChecklistIncompletoSinPersistirRelevo() {
        var incompleto = command(
                List.of(
                        new GestionarRelevoUseCase.ChecklistItem(
                                banos.getId(),
                                EstadoRelevo.OPERATIVO,
                                null,
                                null
                        )
                ),
                List.of()
        );

        assertThrows(
                BusinessException.class,
                () -> gestionarService.registrar(incompleto)
        );

        assertEquals(0, relevoRepository.count());
    }

    @Test
    void rechazaViaDeOtraPlazaSinPersistirRelevo() {
        var invalido = command(
                checklistOperativo(),
                List.of(
                        new GestionarRelevoUseCase.ViaItem(
                                viaOtraPlaza.getId(),
                                EstadoRelevo.OPERATIVO,
                                null
                        )
                )
        );

        assertThrows(
                BusinessException.class,
                () -> gestionarService.registrar(invalido)
        );

        assertEquals(0, relevoRepository.count());
    }

    @Test
    void evidenciaChecklistYViaSePersisteConsultaYElimina()
            throws IOException {
        var registrado = gestionarService.registrar(
                command(
                        checklistOperativo(),
                        List.of(
                                new GestionarRelevoUseCase.ViaItem(
                                        via1.getId(),
                                        EstadoRelevo.OPERATIVO,
                                        null
                                )
                        )
                )
        );

        Long checklistId = registrado.checklist().get(0).id();
        Long relevoViaId = registrado.vias().get(0).id();

        StubStorage storage = new StubStorage();

        GestionarEvidenciaRelevoService evidenciaService =
                new GestionarEvidenciaRelevoService(
                        evidenciaAdapter,
                        storage
                );

        var checklistEvidencia =
                evidenciaService.guardarChecklist(
                        checklistId,
                        archivo("checklist.jpg")
                );

        var viaEvidencia =
                evidenciaService.guardarVia(
                        relevoViaId,
                        archivo("via.jpg")
                );

        assertNotNull(checklistEvidencia.id());
        assertNotNull(viaEvidencia.id());
        assertEquals(1, checklistEvidenciaRepository.count());
        assertEquals(1, viaEvidenciaRepository.count());

        var consultado = consultaService.obtener(registrado.id());

        assertEquals(
                1,
                consultado.checklist()
                        .stream()
                        .filter(item -> item.id().equals(checklistId))
                        .findFirst()
                        .orElseThrow()
                        .evidencias()
                        .size()
        );

        assertEquals(
                1,
                consultado.vias().get(0).evidencias().size()
        );

        evidenciaService.eliminarChecklist(
                checklistId,
                checklistEvidencia.id()
        );

        evidenciaService.eliminarVia(
                relevoViaId,
                viaEvidencia.id()
        );

        assertEquals(0, checklistEvidenciaRepository.count());
        assertEquals(0, viaEvidenciaRepository.count());
        assertEquals(2, storage.eliminados);
    }

    private GestionarRelevoUseCase.Command command(
            List<GestionarRelevoUseCase.ChecklistItem> checklist,
            List<GestionarRelevoUseCase.ViaItem> vias
    ) {
        return new GestionarRelevoUseCase.Command(
                plaza.getId(),
                turno.getId(),
                operador.getId(),
                LocalDate.of(2026, 9, 23),
                LocalTime.of(6, 15),
                "  Sin novedades adicionales  ",
                "  Relevo registrado  ",
                checklist,
                vias
        );
    }

    private List<GestionarRelevoUseCase.ChecklistItem> checklistOperativo() {
        return List.of(
                new GestionarRelevoUseCase.ChecklistItem(
                        banos.getId(),
                        EstadoRelevo.OPERATIVO,
                        null,
                        null
                ),
                new GestionarRelevoUseCase.ChecklistItem(
                        conos.getId(),
                        EstadoRelevo.OPERATIVO,
                        null,
                        10
                )
        );
    }

    private GestionarEvidenciaRelevoUseCase.ArchivoEntrada archivo(
            String nombre
    ) {
        return new GestionarEvidenciaRelevoUseCase.ArchivoEntrada(
                nombre,
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
    }

    private static class StubStorage
            implements RelevoStoragePort {

        private int secuencia;
        private int eliminados;

        @Override
        public ArchivoSubido subir(
                GestionarEvidenciaRelevoUseCase.ArchivoEntrada archivo,
                String folder
        ) {
            secuencia++;
            return new ArchivoSubido(
                    "https://storage.test/" + secuencia + ".jpg",
                    "public-" + secuencia
            );
        }

        @Override
        public void eliminar(String publicId) {
            eliminados++;
        }
    }
}
