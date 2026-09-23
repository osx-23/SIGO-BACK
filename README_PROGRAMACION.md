# SIGO - Programación / Distribución de Personal

Esta rama parte del backend completo actual de `SIGO-BACK-PROD/main` y agrega el módulo de programación/distribución sin modificar producción.

## Reglas implementadas

- **Supervisor**
  - Programa turno/estado diario de agentes: `A`, `B`, `C`, `D`, `V`, `COM`, `DM`, `LIC`.
  - Crea y modifica grupos fijos de trabajo y su líder/controlador.
  - Cuando cambia una jornada a un estado no operativo se eliminan automáticamente la vía/caseta de ese día.

- **Controlador**
  - Solo asigna casetas/vías a agentes de su propia plaza.
  - Todos los controladores de una plaza pueden editar la distribución de esa plaza.
  - Solo puede asignar caseta cuando el agente tiene jornada `A`, `B` o `C`.
  - Puede usar una vía del catálogo existente o una posición especial: `AUX 1`, `AUX 2`, `AUX 3`, `APPMOVIL`.

- **Agente / OPERADOR**
  - Consulta únicamente su horario semanal mediante el usuario autenticado.
  - La respuesta incluye su líder fijo cuando pertenece a un grupo.

## Base de datos local / SIGO-TEST

Antes de arrancar el backend con `spring.jpa.hibernate.ddl-auto=validate`, ejecuta:

```text
src/main/resources/programacion-migration.sql
```

en el SQL Editor de **SIGO-TEST**.

El módulo reutiliza:
- `trabajadores`
- `plazas`
- `vias`
- autenticación JWT existente

y crea:
- `programacion_grupo`
- `programacion_grupo_miembro`
- `programacion_dia`

## Endpoints

### Agente

```http
GET /api/programacion/me/semana
GET /api/programacion/me/semana?inicio=2026-09-14
```

`inicio` se normaliza al lunes de esa semana.

### Supervisor - jornadas

```http
PUT /api/programacion/supervisor/jornadas
Content-Type: application/json
Authorization: Bearer <token>
```

Ejemplo:

```json
[
  {
    "trabajadorId": 10,
    "fecha": "2026-09-21",
    "codigo": "A"
  },
  {
    "trabajadorId": 11,
    "fecha": "2026-09-21",
    "codigo": "D"
  }
]
```

### Controlador - casetas

```http
PUT /api/programacion/controlador/asignaciones
```

Vía normal:

```json
[
  {
    "trabajadorId": 10,
    "fecha": "2026-09-21",
    "viaId": 5,
    "posicionEspecial": null
  }
]
```

Posición especial:

```json
[
  {
    "trabajadorId": 10,
    "fecha": "2026-09-21",
    "viaId": null,
    "posicionEspecial": "AUX 1"
  }
]
```

### Vista mensual y cobertura

```http
GET /api/programacion/plazas/{plazaId}/mes?anio=2026&mes=9
GET /api/programacion/plazas/{plazaId}/cobertura?anio=2026&mes=9
```

### Grupos fijos

```http
GET    /api/programacion/grupos?plazaId=3
POST   /api/programacion/grupos
PUT    /api/programacion/grupos/{id}
DELETE /api/programacion/grupos/{id}
```

Ejemplo:

```json
{
  "plazaId": 3,
  "nombre": "Grupo P4 - A",
  "controladorId": 25,
  "miembroIds": [41, 42, 43, 44]
}
```

## Arranque local

Configura las mismas variables del archivo `.env.example`, apuntando a SIGO-TEST, y ejecuta:

```bash
mvn spring-boot:run
```

El backend sigue incluyendo Asistencia, Relevos, Inventario, Chat, Personal y Seguridad; Programación se agrega como un módulo nuevo.


> Rama de trabajo local: `chatgpt/programacion-distribucion-local`.
