/**
 * Logo de Meridian, la marca del producto (no la del alojamiento, DEC-83).
 *
 * @param apilado    el símbolo encima de la palabra, como en la entrada; si no, uno al lado del otro
 * @param decorativo cuando el mismo logo ya se anuncia en otro lugar de la pantalla
 */
export default function Logo({ apilado = false, decorativo = false }: { apilado?: boolean; decorativo?: boolean }) {
  return (
    <span className={apilado ? 'logo logo-apilado' : 'logo'}>
      <img className="logo-simbolo" src="/marca/simbolo.png" alt="" width={346} height={408} />
      <img className="logo-palabra" src="/marca/palabra.png" alt={decorativo ? '' : 'Meridian'} width={964} height={117} />
    </span>
  )
}
