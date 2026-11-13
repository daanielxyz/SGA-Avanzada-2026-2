import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { listarPendientesPreparacion } from '../api/apartamentos'
import { listarSalidasVencidas, type Reserva } from '../api/reservas'
import Icono from '../componentes/Icono'
import { usePulso } from '../componentes/usePulso'
import { useTitulo } from '../componentes/useTitulo'
import { cantidad, fechaCorta, fechaHoy } from '../formato'
import { menuPara } from '../navegacion'
import { useSesion } from '../sesion/contexto'

/**
 * Primera pantalla después de entrar: saludo y accesos a lo que el rol puede hacer. Al personal de servicio le dice
 * de una vez cuántos apartamentos tiene por preparar, y ese acceso va primero y en grande.
 */
export default function Inicio() {
  const { sesion } = useSesion()
  useTitulo('Inicio')
  const [porPreparar, setPorPreparar] = useState<number | null>(null)
  const prepara = sesion?.rol === 'PERSONAL_SERVICIO' || sesion?.rol === 'ADMINISTRADOR'
  const atiende = sesion?.rol === 'RECEPCIONISTA' || sesion?.rol === 'ADMINISTRADOR'
  const [vencidas, setVencidas] = useState<Reserva[]>([])
  const pulso = usePulso()

  // Una salida que no se registró deja el apartamento ocupado: recepción lo ve apenas entra
  useEffect(() => {
    if (!atiende) return
    let vigente = true
    listarSalidasVencidas(fechaHoy())
      .then((lista) => {
        if (vigente) setVencidas(lista)
      })
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [atiende, pulso])

  useEffect(() => {
    if (!prepara) return
    let vigente = true
    // Si no se puede contar, el acceso se muestra igual, sin la cantidad
    listarPendientesPreparacion(0)
      .then((pagina) => {
        if (vigente) setPorPreparar(pagina.totalElementos)
      })
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [prepara, pulso])

  if (!sesion) return null
  const servicio = sesion.rol === 'PERSONAL_SERVICIO'

  return (
    <div className="bienvenida">
      <div className="bienvenida-saludo">
        <div className="icono-estado">
          <Icono nombre="alojamiento" />
        </div>
        <h1>Hola, {sesion.nombre}</h1>
        <p className="suave">Estos son los accesos de tu rol.</p>
      </div>
      {vencidas.length > 0 && (
        <section className="tarjeta salidas-vencidas" aria-labelledby="titulo-vencidas">
          <h2 id="titulo-vencidas">
            <span aria-hidden="true">!</span> {cantidad(vencidas.length, 'salida vencida', 'salidas vencidas')}
          </h2>
          <p className="suave">Siguen en curso aunque su salida ya pasó: el apartamento no se puede entregar a la próxima reserva.</p>
          <ul>
            {vencidas.map((reserva) => (
              <li key={reserva.codigo}>
                <Link to={`/reservas/${encodeURIComponent(reserva.codigo)}`} className="codigo">{reserva.codigo}</Link>
                {' · '}{reserva.apartamento} · salía el {fechaCorta(reserva.salida)}
              </li>
            ))}
          </ul>
        </section>
      )}
      <div className="accesos">
        {menuPara(sesion.rol).flatMap(({ opciones }) =>
          opciones.map((opcion) => {
            const esPreparacion = opcion.ruta === '/preparacion'
            const destacado = servicio && esPreparacion
            return (
              <Link key={opcion.ruta} to={opcion.ruta} className={destacado ? 'acceso acceso-destacado' : 'acceso'}>
                <Icono nombre={opcion.icono} />
                <span>
                  <span className="acceso-titulo">{destacado ? 'Por preparar' : opcion.titulo}</span>
                  <span className="acceso-texto">
                    {esPreparacion && porPreparar !== null
                      ? porPreparar === 0
                        ? 'Todo está preparado.'
                        : `${cantidad(porPreparar, 'apartamento', 'apartamentos')} por preparar.`
                      : opcion.descripcion}
                  </span>
                </span>
              </Link>
            )
          }),
        )}
      </div>
    </div>
  )
}
