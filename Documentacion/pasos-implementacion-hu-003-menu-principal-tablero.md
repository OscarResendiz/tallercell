# Pasos de implementación: HU-003, menú principal y tablero

## 1. Objetivo

Implementar la pantalla principal que recibe a la persona después de autenticarse y le permite orientarse en la operación del taller. El tablero debe mostrar el estado de las órdenes y ayudar a identificar qué requiere atención, sin modificar automáticamente los registros.

La implementación debe permitir:

1. Navegar desde un menú a las áreas operativas previstas.
2. Consultar un resumen y una lista de órdenes con la información esencial para seguimiento.
3. Buscar y filtrar órdenes localmente.
4. Detectar órdenes estancadas o con fecha prometida vencida, sin cambiar su estado.
5. Entender qué hacer cuando todavía no existen órdenes.

Este documento desarrolla `HU-003` de [historias-usuario-acceso-menu-catalogos-taller-local.md](historias-usuario-acceso-menu-catalogos-taller-local.md). El ingreso y bloqueo pertenecen a HU-002; el registro y ciclo de vida completo de órdenes se desarrollan en historias posteriores.

## 2. Referencias y criterios

- [Historias de usuario de acceso, menú y catálogos](historias-usuario-acceso-menu-catalogos-taller-local.md): HU-003, criterios de aceptación 1 a 5.
- [Funcionalidades de la aplicación](funcionalidades-taller-local-android.md): F-001 a F-005, F-082 y F-083.
- [Entidades de la base de datos](entidades-base-datos-taller-local.md): `OrdenServicio`, `EventoOrden`, `Cliente`, `Dispositivo`, `RecordatorioLocal` y entidades operativas relacionadas.
- [Ciclo de vida de una orden](ciclo-vida-orden-servicio.md): estados del proceso, esperas, seguimiento y campos útiles en el tablero móvil.
- [Pasos de HU-002](pasos-implementacion-hu-002-ingreso-y-reautenticacion.md): sesión local obligatoria, bloqueo por inactividad y privacidad en recientes.
- [Plan de trabajo de la aplicación](plan-trabajo-aplicacion-taller-local.md): pasos 3 a 5 para base local, autenticación, recepción y tablero.
- [AccesoActivity.kt](../TallerCelulares/app/src/main/java/com/example/tallercelulares/Modulos/acceso/AccesoActivity.kt): flujo de autenticación que debe habilitar el acceso a la pantalla principal.

La HU se considera completa cuando los cinco criterios de aceptación funcionan con datos persistidos, sin red y respetando el estado de autenticación de HU-002.

## 3. Alcance

### Incluido

- Destino principal posterior al acceso autenticado.
- Menú de navegación hacia tablero y áreas incluidas en HU-003.
- Tablero con resumen de órdenes agrupadas por etapa/estado.
- Lista ordenada con folio, cliente, modelo, fecha de ingreso, estado, antigüedad y próxima acción.
- Búsqueda y filtro de órdenes sin conexión.
- Indicadores informativos de atraso y estancamiento.
- Estados de carga, error y contenido vacío.
- Reconsulta al volver de una operación y respeto del bloqueo local.
- Pruebas de repositorio/consulta, filtros, resumen, navegación, privacidad y accesibilidad.

### No incluido

- Implementación funcional completa de las pantallas de órdenes, clientes, dispositivos, servicios, inventario, caja, garantías, informes, configuración o respaldo. Cada área debe conectarse al módulo de su propia historia; los accesos aún no disponibles se identifican como pendientes y no simulan éxito.
- Crear, editar, transicionar o cerrar órdenes desde el tablero.
- Enviar mensajes, notificaciones o recordatorios automáticamente.
- Cambiar el estado de una orden por vencimiento, antigüedad o inactividad.
- Sincronización, backend, inicio de sesión en línea o analítica.
- Redefinir los estados del dominio o las reglas operativas del ciclo de servicio.

## 4. Decisiones previas a la implementación

Resolver o registrar las siguientes decisiones antes de fijar el diseño:

1. **Menú y disponibilidad:** confirmar si todas las áreas indicadas por HU-003 aparecen desde el MVP. Si un módulo aún no existe, decidir si el acceso se muestra deshabilitado con una explicación o se oculta hasta que esté disponible; nunca conducir a una pantalla vacía que parezca un error.
2. **Orden de prioridad de la lista:** acordar el orden predeterminado. Como recomendación, mostrar primero las órdenes que requieren atención (atrasadas/estancadas), luego las más antiguas en espera, y después las demás por última transición/recepción descendente. Permitir cambiar el orden solo si no dificulta localizar urgencias.
3. **Significado de “estancada”:** definir umbrales editables por estado o utilizar los plazos de seguimiento que ya configure el taller. No codificar un límite universal; mientras falte configuración, el indicador no debe presentarse como incumplimiento legal ni cambiar el estado.
4. **Próxima acción:** establecer reglas legibles derivadas del estado y los datos disponibles. Si el modelo no permite identificar una acción segura, mostrar “Revisar orden” o no especificada, no inventar tareas ni asignar responsables.
5. **Fecha prometida:** acordar si una orden sin fecha prometida carece de indicador de vencimiento. No inferir una fecha a partir de la fecha de ingreso.
6. **Zona horaria:** presentar fechas/horas según `ConfiguracionTaller.zona_horaria`, pero conservar las fechas persistidas en UTC como fuente de verdad.
7. **Búsqueda:** definir normalización de mayúsculas, acentos, espacios y teléfonos. La búsqueda por IMEI/serie debe respetar la protección y exposición mínima de esos identificadores.
8. **Paginación y volumen:** estimar el máximo esperado de órdenes locales y decidir paginación/ventanas de consulta para evitar cargar toda la historia en memoria.
9. **Diseño de áreas pendientes:** mostrar de manera explícita cuáles destinos están implementados. No reutilizar enlaces ficticios a `MainActivity` ni permitir eludir la autenticación de HU-002.

## 5. Pasos de implementación

### Paso 1. Trazar los criterios a comportamientos y pruebas

Crear una matriz de trazabilidad:

| Criterio HU-003 | Comportamiento observable | Evidencia |
|---|---|---|
| 1. Menú de áreas | Tras autenticar se ve el tablero y los destinos previstos; cada destino implementado abre su módulo y los pendientes informan su estado | Pruebas de navegación y matriz de módulos disponibles |
| 2. Resumen y lista | Se presentan conteos por etapa y los campos requeridos por orden | Prueba de mapeo de datos y prueba visual con varios estados |
| 3. Buscar y filtrar | Consulta local por estado, folio, cliente, teléfono, marca, modelo y serie/IMEI según los datos disponibles | Pruebas de consulta combinada y normalización |
| 4. Destacar sin mutar | Vencimiento o estancamiento produce una señal visual y no cambia el estado persistido | Prueba que compara la entidad antes/después de renderizar/refrescar |
| 5. Estado vacío | Sin órdenes se muestra explicación y un acceso claro a recepción/crear orden cuando exista | Prueba de estado vacío y navegación |

Asociar cada escenario a pruebas automatizadas. Las validaciones táctiles, lector de pantalla y presentación en dispositivos se complementan con pruebas instrumentadas/manuales.

### Paso 2. Definir la frontera con autenticación y navegación

1. Integrar el tablero como destino posterior al resultado de autenticación exitosa de HU-002.
2. Mantener el chequeo de sesión en un punto central de navegación o contenedor protegido; no basar la protección solo en ocultar un botón.
3. Asegurar que el inicio en frío vaya primero a HU-002 y que ninguna pantalla o enlace directo abra información antes de autenticar.
4. Si la sesión vence o la aplicación vuelve tras el umbral, cubrir el contenido y volver al acceso según las reglas de HU-002 antes de mostrar datos.
5. Al salir del tablero hacia una sección protegida, conservar la misma sesión; al bloquearse, no dejar visible el contenido en recientes.
6. Separar el destino operativo de la pantalla provisional “Acceso concedido”. Una vez implementado HU-003, el éxito de autenticación debe navegar al tablero y no a una pantalla que finja serlo.
7. Definir el comportamiento del botón Atrás: regresar al destino anterior permitido sin volver al login de forma que exponga una sesión activa ni abandonar inadvertidamente una operación.

### Paso 3. Acordar menú, jerarquía y adaptación móvil

Diseñar el menú pensando en uso táctil y en una sola persona usuaria:

1. Priorizar tablero e ingreso a órdenes.
2. Agrupar clientes/dispositivos, servicios, inventario, caja, garantías, informes, configuración y respaldo en categorías comprensibles.
3. Mantener navegación consistente entre menú lateral, barra inferior o tarjetas; escoger un patrón apropiado al número de destinos y al tamaño de pantalla.
4. Mostrar etiqueta y un icono reconocible por destino; no depender únicamente del color o de iconos sin texto.
5. Marcar claramente destinos no implementados o bloqueados por dependencias.
6. Evitar duplicar operaciones sensibles dentro del menú: respaldo, cambio de acceso o exposición de credenciales deben invocar la autorización reciente de HU-002.
7. Adaptar tamaños, espacios, scroll y targets táctiles para pantallas pequeñas y fuente ampliada.

### Paso 4. Revisar el esquema y definir los datos derivados

Antes de crear consultas:

1. Verificar que `OrdenServicio` provea folio, estado, fecha de recepción, fecha prometida y snapshots del equipo/cliente que deben seguir visibles aunque cambien los catálogos.
2. Usar snapshots (`cliente_nombre_snapshot`, `modelo_snapshot`, `serie_snapshot`, `imei_snapshot`) para mantener el contenido histórico de la orden.
3. Consultar la relación a `Cliente` y `Dispositivo` solo cuando se necesiten datos actuales adicionales. No reemplazar snapshots históricos con valores maestros actualizados.
4. Consultar `EventoOrden` para obtener la marca de tiempo de la última transición a estado y calcular el tiempo en ese estado. Si no existe evento inicial, definir un fallback explícito a `recibida_en` y mostrar una antigüedad coherente.
5. Si el modelo no almacena “próxima acción”, derivarla mediante una función pura basada en estado y dependencias disponibles. Documentar el mapeo y ofrecer un valor neutro cuando falte información.
6. Determinar qué estados entran en cada grupo del resumen (diagnóstico, autorización, refacción, reparación, control de calidad, listas y devoluciones pendientes) a partir de los códigos controlados del esquema.
7. No inferir “lista” o “pendiente” a partir de etiquetas localizadas. Comparar códigos estables del dominio.
8. Revisar índices existentes para estado/fecha, folio, cliente y dispositivo. Proponer migración solo si una consulta real del tablero lo necesita.
9. Mantener las reglas de baja: órdenes eliminadas lógicamente/canceladas no deben desaparecer si la regla exige conservarlas; definir qué estados finales aparecen por defecto y permitir consultarlos en historial.

### Paso 5. Implementar consultas locales de órdenes

Definir las consultas en DAO/repositorio, no filtrar únicamente la lista ya descargada por la interfaz:

1. **Resumen por estado:** devolver código estable y conteo, incluyendo cero para los grupos visibles, de forma que las tarjetas no desaparezcan por falta de órdenes.
2. **Listado del tablero:** seleccionar solo las columnas necesarias para tarjetas y ordenamiento.
3. **Búsqueda:** aceptar texto libre y aplicar normalización acordada a folio, snapshots de cliente/contacto/equipo y, si están presentes y permitidos, serie/IMEI.
4. **Filtro de estado:** permitir uno o varios estados o grupos; combinarlo correctamente con búsqueda.
5. **Combinación de filtros:** al limpiar búsqueda no limpiar el estado seleccionado, salvo acción explícita de limpiar filtros.
6. **Cancelación/actualización:** al cambiar el texto con rapidez, cancelar o invalidar resultados previos para que una consulta lenta no reemplace el resultado nuevo.
7. **Escalabilidad:** limitar/paginar resultados y usar una consulta reactiva local cuando sea útil. No cargar historial completo ni hacer una consulta por orden (evitar N+1).
8. **Errores:** exponer fallos de base de datos como estado de error con opción de reintento; no sustituirlos silenciosamente por una lista vacía.
9. **Localidad:** confirmar que ninguna búsqueda requiere permisos, red, servicio remoto ni SDK de analítica.

### Paso 6. Calcular etapa, antigüedad y próxima acción

1. Crear un mapeo de código de estado a grupo del tablero, etiqueta localizada y prioridad de atención.
2. Mantener etiquetas de interfaz traducibles separadas de los códigos persistidos.
3. Calcular la antigüedad de la orden desde `recibida_en`; calcular el tiempo en estado desde el último `EventoOrden` de transición.
4. Convertir UTC a la zona configurada solo para presentación; usar instantes absolutos para comparar fechas y evitar errores de cambio horario.
5. Definir próxima acción por estado con reglas simples y verificables; ejemplos posibles:
   - `RECIBIDA` / `COLA_DIAGNOSTICO`: iniciar revisión o diagnóstico.
   - `ESPERANDO_AUTORIZACION`: dar seguimiento a la decisión del cliente.
   - `ESPERANDO_REFACCION`: revisar disponibilidad/llegada de la pieza.
   - `EN_REPARACION`: registrar avance o resultado del trabajo.
   - `CONTROL_CALIDAD`: completar pruebas de calidad.
   - `LISTA_ENTREGA`: coordinar devolución y cierre.
   - `DEVOLUCION_SIN_REPARAR`: preparar y confirmar la devolución.
6. Ajustar ejemplos a los flujos habilitados realmente por el modelo/caso de uso; no permitir que una etiqueta sugiera una transición que no está autorizada.
7. Cuando la regla dependa de información que aún no existe, usar una indicación neutra en vez de asegurar una acción no respaldada.
8. Documentar cómo se tratan estados terminales (`ENTREGADA`, `CANCELADA`) en la lista activa y el historial.

### Paso 7. Detectar órdenes atrasadas o estancadas sin mutación

1. Marcar vencimiento cuando `fecha_prometida` exista y haya pasado respecto al instante actual de la zona local configurada.
2. Definir por estado qué antigüedad cuenta como estancamiento y cuál es su fuente de configuración. Mostrar que el indicador es de seguimiento.
3. No confundir “sin actualizar” con “sin trabajo”: utilizar el último evento de estado disponible y no la hora de sincronización de la interfaz.
4. Excluir estados terminales del resaltado de atraso operativo, salvo que exista una regla explícita para pendientes administrativos/entrega.
5. Presentar indicadores con texto o icono además del color y permitir filtrarlos/ordenarlos.
6. Calcular las alertas como proyección de lectura; el tablero no puede actualizar `OrdenServicio.estado`, insertar `EventoOrden` ni crear notificaciones al consultar.
7. Probar explícitamente que cambiar la fecha del dispositivo, recargar o abrir el tablero no persiste modificaciones.
8. Mostrar qué condición produjo la alerta (“promesa vencida”, “espera prolongada”) y, cuando se pueda, cuánto tiempo lleva.

### Paso 8. Implementar la interfaz del tablero

La pantalla debe incluir, en orden de lectura:

1. Encabezado con nombre de taller si está disponible y acceso al menú/cuenta local; no revelar credenciales.
2. Resumen por etapas con conteos y acceso táctil que aplica el filtro correspondiente.
3. Campo de búsqueda con hint que enumera ejemplos sin exponer datos de otra orden.
4. Filtro de estado, indicador del filtro aplicado y acción accesible para limpiarlo.
5. Lista de tarjetas compactas que muestren:
   - Folio.
   - Cliente (o etiqueta neutral si no se registró).
   - Marca/modelo del snapshot.
   - Fecha de recepción y antigüedad.
   - Estado y tiempo en estado.
   - Próxima acción.
   - Advertencia de atraso/estancamiento si aplica.
6. Acciones secundarias contextuales solo si están implementadas y autorizadas; tocar una tarjeta abre el detalle de la orden cuando esa historia exista.
7. Actualización explícita o automática al volver de una operación, cuidando que no aparezcan resultados obsoletos.
8. Estado de carga sin mostrar datos viejos como si fueran actuales.
9. Error de lectura con mensaje y reintento; no representar un fallo como “No hay órdenes”.
10. Estado vacío diferenciado entre “no hay órdenes” y “no hay coincidencias con esta búsqueda”. Incluir un acceso a recepción/crear orden únicamente cuando el módulo pueda recibir la navegación.

### Paso 9. Construir navegación segura y honesta para módulos futuros

1. Crear destinos tipados o identificadores centralizados para las secciones enumeradas en HU-003.
2. Conectar al módulo existente usando navegación interna, preservando la sesión y el estado mínimo necesario.
3. Para una sección no disponible, indicar “Disponible en una etapa posterior” o no mostrarla según la decisión UX; no abrir una pantalla vacía ni persistir un cambio inexistente.
4. El acceso a configuración debe seguir protegido y no exponer ni cambiar el método de acceso sin reautenticación.
5. El respaldo y el borrado de datos, al incorporarse, deben invocar `ContratoAutenticacionReciente` y consumir la autorización justo antes de ejecutar su acción.
6. Clientes, catálogo y operaciones con datos sensibles deben entrar por componentes protegidos bajo la sesión de HU-002.
7. Cuando una ruta profunda o una notificación local se agregue posteriormente, exigir la misma verificación de sesión antes de cargar detalles.

### Paso 10. Respetar búsqueda, datos personales y accesibilidad

1. No registrar consultas, teléfonos, IMEI, serie o nombres de clientes en logs.
2. Evitar mostrar IMEI/serie completos en el resumen; solo usarlos para encontrar la orden y mostrar el detalle cuando sea necesario/autorizado.
3. No copiar los datos del tablero al portapapeles ni generar notificaciones automáticas.
4. Proporcionar labels accesibles para búsqueda, filtros, tarjetas y estados; anunciar cambios de resultados sin leer toda la lista.
5. No codificar estados solo por color; combinar texto, iconografía y etiquetas accesibles.
6. Asegurar contraste, foco, targets táctiles, soporte para lector de pantalla, tamaño de fuente del sistema y orientación/viewport reducido.
7. Mantener la protección de recientes de HU-002 y ocultar los datos en capturas según la política de la aplicación.

### Paso 11. Probar consultas, cálculos y presentación

#### Pruebas de datos y dominio

- Conteo correcto por cada grupo con registros de todos los estados, estados vacíos y estados terminales.
- Mapeo de estado a etiqueta/grupo y próxima acción, incluidos estados desconocidos.
- Antigüedad con fecha presente, evento de transición, ausencia de evento y cambio de zona horaria.
- Búsqueda por folio, nombre, teléfono, marca, modelo y serie/IMEI cuando haya valores.
- Normalización de mayúsculas, acentos, espacios y formato telefónico según decisión.
- Filtro de estado junto con búsqueda, resultado vacío y limpieza independiente de cada filtro.
- Resultado ordenado según prioridad acordada y estabilidad de paginación.
- Vencimiento según fecha prometida y exclusión/tratamiento explícito de estados terminales.
- Cálculo de estancamiento por estado sin persistir una transición o evento.
- Entradas nulas u opcionales (cliente, contacto, modelo, IMEI, fecha prometida) no provocan fallos.

#### Pruebas de persistencia e integración

- La consulta refleja inserciones y cambios de estado locales tras regresar al tablero.
- La proyección de tarjetas usa snapshots y no cambia por editar después el catálogo de cliente/dispositivo.
- Las consultas usan índices/ventanas adecuados y no realizan N+1.
- Errores de DAO se exponen como error recuperable; no se muestran como lista vacía.
- Renderizar, refrescar, filtrar o destacar una orden no escribe en la base.
- El uso en modo avión produce los mismos resultados y no invoca servicios de red.

#### Pruebas de navegación y seguridad

- Sin autenticación no se llega al tablero desde lanzamiento, atrás, intent o enlace futuro.
- Tras autenticación correcta el tablero es el destino, no la pantalla provisional.
- Al expirar la sesión, se oculta el tablero y el acceso vuelve a solicitar autenticación.
- Los destinos de módulos protegidos mantienen el guard de sesión.
- Las vistas previas de aplicaciones recientes no muestran datos del cliente u órdenes.
- Los accesos pendientes no simulan operaciones exitosas ni alteran el estado de la orden.

#### Pruebas de interfaz

- Resumen, lista, búsqueda, filtros, carga, error, vacío y ausencia de coincidencias.
- Órdenes atrasadas/estancadas se distinguen y no cambian de estado.
- Teléfonos, folios largos, nombres/modelos extensos y ausencia de datos opcionales no rompen tarjetas.
- Lector de pantalla identifica estado, antigüedad, alerta y acción de cada orden.
- Validación manual en distintos tamaños de teléfono y escalas de fuente.

### Paso 12. Medir rendimiento y estabilidad

1. Probar con un volumen representativo y el volumen máximo previsto de órdenes históricas.
2. Medir tiempo hasta primer tablero utilizable, respuesta al filtro y tecleo de búsqueda.
3. Evitar cálculos costosos en el hilo principal; precomputar únicamente si hay una necesidad comprobada.
4. Cancelar consultas antiguas al iniciar otras y controlar cambios rápidos de estado/búsqueda.
5. No retener entidades o vistas completas fuera del ciclo de vida de la pantalla.
6. Si se necesita caché, invalidarla con cambios de órdenes/eventos y nunca tratarla como fuente de verdad.
7. Confirmar que el bloqueo por inactividad no dependa de que la pantalla esté realizando una consulta.

### Paso 13. Revisar privacidad y dependencias

- Mantener todos los datos en la base local privada y operar sin conexión.
- No añadir permiso `INTERNET`, SDK remoto, analítica, publicidad o sincronización para poblar el tablero.
- No emitir datos personales en logs, errores, eventos de analítica, nombres de tareas ni telemetría.
- Seguir la política de `FLAG_SECURE`/vista de recientes establecida por HU-002.
- Mostrar únicamente datos necesarios para orientar el trabajo; los secretos del equipo no forman parte de las tarjetas ni de la búsqueda visible.
- Revisar las opciones Android de compartir/imprimir para asegurar que solo se activen mediante acción explícita en la pantalla correspondiente.

### Paso 14. Documentar la matriz de módulos y cerrar HU-003

Antes del cierre:

1. Mantener una tabla de las áreas del menú con estado: implementada, en curso o pendiente y la HU responsable.
2. Documentar mapeo de estados a grupos, orden de prioridad y regla de próxima acción.
3. Registrar umbrales de estancamiento y su configuración/origen.
4. Actualizar documentación de esquema, consultas e índices si cambió el modelo.
5. Registrar casos de prueba automatizados y validación manual en dispositivo.
6. Confirmar que `MainActivity` u otras pantallas legacy no sean una ruta alternativa que omita la sesión o muestre contenido duplicado.

## 6. Registro de implementación en la aplicación actual

- El tablero se presenta dentro de `AccesoActivity`, después de autenticar, para conservar su sesión efímera, bloqueo por inactividad y protección `FLAG_SECURE`. `MainActivity` no contiene funcionalidad del tablero.
- El menú informa cuáles áreas están disponibles. El tablero está implementado; las operaciones de órdenes/recepción, clientes, dispositivos, servicios, inventario, caja, garantías, informes, configuración y respaldo continúan pendientes y no se simulan como exitosas.
- La lista excluye estados terminales de forma predeterminada; se pueden consultar seleccionando explícitamente `Entregada` o `Cancelada`. Los estados desconocidos conservan una etiqueta y acción neutrales.
- Los grupos del resumen son: diagnóstico (`RECIBIDA`, `COLA_DIAGNOSTICO`, `EN_DIAGNOSTICO`), espera (`ESPERANDO_AUTORIZACION`, `ESPERANDO_REFACCION`), reparación (`COLA_REPARACION`, `EN_REPARACION`, `CONTROL_CALIDAD`) y entrega (`LISTA_ENTREGA`, `DEVOLUCION_SIN_REPARAR`).
- Las alertas por espera usan los plazos ya configurados en `ConfiguracionTaller` para autorización, refacción y equipo listo. Un plazo de cero desactiva esa alerta; no se establece un umbral universal para otros estados. La fecha prometida se compara como instante y los estados terminales no se alertan.
- La consulta usa snapshots históricos, incluye serie/IMEI solo en la búsqueda (no en tarjetas), pagina de 50 órdenes y permite combinar texto con grupo o estado. La consulta, filtros y alertas son de solo lectura.
- Pruebas unitarias de `ReglasTableroTest` verifican búsqueda acentuada y por identificador, estado desconocido, plazos configurados, vencimiento y no mutación lógica de órdenes.

## 7. Definición de terminado

HU-003 queda lista cuando:

1. Después de una autenticación válida se abre el tablero; sin sesión válida no se muestra información de órdenes.
2. Los accesos indicados por el alcance de HU-003 están disponibles o se identifican de manera honesta como pendientes.
3. El resumen refleja los conteos reales y los grupos de estado definidos.
4. Cada fila muestra folio, cliente, modelo, fecha de ingreso, estado, antigüedad y próxima acción con valores seguros ante campos opcionales.
5. Buscar y filtrar por los campos establecidos funciona localmente y permite combinar/limpiar criterios correctamente.
6. Las órdenes vencidas o estancadas se reconocen visualmente y el tablero no modifica su estado ni historial.
7. Un taller sin órdenes ve una pantalla vacía útil con una ruta a recepción cuando esté disponible; una búsqueda sin coincidencias tiene un mensaje diferente.
8. Errores de almacenamiento se presentan como errores recuperables, nunca como cero órdenes.
9. El tablero respeta la sesión e inactividad de HU-002 y protege la vista de recientes.
10. Pruebas unitarias, de integración/navegación y visuales correspondientes pasan; compilación y lint pasan.
11. Se valida en modo avión y en dispositivos/tamaños de pantalla representativos.

## 8. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Un tablero inicial parece completo, pero enlaza a pantallas que no existen | Definir el estado de cada destino y presentar solo rutas reales o una explicación explícita de disponibilidad. |
| La lista usa datos maestros actuales y altera la representación histórica | Mostrar primero snapshots de recepción; usar datos maestros solo para información no histórica. |
| Una búsqueda en memoria omite órdenes antiguas o consume demasiada memoria | Filtrar y ordenar en DAO con índices y límites/paginación, probando el volumen esperado. |
| Fecha/hora local causa falsas alertas en cambios de zona o horario | Persistir/comparar instantes UTC y presentar conforme a la zona configurada. |
| Umbral universal marca como estancada una orden que espera autorización o piezas dentro del plazo | Usar reglas por estado y plazos configurables; explicar el motivo de alerta. |
| Mostrar identificadores completos expone datos personales | Minimizar lo visible, no incluir secretos y limitar serie/IMEI a búsquedas/detalles necesarias. |
| La UI trata una excepción de base como estado vacío | Modelar explícitamente `Loading`, `Success`, `Empty` y `Error` con reintento. |
| Un refresh de tablero cambia estado o crea eventos accidentalmente | Mantener cálculos de alerta como lecturas puras y verificar que no haya escrituras. |
| Una ruta del menú permite evitar el bloqueo al expirar sesión | Centralizar guard de navegación y cubrir acceso directo, reanudación y back stack con pruebas. |
| La lista crece y degrada el rendimiento en teléfonos modestos | Consultas acotadas, paginación, cancelación de búsquedas y pruebas de volumen. |
