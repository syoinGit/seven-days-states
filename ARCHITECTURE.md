# Project structure

The application is organized by responsibility. The package names are intentionally
small and stable so that log parsing, persistence, domain processing, and the web UI
can evolve independently.

| Package | Responsibility |
| --- | --- |
| `config` | Spring configuration, application properties, and migration compatibility hooks |
| `batch` | Scheduled or startup runners that trigger imports and polling |
| `entity` | JPA persistence models (`M_` master tables and `T_` transaction/state tables) |
| `repository` | Spring Data repositories; database access stays behind this boundary where practical |
| `log.dto` | Immutable values produced by log parsing |
| `log.parser` | Pure, side-effect-free parsers for individual 7DTD log message families |
| `service` | Import orchestration, game-domain rules, telnet integration, and account/social use cases |
| `web` | Controllers, dashboard/diary view queries, presentation formatting, and Thymeleaf-facing models |
| `util` | Stateless cross-layer helpers that do not depend on Spring, JPA, or the web layer |

## Dependency direction

The normal direction is `web`/`batch` → `service` → `repository`/`entity`.
`log.parser` and `util` remain framework-independent. Presentation-only helpers may
be used by web code, while generic helpers such as timestamp formatting and player
identity normalization live in `util` so that import and social services do not depend
on the web package.

## Refactoring boundary

The import and dashboard services contain the SQL needed to build the current read
models and are covered by integration tests. They are kept as cohesive application
services for now; future extractions should move one query family at a time behind a
small read-model component rather than splitting methods mechanically.
