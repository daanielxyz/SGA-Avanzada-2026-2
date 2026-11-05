# Diseño visual de la web de reservas

La web es un solo esqueleto para todos los hoteles (DEC-83). Lo lee el equipo web y Claude antes de tocar el aspecto de `web/`. Un hotel que quiera un diseño propio contrata un sitio aparte, que usa la API del portal.

## Las dos capas
| Capa | Qué tiene | Dónde vive |
|---|---|---|
| **Esqueleto** (del producto) | Estructura de las pantallas, escala de tipografía, espacios, sombras y componentes | `src/index.css`, igual para todos |
| **Marca** (de cada hotel) | Colores, fuentes, radios, logo y foto de portada | Llega por la API en `marca` y `src/marca.ts` reemplaza las variables de `:root`. Sin marca, se ve con los valores neutros |

## Reglas
- Nada del hotel en el código: ni nombre, ni colores, ni logo, ni fotos.
- Fuera de `:root`, ningún color suelto: todo sale de una variable con nombre de papel (`--acento`, `--texto-suave`). El velo oscuro de la portada es la excepción, porque va sobre una foto, no sobre la marca.
- El tono es cálido y sencillo, y la protagonista es la foto: sirve para la mayoría de hoteles y apartamentos turísticos.
- Una sola acción principal por pantalla; las demás se ven como enlace (`.secundario`).
- Las pruebas buscan por rol y por texto, nunca por estilos, salvo las de las fotos y la portada.
- Campos de 16 px y controles de 44 px como mínimo. El hover solo con `(hover: hover) and (pointer: fine)`.

## Qué necesita un hotel para tener su marca
Los valores de `marca` en `GET /api/alojamiento`, que se cambian con `PUT /api/alojamiento/marca` sin desplegar código (BL-05 · DEC-87). Todos son opcionales: lo que falte queda neutro.

Antes de usar una paleta **se mide el contraste**: el texto necesita 4,5:1 y los bordes de campos y botones 3:1. Los valores de un brandbook no se dan por buenos.

## Marca de ejemplo: Puerta al Sol
Hotel inventado por el equipo, para las demos. El brandbook está en `diseno/brandbook-ejemplo.png`. Sus contrastes estaban mal calculados; medidos con la fórmula WCAG:

| Combinación | El brandbook dice | Medido | Decisión |
|---|---|---|---|
| Texto `#1B3A24` sobre fondo `#F8F7F1` | 15,1 | 11,67 | Se usa |
| Blanco sobre botón `#F4B942` | 5,2 | **1,77** | Texto del botón `#1B3A24` (7,07), como en su propia tarjeta de ejemplo |
| Texto suave `#6B8F6B` sobre fondo | 6,3 | **3,39** | Se oscurece a `#55705A` (5,08) |
| Error `#E63946` sobre fondo | 4,6 | **3,88** | Se oscurece a `#B3261E` (6,09) |
| Éxito `#22C55E` sobre fondo | 5,8 | **2,12** | Se oscurece a `#1E7A3F` (5,00) |
| Borde `#D9C9A3` sobre fondo | 7,3 | 1,52 | Solo para separadores; los campos usan `#8A8066` (3,92) |

Valores finales de la marca de ejemplo:

```json
{
  "colores": {
    "fondo": "#F8F7F1", "superficie": "#FFFFFF", "texto": "#1B3A24", "textoSuave": "#55705A", "borde": "#D9C9A3",
    "bordeCampo": "#8A8066", "acento": "#F4B942", "textoSobreAcento": "#1B3A24", "peligro": "#B3261E", "exito": "#1E7A3F"
  },
  "fuentes": { "titulos": "Playfair Display", "texto": "Inter" },
  "radio": { "pequeno": "6px", "mediano": "12px", "grande": "20px" }
}
```

## Skills
- `impeccable` da la dirección: `critique`, `shape`, `audit`, `harden`, `adapt`. Se usa sin su programa (la skill lo prevé), así que no corre su detector automático.
- `emil-design-eng` para el pulido fino, `break-ui` para datos extremos y `mobile-native` para el celular.
- Nada de una segunda skill de dirección visual: se contradicen.

## Pendiente
- La pantalla de marca en el PMS (`PUT /api/alojamiento/marca` ya existe, DEC-87), y el logo y la portada definitivos de la marca de ejemplo.
- Calendario de noches libres en la página del apartamento: `GET /api/apartamentos/{codigo}/calendario` ya existe (DEC-86).
- Una barra fija con el total y «Reservar» en el celular, en la página del apartamento.
- Quitar los botones de pago de prueba cuando llegue la pasarela real (BL-01).
