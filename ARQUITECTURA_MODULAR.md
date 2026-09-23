# SIGO - Arquitectura de monolito modular

## Objetivo
SIGO sigue siendo una sola aplicación Spring Boot y un solo despliegue, pero el código se organiza por capacidades de negocio en lugar de capas técnicas globales.

## Módulos

- `security`: login, JWT, autenticación y autorización HTTP.
- `personal`: trabajadores, puestos, plazas, turnos y rol global del sistema.
- `asistencia`: registro de asistencia, ausencias, evidencias y dashboard de asistencia.
- `relevo`: relevos, checklist, vías y evidencias.
- `inventario`: productos, conteos, stock, catálogos y reglas internas de visibilidad.
- `chat`: asistente y consultas de solo lectura.
- `shared`: infraestructura transversal (Cloudinary, CORS, excepciones).

## Regla principal
Un módulo no debe consultar directamente repositorios internos de otro módulo salvo durante esta fase de migración. La siguiente iteración debe exponer servicios/API de módulo y sustituir esas referencias gradualmente.

## Seguridad

### Login
`POST /api/auth/login`

```json
{
  "codigo": 287,
  "password": "MiClaveSegura2026!"
}
```

La respuesta contiene un JWT Bearer y los módulos visibles para el usuario.

### Usuario actual
`GET /api/auth/me`

Header:
`Authorization: Bearer <token>`

### Roles globales
- `SUPERVISOR`
- `CONTROLADOR`
- `OPERADOR`

Los roles globales son independientes de `inventario_rol`, porque los roles/permisos de inventario son reglas internas de ese módulo.

### Navegación sugerida
- SUPERVISOR: dashboard, relevos, asistencia, inventario, administración de productos, trabajadores y chat.
- CONTROLADOR: dashboard, relevos, asistencia, inventario, administración de productos y chat.
- OPERADOR: relevos e inventario.

La navegación del frontend es una conveniencia de UX. La seguridad real siempre se valida en Spring Security y en las reglas de negocio del backend.

## Migración de base de datos
Ejecutar `src/main/resources/security-migration.sql` en Supabase antes de desplegar.

Después asignar contraseñas bcrypt a los usuarios y, cuando todos estén migrados, activar las restricciones NOT NULL indicadas en el mismo script.

## Variables de entorno

```properties
JWT_SECRET=<secreto aleatorio de al menos 32 bytes>
JWT_ISSUER=sigo-api
JWT_EXPIRATION_SECONDS=28800
```

Nunca subir el JWT_SECRET real a Git.

## Cambio importante en Inventario
Ya no se debe enviar `X-Usuario-Codigo`. El backend toma el trabajador autenticado desde Spring Security/JWT.

Antes:
`X-Usuario-Codigo: 287`

Ahora:
`Authorization: Bearer <jwt>`

## Próxima fase recomendada
1. Crear interfaces públicas por módulo.
2. Eliminar dependencias directas entre repositories de módulos.
3. Añadir Spring Modulith 1.3.x para validar la estructura con Spring Boot 3.4.x.
4. Añadir tests de autorización por rol y tests de módulos.
5. Implementar refresh token si la sesión debe durar más que el access token.
