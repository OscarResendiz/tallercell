# Stack y habilidades necesarias

## 1. Objetivo y restricciones

Stack recomendado para la aplicación descrita en [funcionalidades-taller-local-android.md](funcionalidades-taller-local-android.md) y [entidades-base-datos-taller-local.md](entidades-base-datos-taller-local.md). Se propone una app Android nativa, local y sin API, backend, sincronización, analítica o dependencia de conexión en tiempo de ejecución.

Las versiones concretas deben fijarse al iniciar el proyecto según la versión estable compatible de Android Studio, Kotlin y las bibliotecas elegidas; mantenerlas centralizadas en el catálogo de versiones de Gradle.

## 2. Stack recomendado

| Capa | Tecnología recomendada | Uso |
|---|---|---|
| Lenguaje | Kotlin | Lenguaje principal de Android. |
| IDE y SDK | Android Studio, Android SDK y emuladores | Desarrollo, profiling y pruebas de dispositivos. |
| Build | Gradle con Kotlin DSL, Version Catalog y JDK requerido por Android Gradle Plugin | Builds reproducibles, dependencias y variantes debug/release. |
| UI | Jetpack Compose + Material 3 | Pantallas táctiles, formularios, listas, estados y accesibilidad. |
| Navegación | Navigation Compose | Navegación entre tablero, clientes, órdenes, caja, inventario y ajustes. |
| Arquitectura de UI | ViewModel, StateFlow y repositorios; arquitectura por funcionalidades con dominio/persistencia separados | Estado predecible y pruebas de reglas de negocio; evitar capas sin utilidad. |
| Asincronía | Kotlin Coroutines y Flow | Acceso no bloqueante a base, archivos y observación local. |
| Persistencia | Room sobre SQLite | Entidades, claves foráneas, consultas, transacciones, índices y migraciones. |
| Cifrado de base | SQLCipher for Android integrado con Room, si se decide cifrar toda la base | Protección adicional de clientes, órdenes y pagos en reposo; validar compatibilidad y recuperar datos mediante copia controlada. |
| Claves del dispositivo | Android Keystore | Proteger claves criptográficas locales; nunca exportar el material de clave. |
| Autenticación del dispositivo | AndroidX Biometric / `BiometricPrompt` | Biometría y credencial del dispositivo cuando sea compatible; fallback definido a PIN/contraseña. |
| Contraseña propia de app | Verificador de contraseña con KDF resistente a fuerza bruta (biblioteca mantenida; salt aleatoria por instalación) | Validación local sin conservar la contraseña reversible. Documentar recuperación local. |
| Secretos de equipos | Cifrado autenticado con clave protegida por Android Keystore | Cifrar patrón/PIN/contraseña por orden; descifrar solo tras reautenticación reciente y limpiar al cerrar. |
| Respaldo/restauración | Storage Access Framework (SAF), formato versionado, cifrado autenticado y hashes de contenido | Guardar/abrir archivo elegido por la persona, sin sincronización automática. Restauración atómica. |
| Archivos e imágenes | Almacenamiento privado de app; CameraX si hace falta control de cámara, o captura mediante intent del sistema | Adjuntar fotos a orden, minimizando permisos y gestionando el consentimiento. |
| Notificaciones locales | WorkManager + notificaciones Android | Recordatorios de seguimiento sin enviar mensajes ni requerir servicios remotos. |
| Compartir/impresión | Android Sharesheet, `FileProvider`, `PdfDocument` | Sacar comprobantes por acción explícita del usuario. No incluir secretos en exportaciones. |
| Informes | Consultas Room; CSV propio sencillo y PDF Android | Informes locales de caja, órdenes e inventario. |
| Pruebas | JUnit, pruebas instrumentadas Android, Room in-memory, Compose UI tests y pruebas de migración | Unitarias, persistencia, pantallas y recorridos críticos en dispositivo/emulador. |
| Calidad | Android Lint, Kotlin static analysis (por ejemplo Detekt si conviene), CI de build/test | Errores, estilo, dependencias y regresiones. |
| CI | GitHub Actions u otro CI que compile y ejecute tests; no es servicio usado por la app | Validación de commits/releases. El CI puede usar internet durante build; la aplicación publicada no debe tener permiso de red. |

## 3. Arquitectura de aplicación

Usar arquitectura local-first de una sola app:

```text
Jetpack Compose
      ↓ eventos / estado
ViewModels (StateFlow)
      ↓
Casos de uso y reglas de negocio
      ↓
Repositorios locales
      ↓
Room/SQLite + archivos privados + Android Keystore
```

- Las pantallas no deben ejecutar SQL ni contener reglas de transición de órdenes.
- Las reglas críticas viven en dominio/repositorios y se prueban sin UI: transiciones, totales, autorización, cierre, stock y restauración.
- Room y archivos forman una sola operación lógica donde sea posible. Para fallos entre DB/archivos, aplicar estrategia transaccional y limpieza recuperable.
- No crear módulo de servidor, cliente HTTP, login remoto ni cola de sincronización.
- Revisar manifiestos fusionados en cada release para asegurar que ninguna dependencia agregue `INTERNET` u otros permisos no justificados.

## 4. Decisiones técnicas de seguridad

1. **Autenticación:** usar `BiometricPrompt` para biometría/credencial Android; una contraseña propia requiere verificador resistente a fuerza bruta, protección contra intentos repetidos y flujo local de recuperación.
2. **Separación de credenciales:** nunca reutilizar el PIN/contraseña del equipo reparado como autenticación de la app. Guardar el dato del cliente en una entidad cifrada y separada de notas/eventos.
3. **Cifrado:** no implementar algoritmos propios. Elegir bibliotecas con mantenimiento activo y documentación Android; definir cifrado autenticado, gestión/rotación de claves, invalidación de Keystore y borrado criptográfico.
4. **Copia de seguridad:** usar cifrado basado en contraseña con KDF adecuado, salt y parámetros/versiones almacenados en el formato. No guardar la contraseña en el archivo. Incluir secretos del equipo solo por opt-in; volver a cifrarlos con claves nuevas al restaurar.
5. **Datos en reposo:** excluir datos sensibles de Auto Backup del sistema cuando evite el formato cifrado propio. Documentar los límites de protección de Android y la pérdida del dispositivo.
6. **Privacidad:** no loggear datos del cliente, IMEI, fotos ni secretos. Ocultar pantallas sensibles en recientes; limpiar temporales y portapapeles; notificaciones discretas.
7. **Permisos:** pedir cámara/archivos/notificaciones solo cuando se usan. Preferir SAF/Photo Picker para evitar acceso amplio a archivos.

La selección final de bibliotecas criptográficas debe verificarse con un prototipo temprano de cifrado, exportación/importación y migración; no fijar una dependencia solo por conveniencia de API.

## 5. Skills requeridos

### Imprescindibles

- **Desarrollo Android nativo:** Kotlin, Android SDK, ciclo de vida de Activity, permisos, intents, almacenamiento y distribución.
- **Jetpack Compose:** layouts adaptables a teléfono, navegación, estado, formularios, accesibilidad y pruebas de UI.
- **Persistencia local:** SQLite/Room, relaciones, índices, transacciones, consultas, migraciones y estrategia de consistencia entre DB y archivos.
- **Diseño de dominio:** modelar estados/transiciones de orden, cotizaciones versionadas, autorización, QA, caja e inventario con historial auditable.
- **Seguridad móvil:** Android Keystore, BiometricPrompt, almacenamiento privado, protección de secretos, manejo de fallos de autenticación y threat modeling básico.
- **Criptografía aplicada:** KDF de contraseñas, cifrado autenticado, nonces/salts, gestión de claves, versionado y recuperación; usar implementaciones confiables, no primitivas caseras.
- **Respaldo/restauración:** SAF, formatos versionados, hashes, copia/importación atómica, migración entre dispositivos y pruebas de corrupción/interrupción.
- **Testing Android:** JUnit, instrumentación, Room tests, Compose UI tests, fixtures y pruebas end-to-end de flujos y seguridad.
- **Diseño de producto móvil:** UX táctil, estados vacíos/error/carga, validación accesible, contraste y uso en taller con manos ocupadas.

### Necesarias según funcionalidades elegidas

- **CameraX y tratamiento de imágenes:** si se necesita controlar captura, compresión, rotación y tamaño de fotos.
- **WorkManager y notificaciones:** recordatorios persistentes locales respetando ahorro de batería y permisos por versión Android.
- **PDF/impresión/compartición:** `PdfDocument`, Sharesheet, `FileProvider` y SAF para comprobantes y cotizaciones.
- **CI/CD Android:** Gradle, firma de release, secretos de firma, build variants y automatización de pruebas.
- **Accesibilidad y QA de dispositivos:** TalkBack, escalado de fuente, diferentes densidades, versiones Android y fabricantes.

### No requeridas para este alcance

- Desarrollo de backend, APIs REST/GraphQL, autenticación OAuth remota, bases de datos cloud o sincronización.
- Kubernetes, microservicios, hosting, observabilidad cloud o mensajería de servidor.
- Roles multiusuario, aislamiento multiempresa o gestión de sucursales.

## 6. Alternativa para equipo pequeño

Para una primera versión puede trabajar una persona desarrolladora Android con experiencia en Kotlin, Compose, Room y seguridad móvil, apoyada por revisión puntual de criptografía y pruebas en dispositivos. Si el equipo no tiene experiencia con cifrado o backups, hacer primero un prototipo técnico aislado y revisión por alguien con experiencia en seguridad Android antes de guardar datos reales.

## 7. Decisiones que deben quedar registradas

- `minSdk`, `targetSdk` y versiones de librerías compatibles.
- Si se cifrará toda la base con SQLCipher o solo campos/archivos especialmente sensibles.
- Algoritmos y bibliotecas finales para verificador local, secretos del equipo y archivo de respaldo.
- Política de bloqueo, reintentos y recuperación de la contraseña de la app.
- Compatibilidad del formato de respaldo entre versiones y tratamiento de claves Keystore invalidadas.
- Política de retención de órdenes, fotos, credenciales temporales, logs y exportaciones.
- Matriz de dispositivos Android reales para pruebas y canal de distribución.
