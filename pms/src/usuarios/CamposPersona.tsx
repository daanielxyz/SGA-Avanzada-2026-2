import { useId, type ReactNode } from 'react'
import { CampoConContador } from '../apartamentos/DialogoFormulario'
import { fechaHoy } from '../formato'
import Campo from '../reserva/nueva/Campo'
import {
  MAXIMO_DOCUMENTO,
  MAXIMO_NOMBRE,
  MAXIMO_TELEFONO,
  TIPOS_DOCUMENTO,
  type CampoPersona,
  type Errores,
  type FormPersona,
} from './logica'

/** Un dato que aquí no se cambia (el correo, el rol): texto con borde punteado, no un campo deshabilitado. */
export function DatoSoloLectura({ rotulo, valor, pista }: { rotulo: string; valor: string; pista?: string }) {
  return (
    <div className="campo">
      <p className="campo-rotulo">{rotulo}</p>
      <p className="dato-solo-lectura texto-largo">{valor}</p>
      {pista && <p className="campo-pista">{pista}</p>}
    </div>
  )
}

/**
 * Nombre, teléfono, documento y fecha de nacimiento: los mismos en «Nuevo usuario», «Editar usuario» y «Mi perfil».
 *
 * @param enRejilla en «Mi perfil» van de a dos por fila en el computador
 */
export default function CamposPersona({ form, errores, deshabilitado, focoInicial = false, enRejilla = false,
  despuesDelNombre, alCambiar }: {
  form: FormPersona
  errores: Errores<CampoPersona>
  deshabilitado?: boolean
  focoInicial?: boolean
  enRejilla?: boolean
  /** El correo, que en el diálogo va justo debajo del nombre. */
  despuesDelNombre?: ReactNode
  alCambiar: (cambio: Partial<FormPersona>) => void
}) {
  const id = useId()
  const errorDocumento = errores.tipoDocumento ?? errores.numeroDocumento
  return (
    <div className={enRejilla ? 'campos-persona campos-persona-rejilla' : 'campos-persona'}>
      <CampoConContador id={`${id}-nombre`} rotulo="Nombre" valor={form.nombre} maximo={MAXIMO_NOMBRE}
        error={errores.nombre} focoInicial={focoInicial} deshabilitado={deshabilitado}
        alCambiar={(nombre) => alCambiar({ nombre })} />
      {despuesDelNombre}
      <Campo etiqueta="Teléfono" type="tel" inputMode="tel" autoComplete="off" maxLength={MAXIMO_TELEFONO}
        value={form.telefono} error={errores.telefono} disabled={deshabilitado}
        onChange={(e) => alCambiar({ telefono: e.target.value })} />
      <fieldset className="campo campo-documento" disabled={deshabilitado}
        aria-describedby={errorDocumento ? `${id}-documento-error` : undefined}>
        <legend className="campo-rotulo">Documento</legend>
        <div className="campo-documento-fila">
          <label className="solo-lectores" htmlFor={`${id}-tipo`}>Tipo de documento</label>
          <select id={`${id}-tipo`} className="campo-control" value={form.tipoDocumento}
            aria-invalid={Boolean(errores.tipoDocumento)}
            onChange={(e) => alCambiar({ tipoDocumento: e.target.value as FormPersona['tipoDocumento'] })}>
            {TIPOS_DOCUMENTO.map(([valor, texto]) => (
              <option key={valor} value={valor} disabled={valor === ''}>{texto}</option>
            ))}
          </select>
          <label className="solo-lectores" htmlFor={`${id}-numero`}>Número de documento</label>
          <input id={`${id}-numero`} className="campo-control" placeholder="Número" autoComplete="off"
            maxLength={MAXIMO_DOCUMENTO} value={form.numeroDocumento} aria-invalid={Boolean(errores.numeroDocumento)}
            onChange={(e) => alCambiar({ numeroDocumento: e.target.value })} />
        </div>
        {errorDocumento && <p className="campo-mensaje" id={`${id}-documento-error`}>{errorDocumento}</p>}
      </fieldset>
      <Campo etiqueta="Fecha de nacimiento" type="date" max={fechaHoy()} value={form.fechaNacimiento}
        error={errores.fechaNacimiento} disabled={deshabilitado}
        onChange={(e) => alCambiar({ fechaNacimiento: e.target.value })} />
    </div>
  )
}
