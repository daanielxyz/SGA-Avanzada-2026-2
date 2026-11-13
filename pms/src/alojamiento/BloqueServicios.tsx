import { useId, useState, type FormEvent } from 'react'
import {
  agregarServicio, cambiarValorServicio, desactivarServicio, type Alojamiento, type ServicioAdicional,
} from '../api/alojamiento'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import { dinero } from '../formato'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'
import { CampoBloque, Opciones } from './Bloque'
import type { AlBloque } from './useBloque'
import { textoPesos, valorServicio } from './formularios'

type Abierto = { tipo: 'nuevo' } | { tipo: 'valor'; servicio: ServicioAdicional } | { tipo: 'desactivar'; servicio: ServicioAdicional }

/** Los activos primero, en el orden del catálogo; los desactivados al final, sin acciones. */
function ordenar(servicios: ServicioAdicional[]): ServicioAdicional[] {
  return [...servicios.filter((s) => s.activo), ...servicios.filter((s) => !s.activo)]
}

function Cobro({ servicio }: { servicio: ServicioAdicional }) {
  return servicio.generaCargo
    ? <span className="etiqueta"><span aria-hidden="true">$</span> Se cobra</span>
    : <span className="etiqueta"><span aria-hidden="true">○</span> Incluido</span>
}

function EstadoServicio({ servicio }: { servicio: ServicioAdicional }) {
  return servicio.activo
    ? <span className="etiqueta etiqueta-activo"><span aria-hidden="true">✓</span> Activo</span>
    : <span className="etiqueta etiqueta-inactivo"><span aria-hidden="true">–</span> Desactivado</span>
}

/**
 * Catálogo de servicios que se cargan como consumos (SERV-01 · SERV-02 · SERV-04). Cada acción se guarda al
 * confirmarla. Lo ya cargado en los folios no cambia.
 */
export default function BloqueServicios({ alojamiento, alGuardar }: AlBloque) {
  const servicios = ordenar(alojamiento.serviciosAdicionales ?? [])
  const [abierto, setAbierto] = useState<Abierto | null>(null)
  const [hecho, setHecho] = useState<string | null>(null)
  const id = useId()

  function terminar(nuevo: Alojamiento, mensaje: string) {
    alGuardar(nuevo)
    setAbierto(null)
    setHecho(mensaje)
  }

  return (
    <section className="tarjeta bloque" aria-labelledby={id}>
      <div className="bloque-cuerpo">
        <header className="bloque-cabecera bloque-cabecera-accion">
          <div>
            <h2 id={id}>Servicios adicionales <span className="suave">({servicios.length})</span></h2>
            <p className="suave">Los servicios activos se pueden cargar como consumos. Cada cambio se guarda al confirmarlo.</p>
          </div>
          <button type="button" className="boton" onClick={() => { setHecho(null); setAbierto({ tipo: 'nuevo' }) }}>
            <span aria-hidden="true">+</span> Nuevo servicio
          </button>
        </header>
        <p role="status" className="bloque-hecho">
          {hecho && <><span className="bloque-sello bloque-sello-guardado" aria-hidden="true">✓</span>{hecho}</>}
        </p>
        {servicios.length === 0 ? (
          <p className="bloque-vacio-texto suave">Todavía no hay servicios. Crea el primero con «Nuevo servicio».</p>
        ) : (
          <ul className="servicios" aria-label="Servicios del alojamiento">
            <li className="servicios-cabecera" aria-hidden="true">
              <span>Nombre</span><span>Cobro</span><span className="derecha">Valor</span><span>Estado</span><span />
            </li>
            {servicios.map((servicio) => (
              <li key={servicio.id} className={servicio.activo ? 'servicio' : 'servicio servicio-inactivo'}>
                <span className="servicio-nombre">{servicio.nombre}</span>
                <span><Cobro servicio={servicio} /></span>
                <span className="servicio-valor importe">
                  {servicio.generaCargo ? dinero(servicio.valor) : <span className="suave" aria-label="Sin valor">–</span>}
                </span>
                <span><EstadoServicio servicio={servicio} /></span>
                <span className="servicio-acciones">
                  {servicio.activo && servicio.generaCargo && (
                    <button type="button" className="boton boton-secundario boton-chico" aria-label={`Cambiar valor: ${servicio.nombre}`}
                      onClick={() => { setHecho(null); setAbierto({ tipo: 'valor', servicio }) }}>
                      Cambiar valor
                    </button>
                  )}
                  {servicio.activo && (
                    <button type="button" className="boton boton-peligro-borde boton-chico" aria-label={`Desactivar: ${servicio.nombre}`}
                      onClick={() => { setHecho(null); setAbierto({ tipo: 'desactivar', servicio }) }}>
                      Desactivar
                    </button>
                  )}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {abierto?.tipo === 'nuevo' && (
        <DialogoNuevo alCerrar={() => setAbierto(null)}
          alCrear={(nuevo, nombre) => terminar(nuevo, `${nombre} creado`)} />
      )}
      {abierto?.tipo === 'valor' && (
        <DialogoValor servicio={abierto.servicio} alCerrar={() => setAbierto(null)}
          alGuardar={(nuevo) => terminar(nuevo, `Valor de ${abierto.servicio.nombre} guardado`)} />
      )}
      {abierto?.tipo === 'desactivar' && (
        <DialogoDesactivar servicio={abierto.servicio} alCerrar={() => setAbierto(null)}
          alDesactivar={(nuevo) => terminar(nuevo, `${abierto.servicio.nombre} desactivado`)} />
      )}
    </section>
  )
}

/** Envía y, si la API rechaza, deja el mensaje en el diálogo. */
function useEnvio() {
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)
  async function enviar(accion: () => Promise<void>) {
    setEnviando(true)
    setError(null)
    try {
      await accion()
    } catch (causa) {
      setError(errorParaMostrar(causa))
      setEnviando(false)
    }
  }
  return { enviando, error, enviar }
}

function DialogoNuevo({ alCerrar, alCrear }: { alCerrar: () => void; alCrear: (nuevo: Alojamiento, nombre: string) => void }) {
  const [nombre, setNombre] = useState('')
  const [seCobra, setSeCobra] = useState(true)
  const [valor, setValor] = useState('')
  const [errores, setErrores] = useState<{ nombre?: string; valor?: string }>({})
  const { enviando, error, enviar } = useEnvio()

  function crear(evento: FormEvent) {
    evento.preventDefault()
    const numero = seCobra ? valorServicio(valor) : 0
    const encontrados = {
      nombre: !nombre.trim() ? 'Escribe el nombre del servicio.' : nombre.length > 120 ? 'El nombre admite hasta 120 caracteres.' : undefined,
      valor: numero === null ? 'Escribe el valor en pesos, sin decimales.' : undefined,
    }
    setErrores(encontrados)
    if (encontrados.nombre || encontrados.valor) return
    void enviar(async () => alCrear(await agregarServicio({ nombre: nombre.trim(), generaCargo: seCobra, valor: numero ?? 0 }), nombre.trim()))
  }

  return (
    <Dialogo titulo="Nuevo servicio" ocupado={enviando} alCerrar={alCerrar}>
      <form className="dialogo-contenido" onSubmit={crear} noValidate>
        <div className="dialogo-cuerpo formulario-dialogo">
          {error && <Aviso tipo="error" asunto="No se creó el servicio" mensaje={error.mensaje} detalles={error.detalles} />}
          <CampoBloque etiqueta="Nombre" valor={nombre} maximo={120} error={errores.nombre} data-foco-inicial
            alCambiar={(texto) => { setNombre(texto); setErrores({ ...errores, nombre: undefined }) }} />
          <Opciones pregunta="Se cobra" pista="Si eliges No, aparece como «Incluido» y no tiene valor."
            valor={seCobra ? 'si' : 'no'} opciones={[['si', 'Sí'], ['no', 'No']]} alCambiar={(opcion) => setSeCobra(opcion === 'si')} />
          {seCobra && (
            <CampoBloque etiqueta="Valor" antes="$" inputMode="numeric" valor={valor} error={errores.valor}
              pista="Lo que se carga al folio cada vez que se registra el consumo."
              alCambiar={(texto) => { setValor(texto); setErrores({ ...errores, valor: undefined }) }} />
          )}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alCerrar}>Volver</button>
          <button type="submit" className="boton" disabled={enviando}>{enviando ? 'Creando…' : 'Crear servicio'}</button>
        </div>
      </form>
    </Dialogo>
  )
}

function DialogoValor({ servicio, alCerrar, alGuardar }: {
  servicio: ServicioAdicional
  alCerrar: () => void
  alGuardar: (nuevo: Alojamiento) => void
}) {
  const [valor, setValor] = useState(textoPesos(servicio.valor))
  const [errorCampo, setErrorCampo] = useState<string>()
  const { enviando, error, enviar } = useEnvio()

  function guardar(evento: FormEvent) {
    evento.preventDefault()
    const numero = valorServicio(valor)
    if (numero === null) {
      setErrorCampo('Escribe el valor en pesos, sin decimales.')
      return
    }
    void enviar(async () => alGuardar(await cambiarValorServicio(servicio.id, numero)))
  }

  return (
    <Dialogo titulo="Cambiar valor" subtitulo={servicio.nombre} ocupado={enviando} alCerrar={alCerrar}>
      <form className="dialogo-contenido" onSubmit={guardar} noValidate>
        <div className="dialogo-cuerpo formulario-dialogo">
          {error && <Aviso tipo="error" asunto="No se cambió el valor" mensaje={error.mensaje} detalles={error.detalles} />}
          <CampoBloque etiqueta="Valor" antes="$" inputMode="numeric" valor={valor} error={errorCampo} data-foco-inicial
            pista="Aplica a los consumos nuevos. Lo ya cargado no cambia."
            alCambiar={(texto) => { setValor(texto); setErrorCampo(undefined) }} />
          <p className="renglon-valor">
            <span className="suave">Valor actual</span>
            <span className="importe">{dinero(servicio.valor)}</span>
          </p>
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alCerrar}>Volver</button>
          <button type="submit" className="boton" disabled={enviando}>{enviando ? 'Guardando…' : 'Guardar valor'}</button>
        </div>
      </form>
    </Dialogo>
  )
}

function DialogoDesactivar({ servicio, alCerrar, alDesactivar }: {
  servicio: ServicioAdicional
  alCerrar: () => void
  alDesactivar: (nuevo: Alojamiento) => void
}) {
  const { enviando, error, enviar } = useEnvio()
  return (
    <Dialogo titulo={`¿Desactivar ${servicio.nombre}?`} ocupado={enviando} alCerrar={alCerrar}>
      <div className="dialogo-contenido">
        <div className="dialogo-cuerpo">
          {error && <Aviso tipo="error" asunto={`No se desactivó ${servicio.nombre}`} mensaje={error.mensaje} detalles={error.detalles} />}
          <p className="dialogo-texto">
            Deja de ofrecerse en los consumos nuevos. Lo ya cargado no cambia. No se puede volver a activar.
          </p>
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" data-foco-inicial disabled={enviando} onClick={alCerrar}>Volver</button>
          <button type="button" className="boton boton-peligro" disabled={enviando}
            onClick={() => void enviar(async () => alDesactivar(await desactivarServicio(servicio.id)))}>
            {enviando ? 'Desactivando…' : 'Sí, desactivar'}
          </button>
        </div>
      </div>
    </Dialogo>
  )
}
