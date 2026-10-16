-- Esquema inicial de SaberData Colombia.
-- Lo gestiona únicamente Flyway (hibernate ddl-auto=validate).

-- ---------------------------------------------------------------
-- Usuarios de la aplicación (autenticación y roles)
-- ---------------------------------------------------------------
CREATE TABLE usuario (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    rol            VARCHAR(10)  NOT NULL CHECK (rol IN ('USER', 'ADMIN')),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------
-- Resultados Saber 11 (dataset ICFES 2022-4). Se crea vacía;
-- la carga de datos se hará en una etapa posterior.
-- ---------------------------------------------------------------
CREATE TABLE resultado_saber11 (
    id                     BIGSERIAL PRIMARY KEY,
    estu_genero            VARCHAR(50),
    estu_depto_reside      VARCHAR(100),
    fami_estratovivienda   VARCHAR(50),
    fami_educacionmadre    VARCHAR(100),
    fami_educacionpadre    VARCHAR(100),
    fami_tieneinternet     VARCHAR(10),
    fami_tienecomputador   VARCHAR(10),
    fami_tieneautomovil    VARCHAR(10),
    fami_tienelavadora     VARCHAR(10),
    fami_cuartoshogar      VARCHAR(30),
    fami_personashogar     VARCHAR(30),
    cole_naturaleza        VARCHAR(30),
    cole_area_ubicacion    VARCHAR(30),
    cole_jornada           VARCHAR(30),
    cole_caracter          VARCHAR(50),
    cole_calendario        VARCHAR(10),
    cole_depto_ubicacion   VARCHAR(100),
    rendimiento            VARCHAR(10) CHECK (rendimiento IN ('ALTO', 'BAJO'))
);

-- Índices sobre las columnas por las que se agrupa en las estadísticas.
CREATE INDEX idx_resultado_estrato      ON resultado_saber11 (fami_estratovivienda);
CREATE INDEX idx_resultado_jornada      ON resultado_saber11 (cole_jornada);
CREATE INDEX idx_resultado_naturaleza   ON resultado_saber11 (cole_naturaleza);
CREATE INDEX idx_resultado_depto_cole   ON resultado_saber11 (cole_depto_ubicacion);
CREATE INDEX idx_resultado_rendimiento  ON resultado_saber11 (rendimiento);

-- ---------------------------------------------------------------
-- Conversaciones y mensajes del chatbot
-- ---------------------------------------------------------------
CREATE TABLE conversacion (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    titulo      VARCHAR(200) NOT NULL,
    creada_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversacion_usuario ON conversacion (usuario_id);

CREATE TABLE mensaje (
    id               BIGSERIAL PRIMARY KEY,
    conversacion_id  UUID        NOT NULL REFERENCES conversacion (id) ON DELETE CASCADE,
    rol              VARCHAR(10) NOT NULL CHECK (rol IN ('USER', 'ASSISTANT')),
    contenido        TEXT        NOT NULL,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_mensaje_conversacion ON mensaje (conversacion_id, id);
