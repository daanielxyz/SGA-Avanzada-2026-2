// Un alojamiento completo, como lo devuelve `GET /api/alojamiento`, para las pruebas de su pantalla.
import type { Alojamiento, Parametros } from '../api/alojamiento'

export const PARAMETROS: Parametros = {
  umbralEdadFacturable: 12,
  horaEntrada: '15:00:00',
  horaSalida: '12:00:00',
  tiempoPreparacionHoras: 3,
  plazoConfirmacionHoras: 24,
  horaLimiteNoShow: '22:00:00',
  anticipoPct: 30,
  minimoMediosPago: 2,
  minimoServiciosAdicionales: 2,
  minimoTemporadas: 3,
  minimoTramosCancelacion: 2,
  minimoCapacidadesDistintas: 2,
  pagoEnLinea: 'ANTICIPO',
  plazoPagoEnLineaMinutos: 30,
  cupoParqueadero: 8,
  aceptaMascotas: true,
  limiteMascotas: 2,
  cargoMascota: 30000,
  cobroParqueadero: 'POR_NOCHE',
  cargoCarro: 15000,
  cargoMoto: 8000,
}

export function alojamientoDePrueba(cambios: Partial<Alojamiento> = {}): Alojamiento {
  return {
    nombre: 'Puerta al Sol',
    descripcion: 'Apartamentos en la vereda.',
    ciudad: 'Salento (Quindío)',
    direccion: 'Vereda Palestina km 2',
    normas: null,
    latitud: 4.6372,
    longitud: -75.5701,
    parametros: PARAMETROS,
    mediosPago: ['EFECTIVO', 'TARJETA', 'TRANSFERENCIA'],
    serviciosAdicionales: [
      { id: 'SRV-1', nombre: 'Desayuno', generaCargo: true, valor: 25000, activo: true },
      { id: 'SRV-2', nombre: 'Wifi', generaCargo: false, valor: 0, activo: true },
      { id: 'SRV-3', nombre: 'Transporte al pueblo', generaCargo: true, valor: 20000, activo: false },
    ],
    marca: {
      colores: {
        fondo: '#FBF7F0', superficie: '#FFFFFF', texto: '#2B2118', textoSuave: '#6E6257', borde: '#E6DDD0',
        bordeCampo: '#9C8E7E', acento: '#C9762B', textoSobreAcento: '#FFFFFF', peligro: '#B3261E', exito: '#2E7D32',
      },
      fuentes: { titulos: 'Lora', texto: 'Inter' },
      radio: { pequeno: '4px', mediano: '8px', grande: '16px' },
      logoUrl: null,
      portadaUrl: null,
    },
    ...cambios,
  }
}
