import type { ReactNode } from 'react'

// Íconos de trazo de los mockups (pms/diseno/mockups). Siempre decorativos: el texto de al lado dice qué son.
const TRAZOS = {
  reservas: (
    <>
      <rect x="3" y="4" width="18" height="17" rx="2" />
      <path d="M3 9h18M8 2v4M16 2v4" />
    </>
  ),
  titulares: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 4-6 8-6s8 2 8 6" />
    </>
  ),
  preparacion: (
    <>
      <path d="M3 18V7M3 14h18v4M21 14v-2a3 3 0 0 0-3-3h-7v5" />
      <circle cx="7" cy="11" r="1.5" />
    </>
  ),
  novedades: (
    <>
      <path d="M6 16V11a6 6 0 0 1 12 0v5l2 2H4l2-2z" />
      <path d="M10 21h4" />
    </>
  ),
  alojamiento: (
    <>
      <path d="M3 11l9-8 9 8" />
      <path d="M5 10v11h14V10M10 21v-6h4v6" />
    </>
  ),
  apartamentos: (
    <>
      <rect x="5" y="3" width="14" height="18" rx="1" />
      <path d="M9 7h2M13 7h2M9 11h2M13 11h2M10 21v-4h4v4" />
    </>
  ),
  tarifas: (
    <>
      <path d="M3 12V4h8l10 10-8 8L3 12z" />
      <circle cx="7.5" cy="8.5" r="1.2" />
    </>
  ),
  politica: (
    <>
      <path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3z" />
      <path d="M9 12l2 2 4-4" />
    </>
  ),
  usuarios: (
    <>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2 20c0-3.5 3-5.5 7-5.5s7 2 7 5.5" />
      <path d="M16 4.5a3.5 3.5 0 0 1 0 7M18 15c2.5.6 4 2.3 4 5" />
    </>
  ),
  canales: (
    <>
      <circle cx="6" cy="12" r="3" />
      <circle cx="18" cy="5" r="3" />
      <circle cx="18" cy="19" r="3" />
      <path d="M8.6 10.5l6.8-4M8.6 13.5l6.8 4" />
    </>
  ),
  perfil: (
    <>
      <circle cx="12" cy="12" r="9" />
      <circle cx="12" cy="10" r="3" />
      <path d="M6.5 18.5c1.5-2.5 3.5-3.5 5.5-3.5s4 1 5.5 3.5" />
    </>
  ),
  candado: (
    <>
      <rect x="5" y="11" width="14" height="10" rx="2" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" />
    </>
  ),
  construccion: <path d="M14.5 6.5l3 3M4 20l1-4L16 5l3 3L8 19l-4 1z" />,
  noEncontrada: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="M20 20l-4-4M9 9l4 4M13 9l-4 4" />
    </>
  ),
  buscar: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="M20 20l-4-4" />
    </>
  ),
  reservaNoEncontrada: (
    <>
      <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
      <path d="M14 3v5h5M9.5 13.5l5 5M14.5 13.5l-5 5" />
    </>
  ),
  abajo: <path d="M6 9l6 6 6-6" />,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  cerrar: <path d="M6 6l12 12M18 6L6 18" />,
} satisfies Record<string, ReactNode>

export type NombreIcono = keyof typeof TRAZOS

export default function Icono({ nombre }: { nombre: NombreIcono }) {
  return (
    <svg className="icono" viewBox="0 0 24 24" aria-hidden="true">
      {TRAZOS[nombre]}
    </svg>
  )
}
