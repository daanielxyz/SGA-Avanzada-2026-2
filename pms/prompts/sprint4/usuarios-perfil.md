# Prompt para Claude Design: usuarios, mi perfil y cuenta (sprint 4, Reyes)

**Adjuntos**: `brandbook.png`; de `mockups-meridian-sprint3-preparacion/preparacion/`, `base.css` y `LEEME.md`; de `mockups-meridian-sprint3-llegada/llegada/`, `S3DialogoTra.html` y su PNG; de `capturas/`, `reservas.png`, `detalle.png` y `ingresar.png` (la pantalla de iniciar sesión).

Copia desde aquí:

---

Continúa el diseño de Meridian, el PMS de un hotel. Sigue exactamente los mockups adjuntos: mismo tema oscuro, mismos componentes y mismo `base.css`. Las capturas son del PMS ya programado y mandan sobre los mockups viejos. Agrega componentes nuevos solo si no existe uno equivalente y lístalos al final.

Entrega: un `.html` por pantalla o por hoja de estados, su `png/` a tamaño real, `base.css` con lo nuevo al final bajo «Sprint 4 · usuarios» y un `LEEME.md` con pantallas, componentes nuevos y «Lo que falta o quedó dudoso». Nombres con prefijo `S4`. Cada pantalla a 1280 px y, con sufijo `Celular`, a 390 px. Los diálogos en el celular ocupan toda la pantalla.

## Reglas
- No inventes datos ni acciones: usa solo lo que se describe aquí. Si crees que falta algo, anótalo en «Lo que falta o quedó dudoso» y no lo dibujes.
- Español de Colombia, sin términos técnicos. Los roles se muestran como «Recepción», «Personal de servicio» y «Administración», nunca `RECEPCIONISTA`.
- Oliva y Terracota no se usan como color de texto. Ningún estado depende solo del color.
- Contraseña: al menos 8 caracteres, una mayúscula y un número. Se dice antes de escribirla, no solo al fallar. Todo campo de contraseña tiene «Ver».

## Cómo funcionan las cuentas del personal
- El **administrador** crea la cuenta con nombre, correo, teléfono, documento (CC, CE, Pasaporte o TI), fecha de nacimiento y rol. La cuenta nace **sin activar**: a la persona le llega un correo con un código de 6 dígitos que vale 24 horas, y con él fija su contraseña en «Activar mi cuenta». Hasta entonces no puede entrar.
- El administrador cambia los datos y el rol de cualquiera (no el correo). Cambiar el rol cierra las sesiones abiertas de esa persona. **Eliminar** es lógico: la cuenta deja de entrar y queda en la lista como eliminada, sin acciones.
- Cada persona, en «Mi perfil», cambia su nombre, teléfono, documento y fecha de nacimiento (no su correo ni su rol) y su contraseña (pide la actual).
- **Olvidé mi contraseña**: se escribe el correo y llega un código de 15 minutos. La respuesta es la misma exista o no el correo («Si el correo está registrado, te llegó un código»). Con el código se fija la contraseña nueva.

## Datos de ejemplo
Hoy es 17 nov 2026. Alojamiento «Puerta al Sol». Administración: Andrés Felipe Gómez (andres.gomez@ejemplo.co).

| Nombre | Correo | Rol | Estado |
|---|---|---|---|
| Andrés Felipe Gómez | andres.gomez@ejemplo.co | Administración | Activa |
| María Fernanda Ospina | maria.ospina@ejemplo.co | Recepción | Activa |
| Carlos Restrepo | carlos.restrepo@ejemplo.co | Recepción | Sin activar |
| Luz Marina Cardona | luz.cardona@ejemplo.co | Personal de servicio | Activa |
| Jorge Muñoz | jorge.munoz@ejemplo.co | Recepción | Eliminada |

## Pantallas

### 1. S4Usuarios: la lista (administrador)
Ruta `/usuarios`, grupo «Configuración», título «Usuarios» y botón «Nuevo usuario». Tabla de 10 en 10 con paginador: nombre, correo, rol, estado («Activa», «Sin activar», «Eliminada») y acciones «Editar» y «Eliminar». La fila del propio administrador dice «Tú» y no tiene «Eliminar». Las eliminadas en gris y sin acciones. En el celular, tarjetas.

Hoja **S4UsuariosEstados**: cargando; error con «Reintentar»; sin permiso (403) para quien no es administrador.

### 2. S4DialogoUsuario: crear y editar
- **Nuevo usuario**: nombre (hasta 120), correo, teléfono, tipo y número de documento, fecha de nacimiento y rol (tres opciones con una línea que explica qué hace cada una). Al crear: «Le enviamos a carlos.restrepo@ejemplo.co un código para activar su cuenta. Vence en 24 horas».
- **Editar**: los mismos campos menos el correo (se muestra como texto). Si cambia el rol, aviso antes de guardar: «María Fernanda tendrá que volver a entrar».

Estados: errores por campo (correo ya registrado, formato), enviando, rechazo general.

### 3. S4DialogoEliminarUsuario
«Carlos Restrepo ya no podrá entrar al PMS. Sus acciones anteriores siguen registradas a su nombre». Botones «Eliminar cuenta» y «Cancelar».

### 4. S4Perfil: mi perfil (cualquier rol)
Ruta `/perfil`, grupo «Mi cuenta». Dos bloques:
- **Mis datos**: nombre, teléfono, documento y fecha de nacimiento editables; correo y rol solo de lectura. «Guardar cambios», con el aviso «Tus datos quedaron guardados».
- **Contraseña**: actual, nueva y repetir la nueva, con la regla visible. Error si la actual no es correcta, junto a ese campo.

Hoja **S4PerfilEstados**: cargando, enviando, guardado, errores por campo.

### 5. S4ActivarCuenta y S4RecuperarContrasena (sin sesión)
Con la misma plantilla de la captura `ingresar.png` (iniciar sesión):
- **Activar mi cuenta** (`/activar`): correo, código de 6 dígitos, contraseña nueva y repetirla. Al terminar: «Tu cuenta quedó activa» con «Iniciar sesión». Errores: código errado o vencido («El código no es válido o ya venció. Pídele al administrador uno nuevo»), demasiados intentos.
- **Olvidé mi contraseña** (`/recuperar`), en dos pasos: correo → «Si el correo está registrado, te llegó un código de 6 dígitos. Vence en 15 minutos»; después código, contraseña nueva y repetirla. Al terminar vuelve a iniciar sesión con «Tu contraseña cambió».
- En «Iniciar sesión», un enlace «Olvidé mi contraseña».

---

## Para Claude Code (no va a Claude Design)
- Endpoints: `GET /api/usuarios?pagina=`, `POST /api/usuarios` (`CrearUsuarioRequest`), `PUT /api/usuarios/{id}` (`ModificarUsuarioRequest`), `PUT /api/usuarios/{id}/eliminar`; `GET` y `PUT /api/usuarios/perfil` (`DatosPersonalesRequest`), `PUT /api/usuarios/perfil/contrasena` (`CambiarContrasenaRequest`: `actual`, `nueva`); públicos: `POST /api/auth/activar` y `/api/auth/restablecer-contrasena` (`FijarContrasenaRequest`: `correo`, `codigo`, `contrasena`) y `POST /api/auth/recuperar-contrasena` (`correo`, responde 202).
- `UsuarioResponse` trae `activado` y `eliminado`. Si el backend no impide que el administrador se elimine a sí mismo, la pantalla igual no lo ofrece; avisa en el PR si hace falta la regla en el núcleo.
- Si se cambia el rol o se elimina la cuenta de quien tiene la sesión abierta, su siguiente petición da 401 y el PMS ya lo maneja (`src/sesion/almacen.ts`).
- Las rutas `/activar` y `/recuperar` van fuera de `ConSesion` en `src/App.tsx`, como `/ingresar`. El correo con el código, en local, sale en el log del backend (`NotificadorLocal`).
