# Plan de trabajo: aplicación Android para taller local

## 1. Objetivo

Construir una aplicación Android nativa para un taller y una persona usuaria, sin API, servidor, cuenta en línea, sincronización ni conexión de red en tiempo de ejecución. Los datos permanecen en el dispositivo y se pueden respaldar/restaurar manualmente.

Este plan deriva sus requisitos de [funcionalidades-taller-local-android.md](funcionalidades-taller-local-android.md) y el modelo de datos de [entidades-base-datos-taller-local.md](entidades-base-datos-taller-local.md). Cada paso produce algo verificable. Se recomienda cerrar un paso antes de avanzar al siguiente, manteniendo pruebas y migraciones desde el inicio.

## 2. Principios de ejecución

- Implementar el flujo completo de una orden antes de ampliar funciones secundarias.
- Mantener una sola fuente local de verdad; no introducir backend ni sincronización.
- Tratar autenticación, credenciales de equipos y respaldo como requisitos centrales de seguridad, no como mejoras de última hora.
- Versionar la base de datos y respaldos desde la primera versión persistente.
- Probar la operación y restauración en dispositivos Android reales, además de emuladores.
- Trazar los requisitos con sus identificadores `F-xxx` del documento funcional.

## 3. Pasos del proyecto

### Paso 1. Cerrar decisiones de producto y seguridad

**Trabajo**
- Confirmar versión mínima de Android y dispositivos objetivo.
- Definir reglas de folio, moneda, impuestos, cargos, garantía, retención de datos y plazos configurables.
- Definir el comportamiento cuando no exista autenticación biométrica o cambie la credencial del dispositivo.
- Acordar política para contraseña propia de la app, recuperación local y bloqueo por inactividad.
- Definir consentimiento, duración y eliminación de los códigos temporales del equipo.
- Elegir y documentar cifrado de base local, secretos y archivo de respaldo; evitar criptografía propia.

**Entregables**: decisiones de producto, modelo de amenazas básico, matriz requisito-prueba y decisiones técnicas pendientes resueltas.

**Criterio de salida**: no quedan decisiones abiertas que alteren el esquema de datos o la seguridad del respaldo.

### Paso 2. Preparar proyecto y calidad automatizada

**Trabajo**
- Crear proyecto Android Kotlin con Gradle y configuración reproducible.
- Definir módulos/capas, convenciones, manejo de errores, formato y análisis estático.
- Configurar pruebas unitarias, instrumentadas y UI; integrar build y tests en CI.
- Revisar manifiesto: no declarar permiso `INTERNET`, ubicación ni permisos innecesarios.
- Añadir registro local de errores sin datos personales, credenciales ni fotografías.

**Entregables**: aplicación de arranque, pipeline de compilación y plantilla de pruebas.

**Criterio de salida**: build limpio y tests de ejemplo ejecutados en CI; el manifiesto no solicita red.

### Paso 3. Implementar almacenamiento, esquema y migraciones

**Trabajo**
- Implementar entidades indicadas en el documento de base de datos con relaciones, índices y restricciones.
- Añadir persistencia Room/SQLite, transacciones para folios y cambios de estado, y repositorios locales.
- Implementar migraciones versionadas y datos iniciales para estados, pruebas y métodos de pago.
- Definir almacenamiento privado de adjuntos y ciclo de vida de archivos.
- Crear pruebas de integridad, borrado restringido, folios únicos y cálculos monetarios.

**Entregables**: base local inicial, migraciones y pruebas del modelo.

**Criterio de salida**: puede abrirse/cerrarse la base sin pérdida y las restricciones principales se verifican automáticamente.

### Paso 4. Autenticación y protección local

**Trabajo**
- Implementar configuración inicial de acceso mediante credencial del dispositivo/biometría o contraseña local según el alcance acordado.
- Solicitar autenticación al abrir y después del periodo de inactividad; ocultar contenido sensible en la vista de aplicaciones recientes.
- Proteger datos locales y secretos usando Android Keystore y bibliotecas criptográficas mantenidas.
- Implementar `CredencialEquipo` con captura dedicada, consentimiento explícito, acceso con reautenticación y borrado al cerrar/cancelar.
- Verificar que claves o códigos no aparezcan en logs, portapapeles, temporales, notificaciones o reportes.

**Entregables**: flujo de acceso local y almacenamiento seguro probado.

**Criterio de salida**: sin autenticación no se accede a órdenes; los secretos nunca quedan en texto claro en almacenamiento o registros.

### Paso 5. Configuración, clientes y recepción

**Trabajo**
- Crear configuración del taller, folios, servicios y preferencias.
- Implementar alta/búsqueda de clientes y dispositivos con campos opcionales.
- Implementar creación de órdenes, snapshots, accesorios, condición física, ubicación y folio.
- Añadir pruebas iniciales, captura de fotografías con consentimiento y comprobante de recepción.
- Añadir tablero, búsqueda, filtros por estado y alertas de antigüedad.

**Requisitos principales**: `F-001` a `F-004`, `F-010` a `F-013`, `F-020` a `F-036`.

**Entregable**: flujo probado de recepción a orden abierta, completamente local.

**Criterio de salida**: una orden nueva conserva folio único, cliente/equipo y evidencia de ingreso tras cerrar y volver a abrir la app.

### Paso 6. Diagnóstico, cotización y autorización

**Trabajo**
- Registrar síntomas, hallazgos, factibilidad, riesgos y diagnósticos versionados.
- Crear cotizaciones con detalle, vigencia, conceptos, impuestos y sumas exactas en unidades menores.
- Registrar decisiones de autorización/rechazo y evidencia relacionada con la versión aceptada.
- Bloquear inicio de reparación sin autorización vigente y forzar nueva aprobación al cambiar alcance/precio.
- Preparar PDF/texto de cotización como acción manual de compartir, si se implementa en esta etapa.

**Requisitos principales**: `F-040` a `F-047`.

**Entregable**: ciclo de presupuesto y autorización trazable.

**Criterio de salida**: la orden no inicia reparación con presupuesto rechazado, vencido o no autorizado.

### Paso 7. Estados, reparación, piezas y control de calidad

**Trabajo**
- Implementar máquina de estados y transiciones permitidas, historial append-only y motivos de espera.
- Registrar trabajo técnico, tiempo si se requiere, hallazgos, cambios de alcance y ubicación.
- Implementar piezas, compras/recepción física, reservas, consumos, devoluciones y ajustes mediante movimientos.
- Registrar pruebas de control de calidad y compararlas con las de ingreso.
- Bloquear `Lista para entrega` si QA falla, salvo excepción documentada y aceptada.

**Requisitos principales**: `F-050` a `F-063`, `F-070` a `F-073`.

**Entregable**: reparación trazable desde autorización hasta QA.

**Criterio de salida**: todas las transiciones tienen evento; no hay consumo de pieza ni listo para entrega sin las condiciones definidas.

### Paso 8. Caja, entrega, garantías e informes básicos

**Trabajo**
- Registrar anticipos, pagos parciales/finales, reembolsos y gastos como movimientos auditables.
- Calcular saldos desde cotización aceptada y caja; comprobar límites de importes y evitar duplicar pagos.
- Generar comprobantes de pago/entrega y confirmar accesorios/persona autorizada.
- Cerrar la orden solo cuando el equipo sale del resguardo; registrar resultado técnico/comercial.
- Crear garantías y órdenes de regreso vinculadas; añadir búsquedas e informes básicos.

**Requisitos principales**: `F-064` a `F-068`, `F-074`, `F-075`, `F-080` a `F-083`.

**Entregable**: flujo completo de recepción a entrega/cierre y consulta histórica.

**Criterio de salida**: un pago y cierre pueden reconstruirse desde movimientos y eventos; garantías no reabren ni alteran órdenes originales.

### Paso 9. Respaldo, restauración y traslado de dispositivo

**Trabajo**
- Diseñar formato versionado de copia con datos, base, archivos, hashes e inventario de contenido.
- Generar copia cifrada con contraseña mediante mecanismos criptográficos probados y selección de destino Android.
- Excluir `CredencialEquipo` por defecto; permitir inclusión solo con elección explícita y advertencia.
- Validar antes de importar: contraseña, integridad, compatibilidad y espacio disponible.
- Restaurar atómicamente; si falla, mantener intactos los datos actuales. Al restaurar secretos opt-in, cifrarlos con clave del dispositivo destino.
- Añadir prueba de respaldo sin reemplazar datos, registro local de operación y aviso por copia atrasada.

**Requisitos principales**: `F-090` a `F-099`.

**Entregable**: traslado probado entre dos dispositivos Android compatibles.

**Criterio de salida**: recuperar clientes, órdenes, historial, fotos y adjuntos; una copia inválida nunca modifica la base actual.

### Paso 10. Pulido funcional y pruebas de aceptación

**Trabajo**
- Completar funciones de segunda etapa priorizadas: recordatorios, etiquetas, exportaciones CSV/PDF y alertas de existencias.
- Ejecutar recorridos end-to-end para aceptación, rechazo, no reparable, falta de refacción, fallo de QA, cancelación, garantía y entrega.
- Probar modo avión, permisos denegados, almacenamiento insuficiente, falta de espacio, interrupción durante respaldo y cambio/restablecimiento del dispositivo.
- Medir rendimiento con historial y fotos representativos; comprobar accesibilidad, contraste, teclado, escalado y orientación.
- Revisar privacidad del manifiesto, dependencias, almacenamiento automático de Android y contenido de notificaciones.

**Entregable**: informe de pruebas y lista de defectos bloqueantes resueltos.

**Criterio de salida**: se cumplen los criterios de aceptación de la sección 8 del documento funcional y no quedan defectos críticos de datos/seguridad.

### Paso 11. Preparar distribución y mantenimiento

**Trabajo**
- Configurar firma de release y resguardar la clave de firma fuera del repositorio.
- Definir distribución inicial (instalación privada o tienda), versionado y notas de versión.
- Documentar instalación, cambio de dispositivo, respaldo/restauración, recuperación de acceso y solución de problemas.
- Preparar migración de esquema en actualizaciones y estrategia de compatibilidad de copias antiguas.
- Realizar una prueba de actualización sobre una instalación con datos reales de prueba.

**Entregable**: APK/AAB release, guía de usuario y procedimiento de soporte/actualización.

**Criterio de salida**: una actualización conserva datos; la copia de seguridad y la clave de firma están resguardadas por el propietario.

## 4. Orden sugerido del MVP

El MVP no debe aplazar seguridad ni recuperación de datos. Orden recomendado:

1. Pasos 1 a 4: decisiones, base del proyecto, persistencia y acceso local.
2. Paso 5: recepción y consultas.
3. Pasos 6 y 7: autorización, reparación, inventario básico y QA.
4. Paso 8: cobros, entrega y garantía.
5. Paso 9: respaldo cifrado y restauración, antes de considerar el MVP listo para uso real.
6. Paso 10: validación completa; las mejoras de segunda etapa pueden pasar a una versión posterior.

## 5. Riesgos a vigilar

- **Pérdida/robo del teléfono:** cifrado, bloqueo local y prueba frecuente de restauración.
- **Contraseña de respaldo olvidada:** no existe recuperación remota; mostrar advertencia clara y permitir cambiarla creando una nueva copia.
- **Android Keystore invalidado:** definir cómo detectar la clave inaccesible y cómo recuperar datos desde respaldo sin sobrescribir datos actuales.
- **Errores de efectivo/inventario:** transacciones atómicas y movimientos compensatorios en lugar de editar saldos históricos.
- **Fotos que faltan en respaldo:** exportar y validar conjuntamente base y archivos con hashes.
- **Cambios de alcance:** versionar diagnósticos, cotizaciones y autorizaciones; no mutar registros aprobados.
