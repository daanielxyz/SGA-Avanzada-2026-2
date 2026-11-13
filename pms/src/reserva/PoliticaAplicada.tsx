import { useEffect, useState } from 'react'
import { consultarPolitica, type Politica } from '../api/politicas'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import PoliticaContada from '../politica/PoliticaContada'
import { resumenPolitica } from './politica'

type Carga = { clave: string; politica: Politica } | { clave: string; error: true }

/**
 * La política congelada en la reserva, en una línea y con «Ver política» (RN-13 · DEC-104). Si no carga, el resto del
 * detalle sigue y aquí se ofrece «Reintentar».
 */
export default function PoliticaAplicada({ politicaId, codigo }: { politicaId: string; codigo: string }) {
  const [intento, setIntento] = useState(0)
  const [carga, setCarga] = useState<Carga | null>(null)
  const [abierta, setAbierta] = useState(false)
  const clave = `${politicaId}#${intento}`

  useEffect(() => {
    let vigente = true
    consultarPolitica(politicaId)
      .then((politica) => {
        if (vigente) setCarga({ clave, politica })
      })
      .catch(() => {
        if (vigente) setCarga({ clave, error: true })
      })
    return () => {
      vigente = false
    }
  }, [politicaId, clave])

  const actual = carga?.clave === clave ? carga : null
  if (!actual) {
    return (
      <span className="politica-cargando" aria-busy="true">
        <span className="fantasma" />
        <span className="suave">Cargando la política…</span>
      </span>
    )
  }
  if ('error' in actual) {
    return (
      <span className="politica-error">
        <span className="campo-mensaje">No pudimos cargar la política.</span>
        <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
          Reintentar
        </button>
      </span>
    )
  }

  const { politica } = actual
  return (
    <>
      <span className="politica-resumen">{resumenPolitica(politica)}</span>
      <button type="button" className="boton boton-secundario boton-chico dato-accion-bloque" onClick={() => setAbierta(true)}>
        Ver política
      </button>
      {abierta && (
        <Dialogo titulo="Política de la reserva" subtitulo={<span className="codigo">{codigo}</span>} alCerrar={() => setAbierta(false)}>
          <div className="dialogo-contenido">
            <div className="dialogo-cuerpo">
              <PoliticaContada politica={politica} />
              <Aviso tipo="info" mensaje="Es la versión que tenía la política al crear la reserva." />
            </div>
            <div className="dialogo-pie">
              <button type="button" className="boton" data-foco-inicial onClick={() => setAbierta(false)}>Cerrar</button>
            </div>
          </div>
        </Dialogo>
      )}
    </>
  )
}
