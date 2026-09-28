-- Regla de integridad:
-- una ubicación/caseta solo puede pertenecer a una persona
-- por plaza, fecha y turno.
--
-- El lock transaccional evita carreras entre dos guardados concurrentes
-- que intenten ocupar la misma caseta al mismo tiempo.

create or replace function public.validar_caseta_unica_por_fecha_turno()
returns trigger
language plpgsql
as $$
declare
    v_plaza_id bigint;
    v_fecha date;
    v_estado text;
    v_lock_key bigint;
begin
    select
        p.plaza_id,
        p.fecha,
        p.estado::text
    into
        v_plaza_id,
        v_fecha,
        v_estado
    from public.programacion_turno p
    where p.id = new.programacion_turno_id;

    if v_plaza_id is null then
        raise exception 'Programación de turno inexistente: %',
            new.programacion_turno_id;
    end if;

    v_lock_key :=
        hashtextextended(
            v_plaza_id::text
                || '|'
                || v_fecha::text
                || '|'
                || v_estado
                || '|'
                || new.ubicacion_id::text,
            0
        );

    perform pg_advisory_xact_lock(v_lock_key);

    if exists (
        select 1
        from public.distribucion_personal d
        join public.programacion_turno p
          on p.id = d.programacion_turno_id
        where d.ubicacion_id = new.ubicacion_id
          and p.plaza_id = v_plaza_id
          and p.fecha = v_fecha
          and p.estado::text = v_estado
          and d.id is distinct from new.id
    ) then
        raise exception using
            errcode = '23505',
            message = format(
                'La caseta %s ya está ocupada en la plaza %s, fecha %s, turno %s',
                new.ubicacion_id,
                v_plaza_id,
                v_fecha,
                v_estado
            );
    end if;

    return new;
end;
$$;

drop trigger if exists trg_caseta_unica_por_fecha_turno
on public.distribucion_personal;

create trigger trg_caseta_unica_por_fecha_turno
before insert or update of programacion_turno_id, ubicacion_id
on public.distribucion_personal
for each row
execute function public.validar_caseta_unica_por_fecha_turno();
