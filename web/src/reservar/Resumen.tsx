import type { ApartamentoPublico, Estadia, Grupo, Precio } from '../api/apartamentos'
import Foto from '../busqueda/Foto'
import { enPesos, fechaCorta, noches } from '../formato'

interface Props {
  apartamento: ApartamentoPublico
  estadia: Estadia
  grupo: Grupo
  /** Vacío mientras llega. */
  precio: Precio | null
}

/** Lo que se está reservando, siempre a la vista durante los pasos. */
export default function Resumen({ apartamento, estadia, grupo, precio }: Props) {
  const foto = apartamento.imagenes.find((i) => i.principal) ?? apartamento.imagenes[0]
  const n = noches(estadia.entrada, estadia.salida)
  const ninos = grupo.ninos > 0 ? ` y ${grupo.ninos} ${grupo.ninos === 1 ? 'niño' : 'niños'}` : ''

  return (
    <aside className="panel-reserva resumen" aria-labelledby="titulo-resumen">
      <div className="resumen-apartamento">
        <Foto url={foto?.url} alt="" />
        <h2 id="titulo-resumen">{apartamento.nombre}</h2>
      </div>
      <dl>
        <dt>Llegada</dt>
        <dd>{fechaCorta(estadia.entrada)}</dd>
        <dt>Salida</dt>
        <dd>{fechaCorta(estadia.salida)}</dd>
        <dt>Viajan</dt>
        <dd>
          {grupo.adultos} {grupo.adultos === 1 ? 'adulto' : 'adultos'}
          {ninos}
        </dd>
      </dl>
      <p className="total-estancia">
        Total {n} {n === 1 ? 'noche' : 'noches'}: <strong>{precio ? enPesos(precio.total) : '…'}</strong>
      </p>
    </aside>
  )
}
