# Pasos de implementación: HU-007, catálogo de piezas y refacciones

## 1. Objetivo

Implementar el catálogo local de piezas y refacciones junto con el control trazable de existencias. La ficha de una pieza debe separar sus datos descriptivos y comerciales de las cantidades derivadas del historial de inventario. Ninguna pantalla debe editar el saldo directamente: toda recepción, reserva, liberación, consumo, devolución o corrección se registra como un movimiento.

La implementación debe:

1. Permitir registrar, buscar, consultar y editar código, número de parte, nombre, variante, calidad, costo, precio de venta y existencia mínima.
2. Consultar existencias físicas, cantidades reservadas y disponibilidad sin confundirlas.
3. Registrar movimientos confirmados con tipo, cantidad, fecha, motivo y orden relacionada cuando aplique.
4. Rechazar reservas o consumos que excedan la cantidad disponible, incluso ante operaciones concurrentes o dobles envíos desde la interfaz.
5. Desactivar una pieza en vez de eliminarla cuando tenga historial o referencias.
6. Corregir operaciones confirmadas con movimientos compensatorios vinculados, sin editar ni borrar la evidencia original.
7. Permitir seleccionar piezas activas como conceptos de refacción en una cotización cuando exista ese flujo, conservando una instantánea del concepto y del precio aplicado. Cotizar no reserva ni consume inventario.
8. Mantener el flujo local, accesible y dentro de la sesión autenticada de HU-002.

Este documento desarrolla HU-007 de [historias-usuario-acceso-menu-catalogos-taller-local.md](historias-usuario-acceso-menu-catalogos-taller-local.md). Los estados de órdenes, las compras completas a proveedores, los pagos y la contabilidad no se convierten en catálogos editables ni forman parte de esta historia.

## 2. Referencias y estado actual

- [Historias de usuario de acceso, menú y catálogos](historias-usuario-acceso-menu-catalogos-taller-local.md): HU-007, criterios de aceptación 1 a 6 y límites de edición.
- [Funcionalidades de la aplicación](funcionalidades-taller-local-android.md): F-070 a F-073; F-076 (alertas bajo mínimo) está en la siguiente etapa.
- [Entidades de la base de datos](entidades-base-datos-taller-local.md): `Pieza`, `MovimientoInventario`, sus relaciones y las reglas de retención.
- [Pasos de implementación de HU-006](pasos-implementacion-hu-006-catalogo-servicios.md): patrones de catálogo, validación monetaria, navegación autenticada y snapshots para conceptos reutilizables.
- `DataBase/Entidades/Pieza/Pieza.kt`: ya contiene código y número de parte opcionales, nombre, variante, calidad, costo y precio en unidades menores, mínimo, estado activo y fechas.
- `DataBase/Entidades/Pieza/PiezaDao.kt`: actualmente ofrece consultas básicas, alta, actualización y borrado físico; no ofrece búsqueda, filtros ni baja lógica.
- `DataBase/Entidades/MovimientoInventario/MovimientoInventario.kt`: ya define tipo, cantidad, orden opcional, costo, proveedor, datos de compra, motivo, fecha de confirmación y creación. La FK a pieza y orden usa `RESTRICT`.
- `DataBase/Entidades/MovimientoInventario/MovimientoInventarioDao.kt`: actualmente ofrece consultas básicas, alta, actualización y borrado físico; no limita movimientos confirmados ni calcula saldos.
- `DataBase/Entidades/DetalleCotizacion/DetalleCotizacion.kt`: ya tiene `piezaId`, precio unitario e importe, además de la descripción snapshot y FK restringida.
- `DataBase/TallerCelularesDatabase.kt`: ya registra `Pieza` y `MovimientoInventario`; la versión actual es 3 y existen migraciones 1→2 y 2→3.
- `Modulos/tablero/TableroPrincipal.kt`: el menú muestra “Inventario de piezas — Pendiente”; la entrada debe abrir el módulo sin salir del contenedor autenticado.
- En el código consultado no se encontró un módulo de interfaz de inventario ni reglas de dominio para movimientos y existencias.

El esquema en el punto de partida contenía la mayoría de los datos maestros y relaciones, pero no era suficiente por sí solo para resolver inequívocamente el signo de un ajuste, enlazar una liberación/consumo con una reserva original ni dejar evidencia estructurada de la compensación.

La implementación de HU-007 añade el módulo `Modulos/Catalogos/Piezas/`, conecta el menú autenticado, agrega reglas y operaciones transaccionales, y migra Room de la versión 3 a la 4. La migración conserva movimientos previos sin inventarles signo ni vínculos. La interfaz los identifica como saldo que requiere revisión cuando un ajuste histórico no tiene sentido registrado. El modelo actualizado está documentado en [entidades-base-datos-taller-local.md](entidades-base-datos-taller-local.md).

## 3. Alcance

### Incluido

- Entrada “Inventario de piezas” desde el menú autenticado.
- Lista paginada y buscable por nombre, variante, código y número de parte, con filtro para activos e inactivos.
- Alta y edición de datos de una pieza, sin editar cantidades en la ficha o formulario.
- Ficha que muestra datos descriptivos y comerciales, existencia física, cantidad reservada, disponibilidad, mínimo y movimientos relacionados.
- Registro y confirmación de entradas, reservas, liberaciones, consumos, devoluciones y ajustes.
- Relación opcional del movimiento con una orden, y captura de motivo y fecha.
- Reglas transaccionales que impiden saldos físicos o disponibles inválidos.
- Baja lógica de piezas y preservación de movimientos, órdenes y cotizaciones.
- Corrección mediante movimientos compensatorios, sin actualización o borrado de un movimiento confirmado.
- Selector mínimo de piezas activas en el flujo de cotización existente, con precio y descripción snapshot; sin reservar ni consumir por seleccionar/cotizar.
- Estados de carga, vacío, error y reintento; accesibilidad y pruebas de dominio, base de datos, historial y navegación.

### No incluido

- Un módulo completo de órdenes de compra, recepción de proveedor, cuentas por pagar o evaluación de proveedores. Los campos actuales de proveedor/compra se pueden conservar en los movimientos, pero no implican implementar ese flujo.
- Crear o editar órdenes, cambiar estados o implementar el ciclo completo de reparación.
- Automatizar consumos por el solo hecho de cotizar o autorizar una cotización.
- Alertas proactivas, reportes de consumo y piezas de mayor costo (F-076, siguiente etapa); sí se muestra el mínimo para consulta.
- Importación masiva, sincronización, transferencia automática o comunicación con servicios externos.
- Borrado físico de movimientos confirmados o piezas referenciadas.

## 4. Decisiones de producto y datos

Resolver estas reglas antes de codificar cálculos o pantallas. Los valores controlados se validan en dominio y persistencia; no se aceptan como cadenas arbitrarias desde la interfaz.

1. **Cantidad ingresada:** solicitar siempre cantidades enteras positivas. El sentido del efecto lo determina el tipo de movimiento; nunca aceptar una cantidad cero o negativa para intentar invertir el efecto.
2. **Tipos de movimiento:** respetar los valores `ENTRADA`, `CONSUMO`, `RESERVA`, `LIBERACION`, `DEVOLUCION` y `AJUSTE`. No renombrar ni crear tipos desde el catálogo.
3. **Sentido del ajuste:** el esquema actual solo tiene `tipo = AJUSTE` y una cantidad positiva. Agregar un campo controlado, por ejemplo `sentido_ajuste` (`INCREMENTO`/`DECREMENTO`), requerido solo para `AJUSTE` y nulo para los demás tipos. No inferir el signo a partir de `motivo`.
4. **Trazabilidad de reserva y compensación:** agregar una referencia opcional `movimiento_origen_id` con FK restringida a `movimiento_inventario`. Usarla para que `LIBERACION` y un `CONSUMO` reservado identifiquen la reserva que reducen, y para enlazar movimientos compensatorios con el movimiento corregido. Verificar que la referencia sea válida para el tipo y que no permita reducir dos veces una reserva.
5. **Momento de efecto:** solo un movimiento confirmado participa en los saldos. Para movimientos confirmados, establecer `confirmada_en` una sola vez; una fila pendiente con `confirmada_en = NULL` no afecta cantidades. Si una operación se registra y confirma en un solo paso, guardar la fila y su confirmación en una transacción.
6. **Existencia física:** calcularla como entradas confirmadas (`ENTRADA`, `DEVOLUCION` y ajustes de incremento) menos salidas confirmadas (`CONSUMO` y ajustes de decremento). `RESERVA` y `LIBERACION` no cambian la existencia física.
7. **Cantidad reservada:** calcularla desde reservas confirmadas todavía no liberadas ni consumidas. Cada liberación y consumo asignado a una reserva reduce únicamente el remanente de esa reserva; no debe excederlo.
8. **Disponibilidad:** `disponible = existencia física - cantidad reservada`. No permitir que sea negativa. Reservar exige que la cantidad solicitada no supere la disponibilidad. Un consumo directo exige disponibilidad suficiente; un consumo ligado a una reserva exige saldo suficiente en esa reserva y reduce la existencia física y el remanente reservado en la misma transacción.
9. **Devoluciones:** definir si una devolución corresponde a una pieza consumida que vuelve al inventario. Si sí, enlazarla al consumo o a la orden cuando aplique, confirmar la condición reutilizable y sumar su cantidad física solo al confirmarse. No usar `DEVOLUCION` para cancelar una reserva sin entrada física; esa operación es `LIBERACION`.
10. **Motivo requerido:** exigir un motivo para ajustes, devoluciones y compensaciones. Para los demás movimientos, exigirlo cuando sea necesario para explicar la operación y la orden relacionada. Recortar espacios y validar un máximo de longitud explícito, sin truncar silenciosamente.
11. **Fecha:** guardar creación y confirmación en UTC con formato consistente con las demás entidades. Presentar fechas según la zona horaria configurada sin cambiar el valor persistido. Una fecha anterior debe ser visible como dato operativo, pero no debe permitir insertar un saldo histórico que invalide movimientos posteriores.
12. **Costo y precio:** conservar `costo_unitario_minor` y `precio_venta_minor` como `Long` en unidades menores; convertir texto mediante `BigDecimal` y la moneda configurada, sin `Float`/`Double`, redondeo o truncamiento silencioso. El costo de un movimiento de entrada es el costo de esa operación y no reescribe los costos históricos.
13. **Moneda:** la pieza no conserva una moneda propia. Usar la moneda vigente de `ConfiguracionTaller`; documentar que cambiar la moneda del taller no convierte automáticamente costos ni precios guardados y resolver esa política junto con HU-008.
14. **Código y número de parte:** ambos son opcionales. Definir si se permiten duplicados antes de introducir restricciones únicas. No hacer único el número de parte sin considerar variantes, calidad o presentaciones. Mientras no haya regla aprobada, permitir duplicados y ofrecer coincidencias para revisión, sin bloquear un alta válida.
15. **Pieza nueva:** iniciar sin existencia atribuida (saldo cero). Para cargar inventario inicial, crear un movimiento confirmado explícito con motivo “Carga inicial”; nunca ingresar una cantidad inicial directamente en `Pieza`.
16. **Edición:** modificar solo los campos descriptivos/comerciales y `actualizado_en`. No modificar `id`, `creado_en`, `activo`, cantidades ni movimientos previos.
17. **Desactivación:** establecer `activo = false`; el registro deja de estar disponible para nuevas cotizaciones y operaciones nuevas que la política no permita, pero permanece consultable en el historial. No borrar referencias o movimientos.
18. **Compensación:** confirmar qué contramovimiento corresponde a cada tipo y sentido, y exigir `movimiento_origen_id` y motivo. La compensación debe respetar las mismas reglas de disponibilidad y no puede crear un saldo inválido; si ya no es posible revertir por consumos posteriores, registrar un ajuste correctivo permitido y explicar el resultado, nunca alterar el original.
19. **Cotización:** al seleccionar una pieza activa, copiar a `DetalleCotizacion` una descripción representativa y el precio de venta actual, además del `piezaId`. La cotización conserva ese snapshot aunque después cambie el catálogo. Cotizar una pieza no reduce ni reserva existencias.
20. **Migración:** los campos de ajuste y referencia de origen requieren una migración Room 3→4. Mantener la compatibilidad con los datos anteriores; para movimientos existentes sin información de signo o vínculo, aplicar una estrategia explícita y verificable (por ejemplo, conservarlos como legado y no inventar campos), no asignarles un efecto supuesto.

### Modelo de cálculo y transacciones

El libro de movimientos es la fuente de verdad. Una consulta que muestre saldos debe derivarse de los movimientos confirmados y aplicar reglas iguales para todos los puntos de entrada. Si por rendimiento se agrega un saldo materializado, este solo se actualiza dentro de la misma transacción que inserta el movimiento y debe poder reconciliarse con el libro; no introducirlo como una segunda fuente independiente.

Cada acción que cambia el saldo debe ejecutarse en una transacción de Room: volver a leer el saldo pertinente, validar la regla de negocio, crear el movimiento y fijar `confirmada_en`. Una comprobación previa en la pantalla ayuda a UX, pero no sustituye la comprobación transaccional. En errores de restricción, mostrar insuficiencia solo si la causa se identifica como tal; reportar explícitamente otros errores.

## 5. Trazabilidad de criterios a comportamientos

| Criterio HU-007 | Comportamiento observable | Evidencia |
|---|---|---|
| 1. Registrar/editar datos | Formulario con campos indicados, validación y precios exactos; las cantidades no se editan aquí | Pruebas de dominio, alta, edición y persistencia |
| 2. Consultar ficha e inventario | La ficha distingue datos de la pieza, físico, reservado, disponible y movimientos confirmados | Pruebas de cálculo, historial y refresco |
| 3. Registrar operaciones | Cada operación confirmada crea un movimiento con cantidad, tipo, fecha, motivo y orden opcional | Pruebas de DAO y transacción por tipo |
| 4. Evitar saldo insuficiente | La reserva o el consumo inválidos se rechazan atómicamente y la UI informa el saldo actualizado | Pruebas de límites, competencia y fallo de escritura |
| 5. Desactivar sin perder evidencia | La pieza deja de ofrecerse en operaciones nuevas; referencias y movimientos permanecen | Pruebas de baja lógica y FK |
| 6. Corregir con compensación | La corrección enlaza el movimiento original y no lo modifica ni elimina | Pruebas de compensación y preservación de historial |

## 6. Pasos de implementación

### Paso 1. Confirmar reglas, modelo actual y consumidores

1. Revisar `Pieza`, `PiezaDao`, `MovimientoInventario`, `MovimientoInventarioDao`, `DetalleCotizacion`, sus DAO y `TallerCelularesDatabase`.
2. Buscar todos los consumidores de `piezaDao()` y `movimientoInventarioDao()`, además de los usos de `DetalleCotizacion.piezaId`, antes de cambiar contratos.
3. Confirmar con producto las reglas de códigos duplicados, devoluciones, consumos reservados/directos, ajustes, fecha retroactiva y compensaciones.
4. Escribir una tabla de efectos para cada tipo de movimiento y una tabla de transiciones de reserva (reservar, consumir parcialmente, consumir totalmente, liberar parcial/total y corregir).
5. Confirmar que “existencia” en cada pantalla significa físico, reservado o disponible; no usar la palabra sin precisar el indicador.
6. Revisar el flujo de cotización disponible y decidir dónde se incorpora el selector de piezas sin implementar el ciclo completo de órdenes/presupuestos.
7. Determinar la estrategia de migración 3→4. Revisar si hay datos reales de versiones anteriores y preparar un caso de actualización que conserve pieza, movimiento, orden y detalle cotizado.
8. Mantener el alcance local y no agregar permisos, red o sincronización.

### Paso 2. Actualizar modelo y migración de Room

1. Añadir a `MovimientoInventario` el sentido requerido para `AJUSTE`; mantenerlo nulo para los tipos donde no aplica.
2. Añadir `movimientoOrigenId` nullable con FK a la misma tabla, índice y `onDelete = RESTRICT` para enlazar liberaciones, consumos de una reserva y compensaciones.
3. Si una reserva puede consumirse/liberarse varias veces, representar cada operación como un movimiento distinto ligado a su reserva; nunca editar su cantidad original.
4. Agregar una migración 3→4 sin borrar tablas ni reconstruir datos de forma destructiva. Actualizar la versión de Room y registrar la migración en el builder.
5. Para movimientos existentes, preservar todos los valores. No inferir dirección de `AJUSTE` ni origen de reservas históricos; documentar cómo se consultan como legado y bloquear una corrección automatizada ambigua.
6. Evaluar restricciones SQL/índices que se puedan expresar sin incompatibilidad con SQLite/Room. Validar también en dominio toda regla que no pueda imponerse mediante la base.
7. No agregar índices únicos al código o número de parte hasta que la política haya sido acordada.
8. Probar migración desde la versión 3 y verificar conteos, IDs, FK, índices y legibilidad de cotizaciones y movimientos previos.

### Paso 3. Crear reglas de dominio y cálculos monetarios

1. Crear el módulo `Modulos/Catalogos/Piezas/` siguiendo la separación existente entre reglas y UI, sin colocar cálculos de inventario en la Activity o en componentes visuales.
2. Normalizar código y número de parte vacíos a `NULL`, recortar espacios exteriores y conservar acentos y caracteres válidos.
3. Validar nombre, variante y calidad con reglas acordadas; validar longitudes antes de persistir y presentar errores junto al campo.
4. Validar costo y precio como valores no negativos en texto decimal, usando `BigDecimal`, precisión de la moneda configurada y conversión exacta a `Long`. Detectar moneda inválida, precisión no soportada y overflow.
5. Validar existencia mínima como entero no negativo. No aceptar decimales, negativos ni overflow.
6. Implementar una regla central que derive existencia física, cantidad reservada y disponibilidad solo de movimientos confirmados.
7. Usar sumas seguras contra overflow (`Math.addExact`/operaciones equivalentes) y fallar con error visible si un agregado excede el rango.
8. Validar cada movimiento: tipo reconocido, cantidad positiva, contexto de ajuste, motivo requerido, orden existente/permitida, fecha y referencia a movimiento origen del tipo correcto.
9. Comprobar que la suma liberada/consumida desde una reserva no supere su cantidad y que la operación corresponda a la misma pieza.
10. Añadir pruebas unitarias antes de conectar la UI: límites de saldo, reserva parcial, liberación, consumo reservado, consumo directo, devolución, ajuste en ambos sentidos, corrección y overflow.

### Paso 4. Completar DAO y repositorio local

1. En `PiezaDao`, agregar búsqueda paginada, filtro activo/inactivo y orden estable. Buscar por nombre, variante, código y número de parte con parámetros enlazados; escapar comodines si se usa `LIKE`.
2. Añadir consulta para abrir la ficha por ID y actualizar solo los campos editables. Sustituir `@Delete` del flujo normal por una operación condicional de desactivación.
3. En `MovimientoInventarioDao`, agregar consultas por pieza ordenadas de forma estable por fecha e ID, paginación si el historial puede crecer y consultas de saldo o datos necesarios para derivarlo.
4. Retirar `@Update` y `@Delete` como operaciones disponibles para movimientos confirmados. Las operaciones de actualización deben limitarse a campos de un movimiento pendiente si el flujo realmente requiere editarlo antes de confirmar.
5. Crear un repositorio o servicio de dominio para coordinar lecturas y escrituras de pieza, movimiento, reserva y orden.
6. Implementar `confirmarMovimiento` dentro de una transacción Room: recuperar los datos vigentes, comprobar la pieza, reconstruir saldo/reserva, validar, insertar el movimiento confirmado y devolver el estado resultante.
7. Implementar operaciones explícitas (entrada, reservar, liberar, consumir, devolver y ajustar), no una escritura pública que acepte cualquier combinación arbitraria de `tipo` y cantidad.
8. Validar de nuevo el saldo dentro de cada transacción, aun si el formulario ya mostró una disponibilidad aparentemente suficiente.
9. Para reserva/consumo/liberación por orden, validar que la orden existe y que la operación se encuentre permitida por las reglas acordadas. No modificar el estado de la orden como efecto lateral de una operación de catálogo.
10. Crear una ruta específica de compensación que cargue el movimiento original, calcule el contramovimiento permitido, exija motivo y referencia, y vuelva a validar el saldo final. No exponer actualización/borrado del original.
11. Propagar fallos de Room y cancelaciones de corrutinas. No convertir errores desconocidos en “sin existencia”, lista vacía o mensaje de éxito.
12. Si los saldos se materializan por rendimiento, insertar el movimiento, cambiar el saldo y guardar cualquier remanente de reserva en una única transacción; implementar reconciliación y prueba de consistencia contra el libro.

### Paso 5. Construir catálogo y navegación protegida

1. Crear lista de piezas con filtro inicial de activas, búsqueda y paginación con orden estable.
2. Mostrar nombre, variante/calidad, identificador disponible, precio y una indicación clara del nivel de disponibilidad. No cargar todos los movimientos para cada fila; obtener resúmenes de manera eficiente.
3. Distinguir estados de carga, lista vacía, filtro sin coincidencias, error recuperable, carga de página y lista con datos.
4. Añadir ficha navegable que cargue la pieza y sus saldos actuales desde persistencia, no desde una copia antigua de la lista.
5. Mostrar explícitamente existencia física, cantidad reservada y disponibilidad. Marcar si está por debajo del mínimo; no sustituir el cálculo por un valor editable.
6. Enlazar la lista y la ficha al contenedor protegido de `AccesoActivity`. No crear rutas públicas o Activities que evadan sesión, bloqueo o protección de pantalla ya existente.
7. Cambiar únicamente “Inventario de piezas — Pendiente” a disponible y conectar ese destino en `TableroPrincipal`; conservar el resto del menú.
8. Conservar regreso, bloqueo de sesión y reautenticación de HU-002. Cancelar trabajos al salir y propagar `CancellationException`.
9. Mantener navegación accesible: etiquetas claras, foco lógico, controles táctiles suficientes y soporte de texto ampliado.
10. No incluir números de orden, datos personales o excepciones internas en logs. El inventario debe funcionar en modo avión.

### Paso 6. Implementar alta y edición de pieza

1. Crear un formulario compartido para alta y edición con código, número de parte, nombre, variante, calidad, costo unitario, precio de venta y existencia mínima.
2. Identificar con claridad los campos opcionales y requeridos conforme a reglas de producto. No marcar código o número de parte como obligatorio.
3. Mostrar la moneda configurada y convertir precios con las reglas de dominio; conservar los datos de formulario cuando falle una validación o escritura.
4. En alta, crear ID y fechas UTC, iniciar activo y sin saldo implícito. No guardar un “inventario inicial” en columnas de `Pieza`.
5. En edición, conservar ID, fecha de creación, estado y movimientos; actualizar solo campos descriptivos/comerciales y fecha de actualización.
6. Si se detectan posibles coincidencias de código o número de parte, mostrarlas como aviso si la unicidad no está aprobada; no rechazar automáticamente registros duplicados.
7. Deshabilitar guardar durante la escritura para evitar doble envío y confirmar persistencia antes de mostrar éxito.
8. Ante cualquier fallo, conservar el formulario y comunicar la causa de forma comprensible sin exponer detalles internos.
9. Para introducir existencias iniciales, ofrecer la acción separada de registrar entrada y exigir cantidad, fecha y motivo correspondiente.

### Paso 7. Implementar detalle e historial de movimientos

1. En la ficha, consultar de nuevo pieza, saldos y movimientos para evitar mostrar disponibilidad obsoleta como confirmada.
2. Separar visualmente datos maestros, precios, existencia física, reservas, disponible y mínimo.
3. Listar movimientos con tipo y etiqueta entendible, cantidad, fecha de confirmación, motivo, orden relacionada cuando exista y estado de confirmación.
4. Indicar reserva de origen y movimiento corregido cuando estén vinculados. Mostrar el historial en orden determinista, más reciente primero.
5. Diferenciar movimientos pendientes de los confirmados; los pendientes no deben aparecer como entradas disponibles ni cambiar los cálculos.
6. Permitir abrir la orden asociada mediante una ruta autorizada si el módulo de órdenes ya está disponible; si aún no lo está, mostrar el folio almacenado sin fingir que existe una pantalla de orden.
7. Al regresar de una operación, refrescar desde la base los saldos y la lista. Si la operación no se confirmó, no actualizar la ficha con un saldo anticipado.
8. Añadir estados vacíos y errores distintos para historial sin movimientos y fallo de consulta.

### Paso 8. Registrar, confirmar y corregir movimientos

1. Ofrecer acciones explícitas según tipo y permitir seleccionar una orden existente cuando aplique. La orden no debe ser obligatoria para movimientos generales.
2. Para `ENTRADA`, solicitar cantidad y motivo; capturar costo de esa entrada cuando esté disponible. Confirmar como inventario recibido, no cuando se crea una solicitud de compra pendiente.
3. Para `RESERVA`, seleccionar orden, pieza y cantidad; mostrar disponibilidad actualizada y validar de nuevo transaccionalmente al confirmar.
4. Para `LIBERACION`, seleccionar la reserva pendiente correspondiente y permitir liberar como máximo su remanente.
5. Para `CONSUMO`, solicitar orden cuando aplique; si proviene de una reserva, enlazarla y reducir simultáneamente cantidad reservada y existencia física. Si es consumo directo, descontar solo de la disponibilidad.
6. Para `DEVOLUCION`, solicitar cantidad, motivo y orden/consumo asociado según la política aprobada. Aumentar existencia solo tras confirmar recepción física reutilizable.
7. Para `AJUSTE`, requerir sentido incremento/decremento, cantidad y motivo. Validar que el decremento no deje existencia física menor a lo reservado ni saldo disponible negativo.
8. Presentar una vista de confirmación con pieza, tipo, cantidad, efecto esperado, orden y motivo para reducir errores operativos.
9. Al confirmar, volver a validar y escribir en una sola transacción. Si otra operación cambió el saldo, rechazar la confirmación, actualizar la ficha y explicar la cantidad vigente; no aceptar el saldo que vio previamente el usuario.
10. Prevenir doble toque mediante estado de envío en UI y usar una estrategia de idempotencia si el flujo permite reintentos tras un resultado de escritura incierto.
11. No permitir editar ni borrar un movimiento confirmado. Una corrección inicia un flujo de compensación que muestra el movimiento original y explica el contramovimiento propuesto.
12. Guardar compensación, vínculo al movimiento original y motivo como un nuevo registro. Volver a consultar y verificar el saldo antes de presentar la corrección como exitosa.
13. Si movimientos posteriores impiden la compensación exacta, detener la operación e informar la causa; ofrecer el ajuste correctivo autorizado sin modificar evidencia anterior.
14. Mantener el formulario abierto ante insuficiencia, conflicto o error de persistencia, ofreciendo actualizar cantidades o reintentar de manera segura.

### Paso 9. Integrar piezas en cotizaciones sin alterar inventario

1. En el selector de conceptos del flujo de cotización disponible, ofrecer piezas activas y búsqueda por nombre, variante, código o número de parte.
2. Al seleccionar una pieza, copiar a `DetalleCotizacion` la referencia `piezaId`, una descripción snapshot legible, precio de venta vigente, cantidad e importe calculado en unidades menores.
3. Usar `Math.multiplyExact` o validación equivalente para prevenir overflow al multiplicar cantidad por precio.
4. Al editar/desactivar la pieza posteriormente, preservar descripción, precio e importe de cotizaciones ya guardadas. No recalcular snapshots históricos desde el catálogo.
5. No permitir seleccionar una pieza inactiva para un concepto nuevo; permitir consultar sus cotizaciones previas mediante sus snapshots.
6. No crear `RESERVA` ni `CONSUMO` al cotizar, enviar o autorizar. Esas operaciones requieren una acción explícita en el flujo de inventario/orden.
7. Si la interfaz de cotizaciones aún no existe, implementar solo el selector y escritura snapshot que permite reutilizar la pieza en un detalle, o documentar esa integración como dependencia bloqueante del criterio de uso en cotización. No simularla en la ficha del catálogo.
8. Añadir prueba que cree un detalle, cambie o desactive la pieza y compruebe que el historial cotizado no cambia.

### Paso 10. Desactivar y conservar referencias

1. Mostrar desactivación solo para piezas activas y pedir confirmación indicando que se preservarán movimientos, referencias de órdenes y cotizaciones.
2. Cambiar `activo` a falso y actualizar `actualizado_en` mediante una operación condicional; no llamar a `@Delete`.
3. Excluir piezas inactivas de nuevas cotizaciones, reservas y operaciones nuevas, salvo la ruta de corrección explícita permitida por política para movimientos históricos.
4. Permitir consultar piezas inactivas y su ficha/historial mediante el filtro correspondiente.
5. Mantener las FK `RESTRICT`; una pieza con movimientos, órdenes o detalles históricos no debe poder desaparecer.
6. Si se encuentra una pieza ya inactiva, tratar la acción como estado obsoleto y refrescar; no presentar una segunda baja como éxito.
7. No agregar reactivación incidental al formulario de edición. La reactivación, si el negocio la requiere, debe ser una acción independiente, confirmada y auditada.

### Paso 11. Pruebas automatizadas

#### Dominio y precios

- Campos requeridos, opcionales, espacios, longitudes, caracteres acentuados y normalización de identificadores vacíos.
- Política acordada de coincidencias/duplicados de código y número de parte.
- Costos y precios cero/positivos, precisión de moneda, negativos, texto inválido, moneda no válida y overflow.
- Existencia mínima vacía, cero, positiva, negativa, decimal y fuera de rango.
- Sumas y productos de inventario que alcanzan y exceden los límites de `Long`/`Int`.

#### Movimientos y consistencia

- Cada tipo acepta solo cantidades positivas y requiere los campos correspondientes.
- Movimiento pendiente no cambia físico, reservado ni disponible; la confirmación lo incorpora exactamente una vez.
- Entradas y devoluciones incrementan físico; consumo y ajuste decremento reducen físico; reserva/liberación solo afectan reservado; ajustes usan sentido explícito.
- Reserva exacta al disponible se acepta; un elemento más de lo disponible se rechaza.
- Consumo directo contra disponibilidad y consumo ligado a reserva actualizan los saldos correctos.
- Liberación parcial/total y consumo parcial/total no pueden exceder el remanente de la reserva ni liberarlo dos veces.
- Devolución requiere contexto válido y no corrige una reserva.
- Ajuste decremento no puede dejar stock físico debajo del reservado; ajuste incremento respeta límites.
- Movimiento compensatorio enlazado conserva intacto el original, aplica el efecto inverso esperado y requiere motivo.
- Falta de pieza/orden, origen de otra pieza, referencia inválida, movimiento duplicado e integridad referencial producen errores explícitos.
- Dos intentos concurrentes que competirían por la última unidad no pueden confirmar ambos.
- Fallo dentro de la transacción deja sin cambios el libro ni el saldo.

#### Persistencia, UI e historial

- Búsqueda, filtro activo/inactivo, orden y paginación no omiten ni duplican filas.
- Alta inicia con saldo cero; editar pieza no cambia movimientos ni fechas históricas.
- Desactivación mantiene la fila y sus referencias, y elimina la pieza de nuevos selectores.
- Historial por pieza y orden conserva fecha, tipo, cantidad, motivo, vínculo y orden correcto.
- Snapshot de cotización conserva precio y descripción tras editar/desactivar pieza; cotizar no crea movimientos.
- Migración desde DB v3 preserva filas, claves foráneas, cotizaciones y movimientos legados; no inventa sentido de ajustes.
- La navegación requiere sesión autenticada; al bloquear, la pantalla se protege y las operaciones pendientes se cancelan con seguridad.
- Los errores de lectura/escritura se distinguen de saldo cero y lista vacía, y no generan éxito falso.

### Paso 12. Validación manual de UX

Probar en emulador y dispositivo Android:

1. Abrir Inventario desde el menú después de autenticarse; verificar regreso al tablero y bloqueo de sesión.
2. Crear una pieza sin código/número de parte; comprobar que su saldo inicial es cero.
3. Registrar una entrada inicial y verificar en detalle la existencia física y disponible.
4. Crear otra pieza con código o número de parte coincidente conforme a la política decidida.
5. Buscar por nombre, variante, código y número de parte; alternar filtros activo/inactivo.
6. Reservar todo el disponible, comprobar que el disponible llega a cero y que una segunda reserva insuficiente no se confirma.
7. Liberar parcialmente, volver a reservar y consumir parcialmente desde una reserva; revisar los tres saldos.
8. Registrar consumo directo, devolución válida y ajustes en ambos sentidos. Confirmar que cada operación pide los datos definidos y aparece en historial.
9. Intentar corregir un movimiento confirmado; verificar que se conserva intacto y que la compensación queda vinculada.
10. Editar y desactivar una pieza con movimientos, orden y detalle cotizado; confirmar que los datos históricos siguen disponibles y que no aparece en nuevas selecciones.
11. Dejar una operación sin confirmar, navegar fuera o bloquear la app y comprobar que no afecta el saldo.
12. Simular error de almacenamiento, conflicto por saldo modificado y reintento; confirmar que no se duplica un movimiento.
13. Probar lista vacía, búsqueda sin coincidencias, carga de página, texto ampliado, lector de pantalla y pantallas pequeñas.
14. Repetir las operaciones principales en modo avión.

## 7. Matriz de entregables sugeridos

| Área | Trabajo esperado |
|---|---|
| Modelo y esquema | Sentido de ajustes y relación con movimiento origen; migración Room 3→4 verificada |
| Dominio | Validación de piezas, movimientos, moneda y cálculos de stock físico/reservado/disponible |
| DAO y repositorio | Búsqueda/paginación, historial y operaciones transaccionales que confirman movimientos |
| Interfaz de catálogo | Lista, filtros, ficha, formulario, desactivación y estados de carga/error |
| Interfaz de inventario | Formularios confirmables para entrada, reserva, liberación, consumo, devolución, ajuste y compensación |
| Navegación | Entrada disponible desde menú autenticado; no altera otros destinos |
| Cotizaciones | Selector de piezas activas y escritura de descripción/precio snapshot; sin movimiento automático |
| Pruebas | Reglas de dominio, transacciones, migración, saldos, concurrencia, snapshots, baja lógica e historial |
| Documentación | Reglas de saldo, reservas, ajustes, confirmación, correcciones, moneda y datos legados |

## 8. Definición de terminado

HU-007 queda lista cuando:

1. Se pueden registrar, buscar, consultar y editar los datos de la pieza sin editar directamente sus cantidades.
2. La ficha presenta por separado datos maestros, existencia física, reserva, disponibilidad, mínimo e historial.
3. Todo cambio de existencia resulta de un movimiento confirmado y persistido con cantidad, tipo, fecha, motivo y orden opcional.
4. Entradas, devoluciones, consumos, reservas, liberaciones y ajustes tienen efectos definidos y probados.
5. Las operaciones son atómicas; no confirman reservas/consumos por encima del saldo ni generan una disponibilidad negativa.
6. Los movimientos confirmados no se editan ni eliminan; una corrección crea un movimiento compensatorio trazable.
7. Una pieza se puede desactivar sin borrar filas, movimientos, órdenes o cotizaciones históricas.
8. Si se habilita el selector de cotización, el snapshot de pieza/precio permanece estable y cotizar no reserva ni consume.
9. La migración preserva los datos de versiones previas y no inventa signo u origen para movimientos legados.
10. El módulo opera dentro de la sesión autenticada y sin red; fallos explícitos no se presentan como saldo cero o éxito.
11. Las pruebas pertinentes, compilación y lint pasan; la UX se valida en tamaños y escalas de texto distintos.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| El tipo `AJUSTE` no indica si suma o resta | Persistir un sentido controlado, validarlo y migrar sin suposiciones |
| Una reserva se libera o consume más de una vez | Enlazar movimientos derivados a la reserva y calcular su remanente en transacción |
| Dos operaciones consumen la misma última unidad | Revalidar saldo dentro de la transacción antes de insertar y confirmar |
| Una cotización altera el inventario | Mantener cotización como snapshot; reservar/consumir solo por acción explícita |
| El saldo se desincroniza del libro | Calcularlo desde movimientos o materializarlo transaccionalmente con reconciliación |
| Un movimiento histórico se edita o elimina | Retirar actualización/borrado de confirmados y usar compensaciones vinculadas |
| La cantidad inicial aparece sin evidencia | Iniciar saldo en cero y registrar carga inicial como entrada confirmada |
| Una devolución se confunde con liberación | Definir su vínculo y efecto físico; usar liberación para cancelar reserva sin recibir pieza |
| Código/número de parte causan falsos duplicados | No introducir unicidad sin política definida; advertir coincidencias sin rechazar |
| Cambio de moneda reinterpreta importes existentes | Documentar moneda de los valores y definir una migración/proceso separado para conversión |
| Error de lectura se muestra como cero existencias | Diferenciar resultado vacío de error y reportar explícitamente fallos |
| Datos legados reciben un signo inventado | Preservar el registro, marcarlo como legado y no inferir efecto no documentado |
