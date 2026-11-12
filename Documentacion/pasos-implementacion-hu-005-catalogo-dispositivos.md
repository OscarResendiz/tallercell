# Pasos de implementación: HU-005, catálogo de dispositivos de clientes

## 1. Objetivo

Implementar el registro y la consulta de los dispositivos que pertenecen a los clientes del taller. Cada dispositivo debe poder identificarse aunque no tenga IMEI ni número de serie; sus datos actuales podrán corregirse sin cambiar la evidencia de órdenes anteriores, y sus referencias históricas deberán conservarse al dejar de estar activo.

La implementación debe:

1. Permitir registrar un dispositivo asociado a un cliente activo con tipo, marca, modelo y color.
2. Admitir dispositivos sin IMEI o número de serie y no tratar esos datos como identificadores únicos.
3. Mostrar los dispositivos en la ficha del cliente y permitir abrir la ficha de cada equipo y su historial de órdenes.
4. Permitir corregir los datos actuales del dispositivo sin modificar las instantáneas de las órdenes existentes.
5. Evitar la eliminación física de un dispositivo que pueda estar referenciado por una orden; conservar su historial.
6. Reducir la exposición de serie e IMEI en listas, pantallas y cualquier exportación futura.
7. Operar localmente, sin permisos de contactos ni conectividad, y dentro de la sesión autenticada de HU-002.

Este documento desarrolla HU-005 de [historias-usuario-acceso-menu-catalogos-taller-local.md](historias-usuario-acceso-menu-catalogos-taller-local.md). HU-004 proporciona el catálogo de clientes y su ficha; la recepción, las órdenes nuevas y su ciclo completo son responsabilidades de historias posteriores.

## 2. Referencias y estado actual

- [Historias de usuario de acceso, menú y catálogos](historias-usuario-acceso-menu-catalogos-taller-local.md): HU-005, criterios de aceptación 1 a 5.
- [Funcionalidades de la aplicación](funcionalidades-taller-local-android.md): F-022 a F-024, así como las políticas de recepción e historial relacionadas.
- [Entidades de la base de datos](entidades-base-datos-taller-local.md): entidad `Dispositivo`, relación con `Cliente` y `OrdenServicio`, restricciones de borrado y datos instantáneos de órdenes.
- [Pasos de implementación de HU-004](pasos-implementacion-hu-004-catalogo-clientes.md): navegación autenticada, ficha de cliente, privacidad, estados de carga/error y manejo local.
- `DataBase/Entidades/Dispositivo/Dispositivo.kt`: ya define tipo, marca, modelo, color, IMEI y serie opcionales, fechas de creación y actualización y una FK requerida a `Cliente` con `RESTRICT`.
- `DataBase/Entidades/Dispositivo/DispositivoDao.kt`: permite consultas básicas y por cliente; también expone borrado físico y no filtra por cliente activo ni ordena el historial.
- `DataBase/Entidades/OrdenServicio/OrdenServicio.kt`: contiene `dispositivoId`, snapshots de tipo, marca, modelo, color, serie e IMEI, y una FK al dispositivo con `RESTRICT`.
- `DataBase/Entidades/OrdenServicio/OrdenServicioDao.kt`: ya consulta órdenes por dispositivo; se debe acordar el tratamiento de órdenes archivadas en la vista histórica.
- `DataBase/TallerCelularesDatabase.kt`: la base está en versión 2 y registra `Dispositivo` y `OrdenServicio`.
- `Modulos/Catalogos/Clientes/CatalogoClientes.kt`: HU-004 ya muestra los dispositivos asociados en la ficha del cliente; la administración independiente del equipo sigue pendiente.
- `Modulos/tablero/TableroPrincipal.kt`: el menú muestra el destino “Dispositivos” como pendiente; HU-005 debe conectar ese destino sin salir de la sesión protegida.

El modelo persistente ya existe, pero no hay operaciones de interfaz para administrar equipos ni un ciclo de baja lógica de dispositivos.

## 3. Alcance

### Incluido

- Acceso a los dispositivos desde la ficha de un cliente y desde la entrada autenticada “Dispositivos” del menú.
- Lista de equipos asociados a un cliente, con identificadores visuales suficientes para distinguir modelos similares.
- Alta y edición de tipo, marca, modelo, color, número de serie e IMEI.
- Ficha de solo lectura del dispositivo con sus datos actuales y órdenes relacionadas.
- Baja lógica de dispositivos con confirmación y conservación de FK e historial.
- Búsqueda acotada al cliente seleccionado y revisión de posibles coincidencias por identificadores, sin imponer unicidad.
- Manejo diferenciado de carga, listas vacías, errores y reintentos.
- Accesibilidad, privacidad de identificadores y pruebas de dominio, persistencia, relaciones, historial y navegación.

### No incluido

- Crear, editar, cancelar o cambiar el estado de órdenes de servicio.
- Registrar una recepción, seleccionar pruebas de ingreso o generar comprobantes.
- Guardar patrones, PIN o contraseñas de desbloqueo en los datos del dispositivo; las credenciales se rigen por las historias de órdenes y nunca pertenecen a las notas generales.
- Exigir IMEI o número de serie, validar su unicidad global o impedir que distintos equipos compartan o tengan corregido un identificador.
- Sincronizar dispositivos con contactos, servicios en línea, inventarios externos o cuentas del fabricante.
- Eliminar físicamente órdenes, clientes o dispositivos históricos.

## 4. Decisiones de producto y datos

1. **Datos requeridos:** cliente activo, tipo, marca y modelo  son obligatorios. IMEI, número de serie y color son opcionales y se guardan como `NULL` cuando el campo queda vacío.
2. **Formato de los campos:** recortar espacios exteriores y rechazar campos obligatorios vacíos; definir límites razonables de longitud. No aplicar reglas nacionales rígidas a marca, modelo, color o serie.
3. **IMEI y serie:** aceptar y conservar el valor escrito sin exigir que el usuario conozca un formato particular. Puede eliminarse espacio exterior, pero no cambiar silenciosamente el identificador original ni truncarlo. No imponer unicidad: puede haber errores por corregir, equipos dual-SIM o identificadores duplicados reportados.
4. **Coincidencias:** antes de guardar se pueden sugerir equipos del mismo cliente con IMEI o serie coincidente. Una sugerencia no prueba identidad; permitir abrir el equipo sugerido y continuar el alta sin fusionarlo ni asociarle órdenes.
5. **Historial:** consultar por `dispositivo_id` y ordenar por fecha de recepción descendente, con desempate estable por folio e ID. Incluir órdenes canceladas, cerradas y archivadas que conserven la referencia; identificarlas cuando estén archivadas y no habilitar edición desde esta ficha.
6. **Corrección de datos:** la edición solo cambia las columnas actuales de `dispositivo` y `actualizado_en`. Los snapshots de cada orden son inmutables aunque el equipo se corrija, se dé de baja o cambie de cliente en una futura función.
7. **Baja:** adoptar baja lógica con `eliminadoEn`, dado que el esquema usa FK `RESTRICT` y una orden ya puede referenciar al equipo. No ofrecer borrado físico ni borrar órdenes asociadas. Un dispositivo inactivo no se ofrece para nuevas operaciones, pero permanece consultable en el historial y en el contexto de la ficha histórica.
8. **Reactivación:** no incluirla en HU-005. Si se requiere después, deberá ser una acción explícita, con permisos y pruebas propios; no debe reactivarse al editar ni al encontrar una coincidencia.
9. **Privacidad:** no mostrar IMEI o serie completos en filas de lista. En la ficha limitar su exposición a la identificación del equipo, con valores enmascarados por defecto y una acción explícita para revelar el dato completo si el flujo lo necesita. No incluirlos en mensajes de error, logs o telemetría. Aplicar el mismo criterio a exportaciones futuras.
10. **Acceso desde el menú:** la entrada “Dispositivos” abre una selección/búsqueda de clientes activos y, tras elegir uno, su lista de equipos. La ficha de cliente ofrece la misma lista sin duplicar la lógica de consulta. No crear una ruta pública ni una Activity exportada para el catálogo.
11. **Relación a cliente:** un dispositivo pertenece a un cliente y no se cambia de dueño en una edición común. Si el negocio necesita transferencias, definirlas como una operación explícita con su propia política de auditoría e historial.
12. **Migración:** añadir `eliminado_en` requiere migrar Room de versión 2 a 3. La migración debe asignar `NULL` a los equipos existentes, preservar IDs, FK, índices y filas y no utilizar migración destructiva.

### Decisiones aplicadas para esta guía

- Se conservan el esquema y los campos existentes salvo la columna opcional `eliminado_en` para implementar una baja no destructiva.
- La lista de un cliente consulta páginas de 50 elementos con orden estable; el buscador y las relaciones se filtran en Room.
- Tipo, marca y  modelo  son obligatorios. IMEI , número de serie y color son opcionales, no únicos y se presentan enmascarados fuera de la ficha.
- La ficha de cliente mantiene la sección de dispositivos y la amplía con acciones de administración; la entrada global “Dispositivos” permite llegar a equipos sin obligar a entrar primero desde la pantalla de clientes.
- Al editar se conserva la referencia al cliente actual y no se actualiza ninguna orden ni snapshot. Al dar de baja se conserva el equipo y las órdenes.

## 5. Trazabilidad de criterios a comportamientos

| Criterio HU-005 | Comportamiento observable | Evidencia |
|---|---|---|
| 1. Registrar el equipo | Desde un cliente activo se guarda un equipo con tipo, marca, modelo y color obligatorios; IMEI y serie pueden omitirse | Pruebas de validación, alta y persistencia de identificadores nulos |
| 2. Distinguir y consultar equipos | La ficha del cliente identifica cada equipo; al abrirlo muestra las órdenes asociadas a ese dispositivo | Pruebas de filtros por cliente/dispositivo y orden del historial |
| 3. Corregir datos actuales | La ficha refleja el valor corregido y las órdenes mantienen sus snapshots originales | Prueba de edición comparando datos actuales e instantáneas |
| 4. Conservar referencias al eliminar | La baja lógica oculta el equipo de la lista activa, pero mantiene fila, FK e historial consultable | Prueba de baja con órdenes relacionadas y revisión del FK |
| 5. Limitar exposición | Las listas muestran IMEI/serie enmascarados o los omiten; solo la ficha permite revelar lo necesario | Pruebas de presentación, accesibilidad y ausencia de identificadores en errores/logs |

## 6. Pasos de implementación

### Paso 1. Confirmar el estado de las relaciones y consumidores

1. Revisar `Dispositivo`, `DispositivoDao`, `OrdenServicio`, `OrdenServicioDao` y `TallerCelularesDatabase`.
2. Buscar todos los usos de `dispositivoDao().eliminar()`, `obtenerTodos()`, `actualizar()` y `obtenerPorClienteId()` antes de cambiar o limitar contratos.
3. Confirmar que `OrdenServicio.dispositivoId` puede ser nulo en órdenes ocasionales y que una FK existente usa `RESTRICT`; no relajar esa restricción.
4. Confirmar que la sección de dispositivos de la ficha HU-004 está asociada con el cliente seleccionado y no mezcla equipos de otro cliente.
5. Identificar dónde se crean snapshots de órdenes en las historias de recepción. Documentar que HU-005 no los modifica y que al crear órdenes futuras deben copiarse los datos vigentes del equipo.

### Paso 2. Incorporar baja lógica con una migración no destructiva

1. Agregar `eliminadoEn: String? = null` a `Dispositivo`, mapeado a `eliminado_en`.
2. Incrementar la base de datos de versión 2 a versión 3 y definir `MIGRACION_2_3`; la migración debe agregar la columna nullable sin reconstruir ni borrar la tabla.
3. Registrar la migración en el builder junto con las migraciones existentes; no activar `fallbackToDestructiveMigration`.
4. Mantener sin cambios la PK, la FK requerida a `Cliente`, las restricciones `RESTRICT` y los índices actuales.
5. Actualizar la documentación de modelo de datos para describir `eliminado_en` como baja lógica, no como borrado físico.
6. Añadir una prueba de migración desde la versión 2 que compruebe:
   - dispositivos y órdenes previos siguen presentes;
   - las filas existentes reciben `eliminado_en = NULL`;
   - las relaciones y los índices siguen válidos;
   - se pueden insertar y consultar equipos después de migrar.

Si una prueba revela una necesidad de reconstrucción, primero definir SQL explícito de copia y verificación; nunca resolver el cambio borrando la base.

### Paso 3. Definir operaciones Room por estado y propietario

Ampliar `DispositivoDao` con operaciones explícitas:

1. `obtenerPorId(id)` para contexto histórico y `obtenerActivoPorId(id)` para editar y operar sobre un equipo activo.
2. Listar dispositivos activos de un cliente con `cliente_id = :clienteId AND eliminado_en IS NULL`, orden estable por marca, modelo, tipo e ID.
3. Listar el historial de todos los equipos del cliente, incluyendo bajas, solo cuando la ficha histórica lo necesite; indicar el estado inactivo en la presentación.
4. Buscar coincidencias activas por serie/IMEI dentro del cliente seleccionado y con límite explícito. Usar parámetros enlazados, normalización segura y no requerir un IMEI/serie para guardar.
5. Insertar el nuevo dispositivo con ID según `EntidadId.nuevo()`, cliente activo y fechas UTC.
6. Actualizar solo tipo, marca, modelo, color, IMEI, serie y `actualizado_en`, condicionando la operación a `id` y `eliminado_en IS NULL`. No reemplazar un registro completo con `@Update` si pudiera alterar su cliente, fecha de creación o estado.
7. Añadir `darDeBaja(id, eliminadoEn, actualizadoEn)` como `UPDATE` condicional; retirar o dejar inaccesible para el flujo de interfaz el borrado físico `@Delete` después de confirmar sus consumidores.
8. Ordenar el historial de órdenes por fecha descendente y claves estables, filtrando por `dispositivo_id`. No ocultar órdenes archivadas del historial; exponer su estado de archivo en la UI.
9. Contar o consultar las órdenes relacionadas antes de confirmar la baja para informar el impacto. La operación de baja no debe borrar ni modificar esas órdenes.
10. No obtener todos los equipos para filtrarlos en memoria, no incluir credenciales del equipo y no registrar valores de identificadores en logs.

### Paso 4. Implementar reglas de dominio y presentación de identificadores

1. Crear reglas reutilizables para validar tipo, marca, modelo y color requeridos luego de recortar espacios exteriores.
2. Definir máximos razonables para los textos e indicar el error junto al campo. Preservar Unicode, acentos, guiones y marcas con puntuación.
3. Aceptar IMEI y serie ausentes; normalizar vacíos a `NULL` consistentemente.
4. Validar longitud máxima de IMEI/serie para proteger almacenamiento y UI, sin tratarlos como únicos ni rechazar correcciones por formato regional desconocido.
5. Implementar una función de presentación que enmascare identificadores en listas (por ejemplo, mostrar únicamente los últimos cuatro caracteres cuando tengan longitud suficiente); no alterar lo almacenado.
6. En ficha, mantener la máscara por defecto y permitir revelar el identificador únicamente con una acción accesible y explícita; volver a ocultarlo al salir de la ficha o bloquear la sesión.
7. No copiar los identificadores al portapapeles automáticamente, colocarlos en descripciones accesibles de listas ni exponerlos en notificaciones, errores o telemetría.
8. Las coincidencias de serie/IMEI son recomendaciones revisables; permitir continuar con el alta y nunca fusionar o cambiar relaciones automáticamente.

### Paso 5. Construir la lista de dispositivos y navegación de cliente

1. Crear `Modulos/Catalogos/Dispositivos/` para las reglas, las pantallas y el acceso específico del módulo.
2. En la ficha del cliente HU-004, convertir cada dispositivo en un elemento seleccionable que muestre tipo, marca, modelo y color, sin serie/IMEI completos.
3. Añadir “Agregar dispositivo” solo para clientes activos. No permitir altas asociadas a un cliente inexistente o dado de baja.
4. Añadir una ruta de lista con cliente estable por ID; toda lectura, escritura y búsqueda del módulo debe mantener ese `clienteId`.
5. Cambiar “Dispositivos — Pendiente” a disponible en el menú autenticado del tablero. Al abrirlo, mostrar clientes activos para seleccionar y continuar a su lista de dispositivos; no crear una segunda actividad pública.
6. Proporcionar navegación de regreso desde lista y ficha de equipo a la ficha/lista de cliente o al tablero conservando sesión y evitando pantallas duplicadas.
7. Al volver de alta, edición o baja, recargar la lista desde Room; no reutilizar una lista obsoleta guardada en memoria.
8. Distinguir lista sin equipos, búsqueda sin coincidencias, resultados, carga inicial, paginación y error recuperable.

### Paso 6. Implementar alta y edición

1. Reutilizar un formulario para alta y edición con títulos y botones inequívocos.
2. En alta, cargar y confirmar que el cliente está activo antes de permitir guardar; persistir su ID, crear un ID local, fechas UTC y `eliminadoEn = null`.
3. Solicitar tipo, marca, modelo y color. Presentar serie e IMEI como opcionales y advertir que no son necesarios para guardar.
4. En edición, cargar el equipo activo por su ID y dejar el cliente asociado en solo lectura.
5. Guardar con una operación DAO limitada a los campos editables. Preservar ID, `clienteId`, `creadoEn` y estado de baja.
6. No escribir ni actualizar ninguna fila de `OrdenServicio`; comprobar que snapshots históricos como `marca_snapshot`, `modelo_snapshot`, `serie_snapshot` e `imei_snapshot` permanecen intactos.
7. Prevenir doble envío. Mostrar confirmación únicamente después de que Room complete la escritura y la lectura posterior encuentre el registro.
8. Si el cliente/equipo ya no está activo, o falla la lectura/escritura, mantener los valores ingresados y mostrar un mensaje con reintento.
9. Antes de crear, mostrar coincidencias posibles del mismo cliente cuando haya serie/IMEI disponible; ofrecer abrir el equipo existente o continuar sin asociarlo ni fusionarlo.

### Paso 7. Implementar la ficha y el historial de un equipo

1. Abrir la ficha por ID y consultar de nuevo tanto el equipo como el cliente relacionado para verificar el ámbito de la ruta.
2. Mostrar cliente, tipo, marca, modelo y color. Para equipos inactivos, informar su estado y ocultar acciones de edición y baja repetida.
3. Presentar serie e IMEI con máscara por defecto y acción explícita de revelado; nunca mostrar credenciales de desbloqueo.
4. Mostrar acciones “Editar” y “Dar de baja” solo mientras el equipo esté activo.
5. Consultar órdenes mediante `OrdenServicioDao.obtenerPorDispositivoId(id)`, de forma indexada y ordenada. Usar `dispositivo_id`, no una coincidencia de nombre/modelo.
6. En cada fila de historial mostrar folio, estado, fecha y campos de identificación apropiados. Mostrar órdenes archivadas como tales sin excluirlas.
7. No ofrecer editar órdenes, alterar sus estados, reasociarlas a otro equipo ni sustituir sus snapshots desde el catálogo.
8. Mostrar “Sin órdenes relacionadas” independientemente de los errores de consulta. Si falla solo la carga del historial, preservar la ficha y ofrecer reintentar esa sección.
9. Dar acceso a la ficha de equipo desde la lista de cliente y desde cualquier resultado de coincidencia sin perder el contexto de retorno.

### Paso 8. Implementar baja lógica y conservación

1. Llamar a la acción “Dar de baja” o “Archivar dispositivo”; no presentarla como eliminación permanente.
2. Antes de confirmar, contar órdenes relacionadas y mostrar que el equipo dejará de estar activo, pero sus órdenes y referencias se conservarán.
3. Guardar `eliminado_en` y `actualizado_en` con el mismo instante UTC usando `UPDATE ... WHERE eliminado_en IS NULL`.
4. No llamar a `DispositivoDao.eliminar()` ni borrar o alterar órdenes relacionadas. La FK `RESTRICT` debe seguir protegiendo el historial.
5. Excluir equipos dados de baja de las listas activas, nuevas selecciones y coincidencias; conservar acceso de lectura desde la ficha y el historial.
6. Si una baja falla, mostrar error y reintento sin fingir éxito ni cambiar la UI a estado inactivo.
7. Probar la baja con cero, una y varias órdenes relacionadas y verificar que ninguna fila histórica se elimina.

### Paso 9. Integrar con HU-004, HU-003 y HU-002

1. Ampliar la sección “Dispositivos” de la ficha HU-004 para abrir el catálogo del cliente y mostrar controles de alta/edición solo para clientes activos.
2. Conectar la entrada “Dispositivos” del menú de HU-003 a una lista/selector de clientes, y desde allí al catálogo correspondiente.
3. Mantener todo el flujo en el contenedor protegido de `AccesoActivity` o una navegación equivalente con el mismo guard; evitar una Activity exportada o acceso por intent no autenticado.
4. Mantener `FLAG_SECURE`, el bloqueo por inactividad y la limpieza de contenido de HU-002. Al bloquear, descartar estado de pantalla y volver a solicitar acceso según el comportamiento actual.
5. Actualizar la ficha del cliente al volver de alta, edición o baja y actualizar la lista de dispositivos al volver de la ficha de equipo.
6. No ampliar la vigencia de sesión por consultas o escrituras en curso. Cancelar trabajos visuales al salir y propagar `CancellationException`.
7. Conservar en el menú los demás destinos pendientes y cambiar únicamente la disponibilidad de “Dispositivos”.
8. No añadir permisos de IMEI, teléfono, almacenamiento o red: los datos se capturan manualmente y se guardan localmente.

### Paso 10. Tratar estados, fallos y accesibilidad

Representar explícitamente:

- **Cargando:** lista, ficha o historial todavía no consultados.
- **Lista vacía:** el cliente no tiene dispositivos activos; ofrecer alta si el cliente está activo.
- **Sin coincidencias:** hay filtro, pero ningún equipo coincide; permitir limpiarlo.
- **Coincidencias posibles:** indicar que no son una confirmación y permitir continuar.
- **Equipo inactivo:** mostrar que se conserva para historial y deshabilitar edición y operaciones nuevas.
- **Historial vacío:** no hay órdenes relacionadas; es distinto de un error.
- **Error:** presentar un mensaje recuperable y reintento, preservar formulario y mostrar datos independientes cuya carga sí tuvo éxito.

También:

1. Asegurar etiquetas accesibles que no revelen IMEI/serie completos en filas o avisos del lector de pantalla.
2. Mantener targets táctiles, orden de foco y navegación por teclado razonables; probar tamaños de fuente ampliados y pantallas pequeñas.
3. Usar mensajes sin datos personales ni identificadores completos. No convertir una excepción en una lista vacía ni capturar cancelaciones como error.

### Paso 11. Pruebas automatizadas

#### Dominio y formulario

- Tipo, marca, modelo y color ausentes, solo espacios y con valores válidos con acentos/Unicode.
- IMEI y serie nulos, vacíos, con formato variado y en límites de longitud.
- Un dispositivo sin IMEI ni serie se puede guardar.
- Identificadores coincidentes solo generan sugerencias; no bloquean la creación ni crean unicidad.
- El enmascaramiento muestra solo la porción acordada, maneja identificadores cortos/vacíos y no altera el dato original.
- Revelar y volver a ocultar un identificador no lo expone después de salir o bloquear.

#### DAO, base local y migración

- Alta con FK al cliente correcto, fechas e ID; rechazo de cliente inexistente por la FK.
- Un cliente ve solo sus dispositivos activos; las búsquedas se limitan al `clienteId`.
- Orden estable y paginación sin duplicados al cargar la página siguiente.
- Edición conserva ID, cliente, creación y baja; actualiza `actualizado_en`.
- Baja lógica excluye el equipo de listados activos y lo conserva en lecturas históricas.
- Equipos con IMEI/serie iguales se pueden almacenar sin conflicto artificial.
- Migración desde versión 2 mantiene todos los equipos, órdenes, FK e índices y asigna `NULL` al nuevo campo.
- Fallos de SQL/Room se propagan para que la UI los represente.

#### Historial e integración

- Consultar un equipo devuelve únicamente órdenes con su `dispositivo_id`, con orden descendente estable.
- Órdenes activas, terminales y archivadas permanecen visibles según la política histórica.
- Editar marca/modelo/color/serie/IMEI no cambia snapshots de órdenes previas.
- Dar de baja un equipo con órdenes preserva todas las filas, los snapshots y las FK.
- Un equipo sin órdenes y un error de consulta se presentan como estados distintos.
- Un equipo de otro cliente no aparece en ficha, búsqueda o edición del cliente actual.

#### Navegación y privacidad

- Sin sesión autenticada no se puede abrir catálogo, lista, ficha o formulario por botón, back stack o intent.
- Menú “Dispositivos” permite elegir cliente activo; la ficha HU-004 abre la misma lista asociada.
- Regreso desde ficha y formulario mantiene el cliente correcto y no duplica pantallas.
- Al expirar la sesión se limpia la pantalla y se activa el bloqueo existente.
- IMEI/serie completos no aparecen en filas, errores, logs ni telemetría; `FLAG_SECURE` permanece activo.

### Paso 12. Validación manual de UX

Probar en emulador y dispositivo:

1. Abrir Dispositivos desde la ficha de un cliente y desde el menú; verificar que ambas rutas muestran el mismo catálogo.
2. Crear un equipo indicando únicamente tipo, marca, modelo y color; confirmar que IMEI y serie pueden omitirse.
3. Registrar varios equipos con marca/modelo similares y confirmar que se distinguen sin mostrar identificadores completos en la lista.
4. Ingresar una serie o IMEI coincidente, revisar la sugerencia y comprobar que se puede continuar sin fusionar registros.
5. Corregir datos de un dispositivo con órdenes históricas y verificar que el historial sigue mostrando los snapshots de recepción.
6. Abrir el historial de equipos sin órdenes, con varias órdenes y con órdenes archivadas.
7. Dar de baja un equipo con órdenes y comprobar texto de confirmación, desaparición de listas activas y conservación de historial.
8. Simular error de lectura/escritura y cancelación; verificar reintento y que los datos del formulario no se pierden.
9. Probar lector de pantalla, identificación accesible enmascarada, acción explícita de revelar, texto ampliado y pantallas pequeñas.
10. Repetir las operaciones en modo avión y después del bloqueo por inactividad.

## 7. Matriz de entregables sugeridos

| Área | Trabajo esperado |
|---|---|
| Datos y migración | Campo de baja lógica, migración 2→3 no destructiva y prueba de preservación |
| DAO de dispositivo | Consultas por ID/cliente/estado, coincidencias acotadas, edición parcial y baja lógica |
| Dominio | Validación de campos, normalización de vacíos y presentación protegida de identificadores |
| Interfaz de dispositivos | Lista por cliente, formulario de alta/edición, ficha e historial por equipo |
| Ficha de cliente | Enlaces a dispositivos y acciones disponibles según su estado |
| Navegación | Entrada “Dispositivos” desde menú autenticado y flujo de selección de cliente |
| Historial | Consulta ordenada por `dispositivo_id`, conservación de snapshots y estados archivados |
| Pruebas | Unitarias, DAO/base local, migración, historial, baja, privacidad y sesión |
| Documentación | Actualizar modelo de datos y decisiones de baja y exposición de identificadores |

## 8. Definición de terminado

HU-005 queda lista cuando:

1. Desde un cliente activo se registra un dispositivo con tipo, marca, modelo y color; IMEI y serie son opcionales.
2. Cada cliente puede consultar su lista de equipos activos, abrir la ficha de uno y regresar sin perder contexto.
3. El menú autenticado permite acceder a Dispositivos y elegir el cliente correspondiente.
4. Edición y consulta reflejan los datos actuales sin modificar snapshots ni historial.
5. La ficha muestra las órdenes vinculadas al equipo por ID, incluidas las archivadas según la política.
6. Dar de baja conserva el registro, las FK y las órdenes, y lo excluye de altas/listas activas.
7. La migración desde la base versión 2 no pierde ni altera datos existentes.
8. IMEI y serie no se requieren, no se imponen como únicos y no se exponen completos en listas, logs o errores.
9. Todas las rutas permanecen bajo autenticación local, bloqueo por inactividad y `FLAG_SECURE`.
10. Las pruebas pertinentes, compilación y lint pasan; UX y accesibilidad se revisan en varios tamaños y con escala de texto ampliada.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Un borrado físico falla por FK o destruye contexto | Retirar el borrado físico del flujo y usar baja lógica con `RESTRICT` conservado |
| Una migración de esquema elimina datos | Migración explícita 2→3 y prueba de migración con equipos y órdenes existentes |
| La edición altera la recepción histórica | Actualizar solo el registro de dispositivo; proteger y probar todos los snapshots de orden |
| Se mezclan equipos de varios clientes | Mantener `clienteId` en consultas/operaciones y probar límites entre propietarios |
| IMEI o serie se exponen accidentalmente | Enmascarar por defecto, revelar explícitamente y revisar listas, accesibilidad, logs y errores |
| No hay identificadores disponibles | No requerirlos ni imponer unicidad; identificar por tipo, marca, modelo, color y cliente |
| Se oculta el historial de un equipo dado de baja | Mantener consulta histórica por ID e incluir órdenes archivadas con indicación de estado |
| Una coincidencia de identificador fusiona equipos | Presentarla solo como sugerencia y exigir una decisión explícita; nunca asociar órdenes automáticamente |
| Error de lectura se interpreta como historial vacío | Modelar carga, vacío y error como estados independientes con reintento |
| La navegación evita la autenticación | Alojar el módulo dentro del contenedor protegido y conservar `FLAG_SECURE` y bloqueo por inactividad |
