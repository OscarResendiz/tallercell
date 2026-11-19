# Pasos de implementación: HU-008, parámetros y opciones del taller

## 1. Objetivo

Implementar el módulo autenticado de configuración del taller para que la persona responsable pueda consultar y actualizar los parámetros comerciales y operativos que usarán las operaciones nuevas.

La implementación debe permitir:

1. Mostrar y editar los datos generales del taller, moneda, zona horaria, secuencia de folios, impuesto, cargo de diagnóstico, vigencia de cotizaciones y plazos de seguimiento y garantía.
2. Validar todos los valores antes de persistirlos y bloquear operaciones que dependan de un dato obligatorio ausente o inválido.
3. Aplicar los parámetros vigentes únicamente a cotizaciones, comprobantes, órdenes y movimientos creados después del cambio.
4. Conservar cotizaciones, órdenes, comprobantes y movimientos históricos sin reescribir sus importes o condiciones.
5. Configurar opciones para textos de recepción, autorización y garantía sin almacenar datos personales innecesarios ni secretos.
6. Configurar métodos de pago permitidos para movimientos de caja, sin confundirlos con tipos de movimiento ni solicitar datos bancarios sensibles.
7. Mantener la operación local, sin sincronización ni transferencia de datos, dentro de la sesión autenticada de HU-002.

Este documento desarrolla HU-008 de [historias-usuario-acceso-menu-catalogos-taller-local.md](historias-usuario-acceso-menu-catalogos-taller-local.md).

## 2. Referencias y estado actual

- [Historias de usuario de acceso, menú y catálogos](historias-usuario-acceso-menu-catalogos-taller-local.md): HU-008, criterios de aceptación 1 a 5 y límites de edición.
- [Funcionalidades de la aplicación](funcionalidades-taller-local-android.md): F-010 a F-014, F-074 y principios generales.
- [Entidades de la base de datos](entidades-base-datos-taller-local.md): `ConfiguracionTaller`, `MovimientoCaja`, fechas UTC e importes exactos.
- [Pasos de implementación de HU-001](pasos-implementacion-hu-001-configuracion-inicial.md): validación inicial ya existente para datos del taller.
- [Pasos de implementación de HU-003](pasos-implementacion-hu-003-menu-principal-tablero.md): patrón de navegación desde el tablero.
- [Ciclo de vida de una orden](ciclo-vida-orden-servicio.md): folios, cotizaciones, recepción, autorización, garantía y seguimiento.
- `DataBase/Entidades/ConfiguracionTaller/ConfiguracionTaller.kt`: ya contiene una fila singleton `id = "TALLER"` con datos generales y parámetros comerciales.
- `ConfiguracionInicial/ConfiguracionInicialValidator.kt`: ya contiene validaciones reutilizables para moneda, zona horaria, folio, impuesto, cargo y plazos.
- `ConfiguracionInicial/RepositorioConfiguracionInicial.kt`: guarda la configuración inicial y el acceso en una transacción.
- `DataBase/Entidades/ConfiguracionTaller/ConfiguracionTallerDao.kt`: solo ofrece consulta, inserción, actualización y borrado de la entidad completa.
- `DataBase/Entidades/MovimientoCaja/MovimientoCaja.kt`: ya tiene `metodo_pago` como texto y `referencia_no_sensible`, pero no existe un catálogo persistido de métodos permitidos.
- `CatalogosSistemaIniciales.kt`: ya prepara catálogos locales de monedas y zonas horarias.
- `TallerCelularesDatabase`: actualmente está en versión 4 y contiene la migración 3→4.
- `Modulos/tablero/TableroPrincipal.kt`: muestra “Configuración — Pendiente” y debe reemplazar ese destino por el módulo real.

El esquema actual permite ajustar los parámetros principales mediante la configuración inicial, pero no ofrece una pantalla posterior de edición ni reglas completas para guardar cambios parciales, completar datos pendientes o administrar métodos de pago y textos documentales.

## 3. Alcance

### Incluido

- Entrada “Configuración” desde el menú autenticado.
- Consulta de la fila singleton de configuración del taller.
- Edición de datos generales y condiciones comerciales disponibles.
- Selección de moneda activa y zona horaria activa.
- Cambio del prefijo y siguiente folio sin reutilizar folios asignados.
- Edición de impuesto, cargo de diagnóstico, vigencia de cotización, garantía y tres plazos de seguimiento.
- Validación central y estado de completitud por operación.
- Persistencia transaccional y actualización segura de `actualizado_en`.
- Configuración de métodos de pago activos y desactivación sin alterar movimientos históricos.
- Configuración de textos opcionales para comprobantes y documentos asociados.
- Prevención de cambios históricos accidentales y advertencias antes de cambios con efecto futuro.
- Estados de carga, vacío, error, guardado y reintento.
- Pruebas de dominio, DAO, migración, historial, navegación, privacidad y compilación.

### No incluido

- Crear sucursales, varias configuraciones por usuario o cuentas en línea.
- Sincronizar configuración entre dispositivos.
- Modificar folios, cotizaciones, comprobantes, garantías o movimientos ya existentes.
- Aplicar conversiones automáticas de moneda a precios, cargos o saldos históricos.
- Crear, editar o eliminar estados y tipos controlados del sistema.
- Configurar restricciones fiscales específicas de una jurisdicción que no hayan sido aprobadas por el taller.
- Solicitar, almacenar o mostrar número de tarjeta, CVV, clave bancaria, PIN, token de pasarela o credenciales.
- Implementar caja, garantías, impresión o respaldo completo; esta historia solo entrega los parámetros que esos módulos consumirán.

## 4. Decisiones de producto y datos

Resolver estas decisiones antes de implementar el formulario:

1. **Fila singleton:** existe una sola fila de taller con `id = "TALLER"`. No permitir insertar una segunda configuración desde la pantalla normal.
2. **Datos generales:** nombre, teléfono y dirección son opcionales cuando el modelo lo permita. Rechazar valores únicamente vacíos cuando sean necesarios para un documento concreto.
3. **Moneda:** representar siempre códigos ISO 4217 válidos y conservar el código del movimiento o documento por separado cuando sea necesario. Un cambio de moneda no convierte importes existentes ni recalcula catálogos.
4. **Zona horaria:** representar identificadores IANA válidos. Persistir instantes en UTC y convertir solamente para presentación.
5. **Impuesto:** mantener `impuesto_tasa` como texto decimal exacto. Debe representar una fracción, por ejemplo `0.16`, si así se interpreta en el cálculo actual. Validar el rango acordado en [0, 1] y no leer ni escribir `Float`.
6. **Cargos e importes:** almacenar `cargo_diagnostico_minor` como `Long` en unidades menores de la moneda vigente. Convertir desde texto usando `BigDecimal`, las fracciones de `Currency` y redondeo prohibido salvo una regla aprobada.
7. **Folios:** validar prefijo de 1 a 10 caracteres con la política existente, permitiendo letras, números y guiones. `siguiente_folio` debe ser positivo y debe asignarse y aumentar dentro de la misma transacción que crea una orden o comprobante.
8. **Secuencia y colisiones:** nunca reutilizar un folio asignado. No usar solamente una lectura previa en la interfaz; bloquear o reservar el siguiente folio transaccionalmente y controlar conflicto único.
9. **Vigencia de cotización:** validar entre 1 y 3650 días. La vigencia predeterminada se aplica al crear una cotización; una cotización existente conserva su fecha o vigencia aplicada.
10. **Garantía:** validar entre 0 y 3650 días. Cero puede significar “sin garantía predeterminada” según la política del taller y debe ser visible, no convertirse en una garantía inventada.
11. **Plazos de seguimiento:** cada plazo se valida entre 0 y 3650 días. Cero debe expresarse como “sin recordatorio automático configurado” si el módulo de recordatorios así lo interpreta.
12. **Completitud:** distinguir datos no configurados de datos inválidos. Mantener configuración inicialmente incompleta sin inventar valores comerciales.
13. **Operaciones bloqueadas:** una operación que necesite moneda, impuesto, folio, garantía o vigencia requiere que el dato correspondiente sea válido. Mostrar qué dato falta y abrir o indicar su resolución.
14. **Historial:** cada cotización, orden, comprobante y movimiento conserva moneda, importes, folio, tasa, garantía y condiciones aplicados en el momento de su creación o confirmación.
15. **Textos documentales:** si se agregan campos para recepción, autorización y garantía, deben ser texto opcional, con límites claros y sin recopilación automática de datos sensibles.
16. **Métodos de pago:** crear una entidad propia configurable, por ejemplo `OpcionMetodoPago`, con identificador estable, nombre visible, activo y fechas. `MovimientoCaja.metodoPago` conserva el identificador o nombre aplicado en el movimiento histórico.
17. **Métodos históricos:** desactivar una opción no modifica movimientos existentes. Permitir consultar el nombre histórico guardado o mostrar una referencia de método ya no disponible, sin borrar el registro.
18. **Protección:** no incluir contraseñas, credenciales, datos de tarjeta, CVV, PIN ni identificadores bancarios en configuración o métodos de pago. La referencia permitida debe ser un identificador externo no sensible, breve y definido por el taller.
19. **Desactivación:** no eliminar opciones de pago referenciadas por movimientos de caja. Mantenerlas como inactivas y conservar su nombre e identificador.
20. **Migración:** cualquier campo nuevo requiere una migración Room no destructiva desde la versión 4. No reconstruir tablas existentes ni borrar movimientos históricos.

## 5. Trazabilidad de criterios a comportamientos

| Criterio HU-008 | Comportamiento observable | Evidencia |
|---|---|---|
| 1. Actualizar parámetros | Se muestra el valor actual, se valida el formulario y solamente la configuración nueva se guarda | Pruebas de edición, persistencia y validación |
| 2. Preservar historia | Importes y condiciones de documentos creados antes del cambio permanecen iguales | Pruebas de snapshots y comparaciones antes/después |
| 3. Textos documentales | Los nuevos documentos usan textos vigentes; no se agregan datos sensibles automáticamente | Pruebas de generación y sanitización de texto |
| 4. Métodos de pago | Solo opciones activas aparecen en caja; movimientos no guardan datos bancarios sensibles | Pruebas de catálogo y registro de caja |
| 5. Parámetro faltante | La operación informa el dato exacto pendiente y permite resolverlo antes de continuar | Pruebas de completitud y bloqueo |

## 6. Pasos de implementación

### Paso 1. Confirmar modelo vigente y consumidores

1. Revisar `ConfiguracionTaller`, `ConfiguracionTallerDao`, `RepositorioConfiguracionInicial`, `ConfiguracionInicialValidator`, catálogos de moneda/zona horaria y `TallerCelularesDatabase`.
2. Buscar todos los lugares que leen `monedaCodigo`, `zonaHoraria`, `prefijoFolio`, `siguienteFolio`, `impuestoTasa`, `cargoDiagnosticoMinor`, `diasVigenciaCotizacion`, `diasGarantiaPredeterminados` y los tres plazos.
3. Buscar todos los consumidores de `MovimientoCaja.metodoPago`, folios, total de cotización, impuesto y garantía ofrecida.
4. Confirmar qué entidades actuales conservan snapshots de importe, moneda, fecha, garantía, vigencia y condiciones comerciales.
5. Identificar qué operaciones ya existen solo parcialmente y no intentar declarar HU-008 completa por cambiar datos que otros módulos todavía no usan.
6. Registrar cada parámetro con su unidad, valor vacío permitido, rango, consumidor y momento de aplicación.
7. Mantener el alcance local y no modificar manifiesto ni dependencias para agregarlos permisos o servicios remotos.

### Paso 2. Separar edición de configuración inicial

1. Reutilizar `ConfiguracionInicialValidator` para las reglas comunes, pero extraer una clase de dominio dedicada a configuración posterior si se vuelve difícil distinguir alta y edición.
2. Exponer operaciones como `validarDatosTaller`, `guardarConfiguracion`, `obtenerConfiguracion` y `obtenerPendientesPorOperacion`.
3. No reutilizar la pantalla completa de onboarding como módulo de edición salvo que la navegación y la autenticación estén bien separadas.
4. Mostrar siempre el valor persistido actual al abrir configuración.
5. Permitir guardar cambios parciales solo si los datos requeridos por la historia siguen siendo válidos o si existe una excepción explícita marcada como pendiente.
6. Mantener el texto escrito cuando falla una validación o una escritura.

### Paso 3. Completar el modelo con campos indispensables

El modelo actual ya cubre datos generales y parámetros principales. Evaluar y, si se aprueba el alcance, añadir con migración:

1. Textos opcionales para comprobante o recepción, autorización y garantía. Usar límites explícitos y no agregar HTML o ejecución de plantillas que pueda interpretar contenido arbitrario.
2. Campos opcionales adicionales solo si son necesarios para operaciones ya previstas. No agregar campos libres sin consumidor.
3. Una entidad `OpcionMetodoPago` o equivalente con `id`, `nombre`, `activo`, `creado_en` y `actualizado_en`.
4. La opción puede incluir una política de referencia no sensible, por ejemplo si acepta folio externo, pero no campos técnicos de tarjeta o cuenta bancaria.
5. Crear índices o unicidad para nombres normalizados solo si la política de duplicados lo aprueba. Mantener las opciones históricas aunque estén inactivas.
6. No usar nombres de métodos como identificadores controlados si pueden editarse; guardar un ID estable y reflejar el nombre vigente o un snapshot al mover caja.
7. Actualizar `MovimientoCaja` solamente si se decide que necesita `metodo_pago_id` además de `metodo_pago`; no eliminar el campo actual sin migración y compatibilidad.

Antes de codificar, decidir el nombre exacto de los campos y el número de versión de Room.

### Paso 4. Crear una migración Room segura

1. Incrementar la versión de Room de 4 a la siguiente versión acordada.
2. Registrar la migración correspondiente junto con las migraciones 1→2, 2→3 y 3→4.
3. Si se agregan columnas opcionales a `ConfiguracionTaller`, usar `ALTER TABLE ... ADD COLUMN` con `NULL` o valores por defecto respaldados por migración.
4. Si se agrega `OpcionMetodoPago`, crear la tabla y índices sin borrar `MovimientoCaja` ni sus datos.
5. Inicializar opciones predeterminadas solo si el taller aprueba una lista mínima. Si no se aprueba, dejar la tabla vacía y mostrar un estado que indique configuración pendiente.
6. Si se requiere migrar datos existentes de un campo opcional, hacer una transformación determinista y conservar los valores originales si no pueden interpretarse sin ambigüedad.
7. Ejecutar pruebas de migración desde cada versión disponible y verificar conteos, FKs e importes exactos.

No usar una fallback destructiva como solución habitual. Cualquier fallback solo puede estar condicionado por una política aprobada y no puede borrar movimientos históricos.

### Paso 5. Implementar reglas centralizadas de dominio

Crear `Modulos/Configuracion/` o el paquete equivalente con separación entre validación, repositorio y UI.

1. Implementar lectura única de `ConfiguracionTaller` por ID `"TALLER"`.
2. Validar nombre, teléfono y dirección con límites explícitos y normalización de espacios exteriores.
3. Normalizar moneda a mayúsculas y validar contra `Currency.getInstance`.
4. Validar zona horaria contra identificadores IANA disponibles; no aceptar una cadena arbitraria.
5. Validar prefijo con letras ASCII, números o guion según la política existente y máximo de 10 caracteres.
6. Validar siguiente folio como `Long` positivo y evitar el desbordamiento al obtener el siguiente número.
7. Convertir impuesto y cargo con `BigDecimal`, sin `Float`/`Double`.
8. La tasa de impuesto debe respetar el rango interno acordado y una representación exacta. Presentar en porcentaje solo para entrada/visualización, sin cambiar el valor persistido.
9. El cargo debe caber en `Long` en unidades menores y utilizar los dígitos decimales de la moneda.
10. Validar vigencia y garantías entre 0 y 3650 según corresponda.
11. Validar los tres plazos de seguimiento como enteros entre 0 y 3650.
12. Validar textos opcionales por longitud y caracteres visibles; rechazar exceso sin truncar.
13. Validar métodos de pago por nombre no vacío, longitud y unicidad; permitir solamente activar/desactivar opciones existentes.
14. Identificar una configuración incompleta por campo y por operación: cotizar, facturar/comprobante, registrar caja, asignar garantía o programar seguimiento.
15. Agregar pruebas unitarias para moneda inválida, zona inválida, folio fuera de rango, impuesto negativo o mayor a 100 por ciento, cargo con precisión inválida, días fuera de rango, Unicode y desbordamiento.

### Paso 6. Implementar persistencia transaccional

Completar `ConfiguracionTallerDao` con operaciones explícitas de lectura y actualización:

1. Obtener el registro por ID.
2. Insertar solamente si no existe, con una operación protegida por el ID singleton.
3. Actualizar campos concretos y `actualizado_en`, no reemplazar una entidad completa si puede sobrescribir un campo nuevo desconocido.
4. Usar una transacción Room para leer la configuración, validar, actualizar y devolver la fila persistida confirmada.
5. Rechazar un guardado si la fila cambió entre lectura y escritura, o indicar que se debe volver a cargar cuando exista una concurrencia real.
6. Inhabilitar el botón de guardar durante la operación para evitar doble envío.
7. Mostrar éxito solamente después de leer la fila guardada y comprobar los valores esperados.
8. Ante una escritura fallida, conservar los datos no secretos del formulario y ofrecer reintento.

No modificar `creado_en` al editar. Cambiar `actualizado_en` una sola vez por la operación y usar el mismo instante en todas las columnas que lo requieran.

### Paso 7. Diseñar y construir la pantalla de configuración

Organizar el formulario en secciones comprensibles:

1. Identidad del taller: nombre, teléfono y dirección.
2. Región y formato: moneda y zona horaria.
3. Folios: prefijo y siguiente folio.
4. Condiciones comerciales: impuesto y cargo de diagnóstico.
5. Vigencia y garantía: días de vigencia y garantía predeterminados.
6. Seguimiento: autorización, refacción atrasada y equipo listo.
7. Textos documentales: recepción, autorización y garantía, solamente si existen los campos aprobados.
8. Métodos de pago: lista activa/inactiva, alta y desactivación.

Para cada campo mostrar:

- Etiqueta y unidad.
- Valor actual y ejemplo de formato.
- Requerido, opcional o condicionado.
- Mensaje de error asociado al campo.
- Efecto futuro o advertencia histórica si corresponde.

No usar solamente color para identificar errores o datos pendientes.

### Paso 8. Conectar el destino desde el tablero

1. Cambiar “Configuración — Pendiente” por “Configuración — Disponible” en `TableroPrincipal.mostrarMenu()`.
2. Crear la clase o fragmento de configuración dentro de `Modulos/Configuracion/`.
3. Abrirlo por el contenedor protegido usado por los módulos disponibles; no crear una ruta pública o directa a la Activity.
4. Mantener sesión activa, bloqueo por inactividad y `FLAG_SECURE` de HU-002.
5. Al volver al tablero, refrescar valores dependientes como zona horaria, plazos y moneda.
6. Cancelar consultas o escrituras al salir y propagar `CancellationException`.
7. Dejar Caja, Garantías, Informes y Respaldo como pendientes hasta que existan sus propias historias.

### Paso 9. Integrar moneda, folios, plazos y condiciones

La integración debe consumir configuración vigente solo en operaciones nuevas:

1. **Moneda:** una cotización y un movimiento de caja creado después del cambio usan la moneda vigente y conservan `moneda_codigo`. Catálogos existentes no cambian ni se convierten automáticamente.
2. **Impuesto:** una cotización nueva aplica la tasa vigente y guarda el resultado exacto. Las cotizaciones anteriores conservan subtotal, impuesto y total calculados.
3. **Cargo:** una recepción nueva puede sugerir el cargo vigente, pero debe conservarlo en el comprobante u orden si se modifica o se omite.
4. **Vigencia:** una cotización nueva calcula su fecha o plazo a partir de los días vigentes y guarda el resultado aplicado.
5. **Garantía:** una garantía nueva usa los días vigentes al crearse y conserva cobertura, exclusiones, inicio y vencimiento propios.
6. **Seguimiento:** los recordatorios nuevos usan los plazos vigentes. Los recordatorios existentes conservan su fecha o se corrigen explícitamente por una acción separada.
7. **Folio:** un registro nuevo reserva el siguiente folio en la transacción de creación y actualiza `siguiente_folio` solo después de confirmar el registro. Las cancelaciones no reutilizan el número.

Si una operación de otro módulo todavía no consume el dato correspondiente, documentarlo como integración pendiente; no fingir que el cambio ya tuvo efecto.

### Paso 10. Implementar textos documentales y métodos de pago

#### Textos documentales

1. Mostrar campos separados para recepción, autorización y garantía.
2. Permitir texto vacío o pendiente solo si el documento puede omitirlo sin información requerida.
3. No insertar datos personales, firmas, credenciales o información contractual sensible automáticamente.
4. Al generar un documento nuevo, tomar una copia de los textos vigentes y guardarla o asociarla al documento.
5. No reemplazar textos o condiciones ya incluidos en cotizaciones, recibos o garantías históricas.
6. Mostrar vista previa mínima con los valores y claramente marcados como vigentes.

#### Métodos de pago

1. Mostrar las opciones activas al crear un movimiento de caja.
2. Permitir agregar, renombrar y desactivar opciones desde configuración cuando el flujo haya sido aprobado.
3. Evitar crear opciones de tipo fijo en código si la política permite administrarlas localmente.
4. En la caja nueva, aceptar solamente un ID/nombre activo. Rechazar un método inactivo o inexistente.
5. Guardar el método aplicado y una referencia no sensible opcional en el movimiento. Nunca guardar tarjeta, CVV o credencial.
6. Desactivar una opción referenciada en vez de eliminarla.
7. Al consultar caja, mostrar el método histórico aplicado aunque haya sido desactivado o renombrado, según se haya diseñado el snapshot.

### Paso 11. Gestionar parámetros faltantes

Para cada operación dependiente:

1. Cargar el estado de configuración.
2. Validar exactamente los datos necesarios para esa operación.
3. Si falta uno, mostrar un bloqueo concreto con el nombre del dato y una ruta a configuración.
4. Permitir resolverlo y volver a la operación sin perder los datos no sensibles ya capturados.
5. No asignar un valor predeterminado comercial inventado.
6. No continuar como si el parámetro fuera válido por tener un valor vacío o incorrecto.
7. Diferenciar configuración ausente de error de lectura de base; no mostrar ambos como cero o vacío.

Ejemplos de dependencia:

- Cotización: moneda, tasa si hay impuesto aplicable y vigencia.
- Folio de orden: prefijo y siguiente folio.
- Garantía: texto opcional y días vigentes cuando se deba ofrecer.
- Seguimiento: cada plazo vigente si se habilita el recordatorio.
- Caja: al menos una opción de pago activa y moneda válida.

### Paso 12. Manejar estados, errores y accesibilidad

Representar explícitamente:

- **Cargando:** se consulta configuración o opciones de pago.
- **Configuración ausente:** no existe fila de taller; ofrecer completar según HU-001, no crear silenciosamente.
- **Datos pendientes:** existe fila pero faltan parámetros necesarios.
- **Sin opciones de pago:** no hay métodos activos y el módulo de caja debe impedir registrar uno.
- **Validación:** mensaje junto al campo con unidad y rango.
- **Guardando:** bloquear doble envío y ofrecer retroalimentación.
- **Error de escritura:** no afirmar que el cambio fue guardado.
- **Éxito:** confirmar con la lectura persistida y volver de manera controlada.

UI:

1. Agrupar campos extensos y usar teclado/entrada apropiada.
2. Mantener contraste y tamaño de toque.
3. Vincular mensajes de error a los campos para lector de pantalla.
4. Permitir navegación por teclado o herramientas externas si aplican.
5. Evitar mostrar datos completos de teléfono o dirección innecesarios en listados.
6. Proteger el contenido con la sesión y privacidad de recientes.

## 7. Pruebas

### Pruebas unitarias/de dominio

- Nombre, teléfono y dirección vacíos u opcionales.
- Límites de longitud y normalización de espacios.
- Moneda válida e inválida.
- Zona horaria válida e inválida.
- Prefijo válido e inválido.
- Siguiente folio positivo, cero, negativo y desbordamiento.
- Impuesto 0, 100 por ciento y fuera de rango.
- Conversión exacta de cargo con moneda de dos dígitos y moneda sin decimales.
- Vigencia 1 y 3650; garantía y seguimiento 0 y 3650.
- Textos excedidos y texto vacío.
- Métodos duplicados, vacíos e inactivos.
- Bloqueo por cada dato faltante.

### Pruebas de persistencia y migración

- Crear/leer la fila singleton `"TALLER"`.
- Evitar una segunda fila singleton.
- Editar solo campos permitidos y conservar `creado_en`.
- Actualizar `actualizado_en` una sola vez por operación.
- Migración desde la versión 4 sin perder parámetros actuales.
- Migración desde las versiones 1, 2 y 3 disponibles.
- Preservar folios, cotizaciones, movimientos de caja e inventario.
- Opciones de pago históricas siguen presentes después de desactivar o renombrar.
- Reintentar una escritura fallida no duplica opciones ni modifica el valor previo.

### Pruebas de historial

- Cambiar moneda no modifica cotización, movimiento o comprobante previo.
- Cambiar impuesto no recalcula total histórico.
- Cambiar cargo no modifica recepción u orden previa.
- Cambiar vigencia no cambia una cotización ya creada.
- Cambiar garantía no modifica garantía, cobertura o vencimiento previos.
- Cambiar plazo no modifica recordatorios o fechas ya registradas.
- Cambiar método activo no modifica `metodo_pago` de movimientos anteriores.

### Pruebas de UI y navegación

- Abrir configuración desde el tablero autenticado.
- Cargar una configuración existente sin mostrar valores de otro taller o usuario.
- Cancelar edición mantiene el dato persistido sin cambios.
- Guardado exitoso actualiza la pantalla y el tablero.
- Guardado fallido muestra error recuperable.
- Datos faltantes aparecen junto a la operación que dependa de ellos.
- Métodos inactivos no aparecen para nuevos movimientos de caja.
- Navegación hacia atrás y recreación de Activity conservan texto no sensible.
- Contraste, tamaño de fuente, lector de pantalla y orientación.
- Modo avión y cierre/reapertura de la aplicación.

## 8. Definición de terminado

HU-008 queda implementada cuando:

1. “Configuración” está disponible desde el menú protegido y abre el módulo real.
2. Se pueden consultar y editar datos generales, moneda, zona horaria, folio, impuesto, cargo, vigencia, garantía y plazos.
3. Todos los valores pasan por validaciones centralizadas y se persisten exactamente.
4. Folio y demás datos dependientes se asignan o aplican de forma transaccional.
5. La configuración no modifica datos históricos ni recalcula documentos previos.
6. Los nuevos documentos usan textos vigentes y pueden asociar snapshots de sus condiciones.
7. Los métodos de pago son opciones configurables y activas; la caja no acepta opciones inexistentes o inactivas.
8. Ningún dato de tarjeta, CVV, PIN ni credencial se solicita o persiste.
9. Las operaciones que requieren un parámetro faltante lo indican y permiten resolverlo.
10. Las migraciones desde versiones anteriores conservan datos existentes.
11. Las pruebas de dominio, persistencia, migración, historial y navegación pasan.
12. La compilación, los lint existentes y las pruebas instrumentadas aplicables pasan.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Cambiar moneda modifica el significado de precios históricos | Separar moneda vigente de moneda e importes snapshots; no convertir sin proceso aprobado |
| El impuesto se redondea o cambia por punto flotante | Usar `BigDecimal`, unidades menores exactas y rangos definidos |
| Dos operaciones toman el mismo folio | Reservar y aumentar la secuencia dentro de una transacción Room |
| El siguiente folio se reutiliza después de cancelar | Marcar y no retroceder el contador una vez asignado |
| Cambiar garantía modifica garantías existentes | Aplicar garantía en la creación y conservar una entidad propia por orden |
| Cambiar plazo altera recordatorios ya creados | Mantener cada fecha calculada en el registro y corregir explícitamente |
| Un texto documental agrega datos sensibles | Validar contenido, mantenerlo como texto del taller y no recopilar datos prohibidos |
| La lista de métodos permite datos bancarios | Limitar el modelo a nombre/ID/activo y referencia no sensible |
| Desactivar un método elimina su historial | Usar baja lógica y snapshot o referencia histórica |
| Un campo nuevo no se migra | Incrementar Room y probar migración desde todas las versiones previas |
| Un parámetro inválido habilita una operación comercial | Bloquear por completitud y validar nuevamente en repositorio |
| La edición reemplaza campos no usados por la UI | Actualizar solo columnas conocidas y comprobar la fila persistida |
