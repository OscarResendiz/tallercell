# Pasos de implementación: HU-001, configuración inicial

## 1. Objetivo

Implementar el primer recorrido de la aplicación para que la persona responsable del taller pueda:

1. Entender que los datos permanecerán en el dispositivo y que no se requiere cuenta ni conexión.
2. Elegir y configurar un método de acceso local.
3. Capturar los datos y parámetros iniciales del taller.
4. Guardar la configuración localmente sin conservar contraseñas en texto claro.
5. Terminar en el flujo de ingreso autenticado.

Este documento desarrolla la historia `HU-001` de [historias-usuario-acceso-menu-catalogos-taller-local.md](historias-usuario-acceso-menu-catalogos-taller-local.md). La historia cubre el alta inicial; el ingreso recurrente y el menú posterior corresponden principalmente a `HU-002` y `HU-003`.

## 2. Referencias y criterios que deben satisfacerse

- [funcionalidades-taller-local-android.md](funcionalidades-taller-local-android.md): requisitos `F-010` a `F-016` y principios de operación local.
- [entidades-base-datos-taller-local.md](entidades-base-datos-taller-local.md): entidades `ConfiguracionTaller` y `ConfiguracionAccesoLocal`, convenciones de importes, fechas y protección de datos.
- [plan-trabajo-aplicacion-taller-local.md](plan-trabajo-aplicacion-taller-local.md): pasos 1, 3, 4 y 5.
- [ciclo-vida-orden-servicio.md](ciclo-vida-orden-servicio.md): reglas operativas que explican el uso de folios, cargos, condiciones y comprobantes.

La implementación se considera completa cuando cubre los seis criterios de aceptación de `HU-001`, sobrevive al cierre y reapertura de la aplicación y no expone credenciales ni depende de servicios remotos.

## 3. Alcance

### Incluido

- Detección de primera ejecución sin configuración completada.
- Asistente de configuración con pasos y navegación controlada.
- Explicación del almacenamiento local, la ausencia de cuenta y las consecuencias de perder el acceso.
- Selección y configuración del método de acceso local.
- Captura de los datos generales y los parámetros iniciales del taller disponibles en el modelo.
- Validación de campos requeridos, opcionales y valores numéricos.
- Persistencia local, recuperación frente a cierre durante el asistente y enrutamiento final al acceso.
- Pruebas de interfaz, persistencia, validación, accesibilidad básica y operación sin red.

### No incluido

- El diseño completo del login recurrente y sus mecanismos de bloqueo: atender `HU-002`.
- El menú principal y el tablero: atender `HU-003`.
- Alta de clientes, dispositivos, servicios u órdenes.
- Cambios a cotizaciones o comprobantes históricos como consecuencia de cambiar la configuración.
- Creación de cuenta, autenticación web, sincronización, telemetría o respaldo remoto.
- Implementación de recuperación de acceso con servidor. La aplicación no tiene servidor y debe comunicar claramente las limitaciones de la recuperación local.

## 4. Decisiones previas a la implementación

Resolver y registrar estas decisiones antes de cerrar el diseño de interfaz y almacenamiento:

1. **Métodos compatibles:** confirmar la versión mínima de Android y las APIs disponibles para credencial del dispositivo y biometría. La opción biométrica solo se ofrece cuando el dispositivo la admite y tiene una credencial de respaldo configurada.
2. **Contraseña propia:** definir política de longitud, derivación/verificación, parámetros, sal aleatoria, intentos y comportamiento de bloqueo. No inventar criptografía; utilizar primitivas y bibliotecas mantenidas adecuadas a Android.
3. **Credencial Android:** confirmar qué mecanismo concreto se invocará y cómo se conserva el vínculo local para las siguientes aperturas. La plantilla biométrica no se almacena en la base.
4. **Valores predeterminados:** decidir valores iniciales propuestos para moneda, zona horaria, folio, tasas, cargos y plazos, y cuáles requieren confirmación. Los valores comerciales deben ser editables y visibles; no deben presentarse como reglas legales universales.
5. **Opcionalidad:** acordar qué datos del taller pueden omitirse en el asistente y cuáles deberán completarse antes de emitir un comprobante o ejecutar una operación dependiente.
6. **Métodos de pago y textos de comprobantes:** `F-012` y `F-013` los mencionan, pero no todos cuentan con campos o entidades definidos en el modelo. Delimitar qué se configura en esta primera entrega; proponer cambios de modelo separados si hace falta persistirlos.
7. **Acceso biométrico cancelado/no disponible:** definir cómo volver a selección de método sin dejar al usuario atrapado ni marcar la configuración como completada.

No continuar a una implementación final de autenticación si una decisión cambia las garantías de seguridad o deja ambiguo cómo volver a abrir la aplicación.

## 5. Pasos de implementación

### Paso 1. Mapear criterios a campos y comportamientos

Crear una matriz de trazabilidad antes de codificar:

| Criterio HU-001 | Comportamiento a implementar | Persistencia o evidencia |
|---|---|---|
| Datos locales y sin cuenta | Pantalla introductoria con explicación clara | No requiere registro remoto |
| Elección de acceso local | Selector de contraseña propia o credencial Android compatible | `ConfiguracionAccesoLocal.metodo` y datos de verificación/referencia necesarios |
| Datos del taller | Formulario de configuración, en grupos comprensibles | `ConfiguracionTaller` |
| Campos opcionales y pendientes | Permitir omisión conforme a reglas acordadas e informar dependencias | Estado de configuración incompleta o reglas explícitas de validación |
| Guardado y continuidad | Confirmar y continuar a ingreso; reanudar o reiniciar con seguridad si se interrumpe | Registros singleton locales y estado de asistente no secreto |
| Seguridad de contraseña | Guardar solo verificador con parámetros y sal; mostrar advertencia de pérdida | `ConfiguracionAccesoLocal`, sin contraseña recuperable |

Verificar que cada campo visible tenga destino definido. No pedir datos que no se usan ni guardar valores de interfaz que no puedan mantenerse después.

### Paso 2. Preparar el modelo y la política de completitud

1. Revisar `ConfiguracionTaller` y `ConfiguracionAccesoLocal` frente a los campos del formulario.
2. Definir identificadores estables para sus filas singleton y una forma inequívoca de determinar si la configuración está completa.
3. Distinguir explícitamente:
   - Campo requerido para completar el asistente.
   - Campo opcional que puede dejarse vacío.
   - Campo con valor predeterminado que la persona puede revisar/cambiar.
   - Campo que puede seguir pendiente y bloquear solo una operación posterior concreta.
4. Definir validaciones de folio: prefijo permitido, secuencia inicial válida, incremento sin reutilización y prevención de colisiones con folios existentes.
5. Definir representación monetaria en unidades menores; tasas como decimal escalado o texto decimal exacto según el esquema aprobado. Evitar `Float`/`Double` para importes.
6. Definir normalización y validación de moneda ISO 4217 y zona horaria IANA. Si se propone un valor regional inicial, mostrarlo para confirmación y permitir editarlo.
7. Si se requiere persistir métodos de pago, textos de documentos u otra configuración no presente en las entidades, proponer el ajuste de esquema versionado antes de añadir controles UI sin persistencia.

### Paso 3. Diseñar el asistente y sus estados

Organizar la configuración en pantallas breves, por ejemplo:

1. **Bienvenida y privacidad:** explicar uso local, ausencia de cuenta y conectividad, almacenamiento en el dispositivo, consecuencias de pérdida/desinstalación y responsabilidad de crear respaldos en el futuro.
2. **Método de acceso:** presentar únicamente métodos compatibles. Explicar contraseña propia frente a credencial Android en lenguaje sencillo.
3. **Configuración del taller:** nombre y datos de contacto, moneda/zona horaria, folio y condiciones iniciales.
4. **Revisión:** mostrar un resumen editable, campos opcionales vacíos y parámetros con sus unidades.
5. **Confirmación:** guardar de manera controlada e iniciar el flujo de ingreso.

Para cada pantalla definir: acción primaria, acción para regresar, validaciones visibles, comportamiento al cancelar y qué ocurre al cerrar la app. No mostrar la aplicación operativa hasta confirmar la configuración y completar la autenticación inicial conforme a la decisión de seguridad.

### Paso 4. Implementar la detección de primera ejecución y navegación

1. Al abrir la aplicación, consultar de forma local el estado de `ConfiguracionTaller` y `ConfiguracionAccesoLocal`.
2. Enrutar según estados claros:
   - Ninguna configuración válida: asistente de primer uso.
   - Configuración incompleta o incompatible: flujo de completar/reparar configuración con explicación; nunca crear silenciosamente una configuración ficticia.
   - Configuración completa: flujo de ingreso (responsabilidad de `HU-002`).
3. Evitar exponer datos del taller durante la pantalla inicial antes de establecer el método de acceso.
4. Conservar el paso actual de forma segura si se interrumpe el asistente, pero no persistir contraseñas ni valores secretos como borrador.
5. Si se cambia el método durante la configuración, borrar de manera segura los datos de verificación temporales del método descartado y permitir continuar con el método elegido.
6. Manejar reinstalación, base de datos vacía y error de lectura como estados distintos; registrar el error de forma local sin incluir datos personales ni credenciales.

### Paso 5. Implementar la selección del método de acceso

#### Contraseña propia de la aplicación

1. Mostrar los requisitos de contraseña antes de pedirla.
2. Solicitar contraseña y confirmación en controles de entrada protegidos, con opción consciente de mostrar/ocultar durante su escritura si el diseño lo permite.
3. Validar política localmente y explicar errores junto al campo, sin descartar los demás datos del formulario.
4. Generar una sal aleatoria y derivar un verificador mediante la estrategia aprobada; almacenar únicamente verificador, sal, algoritmo/parámetros y fecha de actualización.
5. Limpiar referencias a la contraseña y buffers temporales tan pronto como sea razonable tras derivar el verificador. Nunca escribirla a logs, preferencias, base, archivos temporales, analítica o estado de navegación.
6. Informar que la contraseña no puede recuperarse desde un servidor. Explicar las consecuencias de olvidarla y el procedimiento local aprobado.
7. Asegurar que una falla al persistir el registro de acceso no produzca una pantalla que parezca configuración exitosa.

#### Credencial del dispositivo Android / biometría

1. Consultar disponibilidad antes de presentar las opciones y describir el mecanismo de respaldo.
2. Solicitar autenticación a través de la API segura de Android elegida, sin capturar ni almacenar datos biométricos.
3. Tratar cancelación, ausencia de hardware, falta de inscripción biométrica, credencial retirada y error de la API como resultados explícitos; ofrecer volver a selección o reintentar.
4. Registrar el método y solo la referencia necesaria para la política local; nunca almacenar plantilla, PIN del dispositivo ni secreto reutilizable.
5. Comprobar el resultado exitoso antes de permitir confirmar el asistente.

### Paso 6. Implementar el formulario de configuración del taller

Agrupar los campos para evitar una pantalla larga:

- **Identidad del taller:** nombre, teléfono y dirección. Respetar su opcionalidad definida; indicar dónde aparecerán.
- **Formato regional:** moneda y zona horaria. Mostrar etiquetas y ejemplos apropiados para no confundir importe, separador decimal y hora.
- **Folios:** prefijo y siguiente número. Indicar que los folios ya asignados no se reutilizarán.
- **Condiciones económicas:** tasa/impuesto si aplica, cargo de diagnóstico, vigencia predeterminada de cotización y demás parámetros que correspondan al esquema.
- **Garantía y seguimiento:** días predeterminados de garantía y plazos de recordatorios locales, con unidad explícita.

Validaciones mínimas:

- Recortar espacios exteriores y rechazar campos obligatorios vacíos.
- Rechazar valores numéricos inválidos, negativos donde no apliquen, desbordamientos y formatos ambiguos.
- Validar prefijo y secuencia conforme a la regla de folio aprobada.
- Permitir omitir contacto y otros datos acordados como opcionales.
- No exigir que la persona invente una tasa, cargo o plazo: permitir una opción explícita “no aplica” o “pendiente” solo cuando el modelo y las operaciones lo permitan.
- Conservar valores ingresados al corregir un campo para no forzar volver a capturar todo.
- No mostrar mensajes de validación exclusivamente mediante color; asociarlos a texto y al campo afectado.

### Paso 7. Guardar la configuración de forma atómica

1. Al revisar y confirmar, volver a validar todos los datos en la capa de dominio/repositorio; la validación no debe existir solo en la interfaz.
2. Guardar `ConfiguracionTaller` y `ConfiguracionAccesoLocal` dentro de una operación atómica. Si el almacenamiento criptográfico requiere más de un paso, definir claramente cómo detectar y limpiar un registro parcial.
3. Actualizar `creado_en` y `actualizado_en` en el formato temporal acordado; conservar zona horaria de presentación separada de almacenamiento UTC.
4. Evitar reemplazar una configuración ya válida por una segunda ejecución accidental del onboarding. Los cambios posteriores deben pasar por la configuración normal y autenticada.
5. Mostrar confirmación solo después de que todas las escrituras necesarias finalicen correctamente.
6. Ante error de escritura, informar que no se completó el guardado, preservar los datos no secretos del formulario mientras la pantalla siga activa y permitir reintentar. No marcar el flujo como completado.
7. Tras un guardado exitoso, continuar al ingreso autenticado, no directamente a las pantallas de operación sin verificar el nuevo método.

### Paso 8. Añadir protección de privacidad y operación local

1. Confirmar que ninguna ruta del asistente hace solicitudes de red ni depende de una cuenta.
2. Revisar manifiesto y dependencias para asegurar que el onboarding no introduce permiso `INTERNET`, analítica, anuncios o servicios remotos.
3. Excluir contraseñas y datos sensibles del registro, portapapeles, notificaciones, vista previa de aplicaciones recientes y persistencia de estado.
4. Mostrar con claridad qué datos se guardan y cuáles opcionales pueden aparecer en comprobantes futuros.
5. No solicitar permisos de cámara, notificaciones ni archivos en esta etapa; pedirlos solo cuando una función posterior los necesite.
6. Presentar valores monetarios y fechas respetando moneda/zona seleccionadas, sin modificar el significado almacenado.

### Paso 9. Probar el flujo de extremo a extremo

#### Pruebas unitarias/de dominio

- Configuración mínima válida y configuración completa.
- Campos opcionales vacíos frente a obligatorios incompletos.
- Límites y valores inválidos de folio, tasa, cargo, moneda y plazos.
- Conversión/validación de importes sin errores de punto flotante.
- Creación de verificador con sal diferente por contraseña y rechazo de valores inválidos.
- Regla de “completado” consistente con ambos registros singleton.

#### Pruebas de persistencia

- Guardar ambas entidades y leerlas tras cerrar/reabrir la base.
- Confirmar que un fallo en la segunda escritura no deja un estado válido parcialmente configurado.
- Inicialización repetida no duplica filas singleton ni cambia la configuración existente.
- Una configuración no puede marcarse completada si faltan los datos del método de acceso requerido.
- Los valores históricos no se reescriben al modificar posteriormente la configuración general.

#### Pruebas de interfaz y navegación

- Primera instalación entra al asistente; instalación configurada pasa a login.
- Flujo completo con contraseña propia, incluidos error y confirmación distinta.
- Flujo con credencial Android en dispositivo compatible y manejo de no disponible/cancelado.
- Regresar entre pasos no pierde datos no secretos.
- Cerrar o interrumpir la aplicación antes de confirmar no permite saltar el onboarding.
- Una falla de almacenamiento presenta error recuperable y no muestra éxito falso.
- El último paso conduce al ingreso autenticado.

#### Pruebas de seguridad y entorno

- Ejecutar en modo avión desde el primer inicio hasta el ingreso.
- Inspeccionar registros y almacenamiento en busca de contraseña en texto claro.
- Probar dispositivo sin biometría, sin credencial inscrita, autenticación cancelada y credencial cambiada conforme a las decisiones aprobadas.
- Verificar contraste, tamaño de texto, teclado apropiado, navegación táctil y mensajes accesibles.
- Probar con rotación/recreación de actividad si está soportada y con cierre del proceso.

### Paso 10. Cerrar trazabilidad y documentación

1. Registrar qué criterios de `HU-001` están cubiertos por pruebas y cuáles dependen de `HU-002`.
2. Documentar los valores iniciales elegidos, su condición de valores predeterminados y el procedimiento local ante pérdida de acceso.
3. Actualizar documentación de usuario para describir el asistente y explicar claramente el almacenamiento local.
4. Actualizar el modelo de datos solo si las decisiones resueltas requieren campos no incluidos; mantener migraciones y pruebas correspondientes.
5. Revisar que el alcance final no convierta el onboarding en creación de cuenta o configuración remota.

## 6. Orden sugerido de entrega

1. Decisiones de producto y seguridad, política de completitud y matriz de trazabilidad.
2. Persistencia singleton y validación del modelo.
3. Selección/validación del método de acceso con el flujo seguro acordado.
4. Bienvenida y asistente con navegación.
5. Formulario del taller y revisión de datos.
6. Guardado atómico, detección de primera ejecución y redirección a ingreso.
7. Pruebas automatizadas y pruebas en dispositivos Android.
8. Documentación para persona usuaria y cierre de criterios.

El orden evita construir formularios que no puedan persistir los datos o que ofrezcan un método de acceso cuya operación posterior no esté definida.

## 7. Riesgos y controles

| Riesgo | Control requerido |
|---|---|
| Contraseña propia guardada en texto claro | Verificador derivado con sal; pruebas e inspección de almacenamiento/logs |
| Pantalla anuncia éxito pese a escritura parcial | Persistencia atómica y pruebas de fallos |
| Método Android no disponible en el dispositivo | Detectar disponibilidad y permitir una ruta alternativa antes de completar |
| Datos comerciales inventados para pasar el asistente | Hacer explícitos defaults, permitir pendientes cuando sea válido y bloquear solo operaciones dependientes |
| Cambio de configuración altera operaciones pasadas | Congelar valores aplicados en órdenes/cotizaciones/comprobantes |
| Reingreso accidental vuelve a crear la configuración | Singleton, detección idempotente y separación entre onboarding y edición |
| Pérdida de la única credencial local | Advertencia comprensible y procedimiento local aprobado, sin sugerir recuperación remota inexistente |
| Requisitos de campos sin soporte en el esquema | Revisar modelo antes de codificar; proponer migración en lugar de datos solo visuales |

## 8. Definición de terminado

HU-001 está terminada cuando:

- La instalación sin configuración muestra primero la explicación local y el asistente.
- La persona puede configurar uno de los métodos de acceso admitidos, con errores y cancelaciones tratados explícitamente.
- La contraseña propia nunca queda guardada como texto claro y la decisión de acceso queda persistida de forma utilizable.
- Los datos configurables del taller se validan y quedan almacenados localmente.
- La omisión de campos opcionales no impide completar indebidamente; las dependencias pendientes se informan antes de las operaciones que las necesitan.
- Una escritura fallida no crea un estado de configuración exitoso ni envía al usuario al área operativa.
- Tras completar el asistente, se solicita el ingreso autenticado.
- El flujo se puede completar sin internet y tiene pruebas para persistencia, navegación, privacidad y errores.
- Se documentaron las decisiones de producto pendientes y se actualizaron las referencias afectadas.

## 9. Implementación realizada

La implementación actual está en [MainActivity.kt](../TallerCelulares/app/src/main/java/com/example/tallercelulares/MainActivity.kt), el paquete `ConfiguracionInicial` y las entidades de catálogos de sistema:

- El asistente presenta bienvenida y privacidad, método de acceso, formulario del taller, revisión y guardado.
- Los métodos disponibles son contraseña propia, bloqueo de pantalla Android y biometría aceptada por AndroidX `BIOMETRIC_WEAK`, que permite huella o reconocimiento facial compatible. Se declara `USE_BIOMETRIC`, el diálogo incluye una acción negativa explícita para usar otro método y se muestran errores de disponibilidad/configuración en vez de cerrar la aplicación. No se guardan credenciales del dispositivo ni plantillas biométricas.
- La contraseña propia admite de 10 a 128 caracteres. Se guarda un verificador PBKDF2-HMAC-SHA-256 con sal aleatoria de 16 bytes, 210 000 iteraciones y salida de 256 bits; la contraseña no se persiste en el estado de la interfaz.
- La configuración del taller se valida y las filas `ConfiguracionTaller` y `ConfiguracionAccesoLocal` se guardan en una transacción local. Monedas y zonas horarias se leen de `CatalogoMonedaSistema` y `CatalogoZonaHorariaSistema`, sembrados localmente con opciones comunes. Cada selector abre una lista completa en un diálogo de búsqueda independiente del valor actualmente seleccionado, por lo que la selección previa no filtra accidentalmente la lista a una sola opción. La selección inicial corresponde a la moneda de la configuración regional y a la zona horaria del dispositivo. Los importes se convierten a unidades menores y la tasa porcentual se guarda como fracción decimal.
- La base local usa una migración Room de versión 1 a 2 para crear ambas tablas de catálogos sin reemplazar la configuración existente. La carga de opciones usa inserciones idempotentes y añade cualquier moneda/zona ya configurada para que siga apareciendo al completar una configuración parcial.
- El catálogo inicial incluye monedas comunes de América, Europa, Asia y otras regiones, así como zonas horarias frecuentes de América, Europa, Asia y Oceanía. Cada lista añade además el valor detectado en el dispositivo y conserva los identificadores normalizados para persistencia.
- La base de datos y las preferencias quedan excluidas de los respaldos automáticos Android. El respaldo explícito cifrado queda fuera de este onboarding.
- Después de guardar, se muestra el flujo de ingreso local. Tras autenticar, se presenta una pantalla provisional; el tablero y las funciones posteriores no forman parte de HU-001.
- Métodos de pago y textos configurables de comprobantes no se agregaron al formulario porque el modelo actual no define campos para persistirlos. Requieren cerrar esa decisión y, si se incluyen, extender el modelo y sus pruebas.

La validación automatizada ejecutada incluye seis pruebas nuevas de validación y verificación de contraseña, además de la prueba unitaria existente. La compilación del APK de depuración y `lintDebug` finalizan correctamente usando el JDK 21 incluido con Android Studio. No se validó la interfaz en emulador/dispositivo porque `adb` no está disponible en el entorno.
