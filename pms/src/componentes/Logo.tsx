/**
 * Logo de Meridian, la marca del producto (no la del alojamiento, DEC-83). Los dos PNG son solo la forma: se pintan
 * como máscara con el color del tema (`--logo-color` en `src/index.css`), Arena de noche y Medianoche de día.
 *
 * @param apilado    el símbolo encima de la palabra, como en la entrada; si no, uno al lado del otro
 * @param decorativo cuando el mismo logo ya se anuncia en otro lugar de la pantalla
 */
export default function Logo({ apilado = false, decorativo = false }: { apilado?: boolean; decorativo?: boolean }) {
  return (
    <span className={apilado ? 'logo logo-apilado' : 'logo'}>
      <span className="logo-simbolo" aria-hidden="true" />
      {decorativo
        ? <span className="logo-palabra" aria-hidden="true" />
        : <span className="logo-palabra" role="img" aria-label="Meridian" />}
    </span>
  )
}
