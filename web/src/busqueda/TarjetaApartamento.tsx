import { Link } from 'react-router'
import type { ApartamentoPublico, Estadia, Grupo } from '../api/apartamentos'
import { enPesos, fechaCorta, noches } from '../formato'
import Foto from './Foto'

interface Props {
  apartamento: ApartamentoPublico
  /** Con la búsqueda por fechas, el precio es el del grupo; sin ella, el de un adulto su próxima noche libre. */
  busqueda?: { estadia: Estadia; grupo: Grupo }
}

const SERVICIOS_A_LA_VISTA = 4

/**
 * Un apartamento en una fila: la foto a la izquierda y el precio en su columna a la derecha, para comparar de un
 * vistazo (en el celular, el precio va abajo). Toda la fila lleva a su página, con las fechas y el grupo ya puestos:
 * los de la búsqueda o, sin ella, la noche del precio y un adulto.
 */
export default function TarjetaApartamento({ apartamento, busqueda }: Props) {
  const foto = apartamento.imagenes.find((i) => i.principal) ?? apartamento.imagenes[0]
  const servicios = apartamento.caracteristicas
  const id = `nombre-${apartamento.codigo}`

  return (
    <article className="tarjeta" aria-labelledby={id}>
      <Foto url={foto?.url} alt={`Foto de ${apartamento.nombre}`} />
      <div className="tarjeta-cuerpo">
        <h3 id={id}>
          <Link to={enlace(apartamento, busqueda)} className="enlace-tarjeta" title={apartamento.nombre}>
            {apartamento.nombre}
          </Link>
        </h3>
        <p className="suave">
          Hasta {apartamento.capacidad} {apartamento.capacidad === 1 ? 'persona' : 'personas'} ·{' '}
          {apartamento.dormitorios} {apartamento.dormitorios === 1 ? 'dormitorio' : 'dormitorios'}
        </p>
        {apartamento.descripcion && <p className="descripcion">{apartamento.descripcion}</p>}
        {servicios.length > 0 && (
          <ul className="dotacion" aria-label="Dotación">
            {servicios.slice(0, SERVICIOS_A_LA_VISTA).map((c) => (
              <li key={c}>{c}</li>
            ))}
            {servicios.length > SERVICIOS_A_LA_VISTA && (
              <li title={servicios.slice(SERVICIOS_A_LA_VISTA).join(', ')}>
                +{servicios.length - SERVICIOS_A_LA_VISTA}
              </li>
            )}
          </ul>
        )}
      </div>
      <PrecioDeLaTarjeta apartamento={apartamento} busqueda={busqueda} />
    </article>
  )
}

function PrecioDeLaTarjeta({ apartamento: { precio }, busqueda }: Props) {
  if (!precio) {
    return (
      <div className="tarjeta-precio">
        <span className="suave">Sin fechas libres por ahora</span>
      </div>
    )
  }
  const llegada = precio.desglose[0]
  if (busqueda) {
    return (
      <div className="tarjeta-precio">
        <strong>{enPesos(precio.total)}</strong>
        <span className="suave">{grupoYNoches(busqueda)}</span>
        <span className="suave">{enPesos(llegada.tarifa)} por adulto y noche</span>
      </div>
    )
  }
  return (
    <div className="tarjeta-precio">
      <strong>{enPesos(llegada.tarifa)}</strong>
      <span className="suave">por adulto y noche</span>
      <span className="suave">noche del {fechaCorta(llegada.noche)}</span>
    </div>
  )
}

function grupoYNoches({ estadia, grupo }: NonNullable<Props['busqueda']>) {
  const n = noches(estadia.entrada, estadia.salida)
  const ninos = grupo.ninos > 0 ? ` y ${grupo.ninos} ${grupo.ninos === 1 ? 'niño' : 'niños'}` : ''
  return `Total ${n} ${n === 1 ? 'noche' : 'noches'}, ${grupo.adultos} ${grupo.adultos === 1 ? 'adulto' : 'adultos'}`
    + ninos
}

// La página del apartamento abre con lo que se ve en la tarjeta
function enlace({ codigo, precio }: ApartamentoPublico, busqueda: Props['busqueda']) {
  const noche = precio?.desglose[0]?.noche
  const parametros = busqueda
    ? new URLSearchParams({ ...busqueda.estadia, adultos: String(busqueda.grupo.adultos),
      ninos: String(busqueda.grupo.ninos) })
    : new URLSearchParams(noche ? { entrada: noche } : {})
  const consulta = parametros.toString()
  return `/apartamentos/${encodeURIComponent(codigo)}${consulta ? `?${consulta}` : ''}`
}
