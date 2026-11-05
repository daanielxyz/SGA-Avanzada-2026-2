import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ErrorApi } from '../api/cliente'
import { consultarCuenta, type Cuenta } from '../api/cuenta'
import type { TipoDocumento } from '../api/reservas'

const DOCUMENTOS: Record<TipoDocumento, string> = {
  CC: 'Cédula de ciudadanía',
  CE: 'Cédula de extranjería',
  PASAPORTE: 'Pasaporte',
  TI: 'Tarjeta de identidad',
}

/** Los datos del huésped (DEC-68). Por ahora solo se ven: editarlos llega con el sprint 3. */
export default function MiCuenta() {
  const [cuenta, setCuenta] = useState<Cuenta | null>(null)
  const [problema, setProblema] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true
    consultarCuenta()
      .then((c) => vigente && setCuenta(c))
      .catch((error: unknown) => vigente && setProblema(
        error instanceof ErrorApi ? error.message : 'No pudimos abrir tu cuenta. Intenta de nuevo en un momento.'))
    return () => {
      vigente = false
    }
  }, [])

  return (
    <main className="ficha cuenta">
      <h1>Mi cuenta</h1>
      {problema && <p role="alert" className="aviso">{problema}</p>}
      {!cuenta && !problema && <p className="suave">Cargando tus datos…</p>}
      {cuenta && (
        <>
          <dl className="datos">
            <dt>Nombre</dt>
            <dd>{cuenta.nombre}</dd>
            <dt>Documento</dt>
            <dd>{DOCUMENTOS[cuenta.tipoDocumento]} {cuenta.numeroDocumento}</dd>
            <dt>Correo</dt>
            <dd>{cuenta.correo ?? 'Sin correo'}</dd>
            <dt>Teléfono</dt>
            <dd>{cuenta.telefono ?? 'Sin teléfono'}</dd>
          </dl>
          <p className="suave">Si algún dato está mal, comunícate con el hotel para corregirlo.</p>
        </>
      )}
      <Link to="/mis-reservas">Ver mis reservas</Link>
    </main>
  )
}
