# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
