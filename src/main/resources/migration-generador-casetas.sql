-- Generador de asignación de casetas
-- Ejecutar en SIGO-TEST / producción antes de desplegar el backend con ddl-auto=validate.

create table if not exists configuracion_asignacion_caseta (
    id bigint generated always as identity primary key,
    plaza_id bigint not null unique references plazas(id),
    max_misma_caseta_semana integer not null default 3,
    max_misma_caseta_mes integer not null default 8,
    max_consecutivos integer not null default 2,
    balancear_flujo boolean not null default true,
    constraint chk_config_caseta_semana check (max_misma_caseta_semana > 0),
    constraint chk_config_caseta_mes check (max_misma_caseta_mes > 0),
    constraint chk_config_caseta_consecutivos check (max_consecutivos > 0)
);

create table if not exists configuracion_caseta (
    id bigint generated always as identity primary key,
    ubicacion_id bigint not null unique references programacion_ubicacion(id) on delete cascade,
    grupo_flujo varchar(30) not null default 'SIN_CLASIFICAR',
    max_semana integer,
    max_mes integer,
    constraint chk_config_caseta_grupo check (
        grupo_flujo in ('ALTO_FLUJO','BAJO_FLUJO','SIN_CLASIFICAR')
    ),
    constraint chk_config_caseta_max_semana check (
        max_semana is null or max_semana > 0
    ),
    constraint chk_config_caseta_max_mes check (
        max_mes is null or max_mes > 0
    )
);

create table if not exists agente_caseta_restriccion (
    id bigint generated always as identity primary key,
    trabajador_id bigint not null references trabajadores(id) on delete cascade,
    ubicacion_id bigint not null references programacion_ubicacion(id) on delete cascade,
    motivo varchar(250),
    activo boolean not null default true,
    constraint uq_agente_caseta_restriccion unique (trabajador_id, ubicacion_id)
);

create index if not exists idx_config_caseta_ubicacion
    on configuracion_caseta(ubicacion_id);

create index if not exists idx_restriccion_trabajador
    on agente_caseta_restriccion(trabajador_id);

create index if not exists idx_restriccion_ubicacion
    on agente_caseta_restriccion(ubicacion_id);
