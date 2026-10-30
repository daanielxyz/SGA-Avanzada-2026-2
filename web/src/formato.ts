// Cómo se muestran el dinero y las fechas en toda la web, para que se vean igual en todas las pantallas.

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

/** 400000 → «$ 400.000». */
export function enPesos(valor: number) {
  return pesos.format(valor)
}

/** «2026-12-20» → «dom, 20 dic». Se lee como fecha local para que no se corra un día por la zona horaria. */
export function fechaCorta(fecha: string) {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  return new Date(anio, mes - 1, dia).toLocaleDateString('es-CO', { weekday: 'short', day: 'numeric', month: 'short' })
}

/** La fecha de hoy, más los días dados, en el formato de los campos de fecha (`AAAA-MM-DD`). */
export function fechaDesdeHoy(dias = 0) {
  const fecha = new Date()
  fecha.setDate(fecha.getDate() + dias)
  const dosCifras = (n: number) => String(n).padStart(2, '0')
  return `${fecha.getFullYear()}-${dosCifras(fecha.getMonth() + 1)}-${dosCifras(fecha.getDate())}`
}

/** «2026-12-31» más 1 día → «2027-01-01». Se calcula en UTC para que el cambio de hora no corra el día. */
export function masDias(fecha: string, dias: number) {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  return new Date(Date.UTC(anio, mes - 1, dia + dias)).toISOString().slice(0, 10)
}

/** Noches entre dos fechas `AAAA-MM-DD`; la de salida no cuenta. */
export function noches(entrada: string, salida: string) {
  const dia = (f: string) => {
    const [anio, mes, d] = f.split('-').map(Number)
    return Date.UTC(anio, mes - 1, d)
  }
  return Math.round((dia(salida) - dia(entrada)) / 86_400_000)
}
