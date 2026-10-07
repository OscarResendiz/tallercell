# Funcionalidades para administrar un taller de reparación de celulares

## 1. Propósito y alcance

Este documento define las funcionalidades de una aplicación Android para administrar un único taller de reparación de celulares, operado por una sola persona. La aplicación debe funcionar sin cuenta en línea, servidor, API, sincronización ni conexión a internet; el acceso sí requiere autenticación local. Los datos y archivos de trabajo se guardan localmente en el dispositivo.

El flujo de órdenes se basa en [ciclo-vida-orden-servicio.md](ciclo-vida-orden-servicio.md), adaptado para quitar sucursales, usuarios múltiples y asignaciones entre personal. La persona usuaria puede actuar como recepción, técnico y responsable del taller.

El respaldo será un archivo que la persona genera y guarda o transfiere manualmente. La aplicación no enviará copias a servicios de nube ni se conectará a sistemas externos. Si Android ofrece un proveedor de archivos en la nube al elegir el destino, será una decisión explícita de la persona, fuera de una transferencia automática de la aplicación.

## 2. Principios del producto

- **Local primero y sin red:** las operaciones principales deben estar disponibles en modo avión. La aplicación no debe requerir ni iniciar conexiones a API, telemetría, autenticación externa o servicios remotos.
- **Un taller y una persona:** no se requieren empresas, sucursales, cuentas en línea, roles ni administración de empleados. Sí se requiere identificar localmente a la persona que abre la aplicación para impedir el acceso casual de terceros.
- **Historial confiable:** cambios importantes de estado, presupuestos, pagos y entregas deben conservar fecha, hora y detalle; no se debe borrar silenciosamente la evidencia de una orden.
- **Privacidad por minimización:** guardar solo los datos necesarios. Los códigos de desbloqueo del equipo del cliente son opcionales, temporales y protegidos; nunca solicitar credenciales de cuentas del cliente ni datos de tarjeta.
- **Respaldo recuperable:** una copia debe incluir los datos y adjuntos necesarios para continuar usando la aplicación en otro dispositivo Android.
- **Operación táctil:** las tareas frecuentes deben ser cómodas en una pantalla de teléfono, con formularios breves y búsqueda accesible.

## 3. Prioridad

- **MVP:** necesario para recibir, reparar, cobrar y devolver equipos sin perder el historial.
- **Siguiente etapa:** mejora útil, pero la operación básica puede comenzar sin ella.

## 4. Funcionalidades

### 4.1 Inicio y tablero

| ID | Prioridad | Funcionalidad |
|---|---|---|
| F-001 | MVP | Mostrar un resumen de órdenes por estado: diagnóstico, autorización, refacción, reparación, control de calidad, listas y devoluciones pendientes. |
| F-002 | MVP | Mostrar órdenes con folio, cliente, modelo, fecha de ingreso, estado, tiempo en estado y próxima acción. |
| F-003 | MVP | Permitir filtrar por estado y buscar por folio, cliente, teléfono, marca, modelo o serie/IMEI. |
| F-004 | MVP | Destacar órdenes estancadas y fechas prometidas vencidas sin cerrar ni cambiar su estado automáticamente. |
| F-005 | Siguiente etapa | Ofrecer accesos rápidos para crear una orden, registrar un pago o consultar piezas con poco inventario. |

### 4.2 Configuración del taller

| ID | Prioridad | Funcionalidad |
|---|---|---|
| F-010 | MVP | Configurar nombre comercial, teléfono, dirección, moneda, zona horaria y datos que aparecerán en comprobantes. |
| F-011 | MVP | Configurar prefijo y secuencia local de folios, y evitar reutilizar folios ya asignados. |
| F-012 | MVP | Configurar servicios, cargos de diagnóstico, impuestos cuando apliquen, métodos de pago, plazos de seguimiento y garantía ofrecida. |
| F-013 | MVP | Permitir definir qué datos son opcionales y los textos de recepción, autorización, limitaciones y garantía usados en los comprobantes. |
| F-014 | Siguiente etapa | Configurar plantillas de mensajes para copiar o compartir manualmente, sin envío automático. |
| F-015 | MVP | Al iniciar por primera vez, configurar un método de desbloqueo local: contraseña propia de la aplicación o autenticación del dispositivo Android, incluyendo biometría cuando esté disponible. Exigir autenticación al abrir la aplicación y después de un periodo de inactividad configurable. No crear una cuenta ni validar la identidad en línea. |
| F-016 | MVP | Requerir autenticación reciente antes de mostrar un patrón, PIN o contraseña de un equipo, exportar o restaurar respaldos, o borrar datos. Permitir cambiar el método de acceso desde la configuración tras autenticarse. |

### 4.3 Clientes y dispositivos

| ID | Prioridad | Funcionalidad |
|---|---|---|
| F-020 | MVP | Crear y editar clientes con nombre y uno o más medios de contacto opcionales. Evitar duplicados mediante búsqueda previa por teléfono o nombre. |
| F-021 | MVP | Mantener el historial de órdenes de cada cliente y permitir registrar un cliente ocasional con los datos mínimos. |
| F-022 | MVP | Registrar equipos con tipo, marca, modelo, color y número de serie o IMEI opcional. No exigir IMEI para guardar una orden. |
| F-023 | MVP | Asociar uno o más dispositivos a un cliente y consultar su historial de reparaciones. |
| F-024 | MVP | Permitir corregir datos de contacto sin alterar los datos históricos de las órdenes ya cerradas. |
| F-025 | MVP | En cada orden, permitir guardar opcionalmente y con consentimiento explícito un patrón, PIN o contraseña de desbloqueo del equipo, solo para que el técnico realice las pruebas acordadas. Capturar el patrón en una interfaz dedicada y almacenar el dato cifrado, nunca en notas generales, historial, notificaciones ni comprobantes. No aceptar contraseñas de Google, Apple u otras cuentas. |
| F-026 | MVP | Ocultar el dato de desbloqueo por defecto; mostrarlo solo tras autenticación reciente y acción explícita. Borrarlo al entregar o cancelar la orden. Si se necesita conservarlo más tiempo, solicitar confirmación explícita y una fecha de eliminación. |

### 4.4 Recepción y resguardo

| ID | Prioridad | Funcionalidad |
|---|---|---|
| F-030 | MVP | Crear una orden con folio, fecha y hora de recepción, cliente, dispositivo, falla descrita por el cliente y fecha prometida estimada. |
| F-031 | MVP | Registrar condición física, daños visibles, reparaciones previas observables y accesorios recibidos, cada uno por separado. |
| F-032 | MVP | Registrar pruebas de ingreso (encendido, pantalla, cámaras, botones, carga, audio, red u otras), su resultado y las pruebas no realizadas con su motivo. |
| F-033 | MVP | Adjuntar fotografías de recepción con consentimiento y asociarlas a la orden. La aplicación debe conservarlas localmente y permitir revisarlas o quitarlas antes de confirmar. |
| F-034 | MVP | Registrar ubicación física del equipo dentro del taller, por ejemplo estante o caja, y actualizarla cuando se mueva. |
| F-035 | MVP | Generar un comprobante de recepción con folio, datos esenciales del equipo, condición, accesorios y términos informados; permitir imprimirlo o compartirlo mediante las opciones de Android. |
| F-036 | Siguiente etapa | Generar una etiqueta imprimible con folio y código QR o de barras para identificar el equipo. El folio también debe poder consultarse sin etiqueta. |

### 4.5 Diagnóstico, presupuesto y autorización

| ID | Prioridad | Funcionalidad |
|---|---|---|
| F-040 | MVP | Separar falla reportada, síntomas reproducidos, diagnóstico, riesgos, factibilidad, alcance propuesto, piezas y tiempo estimado. |
| F-041 | MVP | Registrar resultado técnico: reparable, reparable con condición o no reparable; mantenerlo separado de la decisión comercial del cliente. |
| F-042 | MVP | Crear presupuestos con conceptos, piezas, mano de obra, descuentos, impuestos configurados, anticipo, total, vigencia y limitaciones. |
| F-043 | MVP | Guardar cada versión de presupuesto y su historial; una nueva cotización no debe sobrescribir las anteriores. |
| F-044 | MVP | Registrar autorización o rechazo con fecha, alcance aprobado, importe, método/evidencia y nota. Para una autorización verbal, registrar quién confirmó y qué aceptó. |
| F-045 | MVP | Impedir el registro de inicio de reparación si no hay autorización vigente para el alcance, excepto cuando se documente que el trabajo está cubierto por una excepción configurada. |
| F-046 | MVP | Solicitar una nueva autorización antes de continuar si cambian alcance, riesgo o precio. |
| F-047 | Siguiente etapa | Generar una cotización imprimible o compartible como PDF y un texto breve que el usuario pueda copiar al canal elegido. |

### 4.6 Seguimiento de la orden y reparación

| ID | Prioridad | Funcionalidad |
|---|---|---|
| F-050 | MVP | Administrar los estados `Recibida`, `En cola de diagnóstico`, `En diagnóstico`, `Esperando autorización`, `Esperando refacción`, `En cola de reparación`, `En reparación`, `En control de calidad`, `Lista para entrega`, `En devolución sin reparar`, `Entregada` y `Cancelada`. |
| F-051 | MVP | Validar transiciones de estado y solicitar motivo cuando se devuelve una orden a una etapa anterior, se cancela o se detiene. |
| F-052 | MVP | Registrar en un historial cada cambio de estado con fecha, hora, estado anterior, nuevo estado, motivo y nota. El actor puede identificarse como la persona usuaria local. |
| F-053 | MVP | Registrar notas de diagnóstico y reparación, avances, piezas instaladas, números de parte, tiempo de trabajo si se requiere y hallazgos nuevos. |
| F-054 | MVP | Registrar motivo de espera, fecha de último contacto y próxima fecha de seguimiento para autorización, información del cliente, refacción, aprobación adicional o servicio externo. |
| F-055 | MVP | En una espera por refacción, registrar pieza, variante/calidad, cantidad, proveedor o fuente, costo, fecha de pedido, fecha estimada y estado de compra/recepción. |
| F-056 | MVP | Marcar una pieza recibida solo después de confirmar físicamente su modelo, variante y condición. No sustituir una pieza incompatible o no disponible sin registrar la decisión y autorización del cliente. |
| F-057 | MVP | Registrar cancelación y causa sin ocultar si el equipo sigue bajo resguardo o falta devolverlo. |
| F-058 | Siguiente etapa | Programar recordatorios locales para órdenes estancadas, piezas atrasadas y equipos listos sin recoger, con plazos configurables. |

### 4.7 Control de calidad y entrega

| ID | Prioridad | Funcionalidad |
|---|---|---|
| F-060 | MVP | Registrar pruebas posteriores a la reparación, resultado por prueba, observaciones, fecha y persona responsable. |
| F-061 | MVP | Comparar pruebas posteriores con las de ingreso y registrar pruebas no ejecutables y limitaciones aceptadas. |
| F-062 | MVP | Exigir QA aprobado para pasar a `Lista para entrega`, o registrar una excepción y aceptación explícita del cliente. |
| F-063 | MVP | Si QA falla, devolver la orden a diagnóstico, reparación o espera adecuada, conservando el motivo y los resultados anteriores. |
| F-064 | MVP | Antes de entregar, confirmar trabajo, piezas, accesorios, saldo, garantía y persona autorizada para recoger. |
| F-065 | MVP | Registrar pago total o parcial, método, importe, fecha, comprobante/referencia no sensible y saldo pendiente. Nunca guardar datos de tarjeta. |
| F-066 | MVP | Registrar fecha y hora de entrega, confirmación de quien recoge, resultado reparada/no reparada y estado final de accesorios. La orden se cierra solo al salir el equipo del resguardo. |
| F-067 | MVP | Generar comprobante de pago/entrega con desglose, saldo, reparación realizada, limitaciones y garantía aplicable. |
| F-068 | Siguiente etapa | Permitir registrar más de un intento de contacto para órdenes listas o devoluciones pendientes, con fecha, canal y resultado, sin enviar mensajes automáticamente. |

### 4.8 Inventario y caja

| ID | Prioridad | Funcionalidad |
|---|---|---|
| F-070 | MVP | Registrar piezas y consumibles con descripción, variante, número de parte opcional, costo, precio de venta y existencia. |
| F-071 | MVP | Registrar entradas, ajustes y salidas de inventario con fecha, cantidad, motivo y vínculo a orden cuando corresponda. No modificar existencias sin conservar el movimiento. |
| F-072 | MVP | Descontar o reservar una pieza para una orden solo al confirmar el movimiento; permitir cancelar o corregir con un movimiento compensatorio. |
| F-073 | MVP | Consultar piezas disponibles y movimientos; advertir cuando una pieza requerida no tiene existencia suficiente. |
| F-074 | MVP | Registrar anticipos, cobros, reembolsos y gastos del taller con importe, fecha, método y vínculo opcional a orden. |
| F-075 | MVP | Mostrar resumen de ingresos, gastos, anticipos pendientes y saldos por cobrar en un periodo seleccionable. No presentarlo como contabilidad fiscal. |
| F-076 | Siguiente etapa | Alertar sobre existencias bajo un mínimo configurable y mostrar piezas de mayor consumo o costo. |

### 4.9 Garantías, búsquedas e informes

| ID | Prioridad | Funcionalidad |
|---|---|---|
| F-080 | MVP | Registrar la garantía ofrecida por orden, con cobertura, exclusiones, inicio y vencimiento según la política del taller. |
| F-081 | MVP | Crear una orden nueva vinculada a la original para un regreso o reclamo; indicar si es garantía, cortesía o una nueva reparación presupuestada. |
| F-082 | MVP | Buscar y filtrar órdenes por estado, fecha, cliente, dispositivo, folio y garantía; permitir consultar órdenes cerradas sin reabrirlas. |
| F-083 | MVP | Mostrar informes básicos: órdenes por estado y periodo, reparadas/no reparadas, ingresos/gastos, saldos pendientes y piezas utilizadas. |
| F-084 | Siguiente etapa | Exportar informes a CSV o PDF mediante una acción iniciada por la persona usuaria. |

### 4.10 Respaldo, restauración y mantenimiento

| ID | Prioridad | Funcionalidad |
|---|---|---|
| F-090 | MVP | Crear un respaldo completo que incluya base de datos, clientes, dispositivos, órdenes, historial, pagos, inventario, configuración, fotografías y adjuntos asociados. Las credenciales temporales de desbloqueo quedan excluidas por defecto. |
| F-091 | MVP | Guardar el respaldo en una ubicación elegida por la persona mediante el selector de archivos de Android; permitir transferirlo por USB u otro medio bajo control del usuario. |
| F-092 | MVP | Cifrar el respaldo con una contraseña elegida por la persona y explicar que no podrá recuperarse si la olvida. Por defecto, excluir patrones, PIN y contraseñas de equipos; permitir incluirlos solo mediante una opción explícita con advertencia, protegidos por la misma contraseña robusta del respaldo. No guardar esa contraseña dentro del respaldo. |
| F-093 | MVP | Incluir versión del formato, fecha de creación, tamaño, integridad verificable y resumen de contenido para detectar copias incompletas o incompatibles. |
| F-094 | MVP | Antes de restaurar, validar contraseña, integridad, espacio disponible y compatibilidad; mostrar qué contiene la copia, incluyendo si contiene credenciales temporales, y pedir confirmación. |
| F-095 | MVP | Restaurar el conjunto completo de forma atómica: si falla, conservar los datos actuales. Advertir claramente que una restauración reemplaza los datos locales para evitar mezclar historiales accidentalmente. Volver a cifrar cualquier credencial incluida usando protección local del nuevo dispositivo. |
| F-096 | MVP | Permitir probar la validez de un respaldo sin reemplazar los datos actuales. |
| F-097 | MVP | Recordar localmente cuándo se hizo el último respaldo y mostrar una advertencia configurable si está desactualizado. La aplicación no debe iniciar un respaldo remoto o automático en nube. |
| F-098 | MVP | Ofrecer eliminación de datos con confirmación reforzada y recomendar crear un respaldo antes de borrar. No borrar órdenes históricas como método normal de limpieza. |
| F-099 | Siguiente etapa | Permitir exportar datos seleccionados para consulta, distinguiendo claramente una exportación parcial de un respaldo restaurable. |

## 5. Reglas de negocio indispensables

1. No iniciar reparación ni comprar piezas especiales sin autorización y condiciones de anticipo acordadas.
2. Si cambian alcance, precio o riesgo, detener el trabajo afectado y guardar una nueva autorización antes de continuar.
3. `No reparable` es un resultado técnico; `Entregada` confirma devolución física. No son estados intercambiables.
4. Una orden lista para entrega sigue abierta y bajo resguardo hasta que el equipo sea devuelto.
5. No pasar a lista para entrega sin QA aprobado o una aceptación de limitaciones documentada.
6. No sobrescribir cotizaciones, pagos, movimientos de inventario ni transiciones previas. Las correcciones deben quedar registradas.
7. Las notificaciones son recordatorios locales o contenido preparado para compartir. No hay envío automático por SMS, correo o mensajería.
8. La eliminación de un cliente no debe destruir la evidencia financiera o de órdenes; cuando se requiera, anonimizar datos personales conservando referencias necesarias.
9. El patrón, PIN o contraseña de desbloqueo del equipo solo puede guardarse si el cliente lo autoriza y es necesario para las pruebas acordadas. Se almacena cifrado, se revela solo tras autenticación local reciente y se elimina al cerrar la orden, salvo que el cliente autorice expresamente una retención con fecha de vencimiento.
10. No solicitar ni guardar credenciales de cuentas del cliente, códigos de recuperación ni datos de tarjeta. No copiar secretos a notas, registros técnicos, notificaciones, comprobantes o informes.
11. Los importes, garantías, cargos por diagnóstico, impuestos, almacenamiento y plazos deben ser configurables; la aplicación no debe asumir reglas legales universales.

## 6. Datos principales

| Entidad | Datos clave |
|---|---|
| Configuración del taller | Nombre, contacto, dirección, moneda, zona horaria, folio, impuestos y políticas configurables. |
| Acceso local | Método de desbloqueo, tiempo de inactividad y verificador seguro si se usa contraseña propia; nunca guardar la contraseña en texto claro. |
| Cliente | Nombre, contactos opcionales, notas no sensibles y fecha de alta. |
| Dispositivo | Cliente, tipo, marca, modelo, color y serie/IMEI opcional. |
| Orden | Folio, cliente, dispositivo, estado, fechas, falla reportada, condición, accesorios, ubicación y resultado final. |
| Evento de orden | Orden, transición o acción, fecha/hora, motivo, nota y usuario local. |
| Diagnóstico y cotización | Hallazgos, riesgos, alcance, importes, vigencia, versión y evidencia de autorización. |
| Pieza y movimiento | Descripción, variante, cantidad, costo/precio, entrada/salida/ajuste y vínculo a orden. |
| Pago y gasto | Tipo, importe, fecha, método, referencia no sensible y orden relacionada cuando aplique. |
| Prueba de calidad | Prueba, resultado, observación, fecha y referencia a prueba inicial cuando aplique. |
| Adjunto | Orden, tipo, ruta/identificador local, fecha y consentimiento cuando corresponda. |
| Garantía | Orden original, cobertura, exclusiones, vigencia y órdenes de regreso vinculadas. |
| Credencial temporal del equipo | Orden, tipo (patrón/PIN/contraseña), valor cifrado, consentimiento y fecha de eliminación. No forma parte de las notas ni del historial general. |

## 7. Requisitos de privacidad y funcionamiento local

- La aplicación debe iniciar y completar sus tareas centrales sin conexión, incluyendo crear órdenes, consultar historial, registrar pagos y restaurar un respaldo local.
- No incluir inicio de sesión remoto, API, SDK de analítica, anuncios, sincronización, telemetría ni dependencia de servicios web. La autenticación de la persona usuaria es local.
- Exigir desbloqueo local con contraseña de la aplicación o credencial/biometría del dispositivo Android. Aplicar bloqueo al volver del segundo plano o al vencer el tiempo de inactividad; no exponer pantallas sensibles en la vista de aplicaciones recientes.
- Guardar la contraseña propia de la aplicación como verificador seguro, nunca en texto claro. La recuperación no debe depender de un servidor; explicar durante la configuración las opciones locales de recuperación y el riesgo de perder acceso a los datos.
- Cifrar los secretos de desbloqueo de los equipos con claves protegidas por Android Keystore cuando esté disponible. No escribirlos en texto claro en base de datos, archivos temporales, portapapeles, logs ni copias automáticas del sistema.
- Los secretos de desbloqueo se excluyen por defecto del respaldo. Si la persona opta por incluirlos, deben quedar dentro del respaldo cifrado con su contraseña; al restaurar, descifrarlos solo en memoria y protegerlos nuevamente con claves del dispositivo de destino.
- Solicitar únicamente permisos de Android necesarios para cámara, notificaciones locales y selección de archivos; pedirlos en el momento de uso y explicar su propósito.
- Guardar base de datos y adjuntos en almacenamiento privado de la aplicación. No confiar en una carpeta pública como única copia de los datos activos.
- Proteger el acceso con autenticación local obligatoria. Cifrar respaldos; documentar la protección de datos en reposo y el comportamiento ante cambio de credencial, pérdida del dispositivo o restauración.
- Evitar que datos personales o fotografías aparezcan en logs, notificaciones visibles o nombres de archivo innecesarios.
- Las opciones Android de imprimir o compartir deben abrirse solo por acción explícita y mostrar al usuario qué contenido va a salir de la aplicación.
- Una desinstalación o pérdida del dispositivo puede eliminar los datos locales; la interfaz debe advertirlo y facilitar respaldos periódicos.

## 8. Criterios de aceptación generales

1. En modo avión se puede crear una orden completa, recorrer diagnóstico, autorización, reparación, QA, cobro y entrega, y consultar después su historial.
2. El sistema impide iniciar trabajos sin autorización y evita cerrar como entregada una orden cuyo equipo aún está bajo resguardo.
3. Un respaldo con fotografías se puede validar y restaurar en otro dispositivo Android compatible, conservando órdenes, folios, adjuntos e historial.
4. Una copia con contraseña incorrecta, dañada o de versión no compatible no altera los datos actuales y explica cómo proceder.
5. El usuario puede imprimir o compartir un comprobante, pero no ocurre ninguna transferencia sin confirmación explícita en Android.
6. El uso principal no requiere crear cuenta, tener varias sucursales ni configurar usuarios adicionales; al abrir la aplicación sí exige el método local de identificación elegido.
7. El usuario puede registrar opcionalmente un patrón, PIN o contraseña del equipo con consentimiento, verla solo tras autenticarse y comprobar que se elimina al entregar o cancelar la orden.
8. Un respaldo excluye esos secretos por defecto; si se opta por incluirlos, están cifrados y se restauran protegidos localmente en el nuevo dispositivo.

## 9. Alcance recomendado de implementación

1. **Base operativa:** configuración local, clientes, dispositivos, folios, recepción, órdenes, estados e historial.
2. **Reparación segura:** diagnóstico, presupuestos versionados, autorización, espera de refacciones, reparación y QA.
3. **Cierre del trabajo:** pagos, gastos básicos, entrega, garantía, comprobantes e informes iniciales.
4. **Continuidad de datos:** respaldo cifrado completo, validación, restauración segura y recordatorio local.
5. **Mejoras:** inventario con alertas, etiquetas, recordatorios, exportaciones e informes ampliados.

La recomendación es no aplazar el respaldo completo hasta una versión futura: perder el dispositivo sin una copia puede significar perder toda la operación e historial del taller.