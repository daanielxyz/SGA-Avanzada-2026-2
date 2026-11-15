import { bloques, trozos } from './logica'

/** Una línea con sus negritas. Todo va como texto de React: nunca se interpreta HTML. */
function Linea({ texto }: { texto: string }) {
  return (
    <>
      {trozos(texto).map((trozo, indice) => (trozo.negrita ? <strong key={indice}>{trozo.texto}</strong> : trozo.texto))}
    </>
  )
}

/** El texto de una respuesta: párrafos, listas numeradas, listas con viñetas y negritas (DEC-127). */
export default function TextoRespuesta({ texto }: { texto: string }) {
  return (
    <div className="asistente-texto">
      {bloques(texto).map((bloque, indice) => {
        if (bloque.tipo === 'parrafo') return <p key={indice}><Linea texto={bloque.texto} /></p>
        const Lista = bloque.tipo === 'numerada' ? 'ol' : 'ul'
        return (
          <Lista key={indice}>
            {bloque.elementos.map((elemento, posicion) => <li key={posicion}><Linea texto={elemento} /></li>)}
          </Lista>
        )
      })}
    </div>
  )
}
