// Utilidades de fechas del calendario del PMS (componentes/Calendario.tsx)
// Las fechas van como texto AAAA-MM-DD y se calculan en UTC, para que la zona horaria no corra un día
export const desdeTexto = (fecha: string) => new Date(`${fecha}T00:00:00Z`)
export const aTexto = (fecha: Date) => fecha.toISOString().slice(0, 10)

/** El primer día del mes de una fecha, corrido `meses` meses. */
export function primeroDelMes(fecha: string, meses = 0): string {
  const base = desdeTexto(fecha)
  return aTexto(new Date(Date.UTC(base.getUTCFullYear(), base.getUTCMonth() + meses, 1)))
}

/** Cuántos meses hay del mes de una fecha al de otra. */
export function mesesEntre(desde: string, hasta: string): number {
  const a = desdeTexto(desde)
  const b = desdeTexto(hasta)
  return (b.getUTCFullYear() - a.getUTCFullYear()) * 12 + b.getUTCMonth() - a.getUTCMonth()
}

const formatoMes = new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric', timeZone: 'UTC' })
export const soloMes = new Intl.DateTimeFormat('es-CO', { month: 'long', timeZone: 'UTC' })
/** «noviembre de 2026» → «Noviembre de 2026»: solo la primera letra. */
export const nombreMes = (fecha: string) => formatoMes.format(desdeTexto(fecha)).replace(/^./, (l) => l.toUpperCase())
export const nombreDia = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' })
