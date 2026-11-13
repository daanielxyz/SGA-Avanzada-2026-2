import { useCallback, useId, type CSSProperties } from 'react'
import { guardarMarca, type Alojamiento, type NombreColor } from '../api/alojamiento'
import { Bloque, CampoBloque } from './Bloque'
import { useBloque, type AlBloque } from './useBloque'
import { COLOR_VALIDO, CONTRASTE_MINIMO, PARES, contraste, textoContraste } from './contraste'
import { cuerpoMarca, formMarca, validarMarca, type CampoMarca, type FormMarca } from './formularios'

const COLORES: readonly [NombreColor, string][] = [
  ['fondo', 'Fondo'], ['superficie', 'Superficie'], ['texto', 'Texto'], ['textoSuave', 'Texto suave'],
  ['borde', 'Borde'], ['bordeCampo', 'Borde de campo'], ['acento', 'Acento'], ['textoSobreAcento', 'Texto sobre acento'],
  ['peligro', 'Peligro'], ['exito', 'Éxito'],
]

/** El campo de la API de un detalle de un 400: «colores.fondo», «fuentes.texto», «radio.pequeno». */
function campoDe(campoApi: string): CampoMarca | null {
  const [grupo, nombre] = campoApi.split('.')
  if (grupo === 'colores' && nombre) return nombre as CampoMarca
  if (grupo === 'fuentes' && nombre) return nombre === 'titulos' ? 'fuenteTitulos' : 'fuenteTexto'
  if (grupo === 'radio' && nombre) return `radio${nombre.charAt(0).toUpperCase()}${nombre.slice(1)}` as CampoMarca
  return grupo === 'logoUrl' || grupo === 'portadaUrl' ? grupo : null
}

function ColorMarca({ nombre, etiqueta, valor, error, alCambiar }: {
  nombre: NombreColor
  etiqueta: string
  valor: string
  error?: string
  alCambiar: (valor: string) => void
}) {
  const id = useId()
  const valido = COLOR_VALIDO.test(valor)
  return (
    <div className="color-marca">
      <label className="color-marca-rotulo" htmlFor={id}>{etiqueta}</label>
      <input type="color" className="color-marca-muestra" aria-label={`Elegir ${etiqueta.toLowerCase()}`}
        value={valido ? valor.toLowerCase() : '#000000'} onChange={(evento) => alCambiar(evento.target.value.toUpperCase())} />
      <input id={id} className="campo-control color-marca-codigo" value={valor} maxLength={7} spellCheck={false}
        aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} data-color={nombre}
        onChange={(evento) => alCambiar(evento.target.value.trim())} />
      {error && <p className="campo-mensaje color-marca-error" id={`${id}-error`}>{error}</p>}
    </div>
  )
}

/** Cada par de texto y fondo con su contraste medido: la API no lo revisa al guardar. */
function Contrastes({ form }: { form: FormMarca }) {
  return (
    <div className="contrastes">
      <h3 className="grupo-reglas-subtitulo">Contraste medido</h3>
      <ul className="contrastes-lista">
        {PARES.map((par) => {
          const valor = contraste(form[par.texto], form[par.fondo])
          const bien = valor !== null && valor >= CONTRASTE_MINIMO
          return (
            <li key={par.nombre}>
              <span className="contraste-muestra" aria-hidden="true"
                style={valor === null ? undefined : { color: form[par.texto], background: form[par.fondo] }}>Aa</span>
              <span className="contraste-nombre">{par.nombre}</span>
              <span className={bien ? 'contraste-valor contraste-bien' : 'contraste-valor contraste-mal'}>
                <span className="contraste-sello" aria-hidden="true">{bien ? '✓' : '!'}</span>
                {valor === null ? 'Falta un color válido' : <><strong>{textoContraste(valor)}</strong> · {bien ? 'se lee bien' : 'difícil de leer'}</>}
              </span>
            </li>
          )
        })}
      </ul>
      <p className="campo-pista">Se mide aquí porque el sistema no lo revisa al guardar. Por debajo de 4,5:1 cuesta leer.</p>
    </div>
  )
}

/** Unos pocos componentes con los valores escritos. No es la web de reservas, que es del otro equipo. */
function VistaPrevia({ form, nombre }: { form: FormMarca; nombre: string }) {
  const color = (clave: NombreColor) => (COLOR_VALIDO.test(form[clave]) ? form[clave] : undefined)
  const radio = (valor: string) => (/^\d{1,2}$/.test(valor) ? `${valor}px` : undefined)
  const estilo = {
    '--vp-fondo': color('fondo'),
    '--vp-superficie': color('superficie'),
    '--vp-texto': color('texto'),
    '--vp-texto-suave': color('textoSuave'),
    '--vp-borde': color('borde'),
    '--vp-borde-campo': color('bordeCampo'),
    '--vp-acento': color('acento'),
    '--vp-sobre-acento': color('textoSobreAcento'),
    '--vp-radio-chico': radio(form.radioPequeno),
    '--vp-radio-medio': radio(form.radioMediano),
    '--vp-radio-grande': radio(form.radioGrande),
    '--vp-letra-titulo': `'${form.fuenteTitulos.replaceAll("'", '')}', serif`,
    '--vp-letra-texto': `'${form.fuenteTexto.replaceAll("'", '')}', sans-serif`,
  } as CSSProperties
  return (
    <aside className="tarjeta vista-previa" aria-label="Vista previa de la marca">
      <h3 className="grupo-reglas-titulo">Vista previa</h3>
      <div className="vista-previa-lienzo" style={estilo} aria-hidden="true">
        <p className="vp-titulo">{nombre}</p>
        <p className="vp-parrafo">Párrafo de ejemplo con el color de texto sobre el fondo.</p>
        <div className="vp-tarjeta">
          <p className="vp-subtitulo">Tarjeta</p>
          <p className="vp-suave">Texto suave sobre la superficie.</p>
          <span className="vp-campo">Campo</span>
          <span className="vp-boton">Botón principal</span>
        </div>
      </div>
      <p className="campo-pista">Solo muestra los valores escritos. No es la web de reservas. Si la fuente no está en este equipo, se ve otra parecida.</p>
    </aside>
  )
}

/** Logo, portada, colores, fuentes y esquinas de la web de reservas (BL-05 · DEC-87). Un solo «Guardar marca». */
export default function BloqueMarca(props: AlBloque) {
  const desde = useCallback((alojamiento: Alojamiento) => formMarca(alojamiento.marca), [])
  const { form, cambiar, errores, estado, rechazo, sucio, guardar } = useBloque(props, { desde, validar: validarMarca, campoDe })

  return (
    <div className="marca-alojamiento">
      <Bloque
        titulo="Marca"
        descripcion="Lo que usa la web de reservas para verse como el hotel."
        estado={estado}
        sucio={sucio}
        rechazo={rechazo}
        guardado="Marca guardada"
        boton="Guardar marca"
        alEnviar={(evento) => guardar(evento, 'No se guardó la marca', (f) => guardarMarca(cuerpoMarca(f)))}
      >
        <section className="grupo-reglas" aria-label="Imágenes">
          <h3 className="grupo-reglas-titulo">Imágenes</h3>
          <CampoBloque etiqueta="Logo" nota="opcional" type="url" valor={form.logoUrl} error={errores.logoUrl} placeholder="https://"
            pista="Dirección de una imagen ya publicada en internet, que empiece por https://"
            alCambiar={(valor) => cambiar('logoUrl', valor)} />
          <CampoBloque etiqueta="Foto de portada" nota="opcional" type="url" valor={form.portadaUrl} error={errores.portadaUrl}
            placeholder="https://" pista="Dirección de una imagen ya publicada en internet, que empiece por https://"
            alCambiar={(valor) => cambiar('portadaUrl', valor)} />
        </section>

        <section className="grupo-reglas" aria-label="Colores">
          <h3 className="grupo-reglas-titulo">Colores</h3>
          <div className="colores-marca">
            {COLORES.map(([nombre, etiqueta]) => (
              <ColorMarca key={nombre} nombre={nombre} etiqueta={etiqueta} valor={form[nombre]} error={errores[nombre]}
                alCambiar={(valor) => cambiar(nombre, valor)} />
            ))}
          </div>
          <Contrastes form={form} />
        </section>

        <section className="grupo-reglas" aria-label="Fuentes">
          <h3 className="grupo-reglas-titulo">Fuentes</h3>
          <div className="campos-bloque">
            <CampoBloque etiqueta="Títulos" valor={form.fuenteTitulos} error={errores.fuenteTitulos} maxLength={60}
              pista="El nombre de la fuente, como Lora." alCambiar={(valor) => cambiar('fuenteTitulos', valor)} />
            <CampoBloque etiqueta="Texto" valor={form.fuenteTexto} error={errores.fuenteTexto} maxLength={60}
              pista="El nombre de la fuente, como Inter." alCambiar={(valor) => cambiar('fuenteTexto', valor)} />
          </div>
        </section>

        <section className="grupo-reglas" aria-label="Esquinas">
          <h3 className="grupo-reglas-titulo">Esquinas</h3>
          <div className="campos-bloque campos-tres">
            {([['radioPequeno', 'Pequeño'], ['radioMediano', 'Mediano'], ['radioGrande', 'Grande']] as const).map(([campo, etiqueta]) => (
              <CampoBloque key={campo} etiqueta={etiqueta} valor={form[campo]} error={errores[campo]} despues="px"
                inputMode="numeric" maxLength={2} alCambiar={(valor) => cambiar(campo, valor)} />
            ))}
          </div>
          <p className="campo-pista">Qué tan redondeadas se ven las esquinas de campos, botones y tarjetas.</p>
        </section>
      </Bloque>
      <VistaPrevia form={form} nombre={props.alojamiento.nombre} />
    </div>
  )
}
