create table if not exists plazas (
  id bigint generated always as identity primary key,
  codigo varchar(20) not null unique,
  descripcion varchar(100),
  activo boolean not null default true
);
create table if not exists turnos (
  id bigint generated always as identity primary key,
  codigo char(1) not null unique,
  nombre varchar(30),
  personal_programado int not null,
  constraint chk_turno_programado check (personal_programado > 0)
);
create table if not exists puestos (
  id bigint generated always as identity primary key,
  nombre varchar(60) not null unique
);
create table if not exists trabajadores (
  id bigint generated always as identity primary key,
  codigo int not null unique,
  nombre_completo varchar(150) not null,
  puesto_id bigint not null references puestos(id),
  plaza_id bigint references plazas(id),
  activo boolean not null default true
);
create table if not exists motivos_ausencia (
  id bigint generated always as identity primary key,
  nombre varchar(60) not null unique
);
create table if not exists asistencia_registro (
  id bigint generated always as identity primary key,
  plaza_id bigint not null references plazas(id),
  turno_id bigint not null references turnos(id),
  controlador_id bigint not null references trabajadores(id),
  fecha date not null,
  programados int not null,
  presentes int not null,
  porcentaje numeric(5,2) generated always as (round(presentes::numeric / nullif(programados,0) * 100,2)) stored,
  notas text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uq_plaza_turno_fecha unique (plaza_id, turno_id, fecha),
  constraint chk_programados_positivo check (programados > 0),
  constraint chk_presentes_validos check (presentes >= 0 and presentes <= programados)
);
create index if not exists idx_asistencia_fecha on asistencia_registro(fecha);
create index if not exists idx_asistencia_plaza_turno on asistencia_registro(plaza_id,turno_id);
create or replace function set_updated_at() returns trigger as $$ begin new.updated_at=now(); return new; end; $$ language plpgsql;
drop trigger if exists trg_asistencia_updated_at on asistencia_registro;
create trigger trg_asistencia_updated_at before update on asistencia_registro for each row execute function set_updated_at();
create table if not exists asistencia_ausencia (
  id bigint generated always as identity primary key,
  asistencia_id bigint not null references asistencia_registro(id) on delete cascade,
  trabajador_id bigint not null references trabajadores(id),
  motivo_id bigint not null references motivos_ausencia(id),
  observacion varchar(255),
  constraint uq_asistencia_trabajador unique(asistencia_id,trabajador_id)
);
create index if not exists idx_ausencia_motivo on asistencia_ausencia(motivo_id);
create table if not exists asistencia_evidencia (
  id bigint generated always as identity primary key,
  asistencia_id bigint not null references asistencia_registro(id) on delete cascade,
  url_archivo varchar(500) not null,
  tipo varchar(20) not null default 'foto',
  subido_en timestamptz not null default now()
);
insert into plazas(codigo,descripcion) values
('P1','Plaza 1'),('P2 y P3','Plaza 2 y 3'),('P4','Plaza 4'),('P5','Plaza 5'),('P6 y P7','Plaza 6 y 7'),('P8','Plaza 8'),('P9','Plaza 9'),('P10','Plaza 10') on conflict(codigo) do nothing;
insert into turnos(codigo,nombre,personal_programado) values ('A','Turno A',7),('B','Turno B',7),('C','Turno C',3) on conflict(codigo) do nothing;
insert into puestos(nombre) values ('Agente de Recaudación'),('Controlador'),('Supervisor') on conflict(nombre) do nothing;
insert into motivos_ausencia(nombre) values
('Compensación'),('Descanso'),('Descanso Médico'),('Falta Injustificada'),('Falta Justificada'),('Licencia con Goce Compensable'),('Licencia con Goce de Haberes'),('Licencia Materna'),('Licencia Paterna'),('Licencia Sindical'),('Licencia sin goce'),('Licencia por Onomástico'),('Retirado'),('Suspensión'),('Vacaciones') on conflict(nombre) do nothing;
