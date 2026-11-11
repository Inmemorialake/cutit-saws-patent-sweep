# 🛡️ Patent Sweep API — Cutit Saws

API REST del módulo de **Patent Sweep (Equipo 1)** para la plataforma modular de Cutit Saws — curso 750021C Desarrollo de Software II, Universidad del Valle. Gestiona la vigilancia automatizada y validación cruzada de riesgo de patentes para proteger los diseños de fabricación.

[![License](https://img.shields.io/badge/license-Academica--Univalle-blue.svg)](#licencia)
[![Status](https://img.shields.io/badge/status-Desarrollo%20--%20HU--02-green.svg)]()
[![CI](https://github.com/inmemorialake/patent-sweep/actions/workflows/ci.yml/badge.svg?branch=develop)]()
[![Quality Gate](https://img.shields.io/badge/quality%20gate-passed-brightgreen.svg)]()
[![Coverage JaCoCo](https://img.shields.io/badge/coverage-98%25-brightgreen.svg)](#cobertura-de-tests)

---

## 📋 Descripción y Contexto del Proyecto

Cutit Saws Ltd. es un fabricante boutique de motosierras hidráulicas de diamante que, tras el éxito de su modelo "Ripit 5000", enfrenta una demanda que supera su capacidad de manufactura. Para escalar sus operaciones de forma segura, la dirección estratégica definió la construcción de una plataforma backend modular basada en 5 APIs REST independientes.

Esta API (**Equipo 1 / Patent Sweep**) es responsable de la **vigilancia y validación cruzada de riesgo de patentes**.

| Rol en la plataforma | Consumidor / Relación |
|---|---|
| **Es consumida por** | Materiales / Fórmulas (Equipo 1) — validación de componentes contra patentes |
| **Es consumida por** | Fabricación (Equipo 3) — validación de lotes de producción |

---

## 🏛️ Arquitectura

La solución está implementada bajo **Arquitectura Multicapa (Controller - Service - Integration - Repository)** en Java 21 con Spring Boot, separando estrictamente las responsabilidades por paquetes y aplicando patrones de diseño para garantizar los atributos de calidad del DAS V3.0.

```
Cliente / Consumidor
        ↓
   Controllers (REST API / Swagger)
        ↓
  Services & Domain (BusquedaService + MotorCoincidencias)
        ↙           ↘
Integration Layer   Repositories (JPA)
    ↓                     ↓
FuentePatentesMockAdapter   PostgreSQL 16 (Flyway)
```

### Documentos de referencia
- `docs/arquitectura/` — Documento de Arquitectura de Software (DAS V3.0).
- `docs/backlog/` — Historias de usuario (HU-01 a HU-12).

---

## 🎯 Endpoints Principales

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/patentes/estado?formulaId=form-001` | Consulta el estado precalculado de una fórmula frente al barrido de patentes |
| `POST` | `/api/v1/patentes/busqueda` | Ejecuta la búsqueda y detección de conflictos para la fórmula indicada (`formulaId`) |

---

## 🛠️ Stack Tecnológico

### Backend
| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 (OpenJDK 21.0.13+9) |
| Framework API | Spring Boot 3.5.16 |
| Persistencia | Spring Data JPA + Flyway (migraciones) |
| Base de datos | PostgreSQL 16 |
| Pruebas & Cobertura | JUnit 5 + Mockito + JaCoCo (98% instrucciones) |
| Documentación API | SpringDoc OpenAPI (Swagger UI) |

### Infraestructura & Herramientas
| Componente | Tecnología |
|---|---|
| Contenedores | Docker & Docker Compose |
| CI/CD | GitHub Actions (`ci.yml`) |
| Calidad de Código | Checkstyle / SonarCloud |
| Monitoreo | Prometheus + Grafana |

---

## 💻 Requisitos Previos

- **Java JDK 21**.
- **Maven 3.10.0+**.
- **Docker Desktop** (opcional para contenedores).
- **Git**.

---

## 🚀 Inicio Rápido (Desarrollo Local)

### 1. Clonar el repositorio
```bash
git clone https://github.com/inmemorialake/patent-sweep.git
cd patent-sweep/backend
```

### 2. Configurar variables de entorno
Copia la plantilla y ajusta tus credenciales de PostgreSQL local:
```bash
cp .env.example .env
```

### 3. Compilar y verificar con Maven y JaCoCo
```bash
mvn clean verify
```

### 4. Ejecutar el servidor localmente
```bash
mvn spring-boot:run
```

### 5. Acceder a la documentación Swagger UI
Abre tu navegador en: **http://localhost:8085/swagger-ui.html**

---

## 🐳 Ejecución con Docker Compose

```bash
# 1. Construir las imágenes
docker compose build

# 2. Levantar los servicios en segundo plano
docker compose up -d

# 3. Verificar el estado de los contenedores
docker compose ps
```

---

## 🔄 Flujo de Trabajo y Calidad (CI/CD)

1. Trabajar en rama personal (`primerainicialnombre` + `primerapellido`).
2. Validar cambios localmente con `mvn clean verify` (incluye Checkstyle y JaCoCo).
3. Crear Pull Request hacia `develop`.
4. Pipeline automático en GitHub Actions valida compilación, pruebas y Quality Gate.

---

## 🧪 Cobertura de Tests (JaCoCo)

| Paquete / Módulo | Cobertura de Instrucciones | Estado |
|---|---|---|
| `co.edu.univalle.patentsweep.service` | 99% | ✅ Supera umbral |
| `co.edu.univalle.patentsweep.controller` | 100% | ✅ Supera umbral |
| `co.edu.univalle.patentsweep.integration.adapter` | 99% | ✅ Supera umbral |
| `co.edu.univalle.patentsweep.model` | 88% | ✅ Supera umbral |
| `co.edu.univalle.patentsweep.integration.adapter` | 99% | ✅ Supera umbral |
| **Global del Proyecto** | **98% (Instrucciones) / 94% (Ramas)** | **✅ Quality Gate Aprobado** |

---

## 👥 Equipo 1 — Patent Sweep

| Nombre | Rol |
|---|---|
| **Isabela Bermúdez Moreno** | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| **Andrés Gerardo González Rosero** | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| **Juan Marín Orozco** | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| **David Taborda Montenegro** | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| **Victoria Yuan Chen** | Desarrollo Backend, Pruebas Unitarias & CI/CD |

---

## 📚 Créditos y Curso

**750021C — Desarrollo de Software II**  
Semestre 2026-2 (Agosto - Diciembre)  
Profesor: Manuel Alejandro Pastrana Pardo, PhD.  
Escuela de Ingeniería de Sistemas y Computación — **Universidad del Valle**

---

## 📄 Licencia

Proyecto académico — Universidad del Valle. Todos los derechos reservados.