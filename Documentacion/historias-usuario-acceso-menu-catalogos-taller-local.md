# Historias de usuario: acceso, menú y catálogos del taller local

## Propósito y alcance

Este documento organiza las historias desde la primera apertura de la aplicación hasta la administración de los catálogos y registros maestros del taller. Se deriva de [funcionalidades-taller-local-android.md](funcionalidades-taller-local-android.md), [entidades-base-datos-taller-local.md](entidades-base-datos-taller-local.md) y [plan-trabajo-aplicacion-taller-local.md](plan-trabajo-aplicacion-taller-local.md).

La aplicación está destinada a una persona y un taller, funciona sin cuenta ni conexión a internet y mantiene los datos localmente. Las historias describen el comportamiento esperado; no implican que todas estas pantallas estén implementadas actualmente.

### Alcance de «catálogos»

Para estas historias, los registros maestros administrables son clientes, dispositivos de clientes, servicios, piezas/refacciones y parámetros del taller. Los estados de órdenes, tipos de movimiento y otros códigos controlados son valores del sistema, no catálogos editables por la persona usuaria. Los accesorios y las pruebas son registros de cada orden según el modelo actual, no catálogos maestros.

Los datos históricos se preservan: cuando un registro ya está referenciado por órdenes, cotizaciones o movimientos, se debe desactivar, archivar o anonimizar según corresponda, no eliminar su evidencia. La existencia de piezas se controla mediante movimientos de inventario, no editando directamente un saldo.

## Historias de usuario en orden de recorrido

### HU-001 — Configurar la aplicación en el primer uso

**Prioridad:** P1  
**Historia:** Como persona responsable del taller, quiero completar una configuración inicial guiada para proteger el acceso y registrar los datos básicos del negocio, para comenzar a usar la aplicación sin crear una cuenta en línea.

**Criterios de aceptación**

1. **Dado** que la aplicación no tiene una configuración inicial, **cuando** la abro por primera vez, **entonces** me informa que los datos se guardan en el dispositivo y que no se requiere una cuenta ni conexión.
2. **Dado** el asistente de configuración inicial, **cuando** elijo el método de acceso, **entonces** puedo configurar una contraseña propia de la aplicación o usar la credencial del dispositivo Android, incluida la autenticación biométrica si está disponible.
3. **Dado** que elegí el método de acceso, **cuando** registro la configuración del taller, **entonces** puedo indicar nombre, teléfono, dirección, moneda, zona horaria, prefijo y siguiente folio, así como los valores iniciales de cargos, impuestos, garantías y plazos que use el taller.
4. **Dado** que algunos datos comerciales son opcionales, **cuando** no los conozco o no los necesito todavía, **entonces** puedo continuar sin inventarlos; la aplicación identifica qué configuración queda pendiente antes de generar documentos que la requieran.
5. **Dado** que termino la configuración, **cuando** confirmo, **entonces** los valores quedan guardados localmente y la aplicación me lleva al ingreso autenticado.
6. **Dado** que configuro una contraseña propia, **cuando** la guardo, **entonces** la aplicación no la conserva en texto claro y me explica las consecuencias de perderla y las opciones locales de recuperación.

**Trazabilidad:** `F-010` a `F-016`; `ConfiguracionTaller`, `ConfiguracionAccesoLocal`.

### HU-002 — Ingresar y volver a autenticarme

**Prioridad:** P1  
**Historia:** Como persona usuaria autorizada, quiero desbloquear la aplicación con el método local que configuré, para impedir que otra persona acceda casualmente a la información del taller.

**Criterios de aceptación**

1. **Dado** que la aplicación ya fue configurada, **cuando** la abro o vuelvo a ella después del tiempo de inactividad establecido, **entonces** solicita el método local de autenticación antes de mostrar información del taller.
2. **Dado** que proporciono una credencial incorrecta o cancelo la autenticación del dispositivo, **cuando** se intenta el ingreso, **entonces** permanezco fuera de las pantallas protegidas y puedo volver a intentarlo conforme al comportamiento del método elegido.
3. **Dado** que la autenticación es correcta, **cuando** ingreso, **entonces** accedo al menú principal y a los datos locales sin depender de internet o de un servidor.
4. **Dado** que intento consultar una credencial de desbloqueo de un equipo, exportar o restaurar un respaldo, o borrar datos, **cuando** mi autenticación reciente ya no es válida, **entonces** la aplicación solicita autenticarme otra vez antes de continuar.
5. **Dado** que la aplicación queda en segundo plano, **cuando** Android muestra la vista de aplicaciones recientes, **entonces** el contenido sensible no queda expuesto en la vista previa.

**Trazabilidad:** `F-015`, `F-016`; `ConfiguracionAccesoLocal`, `CredencialEquipo`.

### HU-003 — Usar el menú principal y consultar el tablero

**Prioridad:** P1  
**Historia:** Como persona responsable del taller, quiero encontrar desde un menú principal las áreas de trabajo y un resumen de las órdenes, para ubicar rápidamente qué requiere atención.

**Criterios de aceptación**

1. **Dado** que ingresé correctamente, **cuando** se muestra la pantalla principal, **entonces** puedo acceder al tablero y a las áreas de órdenes, clientes y dispositivos, servicios, inventario de piezas, caja, garantías e informes, configuración y respaldo.
2. **Dado** que existen órdenes, **cuando** consulto el tablero, **entonces** veo el resumen por estado y una lista con folio, cliente, modelo, fecha de ingreso, estado, antigüedad y próxima acción.
3. **Dado** el tablero, **cuando** busco o filtro órdenes, **entonces** puedo hacerlo por estado y por los datos definidos para la búsqueda, incluidos folio, cliente, teléfono, equipo y serie/IMEI cuando exista.
4. **Dado** que una orden está estancada o venció su fecha prometida, **cuando** aparece en el tablero, **entonces** se destaca para seguimiento sin cambiar su estado automáticamente.
5. **Dado** que no hay órdenes registradas, **cuando** abro el tablero, **entonces** veo el estado vacío y una ruta clara para comenzar a registrar la operación.

**Trazabilidad:** `F-001` a `F-005`, `F-082` y `F-083`.

### HU-004 — Administrar el catálogo de clientes

**Prioridad:** P1  
**Historia:** Como persona responsable del taller, quiero registrar, consultar y corregir clientes, para asociar sus equipos y órdenes y conservar su historial.

**Criterios de aceptación**

1. **Dado** el catálogo de clientes, **cuando** busco por nombre o teléfono, **entonces** puedo revisar coincidencias antes de crear un registro y reducir duplicados sin impedir que dos personas compartan un teléfono.
2. **Dado** que registro un cliente, **cuando** guardo su nombre y los medios de contacto disponibles, **entonces** el cliente se conserva aunque teléfono, correo o notas sean opcionales.
3. **Dado** un cliente existente, **cuando** actualizo sus datos de contacto, **entonces** el cambio aparece en el registro actual sin modificar las instantáneas históricas de órdenes ya creadas.
4. **Dado** un cliente con órdenes asociadas, **cuando** solicito eliminarlo, **entonces** la aplicación evita destruir el historial y ofrece la alternativa disponible de baja lógica o anonimización, conservando las referencias necesarias.
5. **Dado** un cliente seleccionado, **cuando** consulto su ficha, **entonces** puedo ver sus dispositivos y el historial de órdenes relacionado.

**Trazabilidad:** `F-020`, `F-021`, `F-024`; `Cliente`.

### HU-005 — Administrar los dispositivos de los clientes

**Prioridad:** P1  
**Historia:** Como persona responsable del taller, quiero registrar y consultar los equipos de cada cliente, para reutilizar sus datos y revisar reparaciones anteriores.

**Criterios de aceptación**

1. **Dado** un cliente existente, **cuando** registro un dispositivo, **entonces** puedo indicar tipo, marca, modelo y color, y dejar número de serie o IMEI vacío si no está disponible.
2. **Dado** que un cliente tiene varios equipos, **cuando** consulto su ficha, **entonces** puedo distinguir los dispositivos asociados y abrir el historial de cada uno.
3. **Dado** un dispositivo existente, **cuando** corrijo sus datos, **entonces** las órdenes anteriores conservan la instantánea de los datos del equipo recibidos en ese momento.
4. **Dado** un dispositivo asociado a una orden histórica, **cuando** solicito eliminarlo, **entonces** la aplicación conserva las referencias y el historial, sin exigir que el equipo tenga IMEI o serie.
5. **Dado** que ingreso un IMEI o número de serie, **cuando** el dato se muestra en listados o exportaciones, **entonces** se limita su exposición a lo necesario para identificar el equipo.

**Trazabilidad:** `F-022` a `F-024`; `Dispositivo`, `Cliente`, `OrdenServicio`.

### HU-006 — Administrar el catálogo de servicios

**Prioridad:** P1  
**Historia:** Como persona responsable del taller, quiero mantener una lista de servicios con su descripción y precio base, para reutilizar conceptos al preparar cotizaciones.

**Criterios de aceptación**

1. **Dado** el catálogo de servicios, **cuando** registro o edito uno, **entonces** puedo indicar código opcional, nombre, descripción y precio base, y el sistema valida los datos e importes.
2. **Dado** un código de servicio ya utilizado, **cuando** intento guardarlo en otro servicio, **entonces** la aplicación informa el conflicto y no duplica el código.
3. **Dado** un servicio activo, **cuando** lo selecciono en una cotización, **entonces** puedo agregarlo como concepto y la cotización conserva la descripción y el precio aplicados en esa versión.
4. **Dado** un servicio que ya aparece en cotizaciones, **cuando** deja de ofrecerse, **entonces** puedo desactivarlo sin alterar cotizaciones anteriores ni eliminar su historial.
5. **Dado** el catálogo, **cuando** busco o filtro servicios, **entonces** puedo distinguir los activos de los inactivos y evitar seleccionar servicios inactivos en nuevas cotizaciones.

**Trazabilidad:** `F-012`, `F-042` y `F-043`; `CatalogoServicio`, `Cotizacion`, `DetalleCotizacion`.

### HU-007 — Administrar el catálogo de piezas y refacciones

**Prioridad:** P1  
**Historia:** Como persona responsable del taller, quiero registrar piezas y consultar su disponibilidad, para cotizar refacciones y controlar existencias con movimientos trazables.

**Criterios de aceptación**

1. **Dado** el catálogo de piezas, **cuando** registro o edito una pieza, **entonces** puedo indicar código y número de parte opcionales, nombre, variante, calidad, costo, precio de venta y existencia mínima.
2. **Dado** una pieza registrada, **cuando** consulto su ficha, **entonces** puedo distinguir los datos descriptivos y de precio de su existencia calculada y revisar los movimientos que la afectan.
3. **Dado** que recibo, consumo, reservo, libero, devuelvo o ajusto una pieza, **cuando** confirmo la operación, **entonces** se registra un movimiento con cantidad, tipo, fecha, motivo y orden relacionada cuando aplique.
4. **Dado** que una pieza no tiene suficiente existencia, **cuando** intento reservarla o consumirla, **entonces** la aplicación advierte la insuficiencia y no confirma un saldo inválido.
5. **Dado** que una pieza tiene movimientos o está vinculada a órdenes, **cuando** deja de utilizarse, **entonces** puedo desactivarla, pero no borrar sus movimientos ni afectar el historial.
6. **Dado** que una corrección afecta una operación confirmada, **cuando** la registro, **entonces** queda como un movimiento compensatorio y no como una edición silenciosa del movimiento original.

**Trazabilidad:** `F-055`, `F-070` a `F-073`; `Pieza`, `MovimientoInventario`.

### HU-008 — Administrar parámetros y opciones del taller

**Prioridad:** P1  
**Historia:** Como persona responsable del taller, quiero revisar y ajustar los parámetros comerciales y operativos, para que las nuevas cotizaciones, comprobantes y seguimientos usen las condiciones vigentes del negocio.

**Criterios de aceptación**

1. **Dado** la configuración del taller, **cuando** actualizo datos generales, moneda, zona horaria, prefijo/secuencia de folios, cargos, impuestos, vigencia de cotización, garantía o plazos de seguimiento, **entonces** la aplicación valida los valores y los aplica a operaciones nuevas según corresponda.
2. **Dado** que cambio una tasa, precio, plazo o política, **cuando** consulto una cotización, orden o comprobante históricos, **entonces** sus importes y condiciones originales no se modifican.
3. **Dado** que la persona responsable configura textos y campos opcionales para recepción, autorización y garantía, **cuando** genera los documentos asociados, **entonces** se usan los textos y opciones vigentes sin incorporar datos sensibles.
4. **Dado** que se configuran métodos de pago, **cuando** registro un movimiento de caja, **entonces** puedo elegir una opción permitida y el registro no solicita ni almacena número de tarjeta, CVV ni credenciales bancarias.
5. **Dado** que un parámetro no está configurado y es necesario para completar una operación, **cuando** intento continuar, **entonces** la aplicación identifica el dato faltante y permite resolverlo sin aplicar valores comerciales inventados.

**Trazabilidad:** `F-010` a `F-014`, `F-074`; `ConfiguracionTaller`, `MovimientoCaja`.

**Nota de modelo:** el requisito de configurar métodos de pago está en `F-012`, pero el modelo actual representa `metodo_pago` como un campo de `MovimientoCaja` y no define una entidad independiente para ese catálogo. Debe implementarse como opción configurable sin confundirla con un nuevo tipo de movimiento ni presentar valores controlados del sistema como editables.

## Límites de edición de datos

- Los estados y tipos controlados enumerados en el modelo de datos se presentan con etiquetas comprensibles, pero no se crean, renombran ni eliminan desde los catálogos del taller.
- Los accesorios, las pruebas de ingreso/control de calidad, diagnósticos, cotizaciones, autorizaciones, pagos, garantías y recordatorios corresponden a órdenes o movimientos concretos; se administran desde su flujo funcional, no como catálogos generales.
- Cambiar una configuración o un registro maestro no debe reescribir cotizaciones, comprobantes, órdenes o movimientos que ya conservaron una instantánea o evidencia histórica.
- Las operaciones de esta sección deben funcionar localmente y no deben provocar sincronización ni transferencia automática de datos.
