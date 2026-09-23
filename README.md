# SIGO - Asistencia API

Proyecto Spring Boot para el módulo de asistencia de SIGO usando Supabase PostgreSQL.

## Requisitos
- Java 21
- Maven 3.9+
- Proyecto Supabase

## Base de datos
Ejecuta `src/main/resources/schema.sql` en Supabase SQL Editor.

## Configuración
Obtén desde Supabase la conexión PostgreSQL/Session Pooler y define:

```bash
export DATABASE_URL='jdbc:postgresql://HOST:PUERTO/postgres'
export DATABASE_USER='TU_USUARIO'
export DATABASE_PASSWORD='TU_PASSWORD'
export CORS_ALLOWED_ORIGINS='http://localhost:5173'
```

Para el frontend desplegado puedes usar:
`CORS_ALLOWED_ORIGINS=https://sigo-lima-expresa.vercel.app`

No uses la Publishable Key como contraseña de PostgreSQL.

## Ejecutar
```bash
mvn spring-boot:run
```

## Endpoints
- GET `/api/plazas`
- GET `/api/turnos`
- GET `/api/motivos-ausencia`
- GET `/api/trabajadores`
- GET `/api/trabajadores?puesto=Controlador`
- GET `/api/trabajadores/codigo/{codigo}`
- POST `/api/asistencias`
- GET `/api/asistencias`
- GET `/api/asistencias/{id}`
- GET `/api/dashboard/asistencia/diario?anio=2026&mes=8`
- GET `/api/dashboard/asistencia/anual?anio=2026`
- GET `/api/dashboard/asistencia/ausencias-motivo?anio=2026&mes=8`
- GET `/api/dashboard/asistencia/resumen`

## Ejemplo POST
```json
{
  "plazaId": 1,
  "turnoId": 1,
  "controladorId": 2,
  "fecha": "2026-08-29",
  "presentes": 5,
  "notas": "Turno registrado correctamente",
  "ausencias": [
    {"trabajadorId": 10, "motivoId": 3, "observacion": "Descanso médico"},
    {"trabajadorId": 11, "motivoId": 15, "observacion": "Vacaciones"}
  ],
  "evidencias": ["https://res.cloudinary.com/tu-cloud/image/upload/foto1.jpg"]
}
```

El backend obtiene `programados` del turno, valida que ausencias = programados - presentes, impide duplicar plaza+turno+fecha y usa porcentaje ponderado para los dashboards.


## Autenticación JWT
- POST `/api/auth/login`
- GET `/api/auth/me`

Para configurar las cuentas de prueba en SIGO-TEST consulta `README_LOGIN_TEST.md`.
