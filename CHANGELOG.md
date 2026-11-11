# Changelog

Todos los cambios notables del proyecto nutrI-plantA se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
El versionado sigue [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

### En progreso

- HU-02: Login con JWT y refresh tokens (Sprint 2).
- HU-03: Logout seguro con blacklist (Sprint 2).
- Gateway como proxy real con validación JWT (Sprint 3).
- diagnostico-service: estructura base y CRUD cultivos (Sprint 4).

---

## [0.2.0] — Sprint 1

### Añadido

- `auth-service` completo con arquitectura hexagonal (Ports and Adapters).
- Modelo de dominio `User` con estados `PENDIENTE_APROBACION`, `APROBADO`, `RECHAZADO`, `DESACTIVADO`.
- `AuthService` con operaciones `register()` y `authenticate()`.
- `TokenService` con emisión de JWT HS256, expiración configurable y `jti` único por token.
- Endpoint `POST /api/v1/auth/register` (HU-01) con validación Pydantic.
- Hash de contraseñas con Argon2id via `pwdlib` (recomendación OWASP 2025).
- `IUserRepository` con implementaciones `PostgresUserRepository` e `InMemoryUserRepository`.
- `IPasswordHasher` con implementación `Argon2PasswordHasher`.
- Exception handler global: traduce excepciones de dominio a HTTP con mensajes en español.
- Migración Alembic `0001_create_users_table` en esquema `auth`.
- 49 tests unitarios y de integración. Cobertura 82%.
- Tres contratos de import-linter que verifican la arquitectura hexagonal.
- `docker-compose.yml` actualizado con `auth-migrations` y `auth-service`.
- CI workflow actualizado con jobs de ruff, mypy, import-linter, tests y Docker build.
- `psycopg2-binary` como driver síncrono para Alembic (separado de `asyncpg` para FastAPI).

### Decisiones arquitectónicas

- ADR-05 (ya existente): DI Container con dependency-injector.
- Ajuste operacional: puerto de PostgreSQL en 2332 (no 5432) para evitar colisiones locales.
- Ajuste operacional: puerto de Gateway en 8085 (no 8080).

### Corregido

- Healthcheck de MinIO: añadido `start_period: 30s` para tolerancia en arranque frío.
- Dependencia faltante: `psycopg2-binary` necesario para Alembic en modo síncrono.

---

## [0.1.0] — Sprint 0

### Añadido

- Estructura inicial del monorepo: `services/`, `frontend/`, `gateway/`, `docs/`.
- `docker-compose.yml` con PostgreSQL 16, MinIO y gateway hello-world.
- Gateway con endpoint `GET /api/v1/health` (FastAPI 0.115).
- Script `scripts/dev/init-db.sql`: crea esquemas `auth` y `diagnostico`.
- `minio-init`: crea bucket `diagnosticos` automáticamente.
- CI workflow `.github/workflows/ci-pr-to-develop.yml` con linter y validación de compose.
- Plantillas de PR e issues en `.github/`.
- Configuración de Qodo Merge (`.pr_agent.toml`).
- Documentación base: `README.md`, `CONTRIBUTING.md`, `docs/arquitectura/`, `docs/backlog/`.
- Estrategia de ramas: `main` ← `develop` ← `mpastrana`.

---

[Unreleased]: https://github.com/manusixx/nutrIplantA/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/manusixx/nutrIplantA/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/manusixx/nutrIplantA/releases/tag/v0.1.0
