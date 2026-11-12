// Qué se le dice al huésped cuando algo falla. El backend escribe sus mensajes para el equipo («Transición de reserva
// no permitida: CONFIRMADA → CANCELADA»); aquí se traducen por su código y su motivo (DEC-33 · DEC-94), que son
// estables, y se dice qué puede hacer. Lo que el backend ya escribe para el huésped (el PIN, el pago rechazado, un
// ocupante repetido) se muestra tal cual.
import { ErrorApi } from './cliente'

/** Un error explicado: el mensaje y, si fueron datos inválidos, qué revisar en cada campo. */
export interface Explicacion {
  mensaje: string
  detalles: string[]
}

const POR_CODIGO: Record<string, string> = {
  ERROR_INTERNO: 'Algo falló de nuestro lado. Intenta de nuevo en un momento; si sigue pasando, comunícate con el '
    + 'hotel.',
  CONCURRENCIA: 'Esto cambió hace un momento. Recarga la página para ver cómo quedó y vuelve a intentarlo.',
  METODO_NO_PERMITIDO: 'Algo falló de nuestro lado. Intenta de nuevo en un momento.',
  VALIDACION: 'Revisa estos datos:',
}

// Por motivo (MotivoRechazo). Recibe el mensaje del backend para los que traen un dato útil (el mínimo de noches)
const POR_MOTIVO: Record<string, (mensaje: string) => string> = {
  NO_DISPONIBLE: () => 'Esas noches ya no están libres en este apartamento: alguien acaba de reservarlas. '
    + 'Elige otras fechas u otro apartamento.',
  FECHAS_INVALIDAS: () => 'Revisa las fechas: la llegada no puede ser antes de hoy y la salida debe ser después de la '
    + 'llegada.',
  CAPACIDAD_EXCEDIDA: () => 'Son más personas de las que caben en este apartamento. Busca uno más grande o ajusta el '
    + 'grupo.',
  ESTANCIA_MINIMA: (mensaje) => `${punto(mensaje)} Agrega noches o elige otras fechas.`,
  SIN_CUPO_PARQUEADERO: () => 'El parqueadero no tiene cupo para tus vehículos esas noches. Quita alguno o elige otras '
    + 'fechas.',
  MASCOTAS_NO_ADMITIDAS: (mensaje) => `${punto(mensaje)} Ajusta la cantidad de mascotas.`,
  SIN_TARIFA: () => 'Este apartamento todavía no tiene precio para esas fechas. Elige otras fechas o comunícate con el '
    + 'hotel.',
  HUESPED_PRINCIPAL_INVALIDO: () => 'Entre quienes se hospedan debe haber al menos una persona mayor de edad: es quien '
    + 'responde por el grupo en el registro del hotel.',
  ESTADO_NO_PERMITIDO: () => 'Tu reserva cambió hace un momento y ya no se puede hacer esto. Recarga la página para '
    + 'ver cómo está.',
}

const punto = (texto: string) => (/[.!?]$/.test(texto.trim()) ? texto.trim() : `${texto.trim()}.`)

/**
 * El mensaje para el huésped.
 *
 * @param siNoResponde lo que se dice si el error no vino del backend, según lo que intentaba hacer
 */
export function explicar(error: unknown, siNoResponde: string): string {
  return explicarConDetalles(error, siNoResponde).mensaje
}

/** Como {@link explicar}, con la lista de qué revisar cuando fueron datos inválidos. */
export function explicarConDetalles(error: unknown, siNoResponde: string): Explicacion {
  if (!(error instanceof ErrorApi)) return { mensaje: siNoResponde, detalles: [] }
  const porMotivo = error.motivo ? POR_MOTIVO[error.motivo] : undefined
  if (porMotivo) return { mensaje: porMotivo(error.message), detalles: [] }
  if (error.codigo === 'VALIDACION') {
    return { mensaje: POR_CODIGO.VALIDACION, detalles: error.detalles.map(detalleClaro) }
  }
  return { mensaje: POR_CODIGO[error.codigo] ?? error.message, detalles: [] }
}

const CAMPOS: Record<string, string> = {
  nombre: 'Nombre', nombres: 'Nombres', nombreRecibido: 'Nombres', primerApellido: 'Apellidos',
  segundoApellido: 'Segundo apellido', fechaNacimiento: 'Fecha de nacimiento', tipoDocumento: 'Tipo de documento',
  numeroDocumento: 'Número de documento', correo: 'Correo', nuevoCorreo: 'Correo nuevo', telefono: 'Teléfono',
  pin: 'PIN', entrada: 'Llegada', salida: 'Salida', carros: 'Carros', motos: 'Motos', mascotas: 'Mascotas',
  ocupantes: 'Quienes se hospedan', apartamento: 'Apartamento',
}

const MENSAJES: Record<string, string> = {
  'hace falta nombres o nombre': 'es obligatorio',
}

/**
 * «ocupantes[1].fechaNacimiento: es obligatoria» → «Persona 2 · Fecha de nacimiento: es obligatoria»;
 * «titular.correo: no tiene formato de correo» → «Correo: no tiene formato de correo». Lo que no se reconoce queda
 * como llegó.
 */
export function detalleClaro(detalle: string) {
  const separador = detalle.indexOf(': ')
  if (separador < 0) return detalle
  const ruta = detalle.slice(0, separador)
  const mensaje = detalle.slice(separador + 2)
  const persona = ruta.match(/ocupantes\[(\d+)\]/)
  const campo = CAMPOS[ruta.split('.').pop()?.replace(/\[\d+\]$/, '') ?? '']
  if (!campo) return detalle
  return `${persona ? `Persona ${Number(persona[1]) + 1} · ` : ''}${campo}: ${MENSAJES[mensaje] ?? mensaje}`
}
