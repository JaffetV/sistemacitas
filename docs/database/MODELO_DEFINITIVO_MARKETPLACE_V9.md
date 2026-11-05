# Modelo definitivo — UNAMAD Citas Marketplace v9

## 1. Visión del producto

Marketplace local, multisectorial y orientado a citas, reservas y pedidos para recoger. El cliente puede usar la plataforma gratuitamente y contratar con varios negocios desde la misma cuenta. Cada negocio es una unidad de administración independiente y contrata su propia suscripción comercial.

## 2. Actores y permisos

| Actor | Acceso | Responsabilidad |
|---|---|---|
| Cliente | Gratuito | Explorar, guardar favoritos, reservar y consultar sus solicitudes |
| Propietario | Cuenta + negocio | Crear uno o más negocios y administrar su suscripción |
| Administrador de negocio | Invitación futura | Administrar catálogo, horarios y reservas según permisos |
| Personal | Invitación futura | Operación de reservas con permisos limitados |
| Plataforma | Administración global futura | Moderación, planes, soporte y conciliación |

Una cuenta de usuario puede ser cliente y propietario simultáneamente. La identidad no se duplica al crear un negocio adicional. `business_members` separa las relaciones de acceso de la tabla `users`.

## 3. Modelo de monetización

- Los clientes no pagan mensualidad por explorar y reservar.
- Cada negocio tiene su propia suscripción.
- El alta crea un plan de prueba de 14 días.
- Los planes Básico y Profesional están definidos, pero el precio queda `NULL` hasta aprobar el tarifario.
- El canal de pago de la reserva puede ser `EN_LINEA` o `EN_LOCAL`.
- Un pago `EN_LINEA` queda `PENDIENTE` hasta que se integre y confirme una pasarela con webhooks. La API no inventa confirmaciones de cobro.

## 4. Catálogo de publicaciones

`categories` es extensible y arranca con barbería/peluquería, belleza/uñas, salud/bienestar, comida/restaurantes, talleres/mantenimiento, tiendas/productos, educación/asesorías y otros.

`listings` representa servicios o productos:

- `SERVICIO`: cita (`CITA`) o cita y recogida (`AMBOS` cuando el negocio habilita ambas modalidades).
- `PRODUCTO`: recogida (`RECOJO`).
- Precio actual y precio comparativo para ofertas.
- Duración para servicios.
- Stock opcional para productos; `NULL` significa stock no controlado.

Cada publicación debe respetar las modalidades activadas por el negocio en `business_modes`.

## 5. Reservas y disponibilidad

`reservations` unifica citas y pedidos, pero sus datos temporales no se mezclan:

- Cita: `starts_at` y `ends_at`.
- Recogida: `pickup_at`.

La base PostgreSQL incluye una restricción de exclusión GiST que evita solapamientos para citas del mismo negocio, incluso ante dos peticiones concurrentes. Esta primera versión bloquea un horario por negocio; todavía no diferencia profesionales, cabinas o recursos. El siguiente incremento será incorporar recursos/calendarios por trabajador.

Los horarios semanales se guardan en `business_hours`, con zona horaria inicial `America/Lima`. Si un negocio todavía no configura horarios, la API permite solicitudes futuras; al configurarlos, las citas y recogidas deben quedar dentro de la atención declarada. Los días festivos, pausas, vacaciones y excepciones por fecha están pendientes.

Los pedidos con stock descuentan inventario dentro de la misma transacción que crea la reserva. Si la solicitud falla, la transacción revierte el descuento. Si la reserva de recogida se cancela, el stock se devuelve. Pagos ya capturados necesitarán una futura política de reembolso.

## 6. Aislamiento multiempresa

Las tablas privadas activan Row-Level Security (RLS). La API abre una transacción por operación y establece el identificador autenticado en `app.user_id`. Las políticas permiten:

- Lectura pública solo de negocios y publicaciones activos con suscripción vigente.
- Administración solo a miembros autorizados del negocio.
- Consulta de reservas por el cliente que la creó o por el negocio que la recibió.
- Consulta de pagos por el cliente o el negocio relacionado.
- Favoritos propios del usuario.

Los identificadores de negocio enviados por el cliente nunca sustituyen la comprobación de membresía en el servidor.

## 7. Arquitectura desplegable

```text
Android (agenda clásica + marketplace local actual)
        |
        | HTTPS / JSON / Bearer JWT (integración Android pendiente)
        v
API Node.js 22 + PostgreSQL 16
        |
        +-- Auth: registro, login, JWT y hash scrypt
        +-- Marketplace público: categorías, negocios y publicaciones
        +-- Tenant API: catálogo, horarios y reservas por negocio
        +-- Reservas: solapamientos, stock y estados transaccionales
        +-- RLS: aislamiento a nivel de base de datos
```

En v9 se entrega la API y el esquema de servidor. La app Android conserva la persistencia local de v8: sus pantallas aún no llaman a la API, por lo que no hay sincronización entre teléfonos hasta una siguiente fase de integración.

## 8. Entidades principales

- `users`
- `categories`
- `businesses`
- `business_members`
- `plans`
- `subscriptions`
- `business_modes`
- `listings`
- `business_hours`
- `reservations`
- `payments`
- `favorites`

## 9. Pendientes antes de producción

1. Integrar la app Android con registro/login, almacenamiento seguro del token y sincronización.
2. Sustituir el despliegue de desarrollo por HTTPS, secretos gestionados, copias de seguridad y migraciones versionadas.
3. Implementar verificación de correo/teléfono, recuperación de contraseña, revocación/renovación de sesiones y límites de abuso distribuidos.
4. Definir precios, límites y cobro/renovación de suscripciones.
5. Integrar pasarela, webhooks, conciliación y reembolsos.
6. Añadir recursos/profesionales, excepciones horarias y reglas configurables de cancelación/no-show.
7. Añadir notificaciones push, imágenes/ficheros, moderación y observabilidad.
8. Probar carga, RLS y migraciones en un entorno PostgreSQL real antes de producción.
