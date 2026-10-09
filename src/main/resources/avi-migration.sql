-- AVIX: configuración central de vías y acción LIBERADO.
-- Ejecutar en SIGO-TEST / producción antes de desplegar con ddl-auto=validate.
-- Idempotente: puede ejecutarse más de una vez.

do $$
begin
    if to_regclass('public.vias') is not null then
        alter table public.vias
            add column if not exists avi_visible boolean not null default true;

        update public.vias
        set avi_visible = true
        where avi_visible is null;
    end if;
end
$$;

do $$
begin
    if to_regclass('public.avi_registros') is not null then
        alter table public.avi_registros
            drop constraint if exists avi_registros_accion_check;

        alter table public.avi_registros
            add constraint avi_registros_accion_check
            check (accion in ('FUGA', 'DERIVADO', 'LIBERADO'));
    end if;
end
$$;
