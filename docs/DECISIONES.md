# Decisiones de diseño acordadas

Complementa `CLAUDE.md` y `docs/MODELO.md`. Cada decisión: qué se decidió y por qué. Si una decisión cambia, se edita aquí (no se acumulan versiones).

## Estructura y dominio
- **DEC-01 · Paquete raíz** `co.edu.uniquindio.sga_avanzada_2026_2`, clase principal `SgaAvanzada20262Application`. Se conserva el nombre original para no romper el trabajo del equipo.
- **DEC-02 · Paquetes por agregado** (`domain/apartamento`, `domain/reserva`…). Cada agregado contiene su raíz, hijos, VO, enums, ids y **su puerto `XRepository`** (no hay carpeta `puerto` separada: el repositorio es parte del contrato del agregado).
- **DEC-03 · `domain/compartido`** guarda lo que usan varios agregados: `ReglaDominioException`, `Dinero`, `Noche`, `Estancia`, `Documento`/`TipoDocumento`, `Correo`, `Porcentaje`, `MedioPago`, `UsuarioId`.
- **DEC-04 · Ids tipados** `record XId(String valor)`, no vacíos. Formato validado donde está definido: `ReservaId` = `RES-\d{4}-\d{5}`, `ApartamentoId` = `APT-\d+`. Las entidades internas también tienen id tipado (`BloqueoId`, `OcupanteId`, `RegistroId`, `SalidaId`, `CargoId`, `PagoId`, `ServicioAdicionalId`, `EventoCanalId`).
- **DEC-05 · Desglose congelado** de la Reserva = `List<LineaCotizacion>` (paquete `tarifa`); `Cotizacion` y `Disponibilidad` son calculados y no se persisten.
- **DEC-06 · Crear vs. reconstruir.** El constructor público de cada entidad solo reconstruye (lo usa el mapper al cargar de BD) y **no** aplica reglas de creación. Las reglas de creación viven en la fábrica `crear(…)` (p. ej. RN-04 entrada ≥ hoy, que fallaría al cargar reservas pasadas). Las **invariantes permanentes** (que deben cumplirse siempre, p. ej. máx. 10 imágenes, una principal, ≥1 característica, rango del bloqueo) sí se validan en el constructor.
- **DEC-07 · Accesores de lectura** estilo record (`codigo()`, `estado()`) permitidos en el dominio; **nunca setters**. Listas expuestas con `List.copyOf`.
- **DEC-08 · Javadoc**: una línea por clase (qué es + regla). Javadoc completo (regla con ID del Excel, `@throws` con cuándo rechaza) en cada método de negocio al implementarlo. Nada en atributos ni getters; atributos no obvios con comentario corto en línea (`// congelado`, `// opcional`).
- **DEC-20 · `Dinero` nunca es negativo** (reemplaza la parte de DIN-03 que lo permitía): el constructor y `restar` rechazan resultados < 0. La dirección se expresa con un indicador, igual que el REVERSO de pago. Propuesta para A5: el cargo AJUSTE lleva monto positivo y un sentido (`AUMENTA`/`DISMINUYE`) (CAR-06); `Folio.saldo()` devuelve `Saldo(monto, situacion)` con situación `PENDIENTE`/`A_FAVOR`/`AL_DIA` (SLD-03), y el folio compara totales antes de restar.
- **DEC-09 · Valores asumidos, por confirmar con el equipo** (marcados `TODO(equipo)` donde aplica): `GravedadNovedad` = BAJA, MEDIA, ALTA; `TipoDocumento` = CC, CE, PASAPORTE, TI; `Rol` = HUESPED, ADMINISTRADOR, RECEPCIONISTA, PERSONAL_SERVICIO (ROL-01, confirmado en el Excel).
- **DEC-21 · Salida de FUERA_DE_SERVICIO** solo hacia PENDIENTE_PREPARACION (EOPE-02 no lo definía): el apartamento vuelve al ciclo de preparación y nunca se entrega sin revisar (RN-11).
- **DEC-22 · Permisos por rol fuera del dominio**: reglas como EOPE-04 (solo el admin declara FUERA_DE_SERVICIO) y BLO-04 (solo el admin bloquea) se controlan en la aplicación/seguridad; el dominio no conoce usuarios.
- **DEC-23 · Hijos mutables encapsulados**: los métodos que cambian una entidad hija (p. ej. `Bloqueo.levantar()`) son de paquete; solo la raíz los invoca. Las listas se exponen como copias inmutables.
- **DEC-24 · Apartamento sin imágenes**: puede existir con 0 imágenes, pero no activarse (IMG-01: "menos de una impide publicarlo").
- **DEC-25 · Parámetros del alojamiento en la tabla** (ALO-03, prevalece sobre la nota de MODELO.md): `ParametrosAlojamiento` se guarda como columnas de `alojamiento` y el admin lo edita desde la app; `sga.*` solo da los valores iniciales al crear el alojamiento (B1). Incluye los **mínimos de la Ficha** (`minimoMediosPago` = 2, `minimoServiciosAdicionales` = 1) para que un hotel real pueda operar con menos (ALO-06).
- **DEC-26 · `alojamientoId` en todo agregado** (ALO-07 · D-04): se agrega a cada agregado cuando se implemente (Temporada, Titular, Reserva, Canal, Novedad…). Con un único alojamiento (ALO-01) el valor es constante.
- **DEC-27 · Catálogo de medios de pago = solo los habilitados** (MPAG-06): habilitar agrega, deshabilitar quita; los pagos históricos conservan el nombre del medio (MPAG-03). En BD es un conjunto (PK alojamiento + nombre), sin columna de orden.

## Aplicación
- **DEC-10 · `@Service` y `@Transactional` permitidos en `application`** (el dominio sigue sin Spring). Una transacción = un agregado, salvo D-02.

## Persistencia
- **DEC-11 · JPA + Hibernate, PostgreSQL principal, H2 solo para pruebas.** Cambio por archivos de propiedades, sin clases extra:
  - `src/main/resources/application.properties` → PostgreSQL, credenciales por variables de entorno (`${DB_USER}`, `${DB_PASSWORD}`), `ddl-auto=validate`, Flyway activo.
  - `src/test/resources/application.properties` → H2 en memoria en `MODE=PostgreSQL`, **Flyway activo y `ddl-auto=validate`**: las pruebas aplican las mismas migraciones y fallan si una entidad JPA no coincide con el SQL (más seguro que `create-drop`, que ocultaría errores del script).
  - H2 es solo dependencia de pruebas (`testRuntimeOnly`); se quitó `spring-boot-h2console`.
- **DEC-12 · Flyway sencillo**: scripts `db/migration/Vn__<agregado>.sql`, uno por incremento, en SQL que entiendan PostgreSQL y H2. Cuando haga falta SQL exclusivo de PostgreSQL (la restricción `EXCLUDE` de A4) irá en una carpeta por motor (`db/migration/postgresql`).
- **DEC-13 · Adaptador por agregado** en `infrastructure/persistencia/<agregado>/`: `XJpa` (@Entity, con Lombok), `XJpaRepository` (Spring Data, de paquete), `XMapper` (dominio ⇄ JPA, de paquete), `XRepositoryJpa` (implementa el puerto). Los casos de uso solo conocen el puerto. Los hijos que solo viven dentro del agregado se mapean como `@ElementCollection` de `@Embeddable` con `@OrderColumn`, sin entidad JPA propia.
- **DEC-14 · Relaciones**: dentro del agregado `@OneToMany` con cascada; entre agregados solo columna de id, **sin `@ManyToOne` ni FK en la BD** (cada agregado se guarda y prueba de forma independiente).
- **DEC-15 · Carga del agregado** = raíz + hijos que pertenecen a esa unidad y que la operación necesita (p. ej. Reserva con ocupantes, registro, salida, desglose). Nunca arrastrar otros agregados.
- **DEC-16 · Tipos**: `Dinero` → `NUMERIC(15,0)`; enums → `VARCHAR` con `@Enumerated(STRING)`; `Duration` → minutos.
- **DEC-17 · Defensa del riesgo central en BD**: `@Version` en Reserva y Apartamento (la versión vive solo en la fila JPA, no en el dominio: `guardar` reutiliza la fila cargada en la transacción del caso de uso, así un cambio concurrente da conflicto en vez de sobrescribirse); `UNIQUE (canal_id, id_externo)` (RN-19); en PostgreSQL `EXCLUDE USING gist (apartamento_id WITH =, daterange(entrada, salida) WITH &&) WHERE estado activo` (RN-01; no aplica en H2).
- **DEC-18 · Paginación**: el puerto devuelve un tipo propio `Pagina<T>`; `Page`/`Pageable` de Spring quedan en el adaptador.

## Forma de trabajo
- **DEC-19 · Desarrollo incremental** según `docs/PLAN.md`: dominio primero, agregado por agregado, y la persistencia de cada agregado entra en cuanto está estable (no al final). Cada incremento se detiene para revisión; los commits los hace el usuario.
