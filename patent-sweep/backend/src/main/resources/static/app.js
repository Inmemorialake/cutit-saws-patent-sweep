(() => {
  "use strict";

  /* ---------- Configuración ---------- */
  const API = "/api/v1";
  const CLAVE_SESION = "ps.sesion";
  const FORMATO_FORMULA = /^form-\d{3}$/;
  const MSG_FORMATO =
    "El código de la fórmula es obligatorio y debe tener el formato form-NNN";

  const ESTADOS_FORMULA = {
    LIBRE: ["Libre", "ok"],
    CONFLICTO_DETECTADO: ["Conflicto detectado", "error"],
    PENDIENTE_REVISION: ["Pendiente de revisión", "alerta"],
    NO_PROCESADO: ["No procesado", "neutro"],
  };
  const ESTADOS_PATENTE = { CONCEDIDA: "ok", SOLICITADA: "alerta" };

  const $ = (id) => document.getElementById(id);

  /* ---------- Sesión (solo sessionStorage; nunca la contraseña) ---------- */
  let temporizador = null;

  function leerSesion() {
    try {
      const s = JSON.parse(sessionStorage.getItem(CLAVE_SESION));
      if (s && s.accessToken && s.expiraEn > Date.now()) return s;
    } catch (_) {
      /* sesión corrupta */
    }
    sessionStorage.removeItem(CLAVE_SESION);
    return null;
  }

  function guardarSesion(r) {
    const sesion = {
      accessToken: r.accessToken,
      username: r.username,
      role: r.role,
      expiraEn: Date.now() + (Number(r.expiresIn) || 3600) * 1000,
    };
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    return sesion;
  }

  function cerrarSesion(mensaje) {
    sessionStorage.removeItem(CLAVE_SESION);
    clearTimeout(temporizador);
    limpiarResultados();
    mostrarLogin(mensaje);
  }

  function programarExpiracion(sesion) {
    clearTimeout(temporizador);
    const resto = sesion.expiraEn - Date.now();
    temporizador = setTimeout(
      () => cerrarSesion("Su sesión expiró. Inicie sesión nuevamente."),
      Math.min(Math.max(resto, 0), 2147483647),
    );
  }

  /* ---------- Cliente HTTP ---------- */
  class ErrorApi extends Error {
    constructor(estado, titulo, detalle, traceId) {
      super(detalle || titulo);
      this.estado = estado;
      this.titulo = titulo;
      this.traceId = traceId;
    }
  }

  async function llamar(
    ruta,
    { metodo = "GET", cuerpo, conToken = true } = {},
  ) {
    const cabeceras = { Accept: "application/json" };
    if (cuerpo !== undefined) cabeceras["Content-Type"] = "application/json";
    if (conToken) {
      const s = leerSesion();
      if (!s) {
        cerrarSesion("Su sesión expiró. Inicie sesión nuevamente.");
        throw new ErrorApi(401, "Sesión expirada");
      }
      cabeceras.Authorization = "Bearer " + s.accessToken;
    }

    let resp;
    try {
      resp = await fetch(API + ruta, {
        method: metodo,
        headers: cabeceras,
        body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
      });
    } catch (_) {
      throw new ErrorApi(
        0,
        "Sin conexión",
        "No se pudo contactar con el servidor. Intente de nuevo.",
      );
    }

    if (resp.ok) return resp.json();

    let p = {};
    try {
      p = await resp.json();
    } catch (_) {
      /* cuerpo vacío o no JSON */
    }
    const err = new ErrorApi(resp.status, p.title, p.detail, p.traceId);

    // 401 en rutas protegidas = token vencido/ inválido: cerrar sesión y volver al login
    if (resp.status === 401 && conToken) {
      cerrarSesion("Su sesión expiró. Inicie sesión nuevamente.");
    }
    throw err;
  }

  /* ---------- Utilidades de DOM ---------- */
  function el(etiqueta, props = {}, ...hijos) {
    const n = document.createElement(etiqueta);
    for (const [k, v] of Object.entries(props)) {
      if (k === "class") n.className = v;
      else if (v !== null && v !== undefined) n.setAttribute(k, v);
    }
    for (const h of hijos)
      n.append(h instanceof Node ? h : document.createTextNode(h ?? ""));
    return n;
  }

  function mostrarAviso(contenedor, tipo, titulo, detalle, traceId) {
    contenedor.className = "aviso " + tipo;
    contenedor.replaceChildren(el("strong", {}, titulo));
    if (detalle) contenedor.append(detalle);
    if (traceId) contenedor.append(el("small", {}, "Referencia: " + traceId));
    contenedor.hidden = false;
  }

  function cargando(contenedor, texto) {
    contenedor.replaceChildren(
      el(
        "div",
        { class: "cargando" },
        el("span", { class: "spinner", "aria-hidden": "true" }),
        texto,
      ),
    );
  }

  const fmtFecha = (v) => {
    if (!v) return "—";
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(v);
    return m ? `${m[3]}/${m[2]}/${m[1]}` : String(v);
  };
  const fmtFechaHora = (v) => {
    if (!v) return "—";
    const d = new Date(v);
    return isNaN(d)
      ? String(v)
      : d.toLocaleString("es-CO", { dateStyle: "short", timeStyle: "short" });
  };

  function insignia(texto, clase) {
    return el("span", { class: "insignia " + clase }, texto);
  }

  /* Texto de error según el ProblemDetail y el código HTTP */
  function describirError(e) {
    const base = {
      400: ["Solicitud inválida", "error"],
      401: ["No autenticado", "error"],
      403: ["Acceso denegado", "error"],
      404: ["Recurso no encontrado", "alerta"],
      409: ["Operación no permitida", "alerta"],
      503: ["Servicio no disponible", "error"],
    }[e.estado] || ["Error", "error"];
    const titulo = e.titulo || base[0];
    let detalle = e.message;
    if (e.estado === 403 && (!detalle || detalle === e.titulo))
      detalle = "No tiene permisos para realizar esta operación.";
    if (e.estado === 503 && !detalle)
      detalle =
        "Un servicio externo no respondió. Intente nuevamente en unos minutos.";
    if (e.estado >= 500 && e.estado !== 503)
      detalle = detalle || "Error interno del servidor.";
    return { titulo, detalle, tipo: base[1], traceId: e.traceId };
  }

  /* ---------- Navegación ---------- */
  function mostrarLogin(mensaje) {
    $("vista-app").hidden = true;
    $("vista-login").hidden = false;
    $("form-login").reset();
    const aviso = $("login-aviso");
    if (mensaje) mostrarAviso(aviso, "info", mensaje);
    else aviso.hidden = true;
    $("login-usuario").focus();
  }

  function mostrarApp(sesion) {
    $("vista-login").hidden = true;
    $("vista-app").hidden = false;
    $("sesion-info").textContent = `${sesion.username} · ${sesion.role}`;

    // Mostrar/ocultar opciones según rol (el backend sigue autorizando)
    document.querySelectorAll(".nav-btn").forEach((b) => {
      const roles = b.dataset.roles;
      b.hidden = !!roles && !roles.split(",").includes(sesion.role);
    });
    programarExpiracion(sesion);
    const inicial = sesion.role === "ADMINISTRADOR" ? "busqueda" : "estado";
    irA(inicial);
  }

  function irA(vista) {
    $("aviso-global").hidden = true;
    for (const v of ["busqueda", "estado"])
      $("pantalla-" + v).hidden = v !== vista;
    document
      .querySelectorAll(".nav-btn")
      .forEach((b) => b.classList.toggle("activo", b.dataset.vista === vista));
    $(vista === "busqueda" ? "busqueda-formula" : "estado-formula").focus();
  }

  function limpiarResultados() {
    for (const id of [
      "busqueda-estado",
      "busqueda-resultado",
      "estado-estado",
      "estado-resultado",
    ])
      $(id).replaceChildren();
    for (const id of ["busqueda-error-campo", "estado-error-campo"])
      $(id).hidden = true;
    $("aviso-global").hidden = true;
  }

  /* ---------- Login ---------- */
  $("form-login").addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const username = $("login-usuario").value.trim();
    const password = $("login-clave").value;
    const aviso = $("login-aviso");
    if (!username || !password) {
      return mostrarAviso(aviso, "error", "Ingrese usuario y contraseña.");
    }
    const boton = $("login-boton");
    boton.disabled = true;
    boton.textContent = "Ingresando…";
    aviso.hidden = true;
    try {
      const r = await llamar("/auth/login", {
        metodo: "POST",
        cuerpo: { username, password },
        conToken: false,
      });
      $("login-clave").value = "";
      mostrarApp(guardarSesion(r));
    } catch (e) {
      // 401 en login = credenciales incorrectas (no es sesión vencida)
      const d = describirError(e);
      mostrarAviso(
        aviso,
        "error",
        e.estado === 401 ? "Credenciales incorrectas" : d.titulo,
        e.estado === 401 ? "Verifique su usuario y contraseña." : d.detalle,
        d.traceId,
      );
    } finally {
      boton.disabled = false;
      boton.textContent = "Ingresar";
    }
  });

  $("btn-salir").addEventListener("click", () => cerrarSesion());
  document
    .querySelectorAll(".nav-btn")
    .forEach((b) => b.addEventListener("click", () => irA(b.dataset.vista)));

  /* ---------- Validación común del formulaId ---------- */
  function validarFormula(input, errorEl) {
    const valor = input.value.trim();
    const ok = FORMATO_FORMULA.test(valor);
    input.setAttribute("aria-invalid", String(!ok));
    errorEl.hidden = ok;
    errorEl.textContent = ok ? "" : MSG_FORMATO;
    return ok ? valor : null;
  }

  /* ---------- HU-02: Búsqueda manual ---------- */
  $("form-busqueda").addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const formulaId = validarFormula(
      $("busqueda-formula"),
      $("busqueda-error-campo"),
    );
    const estado = $("busqueda-estado"),
      salida = $("busqueda-resultado"),
      boton = $("busqueda-boton");
    salida.replaceChildren();
    if (!formulaId) return;

    boton.disabled = true;
    cargando(estado, "Buscando conflictos…");
    try {
      const r = await llamar("/patentes/busqueda", {
        metodo: "POST",
        cuerpo: { formulaId },
      });
      estado.replaceChildren();
      pintarBusqueda(r, salida);
    } catch (e) {
      if (e.estado === 401) return; // ya se redirigió al login
      const d = describirError(e);
      const caja = el("div");
      estado.replaceChildren(caja);
      mostrarAviso(caja, d.tipo, d.titulo, d.detalle, d.traceId);
    } finally {
      boton.disabled = false;
    }
  });

  function pintarBusqueda(r, salida) {
    const lista = r.conflictos || [];
    if (lista.length === 0) {
      const caja = el("div");
      salida.append(caja);
      return mostrarAviso(
        caja,
        "info",
        "Sin conflictos",
        r.mensaje || "No se encontraron conflictos para la fórmula.",
      );
    }
    if (r.mensaje) salida.append(el("p", { class: "resumen" }, r.mensaje));
    const cols = [
      "conflictoId",
      "tituloPatente",
      "tipoProducto",
      "estadoPatente",
      "fechaConcesion",
    ];
    const tabla = el(
      "table",
      {},
      el(
        "thead",
        {},
        el("tr", {}, ...cols.map((c) => el("th", { scope: "col" }, c))),
      ),
      el(
        "tbody",
        {},
        ...lista.map((c) =>
          el(
            "tr",
            {},
            el("td", {}, c.conflictoId),
            el("td", { class: "ancho" }, c.tituloPatente),
            el("td", {}, c.tipoProducto),
            el(
              "td",
              {},
              insignia(
                c.estadoPatente || "—",
                ESTADOS_PATENTE[c.estadoPatente] || "neutro",
              ),
            ),
            el("td", {}, fmtFecha(c.fechaConcesion)),
          ),
        ),
      ),
    );
    salida.append(el("div", { class: "tabla-wrap" }, tabla));
  }

  /* ---------- HU-04: Estado de fórmula ---------- */
  $("form-estado").addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const formulaId = validarFormula(
      $("estado-formula"),
      $("estado-error-campo"),
    );
    const estado = $("estado-estado"),
      salida = $("estado-resultado"),
      boton = $("estado-boton");
    salida.replaceChildren();
    if (!formulaId) return;

    boton.disabled = true;
    cargando(estado, "Consultando estado…");
    try {
      const r = await llamar(
        "/patentes/estado/" + encodeURIComponent(formulaId),
      );
      estado.replaceChildren();
      const [texto, clase] = ESTADOS_FORMULA[r.estado] || [
        r.estado || "—",
        "neutro",
      ];
      salida.append(
        el(
          "dl",
          { class: "detalle tarjeta" },
          el("dt", {}, "formulaId"),
          el("dd", {}, r.formulaId),
          el("dt", {}, "estado"),
          el("dd", {}, insignia(texto, clase)),
          el("dt", {}, "fechaUltimaRevision"),
          el("dd", {}, fmtFechaHora(r.fechaUltimaRevision)),
          el("dt", {}, "mensaje"),
          el("dd", {}, r.mensaje || "—"),
        ),
      );
    } catch (e) {
      if (e.estado === 401) return;
      const d = describirError(e);
      const caja = el("div");
      estado.replaceChildren(caja);
      mostrarAviso(caja, d.tipo, d.titulo, d.detalle, d.traceId);
    } finally {
      boton.disabled = false;
    }
  });

  /* ---------- Arranque ---------- */
  const sesion = leerSesion();
  if (sesion) mostrarApp(sesion);
  else mostrarLogin();
})();
