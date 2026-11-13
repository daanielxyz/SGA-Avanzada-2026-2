import { useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import {
  actualizarApartamento,
  consultarFicha,
  crearApartamento,
  type FichaApartamento,
} from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import Icono from '../componentes/Icono'
import Campo from '../reserva/nueva/Campo'
import { errorParaMostrar } from '../reserva/mensajes'
import DialogoFormulario, { CampoConContador, type Rechazo } from './DialogoFormulario'
import {
  agregarCaracteristica,
  agregarFoto,
  campoDelDetalle,
  cuerpoDatos,
  cuerpoNuevo,
  enfocarPrimerError,
  FORM_VACIO,
  formDe,
  MAXIMO_DESCRIPCION,
  MAXIMO_FOTOS,
  MAXIMO_NOMBRE,
  MAXIMO_URL,
  mensajeDelDetalle,
  quitarFoto,
  validarApartamento,
  type CampoApartamento,
  type Errores,
  type FormApartamento,
} from './formulario'

/**
 * «Nuevo apartamento» (sin `apartamento`) o «Editar datos». Al crear pide código, capacidad y fotos, que después se
 * cambian aparte; al editar, solo nombre, descripción, dormitorios y características.
 */
export default function DialogoDatos({ apartamento, alCerrar, alGuardar }: {
  apartamento?: FichaApartamento
  alCerrar: () => void
  alGuardar: (apartamento: FichaApartamento) => void
}) {
  const nuevo = !apartamento
  const id = useId()
  const [form, setForm] = useState<FormApartamento>(() => (apartamento
    ? formDe(apartamento)
    : { ...FORM_VACIO, imagenes: agregarFoto([]) }))
  const [caracteristica, setCaracteristica] = useState('')
  const [errorCaracteristica, setErrorCaracteristica] = useState<string | null>(null)
  const [errores, setErrores] = useState<Errores<CampoApartamento>>({})
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)
  const [enviando, setEnviando] = useState(false)
  const asunto = nuevo ? 'No se creó el apartamento' : 'No se guardaron los cambios'

  const numeroActual = useRef(form.numero)

  function cambiar<C extends CampoApartamento>(campo: C, valor: FormApartamento[C]) {
    if (campo === 'numero') numeroActual.current = valor as string
    setForm((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  function sumarCaracteristica() {
    const resultado = agregarCaracteristica(form.caracteristicas, caracteristica)
    if (typeof resultado === 'string') {
      setErrorCaracteristica(resultado)
      return
    }
    cambiar('caracteristicas', resultado)
    setCaracteristica('')
    setErrorCaracteristica(null)
  }

  // En el campo de característica, Enter la agrega en vez de enviar todo el formulario
  function alTeclear(evento: KeyboardEvent<HTMLInputElement>) {
    if (evento.key === 'Enter') {
      evento.preventDefault()
      sumarCaracteristica()
    }
  }

  // Al salir del código se revisa si ya existe, sin esperar a «Crear»: si existe, la API responde 200; si no, 404.
  // Cualquier otra falla se ignora aquí: al crear, la API lo vuelve a revisar.
  function revisarCodigo() {
    const numero = form.numero.trim()
    if (!nuevo || !/^\d+$/.test(numero)) return
    consultarFicha(`APT-${numero}`)
      .then(() => {
        // Si mientras respondía se escribió otro código, este aviso ya no aplica
        if (numeroActual.current.trim() !== numero) return
        setErrores((actuales) => ({ ...actuales, numero: `Ya hay un apartamento con el código APT-${numero}.` }))
      })
      .catch(() => undefined)
  }

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const encontrados = validarApartamento(form, nuevo)
    setErrores(encontrados)
    setRechazo(null)
    if (Object.values(encontrados).some(Boolean)) {
      enfocarPrimerError(formulario)
      return
    }
    setEnviando(true)
    try {
      alGuardar(apartamento
        ? await actualizarApartamento(apartamento.codigo, cuerpoDatos(form))
        : await crearApartamento(cuerpoNuevo(form)))
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.codigo === 'DUPLICADO') {
        setErrores({ numero: `Ya hay un apartamento con el código APT-${form.numero.trim()}.` })
        enfocarPrimerError(formulario)
      } else if (causa instanceof ErrorApi && causa.estado === 400) {
        const porCampo: Errores<CampoApartamento> = {}
        const otros: string[] = []
        for (const detalle of causa.detalles) {
          const campo = campoDelDetalle(detalle)
          if (campo) porCampo[campo] = mensajeDelDetalle(detalle)
          else otros.push(mensajeDelDetalle(detalle))
        }
        setErrores(porCampo)
        if (otros.length > 0) setRechazo({ asunto, mensaje: 'Revisa estos datos:', detalles: otros })
        enfocarPrimerError(formulario)
      } else {
        setRechazo({ asunto, ...errorParaMostrar(causa) })
      }
    }
  }

  const fotosConError = Boolean(errores.imagenes)
  return (
    <DialogoFormulario
      titulo={nuevo ? 'Nuevo apartamento' : 'Editar datos'}
      subtitulo={nuevo ? 'Nace retirado de la venta.' : <><span className="codigo">{apartamento.codigo}</span> · {apartamento.nombre}</>}
      enviando={enviando}
      rechazo={rechazo}
      textoBoton={nuevo ? 'Crear apartamento' : 'Guardar cambios'}
      textoEnviando={nuevo ? 'Creando…' : 'Guardando…'}
      alCerrar={alCerrar}
      alEnviar={enviar}
    >
      <fieldset className="formulario-apartamento" disabled={enviando}>
        {nuevo && (
          <div className="campo">
            <label className="campo-rotulo" htmlFor={`${id}-codigo`}>Código</label>
            <div className="campo-unidad">
              <span className="campo-unidad-antes codigo" aria-hidden="true">APT-</span>
              <input
                id={`${id}-codigo`}
                className="campo-control codigo"
                inputMode="numeric"
                autoComplete="off"
                data-foco-inicial
                value={form.numero}
                aria-invalid={Boolean(errores.numero)}
                aria-describedby={`${id}-codigo-pista${errores.numero ? ` ${id}-codigo-error` : ''}`}
                onChange={(e) => cambiar('numero', e.target.value.replace(/\D/g, ''))}
                onBlur={revisarCodigo}
              />
            </div>
            {errores.numero && <p className="campo-mensaje" id={`${id}-codigo-error`}>{errores.numero}</p>}
            <p className="campo-pista" id={`${id}-codigo-pista`}>Empieza por APT-. No se puede cambiar después.</p>
          </div>
        )}

        <CampoConContador id={`${id}-nombre`} rotulo="Nombre" valor={form.nombre} maximo={MAXIMO_NOMBRE}
          error={errores.nombre} focoInicial={!nuevo} alCambiar={(v) => cambiar('nombre', v)} />
        <CampoConContador id={`${id}-descripcion`} rotulo="Descripción" nota="opcional" varias
          valor={form.descripcion} maximo={MAXIMO_DESCRIPCION} error={errores.descripcion}
          alCambiar={(v) => cambiar('descripcion', v)} />

        <div className="formulario-apartamento-pareja">
          {nuevo && (
            <Campo etiqueta="Capacidad (personas)" type="number" min={1} inputMode="numeric" value={form.capacidad}
              error={errores.capacidad} onChange={(e) => cambiar('capacidad', e.target.value)} />
          )}
          <Campo etiqueta="Dormitorios" type="number" min={1} inputMode="numeric" value={form.dormitorios}
            error={errores.dormitorios} onChange={(e) => cambiar('dormitorios', e.target.value)} />
        </div>

        <div className="campo">
          <label className="campo-rotulo" htmlFor={`${id}-caracteristica`}>Características</label>
          <div className="agregar-en-linea">
            <input
              id={`${id}-caracteristica`}
              className="campo-control"
              placeholder="Por ejemplo, Balcón"
              autoComplete="off"
              maxLength={100}
              value={caracteristica}
              aria-invalid={Boolean(errores.caracteristicas || errorCaracteristica)}
              aria-describedby={`${id}-caracteristicas-mensaje`}
              onKeyDown={alTeclear}
              onChange={(e) => {
                setCaracteristica(e.target.value)
                setErrorCaracteristica(null)
              }}
            />
            <button type="button" className="boton boton-secundario" onClick={sumarCaracteristica}>Agregar</button>
          </div>
          {form.caracteristicas.length > 0 && (
            <ul className="fichas" aria-label="Características agregadas">
              {form.caracteristicas.map((texto) => (
                <li key={texto} className="etiqueta ficha">
                  {texto}
                  <button
                    type="button"
                    className="ficha-quitar"
                    aria-label={`Quitar ${texto}`}
                    onClick={() => cambiar('caracteristicas', form.caracteristicas.filter((c) => c !== texto))}
                  >
                    <Icono nombre="cerrar" />
                  </button>
                </li>
              ))}
            </ul>
          )}
          <p id={`${id}-caracteristicas-mensaje`}
            className={errores.caracteristicas || errorCaracteristica ? 'campo-mensaje' : 'campo-pista'}>
            {errorCaracteristica ?? errores.caracteristicas ?? 'Escribe una y pulsa «Agregar». Debe haber al menos una.'}
          </p>
        </div>

        {nuevo && (
          <fieldset className="campo fotos-formulario" aria-describedby={`${id}-fotos-mensaje`}>
            <legend className="campo-rotulo">Fotos</legend>
            <p className="campo-pista">Pega el enlace de una foto ya publicada (https://…).</p>
            <ul className="fotos-renglones">
              {form.imagenes.map((imagen, indice) => (
                // El renglón no tiene otra identidad que su lugar: la dirección cambia mientras se escribe
                <li key={indice} className="foto-renglon">
                  <div className="foto-renglon-campos">
                    <label className="campo-rotulo" htmlFor={`${id}-foto-${indice}`}>
                      Dirección de la imagen{form.imagenes.length > 1 ? ` ${indice + 1}` : ''}
                    </label>
                    <input
                      id={`${id}-foto-${indice}`}
                      className="campo-control"
                      type="url"
                      inputMode="url"
                      placeholder="https://…"
                      maxLength={MAXIMO_URL}
                      value={imagen.url}
                      aria-invalid={fotosConError}
                      onChange={(e) => cambiar('imagenes', form.imagenes.map((actual, i) =>
                        (i === indice ? { ...actual, url: e.target.value } : actual)))}
                    />
                    <label className="opcion-principal">
                      <input
                        type="radio"
                        name={`${id}-principal`}
                        checked={imagen.principal}
                        onChange={() => cambiar('imagenes', form.imagenes.map((actual, i) =>
                          ({ ...actual, principal: i === indice })))}
                      />
                      Principal
                    </label>
                  </div>
                  {form.imagenes.length > 1 && (
                    <button
                      type="button"
                      className="boton boton-secundario boton-icono"
                      aria-label={`Quitar la foto ${indice + 1}`}
                      onClick={() => cambiar('imagenes', quitarFoto(form.imagenes, indice))}
                    >
                      <span aria-hidden="true">✕</span>
                    </button>
                  )}
                </li>
              ))}
            </ul>
            {errores.imagenes && <p className="campo-mensaje" id={`${id}-fotos-mensaje`}>{errores.imagenes}</p>}
            {form.imagenes.length < MAXIMO_FOTOS && (
              <button type="button" className="boton boton-secundario boton-chico"
                onClick={() => cambiar('imagenes', agregarFoto(form.imagenes))}>
                <span aria-hidden="true">+</span> Agregar otra foto
              </button>
            )}
            <p className="campo-pista">Hasta {MAXIMO_FOTOS} fotos. Marca una como principal.</p>
          </fieldset>
        )}
      </fieldset>
    </DialogoFormulario>
  )
}
