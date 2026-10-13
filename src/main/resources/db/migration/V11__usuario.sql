-- Usuario del módulo de acceso (USU-01 · ROL-01). Vive fuera del dominio (USU-07): ninguna tabla del negocio lo
-- referencia con llave foránea; registros, salidas y autorizaciones guardan su id como autor.
CREATE TABLE usuario (
    id               VARCHAR(40)  PRIMARY KEY,
    alojamiento_id   VARCHAR(40)  NOT NULL,                                     -- DEC-26
    nombre           VARCHAR(120) NOT NULL,
    correo           VARCHAR(150) NOT NULL UNIQUE,                              -- USU-01: identifica al usuario
    contrasena       VARCHAR(100) NOT NULL,                                     -- USU-02: solo el resumen BCrypt
    rol              VARCHAR(20)  NOT NULL,                                     -- ROL-02: exactamente uno
    -- Los pide USU-01 al registrarse; el primer administrador, que nace al arrancar, los completa después (DEC-67)
    telefono         VARCHAR(30),
    tipo_documento   VARCHAR(20),
    numero_documento VARCHAR(30),
    fecha_nacimiento DATE,
    eliminado        BOOLEAN      NOT NULL DEFAULT FALSE,                       -- USU-05: eliminación lógica
    version          BIGINT       NOT NULL,
    CHECK (rol IN ('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR', 'PERSONAL_SERVICIO'))
);

CREATE SEQUENCE seq_usuario START WITH 1;
