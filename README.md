# Unstoppable campaign sheet

[![Tests](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/dogeared/dcc-unstoppable-campaign/badges/tests.json)](https://github.com/dogeared/dcc-unstoppable-campaign/actions/workflows/ci.yml)
[![Backend coverage](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/dogeared/dcc-unstoppable-campaign/badges/backend-coverage.json)](https://github.com/dogeared/dcc-unstoppable-campaign/actions/workflows/ci.yml)
[![Frontend coverage](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/dogeared/dcc-unstoppable-campaign/badges/frontend-coverage.json)](https://github.com/dogeared/dcc-unstoppable-campaign/actions/workflows/ci.yml)
[![Version](https://img.shields.io/github/v/tag/dogeared/dcc-unstoppable-campaign?label=version)](CHANGELOG.md)

A web replacement for the paper campaign sheet in *Dungeon Crawler Carl:
Unstoppable*. It keeps the paper layout for reading, adds a mobile-friendly
tabbed editor, computes XP and final scores, and lets a whole party share one
sheet through private invite links.

Built with Spring Boot 4.1, Spring Security 7, Vaadin 25, Java 21, PostgreSQL 17,
Flyway, and Keycloak (OIDC).

## What it does

- **Read view** mirrors Side A of the paper sheet: floor grid, party row,
  player blocks, deck lists, achievements, purchases, and every logged attempt.
- **Editor** is split into tabs so it works on a phone: party, players, deck
  lists, floors, achievements, purchases, and sharing.
- **Derived totals** are computed, never typed:
  - `available XP = Σ floor scores − Σ purchase costs`
  - `final score = Σ floor scores − 3 × defeats + difficulty bonus`
  - Floor score `= Σ achievement XP on that floor + boss/quest XP`
- **Attempts** are individual records. A failed attempt adds one defeat; a
  successful attempt marks the floor cleared. Achievement XP still counts after
  a loss; boss/quest XP is entered manually and only makes sense after a win.
- **Sharing** uses role-scoped, reusable invite links (viewer or editor) owned by
  the campaign owner. Tokens are stored as SHA-256 hashes; the raw link is shown
  once, when it is created.
- **Auth** is Keycloak OIDC. The app stores the immutable Keycloak subject, so
  renaming or re-emailing a user does not lose ownership.

See [`docs/form-field-map.md`](docs/form-field-map.md) for the field-by-field
mapping from the paper sheet and the rule semantics.

## Stack

| Piece | Version |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.2 |
| Spring Security | 7 |
| Vaadin | 25.3.5 |
| PostgreSQL | 17 |
| Keycloak | 26.x |
| Flyway | managed by Spring Boot |

## Run locally

### 1. Start PostgreSQL

```bash
docker compose up -d db
```

This starts `unstoppable-db` on `localhost:5435` with database, user, and
password all set to `unstoppable`. Flyway creates the schema on first boot.

### 2. Configure an OpenID Connect provider

Sign-in uses the OIDC authorization-code flow with PKCE, so any
spec-compliant provider works. The author uses
[Keycloak](https://www.keycloak.org/). Point the app at your provider with
these environment variables:

| Variable | Meaning |
| --- | --- |
| `KEYCLOAK_PUBLIC_URL` | Browser-facing issuer URL |
| `KEYCLOAK_INTERNAL_URL` | Back-channel token/JWKS URL (may equal the public URL) |
| `KEYCLOAK_REALM` | Realm name |
| `KEYCLOAK_CLIENT_ID` | OIDC client id |

The client must be public, allow the authorization-code flow with PKCE, and
register the redirect URI:

```
http://localhost:8084/login/oauth2/code/keycloak
```

Enable user registration on the provider if you want the landing page's
Register button to open a sign-up form.

The repository includes a starting realm import and login theme under
`keycloak/`. To use them with Keycloak, import
`keycloak/realm-unstoppable.json` into your instance and copy
`keycloak/themes/unstoppable` into the instance's `themes` directory. Adjust
the realm's redirect URIs, web origins, and frontend URL for your own host.
`--import-realm` only creates realms that do not exist yet, so apply later
realm edits to the running instance or delete and re-import it.

### 3. Run the app

```bash
export KEYCLOAK_PUBLIC_URL=https://auth.example.test
export KEYCLOAK_INTERNAL_URL=https://auth.example.test
export KEYCLOAK_REALM=unstoppable
export KEYCLOAK_CLIENT_ID=unstoppable-app
mvn spring-boot:run
```

Open <http://localhost:8084>. The landing page has Log in and Register buttons;
after sign-in you land on `/campaigns`. Register opens the identity provider's
registration form directly.

> Vaadin's dev-tools connection only trusts loopback, so the dev-tools panel and
> live reload work on `http://localhost:8084`. Application pages, login, and
> `@Push` work normally through any proxy or alternate hostname.

Environment overrides:

| Variable | Default | Meaning |
| --- | --- | --- |
| `SERVER_PORT` | `8084` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5435/unstoppable` | JDBC URL |
| `DB_USER` | `unstoppable` | Database user |
| `DB_PASSWORD` | `unstoppable` | Database password |
| `KEYCLOAK_PUBLIC_URL` | — | Browser-facing issuer URL |
| `KEYCLOAK_INTERNAL_URL` | — | Back-channel token/JWKS URL |
| `KEYCLOAK_REALM` | — | Realm name |
| `KEYCLOAK_CLIENT_ID` | — | OIDC client id |

If your provider uses a private or self-signed certificate, add its CA to the
Java trust store used to run the app.

### Run the whole dev stack in Docker

```bash
docker compose up --build
```

`compose.yaml` joins an external Docker network named `local-infra` so the app
can reach an OIDC provider started by another Compose project. Create it once if
it does not exist:

```bash
docker network create local-infra
```

Inside Docker, set `KEYCLOAK_INTERNAL_URL` to a URL the app container can reach
(and `KEYCLOAK_PUBLIC_URL` to the browser-facing URL). If your provider is not
on `local-infra`, point the URL at whatever network the container can reach and
adjust the Compose file accordingly. The realm's `frontendUrl` should pin the
issuer to the public host so the browser and back-channel views agree.

## Test and build

```bash
mvn test
mvn -Pproduction package
```

The production profile builds the Vaadin frontend bundle. The resulting jar is
`target/unstoppable-0.2.1.jar`.

The Docker image is a two-stage build (Maven build, then Temurin 21 JRE):

```bash
docker build -t unstoppable .
```

## Continuous integration

| Workflow | Trigger | What it does |
| --- | --- | --- |
| [`CI`](.github/workflows/ci.yml) | pushes to `main`, pull requests | Runs the backend (JUnit + JaCoCo) and frontend (Vitest) tests with coverage. On `main`, it publishes the badge data to the `badges` branch, creates a git tag for a new `pom.xml` version, pushes a container image to GHCR, and calls the deploy webhook. |
| [`Snyk`](.github/workflows/snyk.yml) | pushes to `main`, pull requests, weekly | Runs `snyk test` and `snyk code test`; fails on high or critical issues. Needs the `SNYK_TOKEN` repository secret. |

### Snyk pre-commit hook

The repository ships a `pre-commit` hook that runs `snyk test` (dependencies)
and `snyk code test` (static analysis), and blocks the commit on any high or
critical issue. Enable it once per clone:

```bash
npm install --global snyk   # if you don't have the CLI
snyk auth
git config core.hooksPath .githooks
```

Skip it for a single commit with `git commit --no-verify`.

The one ignored finding in [`.snyk`](.snyk) is an `org.glassfish.jaxb:jaxb-core`
XXE for which no fixed release has been published. The ignore expires on
2027-02-01; remove it as soon as an upgrade is available.

## Production deployment (Synology + Cloudflare tunnel)

Production uses:

- `dcc-unstoppable-campaign.dogeared.dev` for the app
- `auth.dogeared.dev` for the central Keycloak
- the shared Docker network `nas-shared`
- `compose.production.yaml`

### 1. Prepare the database and environment

On the NAS, copy the project to a folder used by Container Manager, then:

```bash
cp .env.production.example .env
# edit .env: set DB_PASSWORD and confirm URLs/network name
```

### 2. Create the Container Manager project

Container Manager → Project → Create:

- Source: the project folder (or a Git repository)
- Compose file: `compose.production.yaml`
- Environment file: `.env`

The stack exposes the app only inside Docker (`unstoppable-app:8080`). It does
not publish a host port; Cloudflare reaches it over the shared network.

### 3. Import the production Keycloak realm

Import `keycloak/realm-unstoppable.production.json` into `auth.dogeared.dev`
once, through the Keycloak admin console (Realm → Create → Import) or `kcadm`.
The realm sets `frontendUrl` to `https://auth.dogeared.dev` and registers the
redirect URI:

```
https://dcc-unstoppable-campaign.dogeared.dev/login/oauth2/code/keycloak
```

Registration is enabled in the realm; the landing page's Register button sends
the user to Keycloak's `/protocol/openid-connect/registrations` endpoint so new
users land on the registration form instead of the sign-in form. That endpoint
works across Keycloak versions, unlike `prompt=create`, which older releases
ignore.

The production realm does not set a `loginTheme`, so it uses the stock Keycloak
page. To reuse the app's look on a Keycloak you control, copy
`keycloak/themes/unstoppable` into that instance's `themes` directory and set
the realm's login theme to `unstoppable`.

### 4. Add the Cloudflare tunnel ingress rule

In the existing `cloudflared` configuration, add:

```yaml
- hostname: dcc-unstoppable-campaign.dogeared.dev
  service: http://unstoppable-app:8080
```

Restart `cloudflared`. The app is then available at
<https://dcc-unstoppable-campaign.dogeared.dev>.

`server.forward-headers-strategy: framework` is enabled, so the app honors
`X-Forwarded-Proto` / `X-Forwarded-Host` when building invite links.

### 5. Health and upgrades

- Health check: `GET /actuator/health`
- Upgrade: pull the new source (or rebuild the image), then recreate the
  project in Container Manager. Flyway migrations run automatically on boot.
- The named volume `unstoppable-pg` holds the database. Back it up like any
  other Postgres volume.

## Access model

| Role | Can read | Can edit | Can invite |
| --- | --- | --- | --- |
| Owner | yes | yes | yes |
| Editor | yes | yes | no |
| Viewer | yes | no | no |

Invite links are generated per role, can expire (`0` = never), and are revoked
individually. Redeeming a link creates a `campaign_membership` row for the
signed-in Keycloak subject; redeeming a stronger link upgrades an existing
viewer to editor.

## Project layout

```
src/main/java/com/example/unstoppable/
  config/      Keycloak client registration and Spring Security
  domain/      JPA entities and enums (Benefit, Difficulty, AccessRole, …)
  repository/  Spring Data repositories
  service/     CampaignService (domain rules and totals), InviteService,
               CampaignAccessService, read models
  ui/          Vaadin routes and UI helpers
    component/ dialogs for deck slots, attempts, achievements, purchases
src/main/resources/
  db/migration/V1__initial_schema.sql
  META-INF/resources/styles/unstoppable.css
keycloak/       sample realm import + login theme, and the production realm import
docs/           form field map
```

UI code only reads materialised view records (`CampaignSheet`, `CampaignTotals`)
and never touches lazy JPA associations outside a transaction.

## Notes

- JPA optimistic locking is enabled on `Campaign`; Vaadin `@Push` is on, so
  multiple open editors can refresh without stale writes silently winning.
- Defeats and floor scores are derived from attempts and achievements. Deleting
  an attempt or achievement recalculates the totals on the next read.
- Deleting a purchase restores spendable XP but intentionally does not undo a
  deck flag that the purchase set (eliminated, mastered, extra hand). Clear the
  flag from the deck editor if you want the physical state reversed too.

## License

Released under the [MIT License](LICENSE).
