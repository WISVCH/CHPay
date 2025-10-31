# CHPay

CHPay is the digital wallet platform for W.I.S.V. ‘Christiaan Huygens’. It combines a Spring Boot back-end, a Tailwind/FlyonUI based front-end, and integrations with Keycloak and Mollie to support secure payments and administration.

## Development

You can work on CHPay in three different ways. All options share the same service topology and expose identical ports, so switching between them is straightforward.

### Option 1 – Dev Container (VS Code or IntelliJ)

1. **Prerequisites**
   - Docker Desktop 4.31+ (Linux users: Docker Engine + Docker Compose v2).
   - For VS Code: install the *Dev Containers* extension.  
     For IntelliJ IDEA: install JetBrains Gateway with the *Dev Containers* plugin.
2. **Open the workspace**
   - VS Code: `Dev Containers: Rebuild and Reopen in Container`.
   - IntelliJ: `File ▸ Open… ▸ .devcontainer/devcontainer.json` via Gateway.
3. The dev container boots the project plus all dependencies using the included Compose files. No local tooling besides Docker is required.
4. **Run the application** with the supplied run configurations:
   - IntelliJ: `Run ▸ Run 'Application [dev]'`.
   - VS Code: `Run and Debug ▸ Application`.

While the container is running:

| Service | Host URL / Port | Purpose |
| --- | --- | --- |
| Spring Boot app | http://localhost:3080 | CHPay web UI & API |
| PostgreSQL 18 | localhost:35432 | Primary database (`postgres/postgres`) |
| pgAdmin | http://localhost:3081 | Database administration (admin/admin) |
| Mailcatcher UI | http://localhost:3082 | Test inbox |
| Mailcatcher SMTP | localhost:3587 | SMTP endpoint for dev mail |
| Mock Keycloak (OIDC) | http://localhost:3083 | Login flow, seeded users |
| Front-end watcher | runs inside Compose | Executes `npm run watch` automatically |

### Option 2 – Docker Compose Dependencies (Local IDE)

1. Install JDK 21, Node.js 20, Docker Desktop/Compose, and your preferred editor.
2. Start the shared services:

   ```bash
   docker compose up -d db pgadmin mailcatcher oidc frontend-watch
   ```

   (Stop them with `docker compose down` when you are done.)
3. Open the project in VS Code or IntelliJ. The project is already Gradle based, so it will import automatically.
4. Use the same run configurations as above (`Application` in VS Code, `Application [dev]` in IntelliJ) to launch the Spring Boot application.

All services listen on the same host ports listed in the table above. The `frontend-watch` container keeps Tailwind output up to date; if you prefer to run it manually, stop that service and execute `npm run watch` in `src/main/frontend`.

### Option 3 – Manual Setup

Configure everything yourself only if you cannot run Docker:

1. **PostgreSQL** – create a database named `chpay`, reachable at `localhost:3080` for the app and `localhost:35432` for manual access (or update the connection in `application-dev.yml`).
2. **Mail** – run a local SMTP server or update `spring.mail.*` in `application-dev.yml` with your provider.
3. **OIDC provider** – register a Keycloak or other OIDC issuer and update:

   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             wisvchconnect:
               client-id: <client id>
               client-secret: <client secret>
               redirect-uri: "{baseUrl}/login/oauth2/code/wisvchlogin"
           provider:
             wisvchconnect:
               issuer-uri: https://login.ch.tudelft.nl/realms/wisvch
   ```

4. **Mollie** – set `mollie.api_key`, `redirect_url`, and `webhook_url`.
5. **Base URL** – match `spring.application.baseurl` (usually `http://localhost:3080`) and `server.port`.
6. Once the configuration is in place, build and run with:

   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

   or reuse the IDE run configurations.

## Deployment

Production deployments require a publicly reachable base URL and hardened configuration:

- **Keycloak** – CHPay authenticates against Keycloak at `https://login.ch.tudelft.nl`. Create a client for the production domain and copy the `issuer-uri`, `client-id`, and `client-secret` into your production `application.yml`.
- **Mollie** – enable a live Mollie account, configure the webhook to point to `${spring.application.baseurl}/topup/status`, and store the live API key in `mollie.api_key`.
- **Persistence** – point `spring.datasource.*` to the production PostgreSQL instance and tighten credentials.
- **Mail** – configure a real SMTP provider so receipts and admin notices are delivered.
- **Security** – disable the mock services, review allowed origins, and ensure TLS termination in front of the application.

Deployments typically use the same Gradle build (`./gradlew bootJar`) and run the fat jar with an environment-specific `application.yml`.

## Frontend Stack

The UI uses FlyonUI components on top of Tailwind CSS, supplemented by DataTables, ApexCharts, lodash, jQuery, Clipboard.js, and Canvas Confetti.

- **Templates** – HTML views live in `src/main/resources/templates`.
- **Static assets** – JavaScript and compiled CSS are served from `src/main/resources/static`.
- **Source styles** – Tailwind input and helper scripts are managed in `src/main/frontend`.

### Building Assets

```bash
cd src/main/frontend
npm install          # first run
npm run build        # one-off build
npm run watch        # continuous rebuild outside the dev container
```

Inside the dev container the `frontend-watch` service runs `npm run watch` automatically, so edits to `styles.css` or imported components immediately rebuild `main.css`.

Use the IDE run configurations to start the Spring Boot server; the compiled assets are served directly from the `static` directory.
