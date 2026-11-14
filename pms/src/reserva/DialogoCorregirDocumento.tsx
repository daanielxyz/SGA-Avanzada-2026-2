import { useEffect, useState, type FormEvent } from 'react'
import { ErrorApi } from '../api/cliente'
import { corregirDocumentoOcupante, corregirDocumentoTitular, type Ocupante, type Reserva } from '../api/reservas'
import { consultarTitular } from '../api/titulares'
import DialogoFormulario, { type Rechazo } from '../apartamentos/DialogoFormulario'
import { enfocarPrimerError } from '../apartamentos/formulario'
import CampoMotivo from '../folio/CampoMotivo'
import { MOTIVO_MAXIMO, TIPOS_DOCUMENTO, TIPOS_DOCUMENTO_OCUPANTE } from './llegada/llegada'
import { errorParaMostrar } from './mensajes'
import Campo from './nueva/Campo'
import { mensajeDocumentoPorEdad, tiposPorEdad } from './nueva/formulario'

/** De quién es el documento: un huésped de la reserva o su titular, que puede no alojarse. */
export type Corregido = { ocupante: Ocupante } | { titular: true }

interface Documento {
  tipo: string
  numero: string
}

/** «CC 1094000000», como en la tabla de ocupantes; el pasaporte, con su nombre. */
function textoDocumento(tipo: string, numero: string): string {
  return `${tipo === 'PASAPORTE' ? 'Pasaporte' : tipo} ${numero}`.trim()
}

/**
 * «Corregir documento» (DEC-120): cuando se escribió mal. Solo cambia el documento; los demás datos del TRA, el huésped
 * principal y las reservas del titular se conservan. Pide el motivo, que queda con quién y cuándo en el historial.
 */
export default function DialogoCorregirDocumento({ reserva, corregido, alCerrar, alCorregir }: {
  reserva: Reserva
  corregido: Corregido
  alCerrar: () => void
  alCorregir: (reserva: Reserva) => void
}) {
  const esTitular = 'titular' in corregido
  const nombre = esTitular ? reserva.titularNombre ?? reserva.titularId : corregido.ocupante.nombre
  // El documento del titular no viene en la reserva: se lee de su ficha
  const [actual, setActual] = useState<Documento | null>(esTitular
    ? null
    : { tipo: corregido.ocupante.tipoDocumento ?? '', numero: corregido.ocupante.numeroDocumento ?? '' })
  const [errorAlLeer, setErrorAlLeer] = useState(false)
  const [tipo, setTipo] = useState(esTitular ? '' : corregido.ocupante.tipoDocumento ?? '')
  const [numero, setNumero] = useState('')
  const [motivo, setMotivo] = useState('')
  const [errores, setErrores] = useState<{ tipo?: string; numero?: string; motivo?: string }>({})
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)

  useEffect(() => {
    if (!esTitular) return
    let vigente = true
    consultarTitular(reserva.titularId)
      .then((titular) => {
        if (!vigente) return
        setActual({ tipo: titular.tipoDocumento, numero: titular.numeroDocumento })
        setTipo(titular.tipoDocumento)
      })
      .catch(() => vigente && setErrorAlLeer(true))
    return () => {
      vigente = false
    }
  }, [esTitular, reserva.titularId])

  // El titular no puede tener registro civil (TIT-01); un huésped, los de su edad (#18)
  const tipos = esTitular
    ? TIPOS_DOCUMENTO
    : tiposPorEdad(TIPOS_DOCUMENTO_OCUPANTE, corregido.ocupante.fechaNacimiento, reserva.entrada, tipo)

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const limpio = numero.trim()
    const encontrados = {
      tipo: !tipo ? 'Elige el tipo de documento.'
        : esTitular ? undefined
        : mensajeDocumentoPorEdad(tipo, corregido.ocupante.fechaNacimiento, reserva.entrada) ?? undefined,
      numero: !limpio ? 'Escribe el número como debe quedar.'
        : actual && tipo === actual.tipo && limpio === actual.numero ? 'Es el mismo documento que tiene ahora.' : undefined,
      motivo: motivo.trim() ? undefined : 'Escribe por qué se corrige.',
    }
    setErrores(encontrados)
    setRechazo(null)
    if (Object.values(encontrados).some(Boolean)) {
      enfocarPrimerError(formulario)
      return
    }
    const datos = { tipoDocumento: tipo, numeroDocumento: limpio, motivo: motivo.trim() }
    setEnviando(true)
    try {
      alCorregir(esTitular
        ? await corregirDocumentoTitular(reserva.codigo, datos)
        : await corregirDocumentoOcupante(reserva.codigo, corregido.ocupante.id, datos))
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.estado === 400 && causa.detalles.length > 0) {
        setRechazo({ asunto: 'No se corrigió el documento', mensaje: 'Revisa estos datos:', detalles: causa.detalles })
      } else {
        setRechazo({ asunto: 'No se corrigió el documento', ...errorParaMostrar(causa) })
      }
    }
  }

  return (
    <DialogoFormulario
      titulo="Corregir documento"
      subtitulo={<>{esTitular ? 'Titular' : 'Huésped'} · <span className="texto-largo">{nombre}</span></>}
      enviando={enviando}
      rechazo={rechazo}
      textoBoton="Corregir documento"
      textoEnviando="Corrigiendo…"
      bloqueado={esTitular && !actual}
      alCerrar={alCerrar}
      alEnviar={enviar}
    >
      <p className="campo-pista">
        Solo para un documento mal escrito: los demás datos se conservan.
        {esTitular
          ? ' Sigue siendo el mismo titular, con sus reservas y su cuenta; si se aloja, su documento de huésped también se corrige.'
          : ' Si es el titular de la reserva, su documento de titular también se corrige.'}
      </p>
      <div className="campo">
        <p className="campo-rotulo">Documento actual</p>
        <p className="dato-solo-lectura codigo" aria-live="polite">
          {actual ? textoDocumento(actual.tipo, actual.numero) : errorAlLeer ? 'No se pudo leer. Cierra y vuelve a abrir.' : 'Cargando…'}
        </p>
      </div>
      <div className="campos-2">
        <Campo etiqueta="Tipo nuevo" opciones={[['', 'Elige'], ...tipos]} value={tipo} error={errores.tipo}
          disabled={enviando} onChange={(e) => {
            setTipo(e.target.value)
            setErrores({ ...errores, tipo: undefined, numero: undefined })
          }} />
        <Campo etiqueta="Número nuevo" value={numero} maxLength={30} autoComplete="off" error={errores.numero}
          disabled={enviando} data-foco-inicial onChange={(e) => {
            setNumero(e.target.value)
            setErrores({ ...errores, numero: undefined })
          }} />
      </div>
      <CampoMotivo id="motivo-correccion" rotulo="Motivo" valor={motivo} maximo={MOTIVO_MAXIMO} error={errores.motivo}
        deshabilitado={enviando} pista="Por ejemplo: se escribió mal un número al hacer la reserva."
        alCambiar={(valor) => {
          setMotivo(valor)
          setErrores({ ...errores, motivo: undefined })
        }} />
    </DialogoFormulario>
  )
}
