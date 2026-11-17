import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { escrito, type Alojamiento } from '../api/alojamiento'
import { consultarPolitica, type PoliticaCancelacion } from '../api/reservas'
import { ListaCondiciones } from '../reservar/Condiciones'

/**
 * Las políticas del sitio en una página, cada una con su ancla para enlazarla desde el pie: cancelación (la del hotel,
 * de la API · DEC-41), tratamiento de datos personales (Ley 1581 de 2012), cookies y registro de turismo (DEC-130).
 * El responsable de los datos es el hotel, con su nombre y su dirección de la API (DEC-83).
 *
 * TODO(equipo): el texto de tratamiento de datos es una base del producto; cada hotel debe revisarlo con su abogado y
 * poner su canal de contacto cuando la API lo traiga.
 */
export default function Politicas({ alojamiento }: { alojamiento: Alojamiento | null }) {
  const [politica, setPolitica] = useState<PoliticaCancelacion | null>(null)
  const [sinPolitica, setSinPolitica] = useState(false)

  useEffect(() => {
    let vigente = true // si la página se cierra antes de que responda, no se toca nada
    consultarPolitica()
      .then((p) => vigente && setPolitica(p))
      .catch(() => vigente && setSinPolitica(true))
    return () => {
      vigente = false
    }
  }, [])

  const hotel = alojamiento?.nombre ?? 'El alojamiento'
  const direccion = [escrito(alojamiento?.direccion), escrito(alojamiento?.ciudad)].filter(Boolean)
  const lugar = [...new Set(direccion)].join(', ')
  const rnt = escrito(alojamiento?.rnt)

  return (
    <main className="politicas">
      <header className="cabeza-pagina">
        <h1>Políticas</h1>
        <p className="suave">Lo que aceptas al reservar y cómo cuidamos tus datos.</p>
      </header>
      <nav className="politicas-indice" aria-label="En esta página">
        <a href="#cancelacion">Cancelación</a>
        <a href="#datos">Tratamiento de datos</a>
        <a href="#cookies">Cookies</a>
        <a href="#registro">Registro de turismo</a>
      </nav>

      <section id="cancelacion" aria-labelledby="titulo-cancelacion">
        <h2 id="titulo-cancelacion">Cancelación</h2>
        <p>Si cancelas, se retiene una parte según cuánto falte para tu llegada:</p>
        {politica && <ListaCondiciones politica={politica} />}
        {sinPolitica && <p className="aviso">No pudimos cargar la política. Intenta de nuevo en un momento.</p>}
        <p className="suave">Es la misma que ves y aceptas en el último paso de la reserva.</p>
      </section>

      <section id="datos" aria-labelledby="titulo-datos">
        <h2 id="titulo-datos">Tratamiento de datos personales</h2>
        <p>
          <strong>{hotel}</strong>{lugar && `, ${lugar},`} es responsable de los datos que nos das al reservar, según la
          Ley 1581 de 2012 y sus decretos.
        </p>
        <h3>Qué datos pedimos</h3>
        <p>
          De quien reserva: nombre, documento, correo y teléfono. De quienes se alojan: nombre, fecha de nacimiento y, cuando se pide,
          su documento. También las fechas, los vehículos y las mascotas de la reserva.
        </p>
        <h3>Para qué los usamos</h3>
        <ul>
          <li>Apartar y confirmar tu reserva, y enviarte por correo su código y sus cambios.</li>
          <li>Calcular el precio: la edad de cada persona a la llegada decide quién paga.</li>
          <li>Registrar tu llegada ante las autoridades de turismo, como exige la ley.</li>
        </ul>
        <h3>Tus pagos</h3>
        <p>Los datos de tu tarjeta los recibe la pasarela de pagos; nosotros no los vemos ni los guardamos.</p>
        <h3>Tus derechos</h3>
        <p>
          Puedes conocer, actualizar y corregir tus datos, pedir que se borren cuando ya no sean necesarios y revocar tu
          autorización. Desde <Link to="/mi-cuenta">Mi cuenta</Link> ves y corriges los tuyos; para lo demás, pídelo en
          la recepción del alojamiento.
        </p>
      </section>

      <section id="cookies" aria-labelledby="titulo-cookies">
        <h2 id="titulo-cookies">Cookies</h2>
        <p>
          Esta web no usa cookies ni herramientas de seguimiento o publicidad. Mientras tienes la sesión abierta, tu
          navegador la guarda en esta pestaña y la borra al cerrarla. Las fuentes de letra se descargan de Google Fonts.
        </p>
      </section>

      <section id="registro" aria-labelledby="titulo-registro">
        <h2 id="titulo-registro">Registro Nacional de Turismo</h2>
        <p>
          {rnt
            ? `${hotel} está inscrito en el Registro Nacional de Turismo con el número ${rnt}.`
            : `${hotel} todavía no ha publicado su número de Registro Nacional de Turismo.`}
        </p>
      </section>
    </main>
  )
}
