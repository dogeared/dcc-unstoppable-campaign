# AGENTS.md

Working notes for coding agents in this repository.

## What this is

Spring Boot + Vaadin app that digitizes the *Dungeon Crawler Carl:
Unstoppable* paper campaign sheet. Read `README.md` for setup and
`docs/form-field-map.md` for the authoritative field and XP semantics.

## Commands

```bash
mvn -q -DskipTests compile     # fast compile
mvn -q test                    # unit tests
mvn -Pproduction package       # production jar + frontend bundle
docker compose up -d db        # local Postgres on :5435
mvn spring-boot:run            # app on :8084
```

Local auth uses the shared Keycloak stack in `~/Projects/local-keycloak` at
`https://auth.dev.dogeared.dev` (realm `unstoppable`, client `unstoppable-app`).
That compose file mounts `keycloak/realm-unstoppable.json` and the login theme
in `keycloak/themes/unstoppable`. The same proxy serves the app at
`https://dcc-unstoppable.dev.dogeared.dev` via
`~/Projects/local-keycloak/nginx/sites/dcc-unstoppable-dev.conf`, which forwards
to `host.docker.internal:8084`.

## Architecture

- `config/` — explicit OIDC client registration (`KeycloakClientConfig`).
  Do not switch to discovery: it would bake the public hostname into
  back-channel URLs. `KeycloakProperties` splits public and internal URLs.
- `domain/` — JPA entities with no business logic beyond small state methods.
  Enum rules (benefit costs, difficulty bonuses) live on the enum.
- `repository/` — Spring Data interfaces. Add query methods here rather than
  fetching everything and filtering in Java when the dataset can grow.
- `service/CampaignService` — the domain boundary. Every mutation goes through
  it, checks campaign access, validates input, and recomputes derived state.
- `service/CampaignSheet` / `CampaignTotals` — immutable read models for the UI.
- `ui/` — Vaadin views. Views must not touch lazy JPA associations; load a
  `CampaignSheet` and render from it.
- `src/main/frontend/scripts/unstoppable.js` — browser helpers, loaded through
  `@JsModule` in `AppShell`. Keep JavaScript there rather than inline
  `executeJs` strings.
- `config/RegisterAwareAuthorizationRequestResolver` — turns
  `/oauth2/authorization/keycloak?mode=register` into Keycloak's registration
  flow (`prompt=create`).

## Conventions

- Java 21, records for read models, `var` only for obvious locals.
- Keep money/XP math in `CampaignTotals` or on the enum; do not duplicate it in
  views.
- Access checks always go through `CampaignAccessService`; never compare
  `ownerSub` in a view.
- Invite tokens are hashed at rest. Never log or persist a raw token.
- UI strings are plain English; the app has no localization layer.
- All styles live in `src/main/resources/META-INF/resources/styles/unstoppable.css`
  and all browser JavaScript in `src/main/frontend/scripts/`; do not hardcode
  CSS or script bodies in Java.
- Prefer the existing CSS classes in `unstoppable.css` over inline styles.

## Vaadin 25 gotchas

- `Page.getLocation()` and `VaadinService.getBaseUri()` do not exist. To build
  absolute invite URLs, read `X-Forwarded-Host`, `Host`, and
  `X-Forwarded-Proto` from `VaadinRequest.getCurrent()`.
- Views are `@PermitAll` and rely on Spring Security for authentication.
  `LandingView` and `LoginView` are `@AnonymousAllowed`.
- After a mutation, call the view's `reload()` which re-reads the sheet; do not
  patch the UI by hand.

## Data semantics (short version)

```
floorScore  = Σ achievement.xp(floor) + floorProgress.bossQuestXp
defeats     = count(attempts where successful = false)
availableXp = Σ floorScore − Σ purchase.cost
finalScore  = Σ floorScore − 3 × defeats + difficultyBonus
```

- Spending XP never changes `finalScore`.
- Defeats never change `availableXp`.
- Achievement XP counts even on a failed attempt.
- A successful attempt marks the floor cleared; deleting the last successful
  attempt reopens it.
- Benefit limits are per player: 1 / 1 / 1 / 2 / unlimited / unlimited.
- `KEEP_STRIKE_UPGRADE` costs `X + 1`; `KEEP_CHARACTER_UPGRADE` costs `X + 3`.

## Known intentional behaviors

- Deleting a purchase does not clear deck flags it previously set.
- Deleting an achievement or attempt updates totals only on the next read.
- `CampaignService.create` seeds two players, 14 deck slots each, and nine
  floors. Single-player campaigns use seat 1 and leave seat 2 blank.
- Global achievement-name autocomplete is per campaign on purpose; do not leak
  names across campaigns.

## Before finishing a change

1. `mvn -q test`
2. Boot the app and open `/campaigns` and a campaign sheet.
3. If you touched the schema, add a new Flyway migration; do not edit V1 after
   it has shipped.
