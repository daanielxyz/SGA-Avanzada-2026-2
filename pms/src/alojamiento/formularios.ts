// Cada bloque de «Alojamiento» se llena con texto y se revisa antes de enviarlo, con los mismos límites del contrato
// (docs/api/openapi.yaml): así el error sale en español junto al campo, sin esperar el 400.
import { NOMBRES_COLOR, type Alojamiento, type DatosAlojamiento, type Marca, type NombreColor, type Parametros } from '../api/alojamiento'
import { COLOR_VALIDO } from './contraste'

export type Errores<C extends string> = Partial<Record<C, string>>

function entero(texto: string): number | null {
  return /^\d+$/.test(texto.trim()) ? Number(texto.trim()) : null
}

/** Pesos sin decimales: «15.000» o «15000». */
function pesos(texto: string): number | null {
  const limpio = texto.trim().replaceAll('.', '').replace(/^\$\s*/, '')
  return /^\d+$/.test(limpio) ? Number(limpio) : null
}

/** «15:00» a partir de «15:00:00». */
function horaCorta(valor: string): string {
  return valor.slice(0, 5)
}

// ---- Datos ----

export type FormDatos = Record<'nombre' | 'descripcion' | 'ciudad' | 'direccion' | 'normas', string>
export const LARGO_DATOS = { nombre: 120, ciudad: 120, direccion: 200 } as const

export function formDatos(alojamiento: Alojamiento): FormDatos {
  return {
    nombre: alojamiento.nombre ?? '',
    descripcion: alojamiento.descripcion ?? '',
    ciudad: alojamiento.ciudad ?? '',
    direccion: alojamiento.direccion ?? '',
    normas: alojamiento.normas ?? '',
  }
}

// ALO-01: nombre, descripción, ciudad y dirección son obligatorios
export function validarDatos(form: FormDatos): Errores<keyof FormDatos> {
  const errores: Errores<keyof FormDatos> = {}
  if (!form.nombre.trim()) errores.nombre = 'Escribe el nombre del alojamiento.'
  else if (form.nombre.length > LARGO_DATOS.nombre) errores.nombre = 'El nombre admite hasta 120 caracteres.'
  if (!form.ciudad.trim()) errores.ciudad = 'Escribe la ciudad.'
  else if (form.ciudad.length > LARGO_DATOS.ciudad) errores.ciudad = 'La ciudad admite hasta 120 caracteres.'
  if (!form.direccion.trim()) errores.direccion = 'Escribe la dirección.'
  else if (form.direccion.length > LARGO_DATOS.direccion) errores.direccion = 'La dirección admite hasta 200 caracteres.'
  if (!form.descripcion.trim()) errores.descripcion = 'Escribe la descripción.'
  return errores
}

export function cuerpoDatos(form: FormDatos): DatosAlojamiento {
  return {
    nombre: form.nombre.trim(),
    descripcion: form.descripcion.trim(),
    ciudad: form.ciudad.trim(),
    direccion: form.direccion.trim(),
    normas: form.normas.trim() || null,
  }
}

// ---- Ubicación ----

export type FormUbicacion = Record<'latitud' | 'longitud', string>

export function formUbicacion(alojamiento: Alojamiento): FormUbicacion {
  return {
    latitud: alojamiento.latitud === null || alojamiento.latitud === undefined ? '' : String(alojamiento.latitud),
    longitud: alojamiento.longitud === null || alojamiento.longitud === undefined ? '' : String(alojamiento.longitud),
  }
}

/** Google Maps copia «4.6372, -75.5701»: se acepta la coma decimal y el punto. */
function coordenada(texto: string): number | null {
  const limpio = texto.trim().replace(',', '.')
  return /^-?\d+(\.\d+)?$/.test(limpio) ? Number(limpio) : null
}

/** Si se pega «4.6372, -75.5701» en la latitud, se reparte entre los dos campos. */
export function repartirPegado(texto: string): FormUbicacion | null {
  const partes = /^\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*$/.exec(texto)
  return partes ? { latitud: partes[1], longitud: partes[2] } : null
}

// ALO-04: latitud entre -90 y 90; longitud entre -180 y 180
export function validarUbicacion(form: FormUbicacion): Errores<keyof FormUbicacion> {
  const errores: Errores<keyof FormUbicacion> = {}
  const latitud = coordenada(form.latitud)
  const longitud = coordenada(form.longitud)
  if (latitud === null) errores.latitud = 'Escribe la latitud con números, por ejemplo 4.6372.'
  else if (latitud < -90 || latitud > 90) errores.latitud = 'La latitud va de -90 a 90.'
  if (longitud === null) errores.longitud = 'Escribe la longitud con números, por ejemplo -75.5701.'
  else if (longitud < -180 || longitud > 180) errores.longitud = 'La longitud va de -180 a 180.'
  return errores
}

export function cuerpoUbicacion(form: FormUbicacion): { latitud: number; longitud: number } {
  return { latitud: coordenada(form.latitud) ?? 0, longitud: coordenada(form.longitud) ?? 0 }
}

// ---- Reglas de la estancia ----

const HORAS = ['horaEntrada', 'horaSalida', 'horaLimiteNoShow'] as const
const ENTEROS = [
  'tiempoPreparacionHoras', 'plazoConfirmacionHoras', 'anticipoPct', 'plazoPagoEnLineaMinutos', 'umbralEdadFacturable',
  'cupoParqueadero', 'limiteMascotas', 'minimoMediosPago', 'minimoServiciosAdicionales', 'minimoTemporadas',
  'minimoTramosCancelacion', 'minimoCapacidadesDistintas',
] as const
const PESOS = ['cargoCarro', 'cargoMoto', 'cargoMascota'] as const

type CampoTexto = (typeof HORAS)[number] | (typeof ENTEROS)[number] | (typeof PESOS)[number]
export type FormReglas = Record<CampoTexto, string> & Pick<Parametros, 'pagoEnLinea' | 'cobroParqueadero' | 'aceptaMascotas'>
export type CampoReglas = keyof FormReglas

/** Formato de pesos para mostrar en el campo: «15.000». */
export function textoPesos(valor: number): string {
  return new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 }).format(valor)
}

export function formReglas(parametros: Parametros): FormReglas {
  const form = {
    pagoEnLinea: parametros.pagoEnLinea,
    cobroParqueadero: parametros.cobroParqueadero,
    aceptaMascotas: parametros.aceptaMascotas,
  } as FormReglas
  for (const campo of HORAS) form[campo] = horaCorta(parametros[campo] ?? '')
  for (const campo of ENTEROS) form[campo] = String(parametros[campo] ?? 0)
  for (const campo of PESOS) form[campo] = textoPesos(parametros[campo] ?? 0)
  return form
}

/** Lo menos que admite cada número (ParametrosAlojamiento): los plazos y los tramos no pueden ser 0. */
const MINIMO: Partial<Record<CampoTexto, number>> = {
  plazoConfirmacionHoras: 1,
  plazoPagoEnLineaMinutos: 1,
  minimoTramosCancelacion: 1,
}

// ALO-02 · ALO-03 · ALO-06 · TPRE-01 · CAP-05 · RP-02 · RP-03
export function validarReglas(form: FormReglas): Errores<CampoReglas> {
  const errores: Errores<CampoReglas> = {}
  for (const campo of HORAS) {
    if (!/^\d{2}:\d{2}$/.test(form[campo])) errores[campo] = 'Escribe la hora, por ejemplo 15:00.'
  }
  for (const campo of ENTEROS) {
    const valor = entero(form[campo])
    const minimo = MINIMO[campo] ?? 0
    if (valor === null) errores[campo] = 'Escribe un número entero, sin puntos ni comas.'
    else if (valor < minimo) errores[campo] = `Debe ser ${minimo} o más.`
  }
  if (!errores.anticipoPct && Number(form.anticipoPct) > 100) errores.anticipoPct = 'El anticipo va de 0 a 100 %.'
  for (const campo of PESOS) {
    if (pesos(form[campo]) === null) errores[campo] = 'Escribe el valor en pesos, sin decimales.'
  }
  // Ocultos cuando no se aceptan mascotas: no pueden frenar el guardado
  if (!form.aceptaMascotas) {
    delete errores.limiteMascotas
    delete errores.cargoMascota
  }
  return errores
}

/** Los 21 parámetros: la API los reemplaza todos, incluidos parqueadero y mascotas aunque estén ocultos. */
export function cuerpoReglas(form: FormReglas): Parametros {
  const cuerpo = {
    pagoEnLinea: form.pagoEnLinea,
    cobroParqueadero: form.cobroParqueadero,
    aceptaMascotas: form.aceptaMascotas,
  } as Parametros
  for (const campo of HORAS) cuerpo[campo] = `${form[campo]}:00`
  for (const campo of ENTEROS) cuerpo[campo] = entero(form[campo]) ?? 0
  for (const campo of PESOS) cuerpo[campo] = pesos(form[campo]) ?? 0
  return cuerpo
}


// ---- Marca ----

type CampoFuenteRadio = 'fuenteTitulos' | 'fuenteTexto' | 'radioPequeno' | 'radioMediano' | 'radioGrande' | 'logoUrl' | 'portadaUrl'
export type FormMarca = Record<NombreColor | CampoFuenteRadio, string>
export type CampoMarca = keyof FormMarca

/** Para un alojamiento que todavía no tiene marca: un punto de partida claro y legible, no la de ningún hotel. */
const MARCA_INICIAL: FormMarca = {
  fondo: '#FFFFFF', superficie: '#F5F5F5', texto: '#1F1F1F', textoSuave: '#5C5C5C', borde: '#E0E0E0',
  bordeCampo: '#8A8A8A', acento: '#1F4E79', textoSobreAcento: '#FFFFFF', peligro: '#B3261E', exito: '#2E7D32',
  fuenteTitulos: 'Inter', fuenteTexto: 'Inter', radioPequeno: '4', radioMediano: '8', radioGrande: '16',
  logoUrl: '', portadaUrl: '',
}

export function formMarca(marca: Marca | null | undefined): FormMarca {
  const form = { ...MARCA_INICIAL }
  if (!marca) return form
  for (const nombre of NOMBRES_COLOR) form[nombre] = (marca.colores?.[nombre] ?? form[nombre]).toUpperCase()
  form.fuenteTitulos = marca.fuentes?.titulos ?? form.fuenteTitulos
  form.fuenteTexto = marca.fuentes?.texto ?? form.fuenteTexto
  form.radioPequeno = (marca.radio?.pequeno ?? '4px').replace('px', '')
  form.radioMediano = (marca.radio?.mediano ?? '8px').replace('px', '')
  form.radioGrande = (marca.radio?.grande ?? '16px').replace('px', '')
  form.logoUrl = marca.logoUrl ?? ''
  form.portadaUrl = marca.portadaUrl ?? ''
  return form
}

const FUENTE_VALIDA = /^[\p{L}0-9 ]{1,60}$/u
const IMAGEN_VALIDA = /^(https?:\/\/|data:image\/)\S+$/

// BL-05 · DEC-87: los mismos patrones de MarcaRequest
export function validarMarca(form: FormMarca): Errores<CampoMarca> {
  const errores: Errores<CampoMarca> = {}
  for (const nombre of NOMBRES_COLOR) {
    if (!COLOR_VALIDO.test(form[nombre])) errores[nombre] = 'Escribe el color como #RRGGBB, por ejemplo #1F4E79.'
  }
  for (const campo of ['fuenteTitulos', 'fuenteTexto'] as const) {
    if (!FUENTE_VALIDA.test(form[campo].trim())) errores[campo] = 'Escribe el nombre de la fuente: letras, números y espacios.'
  }
  for (const campo of ['radioPequeno', 'radioMediano', 'radioGrande'] as const) {
    if (!/^\d{1,2}$/.test(form[campo].trim())) errores[campo] = 'De 0 a 99 px.'
  }
  for (const campo of ['logoUrl', 'portadaUrl'] as const) {
    const valor = form[campo].trim()
    if (valor.length > 2000) errores[campo] = 'La dirección admite hasta 2000 caracteres.'
    else if (valor && !IMAGEN_VALIDA.test(valor)) errores[campo] = 'Pega la dirección completa, que empiece por https://'
  }
  return errores
}

export function cuerpoMarca(form: FormMarca): Marca {
  const colores = {} as Marca['colores']
  for (const nombre of NOMBRES_COLOR) colores[nombre] = form[nombre].toUpperCase()
  return {
    colores,
    fuentes: { titulos: form.fuenteTitulos.trim(), texto: form.fuenteTexto.trim() },
    radio: {
      pequeno: `${Number(form.radioPequeno)}px`,
      mediano: `${Number(form.radioMediano)}px`,
      grande: `${Number(form.radioGrande)}px`,
    },
    logoUrl: form.logoUrl.trim() || null,
    portadaUrl: form.portadaUrl.trim() || null,
  }
}

// ---- Servicios y medios de pago ----

/** «Pago en línea» a partir de «PAGO_EN_LINEA»; los demás, como «Efectivo». */
export function nombreMedio(medio: string): string {
  if (medio === 'PAGO_EN_LINEA') return 'Pago en línea'
  const texto = medio.replaceAll('_', ' ').toLowerCase()
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

/** El valor de un servicio: pesos sin decimales, 0 o más. Nulo si no se entiende. */
export function valorServicio(texto: string): number | null {
  return pesos(texto)
}
