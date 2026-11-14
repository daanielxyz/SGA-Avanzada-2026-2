// Lo que dice un filtro de varios valores (DEC-119), sin pantalla, para usarlo también en los filtros aplicados.

/** Un grupo con nombre que se elige de un solo toque, como «Activas» = pendiente, confirmada y en curso. */
export interface Atajo {
  nombre: string
  valores: readonly string[]
}

/** Lo que dice el botón: «Todos», el atajo que coincide, uno o dos nombres, o cuántos se eligieron. */
export function textoDeEleccion(elegidos: readonly string[], opciones: readonly (readonly [string, string])[],
  atajos: readonly Atajo[] = [], todos = 'Todos'): string {
  if (elegidos.length === 0) return todos
  const atajo = atajos.find((a) => a.valores.length === elegidos.length && a.valores.every((v) => elegidos.includes(v)))
  if (atajo) return atajo.nombre
  const nombres = opciones.filter(([valor]) => elegidos.includes(valor)).map(([, nombre]) => nombre)
  // Un valor que ya no está en la lista (llegó en la dirección) se muestra tal cual
  const sueltos = elegidos.filter((valor) => !opciones.some(([v]) => v === valor))
  const todosLosNombres = [...nombres, ...sueltos]
  return todosLosNombres.length <= 2 ? todosLosNombres.join(', ') : `${todosLosNombres.length} elegidos`
}
