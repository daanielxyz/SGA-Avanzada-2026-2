import { useEffect, useState, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router'
import { escrito, nombreDelMedio, type Alojamiento } from '../api/alojamiento'
import { listarApartamentos, type ApartamentoPublico, type Estadia, type Grupo, type Pagina } from '../api/apartamentos'
import { consultarPolitica, type PoliticaCancelacion } from '../api/reservas'
import { enlaceAlApartamento } from '../busqueda/enlace'
import Foto from '../busqueda/Foto'
import { llevaIva, tarifaSinIva } from '../busqueda/iva'
import FormularioBusqueda from '../busqueda/FormularioBusqueda'
import { enPesos, horaDelDia } from '../formato'
import { ListaCondiciones } from '../reservar/Condiciones'

/** Cuántos apartamentos se muestran en la presentación; el resto, en `/apartamentos`. */
const DESTACADOS = 3
/** Cuántas comodidades se nombran en «Sobre el hotel»: las más comunes entre sus apartamentos. */
const COMODIDADES = 8

/**
 * La presentación del hotel: lo primero que ve quien llega. En orden: la portada con el buscador de fechas, que lleva
 * a los apartamentos libres; algunos apartamentos; el hotel (su descripción, sus cifras, lo que tienen sus
 * apartamentos y sus fotos); lo que hay que saber antes de reservar, en fichas iguales; y un último llamado a reservar
 * junto a cómo llegar. Todo sale de la API (DEC-83): lo que el hotel no ha escrito no se muestra.
 *
 * @param alojamiento datos, parámetros y marca del hotel; vacío mientras llega o si la API falla
 */
export default function Inicio({ alojamiento }: { alojamiento: Alojamiento | null }) {
  const navegar = useNavigate()
  const [apartamentos, setApartamentos] = useState<Pagina<ApartamentoPublico> | null>(null)
  const [politica, setPolitica] = useState<PoliticaCancelacion | null>(null)

  useEffect(() => {
    let vigente = true // si la página se cierra antes de que responda, no se toca nada
    // Si alguna falla, su parte no aparece: el buscador y el resto de la página siguen sirviendo para reservar
    listarApartamentos().then((p) => vigente && setApartamentos(p)).catch(() => {})
    consultarPolitica().then((p) => vigente && setPolitica(p)).catch(() => {})
    return () => {
      vigente = false
    }
  }, [])

  function buscar({ entrada, salida }: Estadia, { adultos, ninos }: Grupo) {
    const consulta = new URLSearchParams({ entrada, salida, adultos: String(adultos), ninos: String(ninos) })
    navegar(`/apartamentos?${consulta}`)
  }

  const conApartamentos = apartamentos !== null && apartamentos.totalElementos > 0
  const fotos = fotosDePortada(alojamiento?.marca?.portadaUrl, apartamentos?.contenido ?? [])
  return (
    <>
      <Portada alojamiento={alojamiento} fotos={fotos} />
      <main className="presentacion">
        <div className="presentacion-busqueda" id="reservar">
          <FormularioBusqueda buscando={false} filtrando={false} accion="Ver apartamentos libres"
            umbral={alojamiento?.parametros.umbralEdadFacturable ?? null} alBuscar={buscar} />
        </div>
        {conApartamentos && <Destacados pagina={apartamentos} />}
        {alojamiento && <SobreElHotel alojamiento={alojamiento} apartamentos={apartamentos} />}
        {alojamiento && <AntesDeReservar alojamiento={alojamiento} politica={politica} />}
        <Cierre alojamiento={alojamiento} />
      </main>
    </>
  )
}

/** Cuántas fotos rotan en la portada y cada cuánto cambia. */
const FOTOS_PORTADA = 5
const CADA_FOTO_MS = 6000

// La portada del hotel primero; después, la principal de cada apartamento, sin repetir. Sin ninguna, queda el color
function fotosDePortada(portada: string | null | undefined, apartamentos: ApartamentoPublico[]) {
  const principales = apartamentos.map((a) => (a.imagenes.find((i) => i.principal) ?? a.imagenes[0])?.url)
  return [...new Set([portada, ...principales].filter((u): u is string => Boolean(u)))].slice(0, FOTOS_PORTADA)
}

// Quien pidió menos movimiento en su sistema ve una sola foto quieta
function prefiereQuieto() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

/**
 * La portada: el nombre del hotel sobre sus fotos, que se turnan con un fundido y un acercamiento lento. Se pueden
 * pausar o elegir con los puntos (una animación que dura más de 5 s necesita cómo detenerla, WCAG 2.2.2), y con la
 * pestaña oculta no avanza.
 */
function Portada({ alojamiento, fotos }: { alojamiento: Alojamiento | null; fotos: string[] }) {
  const ciudad = escrito(alojamiento?.ciudad)
  const [actual, setActual] = useState(0)
  const [pausada, setPausada] = useState(false)
  const [quieta] = useState(prefiereQuieto)
  const rota = fotos.length > 1 && !pausada && !quieta
  const visible = fotos.length > 0 ? actual % fotos.length : 0

  useEffect(() => {
    if (!rota) {
      return
    }
    const reloj = setInterval(() => {
      if (document.visibilityState !== 'hidden') {
        setActual((a) => (a + 1) % fotos.length)
      }
    }, CADA_FOTO_MS)
    return () => clearInterval(reloj)
  }, [rota, fotos.length, actual])

  return (
    <section className="portada" aria-labelledby="titulo-hotel" aria-roledescription="portada">
      {fotos.map((url, i) => (
        <img key={url} src={url} alt="" data-activa={i === visible} fetchPriority={i === 0 ? 'high' : 'low'}
          onLoad={(e) => { e.currentTarget.dataset.cargada = 'true' }} />
      ))}
      <div className="portada-texto">
        {ciudad && <p className="portada-ciudad">{ciudad}</p>}
        <h1 id="titulo-hotel">{alojamiento?.nombre ?? 'Reserva tu estadía'}</h1>
        {/* La descripción del hotel va en «Sobre el hotel»; aquí, a qué invita la portada */}
        <p>Elige tus fechas y mira qué apartamentos están libres, con el precio de tu grupo.</p>
      </div>
      {fotos.length > 1 && (
        <div className="portada-controles" role="group" aria-label="Fotos de la portada">
          {fotos.map((url, i) => (
            <button key={url} type="button" className="portada-punto" aria-label={`Foto ${i + 1} de ${fotos.length}`}
              aria-current={i === visible} onClick={() => setActual(i)} />
          ))}
          {!quieta && (
            <button type="button" className="portada-pausa" aria-pressed={pausada}
              onClick={() => setPausada((p) => !p)}>
              {pausada ? 'Seguir' : 'Pausar'}
            </button>
          )}
        </div>
      )}
    </section>
  )
}

function Destacados({ pagina }: { pagina: Pagina<ApartamentoPublico> }) {
  const total = pagina.totalElementos
  return (
    <section className="destacados" aria-labelledby="titulo-destacados">
      <div className="seccion-cabeza">
        <div>
          <h2 id="titulo-destacados">Dónde te puedes quedar</h2>
          <p className="entrada-seccion">El precio es el de un adulto en la próxima noche libre de cada apartamento.</p>
        </div>
        {total > DESTACADOS && (
          <Link to="/apartamentos" className="enlace-fuerte">Ver los {total} apartamentos</Link>
        )}
      </div>
      <div className="destacados-lista">
        {pagina.contenido.slice(0, DESTACADOS).map((a) => <Destacado key={a.codigo} apartamento={a} />)}
      </div>
    </section>
  )
}

function Destacado({ apartamento }: { apartamento: ApartamentoPublico }) {
  const foto = apartamento.imagenes.find((i) => i.principal) ?? apartamento.imagenes[0]
  const id = `destacado-${apartamento.codigo}`
  const { precio } = apartamento
  // Para comparar, sin IVA; al abrir el apartamento se desglosa (DEC-141)
  const tarifa = precio?.desglose[0] ? tarifaSinIva(precio, precio.desglose[0].tarifa) : undefined
  return (
    <article className="destacado" aria-labelledby={id}>
      <Foto url={foto?.url} alt={`Foto de ${apartamento.nombre}`} />
      <div className="destacado-cuerpo">
        <h3 id={id}>
          <Link to={enlaceAlApartamento(apartamento)} className="enlace-tarjeta" title={apartamento.nombre}>
            {apartamento.nombre}
          </Link>
        </h3>
        <p className="suave">
          Hasta {apartamento.capacidad} {apartamento.capacidad === 1 ? 'persona' : 'personas'},{' '}
          {apartamento.dormitorios} {apartamento.dormitorios === 1 ? 'dormitorio' : 'dormitorios'}
        </p>
        {tarifa === undefined
          ? <p className="suave">Sin fechas libres por ahora</p>
          : (
            <p className="destacado-precio">
              <strong>{enPesos(tarifa)}</strong> por adulto y noche{precio && llevaIva(precio) ? ' + IVA' : ''}
            </p>
          )}
      </div>
    </article>
  )
}

interface PropsSobre {
  alojamiento: Alojamiento
  apartamentos: Pagina<ApartamentoPublico> | null
}

/** El hotel en sí: lo que escribió de él, sus cifras, lo que tienen sus apartamentos y algunas de sus fotos. */
function SobreElHotel({ alojamiento, apartamentos }: PropsSobre) {
  const descripcion = escrito(alojamiento.descripcion)
  const ciudad = escrito(alojamiento.ciudad)
  const lista = apartamentos?.contenido ?? []
  const comodidades = masComunes(lista.flatMap((a) => a.caracteristicas), COMODIDADES)
  // Las fotos que no salieron arriba, en los destacados: así el mosaico muestra algo nuevo
  const fotos = lista.slice(DESTACADOS).concat(lista.slice(0, DESTACADOS))
    .flatMap((a) => a.imagenes.map((i) => ({ url: i.url, alt: `Foto de ${a.nombre}` })))
    .slice(0, 3)
  const cifras: { valor: string; texto: string }[] = []
  if (apartamentos && apartamentos.totalElementos > 0) {
    const total = apartamentos.totalElementos
    cifras.push({ valor: String(total), texto: total === 1 ? 'apartamento' : 'apartamentos' })
    const mayor = Math.max(...lista.map((a) => a.capacidad))
    cifras.push({ valor: `Hasta ${mayor}`, texto: 'personas en un apartamento' })
  }
  if (ciudad) {
    cifras.push({ valor: ciudad, texto: 'ubicación' })
  }
  if (!descripcion && cifras.length === 0 && comodidades.length === 0) {
    return null
  }
  return (
    <section className="sobre" aria-labelledby="titulo-sobre">
      <div className="sobre-texto">
        <h2 id="titulo-sobre">Sobre {alojamiento.nombre}</h2>
        {descripcion && <p className="sobre-descripcion">{descripcion}</p>}
        {cifras.length > 0 && (
          <dl className="cifras">
            {cifras.map((c) => (
              <div key={c.texto}>
                <dt>{c.texto}</dt>
                <dd>{c.valor}</dd>
              </div>
            ))}
          </dl>
        )}
        {comodidades.length > 0 && (
          <>
            <h3>Lo que encuentras en los apartamentos</h3>
            <ul className="comodidades">
              {comodidades.map((c) => <li key={c}>{c}</li>)}
            </ul>
          </>
        )}
      </div>
      {fotos.length > 0 && (
        <div className="mosaico" data-fotos={fotos.length}>
          {fotos.map((f) => <Foto key={f.url} url={f.url} alt={f.alt} />)}
        </div>
      )}
    </section>
  )
}

// Las comodidades que más apartamentos tienen primero; con el mismo número, en el orden en que aparecen
function masComunes(valores: string[], cuantos: number) {
  const veces = new Map<string, number>()
  for (const v of valores) {
    veces.set(v, (veces.get(v) ?? 0) + 1)
  }
  return [...veces.entries()].sort((a, b) => b[1] - a[1]).slice(0, cuantos).map(([v]) => v)
}

/** Lo que hay que saber antes de reservar, en fichas iguales: cada una un tema, con su título y su contenido. */
function AntesDeReservar({ alojamiento, politica }: { alojamiento: Alojamiento; politica: PoliticaCancelacion | null }) {
  const p = alojamiento.parametros
  const fichas: { titulo: string; contenido: ReactNode }[] = []
  if (p.horaEntrada || p.horaSalida) {
    fichas.push({
      titulo: 'Llegada y salida',
      contenido: (
        <dl className="horarios">
          {p.horaEntrada && (
            <div>
              <dt>Llegada desde</dt>
              <dd>{horaDelDia(p.horaEntrada)}</dd>
            </div>
          )}
          {p.horaSalida && (
            <div>
              <dt>Salida hasta</dt>
              <dd>{horaDelDia(p.horaSalida)}</dd>
            </div>
          )}
        </dl>
      ),
    })
  }
  if (politica) {
    fichas.push({ titulo: 'Si cambian tus planes', contenido: <ListaCondiciones politica={politica} /> })
  }
  const medios = alojamiento.mediosPago ?? []
  if (medios.length > 0) {
    fichas.push({
      titulo: 'Formas de pago',
      contenido: <ul>{medios.map((m) => <li key={m}>{nombreDelMedio(m)}</li>)}</ul>,
    })
  }
  const conParqueadero = (p.cupoParqueadero ?? 0) > 0
  if (conParqueadero) {
    const carro = p.cargoCarro ?? 0
    const moto = p.cargoMoto ?? 0
    const cada = p.cobroParqueadero === 'POR_ESTADIA' ? 'por estadía' : 'por noche'
    fichas.push({
      titulo: 'Parqueadero',
      contenido: <p>{carro === 0 && moto === 0
        ? 'Sin costo, sujeto a cupo.' : `Carro ${enPesos(carro)} y moto ${enPesos(moto)}, ${cada}. Sujeto a cupo.`}</p>,
    })
  }
  if (p.aceptaMascotas !== undefined) {
    const cargo = p.cargoMascota ?? 0
    fichas.push({
      titulo: 'Mascotas',
      contenido: p.aceptaMascotas
        ? <p>Son bienvenidas{p.limiteMascotas ? `, hasta ${p.limiteMascotas} por reserva` : ''}.{' '}
          {cargo > 0 ? `Cada una suma ${enPesos(cargo)} a tu total, una sola vez.` : 'Sin costo.'}</p>
        : <p>Este alojamiento no recibe mascotas.</p>,
    })
  }
  // El parqueadero ya tiene su ficha, con su precio
  const servicios = (alojamiento.serviciosAdicionales ?? [])
    .filter((s) => s.activo && !(conParqueadero && /parqueadero/i.test(s.nombre)))
  if (servicios.length > 0) {
    fichas.push({
      titulo: 'Servicios del alojamiento',
      contenido: (
        <ul>
          {servicios.map((s) => (
            <li key={s.id}>{s.nombre}: {s.generaCargo && s.valor > 0 ? enPesos(s.valor) : 'sin costo'}</li>
          ))}
        </ul>
      ),
    })
  }
  const normas = escrito(alojamiento.normas)
  if (normas) {
    fichas.push({ titulo: 'Normas de la casa', contenido: <p className="normas">{normas}</p> })
  }
  if (fichas.length === 0) {
    return null
  }
  return (
    <section className="antes" aria-labelledby="titulo-antes">
      <h2 id="titulo-antes">Antes de reservar</h2>
      <p className="entrada-seccion">Lo que conviene saber antes de elegir tus fechas.</p>
      <div className="fichas">
        {fichas.map((f) => (
          <section key={f.titulo} className="ficha-info" aria-label={f.titulo}>
            <h3>{f.titulo}</h3>
            {f.contenido}
          </section>
        ))}
      </div>
    </section>
  )
}

/** El último llamado a reservar y, a su lado, cómo llegar. */
function Cierre({ alojamiento }: { alojamiento: Alojamiento | null }) {
  const direccion = escrito(alojamiento?.direccion)
  const ciudad = escrito(alojamiento?.ciudad)
  const { latitud = 0, longitud = 0 } = alojamiento ?? {}
  const conCoordenadas = latitud !== 0 || longitud !== 0
  // Si la dirección ya dice la ciudad (como «Salento, Quindío»), no se repite
  const lineas = [direccion, ciudad && !direccion?.includes(ciudad) ? ciudad : null].filter(Boolean)
  const conLugar = lineas.length > 0 || conCoordenadas
  // Las coordenadas llevan al punto exacto; sin ellas, el mapa busca la dirección escrita
  const lugar = conCoordenadas ? `${latitud},${longitud}` : lineas.join(', ')
  const mapa = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(lugar)}`
  return (
    <section className="cierre" aria-labelledby="titulo-cierre" data-con-lugar={conLugar}>
      <div className="cierre-llamado">
        <h2 id="titulo-cierre">¿Ya sabes cuándo vienes?</h2>
        <p>Mira qué apartamentos están libres en tus fechas y reserva en pocos pasos.</p>
        <Link to="/apartamentos" className="boton">Ver apartamentos</Link>
      </div>
      {conLugar && (
        <div className="cierre-lugar" role="group" aria-labelledby="titulo-llegar">
          <h3 id="titulo-llegar">Cómo llegar</h3>
          {lineas.length > 0 && <address>{lineas.join(', ')}</address>}
          <a href={mapa} target="_blank" rel="noopener noreferrer" className="enlace-fuerte">
            Abrir en Google Maps<span className="visualmente-oculto"> (se abre en otra pestaña)</span>
          </a>
        </div>
      )}
    </section>
  )
}
