import { useState } from 'react'
import type { ApartamentoPublico, Estadia } from '../api/apartamentos'
import Cotizador from './Cotizador'

interface Props {
  apartamento: ApartamentoPublico
  estadia: Estadia
  ocupantes: number
}

/** Un apartamento libre: su foto principal, lo que ofrece y el botón para ver el precio. */
export default function TarjetaApartamento({ apartamento, estadia, ocupantes }: Props) {
  const [viendoPrecio, setViendoPrecio] = useState(false)
  const foto = apartamento.imagenes.find((i) => i.principal) ?? apartamento.imagenes[0]

  return (
    <article className="tarjeta" aria-labelledby={`nombre-${apartamento.codigo}`}>
      {foto && <img src={foto.url} alt={`Foto de ${apartamento.nombre}`} loading="lazy" />}
      <div className="tarjeta-cuerpo">
        <h3 id={`nombre-${apartamento.codigo}`}>{apartamento.nombre}</h3>
        <p className="suave">
          Hasta {apartamento.capacidad} {apartamento.capacidad === 1 ? 'persona' : 'personas'} ·{' '}
          {apartamento.dormitorios} {apartamento.dormitorios === 1 ? 'dormitorio' : 'dormitorios'}
        </p>
        {apartamento.descripcion && <p>{apartamento.descripcion}</p>}
        {apartamento.caracteristicas.length > 0 && (
          <ul className="dotacion" aria-label="Dotación">
            {apartamento.caracteristicas.map((c) => (
              <li key={c}>{c}</li>
            ))}
          </ul>
        )}
        <button type="button" aria-expanded={viendoPrecio} onClick={() => setViendoPrecio((v) => !v)}>
          {viendoPrecio ? 'Ocultar precio' : 'Ver precio'}
        </button>
        {viendoPrecio && <Cotizador apartamento={apartamento.codigo} estadia={estadia} ocupantes={ocupantes} />}
      </div>
    </article>
  )
}
