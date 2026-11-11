# Pasos de implementación: HU-004, catálogo de clientes

## 1. Objetivo

Implementar el catálogo local de clientes para que la persona responsable del taller pueda buscar posibles coincidencias, registrar y corregir sus datos, consultar sus dispositivos y órdenes relacionadas, y preservar las referencias históricas al dar de baja un registro.

La implementación debe:

1. Permitir buscar clientes por nombre o teléfono y revisar coincidencias antes de crear otro registro.
2. No exigir teléfono, correo ni notas; el nombre es obligatorio para el registro ordinario.
3. Permitir editar los datos actuales del cliente sin alterar las instantáneas de órdenes existentes.
4. Proteger los historiales: una baja lógica no debe borrar al cliente ni sus relaciones.
5. Mostrar desde la ficha los dispositivos y las órdenes asociadas.
6. Operar sin red y únicamente después de la autenticación local de HU-002.

Este documento desarrolla HU-004 de [historias-usuario-acceso-menu-catalogos-taller-local.md](historias-usuario-acceso-menu-catalogos-taller-local.md). El registro y administración de dispositivos como flujo independiente corresponde a HU-005; HU-004 solo requiere consultar los dispositivos relacionados en la ficha del cliente. El registro de órdenes y su ciclo completo pertenecen a historias posteriores.

## 2. Referencias y estado actual

- [Historias de usuario de acceso, menú y catálogos](historias-usuario-acceso-menu-catalogos-taller-local.md): HU-004, criterios de aceptación 1 a 5.
- [Funcionalidades de la aplicación](funcionalidades-taller-local-android.md): F-020, F-021 y F-024.
- [Entidades de la base de datos](entidades-base-datos-taller-local.md): reglas de `Cliente`, relaciones con `Dispositivo` y `OrdenServicio`, instantáneas históricas y conservación de referencias.
- [Pasos de implementación de HU-003](pasos-implementacion-hu-003-menu-principal-tablero.md): navegación protegida, privacidad, estados de carga/error y estado honesto de los destinos pendientes.
- `DataBase/Entidades/Cliente/Cliente.kt`: ya contiene nombre, teléfono, correo, notas, marcas de tiempo y `eliminadoEn`.
- `DataBase/Entidades/Cliente/ClienteDao.kt`: ya cuenta con operaciones básicas, pero `obtenerTodos()` incluye bajas y `eliminar()` permite borrar físicamente.
- `DataBase/Entidades/Dispositivo/DispositivoDao.kt`: permite consultar dispositivos por `clienteId`.
- `DataBase/Entidades/OrdenServicio/OrdenServicioDao.kt`: permite consultar órdenes por `clienteId`; se debe revisar el tratamiento de eliminaciones lógicas al usarlo desde la ficha.
- `DataBase/TallerCelularesDatabase.kt`: la base está en versión 2 y registra `Cliente`, `Dispositivo` y sus DAO.

La guía describe trabajo pendiente; la existencia de entidades y operaciones básicas no implica que la interfaz HU-004 esté implementada.

## 3. Alcance

### Incluido

- Acceso autenticado al catálogo desde el menú de HU-003.
- Lista de clientes activos con búsqueda local por nombre o teléfono.
- Revisión explícita de coincidencias antes de confirmar una nueva alta.
- Formulario para crear y editar nombre, teléfono, correo y notas opcionales.
- Ficha de solo lectura con dispositivos y órdenes relacionadas.
- Baja lógica, preservación de referencias y manejo explícito de clientes inactivos.
- Manejo de carga, lista vacía, cero coincidencias, errores y reintentos.
- Pruebas de validación, búsqueda, persistencia, relaciones, historial y navegación protegida.

### No incluido

- Alta, edición o baja de dispositivos como flujo de catálogo: corresponde a HU-005.
- Crear o editar órdenes, registrar recepción o alterar datos de una orden.
- Enviar mensajes, realizar llamadas, sincronizar con contactos del teléfono o subir datos.
- Eliminar físicamente órdenes, dispositivos o movimientos relacionados.
- Imponer unicidad a teléfonos o correos.
- Guardar credenciales de desbloqueo del equipo en notas u otros datos del cliente.
- Cambiar los snapshots de cliente/contacto almacenados en órdenes previas.

## 4. Decisiones de producto y datos

Resolver y documentar estas decisiones antes de fijar la interfaz:

1. **Campos requeridos:** tratar el nombre como requerido para el cliente normal. Teléfono, correo y notas permanecen opcionales. Si una recepción futura admite clientes anónimos u ocasionales, deberá definir su flujo sin crear registros ficticios en este catálogo.
2. **Coincidencias:** buscar coincidencias por nombre y teléfono como una ayuda, no como una validación de unicidad. Dos clientes pueden compartir un número; no bloquear la creación ni fusionar registros automáticamente.
3. **Normalización:** acordar cómo comparar mayúsculas, acentos, espacios y puntuación telefónica. Para teléfono, comparar dígitos normalizados además de permitir coincidencias parciales útiles. No cambiar el valor original guardado sin una acción explícita de edición.
4. **Umbral de búsqueda:** evitar consultas amplias mientras el campo está vacío o contiene solo uno o dos caracteres, salvo que se trate de una lista inicial paginada. Indicar cuándo la búsqueda aún necesita más texto.
5. **Duplicados posibles:** presentar resultados con suficiente información para distinguirlos —nombre y medios de contacto disponibles—, sin exponer notas completas en la lista. Ofrecer “Continuar con este cliente” o “Crear de todos modos”; no sugerir que el sistema confirmó que son la misma persona.
6. **Baja de clientes:** preferir la baja lógica mediante `eliminadoEn`, ya disponible en el modelo, para conservar las FK y el historial. No invocar `@Delete` cuando el registro tenga referencias; tampoco borrar automáticamente dispositivos.
7. **Clientes inactivos:** excluirlos de la lista normal y de las nuevas selecciones, excepto cuando una vista histórica necesite mostrar la relación. Definir si se permite reactivar un registro y quién puede hacerlo; si no se implementa reactivación, dejarlo identificado como pendiente.
8. **Edición histórica:** actualizar `Cliente` únicamente. Las columnas `cliente_nombre_snapshot` y `cliente_contacto_snapshot` de `OrdenServicio` son evidencia histórica y no deben actualizarse en cascada.
9. **Privacidad de notas:** advertir que las notas son información general del cliente, no un lugar para credenciales, PIN, patrones, IMEI u otros secretos innecesarios.
10. **Historial:** decidir qué estados y registros de orden aparecen y cómo presentar órdenes eliminadas lógicamente. No ocultar historial por el hecho de que el cliente esté inactivo.
11. **Paginación:** definir un límite de lista y orden estable. La búsqueda y las consultas de ficha deben resolverse en SQLite/Room, no cargando todos los clientes y filtrándolos solo en la vista.
12. **Acción de baja:** requerir confirmación con consecuencias claras. Informar que el cliente dejará de aparecer como activo y que sus órdenes y dispositivos históricos se conservan.

### Decisiones aplicadas en la implementación

- El nombre es obligatorio (máximo 120 caracteres); teléfono, correo y notas son opcionales. Los valores opcionales vacíos se guardan como `NULL`; el correo se valida solo cuando se proporciona.
- La lista consulta 50 clientes por página con orden estable. La búsqueda compara nombres sin distinguir mayúsculas ni acentos y teléfonos por dígitos normalizados; los resultados se consultan en Room.
- Antes de crear se muestran hasta cinco coincidencias por nombre o teléfono. Se puede abrir una ficha existente y volver al formulario sin perder lo escrito, o crear el registro de todos modos; compartir teléfono no bloquea el alta.
- La baja es lógica y confirmada; no se ofrece eliminación física ni reactivación. Los inactivos se excluyen del catálogo activo y las coincidencias, pero la ficha histórica conserva el acceso por identificador.
- La ficha lista dispositivos y todas las órdenes relacionadas en orden estable, incluidas las archivadas. La edición solo actualiza los datos actuales del cliente, nunca los snapshots de las órdenes.
- La navegación permanece dentro de `AccesoActivity`, con bloqueo por inactividad y `FLAG_SECURE`; las notas advierten no guardar secretos.

## 5. Trazabilidad de criterios a comportamientos

| Criterio HU-004 | Comportamiento observable | Evidencia |
|---|---|---|
| 1. Buscar antes de crear | La búsqueda por nombre/teléfono muestra coincidencias locales antes de confirmar una nueva alta; permite continuar si dos personas comparten datos | Pruebas de normalización, coincidencias y creación permitida |
| 2. Datos opcionales | Se guarda un nombre válido con teléfono, correo y notas nulos o vacíos según política de normalización | Pruebas de validación y persistencia |
| 3. Corregir contacto | La ficha refleja el contacto nuevo; las órdenes existentes conservan sus snapshots | Prueba de edición y comparación de snapshots |
| 4. Evitar destrucción | Una solicitud de baja conserva las FK y ofrece/ejecuta baja lógica o anonimización aprobada | Prueba de referencias, baja lógica y consulta histórica |
| 5. Consultar ficha | La ficha muestra dispositivos y órdenes asociados al cliente seleccionado | Pruebas de consultas de relación y estado vacío por sección |

## 6. Pasos de implementación

### Paso 1. Inspeccionar y proteger las operaciones de persistencia existentes

1. Confirmar que `Cliente` representa la tabla `cliente`, que `nombre` es no nulo y que teléfono, correo, notas y `eliminadoEn` son opcionales.
2. Confirmar que `Dispositivo.clienteId` y `OrdenServicio.clienteId` tienen las restricciones de clave foránea esperadas.
3. Revisar todas las llamadas de `clienteDao().eliminar()` antes de cambiar su contrato. No dejar una ruta de UI que pueda borrar físicamente clientes por accidente.
4. Reemplazar `obtenerTodos()` en el flujo del catálogo por consultas explícitas para activos, búsquedas, ficha y posibles bajas; evitar que los consumidores decidan por su cuenta qué significa “activo”.
5. Si el catálogo solo utiliza campos ya existentes, no cambiar el esquema ni incrementar la versión de Room. Si el diseño requiere nuevos campos o índices, especificar una migración no destructiva, probarla desde versión 2 y actualizar la documentación del modelo.
6. No activar migraciones destructivas ni eliminar registros para resolver incompatibilidades del esquema.

### Paso 2. Definir las consultas de Room y sus límites

Ampliar `ClienteDao` con operaciones explícitas y seguras:

1. Consultar por identificador para edición/ficha.
2. Listar clientes activos (`eliminado_en IS NULL`) con orden estable por nombre e identificador.
3. Buscar por nombre y teléfono, incluyendo solo clientes activos para sugerir coincidencias de alta.
4. Consultar bajas solo desde una vista histórica o de administración explícita, si se decide incluirla.
5. Contar o comprobar si existen órdenes/dispositivos relacionados cuando se confirme una baja o se decida qué opciones mostrar.
6. Actualizar cliente con ID estable y marca `actualizadoEn`; no hacer un `INSERT` nuevo para editar.
7. Usar parámetros enlazados en consultas Room/SQLite. Nunca interpolar texto de búsqueda en SQL.
8. Añadir índices únicamente si una consulta y volumen esperado lo justifican. Ya existen índices por nombre y teléfono; evitar índices duplicados.
9. Añadir métodos específicos para baja lógica/reactivación que actualicen `eliminadoEn`; no reutilizar `@Delete` para la acción de la interfaz.
10. Definir consultas de dispositivos y órdenes asociadas que no ejecuten una consulta por cada fila ni incluyan información sensible que la ficha no necesita.

Para una lista grande, usar paginación o límites con una clave de orden estable. Las búsquedas deben cancelar o invalidar resultados antiguos cuando la consulta cambia rápidamente.

### Paso 3. Implementar normalización y validación del formulario

1. Crear validación de dominio reutilizable, separada de la vista, que:
   - rechace nombre vacío después de quitar espacios exteriores;
   - aplique límites razonables de longitud acordes con el esquema y el diseño;
   - acepte teléfono, correo y notas opcionales;
   - valide formato de correo solo cuando se haya proporcionado, sin exigir correo;
   - no exija que el teléfono sea único ni tenga un formato nacional único;
   - preserve texto Unicode y permita nombres legítimos con acentos, guiones y espacios.
2. Normalizar espacios exteriores antes de guardar. Acordar si se comprimen espacios internos; no alterar silenciosamente nombres de forma que cambien su significado.
3. Mantener el teléfono presentado/guardado según la decisión del producto; usar una forma derivada con dígitos para búsqueda, sin reemplazar el dato original por una forma irreconocible.
4. Tratar texto opcional vacío como `null` solo si esa es la convención adoptada y aplicada consistentemente.
5. Mostrar errores junto al campo correspondiente y preservar la entrada cuando falle la validación o el guardado.
6. Prevenir doble envío mientras la escritura esté en curso y comunicar éxito únicamente después de confirmar la operación local.
7. No registrar en logs nombres, teléfonos, correos ni notas.

### Paso 4. Construir la lista y la búsqueda del catálogo

La pantalla del catálogo debe incluir:

1. Encabezado “Clientes”, acción accesible de búsqueda y botón claro para crear cliente.
2. Lista con nombre y, como dato secundario, el medio de contacto disponible. No mostrar notas ni datos innecesarios en cada fila.
3. Búsqueda reactiva o con debounce por nombre/teléfono, sin requerir internet ni permisos de contactos.
4. Estado inicial/carga distinto de lista vacía, cero coincidencias y error de lectura.
5. Acción para limpiar búsqueda y regresar a la lista normal.
6. Paginación o carga incremental, si el volumen lo requiere.
7. Accesibilidad para búsqueda, acciones, elementos de la lista y mensajes de estado; soporte para fuentes ampliadas y targets táctiles.
8. Navegación a la ficha al seleccionar un cliente, conservando el identificador, no una copia mutable de todos sus datos.

### Paso 5. Evitar duplicados mediante revisión previa

1. En el formulario de alta, permitir buscar por nombre o teléfono antes de confirmar la creación.
2. Cuando haya coincidencias, mostrar un panel o diálogo con los registros sugeridos y una acción para abrir la ficha.
3. Permitir continuar con la creación incluso si el teléfono coincide: un número familiar, compartido o de negocio no identifica de forma única a una persona.
4. No fusionar registros, reemplazar datos o asociar órdenes de forma automática.
5. Actualizar la búsqueda de coincidencias si cambia el nombre o teléfono introducido.
6. Evitar que la búsqueda de posibles duplicados guarde datos parciales o cree un registro provisional en la base.
7. Cubrir la carrera entre revisión y guardado: validar de nuevo que los valores sigan siendo los enviados; la ausencia de coincidencias no garantiza unicidad.

### Paso 6. Implementar alta y edición

1. Reutilizar el mismo formulario en modos “Nuevo cliente” y “Editar cliente”, con títulos y acciones identificables.
2. En modo alta, crear un ID local mediante la convención existente, definir `creadoEn` y `actualizadoEn` en UTC y dejar `eliminadoEn = null`.
3. En modo edición, cargar el cliente activo por ID y actualizar solo los campos de cliente permitidos y `actualizadoEn`.
4. No cambiar `creadoEn`, ID ni el estado de baja durante una edición común.
5. Al guardar, volver a consultar el registro para que lista y ficha presenten la fuente persistida.
6. Si el registro desapareció, fue dado de baja o la escritura falla, presentar un error recuperable en lugar de una confirmación simulada.
7. Proteger la escritura con una transacción si la operación futura modifica más de una entidad; para el formulario simple de cliente no inventar escrituras relacionadas.

### Paso 7. Implementar la ficha y las relaciones históricas

1. Consultar el cliente por ID y mostrar nombre, teléfono, correo y notas según los campos existentes.
2. Ofrecer edición solo cuando el cliente esté activo; si está inactivo, mostrar su estado y evitar acciones de edición que no estén definidas.
3. Consultar los dispositivos con `DispositivoDao.obtenerPorClienteId(clienteId)` o una consulta equivalente que filtre y ordene de forma explícita.
4. Consultar órdenes mediante una operación específica por cliente, ordenada por fecha/folio conforme a la experiencia acordada.
5. Mostrar de cada dispositivo los campos necesarios para identificarlo —tipo, marca, modelo y color—; limitar serie/IMEI y no incluir credenciales del equipo.
6. Mostrar de cada orden datos de identificación y estado apropiados para historial. No convertir la ficha en un editor de órdenes.
7. Separar visualmente las secciones “Dispositivos” y “Órdenes”; cada sección puede estar vacía independientemente.
8. Diferenciar “sin dispositivos/órdenes” de error de consulta. Si una sección falla, permitir reintentar sin declarar que no hay datos.
9. No incluir consultas de notas, series/IMEI o datos sensibles en mensajes de error, telemetría o logs.
10. No sustituir snapshots históricos de orden por los datos actuales del cliente. Una edición de teléfono/nombre debe cambiar la ficha, pero no el registro histórico de recepción.

### Paso 8. Implementar baja lógica y conservación

1. En la ficha, ofrecer una acción explícita para dar de baja al cliente; no llamarla “borrar para siempre” si las referencias se conservan.
2. Antes de confirmar, determinar si existen dispositivos u órdenes relacionadas y presentar el impacto con texto claro.
3. Guardar la baja actualizando `eliminadoEn` a un instante UTC y `actualizadoEn`, sin eliminar filas ni dependencias.
4. No borrar dispositivos ni órdenes en cascada. El esquema actual usa restricciones de borrado y debe seguir preservándose.
5. Excluir el registro de búsquedas y listas normales después de la baja, pero conservarlo en consultas de órdenes/fichas históricas.
6. Si una orden antigua todavía referencia al cliente, mostrar la relación conforme a la política histórica; no impedir la consulta solo por la baja.
7. No anonimizar automáticamente, porque eso cambiaría datos legales/comerciales y puede borrar contexto histórico. Si se decide ofrecer anonimización, requerirá decisión explícita, confirmación adicional, regla para snapshots existentes y pruebas específicas.
8. Si se implementa reactivación, exigir una acción explícita y actualizar `eliminadoEn = null` y `actualizadoEn`; no reactivar por editar ni por una nueva orden.
9. Probar que el método de baja no invoca `ClienteDao.eliminar()` y que conserva todas las FK. Considerar retirar o limitar la API de borrado físico si no hay otros consumidores legítimos.

### Paso 9. Conectar navegación con HU-003 y respetar HU-002

1. Reemplazar el aviso pendiente del acceso “Clientes” en el menú de HU-003 por una navegación real al catálogo.
2. Integrar el destino dentro del contenedor/sesión protegida actual (el tablero se aloja en `AccesoActivity`) o en una navegación que conserve el guard; no lanzar una Activity exportada ni crear una ruta pública de datos.
3. Diseñar una forma explícita de volver del catálogo/ficha al tablero sin restablecer autenticación ni duplicar pantallas en el back stack.
4. Si la sesión vence, limpiar la vista de clientes y regresar al acceso mediante el bloqueo existente. Mantener `FLAG_SECURE` en todas las pantallas con datos personales.
5. Registrar actividad de usuario conforme a HU-002, sin extender el tiempo de autenticación por una consulta en curso.
6. Refrescar lista y ficha después de alta, edición, baja o reactivación; al volver, no reutilizar datos obsoletos.
7. Mantener los destinos todavía no implementados etiquetados como pendientes. HU-004 solo debe activar su entrada de clientes.

### Paso 10. Tratar estados de interfaz y fallos

Representar explícitamente estos estados:

- **Cargando:** no presentar valores antiguos como si fueran resultado actual.
- **Lista vacía:** no hay clientes activos; ofrecer la acción de crear.
- **Sin coincidencias:** existe búsqueda, pero no hay resultados; permitir limpiar o ajustar la consulta.
- **Coincidencias posibles:** explicar que son sugerencias y dejar continuar con la creación.
- **Detalle vacío por sección:** no hay dispositivos o historial asociado; no implica que el cliente esté incompleto.
- **Error:** fallo de lectura/escritura mostrado con opción de reintento y sin descartar los datos del formulario.
- **Cliente inactivo:** informarlo y preservar el acceso de solo lectura necesario para contexto histórico.

Capturar cancelación de coroutines y relanzarla; no convertir excepciones de Room en lista vacía ni mostrar mensajes de éxito antes de persistir.

### Paso 11. Pruebas automatizadas

#### Dominio y validación

- Nombre en blanco, con espacios, Unicode, acentos, guiones y longitudes límite.
- Teléfono, correo y notas nulos/vacíos; correo inválido solo cuando se proporciona.
- Dos clientes con el mismo teléfono se permiten.
- Normalización de búsqueda con mayúsculas, acentos, espacios y puntuación/dígitos de teléfono.
- Resultados de nombre y teléfono y coincidencias parciales conforme a la decisión de búsqueda.
- Una búsqueda vacía no dispara consultas amplias inesperadas.

#### DAO y base local

- Alta y lectura del cliente; campos opcionales permanecen nulos o guardados según la política.
- Lista y búsqueda excluyen bajas lógicas.
- Edición conserva ID y fecha de creación y actualiza `actualizadoEn`.
- Baja lógica conserva el registro y sus referencias a dispositivos y órdenes.
- No existe unicidad accidental de teléfono/correo.
- Consultas de dispositivos y órdenes devuelven únicamente los registros del cliente esperado, con orden estable.
- Errores de SQL/Room se propagan para que la interfaz pueda representarlos.
- Si se cambia el esquema, migración desde versión 2 conserva todos los datos y pasa validación de Room.

#### Historial e integración

- Editar el nombre o teléfono no modifica `cliente_nombre_snapshot` ni `cliente_contacto_snapshot` de órdenes existentes.
- Dar de baja al cliente no elimina sus dispositivos, órdenes, eventos u otros registros relacionados.
- Una orden histórica sigue mostrando contexto aunque el cliente esté inactivo.
- El alta no crea un duplicado automáticamente ni fusiona personas con teléfono compartido.
- Tras alta/edición/baja, la lista y ficha se refrescan desde la base local.

#### Navegación y privacidad

- Sin sesión autenticada no se puede abrir lista, ficha ni formulario por botón, intent o recreación de pantalla.
- Tras expirar la sesión, la información deja de mostrarse y se activa el bloqueo de HU-002.
- La vista de recientes no expone nombres, teléfonos, correos, notas ni historial.
- No se requieren permisos de contactos ni conectividad.
- No aparecen datos personales en logs o errores.

### Paso 12. Validación manual de UX

Probar en emulador y dispositivo:

1. Abrir la lista sin clientes, crear uno con solo nombre y verificar que aparece.
2. Crear clientes con teléfonos compartidos y revisar que se ofrece coincidencia pero no se bloquea el alta.
3. Buscar por acentos, espacios y formatos diferentes de teléfono; limpiar la búsqueda.
4. Editar el teléfono/correo y comprobar que la lista y la ficha se actualizan.
5. Consultar una ficha sin dispositivos y otra con dispositivos e historial.
6. Dar de baja un cliente con historial y comprobar el texto de confirmación, su desaparición de la lista activa y la conservación del historial.
7. Simular error de almacenamiento, cancelar una operación y revisar reintento/preservación de formulario.
8. Verificar lector de pantalla, tamaño de texto ampliado, teclado, targets táctiles y pantallas pequeñas.
9. Repetir las operaciones en modo avión.

## 7. Matriz de entregables sugeridos

| Área | Trabajo esperado |
|---|---|
| DAO cliente | Consultas por ID, lista/búsqueda de activos y baja lógica; revisar/remover uso de borrado físico |
| Dominio | Normalización de búsqueda, validación y reglas para edición/baja |
| Interfaz clientes | Lista, búsqueda, revisión de coincidencias, formulario y ficha |
| Consultas relacionadas | Dispositivos e historial de órdenes consultables desde una ficha |
| Navegación | Conectar menú de HU-003 con el catálogo dentro de la sesión protegida |
| Pruebas | Unitarias de reglas, DAO/base local, snapshots, baja, navegación y privacidad |
| Documentación | Actualizar esquema solo si se cambia el modelo; registrar política de baja y búsqueda |

## 8. Definición de terminado

HU-004 queda lista cuando:

1. El menú autenticado abre una lista local de clientes.
2. Nombre/teléfono permiten revisar coincidencias antes de crear, sin imponer unicidad del teléfono.
3. Un cliente puede guardarse con nombre válido y sin teléfono, correo ni notas.
4. Alta y edición persisten localmente y la edición actualiza el cliente sin cambiar snapshots de órdenes.
5. La ficha muestra dispositivos e historial relacionados, distinguiendo secciones vacías de errores.
6. La baja de clientes con relaciones conserva sus referencias y ofrece una alternativa explícita a la eliminación destructiva.
7. Clientes inactivos no aparecen como activos ni desaparecen del historial que los necesita.
8. Búsquedas, lista, ficha y cambios funcionan sin conexión y no filtran datos personales en logs.
9. Todas las rutas siguen protegidas por sesión, bloqueo por inactividad y `FLAG_SECURE` de HU-002.
10. Pruebas unitarias/DAO/integración relevantes, compilación y lint pasan; la UX se valida en tamaños de pantalla y con escala de texto ampliada.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| El teléfono se trata como identificador único | Usar coincidencias como sugerencias y probar la creación con números compartidos |
| Un `@Delete` rompe o destruye historial | Implementar baja lógica, revisar las llamadas al borrado físico y verificar FK |
| La edición altera documentos históricos | Actualizar únicamente `Cliente`; mantener snapshots de orden como fuente de verdad histórica |
| Una consulta de ficha mezcla datos de distintos clientes | Filtrar todas las relaciones por el ID estable y probar límites de asociación |
| Errores de almacenamiento aparecen como “sin clientes” | Modelar errores recuperables independientemente del estado vacío |
| Bajas aparecen para nuevas selecciones | Filtrar por `eliminadoEn IS NULL` en consultas activas |
| Notas contienen credenciales o información excesiva | Mostrar una advertencia, minimizar exposición y nunca usar notas para secretos |
| Una ruta directa evita autenticación o expone recientes | Mantener el módulo bajo el contenedor protegido y conservar `FLAG_SECURE` |
| Búsqueda local no escala | Filtrar/paginar en DAO, mantener orden estable y evitar cargar todo el catálogo |
