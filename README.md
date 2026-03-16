# BotLocalEnsayo Android

Base de app Android nativa para la banda.

## Stack elegido

- Kotlin
- Jetpack Compose
- Navigation Compose
- Media3 / ExoPlayer

## Objetivo de esta v1

- Login propio en la app
- Dashboard inicial
- Biblioteca de audios
- Pagos
- Miembros

## Apertura

1. Abre esta carpeta en Android Studio.
2. Deja que sincronice Gradle.
3. Si Android Studio pide actualizar wrapper o SDK, acepta solo si bloquea la sync.

## Flujo de acceso previsto

1. La app intenta sugerir el numero con Phone Number Hint.
2. El backend valida si ese telefono existe en `members`.
3. Si es la primera vez, el usuario crea su PIN.
4. Si ya existe PIN, el usuario lo introduce.
5. La app guarda una sesion larga en local y al volver a abrir intenta restaurarla automaticamente.

## Backend previsto

- `POST /api/app-auth/start`
- `POST /api/app-auth/register-pin`
- `POST /api/app-auth/login`
- `POST /api/app-auth/refresh`
- `POST /api/app-auth/logout`
- `GET /api/app/meta`

## Notificaciones y actualizaciones

- Nuevos audios: usaremos Firebase Cloud Messaging y la tabla `app_push_tokens` para registrar dispositivos.
- Actualizaciones:
  - si la app se distribuye por Google Play, usaremos Play In-App Updates;
  - si se distribuye fuera de Play, la app consultara `/api/app/meta` y ofrecera abrir la URL de descarga nueva.

## Siguiente integracion real

- Sustituir `FakeBotRepository` por llamadas a backend y Supabase
- Activar los endpoints reales de auth en la web
- Registrar tokens FCM por dispositivo
- Reproduccion real con Media3 desde endpoint seguro
