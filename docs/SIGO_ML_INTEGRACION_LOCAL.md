# Integracion inicial SIGO-ML (experimental)

SIGO-BACK puede consultar la API Python de SIGO-ML desde un endpoint autenticado del Dashboard. No se cambia el calculo oficial de asistencia ni la base de datos.

## Requisitos

- Iniciar FastAPI en `localhost:8000` con el modelo entrenado.
- Ejecutar SIGO-BACK con la base de pruebas SIGO-TEST y un JWT autentico.
- Configurar las siguientes variables en IntelliJ (Run Configuration > Environment variables):

```
SIGO_ML_ENABLED=true
SIGO_ML_URL=http://localhost:8000
SIGO_ML_INTERNAL_TOKEN=<mismo token que usa el FastAPI local>
```

El token no debe subirse al repositorio. Por defecto la integracion esta deshabilitada. En produccion, usar direccion privada de red y credenciales diferentes.

## Endpoint de Spring Boot

`POST http://localhost:8080/api/dashboard/asistencia/predecir`

Headers:

- `Authorization: Bearer <JWT_DE_SIGO>`
- `Content-Type: application/json`

Body:

```json
{
  "plazaId": 3,
  "turnoId": 2,
  "fecha": "2026-11-05",
  "programados": 18
}
```

El backend reenvia estos datos a `POST /internal/v1/asistencia/predecir` del servicio SIGO-ML y devuelve la respuesta validada. Solo roles SUPERVISOR y CONTROLADOR tienen acceso al Dashboard.

## Limitaciones

- No se consulta Supabase en tiempo real desde SIGO-ML; el modelo actual se entreno con un CSV.
- Los pronosticos son experimentales. No automatizan la programacion del personal.
- `programados` debe coincidir con la programacion elegida: el backend aun no obtiene automaticamente el conteo desde la programacion.
- El endpoint no funciona hasta habilitar explicitamente SIGO-ML y configurar su token.
- Cuando FastAPI no este disponible, se devuelve HTTP 503 sin afectar otras rutas.
