# Punto de partida para Reyes: TRA y canales de venta (sprint 5)

Se pega en un chat nuevo de Claude Code, abierto en la raíz del clon. Antes: `CLAUDE.local.md` debe decir equipo **pms**, y el backend y el PMS deben arrancar en local (`pms/prompts/sprint4/LEEME.md`, «Antes de empezar»).

Copia desde aquí:

---

Soy Reyes, del equipo pms. Antes de nada: revisa que `CLAUDE.local.md` diga equipo pms y trae lo último de develop.

Lee `CLAUDE.md`, `docs/SPRINTS.md` (sprint 5, «Reparto final»), en `pms/DISENO.md` las secciones «Sprint 5: reparto final» y «Sprint 4: nuevo reparto», y `pms/prompts/sprint4/LEEME.md` (cómo se hace cada pantalla, DEC-84). Para el TRA lee además DEC-96 y DEC-98 en `docs/DECISIONES.md` y BL-07, BL-18 y BL-19 en `docs/BACKLOG.md`; para canales, DEC-62 y DEC-73 y los endpoints de `CanalController` y `ConflictoController` en `docs/api/openapi.yaml`.

Me tocan tres cosas, en este orden:

1. **Núcleo, primero y corto**: las pruebas del backend fallaron en GitHub (Actions, «Desplegar el backend») al integrar los PR #96 y #97, el 15 nov a las 03:29 y 03:51 UTC, y pasaron en local y en #98. Averigua si alguna prueba depende de la fecha en UTC o de la hora del día, y arréglala en una rama `feature/nucleo/…` con su PR. Mientras eso pase, un merge a develop por la noche no se despliega.

2. **Canales de venta** (`/canales`, solo administración, hoy «en construcción»): los canales registrados (registrar y desactivar), los conflictos pendientes con «resolver», y la bitácora de cada canal. El backend ya existe. Escribe el prompt para Claude Design como los de `pms/prompts/sprint4/`, con el diseño actual (el último `base.css`), y guárdalo en `pms/prompts/sprint5/canales.md`; lo reviso, lo lanzo, y con los mockups seguimos el flujo: revisión contra el contrato, código con sus pruebas, prueba en el navegador y auditoría.

3. **B5 TRA, el envío del reporte**: los datos de cada huésped ya se piden en la llegada y la salida se bloquea sin ellos; falta enviar el reporte. Todavía no programes: primero una lluvia de ideas sobre qué es «enviar» para el MVP1 (qué exige la plataforma del Ministerio, qué se puede hacer sin su API, qué ve recepción, qué se guarda de cada envío), qué hacer con SIRE para extranjeros y los adjuntos (BL-18, BL-19) si alcanza, y qué preguntarle al cliente en el cierre 4 (vie 20 nov). Lo decidido queda como un `DEC-nn` y una sección en `pms/DISENO.md`; después, el núcleo y la pantalla.

Reglas: no cambies el diseño visual (Daniel está rediseñando en paralelo y le aplicará el rediseño a mis pantallas al integrarse): usa las clases y variables que ya existen. Edito solo lo del equipo pms; lo del núcleo, en ramas `feature/nucleo/…`. Los commits, merges y PR los hago yo.
