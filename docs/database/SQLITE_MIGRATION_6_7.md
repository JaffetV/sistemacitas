# SQLite: migración histórica segura 6 -> 7

## Qué cambia

- `DATABASE_VERSION` pasa de 6 a 7.
- Se elimina el comportamiento destructivo de `onUpgrade()`.
- La migración 6 -> 7 agrega únicamente índices; no elimina, recrea ni vuelve a sembrar tablas.
- Las instalaciones nuevas crean el esquema actual y el catálogo inicial de servicios una sola vez.
- Se rechazan explícitamente versiones anteriores a 6 porque el repositorio entregado no contiene un historial verificable de sus esquemas. Es preferible detener la actualización antes que borrar datos silenciosamente.
- Se rechazan degradaciones de versión.

## Índices agregados

- `idx_citas_fecha_inicio`
- `idx_citas_estado_fecha`
- `idx_citas_cliente_id`
- `idx_citas_servicio_id`
- `idx_clientes_telefono`
- `idx_servicios_activo`

## Prueba de migración

`CitasDbHelperMigrationTest` crea una base de datos de versión 6 con un cliente, un servicio y una cita. Al abrirla con la versión 7 verifica que los tres registros y el contenido de la cita siguen existiendo y que los índices fueron creados.

Ejecutar desde Android Studio en un emulador o dispositivo:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Antes de publicar

1. Ejecutar la prueba instrumentada en un dispositivo/emulador.
2. Instalar la versión actual sobre una instalación de prueba con datos.
3. Comprobar que clientes, servicios y citas continúan visibles.
4. No cambiar `applicationId` si se desea actualizar la misma aplicación instalada.
5. Mantener copias de seguridad externas antes de cualquier migración que altere columnas o datos.

Esta migración no sincroniza todavía con la nube ni resuelve reservas simultáneas; esas tareas pertenecen a las siguientes fases.


## Estado actual del esquema

Este documento describe únicamente el paso histórico 6 -> 7. La versión actual es SQLite **v8** y agrega el marketplace mediante la migración incremental 7 -> 8. Consulta `MODELO_MARKETPLACE_V8.md` para el esquema actual y sus reglas. La migración 7 -> 8 no elimina ni recrea las tablas operativas existentes.
