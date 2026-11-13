/**
 * Motivo con contador, para revertir un cargo (hasta 170), autorizar un cierre con saldo (hasta 500) o, opcional, decir
 * por qué un apartamento queda fuera de servicio. El límite es el del Request: más no se puede escribir.
 */
export default function CampoMotivo({ id, rotulo, valor, maximo, error, deshabilitado, pista, focoInicial, opcional = false, alCambiar }: {
  id: string
  rotulo: string
  valor: string
  maximo: number
  error?: string
  deshabilitado?: boolean
  pista?: string
  focoInicial?: boolean
  opcional?: boolean
  alCambiar: (valor: string) => void
}) {
  const descritoPor = [error ? `${id}-error` : null, `${id}-contador`].filter(Boolean).join(' ')
  return (
    <div>
      <label className="campo-rotulo" htmlFor={id}>
        {rotulo} <span className="campo-rotulo-nota">({opcional ? 'opcional' : 'obligatorio'})</span>
      </label>
      <textarea
        id={id}
        className="campo-control campo-texto"
        rows={3}
        maxLength={maximo}
        value={valor}
        placeholder={pista}
        disabled={deshabilitado}
        data-foco-inicial={focoInicial || undefined}
        aria-invalid={Boolean(error)}
        aria-describedby={descritoPor}
        onChange={(evento) => alCambiar(evento.target.value)}
      />
      <div className="campo-contador">
        {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
        <p className="contador" id={`${id}-contador`}>
          {valor.length} / {maximo}
        </p>
      </div>
    </div>
  )
}
