import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router'
import type { Pagina } from '../api/reservas'
import { listarUsuarios, type Usuario } from '../api/usuarios'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import { errorParaMostrar } from '../reserva/mensajes'
import { useSesion } from '../sesion/contexto'
import { NOMBRE_DEL_ROL } from '../sesion/roles'
import { DialogoEliminarUsuario, DialogoUsuario } from '../usuarios/Dialogos'
import EstadoCuenta, { Correo } from '../usuarios/EstadoCuenta'

type Resultado = { clave: string; pagina: Pagina<Usuario> } | { clave: string; error: unknown }

type Abierto = { nuevo: true } | { editar: Usuario } | { eliminar: Usuario }

/**
 * Las cuentas del personal, de 10 en 10 y por nombre, también las eliminadas (sprint 4, solo el administrador). La
 * propia dice «Tú» y no se puede eliminar desde aquí.
 */
export default function Usuarios() {
  const { sesion, terminar, renombrar } = useSesion()
  const [parametros, setParametros] = useSearchParams()
  const numero = Math.max(0, Number(parametros.get('pagina')) || 0)
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [abierto, setAbierto] = useState<Abierto | null>(null)
  const [hecho, setHecho] = useState<{ asunto: string; mensaje: string } | null>(null)
  const titulo = useRef<HTMLHeadingElement>(null)
  const clave = `${numero}#${intento}`
  useTitulo('Usuarios')

  useEffect(() => {
    let vigente = true
    listarUsuarios(numero)
      .then((pagina) => vigente && setResultado({ clave, pagina }))
      .catch((error: unknown) => vigente && setResultado({ clave, error }))
    return () => {
      vigente = false
    }
  }, [numero, clave])

  const actual = resultado?.clave === clave ? resultado : null
  const pagina = actual && 'pagina' in actual ? actual.pagina : null
  const esPropio = (usuario: Usuario) => usuario.id === sesion?.usuario

  const cerrar = () => setAbierto(null)
  const terminarCon = (aviso: { asunto: string; mensaje: string }, cambiado?: Usuario) => {
    if (pagina && cambiado) {
      setResultado({ clave, pagina: { ...pagina, contenido: pagina.contenido.map((u) => (u.id === cambiado.id ? cambiado : u)) } })
    }
    setHecho(aviso)
    setAbierto(null)
    // Si el botón que se usó ya no está (una cuenta eliminada no tiene acciones), el foco vuelve al título
    requestAnimationFrame(() => {
      if (!document.activeElement || document.activeElement === document.body) titulo.current?.focus()
    })
  }

  const acciones = (usuario: Usuario) => !usuario.eliminado && (
    <div className="usuario-acciones">
      <button type="button" className="boton boton-secundario boton-chico" aria-label={`Editar a ${usuario.nombre}`}
        onClick={() => setAbierto({ editar: usuario })}>
        Editar
      </button>
      {!esPropio(usuario) && (
        <button type="button" className="boton boton-peligro-borde boton-chico" aria-label={`Eliminar a ${usuario.nombre}`}
          onClick={() => setAbierto({ eliminar: usuario })}>
          Eliminar
        </button>
      )}
    </div>
  )
  const nombre = (usuario: Usuario) => (
    <span className="usuario-nombre">
      <span className="texto-largo">{usuario.nombre}</span>
      {esPropio(usuario) && <span className="marca-tu">Tú</span>}
    </span>
  )

  return (
    <div className="usuarios">
      <div className="reservas-encabezado">
        <h1 ref={titulo} tabIndex={-1}>Usuarios</h1>
        <button type="button" className="boton" onClick={() => setAbierto({ nuevo: true })}>
          <span aria-hidden="true">+</span> Nuevo usuario
        </button>
      </div>

      {hecho && <Aviso tipo="exito" asunto={hecho.asunto} mensaje={hecho.mensaje} />}
      {!actual && <Cargando />}
      {actual && 'error' in actual && (
        <Aviso
          tipo="error"
          asunto="No pudimos cargar los usuarios"
          mensaje={errorParaMostrar(actual.error).mensaje}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      )}

      {pagina && pagina.contenido.length === 0 && (
        <BloqueVacio icono="usuarios" titulo="Esta página ya no tiene usuarios"
          accion={<button type="button" className="boton boton-secundario" onClick={() => setParametros({})}>Ir a la primera página</button>}>
          La lista es más corta de lo que era.
        </BloqueVacio>
      )}

      {pagina && pagina.contenido.length > 0 && (
        <>
          <div className="tarjeta desplazable tabla-usuarios-marco">
            <table className="tabla tabla-usuarios">
              <caption className="solo-lectores">Usuarios del personal</caption>
              <thead>
                <tr>
                  <th scope="col">Nombre</th>
                  <th scope="col">Correo</th>
                  <th scope="col">Rol</th>
                  <th scope="col">Estado</th>
                  <th scope="col"><span className="solo-lectores">Acciones</span></th>
                </tr>
              </thead>
              <tbody>
                {pagina.contenido.map((usuario) => (
                  <tr key={usuario.id} className={usuario.eliminado ? 'usuario-eliminado' : undefined}>
                    <td className="celda-nombre">{nombre(usuario)}</td>
                    <td className="celda-correo"><Correo correo={usuario.correo} /></td>
                    <td className="sin-cortar">{NOMBRE_DEL_ROL[usuario.rol]}</td>
                    <td><EstadoCuenta usuario={usuario} /></td>
                    <td className="derecha">{acciones(usuario)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="usuarios-tarjetas" aria-label="Usuarios del personal">
            {pagina.contenido.map((usuario) => (
              <li key={usuario.id} className={`tarjeta usuario-tarjeta${usuario.eliminado ? ' usuario-eliminado' : ''}`}>
                <div className="usuario-tarjeta-cabecera">
                  <strong>{nombre(usuario)}</strong>
                  <EstadoCuenta usuario={usuario} />
                </div>
                <p className="suave"><Correo correo={usuario.correo} /></p>
                <p className="suave">{NOMBRE_DEL_ROL[usuario.rol]}</p>
                {acciones(usuario)}
              </li>
            ))}
          </ul>

          <Paginador numero={pagina.numero} totalPaginas={pagina.totalPaginas} totalElementos={pagina.totalElementos}
            unidad={['usuario', 'usuarios']} alCambiar={(n) => setParametros({ pagina: String(n) })} />
        </>
      )}

      {abierto && 'nuevo' in abierto && (
        <DialogoUsuario alCerrar={cerrar} alGuardar={(usuario) => {
          terminarCon({ asunto: 'Usuario creado', mensaje: `${usuario.nombre} tiene 24 horas para activar su cuenta con el código que le llegó al correo.` })
          setIntento(intento + 1)
        }} />
      )}
      {abierto && 'editar' in abierto && (
        <DialogoUsuario usuario={abierto.editar} propio={esPropio(abierto.editar)} alCerrar={cerrar}
          alGuardar={(usuario) => {
            if (esPropio(usuario) && usuario.rol !== abierto.editar.rol) {
              // El backend ya no acepta su token: se entra de nuevo con el rol nuevo
              terminar(`Tu rol cambió a ${NOMBRE_DEL_ROL[usuario.rol]}. Vuelve a entrar.`)
              return
            }
            if (esPropio(usuario)) renombrar(usuario.nombre)
            terminarCon({ asunto: 'Cambios guardados', mensaje: `Los datos de ${usuario.nombre} quedaron guardados.` }, usuario)
          }} />
      )}
      {abierto && 'eliminar' in abierto && (
        <DialogoEliminarUsuario usuario={abierto.eliminar} alCerrar={cerrar}
          alEliminar={(usuario) => terminarCon({ asunto: 'Cuenta eliminada', mensaje: `${usuario.nombre} ya no puede entrar al PMS.` }, usuario)} />
      )}
    </div>
  )
}

function Cargando() {
  return (
    <div className="tarjeta" aria-busy="true">
      {Array.from({ length: 4 }, (_, fila) => (
        <div key={fila} className="fila-fantasma">
          {Array.from({ length: 4 }, (__, celda) => (
            <span key={celda} className="fantasma" />
          ))}
        </div>
      ))}
      <p className="carga-texto suave" role="status">
        Cargando usuarios…
      </p>
    </div>
  )
}
