import { useCallback, type ClipboardEvent } from 'react'
import { guardarDatos, guardarUbicacion } from '../api/alojamiento'
import { Bloque, CampoBloque } from './Bloque'
import { useBloque, type AlBloque } from './useBloque'
import {
  LARGO_DATOS, cuerpoDatos, cuerpoUbicacion, formDatos, formUbicacion, repartirPegado, validarDatos, validarUbicacion,
} from './formularios'

/** Nombre, descripción, ciudad, dirección y normas de la casa (CU-50). */
export function BloqueDatos(props: AlBloque) {
  const { form, cambiar, errores, estado, rechazo, sucio, guardar } = useBloque(props, { desde: formDatos, validar: validarDatos })
  return (
    <Bloque
      titulo="Datos"
      descripcion="Lo que ven los huéspedes en la web de reservas."
      estado={estado}
      sucio={sucio}
      rechazo={rechazo}
      guardado="Datos guardados"
      boton="Guardar datos"
      alEnviar={(evento) => guardar(evento, 'No se guardaron los datos', (f) => guardarDatos(cuerpoDatos(f)))}
    >
      <div className="campos-bloque">
        <CampoBloque etiqueta="Nombre" valor={form.nombre} maximo={LARGO_DATOS.nombre} error={errores.nombre}
          autoComplete="organization" alCambiar={(valor) => cambiar('nombre', valor)} />
        <CampoBloque etiqueta="Ciudad" valor={form.ciudad} maximo={LARGO_DATOS.ciudad} error={errores.ciudad}
          alCambiar={(valor) => cambiar('ciudad', valor)} />
        <div className="campo-ancho">
          <CampoBloque etiqueta="Dirección" valor={form.direccion} maximo={LARGO_DATOS.direccion} error={errores.direccion}
            alCambiar={(valor) => cambiar('direccion', valor)} />
        </div>
        <div className="campo-ancho">
          <CampoBloque etiqueta="Descripción" largo valor={form.descripcion} error={errores.descripcion}
            alCambiar={(valor) => cambiar('descripcion', valor)} />
        </div>
        <div className="campo-ancho">
          <CampoBloque etiqueta="Normas de la casa" nota="opcional" largo valor={form.normas}
            pista="Horarios de silencio, visitas, fumar… Las ven los huéspedes antes de reservar."
            alCambiar={(valor) => cambiar('normas', valor)} />
        </div>
      </div>
    </Bloque>
  )
}

/** Latitud y longitud, que se guardan aparte. Sin mapa: no hay un servicio de mapas conectado. */
export function BloqueUbicacion(props: AlBloque) {
  const { form, setForm, cambiar, errores, estado, rechazo, sucio, guardar } = useBloque(props, {
    desde: formUbicacion,
    validar: validarUbicacion,
  })

  // Google Maps copia los dos números juntos: se reparten solos
  const alPegar = useCallback((evento: ClipboardEvent<HTMLInputElement>) => {
    const repartido = repartirPegado(evento.clipboardData.getData('text'))
    if (!repartido) return
    evento.preventDefault()
    setForm(repartido)
  }, [setForm])

  return (
    <Bloque
      titulo="Ubicación"
      descripcion="Cópiala de Google Maps: clic derecho sobre el lugar y clic en los números. Si pegas los dos juntos, se reparten solos."
      estado={estado}
      sucio={sucio}
      rechazo={rechazo}
      guardado="Ubicación guardada"
      boton="Guardar ubicación"
      alEnviar={(evento) => guardar(evento, 'No se guardó la ubicación', (f) => {
        const { latitud, longitud } = cuerpoUbicacion(f)
        return guardarUbicacion(latitud, longitud)
      })}
    >
      <div className="campos-bloque">
        <CampoBloque etiqueta="Latitud" valor={form.latitud} error={errores.latitud}
          placeholder="4.6372" onPaste={alPegar} alCambiar={(valor) => cambiar('latitud', valor)} />
        <CampoBloque etiqueta="Longitud" valor={form.longitud} error={errores.longitud}
          placeholder="-75.5701" onPaste={alPegar} alCambiar={(valor) => cambiar('longitud', valor)} />
      </div>
    </Bloque>
  )
}
