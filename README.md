# Unstoppable campaign sheet

[![Tests](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/dogeared/dcc-unstoppable-campaign/badges/tests.json)](https://github.com/dogeared/dcc-unstoppable-campaign/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/dogeared/dcc-unstoppable-campaign/badges/coverage.json)](https://github.com/dogeared/dcc-unstoppable-campaign/actions/workflows/ci.yml)
[![Version](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/dogeared/dcc-unstoppable-campaign/badges/version.json)](CHANGELOG.md)

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

### 2. Start the shared local Keycloak

Local auth runs through the shared Keycloak stack in
`~/Projects/local-keycloak`, exposed by its mkcert reverse proxy at
<https://auth.dev.dogeared.dev>:

```bash
cd ~/Projects/local-keycloak
docker compose up -d
```

That compose file mounts this repository's local Keycloak config:

- realm import: `keycloak/realm-unstoppable.json`
- login theme: `keycloak/themes/unstoppable`

The realm creates the `unstoppable-app` public client and the `demo` /
`demo` user on first import. `--import-realm` only creates realms that do not
exist yet, so after editing the realm file apply the changes to the running
instance (or delete the realm and restart Keycloak):

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password admin
docker exec keycloak /opt/keycloak/bin/kcadm.sh update realms/unstoppable \
  -s loginTheme=unstoppable \
  -s attributes.frontendUrl=https://auth.dev.dogeared.dev
```

Theme files are mounted read-only and picked up without a restart because the
shared stack disables the theme caches.

### 3. Run the app

```bash
mvn spring-boot:run
```

Open <http://localhost:8084>. The landing page has Log in and Register buttons;
after sign-in you land on `/campaigns`. Register opens the identity provider's
registration form directly. No environment overrides are needed: the defaults
already point at `https://auth.dev.dogeared.dev`.

To test from another device on the LAN, the shared proxy serves the app at
<https://dcc-unstoppable.dev.dogeared.dev> (`nginx/sites/dcc-unstoppable-dev.conf`
in `~/Projects/local-keycloak` forwards to `host.docker.internal:8084`). Point
the other device's `/etc/hosts` at this machine's LAN IP; the mkcert certificate
already covers `*.dev.dogeared.dev`. The realm's redirect URIs allow that host.

> Vaadin's dev-tools connection only trusts loopback, so when the app is opened
> through the proxy the dev-tools panel logs "Dev tools functionality denied for
> this host". Application pages, login, and `@Push` work normally; use
> `http://localhost:8084` when you need the dev-tools panel or live reload.

Environment overrides:

| Variable | Default | Meaning |
| --- | --- | --- |
| `SERVER_PORT` | `8084` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5435/unstoppable` | JDBC URL |
| `DB_USER` | `unstoppable` | Database user |
| `DB_PASSWORD` | `unstoppable` | Database password |
| `KEYCLOAK_PUBLIC_URL` | `https://auth.dev.dogeared.dev` | Browser-facing issuer URL |
| `KEYCLOAK_INTERNAL_URL` | `https://auth.dev.dogeared.dev` | Back-channel token/JWKS URL |
| `KEYCLOAK_REALM` | `unstoppable` | Realm name |
| `KEYCLOAK_CLIENT_ID` | `unstoppable-app` | OIDC client id |

The mkcert development CA is installed in this machine's Java trust store, so
the app trusts the proxy certificate during local testing.

### Run the whole dev stack in Docker

```bash
docker compose up --build
```

`compose.yaml` joins the external `local-infra` network so the app can reach the
shared Keycloak by service name. Create it once if it does not exist:

```bash
docker network create local-infra
```

Inside Docker, the app uses `KEYCLOAK_INTERNAL_URL=http://keycloak:8080` for
back-channel calls while the browser still uses
`https://auth.dev.dogeared.dev`. The realm's `frontendUrl` pins the issuer to the
public host, so both paths agree.

## Test and build

```bash
mvn test
mvn -Pproduction package
```

The production profile builds the Vaadin frontend bundle. The resulting jar is
`target/unstoppable-0.1.1.jar`.

The Docker image is a two-stage build (Maven build, then Temurin 21 JRE):

```bash
docker build -t unstoppable .
```

## Continuous integration

| Workflow | Trigger | What it does |
| --- | --- | --- |
| [`CI`](.github/workflows/ci.yml) | pushes to `main`, pull requests | Runs the tests with coverage. On `main`, it publishes the badge data to the `badges` branch and creates a git tag for a new `pom.xml` version. |
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

- `dcc-unstoppable.dogeared.dev` for the app
- `auth.dogeared.dev` for the central Keycloak
- the shared Docker network `tnra-shared`
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
https://dcc-unstoppable.dogeared.dev/login/oauth2/code/keycloak
```

Registration is enabled in the realm; the landing page's Register button sends
`prompt=create` (with `kc_action=register` for older releases) so new users land
on the registration form instead of the sign-in form.

The production realm does not set a `loginTheme`, so it uses the stock Keycloak
page. To reuse the app's look on a Keycloak you control, copy
`keycloak/themes/unstoppable` into that instance's `themes` directory and set
the realm's login theme to `unstoppable`.

### 4. Add the Cloudflare tunnel ingress rule

In the existing `cloudflared` configuration, add:

```yaml
- hostname: dcc-unstoppable.dogeared.dev
  service: http://unstoppable-app:8080
```

Restart `cloudflared`. The app is then available at
<https://dcc-unstoppable.dogeared.dev>.

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
keycloak/       local realm + login theme (mounted by ~/Projects/local-keycloak)
                and the production realm import
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
