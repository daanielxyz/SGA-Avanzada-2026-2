import { Link } from 'react-router'
import { bloques, trozos } from './logica'

/**
 * Una línea con sus negritas y sus enlaces a pantallas del PMS. Todo va como texto de React: nunca se interpreta HTML.
 */
function Linea({ texto, alSeguirEnlace }: { texto: string; alSeguirEnlace?: () => void }) {
  return (
    <>
      {trozos(texto).map((trozo, indice) => {
        if (trozo.ruta) return <Link key={indice} to={trozo.ruta} onClick={alSeguirEnlace}>{trozo.texto}</Link>
        return trozo.negrita ? <strong key={indice}>{trozo.texto}</strong> : trozo.texto
      })}
    </>
  )
}

/**
 * El texto de una respuesta: párrafos, listas numeradas, listas con viñetas, negritas y enlaces internos (DEC-127).
 *
 * @param alSeguirEnlace al seguir un enlace del texto (en el celular, el panel se cierra para ver la pantalla)
 */
export default function TextoRespuesta({ texto, alSeguirEnlace }: { texto: string; alSeguirEnlace?: () => void }) {
  return (
    <div className="asistente-texto">
      {bloques(texto).map((bloque, indice) => {
        if (bloque.tipo === 'parrafo') return <p key={indice}><Linea texto={bloque.texto} alSeguirEnlace={alSeguirEnlace} /></p>
        const Lista = bloque.tipo === 'numerada' ? 'ol' : 'ul'
        return (
          <Lista key={indice}>
            {bloque.elementos.map((elemento, posicion) => (
              <li key={posicion}><Linea texto={elemento} alSeguirEnlace={alSeguirEnlace} /></li>
            ))}
          </Lista>
        )
      })}
    </div>
  )
}
