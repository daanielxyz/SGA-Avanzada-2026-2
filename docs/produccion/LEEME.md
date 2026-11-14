# Producción: base, backend y copias de respaldo

Cómo está publicado el SGA y cómo se opera (DEC-125). Aquí no hay ninguna dirección, usuario ni clave: todo eso vive en Bitwarden (colección de la organización) y en el panel de cada proveedor. Si alguna vez aparece un secreto en git, en un commit o en un PR, se cambia ese mismo día.

## Qué hay publicado

| Pieza | Dónde | Notas |
|---|---|---|
| Base PostgreSQL | Supabase, proyecto `sga-demo`, East US (North Virginia) | API de datos apagada, TLS obligatorio, entrada por el *Session pooler* |
| Backend | Render, servicio `sga-demo-api`, Virginia, Docker desde `backend/` | Plan gratuito: se duerme a los 15 min sin uso y tarda ~1 min en despertar |
| Copias de respaldo | GitHub Actions, workflow «Respaldo de la base» | Diaria, a las 3 a. m. de Colombia; cifrada; se guarda 30 días |
| Secretos | Bitwarden, colección `SGA demo` de la organización | Cuentas de los proveedores a nombre de la empresa, con verificación en dos pasos |

## Dónde va cada secreto

| Nombre | Dónde se usa | Qué es |
|---|---|---|
| `postgres` de Supabase | Solo para administrar (psql, SQL Editor) | Lo genera Supabase al crear el proyecto. La aplicación no lo usa |
| `DB_URL` | Render | `jdbc:postgresql://<host del Session pooler>:5432/postgres?sslmode=require` |
| `DB_USER`, `DB_PASSWORD` | Render | `sga_app.<referencia del proyecto>` y su clave |
| `SPRING_FLYWAY_USER`, `SPRING_FLYWAY_PASSWORD` | Render | `sga_migrador.<referencia>` y su clave |
| `DB_POOL_MAXIMO` | Render | `5` en el plan gratuito |
| `SGA_JWT_SECRETO` | Render | 64 caracteres al azar |
| `SGA_ADMIN_CORREO`, `SGA_ADMIN_CLAVE` | Render | El administrador de la empresa (la clave con mayúscula y número) |
| `SGA_DATOS_DEMO`, `SGA_DEMO_CLAVE` | Render | `true` en la demo; la clave de los usuarios de prueba (no la del README) |
| `SGA_DOCUMENTACION_API` | Render | `true` hasta la entrega; `false` en producción |
| `SGA_CORREO_MODO` | Render | `local` (los correos quedan en el log de Render) |
| `RESPALDO_PGHOST`, `RESPALDO_PGUSER`, `RESPALDO_PGPASSWORD` | GitHub → Settings → Secrets and variables → Actions → *Secrets* | El host del pooler, `sga_respaldo.<referencia>` y su clave. Los carga el dueño del repositorio |
| `RESPALDO_AGE_PUBLICA` | GitHub → … → *Variables* | Solo la línea `age1…`. Es pública: no abre nada |
| Clave privada de age (`AGE-SECRET-KEY-1…`) | **Solo Bitwarden** | La única llave de las copias. Sin ella no se puede abrir ninguna |

Las claves se generan al azar, de 32 caracteres, solo letras y números. En PowerShell (copia al portapapeles sin mostrarla; para 64, cambiar `-lt 32` por `-lt 64`):

```powershell
$c=[char[]]'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'; $r=[System.Security.Cryptography.RandomNumberGenerator]::Create(); $b=New-Object byte[] 1; $s=''; while($s.Length -lt 32){ $r.GetBytes($b); if($b[0] -lt 248){ $s+=$c[$b[0] % 62] } }; $s | Set-Clipboard; Remove-Variable s; 'Copiada al portapapeles'
```

Después de pegarla en Bitwarden: `Set-Clipboard -Value ' '` y borrar la entrada del historial (Win + V).

## Conectarse a la base con psql

PostgreSQL 18 trae `psql`. El host y la referencia están en Supabase → **Connect** → *Connection String* → *Method: Session pooler*. Desde cualquier carpeta:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" "host=<host> port=5432 dbname=postgres user=postgres.<referencia> sslmode=require"
```

El usuario siempre lleva la referencia después de un punto (`sga_app.<referencia>`): así sabe el pooler a qué proyecto entrar.

## Crear un ambiente nuevo (otro hotel, producción)

1. **Supabase**: proyecto nuevo en la organización de la empresa, en la misma región que el backend. Contraseña de `postgres` con «Generate», a Bitwarden. **Data API apagada**, sin exponer tablas y **sin RLS automático** (con RLS y sin reglas, `sga_app` no podría leer nada). Luego, *Enforce SSL* encendido.
2. **Usuarios**: en el SQL Editor, todo `docs/produccion/usuarios-base.sql` → *Run*. Debe terminar con la tabla de los tres usuarios, sin superusuario.
3. **Claves**: tres nuevas en Bitwarden y, en psql como `postgres`, `\password sga_migrador`, `\password sga_app` y `\password sga_respaldo` (pide la clave dos veces; no se ve al pegar).
4. **Comprobar**: entrar como `sga_app.<referencia>`, `select current_user;` debe decir `sga_app` y `create table prueba (i int);` **debe fallar** con `permission denied for schema public`.
5. **Render**: *New → Web Service*, región igual a la base, *Root Directory* `backend`, *Language* Docker, *Dockerfile Path* vacío (o `./Dockerfile`; es relativo a `backend`), *Health Check Path* `/actuator/health`. Las variables de la tabla de arriba **antes** del primer despliegue: si arranca sin `SGA_DEMO_CLAVE`, los usuarios de prueba nacen con la clave del README y hay que vaciar la base (abajo).
6. **Primer arranque**: tarda de 2 a 5 minutos (Flyway crea las tablas). Mientras tanto Render repite «No open ports detected»; es normal. Al final, `/actuator/health` responde `{"status":"UP"}`.
7. **Respaldo**: los tres secretos y la variable de GitHub (una pareja de age por ambiente, o la misma si lo decide el equipo), y lanzar el workflow a mano una vez.
8. **Simulacro**: restaurar esa primera copia (abajo) antes de dar el ambiente por listo.

## Desplegar una versión nueva

- Hoy Render usa el repositorio público sin despliegue automático: *Manual Deploy → Deploy latest commit* desde la rama configurada (`develop` cuando se integre DEC-125).
- Las migraciones nuevas se aplican solas al arrancar. **Una migración ya integrada nunca se edita**: Flyway la rechaza (`checksum mismatch`) y el backend no arranca.
- Si cambian `application.properties` o la configuración, es núcleo (`feature/nucleo/…`).

## Copias de respaldo

- Corren solas cada día desde `develop`. A mano: GitHub → **Actions → Respaldo de la base → Run workflow**.
- Cada copia se restaura antes de guardarse; si la restauración no coincide con la base, el workflow falla y GitHub manda un correo a quien lo cambió por última vez. **Una ejecución en rojo se revisa el mismo día.**
- Los archivos (`sga-AAAAMMDD-HHMM.dump.age`) quedan en la ejecución por 30 días.

### Restaurar una copia (y el simulacro mensual)

Se necesitan Docker Desktop, `age` (`winget install --id FiloSottile.age -e`) y la clave privada de Bitwarden.

1. Descargar el archivo de la ejecución (en la página de la ejecución, *Artifacts*, o `gh run download <id> -n <nombre>`) en una carpeta vacía, fuera del repositorio.
2. Descifrar sin dejar la clave en el portapapeles ni en el historial:

   ```powershell
   $d = "<carpeta del archivo>"
   $k = "$d\clave-temporal.txt"
   $s = Read-Host "Pega la clave privada de age y presiona Enter" -AsSecureString
   [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)).Trim() | Set-Content -Encoding ascii $k
   age -d -i $k -o "$d\sga.dump" "$d\<archivo>.dump.age"
   Remove-Item $k; Remove-Variable s; Set-Clipboard -Value ' '
   ```

   `unknown identity type` = lo pegado no era la línea `AGE-SECRET-KEY-1…` (74 caracteres). `no identity matched` = la clave no corresponde a la pública de GitHub: hay que generar otra pareja y volver a respaldar.
3. Restaurar en un PostgreSQL temporal de la misma versión que la base (la copia trae su propio `CREATE SCHEMA public`; `btree_gist` va aparte):

   ```bash
   docker run -d --name sga-restaurada -e POSTGRES_PASSWORD=solo-local -p 55432:5432 postgres:17
   docker exec sga-restaurada psql -U postgres -c "drop schema public cascade" -c "create schema extensions" -c "create extension btree_gist with schema extensions"
   docker cp sga.dump sga-restaurada:/tmp/sga.dump
   docker exec sga-restaurada pg_restore -U postgres -d postgres --no-owner --no-privileges --exit-on-error /tmp/sga.dump
   ```
4. Comprobar: tablas en `public`, `flyway_schema_history`, apartamentos, usuarios y que exista `ex_reserva_sin_solape`. Arrancar el backend contra ella (`DB_URL=jdbc:postgresql://localhost:55432/postgres`, `DB_USER=postgres`, `SGA_DATOS_DEMO=false`) y entrar con una cuenta.
5. Borrar todo: `docker rm -f sga-restaurada`, la carpeta con `sga.dump` y el `.age`.
6. Anotar fecha y resultado en DEC-125.

Para una restauración de verdad (la base se dañó): la copia se restaura en un proyecto nuevo de Supabase con los usuarios de `usuarios-base.sql`, restaurando como `sga_migrador` para que siga siendo el dueño, y se cambia `DB_URL` en Render.

## Vaciar la demo y volver a cargarla

Solo mientras la base sea de demo. Los cargadores no hacen nada si ya existe el APT-101, así que los datos de prueba nuevos solo entran a una base vacía. Antes de la entrega del 27 nov, con todo integrado en `develop`:

1. Render → el servicio → *Settings* → **Suspend**.
2. Respaldo a mano (por si acaso) y esperar que quede en verde.
3. En psql como `postgres`: `select tableowner, count(*) from pg_tables where schemaname = 'public' group by tableowner;` debe decir solo `sga_migrador`. Entonces `DROP OWNED BY sga_migrador;` y `\dt` debe responder `Did not find any relations`. Borra tablas, secuencias e historial de Flyway; no borra usuarios ni claves.
4. Volver a ejecutar todo `usuarios-base.sql` (`DROP OWNED` también le quitó al migrador sus permisos).
5. Render → **Resume** (o *Manual Deploy*) y esperar `UP`.

## Cambiar una clave

| Clave | Pasos |
|---|---|
| De un usuario de la base | Nueva en Bitwarden → psql como `postgres`, `\password <usuario>` → actualizarla en Render (`DB_PASSWORD` o `SPRING_FLYWAY_PASSWORD`) o en GitHub (`RESPALDO_PGPASSWORD`) → desplegar o relanzar el respaldo |
| `postgres` de Supabase | Supabase → *Database → Settings → Reset database password* → Bitwarden |
| `SGA_JWT_SECRETO` | Nueva en Render y desplegar. Todas las sesiones abiertas se cierran |
| `SGA_ADMIN_CLAVE`, `SGA_DEMO_CLAVE` | Solo valen al crear los usuarios: después se cambian desde la aplicación (mi perfil o usuarios). En una base vacía, cambiar la variable antes del primer arranque |
| Pareja de age | Nueva pareja a Bitwarden → `RESPALDO_AGE_PUBLICA` en GitHub → relanzar el respaldo → simulacro. Guardar la privada vieja mientras haya copias cifradas con ella (30 días) |

## Si algo falla

| Síntoma | Causa probable |
|---|---|
| `could not translate host name` en el respaldo | El secreto del host tiene un espacio o un salto de línea (el workflow ya los quita) o está mal escrito |
| `password authentication failed` | La clave en Render o GitHub no es la que se puso con `\password`, o al usuario le falta `.<referencia>` |
| `permission denied for schema public` al arrancar | Flyway entró con `sga_app`: falta `SPRING_FLYWAY_USER` o `SPRING_FLYWAY_PASSWORD` en Render |
| `permission denied to create extension` | No se ejecutó `usuarios-base.sql`, que crea `btree_gist` |
| `Validate failed` / `checksum mismatch` | Se editó una migración ya aplicada, o la base no quedó vacía antes de recargar |
| `OutOfMemoryError` o reinicios en Render | 512 MB no alcanzan: bajar `MaxRAMPercentage` en el Dockerfile o subir de plan |
| El estado en Render se queda en *In progress* | Normal hasta 5 min en el primer arranque; después, revisar los logs (`ERROR`, `APPLICATION FAILED TO START`) |
| La base no responde tras días sin uso | Supabase gratuito pausó el proyecto: *Restore* en su panel |

## Pasar a la etapa 2 (después de la entrega)

1. `gitleaks` sobre toda la historia (`docker run --rm -v "<repo>:/repo" zricethezav/gitleaks:v8.24.3 git /repo --redact`): si aparece algo, se cambia esa clave.
2. Crear la organización de GitHub, transferirle el repositorio y ponerlo **privado**. Activar Dependabot y `gitleaks` como chequeo obligatorio de los PR.
3. **Cambiar todas las claves** de la etapa 1 (tabla de arriba), aunque no se hayan filtrado: el repositorio fue público.
4. Supabase Pro y Render de pago, siempre encendido, en la misma región. Cerrar la base a las IP del backend si el plan lo permite. `sslmode=verify-full`.
5. Un ambiente de **producción por cliente** con base limpia (`SGA_DATOS_DEMO=false`, `SGA_DOCUMENTACION_API=false`); el demo actual queda para ventas.
6. Conectar Render al repositorio privado (despliegue automático desde `main`) y mover las copias a Cloudflare R2 (30 diarias y 12 mensuales).
7. Dominio propio, frontends en Cloudflare Pages (DEC-83) con `SGA_CORS_ORIGENES`, correo transaccional con SPF, DKIM y DMARC, y un monitor de disponibilidad sobre `/actuator/health`.
8. Antes del primer cliente: confirmar con un asesor lo que exige la Ley 1581 de 2012 para guardar datos personales de huéspedes fuera del país, y publicar la política de tratamiento de datos.
