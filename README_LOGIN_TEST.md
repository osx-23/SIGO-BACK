# Login JWT en SIGO-TEST

Esta versión usa la tabla `trabajadores` como identidad del sistema.

## Usuarios preparados en SIGO-TEST

- OPERADOR: código `2396`
- CONTROLADOR: código `287`
- SUPERVISOR: código `550`

El usuario `550` conserva su puesto original en la tabla de personal; únicamente `rol_sistema` fue marcado como `SUPERVISOR` en SIGO-TEST para probar permisos.

## 1. Configura la conexión a SIGO-TEST

Define `DATABASE_URL`, `DATABASE_USER` y `DATABASE_PASSWORD` con las credenciales PostgreSQL de **SIGO-TEST**, nunca con las de producción.

Ejemplo de formato:

```bash
export DATABASE_URL='jdbc:postgresql://HOST:PUERTO/postgres'
export DATABASE_USER='USUARIO'
export DATABASE_PASSWORD='PASSWORD'
```

## 2. Configura JWT

```bash
export JWT_SECRET='un-secreto-aleatorio-de-al-menos-32-bytes'
export JWT_ISSUER='sigo-api-test'
export JWT_EXPIRATION_SECONDS='28800'
```

## 3. Inicializa las tres contraseñas de prueba

El bootstrap está desactivado por defecto y solo escribe un hash si `password_hash` está vacío.

```bash
export SIGO_TEST_BOOTSTRAP_ENABLED='true'
export SIGO_TEST_OPERADOR_PASSWORD='TestOp2026!'
export SIGO_TEST_CONTROLADOR_PASSWORD='TestCtrl2026!'
export SIGO_TEST_SUPERVISOR_PASSWORD='TestSup2026!'
```

Arranca la aplicación una vez. Spring genera BCrypt y guarda solamente el hash en `trabajadores.password_hash`.

Después del primer arranque correcto:

```bash
export SIGO_TEST_BOOTSTRAP_ENABLED='false'
```

Así las contraseñas no se vuelven a inicializar.

## 4. Probar login

### Operador

```http
POST /api/auth/login
Content-Type: application/json

{
  "codigo": 2396,
  "password": "TestOp2026!"
}
```

### Controlador

```json
{
  "codigo": 287,
  "password": "TestCtrl2026!"
}
```

### Supervisor

```json
{
  "codigo": 550,
  "password": "TestSup2026!"
}
```

La respuesta incluye `token`, `tipo`, `expiresIn`, los datos del trabajador, su plaza, rol y módulos habilitados.

## 5. Probar el JWT

```http
GET /api/auth/me
Authorization: Bearer TU_TOKEN
```

## Roles y módulos actuales

### SUPERVISOR
- DASHBOARD
- RELEVOS
- ASISTENCIA
- INVENTARIO
- ADMIN_PRODUCTOS
- TRABAJADORES
- CHAT

### CONTROLADOR
- DASHBOARD
- RELEVOS
- ASISTENCIA
- INVENTARIO
- ADMIN_PRODUCTOS
- CHAT

### OPERADOR
- RELEVOS
- INVENTARIO

## Seguridad

- La contraseña nunca se almacena en texto plano.
- Se usa BCrypt mediante `PasswordEncoder`.
- El API es stateless.
- Spring Security valida JWT HS256.
- Un trabajador con `activo=false` no puede iniciar sesión ni seguir siendo resuelto como usuario activo mediante `/api/auth/me`.
- El bootstrap de pruebas debe permanecer desactivado en producción.
