# Clean setup

Create an empty PostgreSQL database and configure env.example as .env: DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME, JWT_SECRET and DEMO_PASSWORD. The bootstrap creates admin@example.test, demo-user@example.test and demo-other@example.test. Admin VITE_API_URL points to http://127.0.0.1:3000/api. PHP viewer API_BASE_URL must reach the same API. Android IQBOX_API_BASE_URL uses 10.0.2.2 for emulator host access.

## Commands

```sh
npm ci
node tools/bootstrap-demo.js
npm run dev
# Separate terminal:
node tools/check-demo.js
cd admin-panel
npm ci
npm run dev
# PHP viewer, separate terminal from root:
cd video-page
php -S 127.0.0.1:8088 router.php
```

## Complete configuration inventory

Create PostgreSQL database/user from `env.example`; copy it to `.env`, generate a JWT secret and install with `npm ci`. Run `npm run dev`; database initialization occurs at startup. In `admin-panel/`, install and run `npm run dev`. Serve `video-page/` through PHP with its API and site URL configuration. Open `IQBox-Android/` in Android Studio with JDK 17 and SDK 34. Set the Gradle property `IQBOX_API_BASE_URL` with a trailing slash; the emulator default is `http://10.0.2.2:3000/api/`.

## Environment variables read by source

| Variable | Source consumer | Configuration rule |
|---|---|---|
| `API_BASE_URL` | `routes/videos.js` | Use the local example/source default; adapt to your disposable environment. |
| `BASE_URL` | `routes/files.js` | Use the local example/source default; adapt to your disposable environment. |
| `DB_HOST` | `config/database.js` | Use the local example/source default; adapt to your disposable environment. |
| `DB_NAME` | `config/database.js` | Use the local example/source default; adapt to your disposable environment. |
| `DB_PASSWORD` | `config/database.js` | Supply privately when enabling its integration; no secret default. |
| `DB_PORT` | `config/database.js` | Use the local example/source default; adapt to your disposable environment. |
| `DB_USER` | `config/database.js` | Use the local example/source default; adapt to your disposable environment. |
| `JWT_SECRET` | `middleware/auth.js` | Supply privately when enabling its integration; no secret default. |
| `MAX_FILE_SIZE` | `middleware/upload.js` | Use the local example/source default; adapt to your disposable environment. |
| `NODE_ENV` | `server.js` | Use the local example/source default; adapt to your disposable environment. |
| `PORT` | `server.js` | Use the local example/source default; adapt to your disposable environment. |
| `SITE_BASE_URL` | `video-page/config.php` | Use the local example/source default; adapt to your disposable environment. |
| `UPLOAD_DIR` | `server.js` | Use the local example/source default; adapt to your disposable environment. |

Environment examples do not load themselves. Node dotenv modules read local `.env` where configured; PHP uses its process/hosting environment. Keep provider integrations disconnected for demos. Generate a new secret with `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"` or equivalent, then store it privately.

## Declared component commands

### `package.json`

```json
{
  "start": "node server.js",
  "dev": "node --watch server.js",
  "test": "echo \"Error: no test specified\" && exit 1"
}
```

### `admin-panel/package.json`

```json
{
  "dev": "vite",
  "build": "vite build",
  "preview": "vite preview"
}
```

## Source boundaries

| Component | Responsibility |
|---|---|
| `IQBox-Android/` | Compose Android application |
| `routes/` | Authentication, uploads, shares, gates and finance API |
| `config/` | PostgreSQL connection and initialization |
| `admin-panel/` | React administration |
| `video-page/` | PHP public sharing viewer |


Variables in the inventory are not all mandatory: the preceding prerequisites identify the required core values. Provider variables are required only for their enabled live integration. Tests may use DEMO_API_URL to override the local target. Never point bootstrap/reset/check scripts at a production database.


## Local share routes

router.php serves /file/<token> and /folder/<token> without a retired reverse proxy. Set BASE_URL and SITE_BASE_URL to the viewer origin on 8088; set API_BASE_URL to the API origin with /api. Store links are optional operator configuration; no unverified store availability is implied.
