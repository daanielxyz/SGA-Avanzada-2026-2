// Los diálogos del detalle de un apartamento, uno por acción (pms/DISENO.md, «Sprint 4: apartamentos»).
import { useId, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import {
  cambiarCapacidad,
  cambiarImagen,
  cambiarVenta,
  fijarLimiteMascotas,
  levantarBloqueo,
  registrarBloqueo,
  type Bloqueo,
  type FichaApartamento,
} from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { fechaCorta, fechaHoy } from '../formato'
import Campo from '../reserva/nueva/Campo'
import { errorParaMostrar } from '../reserva/mensajes'
import DialogoFormulario, { CampoConContador, type Rechazo } from './DialogoFormulario'
import {
  enfocarPrimerError,
  esDireccionDeImagen,
  MAXIMO_FOTOS,
  MAXIMO_MOTIVO_BLOQUEO,
  MAXIMO_URL,
  mensajeDelDetalle,
  validarBloqueo,
  type Errores,
  type FormBloqueo,
} from './formulario'
import CalendarioBloqueo from './CalendarioBloqueo'
import { Foto } from './Piezas'

/** Lo que el detalle muestra al terminar una acción. */
export interface Hecho {
  apartamento: FichaApartamento
  asunto: string
  mensaje: string
}

interface Comunes {
  apartamento: FichaApartamento
  alCerrar: () => void
  alHacer: (hecho: Hecho) => void
}

/** El rechazo de la API como se muestra: los detalles de un 400 sin la ruta del campo. */
function rechazoDe(causa: unknown, asunto: string): Rechazo {
  const { mensaje, detalles } = errorParaMostrar(causa)
  return { asunto, mensaje, detalles: detalles.map(mensajeDelDetalle) }
}

/** Envía, y si la API lo rechaza, deja el rechazo en el diálogo. */
function useEnvio() {
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)
  async function intentar(asunto: string, accion: () => Promise<void>) {
    setEnviando(true)
    setRechazo(null)
    try {
      await accion()
    } catch (causa) {
      setRechazo(rechazoDe(causa, asunto))
    } finally {
      setEnviando(false)
    }
  }
  return { enviando, rechazo, setRechazo, intentar }
}

const subtituloDe = (apartamento: FichaApartamento) => (
  <><span className="codigo">{apartamento.codigo}</span> · {apartamento.nombre}</>
)

/**
 * Capacidad (CAP-01). La API no la rechaza por las reservas que ya no caben: las devuelve, y el diálogo las lista con
 * su enlace para revisarlas con recepción.
 */
export function DialogoCapacidad({ apartamento, alCerrar, alHacer }: Comunes) {
  const [valor, setValor] = useState(String(apartamento.capacidad))
  const [error, setError] = useState<string | null>(null)
  const [afectadas, setAfectadas] = useState<{ ficha: FichaApartamento; codigos: string[] } | null>(null)
  const { enviando, rechazo, intentar } = useEnvio()

  if (afectadas) {
    const cerrar = () => alHacer({ apartamento: afectadas.ficha, asunto: 'Capacidad cambiada',
      mensaje: `Quedó en ${afectadas.ficha.capacidad} personas. Revisa con recepción las reservas que tienen más ocupantes.` })
    return (
      <DialogoFormulario titulo="Capacidad cambiada" subtitulo={subtituloDe(apartamento)} enviando={false}
        rechazo={null} textoBoton="Cerrar" textoEnviando="" alCerrar={cerrar}
        alEnviar={(e) => { e.preventDefault(); cerrar() }}>
        <div className="aviso aviso-advertencia" role="status">
          <span className="aviso-sello" aria-hidden="true">!</span>
          <div className="aviso-cuerpo">
            <strong>Quedó en {afectadas.ficha.capacidad} personas</strong>
            <p>Estas reservas tienen más ocupantes. Revísalas con recepción:</p>
            <ul className="reservas-afectadas">
              {afectadas.codigos.map((codigo) => (
                <li key={codigo}>
                  <Link to={`/reservas/${encodeURIComponent(codigo)}`} className="codigo">{codigo}</Link>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </DialogoFormulario>
    )
  }

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const numero = /^\d+$/.test(valor.trim()) ? Number(valor) : 0
    if (numero < 1) {
      setError('Escribe cuántas personas caben, desde 1.')
      enfocarPrimerError(evento.currentTarget)
      return
    }
    void intentar('No se cambió la capacidad', async () => {
      const { apartamento: ficha, reservasAfectadas } = await cambiarCapacidad(apartamento.codigo, numero)
      if (reservasAfectadas.length > 0) setAfectadas({ ficha, codigos: reservasAfectadas })
      else alHacer({ apartamento: ficha, asunto: 'Capacidad cambiada', mensaje: `Quedó en ${ficha.capacidad} personas.` })
    })
  }

  return (
    <DialogoFormulario titulo="Cambiar capacidad" subtitulo={subtituloDe(apartamento)} enviando={enviando}
      rechazo={rechazo} textoBoton="Guardar" textoEnviando="Guardando…" alCerrar={alCerrar} alEnviar={enviar}>
      <Campo etiqueta="Capacidad (personas)" type="number" min={1} inputMode="numeric" value={valor}
        data-foco-inicial disabled={enviando} error={error ?? undefined}
        pista="Cuenta a todos, también a los niños. Si hay reservas con más ocupantes, te las mostramos."
        onChange={(e) => { setValor(e.target.value); setError(null) }} />
    </DialogoFormulario>
  )
}

/** Agregar una foto por su dirección, con una vista previa (IMG-01). */
export function DialogoFoto({ apartamento, alCerrar, alHacer }: Comunes) {
  const id = useId()
  const [url, setUrl] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [noCarga, setNoCarga] = useState<string | null>(null)
  const { enviando, rechazo, setRechazo, intentar } = useEnvio()
  const limpia = url.trim()
  const lleno = apartamento.imagenes.length >= MAXIMO_FOTOS

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const motivo = !limpia ? 'Pega la dirección de la imagen.'
      : !esDireccionDeImagen(limpia) ? 'La dirección debe empezar por https:// y no llevar espacios.'
        : apartamento.imagenes.some((imagen) => imagen.url === limpia) ? 'Esa foto ya está en el apartamento.'
          : null
    setError(motivo)
    if (motivo) {
      enfocarPrimerError(evento.currentTarget)
      return
    }
    void intentar('No se agregó la foto', async () => {
      const ficha = await cambiarImagen(apartamento.codigo, 'agregar', limpia)
      alHacer({ apartamento: ficha, asunto: 'Foto agregada', mensaje: `Ya son ${ficha.imagenes.length} de ${MAXIMO_FOTOS}.` })
    })
  }

  const descritoPor = error ? `${id}-error` : `${id}-pista`
  return (
    <DialogoFormulario titulo="Agregar foto" subtitulo={<>{subtituloDe(apartamento)} · {apartamento.imagenes.length} de {MAXIMO_FOTOS}</>}
      enviando={enviando} rechazo={rechazo ?? (lleno ? {
        asunto: 'No se puede agregar', mensaje: `Un apartamento puede tener hasta ${MAXIMO_FOTOS} fotos. Quita una antes.`, detalles: [],
      } : null)}
      bloqueado={lleno} textoBoton="Agregar foto" textoEnviando="Agregando…" alCerrar={alCerrar} alEnviar={enviar}>
      <div className="campo">
        <label className="campo-rotulo" htmlFor={id}>Dirección de la imagen</label>
        <input id={id} className="campo-control" type="url" inputMode="url" placeholder="https://…" autoComplete="off"
          maxLength={MAXIMO_URL} value={url} disabled={enviando || lleno} data-foco-inicial
          aria-invalid={Boolean(error)} aria-describedby={descritoPor}
          onChange={(e) => { setUrl(e.target.value); setError(null); setRechazo(null) }} />
        {error
          ? <p className="campo-mensaje" id={`${id}-error`}>{error}</p>
          : <p className="campo-pista" id={`${id}-pista`}>Pega el enlace de una foto ya publicada (https://…).</p>}
      </div>
      <div className="vista-previa">
        {esDireccionDeImagen(limpia) && noCarga !== limpia
          ? <img src={limpia} alt="Vista previa" onError={() => setNoCarga(limpia)} />
          : <Foto clase="vista-previa-vacia" />}
        <p className="campo-pista" role="status">
          {esDireccionDeImagen(limpia) && noCarga === limpia
            ? 'No se pudo ver la imagen: revisa el enlace.'
            : esDireccionDeImagen(limpia) ? 'Vista previa' : 'Aquí verás la foto al pegar su dirección.'}
        </p>
      </div>
    </DialogoFormulario>
  )
}

/** Cuántas mascotas admite (RP-03): desde 0, que es «no admite». */
export function DialogoMascotas({ apartamento, limiteDelHotel, alCerrar, alHacer }: Comunes & {
  limiteDelHotel: number | null
}) {
  const [valor, setValor] = useState(String(apartamento.limiteMascotas ?? limiteDelHotel ?? ''))
  const [error, setError] = useState<string | null>(null)
  const { enviando, rechazo, intentar } = useEnvio()

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    if (!/^\d+$/.test(valor.trim())) {
      setError('Escribe un número desde 0.')
      enfocarPrimerError(evento.currentTarget)
      return
    }
    void intentar('No se cambió el límite de mascotas', async () => {
      const ficha = await fijarLimiteMascotas(apartamento.codigo, Number(valor))
      alHacer({ apartamento: ficha, asunto: 'Límite de mascotas cambiado',
        mensaje: ficha.limiteMascotas === 0 ? 'Este apartamento no admite mascotas.' : `Admite hasta ${ficha.limiteMascotas}.` })
    })
  }

  return (
    <DialogoFormulario titulo="Mascotas" subtitulo={subtituloDe(apartamento)} enviando={enviando} rechazo={rechazo}
      textoBoton="Guardar" textoEnviando="Guardando…" alCerrar={alCerrar} alEnviar={enviar}>
      <Campo etiqueta="¿Cuántas mascotas admite este apartamento?" type="number" min={0} inputMode="numeric"
        value={valor} data-foco-inicial disabled={enviando} error={error ?? undefined}
        pista="Número desde 0 (0: no admite)."
        onChange={(e) => { setValor(e.target.value); setError(null) }} />
    </DialogoFormulario>
  )
}

/** Un bloqueo: esas noches no se venden en ningún canal. «Hasta» es el día en que se vuelve a vender. */
export function DialogoBloqueo({ apartamento, alCerrar, alHacer }: Comunes) {
  const id = useId()
  const [form, setForm] = useState<FormBloqueo>({ inicio: '', fin: '', motivo: '' })
  const [errores, setErrores] = useState<Errores<keyof FormBloqueo>>({})
  const [errorFechas, setErrorFechas] = useState<string | null>(null)
  const { enviando, rechazo, setRechazo, intentar } = useEnvio()

  const cambiar = (campo: keyof FormBloqueo, valor: string) => {
    setForm((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
    if (campo !== 'motivo') setErrorFechas(null)
  }

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const encontrados = validarBloqueo(form, fechaHoy())
    setErrores(encontrados)
    setErrorFechas(null)
    if (Object.values(encontrados).some(Boolean)) {
      enfocarPrimerError(formulario)
      return
    }
    void intentar('No se creó el bloqueo', async () => {
      try {
        const ficha = await registrarBloqueo(apartamento.codigo, { ...form, motivo: form.motivo.trim() })
        alHacer({ apartamento: ficha, asunto: 'Bloqueo creado',
          mensaje: `Del ${fechaCorta(form.inicio)} al ${fechaCorta(form.fin)} no se vende en ningún canal.` })
      } catch (causa) {
        // Un 409 de un bloqueo es por sus fechas: el mensaje va junto a ellas
        if (causa instanceof ErrorApi && causa.estado === 409) {
          setErrorFechas(causa.message)
          setRechazo(null)
          enfocarPrimerError(formulario)
          return
        }
        throw causa
      }
    })
  }

  return (
    <DialogoFormulario titulo="Nuevo bloqueo" subtitulo={subtituloDe(apartamento)} enviando={enviando}
      rechazo={rechazo} textoBoton="Crear bloqueo" textoEnviando="Creando…" alCerrar={alCerrar} alEnviar={enviar}>
      <fieldset className="bloqueo-fechas" disabled={enviando}>
        <legend className="campo-rotulo">Noches que no se venden</legend>
        <CalendarioBloqueo codigo={apartamento.codigo} bloqueos={apartamento.bloqueos}
          rango={{ inicio: form.inicio, fin: form.fin }}
          invalido={Boolean(errores.inicio || errores.fin || errorFechas)}
          alCambiar={({ inicio, fin }) => {
            setForm((actual) => ({ ...actual, inicio, fin }))
            setErrores((actuales) => ({ ...actuales, inicio: undefined, fin: undefined }))
            setErrorFechas(null)
          }} />
        {errores.inicio || errores.fin || errorFechas
          ? <p className="campo-mensaje" role="alert">{errores.inicio ?? errores.fin ?? errorFechas}</p>
          : <p className="campo-pista">Esas noches no se venden en ningún canal.</p>}
      </fieldset>
      <CampoConContador id={`${id}-motivo`} rotulo="Motivo" valor={form.motivo} maximo={MAXIMO_MOTIVO_BLOQUEO}
        varias deshabilitado={enviando} error={errores.motivo} pista="Por ejemplo, Mantenimiento de la cubierta."
        alCambiar={(v) => cambiar('motivo', v)} />
    </DialogoFormulario>
  )
}

/** Levantar un bloqueo: sus noches vuelven a venderse. */
export function DialogoLevantar({ apartamento, bloqueo, alCerrar, alHacer }: Comunes & { bloqueo: Bloqueo }) {
  const { enviando, rechazo, intentar } = useEnvio()
  return (
    <DialogoFormulario titulo="Levantar bloqueo" subtitulo={subtituloDe(apartamento)} enviando={enviando}
      rechazo={rechazo} textoBoton="Levantar bloqueo" textoEnviando="Levantando…" alCerrar={alCerrar}
      alEnviar={(e) => {
        e.preventDefault()
        void intentar('No se levantó el bloqueo', async () => {
          const ficha = await levantarBloqueo(apartamento.codigo, bloqueo.id)
          alHacer({ apartamento: ficha, asunto: 'Bloqueo levantado', mensaje: 'Esas noches vuelven a estar a la venta.' })
        })
      }}>
      <dl className="resumen-dialogo">
        <dt>Fechas</dt>
        <dd>{fechaCorta(bloqueo.fechaInicio)} → {fechaCorta(bloqueo.fechaFin)}</dd>
        <dt>Motivo</dt>
        <dd className="texto-largo">{bloqueo.motivo}</dd>
      </dl>
      <p className="dialogo-texto">Esas noches vuelven a quedar disponibles para la venta.</p>
    </DialogoFormulario>
  )
}

/**
 * Poner a la venta o retirar. Si la API rechaza ponerlo a la venta, se ofrece ir a temporadas y tarifas, que es lo que
 * suele faltar; el mensaje es el de la API.
 */
export function DialogoVenta({ apartamento, alCerrar, alHacer }: Comunes) {
  const { enviando, rechazo, intentar } = useEnvio()
  const retirar = apartamento.activo
  return (
    <DialogoFormulario
      titulo={retirar ? 'Retirar de la venta' : 'Poner a la venta'}
      subtitulo={subtituloDe(apartamento)}
      enviando={enviando}
      rechazo={rechazo}
      bloqueado={Boolean(rechazo)}
      pie={rechazo && !retirar ? <Link to="/tarifas" className="boton boton-secundario boton-chico">Ir a temporadas y tarifas</Link> : undefined}
      textoBoton={retirar ? 'Retirar de la venta' : 'Poner a la venta'}
      textoEnviando={retirar ? 'Retirando…' : 'Publicando…'}
      alCerrar={alCerrar}
      alEnviar={(e) => {
        e.preventDefault()
        void intentar(retirar ? 'No se retiró' : 'No se puso a la venta', async () => {
          const ficha = await cambiarVenta(apartamento.codigo, retirar ? 'retirar' : 'activar')
          alHacer(retirar
            ? { apartamento: ficha, asunto: 'Retirado de la venta', mensaje: `${ficha.codigo} ya no aparece en la web.` }
            : { apartamento: ficha, asunto: 'A la venta', mensaje: `${ficha.codigo} ya aparece en la web y se puede reservar.` })
        })
      }}
    >
      <p className="dialogo-texto">
        {retirar
          ? <><span className="codigo">{apartamento.codigo}</span> deja de aparecer en la web y no se podrá reservar. Las reservas que ya tiene no cambian.</>
          : <><span className="codigo">{apartamento.codigo}</span> vuelve a aparecer en la web y se podrá reservar.</>}
      </p>
    </DialogoFormulario>
  )
}
