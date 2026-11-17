// Datos del alojamiento que necesita el PMS: nada de él va escrito en el código (DEC-83).
import { pedir } from './cliente'

/** Los parámetros que el alojamiento configura (`ParametrosResponse`, DEC-25). Las horas llegan como «15:00:00». */
export interface Parametros {
  /** Desde esta edad, a la fecha de entrada, un ocupante paga (RN-06). */
  umbralEdadFacturable: number
  horaEntrada: string
  horaSalida: string
  tiempoPreparacionHoras: number
  /** Horas que tiene una pendiente para pagar el anticipo. */
  plazoConfirmacionHoras: number
  /** Desde esta hora del día de entrada se puede declarar el no-show (RES-16). */
  horaLimiteNoShow: string
  /** 0 = no se exige anticipo. */
  anticipoPct: number
  minimoMediosPago: number
  minimoServiciosAdicionales: number
  minimoTemporadas: number
  minimoTramosCancelacion: number
  minimoCapacidadesDistintas: number
  /** Lo que se paga en la web de reservas (DEC-70). */
  pagoEnLinea: 'ANTICIPO' | 'TOTAL'
  plazoPagoEnLineaMinutos: number
  /** Vehículos (carros y motos) que admite el parqueadero cada noche; 0 = no hay (RP-02). */
  cupoParqueadero: number
  aceptaMascotas: boolean
  limiteMascotas: number
  /** Por mascota, una vez por estancia (RP-03). */
  cargoMascota: number
  /** Si el parqueadero se cobra por cada noche o una vez por estancia (DEC-89). */
  cobroParqueadero: 'POR_NOCHE' | 'POR_ESTADIA'
  /** Por carro y por moto; 0 = no se cobra. */
  cargoCarro: number
  cargoMoto: number
}

export interface ServicioAdicional {
  id: string
  nombre: string
  /** Falso: el servicio se ofrece sin cobro y no se carga al folio. */
  generaCargo: boolean
  valor: number
  activo: boolean
}

export const NOMBRES_COLOR = [
  'fondo', 'superficie', 'texto', 'textoSuave', 'borde', 'bordeCampo', 'acento', 'textoSobreAcento', 'peligro', 'exito',
] as const
export type NombreColor = (typeof NOMBRES_COLOR)[number]

/** Lo que usa la web de reservas para verse como el hotel (BL-05, DEC-87). Los radios llegan como «8px». */
export interface Marca {
  colores: Record<NombreColor, string>
  fuentes: { titulos: string; texto: string }
  radio: { pequeno: string; mediano: string; grande: string }
  logoUrl: string | null
  portadaUrl: string | null
}

/** Lo que usa el PMS de `AlojamientoResponse`. */
export interface Alojamiento {
  nombre: string
  descripcion?: string
  ciudad?: string
  direccion?: string
  normas?: string | null
  latitud?: number | null
  longitud?: number | null
  parametros: Parametros
  /** Solo los habilitados, en mayúsculas («EFECTIVO»). */
  mediosPago: string[]
  /** Catálogo de consumos que se cargan al folio (CAR-05), con los desactivados. Puede faltar en respuestas viejas. */
  serviciosAdicionales?: ServicioAdicional[]
  marca?: Marca | null
  /** Registro Nacional de Turismo, con el que se envía el TRA (DEC-130); null si no se ha escrito. */
  rnt?: string | null
  /** El IVA de la estancia (DEC-133 · DEC-141). Puede faltar en respuestas viejas. */
  impuesto?: Impuesto | null
}

/** Si el alojamiento cobra el IVA de la estancia y su porcentaje (19 por defecto, apagado al empezar). */
export interface Impuesto {
  cobraIva: boolean
  porcentaje: number
  /** DEC-141: si las tarifas ya traen el IVA; si no, se les suma. Puede faltar en respuestas viejas. */
  incluidoEnTarifa?: boolean
}

export interface DatosAlojamiento {
  nombre: string
  descripcion: string
  ciudad: string
  direccion: string
  normas: string | null
}

/** `GET /api/alojamiento` (público). */
export function consultarAlojamiento(): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento')
}

/** `PUT /api/alojamiento/datos` (CU-50). */
export function guardarDatos(datos: DatosAlojamiento): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento/datos', { method: 'PUT', cuerpo: datos })
}

/** `PUT /api/alojamiento/ubicacion`. */
export function guardarUbicacion(latitud: number, longitud: number): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento/ubicacion', { method: 'PUT', cuerpo: { latitud, longitud } })
}

/** `PUT /api/alojamiento/rnt` (DEC-130): solo números, hasta 20; vacío lo borra. */
export function guardarRnt(rnt: string): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento/rnt', { method: 'PUT', cuerpo: { rnt } })
}

/**
 * `PUT /api/alojamiento/iva` (DEC-133), solo el administrador. Rige para las llegadas que vienen: lo ya cobrado no
 * cambia.
 *
 * @throws ErrorApi 409 si se pide cobrarlo con el 0 %
 */
export function guardarIva(impuesto: Impuesto): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento/iva', { method: 'PUT', cuerpo: impuesto })
}

/** `PUT /api/alojamiento/parametros`: **reemplaza todos**, así que siempre van los 21 (los que faltan se apagan). */
export function guardarParametros(parametros: Parametros): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento/parametros', { method: 'PUT', cuerpo: parametros })
}

/** `PUT /api/alojamiento/marca`. La web de reservas la lee de `GET /api/alojamiento`. */
export function guardarMarca(marca: Marca): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento/marca', { method: 'PUT', cuerpo: marca })
}

/** `POST /api/alojamiento/servicios` (SERV-01). */
export function agregarServicio(servicio: { nombre: string; generaCargo: boolean; valor: number }): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento/servicios', { method: 'POST', cuerpo: servicio })
}

/** `PUT /api/alojamiento/servicios/{id}/valor`: lo ya cargado no cambia (SERV-02). */
export function cambiarValorServicio(id: string, valor: number): Promise<Alojamiento> {
  return pedir<Alojamiento>(`/api/alojamiento/servicios/${encodeURIComponent(id)}/valor`, { method: 'PUT', cuerpo: { valor } })
}

/** `PUT /api/alojamiento/servicios/{id}/desactivar`: no se puede volver a activar (SERV-04). */
export function desactivarServicio(id: string): Promise<Alojamiento> {
  return pedir<Alojamiento>(`/api/alojamiento/servicios/${encodeURIComponent(id)}/desactivar`, { method: 'PUT' })
}

/** `PUT /api/alojamiento/medios-pago/{medio}/habilitar` o `…/deshabilitar` (MPAG-06). El medio es texto libre. */
export function cambiarMedioPago(medio: string, accion: 'habilitar' | 'deshabilitar'): Promise<Alojamiento> {
  return pedir<Alojamiento>(`/api/alojamiento/medios-pago/${encodeURIComponent(medio)}/${accion}`, { method: 'PUT' })
}

const EVENTO_NOMBRE = 'sga:alojamiento-renombrado'

/** La cabecera del panel muestra el nombre: se le avisa cuando se guarda otro. */
export function avisarNombre(nombre: string) {
  window.dispatchEvent(new CustomEvent(EVENTO_NOMBRE, { detail: nombre }))
}

/** @returns cómo dejar de escuchar */
export function alCambiarNombre(escuchar: (nombre: string) => void): () => void {
  const alAvisar = (evento: Event) => escuchar((evento as CustomEvent<string>).detail)
  window.addEventListener(EVENTO_NOMBRE, alAvisar)
  return () => window.removeEventListener(EVENTO_NOMBRE, alAvisar)
}
