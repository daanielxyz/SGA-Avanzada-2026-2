import { useState } from 'react'
import type { ApartamentoPublico, Grupo, Precio } from '../api/apartamentos'
import { enPesos, fechaCorta } from '../formato'

interface Props {
  apartamento: ApartamentoPublico
  grupo: Grupo
}

const SERVICIOS_A_LA_VISTA = 3

/**
 * Un apartamento libre, compacto para ver varios a la vez: foto, lo que ofrece, lo que cuesta un adulto la noche de
 * llegada (con la temporada de esa noche) y el total del grupo, con el detalle noche por noche si lo pide.
 */
export default function TarjetaApartamento({ apartamento, grupo }: Props) {
  const foto = apartamento.imagenes.find((i) => i.principal) ?? apartamento.imagenes[0]
  const servicios = apartamento.caracteristicas
  const id = `nombre-${apartamento.codigo}`

  return (
    <article className="tarjeta" aria-labelledby={id}>
      {foto && <img src={foto.url} alt={`Foto de ${apartamento.nombre}`} loading="lazy" />}
      <div className="tarjeta-cuerpo">
        <h3 id={id}>{apartamento.nombre}</h3>
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
                +{servicios.length - SERVICIOS_A_LA_VISTA} más
              </li>
            )}
          </ul>
        )}
        {apartamento.precio ? (
          <PrecioDelGrupo precio={apartamento.precio} grupo={grupo} />
        ) : (
          <p className="suave">Precio por confirmar</p>
        )}
      </div>
    </article>
  )
}

function PrecioDelGrupo({ precio, grupo }: { precio: Precio; grupo: Grupo }) {
  const [viendoDetalle, setViendoDetalle] = useState(false)
  const llegada = precio.desglose[0]
  const n = precio.desglose.length

  return (
    <div className="precio">
      <p className="por-noche">
        <strong>{enPesos(llegada.tarifa)}</strong> por adulto, noche del {fechaCorta(llegada.noche)}
      </p>
      <p>
        Total {n} {n === 1 ? 'noche' : 'noches'}, {grupo.adultos} {grupo.adultos === 1 ? 'adulto' : 'adultos'}
        {grupo.ninos > 0 && ` y ${grupo.ninos} ${grupo.ninos === 1 ? 'niño' : 'niños'}`}:{' '}
        <strong>{enPesos(precio.total)}</strong>
      </p>
      <button type="button" className="secundario" aria-expanded={viendoDetalle}
        onClick={() => setViendoDetalle((v) => !v)}>
        {viendoDetalle ? 'Ocultar detalle' : 'Ver detalle por noche'}
      </button>
      {viendoDetalle && (
        <table>
          <caption>Precio noche por noche</caption>
          <thead>
            <tr>
              <th scope="col">Noche</th>
              <th scope="col">Por adulto</th>
              <th scope="col">Pagan</th>
              <th scope="col">Subtotal</th>
            </tr>
          </thead>
          <tbody>
            {precio.desglose.map((noche) => (
              <tr key={noche.noche}>
                <td>{fechaCorta(noche.noche)}</td>
                <td>{enPesos(noche.tarifa)}</td>
                <td>{noche.ocupantesFacturables}</td>
                <td>{enPesos(noche.subtotal)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row" colSpan={3}>
                Total
              </th>
              <td>{enPesos(precio.total)}</td>
            </tr>
          </tfoot>
        </table>
      )}
    </div>
  )
}
