import { useEffect, useRef, useState, type FormEvent } from 'react'
import Aviso from '../../componentes/Aviso'
import { cantidad, fechaHoy } from '../../formato'
import Campo from './Campo'
import {
  esAdultoAlEntrar,
  MAXIMO_APELLIDO,
  MAXIMO_NOMBRES,
  MAYORIA_DE_EDAD,
  sinError,
  TIPOS_DE_DOCUMENTO,
  TIPOS_DE_DOCUMENTO_OCUPANTE,
  tiposPorEdad,
  validarPersonas,
  type DatosOcupante,
  type DatosTitular,
  type Errores,
} from './formulario'

const TIPOS = TIPOS_DE_DOCUMENTO.map((tipo) => [tipo, tipo] as const)
const TIPOS_OCUPANTE = [['', 'Elige'] as const, ...TIPOS_DE_DOCUMENTO_OCUPANTE]

/**
 * Paso 3: el titular, con nombres y apellidos aparte (DEC-98), y el grupo con su huésped principal (OCU-02 · OCU-05 ·
 * DEC-97). El titular puede no alojarse.
 */
export default function PasoPersonas({ titular, grupo, titularSeAloja, principal, entrada, alCambiarTitular, alCambiarOcupante, alCambiarSeAloja, alElegirPrincipal, alSeguir, alVolver }: {
  titular: DatosTitular
  grupo: DatosOcupante[]
  titularSeAloja: boolean
  /** La posición del huésped principal; nula mientras no haya un adulto. */
  principal: number | null
  entrada: string
  alElegirPrincipal: (indice: number) => void
  alCambiarTitular: (titular: DatosTitular) => void
  alCambiarOcupante: (indice: number, ocupante: DatosOcupante) => void
  alCambiarSeAloja: (seAloja: boolean) => void
  alSeguir: () => void
  alVolver: () => void
}) {
  const [errores, setErrores] = useState<Errores>({})
  // Cada envío con errores lleva el foco al primer campo marcado, ya dibujado como inválido
  const [intentoFallido, setIntentoFallido] = useState(0)
  const formulario = useRef<HTMLFormElement>(null)
  const hoy = fechaHoy()
  const total = Object.keys(errores).length
  const adultos = grupo.filter((ocupante) => esAdultoAlEntrar(ocupante, entrada)).length

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    const encontrados = validarPersonas(titular, grupo, entrada)
    setErrores(encontrados)
    if (Object.keys(encontrados).length === 0) alSeguir()
    else setIntentoFallido((intento) => intento + 1)
  }

  useEffect(() => {
    if (intentoFallido > 0) formulario.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
  }, [intentoFallido])

  const delTitular = <K extends keyof DatosTitular>(campo: K) => ({
    value: titular[campo],
    error: errores[`titular.${campo}`],
    onChange: (evento: { target: { value: string } }) => {
      setErrores((actuales) => sinError(actuales, `titular.${campo}`))
      alCambiarTitular({ ...titular, [campo]: evento.target.value })
    },
  })

  return (
    <form className="paso" onSubmit={enviar} noValidate ref={formulario}>
      <h2 className="solo-lectores" tabIndex={-1}>Personas</h2>
      {total > 0 && (
        <Aviso
          tipo="error"
          asunto={`Revisa ${cantidad(total, 'campo marcado', 'campos marcados')}`}
          mensaje="Los errores están junto a cada campo."
        />
      )}

      <section className="tarjeta paso-tarjeta" aria-labelledby="titulo-titular">
        <h3 id="titulo-titular">Titular</h3>
        <Campo etiqueta="Nombres" autoComplete="off" maxLength={MAXIMO_NOMBRES} contador {...delTitular('nombres')} />
        <div className="paso-campos-2">
          <Campo etiqueta="Primer apellido" autoComplete="off" maxLength={MAXIMO_APELLIDO} contador {...delTitular('primerApellido')} />
          <Campo
            etiqueta="Segundo apellido"
            nota="opcional"
            autoComplete="off"
            maxLength={MAXIMO_APELLIDO}
            contador
            {...delTitular('segundoApellido')}
          />
          <Campo etiqueta="Tipo de documento" opciones={TIPOS} {...delTitular('tipoDocumento')} />
          <Campo etiqueta="Número de documento" autoComplete="off" maxLength={30} {...delTitular('numeroDocumento')} />
          <Campo etiqueta="Correo electrónico" type="email" autoComplete="off" maxLength={150} {...delTitular('correo')} />
          <Campo etiqueta="Teléfono" type="tel" autoComplete="off" maxLength={30} {...delTitular('telefono')} />
        </div>
        <div>
          <label className="casilla">
            <input
              type="checkbox"
              checked={titularSeAloja}
              onChange={(evento) => alCambiarSeAloja(evento.target.checked)}
            />
            El titular también se aloja
          </label>
          <p className="campo-pista">
            {titularSeAloja
              ? 'Sus datos llenan el ocupante 1.'
              : 'El titular responde por la reserva y su folio; puede no alojarse, por ejemplo si reserva para otra persona.'}
          </p>
        </div>
      </section>

      <section className="paso" aria-labelledby="titulo-ocupantes-nuevos">
        <div>
          <h3 id="titulo-ocupantes-nuevos">Ocupantes ({grupo.length})</h3>
          <p className="campo-pista">Nombre, fecha de nacimiento y documento son obligatorios; la nacionalidad, opcional. Un niño menor de 7 años va con su registro civil.</p>
          <p className="campo-pista">El huésped principal responde por el grupo en el reporte a Migración (TRA). Elígelo entre los adultos: debe tener {MAYORIA_DE_EDAD} años o más el día de entrada.</p>
          {errores['grupo.mayor'] && <p className="campo-mensaje">{errores['grupo.mayor']} Revisa las fechas marcadas.</p>}
        </div>
        {grupo.map((ocupante, indice) => {
          const esTitular = titularSeAloja && indice === 0
          const esPrincipal = indice === principal
          const delOcupante = <K extends keyof DatosOcupante>(campo: K) => ({
            value: ocupante[campo],
            error: errores[`ocupante.${indice}.${campo}`],
            onChange: (evento: { target: { value: string } }) => {
              setErrores((actuales) => sinError(actuales, `ocupante.${indice}.${campo}`))
              alCambiarOcupante(indice, { ...ocupante, [campo]: evento.target.value })
            },
          })
          return (
            <fieldset key={indice} className={`tarjeta ocupante${esPrincipal ? ' ocupante-elegido' : ''}`}>
              <legend>Ocupante {indice + 1}{esTitular && ' · titular'}</legend>
              <div className="ocupante-campos">
                <Campo etiqueta="Nombre completo" autoComplete="off" maxLength={120} readOnly={esTitular} {...delOcupante('nombre')} />
                <Campo
                  etiqueta="Fecha de nacimiento"
                  type="date"
                  max={hoy}
                  invalido={Boolean(errores['grupo.mayor'])}
                  {...delOcupante('fechaNacimiento')}
                  onChange={(evento) => {
                    setErrores((actuales) => sinError(sinError(actuales, `ocupante.${indice}.fechaNacimiento`), 'grupo.mayor'))
                    alCambiarOcupante(indice, { ...ocupante, fechaNacimiento: evento.target.value })
                  }}
                />
                <Campo
                  etiqueta="Documento"
                  opciones={tiposPorEdad(TIPOS_OCUPANTE, ocupante.fechaNacimiento, entrada, ocupante.tipoDocumento)}
                  disabled={esTitular}
                  {...delOcupante('tipoDocumento')}
                />
                <Campo etiqueta="Número" autoComplete="off" maxLength={30} readOnly={esTitular} {...delOcupante('numeroDocumento')} />
                <Campo etiqueta="Nacionalidad" nota="opcional" autoComplete="off" maxLength={60} {...delOcupante('nacionalidad')} />
              </div>
              {/* DEC-97: solo un adulto al entrar puede ser el principal; todas las opciones comparten nombre: se elige una */}
              {esAdultoAlEntrar(ocupante, entrada) && (
                <div className="ocupante-pie">
                  <label className="casilla ocupante-opcion">
                    <input
                      type="radio"
                      name="huesped-principal"
                      checked={esPrincipal}
                      aria-describedby={adultos === 1 ? `principal-unico-${indice}` : undefined}
                      onChange={() => alElegirPrincipal(indice)}
                    />
                    Huésped principal
                  </label>
                  {adultos === 1 && (
                    <p className="campo-pista" id={`principal-unico-${indice}`}>
                      Es el único mayor de edad del grupo, así que no se puede cambiar.
                    </p>
                  )}
                </div>
              )}
            </fieldset>
          )
        })}
      </section>

      <div className="paso-pie">
        <button type="button" className="boton boton-secundario" onClick={alVolver}>
          <span aria-hidden="true">←</span> Atrás
        </button>
        <button type="submit" className="boton">
          Siguiente: resumen <span aria-hidden="true">→</span>
        </button>
      </div>
    </form>
  )
}
