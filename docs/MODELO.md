# Modelo del dominio SGA

Notación: **R** raíz de agregado · **E** entidad interna (sin repositorio) · **VO** `record` inmutable · **EN** `enum`. `→Id` = referencia por id tipado a otro agregado. `[]` = lista. Métodos: solo los del negocio (sin getters). Los IDs de regla (`RES-01`…) están en el Excel.
Fechas = `LocalDate`, horas = `LocalTime` (no hay VO propio para fecha de nacimiento, hora ni rango de fechas). «Ocupante facturable» y «Temporada base» no son clases: son `Ocupante.esFacturableA()` y `Temporada.esBase`.

## 1. Agregados

```
Apartamento ─ Bloqueo(E), Capacidad, Dormitorio, EstadoOperativo, Imagen, Caracteristica      →Id: alojamiento
Reserva     ─ Ocupante(E), Registro(E), Salida(E), Estancia, EstadoReserva, CanalOrigen                                →Id: apartamento, titular, canal(si EXTERNO), politica(versión)
Folio       ─ Cargo(E), Pago(E), AutorizacionCierre, TipoCargo, TipoPago, MedioPago           →Id: reserva
Titular
Temporada   (calendario de temporadas)
Tarifa      (una por versión)                                                                  →Id: apartamento, temporada
PoliticaCancelacion (una por versión) ─ TramoCancelacion, Porcentaje                           →Id: alojamiento
Alojamiento ─ ServicioAdicional(E), Ubicacion, ParametrosAlojamiento, MedioPago(catálogo)
Canal  ·  ConflictoCanal  ·  EventoCanal(bitácora, solo escritura)  ·  Novedad                →Id: canal, apartamento
Transversales: Dinero, Noche, Documento, Correo, ids tipados.   Calculados (no se guardan): Disponibilidad, Cotizacion.
Fuera del dominio (infrastructure.seguridad): Usuario, Rol.
```

## 2. Clases

### Apartamento
- **R Apartamento**: codigo: ApartamentoId · alojamientoId · nombre · descripcion · capacidad: Capacidad · dormitorios: Dormitorio · estadoOperativo · imagenes: Imagen[] (1..10, una principal) · caracteristicas: Caracteristica[] (≥1) · bloqueos: Bloqueo[] · activo.
  `crear(…)` (inactivo, PENDIENTE_PREPARACION — DEC-34) · `activar(tarifasCompletas)` · `retirarDeVenta(hayReservasActivas)` · `cambiarCapacidad(c)` · `cambiarEstadoOperativo(e)` · `registrarBloqueo(ini, fin, motivo)` · `levantarBloqueo(id)` · `tieneBloqueoEn(noche)` · `admite(nOcupantes)`
- **E Bloqueo**: id · fechaInicio · fechaFin (exclusiva, como la Estancia) · motivo · vigente. `levantar()` · `cubre(noche)`
- **VO Capacidad**(valor ≥1) `admite(n)` · **VO Dormitorio**(cantidad ≥1) · **VO Imagen**(url, principal) · **VO Caracteristica**(nombre)
- **EN EstadoOperativo**: PREPARADO, OCUPADO, PENDIENTE_PREPARACION, EN_PREPARACION, FUERA_DE_SERVICIO. `puedePasarA(e)` · `permiteRegistro()`

### Reserva
- **R Reserva**: codigo: ReservaId · apartamentoId · titularId · estancia · estado · canalOrigen · canalId? · idExterno? (ambos solo si EXTERNO) · ocupantes: Ocupante[] (≥1, sin repetidos) · registro: Registro? · salida: Salida? · horaEstimadaLlegada: LocalTime? · valorTotal: Dinero (congelado) · desglose (por noche, congelado) · politicaVersionId (congelada) · creadaEn: LocalDateTime (plazo de confirmación).
  `crear(codigo, apartamentoId, titularId, estancia, canalOrigen, canalId, idExterno, ocupantes, horaEstimada, cotizacion, politicaId, capacidad, estanciaMinima, ahora)` · `modificar(estancia, ocupantes, apartamentoId, cotizacion, capacidad, estanciaMinima, hoy)` (reemplaza a `agregarOcupante` — DEC-38) · `indicarHoraEstimadaLlegada(h)` · `confirmar(anticipo, pagado)` · `cancelar()` · `declararNoShow(ahora, horaLimite)` · `expirar(ahora, plazo)` · `registrarLlegada(id, ahora, autor)` · `registrarSalida(id, ahora, autor)` · `estaActiva()` · `retieneNochesDe(estancia)`
- **E Ocupante**: id · nombre · fechaNacimiento (no futura) · documento? · nacionalidad? (String). `edadA(fecha)` · `esFacturableA(fechaEntrada, umbral)` · `esDuplicadoDe(o)` (OCU-05)
- **E Registro** (check-in): id · fechaHora real · autor: UsuarioId · anulado. Un solo registro vigente; uno equivocado se anula con evento de corrección, no se borra ni revierte el estado.
- **E Salida** (check-out): id · fechaHora real (no anterior al registro) · autor: UsuarioId.
- **VO Estancia**(entrada, salida > entrada) `noches(): Noche[]` · `cantidadNoches()` · `seSolapaCon(otra)`
- **VO Noche**(fecha) — unidad de venta; Bloqueo y Temporada comparan por `Noche`.
- **EN EstadoReserva**: PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA, NO_SHOW. `puedePasarA(e)` · `retieneDisponibilidad()`
- **EN CanalOrigen**: PORTAL, DIRECTO, EXTERNO

### Folio
- **R Folio**: id · reservaId · cargos: Cargo[] · pagos: Pago[] · cerrado · autorizacion?. Ids de movimientos `CAR-n`/`PAG-n` numerados por el folio (DEC-43).
  `abrir(id, reservaId, valorAlojamiento, fecha)` · `agregarServicioAdicional(concepto, valor, fecha)` · `registrarPago(medio, monto, fecha, hoy, mediosHabilitados)` · `registrarDevolucion(…)` · `revertirPago(pagoId, fecha, hoy)` · `revertirCargo(cargoId, motivo, fecha)` · `ajustarPorModificacion(anterior, nuevo, fecha)` · `liquidarPenalidad(valorEstancia, penalidad, motivo, fecha)` · `saldo(): Saldo` · `totalPagado()` · `cerrar()` · `cerrar(autorizacion)`. No existe editar/eliminar cargo o pago.
- **E Cargo**: id · tipo · concepto · valor: Dinero (> 0) · sentido: SentidoAjuste? (solo AJUSTE — DEC-20) · fecha · corrigeA: CargoId?. `aumentaSaldo()` · `esAjuste()`
- **E Pago**: id · medio: MedioPago · monto: Dinero (>0) · tipo: TipoPago · fecha · reversaDe: PagoId? (null en una devolución). `esReverso()`
- **VO Saldo**(monto ≥ 0, situacion: PENDIENTE | A_FAVOR | AL_DIA) — calculado, nunca se guarda · **EN SentidoAjuste**: AUMENTA, DISMINUYE
- **VO AutorizacionCierre**(autor: UsuarioId, motivo, fechaHora) · **VO MedioPago**(nombre; validado contra el catálogo habilitado del Alojamiento)
- **EN TipoCargo**: HOSPEDAJE, SERVICIO_ADICIONAL, PENALIDAD_CANCELACION, AJUSTE · **EN TipoPago**: ABONO, REVERSO (el reverso mantiene el monto positivo y resta en el reporte de ingresos)

### Titular, tarifas y política
- **R Titular**: id · nombre · documento · correo · telefono. `actualizarDatos(…)`
- **R CalendarioTemporadas** (DEC-28, uno por alojamiento): alojamientoId · temporadas: Temporada[]. `agregarTemporada(id, nombre, ini, fin, estanciaMinima)` · `cambiarFechas(id, ini, fin)` · `desactivarTemporada(id)` · `temporadaDe(noche)` · `estanciaMinimaPara(estancia)` (RP-01) · `temporadasActivas()` · `cantidadTemporadasEspecificas()`. Exactamente una base; las específicas no se solapan.
- **E Temporada**: id · nombre · fechaInicio · fechaFin (inclusiva; ambas null si es la base) · esBase · estanciaMinimaNoches (0 = sin mínimo) · activa. `cubre(noche)` · `seSolapaCon(t)`. La base cubre lo no asignado y no se desactiva.
- **R Tarifa**: id · apartamentoId · temporadaId · valorPorOcupante: Dinero (> 0) · version · vigenteDesde. `nuevaVersion(nuevoId, valor, desde)` · `aplicaA(apartamento, temporada)`. Todo apartamento activo tiene tarifa en todas las temporadas activas. Vigente = versión más alta que ya rige (DEC-30).
- **R PoliticaCancelacion** (una por versión, inmutable; vigente = versión más alta — DEC-42): id · alojamientoId · version · tramos: TramoCancelacion[] (≥ mínimo configurable, uno desde 0 h) · penalizacionNoShow: Penalizacion. `crear(…, minimoTramos)` · `nuevaVersion(nuevoId, tramos, noShow, minimoTramos)` · `penalizacionPara(horasAntelacion)`
- **VO TramoCancelacion**(antelacionMinHoras, penalizacion) · **VO Penalizacion**(base: BaseRetencion, porcentaje? | montoFijo?) `calcular(valorTotal, pagado, anticipoExigido)` · **EN BaseRetencion**: VALOR_TOTAL, PAGADO, ANTICIPO_EXIGIDO (DEC-41) · **VO Porcentaje**(0..100)

### Alojamiento, canales y otros
- **R Alojamiento**: id · nombre · descripcion · ciudad · direccion · ubicacion · normas · parametros: ParametrosAlojamiento · serviciosAdicionales: ServicioAdicional[] (≥1) · mediosPago: MedioPago[] (≥2).
- **R Alojamiento** métodos: `cambiarParametros(p)` · `cambiarUbicacion(u)` · `habilitarMedioPago(m)` · `deshabilitarMedioPago(m)` · `aceptaMedioPago(m)` · `agregarServicioAdicional(s)` · `cambiarValorServicio(id, valor)` · `desactivarServicio(id)`.
- **VO ParametrosAlojamiento**: umbralEdadFacturable · horaEntrada · horaSalida · tiempoPreparacion (Duration, horas enteras) · plazoConfirmacion (Duration) · horaLimiteNoShow · anticipo (Porcentaje; 0 = no se exige) · minimoMediosPago · minimoServiciosAdicionales · minimoTemporadas · minimoTramosCancelacion (DEC-41). Se guarda en la tabla `alojamiento`; `sga.*` solo da los valores iniciales (DEC-25).
- **E ServicioAdicional**: id · nombre · generaCargo · valor: Dinero (≥0) · activo (el valor se congela en el Cargo) · **VO Ubicacion**(latitud −90..90, longitud −180..180)
- **R Canal**: id · nombre · tipo: CanalOrigen · credencial (externa, no versionada) · activo. `desactivar()`
- **R ConflictoCanal**: id · canalId · idExterno · apartamentoId · fechaInicio · fechaFin · estado (PENDIENTE|RESUELTO) · resolucion. `resolver(autor, decision)`
- **R EventoCanal** (bitácora): id · canalId · operacion · sentido (ENTRANTE|SALIENTE) · fechaHora · cargaUtil · resultado. Solo se crea; nunca guarda credenciales.
- **R Novedad**: id · apartamentoId · fecha · autor · descripcion · gravedad (enum cerrado) · estado (ABIERTA→EN_REVISION→CERRADA). No se elimina, se cierra.
- **VO Dinero**(monto: BigDecimal COP sin decimales, ≥ 0 — DEC-20) `sumar` · `restar` · `multiplicar(n)` · `porcentaje(p)` · `esMenorQue(d)`
- **VO Documento**(tipo, número, no vacío) · **VO Correo**(formato válido, normalizado) · ids tipados (`ReservaId`, `ApartamentoId`, `FolioId`, `TitularId`, `TemporadaId`, `TarifaId`, `PoliticaId`, `CanalId`, `ConflictoId`, `NovedadId`, `UsuarioId`…)
- **Calculados**: `Disponibilidad`(apartamentoId, estancia, disponible, motivo) `exigir()` (DEC-36) · `Cotizacion`(desglose: línea por noche {noche, temporada, tarifa, ocupantesFacturables, subtotal}, total: Dinero).

## 3. Servicios de dominio (`domain.servicio`)

| Servicio | Método principal | Reglas | Cruza |
|---|---|---|---|
| DisponibilidadDomainService | `verificarDisponibilidad(apartamento, estancia, nOcupantes, reservas, parametros)` → Disponibilidad · `verificarParaModificar(reserva, …)` | RN-01, 07, 20, DISP-02 | Apartamento + Reservas |
| TarificacionDomainService | `calcularValorEstancia(apartamentoId, estancia, ocupantes, umbral, calendario, tarifas)` → Cotizacion | RN-05, 06 | CalendarioTemporadas + Tarifa + Ocupantes |
| CancelacionDomainService | `procesarCancelacion(reserva, folio, politica, parametros, ahora)` → retenido | RN-08, 12, 13 | Reserva + Folio + Política (DEC-44) |
| NoShowDomainService | `declararNoShow(reserva, folio, politica, parametros, ahora)` → retenido | RN-08, 12, 13, POL-06 | Reserva + Folio + Política (DEC-44) |
| RegistroLlegadaDomainService | `procesarCheckIn(reserva, apartamento, hoy)` | RN-08, 10, 11 | Reserva + Apartamento |
| SalidaOperativaDomainService | `procesarCheckOut(reserva, apartamento, folio, autorizacion?)` | RN-08, 17 | Reserva + Apartamento + Folio |
| SincronizacionCanalDomainService | `integrarReservaExterna(datos, canalId, reservasActivas)` | RN-01, 18, 19 | Reserva + ConflictoCanal |
| BloqueoOperativoDomainService | `registrarBloqueo(apartamento, id, ini, fin, motivo, reservas)` | BLO-01, RN-07 | Apartamento + Reservas |
| BajaApartamentoDomainService | `retirarDeVenta(apartamento, reservas)` | APA-16 | Apartamento + Reservas |
| ActivadorApartamentoService | `puedeActivarse(apartamento, calendario, tarifas, minimoTemporadas)` | TAR-03, APA-11, TEM-04 | Apartamento + CalendarioTemporadas + Tarifa |

**Casos de uso (application), no servicios de dominio:** `CrearReserva`, `ModificarReserva` (la regla vive en `Reserva.crear/modificar`), `VencerReservasPendientes` (usa `Reserva.expirar`; lo dispara un planificador). Lista completa `CU-nn` en el Excel.
Las 6 validaciones de crear reserva, en este orden: salida>entrada (Estancia) · entrada≥hoy (Reserva) · apartamento activo con tarifas completas (Apartamento) · capacidad (Reserva/Apartamento) · sin solape con reservas ni bloqueos (Disponibilidad) · tiempo de preparación (Disponibilidad).
Flujo de `CrearReserva` (DEC-36, DEC-37): cargar apartamento, reservas activas, calendario, tarifas y parámetros → `disponibilidad.verificarDisponibilidad(…).exigir()` → `tarificacion.calcularValorEstancia(…)` → `Reserva.crear(…, politicaVigente.id(), …, calendario.estanciaMinimaPara(estancia), ahora)` → `Folio.abrir(…, reserva.valorTotal(), hoy)` (FOL-01) → guardar ambos. Sin `if` en la aplicación.

## 4. Reglas invariantes del enunciado

RN-01 sin solape de reservas activas, cualquier canal · RN-02 ocupantes ≤ capacidad · RN-03 salida > entrada · RN-04 entrada ≥ hoy · RN-05 valor = Σ por noche (tarifa de la temporada × ocupantes facturables) · RN-06 facturable por umbral a la fecha de entrada · RN-07 bloqueo vigente = no disponible · RN-08 solo transiciones permitidas · RN-09 hora estimada de llegada antes de confirmar · RN-10 no registrar antes de la entrada ni sin estar CONFIRMADA · RN-11 solo se entrega apartamento PREPARADO · RN-12 reserva inactiva libera noches · RN-13 retención según política congelada · RN-14 modificar revalida y recalcula (ajuste) · RN-15 pago con medio y fecha; saldo = cargos − pagos · RN-16 cargos/pagos inmutables · RN-17 folio no cierra con saldo ≠ 0 sin autorización · RN-18 conflicto externo se rechaza y registra · RN-19 canal+idExterno único · RN-20 tiempo de preparación entre estancias · RN-21 PENDIENTE vencida → CANCELADA · RN-22 valor y política congelados al crear.
