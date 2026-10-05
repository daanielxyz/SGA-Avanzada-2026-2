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
  `activar(tarifasCompletas)` · `retirarDeVenta(hayReservasActivas)` · `cambiarCapacidad(c)` · `cambiarEstadoOperativo(e)` · `registrarBloqueo(ini, fin, motivo)` · `levantarBloqueo(id)` · `tieneBloqueoEn(noche)` · `admite(nOcupantes)`
- **E Bloqueo**: id · fechaInicio · fechaFin (exclusiva, como la Estancia) · motivo · vigente. `levantar()` · `cubre(noche)`
- **VO Capacidad**(valor ≥1) `admite(n)` · **VO Dormitorio**(cantidad ≥1) · **VO Imagen**(url, principal) · **VO Caracteristica**(nombre)
- **EN EstadoOperativo**: PREPARADO, OCUPADO, PENDIENTE_PREPARACION, EN_PREPARACION, FUERA_DE_SERVICIO. `puedePasarA(e)` · `permiteRegistro()`

### Reserva
- **R Reserva**: codigo: ReservaId · apartamentoId · titularId · estancia · estado · canalOrigen · canalId? · idExterno? · ocupantes: Ocupante[] · registro: Registro? · salida: Salida? · horaEstimadaLlegada: LocalTime · valorTotal: Dinero (congelado) · desglose (por noche, congelado) · politicaVersionId (congelada).
  `crear(…, fechaHoy)` · `agregarOcupante(o)` · `modificar(estancia, ocupantes, apartamentoId)` · `confirmar()` · `cancelar(ahora)` · `declararNoShow(ahora, horaLimite)` · `expirar(ahora, plazo)` · `registrarLlegada(hoy)` · `registrarSalida()` · `estaActiva()`
- **E Ocupante**: id · nombre · fechaNacimiento (no futura) · documento? · nacionalidad? (String). `edadA(fecha)` · `esFacturableA(fechaEntrada, umbral)`
- **E Registro** (check-in): id · fechaHora real · autor: UsuarioId · anulado. Un solo registro vigente; uno equivocado se anula con evento de corrección, no se borra ni revierte el estado.
- **E Salida** (check-out): id · fechaHora real (no anterior al registro) · autor: UsuarioId.
- **VO Estancia**(entrada, salida > entrada) `noches(): Noche[]` · `cantidadNoches()` · `seSolapaCon(otra)`
- **VO Noche**(fecha) — unidad de venta; Bloqueo y Temporada comparan por `Noche`.
- **EN EstadoReserva**: PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA, NO_SHOW. `puedePasarA(e)` · `retieneDisponibilidad()`
- **EN CanalOrigen**: PORTAL, DIRECTO, EXTERNO

### Folio
- **R Folio**: id · reservaId · cargos: Cargo[] · pagos: Pago[] · cerrado · autorizacion?.
  `agregarCargo(c)` · `registrarPago(p)` · `ajustarMovimiento(movimiento, motivo)` · `saldo(): Dinero` (calculado) · `cerrar(autorizacion?)`. No existe editar/eliminar cargo o pago.
- **E Cargo**: id · tipo · concepto · valor: Dinero (nunca negativo; el AJUSTE indica si aumenta o disminuye — DEC-20) · fecha. `esAjuste()`
- **E Pago**: id · medio: MedioPago · monto: Dinero (>0) · tipo: TipoPago · fecha · reversaDe: PagoId? `esReverso()`
- **VO AutorizacionCierre**(autor: UsuarioId, motivo, fechaHora) · **VO MedioPago**(nombre; validado contra el catálogo habilitado del Alojamiento)
- **EN TipoCargo**: HOSPEDAJE, SERVICIO_ADICIONAL, PENALIDAD_CANCELACION, AJUSTE · **EN TipoPago**: ABONO, REVERSO (el reverso mantiene el monto positivo y resta en el reporte de ingresos)

### Titular, tarifas y política
- **R Titular**: id · nombre · documento · correo · telefono. `actualizarDatos(…)`
- **R Temporada**: id · nombre · fechaInicio · fechaFin (inclusiva; null si es la base) · esBase · estanciaMinimaNoches. `cubre(noche)` · `seSolapaCon(t)`. No se solapan entre sí; la base cubre lo no asignado y es obligatoria.
- **R Tarifa**: id · apartamentoId · temporadaId · valorPorOcupante: Dinero · version · vigenteDesde. `nuevaVersion(valor)`. Todo apartamento activo tiene tarifa en todas las temporadas.
- **R PoliticaCancelacion**: id · alojamientoId · version · tramos: TramoCancelacion[] (≥2) · retencionNoShow: Porcentaje · vigente. `retencionPara(diasAntelacion)` · `nuevaVersion(tramos)`
- **VO TramoCancelacion**(antelacionMinDias, retencion: Porcentaje; retención + devolución = 100) · **VO Porcentaje**(0..100)

### Alojamiento, canales y otros
- **R Alojamiento**: id · nombre · descripcion · ciudad · direccion · ubicacion · normas · parametros: ParametrosAlojamiento · serviciosAdicionales: ServicioAdicional[] (≥1) · mediosPago: MedioPago[] (≥2).
- **R Alojamiento** métodos: `cambiarParametros(p)` · `cambiarUbicacion(u)` · `habilitarMedioPago(m)` · `deshabilitarMedioPago(m)` · `aceptaMedioPago(m)` · `agregarServicioAdicional(s)` · `cambiarValorServicio(id, valor)` · `desactivarServicio(id)`.
- **VO ParametrosAlojamiento**: umbralEdadFacturable · horaEntrada · horaSalida · tiempoPreparacion (Duration, horas enteras) · plazoConfirmacion (Duration) · horaLimiteNoShow · anticipo (Porcentaje; 0 = no se exige) · minimoMediosPago · minimoServiciosAdicionales. Se guarda en la tabla `alojamiento`; `sga.*` solo da los valores iniciales (DEC-25).
- **E ServicioAdicional**: id · nombre · generaCargo · valor: Dinero (≥0) · activo (el valor se congela en el Cargo) · **VO Ubicacion**(latitud −90..90, longitud −180..180)
- **R Canal**: id · nombre · tipo: CanalOrigen · credencial (externa, no versionada) · activo. `desactivar()`
- **R ConflictoCanal**: id · canalId · idExterno · apartamentoId · fechaInicio · fechaFin · estado (PENDIENTE|RESUELTO) · resolucion. `resolver(autor, decision)`
- **R EventoCanal** (bitácora): id · canalId · operacion · sentido (ENTRANTE|SALIENTE) · fechaHora · cargaUtil · resultado. Solo se crea; nunca guarda credenciales.
- **R Novedad**: id · apartamentoId · fecha · autor · descripcion · gravedad (enum cerrado) · estado (ABIERTA→EN_REVISION→CERRADA). No se elimina, se cierra.
- **VO Dinero**(monto: BigDecimal COP sin decimales, ≥ 0 — DEC-20) `sumar` · `restar` · `multiplicar(n)`
- **VO Documento**(tipo, número, no vacío) · **VO Correo**(formato válido, normalizado) · ids tipados (`ReservaId`, `ApartamentoId`, `FolioId`, `TitularId`, `TemporadaId`, `TarifaId`, `PoliticaId`, `CanalId`, `ConflictoId`, `NovedadId`, `UsuarioId`…)
- **Calculados**: `Disponibilidad`(apartamentoId, estancia, disponible, motivo) · `Cotizacion`(desglose: línea por noche {noche, temporada, tarifa, ocupantesFacturables, subtotal}, total: Dinero).

## 3. Servicios de dominio (`domain.servicio`)

| Servicio | Método principal | Reglas | Cruza |
|---|---|---|---|
| DisponibilidadDomainService | `verificarDisponibilidad(apartamento, estancia, nOcupantes, reservasActivas)` | RN-01, 07, 20 | Apartamento + Reservas |
| TarificacionDomainService | `calcularValorEstancia(estancia, ocupantes, temporadas, tarifas)` → Cotizacion | RN-05, 06 | Temporada + Tarifa + Ocupantes |
| CancelacionDomainService | `procesarCancelacion(reserva, politica, ahora)` | RN-08, 12, 13 | Reserva + Folio + Política |
| NoShowDomainService | `declararNoShow(reserva, politica, ahora)` | RN-08, 12, 13 | Reserva + Folio + Política |
| RegistroLlegadaDomainService | `procesarCheckIn(reserva, apartamento, hoy)` | RN-08, 10, 11 | Reserva + Apartamento |
| SalidaOperativaDomainService | `procesarCheckOut(reserva, apartamento, folio, autorizacion?)` | RN-08, 17 | Reserva + Apartamento + Folio |
| SincronizacionCanalDomainService | `integrarReservaExterna(datos, canalId, reservasActivas)` | RN-01, 18, 19 | Reserva + ConflictoCanal |
| BloqueoOperativoDomainService | `registrarBloqueo(apartamento, ini, fin, motivo, reservasActivas)` | RN-07 | Apartamento + Reservas |
| BajaApartamentoDomainService | `retirarDeVenta(apartamento, reservasActivas)` | APA-16 | Apartamento + Reservas |
| ActivadorApartamentoService | `puedeActivarse(apartamento, temporadas, tarifas)` | TAR-03, APA-11 | Apartamento + Temporada + Tarifa |

**Casos de uso (application), no servicios de dominio:** `CrearReserva`, `ModificarReserva` (la regla vive en `Reserva.crear/modificar`), `VencerReservasPendientes` (usa `Reserva.expirar`; lo dispara un planificador). Lista completa `CU-nn` en el Excel.
Las 6 validaciones de crear reserva, en este orden: salida>entrada (Estancia) · entrada≥hoy (Reserva) · apartamento activo con tarifas completas (Apartamento) · capacidad (Reserva/Apartamento) · sin solape con reservas ni bloqueos (Disponibilidad) · tiempo de preparación (Disponibilidad).

## 4. Reglas invariantes del enunciado

RN-01 sin solape de reservas activas, cualquier canal · RN-02 ocupantes ≤ capacidad · RN-03 salida > entrada · RN-04 entrada ≥ hoy · RN-05 valor = Σ por noche (tarifa de la temporada × ocupantes facturables) · RN-06 facturable por umbral a la fecha de entrada · RN-07 bloqueo vigente = no disponible · RN-08 solo transiciones permitidas · RN-09 hora estimada de llegada antes de confirmar · RN-10 no registrar antes de la entrada ni sin estar CONFIRMADA · RN-11 solo se entrega apartamento PREPARADO · RN-12 reserva inactiva libera noches · RN-13 retención según política congelada · RN-14 modificar revalida y recalcula (ajuste) · RN-15 pago con medio y fecha; saldo = cargos − pagos · RN-16 cargos/pagos inmutables · RN-17 folio no cierra con saldo ≠ 0 sin autorización · RN-18 conflicto externo se rechaza y registra · RN-19 canal+idExterno único · RN-20 tiempo de preparación entre estancias · RN-21 PENDIENTE vencida → CANCELADA · RN-22 valor y política congelados al crear.
