-- Restricciones de datos: la base rechaza valores fuera de dominio aunque el código se equivoque.
-- Los estados son texto en la base; cada CHECK repite los valores del enum de Java correspondiente.

ALTER TABLE usuario
    ADD CONSTRAINT ck_usuario_username CHECK (char_length(btrim(username)) > 0),
    ADD CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMINISTRADOR', 'SERVICIO'));

ALTER TABLE patente
    ADD CONSTRAINT ck_patente_codigo CHECK (codigo_patente ~ '^PAT-[0-9]{6}$'),
    ADD CONSTRAINT ck_patente_titulo CHECK (char_length(btrim(titulo)) > 0),
    ADD CONSTRAINT ck_patente_vigencia
        CHECK (estado_vigencia IS NULL OR estado_vigencia IN ('CONCEDIDA', 'EN_TRAMITE', 'EXPIRADA')),
    ADD CONSTRAINT ck_patente_fechas CHECK (
        (fecha_concesion IS NULL OR fecha_solicitud IS NULL OR fecha_concesion >= fecha_solicitud)
        AND (fecha_vencimiento IS NULL OR fecha_solicitud IS NULL OR fecha_vencimiento >= fecha_solicitud));

ALTER TABLE conflicto
    ADD CONSTRAINT ck_conflicto_formula CHECK (formula_id ~ '^form-[0-9]{3}$'),
    ADD CONSTRAINT ck_conflicto_version CHECK (char_length(btrim(version_formula)) > 0),
    ADD CONSTRAINT ck_conflicto_estado CHECK (estado IN ('DETECTADO', 'DESCARTADO')),
    -- Un descarte siempre lleva motivo y fecha; un conflicto detectado no los tiene.
    ADD CONSTRAINT ck_conflicto_descarte CHECK (
        (estado = 'DESCARTADO' AND char_length(btrim(coalesce(motivo_descarte, ''))) > 0
            AND fecha_descarte IS NOT NULL)
        OR (estado = 'DETECTADO' AND motivo_descarte IS NULL AND fecha_descarte IS NULL));

ALTER TABLE revision_formula
    ADD CONSTRAINT ck_revision_formula CHECK (formula_id ~ '^form-[0-9]{3}$'),
    ADD CONSTRAINT ck_revision_version CHECK (version_formula IS NULL OR char_length(btrim(version_formula)) > 0),
    ADD CONSTRAINT ck_revision_origen
        CHECK (origen IN ('BARRIDO_AUTOMATICO', 'BUSQUEDA_MANUAL', 'EVALUACION_AUTOMATICA')),
    ADD CONSTRAINT ck_revision_resultado CHECK (resultado IN ('CON_CONFLICTO', 'SIN_CONFLICTO', 'FALLO_TECNICO'));

ALTER TABLE evaluacion_formula
    ADD CONSTRAINT ck_evaluacion_formula CHECK (formula_id ~ '^form-[0-9]{3}$'),
    ADD CONSTRAINT ck_evaluacion_ticket CHECK (char_length(btrim(ticket)) > 0);

ALTER TABLE notificacion
    ADD CONSTRAINT ck_notificacion_tipo CHECK (char_length(btrim(tipo)) > 0),
    ADD CONSTRAINT ck_notificacion_estado CHECK (estado IN ('PENDIENTE_NOTIFICACION', 'NOTIFICADO')),
    ADD CONSTRAINT ck_notificacion_intentos CHECK (intentos >= 0);

ALTER TABLE configuracion
    ADD CONSTRAINT ck_configuracion_frecuencia CHECK (frecuencia_valor > 0 AND frecuencia_horas > 0),
    ADD CONSTRAINT ck_configuracion_unidad CHECK (frecuencia_unidad IN ('HORAS', 'DIAS', 'SEMANAS')),
    ADD CONSTRAINT ck_configuracion_rango CHECK (rango_cantidad IS NULL OR rango_cantidad > 0),
    ADD CONSTRAINT ck_configuracion_rango_unidad CHECK (rango_unidad IS NULL OR rango_unidad IN ('MESES', 'ANOS')),
    ADD CONSTRAINT ck_configuracion_clasificaciones CHECK (jsonb_typeof(clasificaciones) = 'array');

ALTER TABLE clasificacion_patente
    ADD CONSTRAINT ck_clasificacion_codigo CHECK (char_length(btrim(codigo)) > 0),
    ADD CONSTRAINT ck_clasificacion_tipo CHECK (tipo IN ('IPC', 'CPC'));
