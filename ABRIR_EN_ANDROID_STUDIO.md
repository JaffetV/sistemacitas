# UNAMAD Citas Marketplace — guía de apertura y ejecución

## Abrir el proyecto

1. Extrae el ZIP completo en una carpeta local; no abras el ZIP directamente.
2. En Android Studio selecciona **Open** y elige `comunamadsistemacitas`, la carpeta que contiene `settings.gradle.kts`.
3. Abre `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`.
4. En **Gradle JDK**, selecciona **Embedded JDK**. La ruta indicada por Android Studio suele ser `C:\Program Files\Android\Android Studio\jbr`. Si tu instalación está en otro lugar, selecciona el JBR que acompaña a Android Studio.
5. Pulsa **Apply**, **OK** y **Sync Project with Gradle Files**.
6. En SDK Manager instala Android SDK Platform 35 si aún no está instalado.
7. Ejecuta la configuración `app` en un emulador o teléfono con Android 7.0/API 24 o superior.

El proyecto incluye `gradle/config.properties` con el path del JBR proporcionado en el mensaje de error. Si se instala Android Studio en otra ruta, la selección **Embedded JDK** desde Settings tiene prioridad y permite corregir la ubicación.

## Requisitos

- Android Studio compatible con Android Gradle Plugin 8.7.3.
- JDK 17 o JBR compatible incluido con Android Studio.
- Android SDK Platform 35.
- Gradle Wrapper 8.9 incluido.
- `minSdk 24`, `targetSdk 35`.

## Conexión con el backend

Por defecto, el cliente usa `http://10.0.2.2:8080`, dirección desde el emulador Android al equipo anfitrión. Primero inicia el backend de la carpeta `backend/` con Docker Compose y PostgreSQL 16.

Para un teléfono físico, utiliza la IP LAN del ordenador donde corre Docker:

```powershell
.\gradlew.bat :app:assembleDebug -PmarketplaceApiBaseUrl=http://192.168.1.20:8080
```

Reemplaza la IP de ejemplo por la IP real del ordenador y comprueba que el teléfono y el ordenador estén en la misma red. El modo HTTP solo está permitido en debug; release requiere HTTPS.

## Funcionalidades de esta iteración

- Portal inicial con interfaz azul oscuro/verde y accesos principales.
- Alta e inicio de sesión remoto de clientes.
- Alta remota de negocio y alta de negocios adicionales con sesión existente.
- Selección del negocio activo mediante UUID.
- Exploración remota de negocios y publicaciones.
- Solicitud de citas/pedidos de recogida y consulta/cancelación de reservas de cliente.
- Panel remoto del propietario con publicaciones, horarios y estados de reservas.
- Agenda clásica y catálogo local SQLite conservados como módulos separados.

La integración remota no convierte automáticamente el SQLite local a PostgreSQL ni sincroniza sus filas antiguas. Los datos consultados en la pantalla remota proceden del servidor; la agenda y el marketplace local siguen siendo almacenamiento local. Así se evita mezclar IDs enteros locales con UUID remotos.

## Pendientes no simulados

- Calendario por trabajador/recurso: el backend actual bloquea solapamientos por negocio completo y aún no tiene recursos/trabajadores ni endpoint para elegirlos.
- Verificación y recuperación de cuenta: faltan endpoints de tokens de un solo uso y un servicio de email/SMS.
- Revocación JWT en servidor: el cierre de sesión actual elimina el token cifrado del dispositivo; el JWT existente en backend expira en 12 horas, pero aún no tiene una lista de revocación.
- Pruebas end-to-end Android/PostgreSQL y prueba en emulador real.

## Pruebas y limitaciones de esta entrega

En backend se ejecutaron `npm test` y `npm run check`: 12 pruebas aprobadas y sintaxis Node correcta. No se pudo completar `assembleDebug` en el entorno de empaquetado porque no había Android SDK configurado y el Wrapper no pudo descargar Gradle 8.9 por falta de acceso DNS a `services.gradle.org`. Por ello no se incluye APK ni se afirma que se haya verificado en emulador.

## Estructura principal

- `app/src/main/java/.../view`: actividades y pantallas.
- `app/src/main/java/.../data/local`: SQLite y migraciones.
- `app/src/main/java/.../data/repository`: repositorios locales.
- `app/src/main/java/.../data/remote`: cliente REST, sesión cifrada y configuración API.
- `backend`: API Node.js y esquema PostgreSQL.
- `docs`: contrato API, diagramas de datos y notas de arquitectura.


**Nota sobre datos locales y remotos:** la agenda clásica continúa en SQLite y el marketplace nuevo consulta el backend por separado. Aún no existe sincronización automática bidireccional ni caché offline del catálogo remoto.
