# Backend UNAMAD Citas Marketplace

API REST en Node.js 22 y PostgreSQL 16, separada de la app Android.

## 1. Levantar localmente con Docker

Requisitos: Docker Engine/Desktop y Docker Compose v2.

```bash
cd backend
cp .env.example .env
```

Edita `backend/.env` y define secretos propios. Ejemplo para generar valores seguros:

```bash
openssl rand -hex 32
```

Usa valores distintos para `POSTGRES_PASSWORD`, `APP_DB_PASSWORD` y `JWT_SECRET`. `JWT_SECRET` debe tener al menos 32 bytes. Evita símbolos reservados de URL en `APP_DB_PASSWORD`; una cadena hexadecimal de `openssl rand -hex 32` funciona.

Arranca la base y la API:

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f api
```

Comprueba:

```bash
curl http://localhost:8080/health
curl http://localhost:8080/api/v1/categories
```

El endpoint `/health` debe responder `{"status":"ok",...}`. La API escucha en `8080`.

## 2. Pruebas sin Docker

Las pruebas unitarias de dominio y seguridad no necesitan PostgreSQL:

```bash
npm test
npm run check
```

Para ejecutar el servidor fuera de Docker se necesita PostgreSQL inicializado con `db/001_bootstrap.sh` y `db/002_marketplace.sql`, además de `DATABASE_URL` y `JWT_SECRET`.

## 3. Configuración de Android

La app incluye `MarketplaceApiClient` y `MarketplaceSessionStore`:

- El token JWT queda cifrado con AES-GCM y una clave no exportable del Android Keystore.
- `ejecutarAsync` ejecuta solicitudes fuera del hilo principal y entrega el resultado en el hilo UI.
- En `debug`, se admite HTTP para usar el emulador con `http://10.0.2.2:8080` o una IP local.
- En `release`, el cliente exige HTTPS.

Ejemplo de inicialización desde una pantalla (el ejemplo no se conecta automáticamente a las pantallas existentes):

```kotlin
val api = MarketplaceApiClient(this, "https://api.tudominio.pe")
api.ejecutarAsync(
    operacion = { api.iniciarSesion(correo, contrasena) },
    callback = { resultado ->
        resultado.onSuccess { usuario -> /* actualizar pantalla */ }
            .onFailure { error -> /* mostrar error */ }
    }
)
```

Para desarrollo en emulador Android, una compilación `debug` puede usar:

```kotlin
val api = MarketplaceApiClient(this, "http://10.0.2.2:8080")
```

Las actividades actuales siguen usando el repositorio SQLite v8. La conexión API está implementada como adaptador listo para integrar; el cambio de las pantallas para usar cuentas remotas y sincronización es el siguiente trabajo, no se debe asumir que ya hay sincronización multi-dispositivo.

## 4. Seguridad operativa

- Nunca subas `.env` al repositorio.
- No uses las contraseñas de `.env.example` en un servidor público.
- Publica la API detrás de HTTPS y un proxy con límites de tamaño/solicitudes.
- Configura `ALLOWED_ORIGINS` solo para dominios web conocidos.
- `TRUST_PROXY=true` solo cuando la API esté detrás de un proxy controlado que establezca `X-Forwarded-For`.
- El volumen de PostgreSQL guarda los datos. No ejecutes `docker compose down -v` salvo que quieras borrar la base local.
- Los scripts en `/docker-entrypoint-initdb.d` se ejecutan solo al crear un volumen vacío. Para cambios futuros se deben crear migraciones incrementales nuevas.

## 5. Límites de esta fase

No integra pasarela de pago, correo/SMS, recuperación de contraseña, verificación de identidad, push notifications, recursos/profesionales ni excepciones de calendario. Los pagos quedan pendientes, nunca se marcan como cobrados sin confirmación externa.
