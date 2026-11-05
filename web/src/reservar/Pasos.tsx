const PASOS = ['Tus datos', 'Pago', 'Confirmación']

/** Dónde va el huésped en la reserva: los pasos hechos llevan una marca, el actual se resalta. */
export default function Pasos({ actual }: { actual: 1 | 2 | 3 }) {
  return (
    <ol className="pasos" aria-label="Pasos de la reserva">
      {PASOS.map((nombre, i) => {
        const numero = i + 1
        const estado = numero < actual ? 'hecho' : numero === actual ? 'actual' : 'pendiente'
        return (
          <li key={nombre} className={`paso paso-${estado}`} aria-current={estado === 'actual' ? 'step' : undefined}>
            <span className="paso-circulo" aria-hidden="true">
              {estado === 'hecho' ? (
                <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.2"
                  strokeLinecap="round" strokeLinejoin="round">
                  <path d="M3.5 8.5l3 3 6-7" />
                </svg>
              ) : numero}
            </span>
            <span className="paso-nombre">
              {nombre}
              {estado === 'hecho' && <span className="visualmente-oculto"> (hecho)</span>}
            </span>
          </li>
        )
      })}
    </ol>
  )
}
