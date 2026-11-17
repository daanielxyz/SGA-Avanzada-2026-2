// Los datos del hotel salen de la API, nunca del código: la misma web sirve a cualquier alojamiento (DEC-83).
import { pedir } from './cliente'

/**
 * `MarcaResponse`: la marca de un hotel (BL-05 · DEC-87). Todo opcional: lo que venga vacío queda con los valores
 * neutros del esqueleto.
 */
export interface Marca {
  colores?: {
    fondo?: string | null
    superficie?: string | null
    texto?: string | null
    textoSuave?: string | null
    borde?: string | null
    bordeCampo?: string | null
    acento?: string | null
    textoSobreAcento?: string | null
    peligro?: string | null
    exito?: string | null
  }
  fuentes?: { titulos?: string | null; texto?: string | null }
  radio?: { pequeno?: string | null; mediano?: string | null; grande?: string | null }
  logoUrl?: string | null
  portadaUrl?: string | null
}

/** De `AlojamientoResponse`, solo lo que usa la web por ahora. */
export interface Alojamiento {
  nombre: string
  descripcion?: string | null
  ciudad?: string | null
  parametros: {
    /** Desde esta edad, cumplida el día de llegada, se paga (RN-06). */
    umbralEdadFacturable: number
    /** `HH:mm:ss`: desde esta hora se puede entrar. */
    horaEntrada?: string
    /** `HH:mm:ss`: hasta esta hora se puede salir. */
    horaSalida?: string
    /** RP-02 y RP-03: lo que el huésped puede declarar al reservar. */
    cupoParqueadero?: number
    aceptaMascotas?: boolean
    limiteMascotas?: number
    cargoMascota?: number
    /** DEC-89: si los precios del parqueadero son por noche o por estadía; 0 es gratis. */
    cobroParqueadero?: 'POR_NOCHE' | 'POR_ESTADIA'
    cargoCarro?: number
    cargoMoto?: number
  }
  marca?: Marca | null
  /** Lo que muestra la presentación del hotel; todo puede faltar mientras el hotel no lo escribe. */
  direccion?: string | null
  /** 0 y 0 es el valor inicial: sin ubicación en el mapa. */
  latitud?: number
  longitud?: number
  normas?: string | null
  serviciosAdicionales?: { id: string; nombre: string; generaCargo: boolean; valor: number; activo: boolean }[]
  /** Códigos (`TRANSFERENCIA`, `PAGO_EN_LINEA`…): `nombreDelMedio` los pone en palabras. */
  mediosPago?: string[]
  /** Registro Nacional de Turismo, público (DEC-130); null si no se ha escrito. */
  rnt?: string | null
}

export function consultarAlojamiento() {
  return pedir<Alojamiento>('/api/alojamiento')
}

/** El valor inicial de `sga.alojamiento.*` mientras el hotel no escribe el suyo no se le muestra al huésped. */
export function escrito(dato: string | null | undefined) {
  const limpio = dato?.trim()
  return limpio && limpio !== 'Por definir' ? limpio : null
}

const MEDIOS: Record<string, string> = {
  EFECTIVO: 'Efectivo',
  TRANSFERENCIA: 'Transferencia',
  PAGO_EN_LINEA: 'Pago en línea',
  TARJETA: 'Tarjeta',
  TARJETA_CREDITO: 'Tarjeta de crédito',
  TARJETA_DEBITO: 'Tarjeta débito',
}

/** «PAGO_EN_LINEA» → «Pago en línea»; un medio que el hotel inventó se lee igual, sin guiones. */
export function nombreDelMedio(codigo: string) {
  const texto = codigo.replace(/_/g, ' ').toLowerCase()
  return MEDIOS[codigo] ?? texto.charAt(0).toUpperCase() + texto.slice(1)
}
