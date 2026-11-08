// Reservas simuladas que comparten las pruebas de las pantallas de recepción.
import type { Pagina, Reserva } from '../api/reservas'

export function reservaDePrueba(cambios: Partial<Reserva> = {}): Reserva {
  return {
    codigo: 'RES-2026-00042',
    apartamento: 'APT-101',
    titularId: 'TIT-7',
    titularNombre: 'Ana Ríos',
    entrada: '2026-11-14',
    salida: '2026-11-17',
    estado: 'NO_SHOW',
    canalOrigen: 'PORTAL',
    ocupantes: [
      {
        id: 'OCU-1', nombre: 'Ana Ríos', fechaNacimiento: '1990-03-02',
        tipoDocumento: 'CC', numeroDocumento: '52481', nacionalidad: 'Colombia',
      },
      {
        id: 'OCU-2', nombre: 'Luis Ríos', fechaNacimiento: '2016-11-14',
        tipoDocumento: 'CC', numeroDocumento: '1094', nacionalidad: 'Colombia',
      },
    ],
    horaEstimadaLlegada: null,
    valor: {
      desglose: [
        { noche: '2026-11-14', temporada: 'Alta', tarifa: 250000, ocupantesFacturables: 2, subtotal: 500000 },
        { noche: '2026-11-15', temporada: 'Alta', tarifa: 250000, ocupantesFacturables: 2, subtotal: 500000 },
        { noche: '2026-11-16', temporada: 'Baja', tarifa: 125000, ocupantesFacturables: 2, subtotal: 250000 },
      ],
      total: 1250000,
    },
    politicaId: 'POL-3',
    creadaEn: '2026-10-18T09:15:00',
    vehiculos: 0,
    carros: 0,
    motos: 0,
    mascotas: 0,
    extras: { parqueadero: 0, mascotas: 0, total: 0 },
    anticipoExigido: null,
    ...cambios,
  }
}

export function paginaDe<T>(contenido: T[]): Pagina<T> {
  return {
    contenido,
    numero: 0,
    tamano: 10,
    totalElementos: contenido.length,
    totalPaginas: contenido.length > 0 ? 1 : 0,
  }
}
