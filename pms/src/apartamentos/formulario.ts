// Lo que deciden las pantallas de apartamentos sin hablar con la API: qué revisar antes de enviar, qué se envía y cómo
// se ordenan los bloqueos. Las reglas son las de CrearApartamentoRequest y del dominio (pms/DISENO.md, «Sprint 4:
// apartamentos»); la API sigue siendo quien decide.
import type { Bloqueo, DatosApartamento, FichaApartamento, Imagen, NuevoApartamento } from '../api/apartamentos'

export const MAXIMO_NOMBRE = 120
export const MAXIMO_DESCRIPCION = 2000
export const MAXIMO_CARACTERISTICA = 100
export const MAXIMO_FOTOS = 10
export const MAXIMO_URL = 500
export const MAXIMO_MOTIVO_BLOQUEO = 500

export type Errores<C extends string> = Partial<Record<C, string>>

/** El formulario de «Nuevo apartamento» y de «Editar»: los números se guardan como se escriben. */
export interface FormApartamento {
  /** Solo los dígitos: «APT-» lo pone la pantalla. */
  numero: string
  nombre: string
  descripcion: string
  capacidad: string
  dormitorios: string
  caracteristicas: string[]
  imagenes: Imagen[]
}

export type CampoApartamento = keyof FormApartamento

export const FORM_VACIO: FormApartamento = {
  numero: '',
  nombre: '',
  descripcion: '',
  capacidad: '',
  dormitorios: '',
  caracteristicas: [],
  imagenes: [],
}

export function formDe(apartamento: FichaApartamento): FormApartamento {
  return {
    numero: apartamento.codigo.replace(/^APT-/, ''),
    nombre: apartamento.nombre,
    descripcion: apartamento.descripcion ?? '',
    capacidad: String(apartamento.capacidad),
    dormitorios: String(apartamento.dormitorios),
    caracteristicas: apartamento.caracteristicas,
    imagenes: apartamento.imagenes,
  }
}

const entero = (texto: string) => (/^\d+$/.test(texto.trim()) ? Number(texto.trim()) : NaN)

/** Una dirección que el navegador puede abrir (`ImagenRequest.url`: `https?://…`, sin espacios). */
export function esDireccionDeImagen(url: string): boolean {
  return /^https?:\/\/\S+$/.test(url.trim()) && url.trim().length <= MAXIMO_URL
}

/**
 * Revisa el formulario antes de enviarlo. Al editar no se revisan el código, la capacidad ni las fotos, que ahí no se
 * cambian.
 */
export function validarApartamento(form: FormApartamento, nuevo: boolean): Errores<CampoApartamento> {
  const errores: Errores<CampoApartamento> = {}
  if (nuevo && !/^\d+$/.test(form.numero.trim())) errores.numero = 'Escribe el número del apartamento, por ejemplo 111.'
  if (!form.nombre.trim()) errores.nombre = 'Escribe el nombre del apartamento.'
  if (nuevo && !(entero(form.capacidad) >= 1)) errores.capacidad = 'Escribe cuántas personas caben, desde 1.'
  if (!(entero(form.dormitorios) >= 1)) errores.dormitorios = 'Escribe cuántos dormitorios tiene, desde 1.'
  if (form.caracteristicas.length === 0) {
    errores.caracteristicas = 'El apartamento debe declarar al menos una característica.'
  }
  if (nuevo) {
    // Acordado el 13 nov: todo apartamento tiene al menos una foto, aunque la API acepte crearlo sin ellas
    const urls = form.imagenes.map((imagen) => imagen.url.trim())
    if (urls.every((url) => !url)) errores.imagenes = 'Agrega al menos una foto.'
    else if (urls.some((url) => !esDireccionDeImagen(url))) {
      errores.imagenes = 'Revisa las direcciones: deben empezar por https:// y no llevar espacios.'
    } else if (new Set(urls).size !== urls.length) {
      errores.imagenes = 'Hay una foto repetida.'
    } else if (form.imagenes.filter((imagen) => imagen.principal).length !== 1) {
      errores.imagenes = 'Exactamente una imagen debe ser la principal.'
    }
  }
  return errores
}

const caracteristicasLimpias = (form: FormApartamento) => form.caracteristicas.map((texto) => texto.trim())

export function cuerpoNuevo(form: FormApartamento): NuevoApartamento {
  return {
    codigo: `APT-${form.numero.trim()}`,
    nombre: form.nombre.trim(),
    descripcion: form.descripcion.trim(),
    capacidad: entero(form.capacidad),
    dormitorios: entero(form.dormitorios),
    imagenes: form.imagenes.map(({ url, principal }) => ({ url: url.trim(), principal })),
    caracteristicas: caracteristicasLimpias(form),
  }
}

export function cuerpoDatos(form: FormApartamento): DatosApartamento {
  return {
    nombre: form.nombre.trim(),
    descripcion: form.descripcion.trim(),
    dormitorios: entero(form.dormitorios),
    caracteristicas: caracteristicasLimpias(form),
  }
}

/**
 * Agrega una característica escrita, sin repetir (sin importar mayúsculas). Devuelve la lista nueva o el motivo por
 * el que no se agregó.
 */
export function agregarCaracteristica(lista: string[], texto: string): string[] | string {
  const limpia = texto.trim().replace(/\s+/g, ' ')
  if (!limpia) return 'Escribe la característica antes de agregarla.'
  if (limpia.length > MAXIMO_CARACTERISTICA) return `Hasta ${MAXIMO_CARACTERISTICA} caracteres por característica.`
  if (lista.some((actual) => actual.toLowerCase() === limpia.toLowerCase())) return 'Esa característica ya está.'
  return [...lista, limpia]
}

/** Un renglón más de foto en el formulario; el primero queda como principal, como lo hace la API (IMG-02). */
export function agregarFoto(lista: Imagen[]): Imagen[] {
  return lista.length >= MAXIMO_FOTOS ? lista : [...lista, { url: '', principal: lista.length === 0 }]
}

/** Quita el renglón `indice`; si era la principal, la toma el primero de los que quedan (IMG-02). */
export function quitarFoto(lista: Imagen[], indice: number): Imagen[] {
  const restantes = lista.filter((_, i) => i !== indice)
  const falta = !restantes.some((imagen) => imagen.principal)
  return restantes.map((imagen, i) => ({ ...imagen, principal: imagen.principal || (falta && i === 0) }))
}

/**
 * A qué campo del formulario pertenece un detalle de un 400 («imagenes[0].url: …», «caracteristicas[1]: …»). Nulo si
 * a ninguno.
 */
export function campoDelDetalle(detalle: string): CampoApartamento | null {
  const campo = detalle.split(':')[0].trim().replace(/\[\d+\].*$/, '')
  if (campo === 'codigo') return 'numero'
  return (Object.keys(FORM_VACIO) as CampoApartamento[]).includes(campo as CampoApartamento)
    ? (campo as CampoApartamento)
    : null
}

/** Lo que se ve de un detalle de un 400: el mensaje, sin la ruta del campo. */
export function mensajeDelDetalle(detalle: string): string {
  const separador = detalle.indexOf(': ')
  const mensaje = separador < 0 ? detalle : detalle.slice(separador + 2)
  return mensaje.charAt(0).toUpperCase() + mensaje.slice(1)
}

/**
 * Los bloqueos que todavía cuentan van arriba, por fecha: los vigentes cuyo fin (exclusivo) no ha llegado. Abajo, en
 * «Ver anteriores», los levantados y los ya terminados, del más reciente al más antiguo.
 */
export function separarBloqueos(bloqueos: Bloqueo[], hoy: string): { vigentes: Bloqueo[]; anteriores: Bloqueo[] } {
  const cuenta = (bloqueo: Bloqueo) => bloqueo.vigente && bloqueo.fechaFin > hoy
  return {
    vigentes: bloqueos.filter(cuenta).sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio)),
    anteriores: bloqueos.filter((b) => !cuenta(b)).sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio)),
  }
}

export interface FormBloqueo {
  inicio: string
  fin: string
  motivo: string
}

export function validarBloqueo(form: FormBloqueo): Errores<keyof FormBloqueo> {
  const errores: Errores<keyof FormBloqueo> = {}
  if (!form.inicio) errores.inicio = 'Elige la primera noche bloqueada.'
  if (!form.fin) errores.fin = 'Elige el día en que vuelve a venderse.'
  else if (form.inicio && form.fin <= form.inicio) errores.fin = '«Hasta» debe ser después de «Desde».'
  if (!form.motivo.trim()) errores.motivo = 'Escribe el motivo del bloqueo.'
  return errores
}

/** Lo que dice la sección Mascotas, o nulo si el hotel no las acepta. */
export function textoMascotas(apartamento: FichaApartamento, limiteDelHotel: number | null): string {
  const propio = apartamento.limiteMascotas
  if (propio === 0) return 'No admite mascotas'
  if (propio !== null) return `Hasta ${propio} ${propio === 1 ? 'mascota' : 'mascotas'}`
  if (limiteDelHotel === null) return 'El límite del hotel'
  return `Hasta ${limiteDelHotel} ${limiteDelHotel === 1 ? 'mascota' : 'mascotas'} (el límite del hotel)`
}

/** La foto principal, o la primera si ninguna lo es. */
export function fotoPrincipal(imagenes: Imagen[]): Imagen | undefined {
  return imagenes.find((imagen) => imagen.principal) ?? imagenes[0]
}

/** «6 personas», «1 dormitorio». */
export function personas(apartamento: Pick<FichaApartamento, 'capacidad'>): string {
  return `${apartamento.capacidad} ${apartamento.capacidad === 1 ? 'persona' : 'personas'}`
}

export function dormitorios(apartamento: Pick<FichaApartamento, 'dormitorios'>): string {
  return `${apartamento.dormitorios} ${apartamento.dormitorios === 1 ? 'dormitorio' : 'dormitorios'}`
}

/** Después de pintar los errores, el foco va al primero, para que el teclado y el lector de pantalla lleguen ahí. */
export function enfocarPrimerError(formulario: HTMLFormElement) {
  requestAnimationFrame(() => formulario.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus())
}
