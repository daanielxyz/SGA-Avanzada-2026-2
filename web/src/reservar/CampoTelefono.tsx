import {
  getCountries,
  getCountryCallingCode,
  isValidPhoneNumber,
  parsePhoneNumberFromString,
  type CountryCode,
} from 'libphonenumber-js/min'
import { useEffect, useId, useMemo, useRef, useState, type KeyboardEvent } from 'react'

// Windows no trae emojis de banderas, así que van como imágenes propias. `no-inline` deja cada una en su archivo: el
// navegador solo descarga las que se ven, y la página no carga con las de todos los países.
const IMAGENES = import.meta.glob<string>('../../node_modules/flag-icons/flags/4x3/*.svg', {
  eager: true,
  exhaustive: true,
  query: '?no-inline',
  import: 'default',
})
const BANDERAS = Object.fromEntries(
  Object.entries(IMAGENES).map(([ruta, url]) => [ruta.split('/').pop()?.replace('.svg', ''), url]),
)

// TODO(equipo): cuando se venda fuera de Colombia, el país inicial debe llegar como configuración del alojamiento.
const PAIS_INICIAL: CountryCode = 'CO'

interface Pais {
  codigo: CountryCode
  nombre: string
  prefijo: string
}

const nombres = new Intl.DisplayNames(['es'], { type: 'region' })
const sinTildes = (texto: string) => texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()

const PAISES: Pais[] = getCountries()
  .map((codigo) => ({ codigo, nombre: nombres.of(codigo) ?? codigo, prefijo: getCountryCallingCode(codigo) }))
  .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
const PRIMERO = PAISES.find((p) => p.codigo === PAIS_INICIAL) as Pais
const LISTA = [PRIMERO, ...PAISES.filter((p) => p !== PRIMERO)]

function Bandera({ codigo }: { codigo: CountryCode }) {
  const url = BANDERAS[codigo.toLowerCase()]
  // Unos pocos territorios no tienen imagen: se deja el espacio para que la lista no se desalinee
  return url ? <img className="bandera" src={url} alt="" loading="lazy" /> : <span className="bandera" />
}

interface Props {
  /** Recibe el número completo con su prefijo (`+573001234567`), o vacío mientras no sea válido. */
  alCambiar: (telefono: string) => void
}

/** El teléfono con su país: cerrado muestra bandera y prefijo; abierto, también el nombre, con un buscador. */
export default function CampoTelefono({ alCambiar }: Props) {
  const id = useId()
  const [pais, setPais] = useState<Pais>(PRIMERO)
  const [numero, setNumero] = useState('')
  const [tocado, setTocado] = useState(false)
  const [abierto, setAbierto] = useState(false)
  const [busqueda, setBusqueda] = useState('')
  const [activo, setActivo] = useState(0)
  const contenedor = useRef<HTMLDivElement>(null)
  const selector = useRef<HTMLButtonElement>(null)
  const campo = useRef<HTMLInputElement>(null)
  const buscador = useRef<HTMLInputElement>(null)
  const lista = useRef<HTMLUListElement>(null)

  const visibles = useMemo(() => {
    const texto = sinTildes(busqueda.trim()).replace(/^\+/, '')
    return LISTA.filter((p) => sinTildes(p.nombre).includes(texto) || p.prefijo.startsWith(texto))
  }, [busqueda])

  const invalido = numero.trim() !== '' && !isValidPhoneNumber(numero, pais.codigo)
  const mensaje = `Revisa el número: no parece de ${pais.nombre}.`

  // Así el navegador no deja enviar el formulario con un número que no sirve
  useEffect(() => {
    campo.current?.setCustomValidity(invalido ? mensaje : '')
  }, [invalido, mensaje])

  useEffect(() => {
    if (!abierto) return
    // En el celular no se abre el teclado de una vez: taparía la lista
    const conTeclado = window.matchMedia?.('(pointer: coarse)').matches !== true
    if (conTeclado) buscador.current?.focus()
    const alTocarAfuera = (evento: PointerEvent) => {
      if (!contenedor.current?.contains(evento.target as Node)) setAbierto(false)
    }
    document.addEventListener('pointerdown', alTocarAfuera)
    return () => document.removeEventListener('pointerdown', alTocarAfuera)
  }, [abierto])

  useEffect(() => {
    if (abierto) lista.current?.children[activo]?.scrollIntoView?.({ block: 'nearest' })
  }, [abierto, activo])

  function avisar(nuevoNumero: string, nuevoPais: Pais) {
    const valido = isValidPhoneNumber(nuevoNumero, nuevoPais.codigo)
    alCambiar(valido ? parsePhoneNumberFromString(nuevoNumero, nuevoPais.codigo)?.number ?? '' : '')
  }

  function escribir(texto: string) {
    const limpio = texto.replace(/[^\d+\s()-]/g, '')
    // Lo que autocompleta el navegador o se pega trae el prefijo: el país se ajusta solo
    const completo = limpio.startsWith('+') ? parsePhoneNumberFromString(limpio) : undefined
    const delNumero = PAISES.find((p) => p.codigo === completo?.country)
    if (completo && delNumero) {
      setPais(delNumero)
      setNumero(completo.nationalNumber)
      avisar(completo.nationalNumber, delNumero)
      return
    }
    setNumero(limpio)
    avisar(limpio, pais)
  }

  function abrir() {
    setBusqueda('')
    setActivo(Math.max(0, LISTA.indexOf(pais)))
    setAbierto(true)
  }

  function elegir(elegido: Pais) {
    setPais(elegido)
    setAbierto(false)
    avisar(numero, elegido)
    campo.current?.focus()
  }

  function alTeclear(evento: KeyboardEvent<HTMLInputElement>) {
    if (evento.key === 'ArrowDown' || evento.key === 'ArrowUp') {
      evento.preventDefault()
      const paso = evento.key === 'ArrowDown' ? 1 : -1
      setActivo((a) => Math.min(Math.max(a + paso, 0), visibles.length - 1))
    } else if (evento.key === 'Enter') {
      evento.preventDefault() // aquí Enter elige el país; no debe enviar el formulario
      if (visibles[activo]) elegir(visibles[activo])
    } else if (evento.key === 'Escape') {
      setAbierto(false)
      selector.current?.focus()
    } else if (evento.key === 'Tab') {
      setAbierto(false)
    }
  }

  return (
    <div className="campo campo-telefono" ref={contenedor}>
      <label htmlFor={`${id}-numero`}>Teléfono</label>
      <div className="telefono">
        <button type="button" ref={selector} className="selector-pais" aria-haspopup="listbox" aria-expanded={abierto}
          aria-label={`País del teléfono: ${pais.nombre}, +${pais.prefijo}`}
          onClick={() => (abierto ? setAbierto(false) : abrir())}>
          <Bandera codigo={pais.codigo} />
          <span>+{pais.prefijo}</span>
          <span className="flecha-pais" aria-hidden="true" />
        </button>
        <input id={`${id}-numero`} ref={campo} type="tel" inputMode="tel" value={numero} maxLength={25} required
          autoComplete="tel" aria-invalid={tocado && invalido} aria-describedby={`${id}-ayuda`}
          onBlur={() => setTocado(true)} onChange={(e) => escribir(e.target.value)} />
      </div>
      <span id={`${id}-ayuda`} className={tocado && invalido ? 'ayuda ayuda-error' : 'ayuda'}>
        {tocado && invalido ? mensaje : 'Con él recuperas tu cuenta.'}
      </span>

      {abierto && (
        <div className="paises">
          <input ref={buscador} type="search" value={busqueda} role="combobox" aria-expanded="true"
            aria-controls={`${id}-paises`} aria-label="Buscar país" placeholder="Buscar país" autoComplete="off"
            aria-activedescendant={visibles[activo] ? `${id}-${visibles[activo].codigo}` : undefined}
            onKeyDown={alTeclear}
            onChange={(e) => {
              setBusqueda(e.target.value)
              setActivo(0)
            }} />
          <ul id={`${id}-paises`} ref={lista} role="listbox" aria-label="Países">
            {visibles.map((p, i) => (
              <li key={p.codigo} id={`${id}-${p.codigo}`} role="option" aria-selected={p === pais}
                className={i === activo ? 'pais activo' : 'pais'} onClick={() => elegir(p)}>
                <Bandera codigo={p.codigo} />
                <span className="pais-nombre">{p.nombre}</span>
                <span className="pais-prefijo">+{p.prefijo}</span>
              </li>
            ))}
            {visibles.length === 0 && <li className="pais-vacio">No encontramos ese país.</li>}
          </ul>
        </div>
      )}
    </div>
  )
}
