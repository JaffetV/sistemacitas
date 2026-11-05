# UNAMAD Citas Marketplace — Iteración de integración Android

## Qué contiene

Esta entrega parte del módulo v9 (Android + backend) y conserva la agenda clásica local. El ZIP `APP.unamad.sistemacitas.zip` adjunto al último intercambio contenía únicamente la aplicación de agenda con SQLite v6; se utilizó el módulo v9 previo porque incorpora la migración de SQLite 7 a 8, el marketplace local y el backend Node.js/PostgreSQL. No se sustituyó la migración segura por el `onUpgrade` destructivo del archivo v6.

### Android

- Portal principal rediseñado con identidad visual azul oscuro y verde, accesos de cliente, negocio, agenda, búsqueda y ofertas.
- Registro e inicio de sesión remoto de clientes contra `/api/v1/auth/register/customer` y `/api/v1/auth/login`.
- Alta remota de negocio contra `/api/v1/auth/register/business`; permite crear otro negocio con una sesión existente.
- Selección y persistencia del negocio activo mediante UUID del servidor.
- Exploración de negocios, carga de publicaciones, creación y consulta/cancelación de reservas desde PostgreSQL.
- Panel remoto básico: publicaciones, horario semanal y estados de reservas.
- El JWT continúa guardándose cifrado con AES-GCM en Android Keystore.
- La agenda clásica, clientes, servicios, ingresos y SQLite 8 se conservan como módulo local independiente. El marketplace remoto consume PostgreSQL; la caché/offline bidireccional y la sincronización entre SQLite y el servidor aún no están implementadas.

### Configuración de conexión

Por defecto, `MARKETPLACE_API_BASE_URL` apunta a `http://10.0.2.2:8080`, que permite al emulador Android acceder al servidor que corre en la computadora anfitriona.

Para un teléfono físico, cambia la propiedad Gradle en `app/build.gradle.kts` o compila con la IP LAN de la computadora:

```powershell
.\gradlew.bat :app:assembleDebug -PmarketplaceApiBaseUrl=http://192.168.1.20:8080
```

Sustituye `192.168.1.20` por la IP real del equipo anfitrión. HTTP está permitido solo en debug. Release requiere HTTPS.

## Abrir en Android Studio

1. Extrae el ZIP y abre la carpeta `comunamadsistemacitas` (la que contiene `settings.gradle.kts`).
2. En `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`, selecciona `Embedded JDK` (JDK 17 o superior compatible con tu Android Studio).
3. Sincroniza Gradle. El Wrapper está fijado en Gradle 8.9, compatible con AGP 8.7.3.
4. Selecciona `app` y ejecuta en un emulador o dispositivo.

Se incluye `gradle/config.properties` con la ruta JDK indicada en el equipo donde se reportó el error. Si Android Studio está instalado en otra carpeta, cambia la ruta desde Gradle JDK en Settings; no edites las versiones de Gradle como solución a ese mensaje.

## Backend

Consulta `backend/README.md`. Requiere Node.js 22, Docker y PostgreSQL 16. Para iniciar en un entorno local:

```bash
cd backend
cp .env.example .env
# Configura secretos fuertes y la contraseña de PostgreSQL en .env
docker compose up --build -d
curl http://localhost:8080/health
```

El servidor no está desplegado en este ZIP. Las pantallas remotas requieren que el backend esté iniciado y accesible desde el emulador/teléfono.

## Alcance implementado y pendientes reales

Implementado en esta iteración: pantallas Android de login/registro remoto, selección de negocio activo, exploración remota, publicaciones, reservas, panel del negocio y manejo de UUID. La agenda clásica sigue operando localmente; no se mezclan automáticamente sus datos con los UUID remotos.

Pendiente antes de considerar la siguiente etapa completa:

- Calendario por trabajador/recurso y reservas paralelas. El esquema actual bloquea solapamientos por negocio, no por trabajador/recurso; se necesita una migración PostgreSQL y endpoints específicos.
- Verificación de correo/teléfono y recuperación de cuenta. Requieren flujo de tokens de un solo uso y un proveedor de envío de mensajes/correo; el backend actual no incluye esos endpoints.
- Revocación de JWT en el servidor. El botón de salida elimina el token cifrado del dispositivo, pero no invalida el JWT ya emitido en backend; hoy expira a las 12 horas.
- Pruebas end-to-end Android/PostgreSQL y ejecución real en emulador. No hay servidor Docker/PostgreSQL ni Android SDK disponible en este entorno de empaquetado.

No se presenta como real ninguna de esas funciones pendientes.

## Pruebas ejecutadas

En `backend/`:

```bash
npm test
npm run check
```

Resultado en esta entrega: 12 pruebas de backend aprobadas y verificaciones de sintaxis Node correctas. La compilación Android no pudo ejecutarse aquí: el Wrapper intentó descargar Gradle 8.9 y el entorno no tuvo acceso DNS a `services.gradle.org`; tampoco había Android SDK configurado. Por tanto, este archivo es código fuente preparado para sincronizar/compilar en Android Studio, no un APK ya verificado.
