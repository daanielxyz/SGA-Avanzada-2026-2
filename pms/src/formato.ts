// Cómo se muestran los datos de la API en todo el PMS. Las fechas llegan como AAAA-MM-DD y se leen por partes para que
// la zona horaria del navegador no las corra un día.

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

const MILES = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 })

function partes(fecha: string): [number, number, number] {
  const [anio, mes, dia] = fecha.slice(0, 10).split('-').map(Number)
  return [anio, mes, dia]
}

/** «14 nov 2026». También sirve con una fecha y hora: toma solo la fecha. */
export function fechaCorta(fecha: string): string {
  const [anio, mes, dia] = partes(fecha)
  return `${dia} ${MESES[mes - 1]} ${anio}`
}

/** «$ 1.250.000»: pesos sin decimales. */
export function dinero(valor: number): string {
  return `$ ${MILES.format(valor)}`
}

/** «4:30 p. m.» a partir de «16:30:00». */
export function hora(valor: string): string {
  const [horas, minutos] = valor.split(':').map(Number)
  const sufijo = horas < 12 ? 'a. m.' : 'p. m.'
  return `${horas % 12 || 12}:${String(minutos).padStart(2, '0')} ${sufijo}`
}

/** Noches de la estancia: la de salida no se ocupa ni se cobra (EST-01). */
export function noches(entrada: string, salida: string): number {
  const dia = (fecha: string) => {
    const [anio, mes, numero] = partes(fecha)
    return Date.UTC(anio, mes - 1, numero)
  }
  return Math.round((dia(salida) - dia(entrada)) / 86_400_000)
}

/** Años cumplidos en una fecha: la edad se calcula, no se guarda. */
export function edadEn(nacimiento: string, fecha: string): number {
  const [anioN, mesN, diaN] = partes(nacimiento)
  const [anio, mes, dia] = partes(fecha)
  const yaCumplio = mes > mesN || (mes === mesN && dia >= diaN)
  return anio - anioN - (yaCumplio ? 0 : 1)
}

/** «1 noche», «3 noches». */
export function cantidad(valor: number, singular: string, plural: string): string {
  return `${valor} ${valor === 1 ? singular : plural}`
}
