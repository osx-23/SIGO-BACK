# SIGO — Pedidos mensuales de materiales (desarrollo)

Implementación en la rama develop.

## Alcance
- Un pedido por plaza y mes (periodo AAAA-MM), preparado por controlador o supervisor.
- Catálogo de productos habilitados para la plaza.
- Sugerencias referenciales: promedio de consumo calculado solo a partir de **dos conteos finales consecutivos** más las recepciones del intervalo.
- Sin historial suficiente, no se inventa promedio ni pedido sugerido. Se registra cantidad manualmente.
- Envío a revisión, aprobación/rechazo de supervisor, consolidado de pedidos aprobados por material y recepción parcial o completa.
- Las recepciones tienen identificador UUID para evitar que un reintento duplique cantidades.
- El stock final es **último conteo finalizado + recepciones posteriores al conteo**.
- Los pedidos del mes están sujetos a fecha límite interna día 8, mostrada como advertencia; no bloquea pedidos atrasados.

## Migración
Se versionó en:
\`supabase/migrations/20261015210000_inventario_pedidos_mensuales.sql\`

Aplicada a SIGO-TEST el 15/10/2026. **No reaplicar manualmente sin verificar primero historial de migraciones**. Se crean:
- \`inventario_pedido\`
- \`inventario_pedido_detalle\`
- \`inventario_pedido_recepcion\`
- \`inventario_pedido_recepcion_detalle\`
- Vista actualizada \`v_inventario_stock_actual\`
- Vista nueva \`v_inventario_consumo_mensual\`

El backend debe conectar con PostgreSQL usando el rol de aplicación del servicio. Las cuatro tablas nuevas tienen RLS habilitada y acceso anónimo/autenticado revocado. La política de tablas antiguas se gestiona por separado.

## Estados
\`BORRADOR → PENDIENTE → APROBADO → ENVIADO → PARCIAL → COMPLETADO\`

Variantes: \`RECHAZADO\` vuelve a borrador por edición y nuevo envío; \`CERRADO_FALTANTES\` lo registra el supervisor cuando se decide que no llegarán las unidades pendientes. \`ENVIADO\` significa marcado como consolidado para el flujo interno; **no se transmite un mensaje, archivo o correo automáticamente** a Planificación.

## API (JWT)
Base \`/api/inventario/pedidos\`. Solo SUPERVISOR y CONTROLADOR. El controlador solo accede a su plaza.

| Método | Endpoint | Acción |
|---|---|---|
| GET | \`/sugerencias?plazaId=1&periodo=2026-11\` | Stock, promedio real y sugerencia |
| GET | \`/consumo?plazaId=1\` | Histórico verificable |
| GET | \`/?periodo=2026-11\` | Listado (supervisor todas las plazas) |
| GET | \`/{id}\` | Detalle con faltantes y recepciones |
| POST | \`/\` | Crear borrador |
| PUT | \`/{id}\` | Editar borrador/rechazado |
| POST | \`/{id}/enviar\` | Enviar para aprobación |
| POST | \`/{id}/aprobar\` | Supervisor |
| POST | \`/{id}/rechazar\` | Supervisor |
| GET | \`/consolidado?periodo=2026-11\` | Consolidación por material, supervisor |
| POST | \`/consolidar?periodo=2026-11\` | Marcar aprobados como consolidados |
| POST | \`/{id}/recepciones\` | Agregar entrada parcial/completa |
| POST | \`/{id}/cerrar-faltantes\` | Cerrar incompleto, supervisor |

### Ejemplo de pedido
\`\`\`json
{
  "plazaId": 1,
  "periodo": "2026-11",
  "observacion": "Pedido noviembre",
  "items": [
    {"productoId": 9,"cantidad": 30,"sugerida": null,"promedio": null,"stock": 0}
  ]
}
\`\`\`

### Ejemplo de recepción parcial
Enviar una UUID distinta por cada entrega real; **reutilizarla** al reintentar la misma entrega si falla la red.

\`\`\`json
{
  "claveIdempotencia": "9c1c1c12-91e7-4d8a-882b-e8981c860001",
  "observacion": "Llegaron 10 de 30",
  "items": [{"productoId":9,"cantidad":10}]
}
\`\`\`

Se calcula cantidad pendiente sin alterar la solicitud original. Toda operación de recepción es transaccional y serializada por pedido (\`FOR UPDATE\`).

## Front
Ruta \`/inventario/pedidos\` (menú Inventario). Secciones:
Nuevo pedido, Seguimiento y recepción, Consumo histórico, Consolidado general (solo supervisor).

## Limitaciones / puesta en marcha
- El sistema todavía no envía el consolidado por correo ni se conecta al sistema del área de Planificación: debe compartirse por el canal actual.
- La persona concreta que puede aprobar se determina hoy por rol SUPERVISOR, no por un trabajador específico. Confirmar si la autorización debe restringirse a una única cuenta.
- Para sugerencias confiables se necesitan conteos y recepciones registrados; el inventario histórico en Excel puede requerir migración revisada.
- Un stock sin conteo ni entrada se muestra como **sin conteo** en el formulario de pedidos, no como cero verificado.
- No desplegar cambios de develop en producción antes de completar las pruebas de backend y frontend.
