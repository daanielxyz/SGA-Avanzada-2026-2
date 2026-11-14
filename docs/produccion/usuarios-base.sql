-- Usuarios de la base publicada (DEC-121). Se ejecuta UNA vez por base nueva, como `postgres`, en el editor SQL de
-- Supabase o con psql. NO es una migración de Flyway: depende del proveedor y del ambiente.
-- Sin contraseñas: cada usuario nace sin clave (no puede entrar) y la clave se pone después desde psql con
-- \password <usuario>, para que no quede en el historial del editor SQL ni en git (guía en docs/produccion/LEEME.md).
--
--   sga_migrador  dueño de las tablas; solo lo usa Flyway al arrancar (DB_MIGRACION_USER).
--   sga_app       la aplicación: lee y escribe datos, sin crear ni borrar tablas (DB_USER).
--   sga_respaldo  solo lectura, para pg_dump en el workflow de respaldo.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'sga_migrador') THEN
        CREATE ROLE sga_migrador LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'sga_app') THEN
        CREATE ROLE sga_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'sga_respaldo') THEN
        CREATE ROLE sga_respaldo LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
    END IF;
END
$$;

-- La migración V4_1 (db/postgresql) necesita btree_gist para la restricción que impide vender dos veces la misma
-- noche. Crear extensiones es cosa del administrador: se crea aquí, en el esquema de extensiones de Supabase, y el
-- CREATE EXTENSION IF NOT EXISTS de la migración ya no hace nada.
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA extensions;

-- Las tablas viven en public. Solo el migrador crea en él; la aplicación y el respaldo solo lo usan. Supabase deja
-- crear en public a todos (PUBLIC), y así sga_app podía crear tablas: se le quita a PUBLIC y se da por nombre
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO postgres, sga_migrador;
GRANT USAGE ON SCHEMA public TO sga_app, sga_respaldo;

-- Permisos de lo que cree el migrador desde ahora (todas las tablas y secuencias de Flyway). Desde PostgreSQL 16,
-- quien crea un rol no es miembro de él, y solo un miembro puede fijar sus privilegios por defecto: postgres entra a
-- sga_migrador. Se queda como miembro: postgres ya administra la base, y quitarlo podría llevarse también el permiso
-- de administrar el rol que necesita \password
GRANT sga_migrador TO CURRENT_USER WITH INHERIT TRUE, SET TRUE;
ALTER DEFAULT PRIVILEGES FOR ROLE sga_migrador IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO sga_app;
ALTER DEFAULT PRIVILEGES FOR ROLE sga_migrador IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO sga_app;
ALTER DEFAULT PRIVILEGES FOR ROLE sga_migrador IN SCHEMA public
    GRANT SELECT ON TABLES TO sga_respaldo;
ALTER DEFAULT PRIVILEGES FOR ROLE sga_migrador IN SCHEMA public
    GRANT SELECT ON SEQUENCES TO sga_respaldo;

-- Defensa extra: los roles de la API de Supabase (apagada) no ven el esquema de la aplicación
REVOKE ALL ON SCHEMA public FROM anon, authenticated;

-- Una consulta trabada no retiene conexiones del pooler para siempre
ALTER ROLE sga_app SET statement_timeout = '30s';
ALTER ROLE sga_app SET idle_in_transaction_session_timeout = '60s';
ALTER ROLE sga_respaldo SET idle_in_transaction_session_timeout = '10min';

-- Comprobación: deben salir los tres, sin superusuario y sin poder crear bases ni roles
SELECT rolname, rolsuper, rolcreatedb, rolcreaterole, rolcanlogin
FROM pg_roles
WHERE rolname LIKE 'sga\_%'
ORDER BY rolname;
