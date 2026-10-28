# Backlog del producto

Todo lo que el cliente pide y todavía no tiene sprint: los requisitos de la entrevista, lo que sale de cada cierre y lo que quedó para el MVP2. Se ordena con el cliente en cada cierre (`docs/SPRINTS.md`). Para que un ítem entre a un sprint necesita producto (web o pms), una prioridad y cómo se sabrá que funcionó.

| Id | Requisito | Origen | Producto | Prioridad | Cómo sabremos que funcionó | Sprint |
|---|---|---|---|---|---|---|
| BL-01 | Pasarela de pago real (Wompi: tarjeta, PSE, Nequi) | DEC-74 | web | MVP2 | — | — |
| BL-02 | Facturación electrónica DIAN (candidato Factus) | DEC-74 | pms | MVP2 | — | — |
| BL-03 | Proveedor de SMS | DEC-74 | núcleo | Por definir | — | — |
| BL-04 | Varios alojamientos en el backend: un despliegue por hotel o un solo backend para todos (y si `web/` pasa a su propio repositorio) | DEC-83 | núcleo | MVP2 (con el segundo cliente) | Dos alojamientos operan a la vez sin ver los datos del otro | — |
| BL-05 | Marca de cada alojamiento en la web: logo y colores como configuración que llega por la API | DEC-83 | web (y núcleo) | Por definir | Se cambian el logo y los colores de un hotel sin desplegar código | — |
| BL-06 | Publicar los frontends (Cloudflare Pages, un proyecto por frontend y alojamiento) y el backend | DEC-83 | núcleo | Por definir | `web/` y `pms/` abren desde su dominio contra la API publicada | — |

<!-- Requisitos de la entrevista: agregarlos aquí como BL-04, BL-05… con origen «Entrevista». -->
