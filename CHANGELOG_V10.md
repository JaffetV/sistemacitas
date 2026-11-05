# Cambios — Iteración integrada v10

## Base del proyecto

- Se toma como base el paquete v9 que ya incluía backend Node.js/PostgreSQL, cliente REST Android y migración SQLite 8.
- Se conserva la agenda clásica local (clientes, servicios, citas e ingresos).
- La migración SQLite es aditiva desde versiones 6 y 7; no se usa el `onUpgrade` destructivo del ZIP local v6.

## Integración Android

- Portal inicial renovado con identidad azul oscuro/verde y accesos a cliente, negocio, agenda, búsqueda, ofertas y reservas.
- Login remoto y alta de clientes.
- Alta de negocio remoto y alta adicional con una sesión existente.
- Selección y persistencia del negocio activo usando UUID del servidor.
- Exploración de negocios, publicaciones y ofertas desde la API.
- Solicitud y cancelación de reservas de cliente.
- Panel remoto de negocio para publicaciones, horario semanal y estados de reserva.
- El cambio de estado de reserva usa PATCH, de acuerdo con el contrato REST.
- Se valida teléfono y ubicación del negocio antes del envío al backend.
- El token continúa protegido por el almacenamiento Android Keystore existente.
- `gradle/config.properties` incluye la ruta JBR indicada por el mensaje de Android Studio; puede elegirse Embedded JDK desde Settings si la instalación está en otra carpeta.

## Alcance que sigue pendiente

- Sincronización bidireccional y caché offline entre SQLite y marketplace remoto.
- Calendario por trabajador/recurso y disponibilidad paralela por recurso.
- Verificación de cuenta y recuperación de contraseña con envío de códigos.
- Revocación de JWT en servidor (el cierre de sesión local borra el token del dispositivo).
- Pruebas de integración con PostgreSQL real y compilación/ejecución en emulador.

## Comprobaciones realizadas

- Backend: 12 pruebas Node aprobadas.
- Backend: `npm run check` aprobado.
- Recursos XML Android: 19 archivos parseados correctamente.
- No se generó APK verificado: el entorno de empaquetado carecía de Android SDK y el Wrapper no pudo acceder a `services.gradle.org` para descargar Gradle.
