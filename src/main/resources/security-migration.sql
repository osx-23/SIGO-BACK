-- Seguridad SIGO: ejecutar sobre SIGO-TEST antes de probar autenticación.
-- En producción debe aplicarse mediante una migración controlada.

alter table public.trabajadores
    add column if not exists rol_sistema varchar(30),
    add column if not exists password_hash varchar(100);

update public.trabajadores t
set rol_sistema = case
    when lower(p.nombre) like '%supervisor%' then 'SUPERVISOR'
    when lower(p.nombre) like '%controlador%' then 'CONTROLADOR'
    else 'OPERADOR'
end
from public.puestos p
where t.puesto_id = p.id
  and t.rol_sistema is null;

alter table public.trabajadores
    alter column rol_sistema set default 'OPERADOR';

-- rol_sistema sí puede ser obligatorio desde el inicio.
alter table public.trabajadores
    alter column rol_sistema set not null;

-- password_hash queda NULL mientras se habilitan cuentas gradualmente.
-- AuthService rechaza el login de cualquier trabajador sin password_hash.
-- Cuando TODOS los usuarios tengan contraseña:
-- alter table public.trabajadores alter column password_hash set not null;
