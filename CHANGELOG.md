# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] - 2026-11-09

### Added

- Viewers and editors can remove a campaign that was shared with them from
  their campaign list. The card shows a "Remove from list" action with a
  confirmation dialog. Campaign data is untouched, and the user can rejoin
  later with a new invite link.

### Changed

- The local development section of the README now describes how to configure
  any OpenID Connect provider instead of documenting the author's private
  Keycloak and reverse-proxy setup. Keycloak is mentioned as the author's
  provider, with a link to the project.

## [0.1.8] - 2026-11-09

### Fixed

- Invite links now forward to the campaign sheet inside the same request after
  sign-in. Previously the post-login join view triggered a second client-side
  navigation and left the browser on a blank page until it was refreshed.
- The Register button now opens Keycloak's registration endpoint directly.
  The previous `prompt=create` parameter is ignored by older Keycloak releases,
  which showed the sign-in form instead.

## [0.1.7] - 2026-11-09

### Changed

- Regenerated the Vaadin frontend manifest and lock file so local builds no
  longer rewrite `package.json`. The generated `@vaadin/*` dependencies and
  version pins now match the 25.3.5 BOM.

## [0.1.6] - 2026-11-09

### Added

- Site-wide footer crediting `dogeared` with a link to
  <https://github.com/dogeared>, plus the running release version read from the
  build metadata.

## [0.1.5] - 2026-11-09

### Changed

- Production host is now `dcc-unstoppable-campaign.dogeared.dev` in the
  production Keycloak realm, the production compose file, and the deployment
  docs, matching the Cloudflare tunnel hostname and the repository name.

## [0.1.4] - 2026-11-08

### Fixed

- Production compose and environment defaults now use the required
  `nas-shared` external Docker network instead of `tnra-shared`.

## [0.1.3] - 2026-11-08

### Added

- MIT license file and a license section in the README.
- Automatic deployment on `main`: CI publishes the container image to GHCR and
  calls the signed deploy webhook. The deploy step is skipped when the webhook
  secrets are not configured.

## [0.1.2] - 2026-11-08

### Added

- Backend unit and browserless Vaadin view tests bringing line coverage to
  more than 90%, with a JaCoCo `check` gate at 90%.
- Vitest coverage for the frontend browser helpers, with a 90% coverage gate.
- Separate backend and frontend coverage badges.

### Changed

- CI now runs the frontend tests and publishes both coverage badges.

### Fixed

- The version badge now reads the git tag directly instead of a JSON file on
  the `badges` branch.

## [0.1.1] - 2026-11-08

### Added

- CI workflow that runs the tests with JaCoCo coverage, publishes tests,
  coverage, and version badges to a `badges` branch, and tags each new `pom.xml`
  version.
- Snyk workflow (`snyk test` and `snyk code test`) on pushes to `main`, on pull
  requests, and weekly; it fails on high or critical issues.
- `pre-commit` hook that runs both Snyk scans and blocks commits on high or
  critical issues.
- Tests, coverage, and version badges in the README.

### Changed

- Production host is now `dcc-unstoppable.dogeared.dev` in the production
  Keycloak realm, the production compose file, and the deployment docs.

### Fixed

- Critical `jackson-databind` deserialization issue (SNYK-JAVA-TOOLSJACKSONCORE-21761838)
  by pinning Jackson to the patched 3.1.8 patch release.
- Documented and time-boxed the `jaxb-core` XXE ignore in `.snyk`, since no
  fixed release is published yet.

## [0.1.0] - 2026-11-08

### Added

- Spring Boot 4.1 + Vaadin 25 campaign sheet app for *Dungeon Crawler Carl:
  Unstoppable*, built on Java 21 and PostgreSQL 17 with Flyway migrations.
- Keycloak OIDC login through a dedicated `unstoppable-app` client, with a
  custom login/registration theme in the game-asset style.
- Anonymous landing page with **Log in** and **Register** actions; Register
  opens the identity provider's registration form directly.
- Campaign sharing with reusable, role-scoped invite links (owner, editor,
  viewer). Invite tokens are hashed at rest and shown once.
- Paper-faithful read view plus a mobile-first sectioned editor covering party
  details, players, deck slots, floors, achievements, purchases, and sharing.
- XP engine: available XP, spent XP, floor score, defeats penalty, and
  difficulty bonus, with per-player benefit limits.
- Dungeon Crawler Carl-inspired visual system (parchment sheets, crimson
  banners, gold rules, dungeon chrome) with all styles externalized to
  `unstoppable.css` and all browser JavaScript in
  `src/main/frontend/scripts/`.
- Docker and Docker Compose setup for local development and Synology
  deployment behind a Cloudflare tunnel.
- Unit tests covering the campaign totals and service rules.
