import { useState, type FormEvent } from 'react'
import Aviso from '../../componentes/Aviso'
import { cantidad, fechaHoy } from '../../formato'
import Campo from './Campo'
import {
  sinError,
  TIPOS_DE_DOCUMENTO,
  validarPersonas,
  type DatosOcupante,
  type DatosTitular,
  type Errores,
} from './formulario'

const TIPOS = TIPOS_DE_DOCUMENTO.map((tipo) => [tipo, tipo] as const)
const TIPOS_OPCIONALES = [['', 'Ninguno'] as const, ...TIPOS]

/** Paso 3: el titular y el grupo (OCU-02 · OCU-05 · DEC-97). El titular puede no alojarse. */
export default function PasoPersonas({ titular, grupo, titularSeAloja, entrada, alCambiarTitular, alCambiarOcupante, alCambiarSeAloja, alSeguir, alVolver }: {
  titular: DatosTitular
  grupo: DatosOcupante[]
  titularSeAloja: boolean
  entrada: string
  alCambiarTitular: (titular: DatosTitular) => void
  alCambiarOcupante: (indice: number, ocupante: DatosOcupante) => void
  alCambiarSeAloja: (seAloja: boolean) => void
  alSeguir: () => void
  alVolver: () => void
}) {
  const [errores, setErrores] = useState<Errores>({})
  const hoy = fechaHoy()
  const total = Object.keys(errores).length

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    const encontrados = validarPersonas(titular, grupo, entrada)
    setErrores(encontrados)
    if (Object.keys(encontrados).length === 0) alSeguir()
  }

  const delTitular = <K extends keyof DatosTitular>(campo: K) => ({
    value: titular[campo],
    error: errores[`titular.${campo}`],
    onChange: (evento: { target: { value: string } }) => {
      setErrores((actuales) => sinError(actuales, `titular.${campo}`))
      alCambiarTitular({ ...titular, [campo]: evento.target.value })
    },
  })

  return (
    <form className="paso" onSubmit={enviar} noValidate>
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
        <Campo etiqueta="Nombre completo" autoComplete="off" maxLength={120} {...delTitular('nombre')} />
        <div className="paso-campos-2">
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
          <p className="campo-pista">Nombre y fecha de nacimiento son obligatorios; documento y nacionalidad, opcionales.</p>
          {errores['grupo.mayor'] && <p className="campo-mensaje">{errores['grupo.mayor']}</p>}
        </div>
        {grupo.map((ocupante, indice) => {
          const esTitular = titularSeAloja && indice === 0
          const delOcupante = <K extends keyof DatosOcupante>(campo: K) => ({
            value: ocupante[campo],
            error: errores[`ocupante.${indice}.${campo}`],
            onChange: (evento: { target: { value: string } }) => {
              setErrores((actuales) => sinError(actuales, `ocupante.${indice}.${campo}`))
              alCambiarOcupante(indice, { ...ocupante, [campo]: evento.target.value })
            },
          })
          return (
            <fieldset key={indice} className="tarjeta ocupante">
              <legend>Ocupante {indice + 1}{esTitular && ' · titular'}</legend>
              <div className="ocupante-campos">
                <Campo etiqueta="Nombre completo" autoComplete="off" maxLength={120} readOnly={esTitular} {...delOcupante('nombre')} />
                <Campo etiqueta="Fecha de nacimiento" type="date" max={hoy} {...delOcupante('fechaNacimiento')} />
                <Campo etiqueta="Documento" nota="opcional" opciones={TIPOS_OPCIONALES} disabled={esTitular} {...delOcupante('tipoDocumento')} />
                <Campo etiqueta="Número" nota="opcional" autoComplete="off" maxLength={30} readOnly={esTitular} {...delOcupante('numeroDocumento')} />
                <Campo etiqueta="Nacionalidad" nota="opcional" autoComplete="off" maxLength={60} {...delOcupante('nacionalidad')} />
              </div>
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
