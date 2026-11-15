// Piezas que comparten la lista de canales, los conflictos y la bitácora (mockups S5Canales y S5Bitacora).
import type { ReactNode } from 'react'
import type { Canal, Conflicto, ResultadoEvento } from '../api/canales'
import { enlaceTelefono, partirCodigos, RESULTADOS } from './logica'

/** Un texto del backend con sus códigos (APT-103, RES-2026-00042) en letra de código. */
export function ConCodigos({ texto }: { texto: string }) {
  return (
    <>
      {partirCodigos(texto).map((parte, i) => (parte.codigo
        ? <span key={i} className="codigo">{parte.texto}</span>
        : <span key={i}>{parte.texto}</span>))}
    </>
  )
}

/** «✓ Activo» u «○ Inactivo»: la forma y la palabra, no solo el color. */
export function EstadoCanal({ canal }: { canal: Pick<Canal, 'activo'> }) {
  return (
    <span className={canal.activo ? 'canal-activo' : undefined}>
      <span className="etiqueta">
        <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">
          {canal.activo ? <path d="M2.5 6.5l2.3 2.3 4.7-5" /> : <circle cx="6" cy="6" r="4" />}
        </svg>
        {canal.activo ? 'Activo' : 'Inactivo'}
      </span>
    </span>
  )
}

const FORMAS: Record<ResultadoEvento, ReactNode> = {
  EXITOSO: <path d="M2.5 6.5l2.3 2.3 4.7-5" />,
  DUPLICADO: <><rect x="1.5" y="4" width="6" height="6" rx="1" /><path d="M4.5 4V2.5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H7.5" /></>,
  CONFLICTO: <><path d="M6 1.5l4.8 8.6H1.2z" /><path d="M6 5v2.2" /><circle className="relleno" cx="6" cy="8.8" r=".7" /></>,
  RECHAZADO: <><circle cx="6" cy="6" r="4.5" /><path d="M2.9 9.1l6.2-6.2" /></>,
  ERROR: <path d="M3 3l6 6M9 3l-6 6" />,
}

/** El resultado de un mensaje del canal, con su forma: ✓, dos cuadros, triángulo, círculo tachado o ✕. */
export function ResultadoMensaje({ resultado }: { resultado: ResultadoEvento }) {
  const { texto, clase } = RESULTADOS[resultado]
  return (
    <span className={`etiqueta ${clase}`}>
      <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">{FORMAS[resultado]}</svg>
      {texto}
    </span>
  )
}

/**
 * A quién llamar por un conflicto (DEC-129): el nombre y el teléfono como enlace; si no hay teléfono, el correo. Los
 * conflictos de antes no traen contacto.
 */
export function ContactoConflicto({ conflicto }: { conflicto: Conflicto }) {
  const { titularNombre: nombre, titularTelefono: telefono, titularCorreo: correo } = conflicto
  if (!nombre) return <span className="suave">Sin datos de contacto</span>
  return (
    <>
      <span className="texto-largo">{nombre}</span>
      {telefono && (
        <a className="conflicto-telefono" href={enlaceTelefono(telefono)} aria-label={`Llamar a ${nombre} al ${telefono}`}>
          {telefono}
        </a>
      )}
      {!telefono && correo && (
        <a className="conflicto-telefono conflicto-correo" href={`mailto:${correo}`} aria-label={`Escribirle a ${nombre} a ${correo}`}>
          {correo}
        </a>
      )}
      {!telefono && !correo && <span className="conflicto-telefono suave">Sin teléfono ni correo</span>}
    </>
  )
}
