import { useState, type FormEvent } from 'react'
import type { ServicioAdicional } from '../api/alojamiento'
import { revertirCargo, type Cargo, type Folio } from '../api/folio'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import { dinero, fechaCorta, fechaHoy } from '../formato'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'
import CampoMotivo from './CampoMotivo'
import FormularioConsumo from './FormularioConsumo'
import { esConsumo, nombreDelCargo, servicioDelCargo, tipoDelCargo } from './movimientos'

const MOTIVO_MAXIMO = 170

/** Cargar un consumo del catálogo (CAR-05 · BL-12). */
export function DialogoConsumo({ codigo, servicios, alCerrar, alRegistrar }: {
  codigo: string
  servicios: ServicioAdicional[]
  alCerrar: () => void
  alRegistrar: (folio: Folio) => void
}) {
  const [enviando, setEnviando] = useState(false)
  const hoy = fechaCorta(fechaHoy())
  return (
    <Dialogo
      titulo="Cargar consumo"
      subtitulo={<><span className="codigo">{codigo}</span> · Se carga con fecha de hoy, {hoy}</>}
      ocupado={enviando}
      alCerrar={alCerrar}
    >
      <FormularioConsumo
        codigo={codigo}
        servicios={servicios}
        textoVolver="Volver"
        alVolver={alCerrar}
        alEnviar={setEnviando}
        alRegistrar={alRegistrar}
      />
    </Dialogo>
  )
}

/** Qué cargo se va a tocar, para no equivocarse de fila. */
function ResumenCargo({ cargo }: { cargo: Cargo }) {
  return (
    <dl className="resumen-dialogo">
      <dt>Concepto</dt>
      <dd>{nombreDelCargo(cargo)}</dd>
      <dt>Fecha</dt>
      <dd>{fechaCorta(cargo.fecha)}</dd>
      <dt>Tipo</dt>
      <dd>{tipoDelCargo(cargo)}</dd>
      <dt>Valor</dt>
      <dd className="codigo">{dinero(cargo.valor)}</dd>
    </dl>
  )
}

/**
 * Motivo y envío del reverso de un cargo (RN-16 · CAR-03): el cargo no se borra, queda tachado junto a su corrección.
 * El autor lo pone el backend con la sesión (DEC-100).
 */
function FormularioRevertir({ codigo, cargo, textoBoton, alVolver, alRevertir, alEnviar }: {
  codigo: string
  cargo: Cargo
  textoBoton: string
  alVolver: () => void
  alRevertir: (folio: Folio) => void
  alEnviar: (enviando: boolean) => void
}) {
  const [motivo, setMotivo] = useState('')
  const [faltaMotivo, setFaltaMotivo] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<ErrorMostrado | null>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const texto = motivo.trim()
    setFaltaMotivo(!texto)
    setError(null)
    if (!texto) return
    setEnviando(true)
    alEnviar(true)
    try {
      alRevertir(await revertirCargo(codigo, cargo.id, texto))
    } catch (causa) {
      setError(errorParaMostrar(causa))
    } finally {
      setEnviando(false)
      alEnviar(false)
    }
  }

  return (
    <form className="dialogo-contenido" onSubmit={enviar} noValidate>
      <div className="dialogo-cuerpo">
        {error && <Aviso tipo="error" asunto="No se revirtió" mensaje={error.mensaje} detalles={error.detalles} />}
        <ResumenCargo cargo={cargo} />
        {!esConsumo(cargo) && (
          <Aviso
            tipo="advertencia"
            asunto="Desde el folio no se puede volver a cargar"
            mensaje="Si el valor cambió, modifica la reserva."
          />
        )}
        <p className="dialogo-texto">El cargo no se borra: queda tachado junto a su corrección.</p>
        <CampoMotivo
          id="revertir-motivo"
          rotulo="Motivo"
          valor={motivo}
          maximo={MOTIVO_MAXIMO}
          focoInicial
          deshabilitado={enviando}
          error={faltaMotivo ? 'Escribe el motivo.' : undefined}
          alCambiar={setMotivo}
        />
        {enviando && <p className="suave" role="status">Revirtiendo el cargo…</p>}
      </div>
      <div className="dialogo-pie">
        <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alVolver}>
          Volver
        </button>
        <button type="submit" className="boton boton-peligro" disabled={enviando}>
          {enviando ? 'Revirtiendo…' : textoBoton}
        </button>
      </div>
    </form>
  )
}

/** Revertir un cargo, con motivo obligatorio. */
export function DialogoRevertirCargo({ codigo, cargo, alCerrar, alRevertir }: {
  codigo: string
  cargo: Cargo
  alCerrar: () => void
  alRevertir: (folio: Folio) => void
}) {
  const [enviando, setEnviando] = useState(false)
  return (
    <Dialogo titulo="Revertir cargo" subtitulo={<span className="codigo">{codigo}</span>} ocupado={enviando} alCerrar={alCerrar}>
      <FormularioRevertir
        codigo={codigo}
        cargo={cargo}
        textoBoton="Revertir cargo"
        alVolver={alCerrar}
        alEnviar={setEnviando}
        alRevertir={alRevertir}
      />
    </Dialogo>
  )
}

/**
 * Corregir un consumo en dos pasos: revertirlo y cargar el correcto, ya propuesto con el mismo servicio y cantidad
 * (DEC-107: un cargo se corrige entero). Son dos llamadas: si se sale después de revertir, el cargo queda revertido y
 * se avisa antes.
 *
 * @param alCambiar el folio cambió (en cada paso)
 */
export function DialogoCorregir({ codigo, cargo, servicios, alCerrar, alCambiar, alTerminar }: {
  codigo: string
  cargo: Cargo
  servicios: ServicioAdicional[]
  alCerrar: () => void
  alCambiar: (folio: Folio) => void
  alTerminar: (folio: Folio) => void
}) {
  const [paso, setPaso] = useState<1 | 2>(1)
  const [enviando, setEnviando] = useState(false)
  const [confirmarSalida, setConfirmarSalida] = useState(false)
  const servicio = servicioDelCargo(cargo, servicios)
  const nombre = nombreDelCargo(cargo)

  if (paso === 1) {
    return (
      <Dialogo
        titulo="Corregir consumo"
        subtitulo={<><span className="codigo">{codigo}</span> · Paso 1 de 2: revertir el cargo actual</>}
        ocupado={enviando}
        alCerrar={alCerrar}
      >
        <FormularioRevertir
          codigo={codigo}
          cargo={cargo}
          textoBoton="Revertir y continuar"
          alVolver={alCerrar}
          alEnviar={setEnviando}
          alRevertir={(folio) => {
            alCambiar(folio)
            setPaso(2)
          }}
        />
      </Dialogo>
    )
  }

  // Cerrar la ventana en el paso 2 primero avisa: el reverso ya quedó hecho
  const pedirSalida = () => setConfirmarSalida(true)
  return (
    <Dialogo
      titulo="Corregir consumo · cargar el correcto"
      subtitulo={<><span className="codigo">{codigo}</span> · Paso 2 de 2</>}
      ocupado={enviando}
      alCerrar={pedirSalida}
    >
      <FormularioConsumo
        codigo={codigo}
        servicios={servicios}
        inicial={{ servicioId: servicio?.id ?? null, cantidad: cargo.cantidad ?? 1 }}
        textoVolver="Salir"
        alVolver={pedirSalida}
        alEnviar={setEnviando}
        alRegistrar={alTerminar}
        encabezado={
          <>
            {confirmarSalida ? (
              <Aviso
                tipo="advertencia"
                asunto="El cargo ya quedó revertido"
                mensaje="Si sales, no se cargará el nuevo."
                accion={
                  <div className="aviso-acciones">
                    <button type="button" className="boton boton-secundario boton-chico" onClick={alCerrar}>
                      Salir sin cargar
                    </button>
                    <button type="button" className="boton boton-chico" onClick={() => setConfirmarSalida(false)}>
                      Seguir aquí
                    </button>
                  </div>
                }
              />
            ) : (
              <Aviso tipo="exito" asunto={`Ya se revirtió ${nombre}`} mensaje="Ahora carga el consumo correcto." />
            )}
            {!servicio && (
              <Aviso
                tipo="advertencia"
                asunto={`${cargo.concepto} ya no está en el catálogo`}
                mensaje="Elige otro consumo para cargar."
              />
            )}
          </>
        }
      />
    </Dialogo>
  )
}
