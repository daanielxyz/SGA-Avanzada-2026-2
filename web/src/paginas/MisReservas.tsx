import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { consultarApartamento, type ApartamentoPublico, type Pagina } from '../api/apartamentos'
import { explicar } from '../api/mensajes'
import { ESTADOS, listarMisReservas, type ReservaDelHuesped } from '../api/cuenta'
import Foto from '../busqueda/Foto'
import Paginacion from '../busqueda/Paginacion'
import { enPesos, fechaCorta } from '../formato'

/**
 * Las reservas del huésped de 10 en 10 (DEC-18), en dos pestañas (DEC-108): «Próximas», lo que busca al entrar, de la
 * próxima llegada en adelante; y «Anteriores», las terminadas o canceladas, de la más reciente a la más antigua.
 */
export default function MisReservas() {
  const [consulta, setConsulta] = useSearchParams()
  const numero = Math.max(0, Number(consulta.get('pagina')) || 0)
  const anteriores = consulta.get('ver') === 'anteriores'
  const [pagina, setPagina] = useState<Pagina<ReservaDelHuesped> | null>(null)
  const [apartamentos, setApartamentos] = useState<Map<string, ApartamentoPublico>>(new Map())
  const [problema, setProblema] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true
    listarMisReservas(!anteriores, numero)
      .then(async (p) => {
        if (!vigente) return
        setProblema(null)
        setPagina(p)
        // El nombre y la foto son un adorno: si alguno no carga, esa reserva se muestra con su código
        const codigos = [...new Set(p.contenido.map((r) => r.apartamento))]
        const respuestas = await Promise.allSettled(codigos.map(consultarApartamento))
        if (vigente) {
          setApartamentos(new Map(respuestas.flatMap((r) => (r.status === 'fulfilled'
            ? [[r.value.codigo, r.value] as const] : []))))
        }
      })
      .catch((error: unknown) => vigente && setProblema(
        explicar(error, 'No pudimos abrir tus reservas. Intenta de nuevo en un momento.')))
    return () => {
      vigente = false
    }
  }, [numero, anteriores])

  return (
    <main className="ficha mis-reservas">
      <h1>Mis reservas</h1>
      <nav className="pestanas" aria-label="Qué reservas ver">
        <Link to="/mis-reservas" aria-current={anteriores ? undefined : 'page'}>Próximas</Link>
        <Link to="/mis-reservas?ver=anteriores" aria-current={anteriores ? 'page' : undefined}>Anteriores</Link>
      </nav>
      {problema && <p role="alert" className="aviso">{problema}</p>}
      {!pagina && !problema && <p className="suave">Cargando tus reservas…</p>}
      {pagina?.contenido.length === 0 && (
        <div className="vacio">
          {anteriores ? <p>No tienes reservas anteriores.</p> : (
            <>
              <p>No tienes reservas próximas.</p>
              <Link to="/" className="boton">Ver apartamentos</Link>
            </>
          )}
        </div>
      )}
      {pagina && pagina.contenido.length > 0 && (
        <>
          <ul className="lista-reservas">
            {pagina.contenido.map((reserva) => (
              <FilaReserva key={reserva.codigo} reserva={reserva} apartamento={apartamentos.get(reserva.apartamento)} />
            ))}
          </ul>
          <Paginacion pagina={pagina}
            alCambiar={(n) => setConsulta({ ...(anteriores ? { ver: 'anteriores' } : {}), pagina: String(n) })} />
        </>
      )}
    </main>
  )
}

function FilaReserva({ reserva, apartamento }: { reserva: ReservaDelHuesped; apartamento?: ApartamentoPublico }) {
  const foto = apartamento?.imagenes.find((i) => i.principal) ?? apartamento?.imagenes[0]
  return (
    <li>
      <Link to={`/mis-reservas/${encodeURIComponent(reserva.codigo)}`} className="fila-reserva">
        <Foto url={foto?.url} alt="" />
        <div className="fila-reserva-datos">
          <span className={`estado-reserva estado-${reserva.estado.toLowerCase()}`}>{ESTADOS[reserva.estado]}</span>
          <strong>{apartamento?.nombre ?? reserva.apartamento}</strong>
          <span className="suave">{fechaCorta(reserva.entrada)} – {fechaCorta(reserva.salida)}</span>
          <span className="suave">{reserva.codigo}</span>
        </div>
        {/* Una cancelada o sin llegada ya no se cobra: su total confundiría; lo retenido está en su detalle */}
        {reserva.estado !== 'CANCELADA' && reserva.estado !== 'NO_SHOW' && (
          <strong className="fila-reserva-total">{enPesos(reserva.valor.total + reserva.extras.total)}</strong>
        )}
      </Link>
    </li>
  )
}
