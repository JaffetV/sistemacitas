# Cambios de v9 respecto de v8

- Se mantiene el módulo Android anterior y SQLite versión 8 con la agenda clásica.
- Se añade un backend REST independiente en `backend/` con Node.js 22 y PostgreSQL 16.
- Se incorporan registro de cliente, alta de negocio, autenticación JWT y hash de contraseñas scrypt.
- Se crean políticas RLS forzadas para usuarios, negocios, miembros, catálogo, horarios, reservas, pagos y favoritos.
- Se protege el acceso a información de clientes: solo el propio usuario y negocios con reservas relacionadas pueden consultarla.
- Se agrega una restricción GiST que bloquea citas superpuestas del mismo negocio.
- El inventario se descuenta y devuelve con triggers transaccionales al crear/cancelar pedidos de recogida.
- Se incorporan horarios de atención con zona horaria `America/Lima`.
- Se añaden `MarketplaceApiClient` y `MarketplaceSessionStore`; el JWT se cifra con AES-GCM usando Android Keystore.
- Se agrega Docker Compose, SQL inicial, contrato REST y OpenAPI.
- La app aún no reemplaza SQLite por la API desde sus pantallas; esa conexión visual y la sincronización son el siguiente incremento.
