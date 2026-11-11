import type { ReactNode } from 'react'
import type { Cargo, Folio, Pago } from '../api/folio'
import type { EstadoReserva } from '../api/reservas'
import { dinero, fechaCorta, nombreLegible } from '../formato'
import {
  accionesDelCargo,
  cargosNetos,
  conceptoVisible,
  filasDeCargos,
  filasDePagos,
  nombreDelCargo,
  puedeRevertirPago,
  tipoDelCargo,
  tipoDelPago,
  montoConSigno,
  valorConSigno,
  type Fila,
} from './movimientos'

/** «$ 45.000» o «− $ 45.000». */
function conSigno(valor: number): string {
  return valor < 0 ? `− ${dinero(-valor)}` : dinero(valor)
}

/** «por Andrea Ruiz»: solo en correcciones, reversos y devoluciones (DEC-100 · DEC-103). */
function Autor({ nombre }: { nombre?: string | null }) {
  return nombre ? <span className="movimiento-autor">por {nombre}</span> : null
}

function EtiquetaRevertido() {
  return (
    <span className="etiqueta etiqueta-revertido">
      <span aria-hidden="true">✕</span> Revertido
    </span>
  )
}

/** Una fila: tachada si la revirtieron; si es el reverso, con «↳» y pegada a la de arriba. */
function FilaMovimiento({ clase, fecha, concepto, tipo, valor, acciones }: {
  clase: 'normal' | 'revertida' | 'reverso'
  fecha: string
  concepto: ReactNode
  tipo: string
  valor: number
  acciones?: ReactNode
}) {
  const tachado = clase === 'revertida'
  return (
    <tr className={`movimiento movimiento-${clase}`}>
      <td className="movimiento-fecha">{fechaCorta(fecha)}</td>
      <td className="movimiento-concepto">
        {clase === 'reverso' && <span className="movimiento-flecha" aria-hidden="true">↳ </span>}
        {concepto}
      </td>
      <td className="movimiento-tipo">{tipo}</td>
      <td className="movimiento-valor codigo">
        {tachado ? <s>{conSigno(valor)}</s> : conSigno(valor)}
      </td>
      <td className="movimiento-acciones">{acciones}</td>
    </tr>
  )
}

function Tabla({ id, titulo, columnas, total, children }: {
  id: string
  titulo: string
  columnas: [string, string, string]
  total: { nombre: string; valor: number }
  children: ReactNode
}) {
  return (
    <section aria-labelledby={id} className="detalle-seccion">
      <h2 id={id}>{titulo}</h2>
      <div className="tarjeta tabla-marco movimientos">
        <table className="tabla tabla-movimientos" aria-labelledby={id}>
          <thead>
            <tr>
              <th scope="col">Fecha</th>
              <th scope="col">{columnas[0]}</th>
              <th scope="col">{columnas[1]}</th>
              <th scope="col" className="derecha">{columnas[2]}</th>
              <th scope="col"><span className="solo-lectores">Acciones</span></th>
            </tr>
          </thead>
          <tbody>{children}</tbody>
        </table>
        <p className="movimientos-total">
          <span>{total.nombre}</span>
          <strong className="codigo">{dinero(total.valor)}</strong>
        </p>
      </div>
    </section>
  )
}

/** Los cargos del folio, cada uno con su corrección debajo (RN-16), y su suma neta. */
export function TablaCargos({ folio, estado, alCorregir, alRevertir }: {
  folio: Folio
  estado: EstadoReserva
  alCorregir: (cargo: Cargo) => void
  alRevertir: (cargo: Cargo) => void
}) {
  const filas = filasDeCargos(folio.cargos)
  return (
    <Tabla id="titulo-cargos" titulo="Cargos" columnas={['Concepto', 'Tipo', 'Valor']} total={{ nombre: 'Cargos netos', valor: cargosNetos(folio) }}>
      {filas.map((fila) => (
        <FilasDeCargo key={fila.movimiento.id} fila={fila} folio={folio} estado={estado} alCorregir={alCorregir} alRevertir={alRevertir} />
      ))}
    </Tabla>
  )
}

function FilasDeCargo({ fila, folio, estado, alCorregir, alRevertir }: {
  fila: Fila<Cargo>
  folio: Folio
  estado: EstadoReserva
  alCorregir: (cargo: Cargo) => void
  alRevertir: (cargo: Cargo) => void
}) {
  const { movimiento: cargo, reverso } = fila
  const nombre = conceptoVisible(cargo, folio.cargos)
  const etiqueta = `${nombreDelCargo(cargo)} del ${fechaCorta(cargo.fecha)}`
  const acciones = accionesDelCargo(fila, estado, folio)
  return (
    <>
      <FilaMovimiento
        clase={reverso ? 'revertida' : 'normal'}
        fecha={cargo.fecha}
        concepto={
          <>
            {reverso ? <s>{nombre}</s> : nombre}
            {reverso && <> <EtiquetaRevertido /></>}
            {cargo.corrigeA && <> <Autor nombre={cargo.autorNombre} /></>}
          </>
        }
        tipo={tipoDelCargo(cargo)}
        valor={valorConSigno(cargo)}
        acciones={(acciones.corregir || acciones.revertir) && (
          <>
            {acciones.corregir && (
              <button type="button" className="boton boton-secundario boton-chico" aria-label={`Corregir ${etiqueta}`} onClick={() => alCorregir(cargo)}>
                Corregir
              </button>
            )}
            {acciones.revertir && (
              <button type="button" className="boton boton-secundario boton-chico" aria-label={`Revertir ${etiqueta}`} onClick={() => alRevertir(cargo)}>
                Revertir
              </button>
            )}
          </>
        )}
      />
      {reverso && (
        <FilaMovimiento
          clase="reverso"
          fecha={reverso.fecha}
          concepto={<>{conceptoVisible(reverso, folio.cargos)} <Autor nombre={reverso.autorNombre} /></>}
          tipo={tipoDelCargo(reverso)}
          valor={valorConSigno(reverso)}
        />
      )}
    </>
  )
}

/** Los pagos del folio, cada abono revertido con su reverso debajo; las devoluciones restan. */
export function TablaPagos({ folio, alRevertir }: { folio: Folio; alRevertir: (pago: Pago) => void }) {
  const filas = filasDePagos(folio.pagos ?? [])
  return (
    <Tabla id="titulo-pagos" titulo="Pagos" columnas={['Medio', 'Tipo', 'Monto']} total={{ nombre: 'Total pagado', valor: folio.totalPagado }}>
      {filas.length === 0 && (
        <tr className="movimiento-vacio">
          <td colSpan={5} className="suave">Aún no hay pagos registrados.</td>
        </tr>
      )}
      {filas.map((fila) => {
        const { movimiento: pago, reverso } = fila
        const medio = nombreLegible(pago.medio)
        const devolucion = pago.tipo === 'REVERSO'
        return (
          <FilasDePago
            key={pago.id}
            pago={pago}
            reverso={reverso}
            medio={medio}
            devolucion={devolucion}
            revertible={puedeRevertirPago(fila, folio)}
            alRevertir={alRevertir}
          />
        )
      })}
    </Tabla>
  )
}

function FilasDePago({ pago, reverso, medio, devolucion, revertible, alRevertir }: {
  pago: Pago
  reverso: Pago | null
  medio: string
  devolucion: boolean
  revertible: boolean
  alRevertir: (pago: Pago) => void
}) {
  return (
    <>
      <FilaMovimiento
        clase={reverso ? 'revertida' : 'normal'}
        fecha={pago.fecha}
        concepto={
          <>
            {reverso ? <s>{medio}</s> : medio}
            {reverso && <> <EtiquetaRevertido /></>}
            {devolucion && <> <Autor nombre={pago.autorNombre} /></>}
          </>
        }
        tipo={tipoDelPago(pago)}
        valor={montoConSigno(pago)}
        acciones={revertible && (
          <button
            type="button"
            className="boton boton-secundario boton-chico"
            aria-label={`Revertir ${medio} del ${fechaCorta(pago.fecha)}`}
            onClick={() => alRevertir(pago)}
          >
            Revertir
          </button>
        )}
      />
      {reverso && (
        <FilaMovimiento
          clase="reverso"
          fecha={reverso.fecha}
          concepto={<>{nombreLegible(reverso.medio)} <Autor nombre={reverso.autorNombre} /></>}
          tipo={tipoDelPago(reverso)}
          valor={montoConSigno(reverso)}
        />
      )}
    </>
  )
}
