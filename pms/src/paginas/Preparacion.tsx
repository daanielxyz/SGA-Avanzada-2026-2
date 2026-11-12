import { useCallback, useEffect, useRef, useState } from 'react'
import {
  listarPendientesPreparacion,
  listarTodosLosApartamentos,
  moverApartamento,
  type ApartamentoOperativo,
  type PasoPreparacion,
} from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import EstadoApartamento from '../componentes/EstadoApartamento'
import { NOMBRE_ESTADO_OPERATIVO } from '../componentes/estadosApartamento'
import { useTitulo } from '../componentes/useTitulo'
import { cantidad, hora } from '../formato'
import { errorParaMostrar } from '../reserva/mensajes'
import { useSesion } from '../sesion/contexto'

type Pestana = 'pendientes' | 'todos'
type Confirmar = { tipo: 'preparado' | 'fuera'; apartamento: ApartamentoOperativo }

/** La acción principal según el estado; la ruta de preparación es la única del personal de servicio (EOPE-04). */
const PASO_PRINCIPAL: Partial<Record<string, { paso: PasoPreparacion; texto: string }>> = {
  PENDIENTE_PREPARACION: { paso: 'iniciar-preparacion', texto: 'Empezar a preparar' },
  EN_PREPARACION: { paso: 'marcar-preparado', texto: 'Marcar como preparado' },
}

/** Los ya empezados primero: es lo que el personal de servicio tiene entre manos. */
function ordenarPendientes(lista: ApartamentoOperativo[]): ApartamentoOperativo[] {
  const peso = (estado: string) => (estado === 'EN_PREPARACION' ? 0 : 1)
  return [...lista].sort((a, b) => peso(a.estadoOperativo) - peso(b.estadoOperativo))
}

function horaActual(): string {
  const ahora = new Date()
  return hora(`${ahora.getHours()}:${ahora.getMinutes()}`)
}

/**
 * Preparación de apartamentos (CU-41 · CU-42), pensada para el celular del personal de servicio: una tarjeta por
 * apartamento con una sola acción grande. El administrador además ve todos los apartamentos y puede declararlos fuera
 * de servicio o devolverlos a la preparación.
 */
export default function Preparacion() {
  const { sesion } = useSesion()
  const esAdministrador = sesion?.rol === 'ADMINISTRADOR'
  useTitulo(esAdministrador ? 'Preparación de apartamentos' : 'Por preparar')
  const [pestana, setPestana] = useState<Pestana>('pendientes')
  const [intento, setIntento] = useState(0)
  const [pendientes, setPendientes] = useState<{ lista: ApartamentoOperativo[]; total: number; paginas: number; pagina: number; consultado: string } | null>(null)
  const [todos, setTodos] = useState<ApartamentoOperativo[] | null>(null)
  const [errorCarga, setErrorCarga] = useState<unknown>(null)
  const [cargandoMas, setCargandoMas] = useState(false)
  const [enviando, setEnviando] = useState<string | null>(null)
  const [rechazo, setRechazo] = useState<{ codigo: string; mensaje: string } | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [confirmar, setConfirmar] = useState<Confirmar | null>(null)
  const titulo = useRef<HTMLHeadingElement>(null)

  const pedirDatos = useCallback(async () => {
    const [primera, lista] = await Promise.all([
      listarPendientesPreparacion(0),
      esAdministrador ? listarTodosLosApartamentos() : Promise.resolve(null),
    ])
    return { primera, lista }
  }, [esAdministrador])

  const aplicar = useCallback(({ primera, lista }: Awaited<ReturnType<typeof pedirDatos>>) => {
    setPendientes({
      lista: ordenarPendientes(primera.contenido),
      total: primera.totalElementos,
      paginas: primera.totalPaginas,
      pagina: 0,
      consultado: horaActual(),
    })
    if (lista) setTodos([...lista].sort((a, b) => a.codigo.localeCompare(b.codigo)))
    setErrorCarga(null)
  }, [])

  /** Vuelve a leer sin mostrar «cargando»: otra persona pudo mover apartamentos mientras tanto. */
  const cargar = () => pedirDatos().then(aplicar)

  useEffect(() => {
    let vigente = true
    pedirDatos()
      .then((datos) => {
        if (vigente) aplicar(datos)
      })
      .catch((causa: unknown) => {
        if (vigente) setErrorCarga(causa)
      })
    return () => {
      vigente = false
    }
  }, [pedirDatos, aplicar, intento])

  async function verMas() {
    if (!pendientes) return
    setCargandoMas(true)
    try {
      const siguiente = await listarPendientesPreparacion(pendientes.pagina + 1)
      setPendientes({
        ...pendientes,
        lista: ordenarPendientes([...pendientes.lista, ...siguiente.contenido]),
        pagina: pendientes.pagina + 1,
        paginas: siguiente.totalPaginas,
        total: siguiente.totalElementos,
      })
    } catch (causa) {
      setRechazo({ codigo: '', mensaje: errorParaMostrar(causa).mensaje })
    } finally {
      setCargandoMas(false)
    }
  }

  /** Hace el paso y vuelve a leer: otra persona pudo mover otros apartamentos mientras tanto. */
  async function mover(apartamento: ApartamentoOperativo, paso: PasoPreparacion) {
    setEnviando(apartamento.codigo)
    setRechazo(null)
    setAviso(null)
    try {
      const nuevo = await moverApartamento(apartamento.codigo, paso)
      await cargar().catch(() => undefined)
      if (nuevo.estadoOperativo === 'PREPARADO') {
        setAviso(`${apartamento.codigo} quedó preparado. Recepción ya puede entregarlo.`)
      } else if (paso === 'declarar-fuera-de-servicio') {
        setAviso(`${apartamento.codigo} quedó fuera de servicio. No se puede entregar hasta volverlo a preparación.`)
      } else if (paso === 'marcar-pendiente-preparacion') {
        setAviso(`${apartamento.codigo} volvió a preparación.`)
      }
      setConfirmar(null)
      titulo.current?.focus()
    } catch (causa) {
      const mensaje = causa instanceof ErrorApi && causa.estado === 409
        ? `Otra persona ya lo cambió o el apartamento cambió de estado. ${causa.message}`
        : errorParaMostrar(causa).mensaje
      setRechazo({ codigo: apartamento.codigo, mensaje })
      setConfirmar(null)
      await cargar().catch(() => undefined)
    } finally {
      setEnviando(null)
    }
  }

  const tarjeta = (apartamento: ApartamentoOperativo, vista: Pestana) => {
    const principal = PASO_PRINCIPAL[apartamento.estadoOperativo]
    const ocupado = enviando === apartamento.codigo
    const fueraDeServicio = apartamento.estadoOperativo === 'FUERA_DE_SERVICIO'
    const puedeSalir = esAdministrador && vista === 'todos' && !fueraDeServicio && apartamento.estadoOperativo !== 'OCUPADO'
    return (
      <li
        key={apartamento.codigo}
        className={`tarjeta preparacion-tarjeta${apartamento.estadoOperativo === 'EN_PREPARACION' ? ' preparacion-empezado' : ''}`}
        aria-busy={ocupado || undefined}
      >
        <span className="codigo preparacion-codigo">{apartamento.codigo}</span>
        <h3 className="preparacion-nombre">{apartamento.nombre}</h3>
        <EstadoApartamento estado={apartamento.estadoOperativo} etiqueta />
        {principal && (
          <button
            type="button"
            className="boton boton-xl"
            disabled={Boolean(enviando)}
            aria-label={`${principal.texto}: ${apartamento.codigo}`}
            onClick={() => (principal.paso === 'marcar-preparado'
              ? setConfirmar({ tipo: 'preparado', apartamento })
              : mover(apartamento, principal.paso))}
          >
            {ocupado ? 'Guardando…' : principal.texto}
          </button>
        )}
        {esAdministrador && vista === 'todos' && fueraDeServicio && (
          <button
            type="button"
            className="boton boton-xl"
            disabled={Boolean(enviando)}
            aria-label={`Volver a preparación: ${apartamento.codigo}`}
            onClick={() => mover(apartamento, 'marcar-pendiente-preparacion')}
          >
            {ocupado ? 'Guardando…' : 'Volver a preparación'}
          </button>
        )}
        {puedeSalir && (
          <button
            type="button"
            className="boton boton-secundario boton-grande"
            disabled={Boolean(enviando)}
            aria-label={`Declarar fuera de servicio: ${apartamento.codigo}`}
            onClick={() => setConfirmar({ tipo: 'fuera', apartamento })}
          >
            Declarar fuera de servicio
          </button>
        )}
        {vista === 'todos' && apartamento.estadoOperativo === 'OCUPADO' && (
          <p className="campo-pista">Se libera al registrar la salida.</p>
        )}
        {rechazo?.codigo === apartamento.codigo && <p className="campo-mensaje" role="alert">{rechazo.mensaje}</p>}
      </li>
    )
  }

  const cargando = !pendientes && !errorCarga
  const visibles = pestana === 'todos' && esAdministrador ? todos ?? [] : pendientes?.lista ?? []
  const empezados = pendientes?.lista.filter((a) => a.estadoOperativo === 'EN_PREPARACION') ?? []
  const porEmpezar = pendientes?.lista.filter((a) => a.estadoOperativo !== 'EN_PREPARACION') ?? []

  const vistaPendientes = pendientes && (
    pendientes.lista.length === 0 ? (
      <div className="tarjeta preparacion-vacia">
        <p className="preparacion-vacia-titulo"><span aria-hidden="true">✓</span> Todo está preparado</p>
        <p className="suave">Última consulta: {pendientes.consultado}.</p>
        <button type="button" className="boton boton-secundario boton-grande" onClick={() => setIntento(intento + 1)}>
          Actualizar
        </button>
      </div>
    ) : (
      <>
        {empezados.length > 0 && (
          <section aria-labelledby="titulo-empezados">
            <h2 id="titulo-empezados" className="preparacion-grupo">Ya empezados ({empezados.length})</h2>
            <ul className="preparacion-lista">{empezados.map((a) => tarjeta(a, 'pendientes'))}</ul>
          </section>
        )}
        {porEmpezar.length > 0 && (
          <section aria-labelledby="titulo-por-empezar">
            <h2 id="titulo-por-empezar" className="preparacion-grupo">Pendientes ({porEmpezar.length})</h2>
            <ul className="preparacion-lista">{porEmpezar.map((a) => tarjeta(a, 'pendientes'))}</ul>
          </section>
        )}
        {pendientes.pagina + 1 < pendientes.paginas && (
          <button type="button" className="boton boton-secundario boton-grande" disabled={cargandoMas} onClick={verMas}>
            {cargandoMas ? 'Cargando…' : 'Ver más'}
          </button>
        )}
      </>
    )
  )

  return (
    <div className="preparacion">
      <div>
        <h1 ref={titulo} tabIndex={-1}>{esAdministrador ? 'Preparación de apartamentos' : 'Por preparar'}</h1>
        {!esAdministrador && pendientes && (
          <p className="suave">{cantidad(pendientes.total, 'apartamento', 'apartamentos')}</p>
        )}
      </div>

      {aviso && <Aviso tipo="exito" asunto={aviso} mensaje="La lista ya está al día." />}
      {rechazo && !visibles.some((a) => a.codigo === rechazo.codigo) && (
        <Aviso tipo="error" asunto={rechazo.codigo ? `No se cambió ${rechazo.codigo}` : 'No pudimos cargar más'} mensaje={rechazo.mensaje} />
      )}

      {cargando && (
        <div className="preparacion-lista" aria-busy="true">
          {Array.from({ length: 3 }, (_, n) => <span key={n} className="tarjeta fantasma preparacion-fantasma" />)}
          <p className="suave" role="status">Cargando lo que falta por preparar…</p>
        </div>
      )}
      {Boolean(errorCarga) && (
        <Aviso
          tipo="error"
          asunto="No pudimos cargar los apartamentos"
          mensaje={errorCarga instanceof ErrorApi ? errorCarga.message : 'Revisa tu conexión e inténtalo de nuevo.'}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      )}

      {pendientes && esAdministrador && (
        <div className="preparacion-pestanas" role="tablist" aria-label="Qué apartamentos ver">
          <button
            type="button"
            role="tab"
            id="pestana-pendientes"
            aria-selected={pestana === 'pendientes'}
            aria-controls="panel-preparacion"
            onClick={() => setPestana('pendientes')}
          >
            Por preparar <span className="suave">({pendientes.total})</span>
          </button>
          <button
            type="button"
            role="tab"
            id="pestana-todos"
            aria-selected={pestana === 'todos'}
            aria-controls="panel-preparacion"
            onClick={() => setPestana('todos')}
          >
            Todos <span className="suave">({todos?.length ?? 0})</span>
          </button>
        </div>
      )}

      {pendientes && (
        <div
          id="panel-preparacion"
          role={esAdministrador ? 'tabpanel' : undefined}
          aria-labelledby={esAdministrador ? `pestana-${pestana}` : undefined}
          className="preparacion-panel"
        >
          {pestana === 'pendientes' || !esAdministrador ? vistaPendientes : (
            <ul className="preparacion-lista">{(todos ?? []).map((a) => tarjeta(a, 'todos'))}</ul>
          )}
        </div>
      )}

      {confirmar?.tipo === 'preparado' && (
        <Dialogo
          titulo="Marcar como preparado"
          subtitulo={<span className="codigo">{confirmar.apartamento.codigo}</span>}
          ocupado={Boolean(enviando)}
          alCerrar={() => setConfirmar(null)}
        >
          <div className="dialogo-contenido">
            <div className="dialogo-cuerpo">
              <p className="dialogo-texto">
                ¿<span className="codigo">{confirmar.apartamento.codigo}</span> {confirmar.apartamento.nombre} quedó listo para entregar?
              </p>
              <p className="suave">Desde ahora recepción ya puede entregarlo, y no lo podrás devolver a pendiente.</p>
              {enviando && <p className="suave" role="status">Guardando…</p>}
            </div>
            <div className="dialogo-pie">
              <button type="button" className="boton boton-secundario boton-grande" disabled={Boolean(enviando)} data-foco-inicial onClick={() => setConfirmar(null)}>
                Todavía no
              </button>
              <button
                type="button"
                className="boton boton-grande"
                disabled={Boolean(enviando)}
                onClick={() => mover(confirmar.apartamento, 'marcar-preparado')}
              >
                {enviando ? 'Guardando…' : 'Sí, está preparado'}
              </button>
            </div>
          </div>
        </Dialogo>
      )}
      {confirmar?.tipo === 'fuera' && (
        <Dialogo
          titulo="Declarar fuera de servicio"
          subtitulo={<span className="codigo">{confirmar.apartamento.codigo}</span>}
          ocupado={Boolean(enviando)}
          alCerrar={() => setConfirmar(null)}
        >
          <div className="dialogo-contenido">
            <div className="dialogo-cuerpo">
              <p className="dialogo-texto">
                <span className="codigo">{confirmar.apartamento.codigo}</span> {confirmar.apartamento.nombre} no se podrá entregar hasta
                que lo vuelvas a preparación.
              </p>
              <p className="suave">
                Hoy está {(NOMBRE_ESTADO_OPERATIVO[confirmar.apartamento.estadoOperativo] ?? confirmar.apartamento.estadoOperativo).toLowerCase()}.
              </p>
              {enviando && <p className="suave" role="status">Guardando…</p>}
            </div>
            <div className="dialogo-pie">
              <button type="button" className="boton boton-secundario boton-grande" disabled={Boolean(enviando)} data-foco-inicial onClick={() => setConfirmar(null)}>
                Volver
              </button>
              <button
                type="button"
                className="boton boton-peligro boton-grande"
                disabled={Boolean(enviando)}
                onClick={() => mover(confirmar.apartamento, 'declarar-fuera-de-servicio')}
              >
                {enviando ? 'Guardando…' : 'Declarar fuera de servicio'}
              </button>
            </div>
          </div>
        </Dialogo>
      )}
    </div>
  )
}
