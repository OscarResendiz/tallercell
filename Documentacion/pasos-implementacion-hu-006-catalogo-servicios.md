# Pasos de implementación: HU-006, catálogo de servicios

## 1. Objetivo

Implementar el catálogo local de servicios del taller para registrar, consultar, buscar, editar y desactivar los conceptos reutilizables de mano de obra. Un servicio activo debe poder seleccionarse al preparar una cotización, y la cotización debe conservar la descripción y el precio usados aunque el servicio cambie después.

La implementación debe:

1. Permitir registrar y editar código opcional, nombre, descripción y precio base.
2. Validar entradas y cantidades monetarias antes de guardarlas.
3. Rechazar códigos duplicados y explicar el conflicto sin perder los datos del formulario.
4. Ofrecer únicamente servicios activos en nuevas cotizaciones.
5. Copiar a cada detalle de cotización el identificador de referencia y los valores usados en esa versión.
6. Desactivar los servicios que dejan de ofrecerse, conservando las cotizaciones históricas.
7. Permitir buscar y distinguir servicios activos e inactivos.
8. Mantener el flujo local y protegido por la sesión autenticada de HU-002.

Este documento desarrolla HU-006 de [historias-usuario-acceso-menu-catalogos-taller-local.md](historias-usuario-acceso-menu-catalogos-taller-local.md). Los conceptos libres, piezas, descuentos, impuestos, autorizaciones y versiones completas de cotización pertenecen al flujo de presupuestos y las historias relacionadas; no deben convertirse en campos del catálogo de servicios.

## 2. Referencias y estado actual

La implementación de esta guía ya se realizó con el alcance mínimo de integración descrito abajo. El inventario de entidades y pantallas de los puntos siguientes refleja el estado encontrado antes de implementar HU-006.

- [Historias de usuario de acceso, menú y catálogos](historias-usuario-acceso-menu-catalogos-taller-local.md): HU-006, criterios de aceptación 1 a 5 y límites de edición.
- [Funcionalidades de la aplicación](funcionalidades-taller-local-android.md): F-042 y F-043, creación de presupuestos y conservación de sus versiones.
- [Entidades de la base de datos](entidades-base-datos-taller-local.md): `CatalogoServicio`, `Cotizacion` y `DetalleCotizacion`.
- [Pasos de implementación de HU-005](pasos-implementacion-hu-005-catalogo-dispositivos.md): patrones de navegación autenticada, estados de carga/error, paginación, bloqueo y pruebas de la aplicación.
- `DataBase/Entidades/CatalogoServicio/CatalogoServicio.kt`: ya define `id`, código nullable con índice único, nombre, descripción, precio en unidades monetarias menores, estado `activo` y fechas de creación/actualización.
- `DataBase/Entidades/CatalogoServicio/CatalogoServicioDao.kt`: ya ofrece lectura por ID, lectura general, alta, `@Update` y borrado físico; aún no filtra por estado, busca por campos ni expresa desactivación.
- `DataBase/Entidades/DetalleCotizacion/DetalleCotizacion.kt`: ya tiene `servicioId` opcional, `descripcionSnapshot`, cantidad, precio unitario e importe. La FK a `CatalogoServicio` usa `RESTRICT`.
- `DataBase/Entidades/DetalleCotizacion/DetalleCotizacionDao.kt`: ya consulta detalles por servicio; debe usarse para identificar referencias y verificar conservación histórica.
- `DataBase/Entidades/Cotizacion/Cotizacion.kt`: guarda moneda, importes y número de versión de la cotización.
- `DataBase/Entidades/ConfiguracionTaller/ConfiguracionTaller.kt`: contiene la moneda actual del taller.
- `DataBase/TallerCelularesDatabase.kt`: ya registra `CatalogoServicio`, `Cotizacion` y `DetalleCotizacion`; la versión actual es 3.
- `Modulos/tablero/TableroPrincipal.kt`: el menú muestra “Servicios” como pendiente. El catálogo debe reemplazar únicamente ese destino y conservar los demás estados.

El esquema y las entidades ya están preparados, pero no se encontró una pantalla para administrar servicios ni una integración de interfaz de cotización que seleccione el catálogo. La ausencia de esa integración es una dependencia relevante del criterio de aceptación 3.

## 3. Alcance

### Incluido

- Entrada “Servicios” desde el menú autenticado.
- Lista paginada, búsqueda local y filtro para distinguir activos e inactivos.
- Alta y edición de código opcional, nombre, descripción y precio base.
- Validaciones de dominio y de importe con errores junto a los campos.
- Consulta de una ficha de servicio y acción de desactivación confirmada.
- Manejo de código duplicado usando tanto una consulta preventiva como la restricción única de Room.
- Selección de servicios activos en el flujo de cotización disponible, con snapshots de descripción y precio.
- Preservación de cotizaciones anteriores al editar o desactivar el servicio.
- Estados de carga, vacío, filtro sin resultados, error y reintento.
- Accesibilidad, navegación local protegida y pruebas de dominio, persistencia, historial e integración.

### No incluido

- Crear una cotización completa, administrar sus versiones o cambiar sus estados, salvo la integración mínima necesaria para seleccionar un servicio y guardar su detalle.
- Crear o editar órdenes, registrar recepción o alterar su ciclo de vida.
- Administrar piezas, inventario, existencias o movimientos de caja.
- Aplicar descuentos, impuestos, anticipos o reglas comerciales dentro del catálogo.
- Borrar físicamente servicios, detalles o cotizaciones.
- Sincronizar el catálogo, consultar precios en internet o exportar datos automáticamente.
- Permitir seleccionar servicios inactivos en nuevas cotizaciones.

## 4. Decisiones de producto y datos

Resolver y documentar estas decisiones antes de fijar la interfaz:

1. **Campos requeridos:** nombre, descripción y precio base son obligatorios; el código es opcional. Un código vacío o con solo espacios se guarda como `NULL`, no como cadena vacía.
2. **Límites sugeridos:** definir máximos explícitos para código, nombre y descripción (por ejemplo, 40, 120 y 1,000 caracteres). Rechazar el exceso sin truncar el texto.
3. **Código duplicado:** recortar espacios exteriores y usar el índice único existente como última defensa contra carreras entre escrituras. La regla de comparación debe coincidir entre búsqueda, validación y SQLite. De forma inicial, conservar la comparación exacta del índice existente; si el negocio requiere que `ABC` y `abc` sean el mismo código, cambiar el índice a `COLLATE NOCASE` mediante una migración probada.
4. **Servicios sin código:** permitir múltiples registros sin código. No convertir la ausencia de código en un valor artificial ni usar el nombre como clave única.
5. **Código de un servicio inactivo:** mantener la unicidad también entre los inactivos para evitar reutilizar referencias ambiguas. Si se decide permitir reutilización, diseñar un índice único parcial de códigos activos y su migración, en vez de eliminar la restricción sin sustitución.
6. **Importe:** almacenar el precio base en `precio_base_minor` como `Long`, nunca como `Float` o `Double`. Interpretarlo en la moneda vigente de `ConfiguracionTaller.monedaCodigo`, que es la fuente monetaria disponible en el modelo.
7. **Conversión monetaria:** analizar el texto con `BigDecimal`, usar los dígitos fraccionarios de `Currency.getInstance(monedaCodigo)` y convertir a unidades menores de forma exacta. Rechazar valores negativos, mal formados, fuera del rango `Long` o con precisión fraccionaria no admitida; no redondear ni truncar silenciosamente.
8. **Cambio de moneda del taller:** como `CatalogoServicio` no guarda moneda propia, aclarar que una modificación de la moneda configurada no convierte automáticamente los precios base. Definir junto con HU-008 si el cambio de moneda requiere revisar los precios vigentes; las cotizaciones ya creadas conservan su moneda e importes.
9. **Precio aplicado:** al seleccionar un servicio, copiar el nombre y la descripción vigentes a `DetalleCotizacion.descripcionSnapshot`, el `precioBaseMinor` a `precioUnitarioMinor` y calcular `importeMinor` con la cantidad. Guardar también `servicioId` para la trazabilidad; no recalcular snapshots al editar el catálogo.
10. **Servicios inactivos:** excluirlos de selección y búsqueda para nuevas cotizaciones, salvo que el usuario active el filtro histórico. Mostrar con claridad su estado; no reactivarlos incidentalmente al editar datos.
11. **Desactivación:** actualizar `activo = false` y `actualizado_en` con un mismo instante UTC. No llamar a `@Delete` ni modificar detalles, cotizaciones o snapshots existentes.
12. **Edición:** cambiar únicamente código, nombre, descripción, precio base y fecha de actualización. No sustituir el registro completo si la operación pudiera modificar su ID, fecha de creación o estado.
13. **Registro semilla:** no inventar servicios predeterminados ni precios comerciales. Si se requiere un catálogo inicial, definir una fuente autorizada y un mecanismo explícito de carga por separado.
14. **Persistencia:** el modelo ya tiene tabla e índice único; no se necesita migración para implementar CRUD y estado activo si se conserva el esquema. Toda decisión que cambie índices o agregue moneda al servicio requiere migración Room no destructiva.

### Decisión de alcance para la cotización

Se eligió catálogo más integración mínima. Desde Servicios se selecciona una orden existente, se abre su borrador más reciente o se crea uno nuevo, y se permite agregar servicios activos como detalles con snapshots de descripción y precio. La integración no crea órdenes ni implementa el ciclo completo de cotizaciones: envío, autorización, cierre, edición de conceptos, descuentos e impuestos configurables quedan fuera de HU-006.

## 5. Trazabilidad de criterios a comportamientos

| Criterio HU-006 | Comportamiento observable | Evidencia |
|---|---|---|
| 1. Registrar y editar | El formulario captura código opcional, nombre, descripción y precio; valida y conserva cambios ante errores | Pruebas de validación, alta, edición y persistencia |
| 2. Evitar códigos repetidos | El formulario informa conflicto; la restricción única impide duplicados incluso ante escrituras concurrentes | Pruebas de coincidencia preventiva y restricción Room |
| 3. Seleccionar en cotización | Solo servicios activos son seleccionables y sus datos vigentes se copian al detalle | Prueba del selector y comparación de snapshots |
| 4. Desactivar sin afectar historial | El servicio deja de ofrecerse, conserva su fila y no cambia cotizaciones anteriores | Prueba de baja lógica y detalles históricos |
| 5. Buscar y filtrar | La consulta permite localizar servicios y distinguir activos/inactivos | Pruebas de búsqueda, filtros y paginación |

## 6. Pasos de implementación

### Paso 1. Confirmar modelo, reglas existentes y dependencias

1. Revisar `CatalogoServicio`, `CatalogoServicioDao`, `TallerCelularesDatabase`, `ConfiguracionTaller`, `Cotizacion`, `DetalleCotizacion` y sus DAO.
2. Confirmar que la tabla e índice único del código están incluidos en Room y que la versión de base actual es 3.
3. Buscar todos los usos de `catalogoServicioDao().obtenerTodos()`, `actualizar()` y `eliminar()` antes de modificar contratos.
4. Buscar el flujo real de edición de detalles/cotizaciones. Confirmar si una pantalla ya existe fuera del módulo de catálogos y quién es responsable de agregar el selector.
5. Confirmar la moneda utilizada por una nueva cotización y definir cómo se obtiene en ausencia de configuración válida; no asignar una moneda o precio por defecto no autorizado.
6. Verificar que la FK `DetalleCotizacion.servicioId` conserva `RESTRICT` y que la referencia es nullable para conceptos libres.

### Paso 2. Definir reglas de dominio y representación monetaria

1. Crear `Modulos/Catalogos/Servicios/` con las reglas y pantallas propias del catálogo.
2. Implementar normalización consistente: recortar espacios exteriores; convertir código vacío a `NULL`; conservar acentos, Unicode y puntuación en nombre y descripción.
3. Validar nombre, descripción y precio requeridos, código opcional y los límites de longitud definidos.
4. Validar precio desde texto decimal con `BigDecimal`; prohibir signos o formatos ambiguos que no admita el formulario y exigir un importe no negativo.
5. Consultar la moneda configurada y obtener sus dígitos decimales. Convertir con precisión exacta a `Long` en unidades menores y detectar overflow.
6. Presentar los errores junto al campo. No incluir el código completo ni valores monetarios sensibles en logs o telemetría.
7. Centralizar la conversión y formato monetario en helpers de dominio/presentación reutilizables; evitar cálculos duplicados en vistas.
8. Añadir pruebas para valores cero y positivos, precisión límite, entrada vacía, formato inválido, negativos, máximos, Unicode y overflow.

### Paso 3. Ajustar el DAO para consultas seguras por estado

Ampliar `CatalogoServicioDao` con operaciones explícitas:

1. Obtener por ID para abrir una ficha y consultar un registro inactivo.
2. Buscar servicios activos e inactivos según un filtro explícito, ordenados de forma estable por nombre, código e ID.
3. Resolver búsqueda y paginación en Room con parámetros enlazados; no cargar el catálogo completo para filtrar solo en memoria.
4. Agregar una consulta por código normalizado para anticipar conflictos y, en edición, excluir el mismo ID.
5. Mantener el índice único como defensa autoritativa frente a duplicados y carreras. Mapear solo el conflicto de código al error de campo correspondiente; no convertir cualquier fallo de base en “código duplicado”.
6. Insertar el nuevo servicio con `EntidadId.nuevo()`, `activo = true` y fechas UTC idénticas.
7. Actualizar solo los datos editables y `actualizado_en`, condicionando la operación a un ID existente.
8. Implementar `desactivar(id, actualizadoEn)` como `UPDATE ... WHERE activo = 1`, devolviendo el número de filas afectadas para detectar un estado obsoleto.
9. Retirar `@Delete` del flujo del catálogo. No borrar servicios referenciados ni detalles de cotización.
10. No cambiar las consultas por detalle de cotización salvo que una vista histórica necesite mostrar cuántas referencias tiene el servicio.

### Paso 4. Construir la lista y la navegación del catálogo

1. Crear una lista que permita filtrar activos/inactivos; el valor inicial debe priorizar servicios activos.
2. Añadir búsqueda por nombre y, cuando exista, código. No buscar por descripción si ello genera resultados excesivos sin ser necesario; mantener criterios explícitos.
3. Aplicar paginación y orden estable. Refrescar desde Room al regresar de alta, edición o desactivación.
4. Mostrar en cada fila nombre, código opcional y precio base formateado con la moneda del taller. No incluir descripciones completas si afectan legibilidad; mostrar el precio solo cuando la configuración monetaria sea válida.
5. Diferenciar carga inicial, lista vacía, filtro sin coincidencias, resultados, carga de página siguiente y error recuperable.
6. Añadir acceso a una ficha para consultar la descripción y el estado. Ocultar acciones de nueva selección y edición operativa cuando corresponda a un servicio inactivo según la decisión de producto.
7. Reutilizar el contenedor protegido de `AccesoActivity`. No crear una Activity pública ni una ruta que evada sesión, bloqueo o `FLAG_SECURE`.
8. Cambiar “Servicios — Pendiente” a disponible en el menú de `TableroPrincipal`, conectando solo esa entrada y dejando sin cambio los demás destinos pendientes.
9. Mantener regreso al tablero, estado de sesión y botón de bloqueo; cancelar trabajos de UI al salir y propagar `CancellationException`.
10. No agregar permisos ni conectividad: la consulta y las escrituras son locales.

### Paso 5. Implementar alta y edición

1. Reutilizar un formulario para alta y edición, con encabezado y acción de guardado inequívocos.
2. Solicitar nombre, descripción y precio base; el código es opcional. Indicar la moneda que se está usando.
3. Mantener el texto introducido si falla una validación, la consulta de código o la escritura.
4. Al crear, validar el código con la consulta preventiva y permitir continuar solo si no existe; no hacer obligatorio el código.
5. Al editar, cargar por ID y consultar el conflicto excluyendo ese mismo servicio.
6. Deshabilitar el botón durante la operación para evitar doble envío.
7. Guardar la entidad con ID nuevo/estable, `activo = true` para altas y fechas UTC. En edición no cambiar `id`, `creado_en` o `activo`.
8. Considerar la restricción única de Room como autoridad final. Mostrar el conflicto solo si se confirma que el índice de código causó la excepción; para cualquier otro error, informar que no se pudo guardar.
9. Confirmar el resultado con una lectura posterior dentro de una transacción cuando la operación requiera verificar conjuntamente el estado; no mostrar éxito antes de comprobar la persistencia.
10. Al volver al listado, releerlo de Room y mantener el filtro o página de manera coherente.

### Paso 6. Implementar ficha y desactivación

1. Abrir la ficha por ID y consultar nuevamente el registro; no depender solo de la copia que estaba en la lista.
2. Presentar nombre, código, descripción, precio formateado y estado activo/inactivo.
3. Mostrar “Editar” y “Desactivar” solo para registros activos. No implementar reactivación implícita como parte de una edición.
4. Antes de desactivar, explicar que dejará de aparecer en nuevas cotizaciones, pero las cotizaciones anteriores conservarán sus detalles.
5. Consultar o contar referencias en `DetalleCotizacion` si se va a mostrar el impacto; esta consulta es informativa y no debe bloquear una desactivación lógica.
6. Persistir `activo = false` y `actualizado_en` con una sola operación condicional.
7. No borrar `CatalogoServicio`, `DetalleCotizacion` ni `Cotizacion`, y no actualizar descripciones o precios snapshot.
8. Si la escritura falla o ya está inactivo, mantener la ficha en estado no confirmado y ofrecer reintento.
9. Al completar, refrescar la lista activa y dejar el registro visible al activar el filtro de inactivos.

### Paso 7. Integrar selección en cotizaciones y conservar snapshots

1. En el formulario de cotización existente, ofrecer búsqueda solo de servicios activos.
2. Al seleccionar un servicio, copiar su nombre/descriptor y precio actual a una línea de la cotización; no enlazar la vista directamente a valores vivos del catálogo.
3. Crear `DetalleCotizacion` con `servicioId`, `descripcionSnapshot`, cantidad, `precioUnitarioMinor` e `importeMinor` calculado en unidades menores.
4. Conservar `servicioId` para trazabilidad, incluso después de desactivar el catálogo. No usarlo para recalcular el detalle histórico.
5. Confirmar que la moneda de la cotización se captura desde la configuración vigente y que el valor no cambia si después se modifica el servicio.
6. Permitir que los conceptos libres existentes mantengan `servicioId = null`; no obligar a que todo detalle provenga de este catálogo.
7. Definir por separado, con la historia dueña de cotizaciones, cómo se editan cantidades, se recalculan totales, se crean versiones y se autorizan presupuestos.
8. Añadir una prueba que cambie nombre/precio del servicio después de crear un detalle y verifique que `descripcionSnapshot`, `precioUnitarioMinor`, `importeMinor` y la cotización previa permanecen iguales.
9. No declarar satisfecho el criterio 3 si no existe una ruta de cotización verificable para seleccionar el servicio y persistir el snapshot.

### Paso 8. Manejar estados, errores y accesibilidad

Representar explícitamente:

- **Cargando:** consulta inicial de servicios o cambio de filtro.
- **Lista vacía:** no hay servicios en el estado seleccionado; ofrecer crear uno si corresponde.
- **Sin coincidencias:** existe filtro pero no hay resultados; permitir limpiarlo.
- **Código en conflicto:** identificar el campo sin borrar lo escrito y permitir corregirlo.
- **Precio inválido:** señalar formato, precisión o rango no aceptado; nunca convertirlo silenciosamente.
- **Inactivo:** explicar que se conserva para historial y no se ofrece en cotizaciones nuevas.
- **Error:** mostrar un mensaje recuperable y botón de reintento; diferenciarlo de una lista vacía.

Además:

1. Mantener etiquetas y orden de foco accesibles, targets táctiles adecuados y soporte de texto ampliado.
2. Formatear los importes con moneda y configuración regional sin cambiar la representación persistida.
3. No incluir detalles personales, valores completos del formulario ni excepciones internas en logs.
4. Evitar estados de éxito si la escritura o lectura de confirmación falla.
5. Propagar cancelaciones de corrutinas en lugar de presentarlas como errores del catálogo.

### Paso 9. Pruebas automatizadas

#### Dominio y precio

- Nombre obligatorio con espacios, Unicode y límite de longitud.
- Código opcional; una entrada vacía se normaliza a `NULL`. Nombre y descripción no pueden quedar vacíos.
- Código con longitud límite y conflicto durante alta/edición, incluyendo edición del mismo registro.
- Conversión monetaria para una moneda sin decimales y una moneda con decimales.
- Precio cero y positivo, negativos, formato inválido, fracciones adicionales, overflow y moneda no válida/no configurada.
- Formateo de precio para listado y ficha sin modificar el importe menor guardado.

#### DAO y persistencia

- Alta con ID, fechas, estado activo y código nulo.
- Índice único rechaza código duplicado; registros sin código pueden coexistir.
- Búsqueda filtra correctamente y no mezcla activos con inactivos si el filtro es solo activo.
- Orden y paginación son estables sin duplicados al cargar la página siguiente.
- Edición conserva ID, fecha de creación y estado; solo cambia los campos editables y fecha de actualización.
- Desactivación condicional excluye el servicio del selector activo y conserva la fila.
- Fallos del DAO se propagan para que la UI no presente una lista vacía o un éxito falso.
- Una base versión 3 puede abrirse sin migración destructiva al implementar CRUD sin cambiar el esquema.
- Si se modifica el índice o esquema, probar la migración desde la versión previa preservando servicios, detalles, referencias e índices.

#### Cotización e historial

- El selector ofrece solo servicios activos.
- Al seleccionar un servicio, el detalle conserva servicioId, descripción, precio y cantidad correctos.
- Editar o desactivar después el servicio no modifica snapshots ni totales de cotizaciones existentes.
- Servicios no asociados a detalles se pueden desactivar sin borrar el registro.
- Un servicio asociado a una o varias cotizaciones puede desactivarse; todas las FK y filas permanecen.
- Los conceptos libres continúan usando `servicioId = null`.

#### Navegación y privacidad

- El catálogo abre desde el menú luego de autenticar y no se puede abrir desde una ruta pública.
- Regreso desde ficha, formulario o filtro conserva el destino esperado y no duplica pantallas.
- Al bloquear o expirar la sesión, se limpia la pantalla y se muestra el acceso local.
- `FLAG_SECURE` permanece activo y el catálogo funciona en modo avión.

### Paso 10. Validación manual de UX

Probar en emulador y dispositivo:

1. Abrir Servicios desde el menú y validar lista, búsqueda, filtro activo/inactivo y retorno al tablero.
2. Registrar un servicio sin código, con descripción válida y un precio válido; comprobar que no se acepte una descripción vacía.
3. Intentar guardar un código duplicado; confirmar que se indica el campo, no se pierden los valores y no se crea una fila extra.
4. Registrar servicios con nombres y descripciones con acentos, símbolos y texto largo dentro del límite.
5. Introducir precios con precisión, formato regional, valor negativo y valor fuera de rango; verificar respuestas explícitas.
6. Editar el nombre y precio de un servicio que ya se utilizó en cotización y comprobar que el historial conserva los snapshots.
7. Desactivar un servicio utilizado; comprobar que no aparece en nuevas selecciones y que las cotizaciones previas siguen legibles.
8. Repetir con cero y múltiples detalles asociados.
9. Probar lista vacía, búsqueda sin coincidencias, error de lectura y error de escritura con sus reintentos.
10. Revisar lector de pantalla, navegación por teclado, escala de texto ampliada, orientación y pantallas pequeñas.
11. Repetir operaciones en modo avión y después del bloqueo por inactividad.

## 7. Matriz de entregables sugeridos

| Área | Trabajo esperado |
|---|---|
| Modelo y esquema | Reutilizar entidad, índice único, estado activo y relaciones existentes; migrar solo si se cambia el esquema |
| DAO de servicio | Consulta activa/inactiva, búsqueda, paginación, detección de código, edición parcial y desactivación |
| Dominio | Validación de campos, normalización de código y conversión monetaria exacta |
| Interfaz del catálogo | Lista, filtros, formulario, ficha, desactivación y estados de error/vacío |
| Navegación | Entrada disponible en el menú y flujo en el contenedor autenticado |
| Cotizaciones | Selección de una orden existente, apertura/creación de borrador, selector de activos y escritura de snapshots en `DetalleCotizacion` |
| Pruebas | Reglas de dominio, precios, snapshots e integración; validación de persistencia e interfaz en el flujo de cotización |
| Documentación | Reglas de código, moneda, desactivación, snapshots y alcance de integración |

## 8. Definición de terminado

HU-006 queda lista cuando:

1. Se pueden registrar y editar servicios con nombre, descripción y precio requeridos, y código opcional.
2. Los importes se validan y almacenan en unidades menores sin `Float`, pérdida de precisión ni redondeo silencioso.
3. Un código duplicado se rechaza con un mensaje recuperable y la restricción única permanece vigente.
4. El catálogo permite buscar y distinguir registros activos e inactivos con estados de carga, vacío y error.
5. Un servicio activo puede seleccionarse en una cotización y su descripción y precio quedan como snapshot.
6. Editar o desactivar un servicio no cambia los detalles ni totales históricos.
7. La desactivación excluye el servicio de nuevas cotizaciones y conserva sus referencias.
8. La navegación sigue protegida por autenticación local, bloqueo de sesión y `FLAG_SECURE`.
9. Las pruebas pertinentes, compilación y lint pasan; la UX se revisa con distintos tamaños y escalas de texto.
10. La integración mínima permite elegir una orden existente, crear/abrir un borrador y guardar snapshots de servicios; el ciclo completo de cotización no forma parte de HU-006.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Un cambio del catálogo altera una cotización anterior | Guardar descripción y precio como snapshots en `DetalleCotizacion` y probar su inmutabilidad |
| El índice único no coincide con la validación | Definir normalización y sensibilidad a mayúsculas una vez; mantener la restricción Room como autoridad |
| Un código vacío causa conflictos | Convertir entradas vacías a `NULL` y permitir varios servicios sin código |
| Se pierde precisión monetaria | Usar `BigDecimal`, moneda configurada y conversión exacta a unidades menores |
| Se reutiliza el código de un servicio inactivo | Mantener por defecto la unicidad global; cambiarla solo con política e índice parcial migrado |
| Un servicio histórico se borra | Usar `activo = false`, retirar borrado físico del flujo y conservar las FK |
| Cambiar la moneda reinterpreta precios base | Documentar la moneda vigente como unidad de los precios y acordar revisión al cambiar configuración |
| El catálogo se entrega sin selector de cotización | La integración mínima agrega servicios con snapshots a un borrador de una orden existente y excluye el ciclo completo de cotización |
| Un fallo se reporta como duplicado o éxito | Mapear errores específicos, mantener el formulario y confirmar la persistencia antes del mensaje de éxito |
| Una vista ofrece servicios inactivos | Filtrar en Room por `activo = 1` para nuevas cotizaciones y probar el selector |
