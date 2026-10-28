/** Lugar de una pantalla del menú que llega en un sprint siguiente (docs/SPRINTS.md). */
export default function EnConstruccion({ titulo }: { titulo: string }) {
  return (
    <>
      <h1>{titulo}</h1>
      <p className="suave">Esta pantalla todavía está en construcción.</p>
    </>
  )
}
