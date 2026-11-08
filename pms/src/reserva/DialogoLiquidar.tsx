import { useEffect, useState } from 'react'
import { ErrorApi } from '../api/cliente'
import type { SituacionSaldo } from '../api/folio'
import {
  cancelarReserva,
  declararNoShow,
  simularCancelacion,
  type Liquidacion,
  type Reserva,
  type SimulacionCancelacion,
} from '../api/reservas'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import { dinero, fechaCorta, noches, sumarDias } from '../formato'
import { mensajeDeRechazo } from './mensajes'
import ResumenDialogo from './ResumenDialogo'

export type TipoLiquidacion = 'cancelar' | 'noShow'

const SITUACION: Record<SituacionSaldo, string> = { PENDIENTE: 'Pendiente', A_FAVOR: 'A favor', AL_DIA: 'Al día' }

const TEXTOS = {
  cancelar: {
    titulo: 'Cancelar reserva',
    enviando: 'Cancelando…',
    errorAsunto: 'No se pudo cancelar',
    resultado: 'Reserva cancelada',
    llamar: cancelarReserva,
    explicar: (liberadas: string) => `Al cancelar se libera ${liberadas} para otras reservas.`,
  },
  noShow: {
    titulo: 'Declarar no-show',
    enviando: 'Declarando…',
    errorAsunto: 'No se pudo declarar el no-show',
    resultado: 'No-show declarado',
    llamar: declararNoShow,
    explicar: (liberadas: string) => `El huésped no llegó. Se libera ${liberadas} y la reserva queda como «No se presentó».`,
  },
} as const

/** «la noche del 14 nov 2026» o «lo reservado: 2 noches, del 14 nov 2026 al 15 nov 2026» (la de salida no se ocupa). */
function nochesLiberadas({ entrada, salida }: Reserva): string {
  const total = noches(entrada, salida)
  if (total === 1) return `la noche del ${fechaCorta(entrada)}`
  return `lo reservado: ${total} noches, del ${fechaCorta(entrada)} al ${fechaCorta(sumarDias(salida, -1))},`
}

type Previa =
  | { estado: 'calculando' }
  | { estado: 'lista'; valor: SimulacionCancelacion }
  | { estado: 'error'; mensaje: string; reintentable: boolean }

/**
 * Lo que se retendría al cancelar, pedido al abrir (DEC-91). Sin ese valor no se deja cancelar. Un 409 dice que la
 * reserva ya no se puede cancelar: reintentar no lo cambia.
 */
function usePreviaCancelacion(codigo: string, activa: boolean) {
  const [intento, setIntento] = useState(0)
  const [previa, setPrevia] = useState<{ intento: number; previa: Previa } | null>(null)

  useEffect(() => {
    if (!activa) return
    let vigente = true
    simularCancelacion(codigo)
      .then((valor) => {
        if (vigente) setPrevia({ intento, previa: { estado: 'lista', valor } })
      })
      .catch((causa: unknown) => {
        if (!vigente) return
        const regla = causa instanceof ErrorApi && causa.estado === 409
        setPrevia({
          intento,
          previa: {
            estado: 'error',
            mensaje: regla ? mensajeDeRechazo(causa as ErrorApi) : 'Sin ese valor no se puede continuar.',
            reintentable: !regla,
          },
        })
      })
    return () => {
      vigente = false
    }
  }, [codigo, activa, intento])

  const actual: Previa = previa?.intento === intento ? previa.previa : { estado: 'calculando' }
  return { previa: actual, reintentar: () => setIntento((n) => n + 1) }
}

/**
 * Cancelar o declarar el no-show (RN-08 · RN-12 · RN-13). Al cancelar, antes de decidir se muestra cuánto se
 * retendría (DEC-91). Para el no-show la API todavía no lo dice (`pms/DISENO.md`, #1b): se avisa y al terminar se
 * muestra el valor.
 *
 * @param alTerminar la acción se hizo; el detalle debe actualizarse
 * @param alFallar   algo falló: la reserva pudo cambiar, así que hay que volver a leerla
 */
export default function DialogoLiquidar({ tipo, reserva, alCerrar, alTerminar, alFallar }: {
  tipo: TipoLiquidacion
  reserva: Reserva
  alCerrar: () => void
  alTerminar: (liquidacion: Liquidacion) => void | Promise<void>
  alFallar: () => void
}) {
  const textos = TEXTOS[tipo]
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [liquidacion, setLiquidacion] = useState<Liquidacion | null>(null)
  const disponible = reserva.estado === 'CONFIRMADA' || (tipo === 'cancelar' && reserva.estado === 'PENDIENTE')
  const conPrevia = tipo === 'cancelar'
  const { previa, reintentar } = usePreviaCancelacion(reserva.codigo, conPrevia && disponible)
  const previaLista = !conPrevia || previa.estado === 'lista'

  async function ejecutar() {
    setEnviando(true)
    setError(null)
    try {
      const respuesta = await textos.llamar(reserva.codigo)
      setLiquidacion(respuesta)
      await alTerminar(respuesta)
    } catch (causa) {
      setError(causa instanceof ErrorApi ? mensajeDeRechazo(causa) : 'Inténtalo de nuevo en un momento.')
      alFallar()
    } finally {
      setEnviando(false)
    }
  }

  if (liquidacion) {
    return (
      <Dialogo titulo={textos.resultado} subtitulo={<span className="codigo">{reserva.codigo}</span>} alCerrar={alCerrar}>
        <div className="dialogo-contenido">
          <div className="dialogo-cuerpo">
            <Aviso tipo="exito" asunto={textos.resultado} mensaje="El detalle ya muestra el nuevo estado." />
            <dl className="resumen-dialogo">
              <dt>Valor retenido</dt>
              <dd className="codigo">{dinero(liquidacion.retenido)}</dd>
              <dt>Saldo</dt>
              <dd className="codigo">{dinero(liquidacion.saldo)}</dd>
              <dt>Situación</dt>
              <dd>{SITUACION[liquidacion.situacionSaldo]}</dd>
            </dl>
          </div>
          <div className="dialogo-pie">
            <button type="button" className="boton" data-foco-inicial onClick={alCerrar}>
              Cerrar
            </button>
          </div>
        </div>
      </Dialogo>
    )
  }

  return (
    <Dialogo titulo={textos.titulo} subtitulo="Revisa antes de decidir." ocupado={enviando} alCerrar={alCerrar}>
      <div className="dialogo-contenido">
        <div className="dialogo-cuerpo">
          {error && <Aviso tipo="error" asunto={textos.errorAsunto} mensaje={error} />}
          <ResumenDialogo reserva={reserva} />
          <p className="dialogo-texto">{textos.explicar(nochesLiberadas(reserva))}</p>
          {conPrevia ? (
            <>
              <Retencion previa={previa} alReintentar={reintentar} />
              <Aviso
                tipo="advertencia"
                asunto="No se puede deshacer"
                mensaje="La retención sale de la política de la reserva y es la de este momento: más tarde puede cambiar."
              />
            </>
          ) : (
            <Aviso
              tipo="advertencia"
              asunto="Puede haber una retención"
              mensaje="Esto no se puede deshacer. La retención depende de la política; verás el valor exacto al terminar."
            />
          )}
          {enviando && <p className="suave" role="status">{textos.enviando}</p>}
        </div>
        <div className="dialogo-pie">
          <button
            type="button"
            className="boton boton-secundario"
            data-foco-inicial
            disabled={enviando}
            onClick={alCerrar}
          >
            Volver
          </button>
          <button type="button" className="boton boton-peligro" disabled={enviando || !disponible || !previaLista} onClick={ejecutar}>
            {enviando ? textos.enviando : textos.titulo}
          </button>
        </div>
      </div>
    </Dialogo>
  )
}

/** «Se retendrá $ X», «No se retiene nada», calculando o el error con «Reintentar» (DEC-91). */
function Retencion({ previa, alReintentar }: { previa: Previa; alReintentar: () => void }) {
  if (previa.estado === 'error') {
    return (
      <Aviso
        tipo="error"
        asunto="No pudimos calcular la retención"
        mensaje={previa.mensaje}
        accion={previa.reintentable && (
          <button type="button" className="boton boton-secundario boton-chico" onClick={alReintentar}>
            Reintentar
          </button>
        )}
      />
    )
  }
  if (previa.estado === 'calculando') {
    return (
      <>
        <div className="retencion" aria-busy="true">
          <span>Se retendrá</span>
          <span className="fantasma retencion-fantasma" />
        </div>
        <p className="suave" role="status">Calculando la retención…</p>
      </>
    )
  }
  const { retenido } = previa.valor
  return (
    <div className="retencion">
      {retenido > 0 ? (
        <>
          <span>Se retendrá</span>
          <strong className="codigo retencion-valor">{dinero(retenido)}</strong>
        </>
      ) : (
        <>
          <span>Retención</span>
          <strong>No se retiene nada</strong>
        </>
      )}
    </div>
  )
}
