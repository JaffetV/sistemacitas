# Plan de evolución — UNAMAD Citas Marketplace

## v8 — Marketplace local

- Portal inicial que separa cliente, alta de negocio y agenda clásica.
- Registro local gratuito de clientes y alta automática de negocios.
- Prueba comercial de 14 días.
- Publicaciones, ofertas, búsqueda local y reserva de cita/recogida.
- Migración SQLite 7 -> 8 conservando agenda anterior.

## v9 — Primera capa multiempresa de servidor

- API REST Node.js 22 con PostgreSQL 16.
- Registro y login con hash scrypt, JWT de 12 horas y límites iniciales por IP.
- RLS en tablas privadas y comprobación de membresía del negocio en API.
- Catálogo, horarios, publicaciones, reservas, favoritos y estados.
- Restricción de exclusión para evitar solapamientos concurrentes por negocio.
- Inventario transaccional y devolución de stock por cancelación de recogida.
- Docker Compose, migración inicial SQL, documentación y OpenAPI.
- Cliente Android HTTP y almacenamiento cifrado de token con Android Keystore.

## Siguiente iteración — Integración de la app y servidor

1. Añadir pantallas de registro/login remoto de clientes y propietarios, con selección de negocio activo.
2. Conectar `MarketplaceRepository`/ViewModels a API y sincronizar el marketplace; conservar SQLite como caché/offline, sin mezclar datos de negocios.
3. Migrar panel de publicaciones, horarios y reservas para usar identificadores UUID del servidor.
4. Incorporar calendario por trabajador/recurso para permitir atenciones paralelas en un mismo negocio.
5. Implementar verificación y recuperación de cuenta, cierre/revocación de sesiones y pruebas PostgreSQL/RLS.

## Después de integrar el cliente

- Pasarela de pagos y webhooks de confirmación, reembolso y conciliación.
- Tarifario comercial, cobro/renovación y suspensión por vencimiento.
- Notificaciones push, email/SMS y recordatorios.
- Excepciones de horario, vacaciones y políticas de cancelación/no-show.
- Imágenes, moderación, panel de administración y métricas.
- Monitoreo, backups y pruebas de carga.

## Condiciones conocidas

El backend v9 está entregado como módulo ejecutable, pero no se desplegó aquí porque no hay Docker ni PostgreSQL disponible. Las pantallas Android continúan usando SQLite local; el adaptador API ya existe pero la integración de UI queda para la siguiente iteración. Las pruebas de Node y la compilación aislada del adaptador Kotlin sí se ejecutaron.
