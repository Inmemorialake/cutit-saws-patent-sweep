# Changelog

Todos los cambios notables del proyecto **Patent Sweep (Equipo 1)** se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
El versionado sigue [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

### En progreso

- Adecuación y evaluación del arquetipo de la plataforma del servicio Patent Sweep de Cutit Saws Ltd., bajo la adaptación de una única transacción funcional y verificable.

---

## [0.2.0-beta.1] — Sprint 0

### Añadido

- Módulo completo de **Patent Sweep** implementado bajo arquitectura multicapa (Controller, Service, Integration, Repository) sobre Java 21 y Spring Boot 3.5.16.
- Endpoint `POST /api/v1/patentes/busqueda` para la ejecución de búsqueda y detección de conflictos de patentes por fórmula.
- Endpoint `GET /api/v1/patentes/estado` para la consulta del estado precalculado de la fórmula con tiempos de respuesta óptimos.
- Autenticación y control de acceso basado en roles (`ADMINISTRADOR`, `SERVICIO`) mediante tokens de sesión JWT (`POST /auth/login`).
- Adaptador robusto de fuentes externas de patentes (`FuentePatentesMockAdapter`) desacoplado mediante puertos.
- Persistencia transaccional con PostgreSQL 16 y migraciones automatizadas gestionadas mediante Flyway.
- Manejo global de excepciones estandarizado bajo el formato **ProblemDetail (RFC 9457)** con correlación de `traceId`.
- Suite completa de pruebas unitarias y de integración alcanzando un **98% de cobertura en instrucciones** y **94% en ramas** con JaCoCo.
- Verificación estática de calidad de código con Checkstyle cumpliendo el estándar sin violaciones (`0 violations`).
- `docker-compose.yml` configurado con contenedor backend, base de datos PostgreSQL local y stack de monitoreo.
- Pipeline de CI/CD en GitHub Actions (`ci.yml`) con validación automática de compilación, estilo y pruebas.

### Decisiones arquitectónicas (DAS V3.0)

- **DA-01:** API REST independiente con base de datos propia en PostgreSQL 16, garantizando el desacoplamiento de la plataforma SOA de Cutit Saws.
- **DA-02:** Arquitectura multicapa (Controller - Service - Integration - Repository) sobre Java 21 y Spring Boot 3.5.16 para una estricta separación de responsabilidades.
- **DA-05:** Abstracción de la fuente externa de patentes detrás de un puerto (`FuentePatentesPort`) con un adaptador intercambiable para el dataset local.
- **DA-12:** Manejo de errores unificado mediante el estándar **ProblemDetail (RFC 9457)** acompañado de identificadores de correlación (`traceId`).

### Corregido

- Corrección de desajustes en las rutas de JSON Path y matchers de Mockito en las pruebas de integración de controladores.
- Ajuste de saltos de línea al final de los archivos (`NewlineAtEndOfFile`) exigidos estrictamente por Checkstyle.

---

## [0.1.0-beta.2] — Sprint 0

### Añadido

- Estructura inicial del repositorio backend (`patent-sweep/backend/`).
- Definición formal del Documento de Arquitectura de Software (DAS V3.0) y especificación de contratos de integración con los demás equipos.
- Configuración base de Maven con dependencias de Spring Security, Spring Data JPA y Lombok.
- Configuración inicial de la estrategia de ramificación (`main` ← `develop`).

---

[Unreleased]: https://github.com/inmemorialake/patent-sweep/compare/v1.0.0-beta.2...HEAD
[0.1.0-beta.2]: https://github.com/inmemorialake/patent-sweep/compare/v1.0.0-beta.1...v1.0.0-beta.2
[0.2.0-beta.1]: https://github.com/inmemorialake/patent-sweep/releases/tag/v1.0.0-beta.1