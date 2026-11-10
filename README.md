# 🛡️ Patent Sweep API — Cutit Saws

API REST del módulo de **Patent Sweep (Equipo 1)** para la plataforma modular de Cutit Saws — curso 750021C Desarrollo de Software II, Univalle. Gestiona la vigilancia automatizada y validación de patentes para proteger los diseños de fabricación. Cuenta con CI/CD automatizado con GitHub Actions, pruebas unitarias con JaCoCo y arquitectura modular en Java 21 con Spring Boot.

---

## 📋 Contexto del proyecto

Cutit Saws Ltd. es un fabricante boutique de motosierras hidráulicas de diamante que, tras el éxito de su modelo "Ripit 5000", enfrenta una demanda que supera su capacidad de manufactura. Su sistema homegrown de contabilidad e inventario ya no escala, por lo que la dirección definió como prioridad estratégica construir una plataforma backend modular basada en 5 APIs REST independientes.

Esta API (**Equipo 1**) es responsable de la **vigilancia y validación cruzada de riesgo de patentes**.

| Rol en la plataforma | API |
|---|---|
| Es consumida por | Materiales/Fórmulas (Equipo 1) — validación de componentes contra patentes |
| Es consumida por | Fabricación (Equipo 3) — validación de lotes de producción |

---

## 🎯 Endpoints mínimos

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/patentes/estado?formulaId=form-001` | Consulta el estado precalculado de una fórmula frente al barrido de patentes |
| `POST` | `/api/v1/patentes/busqueda` | Busca conflictos de patentes para la fórmula indicada en el cuerpo JSON (`formulaId`) |

---

## 🛠️ Stack tecnológico

- **Lenguaje / Framework:** Java 21 / Spring Boot 3.5.16
- **Base de datos:** PostgreSQL (con migraciones automatizadas vía Flyway)
- **Gestión de Dependencias:** Maven
- **Calidad y Cobertura:** JaCoCo (mínimo 81% de cobertura de líneas y ramas en cada uno de los paquetes)
- **Documentación:** SpringDoc OpenAPI (Swagger UI)
- **CI/CD:** GitHub Actions (build, pruebas unitarias y análisis de cobertura en `ubuntu-24.04`)
- **Contenedores:** Docker
- **Monitoreo:** Prometheus + Grafana

---

## 🚀 Cómo correr el proyecto localmente

> Para ejecutar sin Docker, prepara una base de datos PostgreSQL accesible desde tu equipo. Copia `patent-sweep/.env.example` a `patent-sweep/backend/.env` y ajusta el perfil, el puerto, las credenciales de PostgreSQL, el secreto JWT y las URLs de integración. Sustituye los nombres de host de Docker (`postgres` y `apis-simuladas`) por direcciones accesibles desde tu equipo. Para la alternativa con Docker, consulta [la guía de configuración](patent-sweep/README.md).

1. **Compilar y verificar pruebas unitarias / cobertura (desde la raíz del repositorio):**
   ```bash
   cd patent-sweep/backend
   mvn clean verify
   ```

2. **Ejecutar el servidor localmente:**
   ```bash
   mvn spring-boot:run
   ```

3. **Acceso a Swagger UI:**
   ```text
   http://localhost:8085/swagger-ui.html
   ```

---

## 🔄 Flujo de trabajo (Git)

- Versionamiento en GitHub con ramas protegidas (`main`, `develop`).
- Todo cambio se integra exclusivamente vía **Pull Request** con aprobación obligatoria de pares.
- Cada PR o Push dispara automáticamente el pipeline en GitHub Actions (`ci.yml`):
    1. Compilación del proyecto.
    2. Ejecución de pruebas unitarias (JUnit 5).
    3. Verificación de Quality Gate / Cobertura de JaCoCo (mínimo 80%).
- Sólo si el pipeline pasa en verde (`passed`), se permite la integración final a la rama principal.

---

## 📁 Estructura del proyecto

El backend está organizado bajo la siguiente estructura modular:
- `config`: Configuraciones globales de Spring (seguridad JWT, filtros de autenticación, beans asíncronos, caché, reloj, clientes REST y OpenAPI/Swagger).
- `controller`: Controladores REST que exponen los endpoints (`AuthController`, `BusquedaController`, `EstadoFormulaController`), apoyados por DTOs y mappers específicos.
- `exception`: Manejo centralizado de errores (`GlobalExceptionHandler`) y excepciones de negocio personalizadas (como conflictos de fórmulas, configuraciones no encontradas y fallas de integración).
- `integration`: Componentes de comunicación externa con otros módulos de la plataforma, conteniendo adaptadores, puertos (`MaterialesClient`, `NotificacionesClient`) y DTOs de transferencia.
- `model`: Entidades JPA de persistencia (`Configuracion`, `Conflicto`, `Formula`, `Patente`, `Usuario`, etc.) junto con sus enumeraciones (`enums`).
- `repository`: Interfaces de persistencia y acceso a datos basadas en Spring Data JPA (`FormulaRepository`, `PatenteRepository`, `UsuarioRepository`, etc.).
- `service`: Lógica de negocio principal del motor de coincidencias, servicios de búsqueda, gestión de estados y eventos (`BusquedaService`, `MotorCoincidencias`, `RevisionService`).

---

## 👥 Equipo 1 — Patent Sweep

| Nombre | Rol |
|---|---|
| Isabela Bermúdez Moreno | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| Andrés Gerardo González Rosero | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| Juan Marín Orozco | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| David Taborda Montenegro | Desarrollo Backend, Pruebas Unitarias & CI/CD |
| Victoria Yuan Chen | Desarrollo Backend, Pruebas Unitarias & CI/CD |

---

## 📚 Curso

**750021C — Desarrollo de Software II**  
Semestre 2026-2 (Agosto - Diciembre)  
Profesor: Manuel Alejandro Pastrana Pardo, PhD.  
Universidad del Valle

---

## 📄 Licencia

Proyecto académico — Universidad del Valle.