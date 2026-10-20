# Pasos de implementación: HU-002, ingreso y reautenticación

## 1. Objetivo

Implementar el acceso recurrente a la aplicación y el bloqueo de sus pantallas cuando corresponda, de manera que:

1. El método de autenticación configurado en HU-001 se solicite antes de mostrar información del taller.
2. Un intento incorrecto o cancelado no dé acceso y permita volver a intentar sin perder el método configurado.
3. Una autenticación correcta permita entrar al área principal de la aplicación, sin requerir cuenta, servidor ni conexión.
4. Las operaciones sensibles soliciten una autenticación reciente antes de ejecutarse.
5. El contenido sensible no aparezca en la vista previa de aplicaciones recientes.

Este documento desarrolla la historia `HU-002` de [historias-usuario-acceso-menu-catalogos-taller-local.md](historias-usuario-acceso-menu-catalogos-taller-local.md). El menú y el tablero completos corresponden a HU-003; su integración aquí debe limitarse a controlar el ingreso y las rutas protegidas.

## 2. Referencias y trazabilidad

- [Historias de usuario de acceso, menú y catálogos](historias-usuario-acceso-menu-catalogos-taller-local.md): HU-002, criterios de aceptación 1 a 5.
- [Funcionalidades de la aplicación](funcionalidades-taller-local-android.md): F-015 y F-016; también F-025, F-026 y F-090 a F-096 para las acciones que requieren autenticación reciente.
- [Entidades de la base de datos](entidades-base-datos-taller-local.md): `ConfiguracionAccesoLocal`, `CredencialEquipo` y `RegistroRespaldo`.
- [Pasos de HU-001](pasos-implementacion-hu-001-configuracion-inicial.md): selección del método, protección de contraseña y persistencia local.
- [AccesoActivity.kt](../TallerCelulares/app/src/main/java/com/example/tallercelulares/Modulos/acceso/AccesoActivity.kt): punto de entrada local, pantalla de ingreso y bloqueo.
- [AutorizacionReciente.kt](../TallerCelulares/app/src/main/java/com/example/tallercelulares/Modulos/acceso/AutorizacionReciente.kt): contrato para pedir y consumir autenticación reciente en acciones protegidas.
- [SesionLocal.kt](../TallerCelulares/app/src/main/java/com/example/tallercelulares/Modulos/acceso/SesionLocal.kt): estado de sesión efímero y cálculo del vencimiento por inactividad.
- [OnboardingActivity.kt](../TallerCelulares/app/src/main/java/com/example/tallercelulares/Modulos/onbording/OnboardingActivity.kt): alta inicial del método local.
- [RepositorioConfiguracionInicial.kt](../TallerCelulares/app/src/main/java/com/example/tallercelulares/ConfiguracionInicial/RepositorioConfiguracionInicial.kt): lectura del estado y verificación de la contraseña configurada.
- [VerificadorContrasena.kt](../TallerCelulares/app/src/main/java/com/example/tallercelulares/ConfiguracionInicial/VerificadorContrasena.kt): derivación y verificación de contraseña propia.
- [BaseTallerActivity.kt](../TallerCelulares/app/src/main/java/com/example/tallercelulares/Modulos/onbording/BaseTallerActivity.kt): ventana segura y componentes compartidos de interfaz.

## 3. Alcance

### Incluido

- Enrutamiento entre configuración inicial, ingreso y área principal según el estado persistido.
- Ingreso con el método de `ConfiguracionAccesoLocal`: contraseña propia, credencial Android o biometría disponible.
- Rechazo seguro de intentos fallidos, cancelaciones y errores de autenticación.
- Bloqueo al iniciar una sesión de proceso nueva y al volver a primer plano después del umbral de inactividad configurado.
- Estado de autenticación reciente para autorizar acciones sensibles.
- Reautenticación antes de revelar una credencial de equipo, exportar o restaurar un respaldo, o borrar datos.
- Ocultamiento del contenido de la aplicación en la vista de aplicaciones recientes.
- Pruebas de dominio, navegación, ciclo de vida, privacidad y autorización de operaciones sensibles.

### No incluido

- Alta inicial o cambio del método durante HU-001, salvo la integración de ingreso con el método ya guardado.
- Implementación del menú/tablero, que corresponde a HU-003.
- Implementación completa de credenciales de equipo, respaldos o borrado de datos; aquí solo se define el punto obligatorio de control de autorización que esas funciones deberán invocar.
- Recuperación en línea, restablecimiento remoto o creación de cuentas.
- Biometría propia de la aplicación o captura de PIN/patrón del dispositivo. La autenticación del dispositivo debe delegarse a Android.
- Reglas de bloqueo del equipo del cliente o intentos de autenticación de Android, que son administrados por el sistema operativo.

## 4. Decisiones previas a la implementación

Registrar las decisiones de producto y seguridad antes de cerrar el comportamiento:

1. **Umbral de inactividad:** se conserva el valor de 300 segundos ya guardado por HU-001 como umbral actual. El modelo permite cambiarlo mediante `tiempo_bloqueo_segundos`; la interfaz para que la persona lo configure queda para la historia de configuración del taller.
2. **Métodos Android:** confirmar que `CREDENCIAL_ANDROID` y `BIOMETRIA_ANDROID` se interpreten igual en la configuración y en el ingreso. La biometría solo se ofrece cuando Android la admite y su resultado debe permanecer bajo control de la API del sistema.
3. **Cancelación biométrica:** definir el texto y las acciones disponibles cuando se cancela: reintentar con el mismo método o regresar a la pantalla de ingreso. No cambiar silenciosamente el método guardado.
4. **Política ante contraseña incorrecta:** acordar si habrá espera progresiva o límite local de intentos. No bloquear por intentos fallidos de Android/biometría desde la aplicación, pues esas políticas son responsabilidad del sistema. Si se añade espera para contraseña, definir persistencia, reinicio y mensajes sin revelar si el verificador existe.
5. **Autenticación reciente:** cada operación sensible solicita una autenticación nueva y recibe una autorización limitada a esa acción, válida por 30 segundos y consumible una sola vez. Este plazo es independiente del umbral de inactividad de la sesión.
6. **Reautenticación biométrica o de credencial:** decidir si se vuelve a pedir el método configurado o si se permite un método alterno previamente aprobado. No degradar a una opción menos segura sin consentimiento.
7. **Comportamiento de navegación al bloquear:** definir si se conserva la pantalla que estaba abierta y se muestra después de autenticar, o si se regresa al menú. Nunca conservar en primer plano un formulario secreto sin ocultarlo ni revalidarlo al reanudar.
8. **Acciones sensibles de futuras historias:** hacer obligatorio el contrato de autorización para acceso a secretos, respaldo y borrado, de forma que los módulos posteriores no puedan omitirlo accidentalmente.

## 5. Pasos de implementación

### Paso 1. Convertir criterios en reglas verificables

Antes de modificar código, crear una matriz de trazabilidad:

| Criterio HU-002 | Regla observable | Evidencia requerida |
|---|---|---|
| 1. Ingreso y retorno por inactividad | No se muestra contenido del taller antes de autenticar; al regresar tras el umbral, se bloquea antes de reanudarlo | Pruebas de estado de configuración, inicio, ciclo de vida y temporizador |
| 2. Intento incorrecto o cancelado | No se concede acceso ni se ejecuta la acción pendiente; se conserva la posibilidad de reintentar | Pruebas de contraseña incorrecta, cancelación Android y error de disponibilidad |
| 3. Entrada local | El éxito abre el destino principal sin llamadas de red | Prueba de navegación y verificación de que la ruta funciona sin internet |
| 4. Operación sensible | Sin autorización reciente no se revela, exporta, restaura ni elimina información | Pruebas de autorización antes y después del vencimiento |
| 5. Vista de recientes | El contenido sensible se cubre antes de que Android capture la vista previa | Prueba manual/instrumentada en ciclo `onStop` y revisión de la ventana segura |

Cada criterio debe quedar asociado a una prueba automatizada o, si depende del comportamiento visual del sistema, a un caso de validación manual reproducible.

### Paso 2. Definir estados de acceso y navegación

Modelar el enrutamiento como estados explícitos, evitando inferir el acceso de una vista que quedó montada:

1. **Configuración inexistente o incompleta:** abrir el onboarding conforme a HU-001; no mostrar datos del taller.
2. **Configuración completa y sesión no autenticada:** mostrar la pantalla de ingreso.
3. **Autenticación en curso:** bloquear controles duplicados y no presentar contenido protegido.
4. **Sesión autenticada:** permitir abrir el menú principal de HU-003.
5. **Aplicación en pausa con sesión aún vigente:** recordar el instante de última actividad y evaluar el umbral al volver.
6. **Sesión vencida, proceso recreado o método cambiado:** invalidar la autorización en memoria y volver al ingreso.
7. **Autorización sensible en curso:** ejecutar únicamente la acción asociada si la autenticación termina correctamente; ante cancelación o error, descartarla.

Reglas adicionales:

- La navegación de retroceso desde el login no debe permitir llegar al menú, onboarding ya completado ni una pantalla protegida del back stack.
- Un intent externo o enlace profundo que apunte a una pantalla protegida debe pasar por el mismo guard de sesión que la navegación interna.
- Una escritura incompleta o incompatible de `ConfiguracionTaller`/`ConfiguracionAccesoLocal` debe seguir el flujo de recuperación de HU-001, no conceder acceso.
- Después de completar HU-002, reemplazar la pantalla provisional autenticada actual por el punto de entrada acordado con HU-003. No simular un menú operativo si aún no existe.

### Paso 3. Revisar el método configurado y el almacenamiento

1. Consultar `ConfiguracionAccesoLocal` desde el repositorio local y validar que el método y sus datos asociados sean coherentes.
2. Para `PASSWORD_APP`, requerir verificador, sal y parámetros reconocidos; usar la verificación existente de `VerificadorContrasena`, nunca comparar una contraseña en texto claro almacenada.
3. Para `CREDENCIAL_ANDROID` y `BIOMETRIA_ANDROID`, verificar que no existan campos de contraseña asociados indebidamente y delegar la comprobación a Android.
4. Si el método es desconocido, faltan parámetros o el registro está corrupto, no mostrar datos ni asumir que la persona está autenticada. Informar el problema y volver a un flujo seguro de reparación local.
5. Confirmar que las consultas se ejecutan con el repositorio existente y el dispatcher apropiado para Room; evitar lógica de seguridad duplicada en las Activities.
6. No incluir contraseñas, sal, verificador, contenido de `CredencialEquipo` ni datos personales en logs de diagnóstico.

### Paso 4. Implementar el ingreso con contraseña propia

1. Mostrar el control de contraseña sin exponer el texto por defecto; evitar autocompletado, copiado accidental y conservación en estado de instancia.
2. Limpiar el campo visual tras cada intento y convertir el valor temporal a un buffer adecuado para la función de verificación.
3. Ejecutar la derivación/verificación fuera del hilo principal.
4. Limpiar el buffer temporal tanto en éxito como en error y cancelación de coroutine.
5. Si la contraseña no coincide, dejar a la persona en el login, mostrar un mensaje neutro y claro, y permitir reintentar según la política definida.
6. No distinguir mediante mensajes entre “no existe usuario”, “verificador ausente” y “contraseña incorrecta” si esa diferencia filtra estado interno.
7. Evitar que una excepción de base de datos o criptografía sea tratada como contraseña incorrecta; reportar que el acceso no pudo verificarse y permitir reintentar o salir de manera segura.
8. Deshabilitar el botón mientras se verifica para prevenir ejecuciones concurrentes y resultados de autenticación duplicados.

### Paso 5. Implementar el ingreso Android y biométrico

1. Para `CREDENCIAL_ANDROID`, comprobar que el dispositivo tenga bloqueo seguro y abrir el flujo de confirmación del sistema.
2. Para `BIOMETRIA_ANDROID`, consultar disponibilidad en cada ingreso; no suponer que el hardware, la inscripción o el permiso se mantienen desde la configuración inicial.
3. Usar los autenticadores admitidos por la política de HU-001 y por el sistema; incluir rostro únicamente cuando Android lo exponga como autenticación compatible. No implementar reconocimiento facial propio.
4. Tratar como resultados diferentes el éxito, cancelación, falta de inscripción, hardware no disponible, credencial retirada y error al iniciar la API.
5. En cancelación o error, permanecer fuera de las pantallas protegidas y dejar una acción visible para reintentar o volver al login.
6. No guardar PIN, patrón, contraseña del dispositivo, plantilla biométrica ni tokens reutilizables. Persistir únicamente el método que ya existe en `ConfiguracionAccesoLocal`.
7. Asegurar que callbacks de actividad/biometría no concedan acceso a una pantalla destruida o a una acción sensible que ya fue cancelada.

### Paso 6. Conceder y caducar la sesión local

1. Representar la sesión autenticada con estado efímero de aplicación, no con un booleano persistido en preferencias o base de datos.
2. Registrar el instante de autenticación y la última actividad con un reloj monotónico (`elapsedRealtime` o equivalente), para que cambiar la hora del dispositivo no extienda la sesión.
3. Leer `tiempo_bloqueo_segundos` de `ConfiguracionAccesoLocal` y aplicar el umbral acordado. Tratar de forma segura valores nulos, negativos o fuera del rango permitido; no convertirlos en una sesión ilimitada.
4. En el arranque de un proceso nuevo, iniciar siempre no autenticado. La actividad recreada por rotación tampoco debe otorgar acceso solo por haber restaurado estado visual.
5. Al ir a segundo plano, guardar la marca monotónica y ocultar el contenido sensible inmediatamente. Al volver, comparar el tiempo transcurrido antes de dibujar/restaurar contenido protegido.
6. Si el periodo transcurrido alcanza el umbral, limpiar el estado de sesión y mostrar el login. Si no lo alcanza, reanudar la navegación de acuerdo con la decisión de producto.
7. Reiniciar el temporizador de inactividad solo con eventos de interacción válidos. No usar actualizaciones automáticas, animaciones o lecturas en segundo plano como actividad humana.
8. Tras cambiar el método de acceso, cerrar sesión e invalidar cualquier autorización sensible existente.
9. Evitar que procesos hijos, vistas previas, tareas de exportación o callbacks asíncronos se ejecuten como si la sesión siguiera vigente después de caducar.

### Paso 7. Centralizar la autenticación reciente para operaciones sensibles

Implementar un guard/servicio común de autorización reciente, independiente de las pantallas, y exigir su uso desde cada caso de uso sensible:

1. **Consultar una credencial de equipo:** pedir autenticación reciente antes de descifrarla; mostrar el secreto solo después de éxito, en memoria y con una acción explícita. Ocultarlo al salir, bloquear o perder foco.
2. **Exportar respaldo:** autenticar antes de preparar o escribir el archivo. Excluir las credenciales temporales por defecto, conforme a F-090.
3. **Restaurar respaldo:** autenticar antes de validar/aplicar una operación que pueda reemplazar datos; mostrar contenido y consecuencias y pedir confirmación adicional antes de confirmar la restauración.
4. **Borrar datos:** autenticar antes de abrir la confirmación final y volver a validar la sesión justo antes de ejecutar el borrado.
5. No consumir una autorización si el usuario cancela antes de la acción; no reutilizarla para otro propósito. La acción debe vincularse a una solicitud concreta.
6. Definir una expiración corta y clara para la autenticación reciente. Si vence antes de la confirmación final, volver a solicitarla.
7. Tratar cancelación, error o pérdida de foco como denegación: no descifrar, exportar, restaurar ni borrar nada.
8. Mantener las protecciones en el dominio/caso de uso además de la interfaz, de modo que un botón oculto o una pantalla alternativa no permita saltarlas.

### Paso 8. Ocultar contenido sensible al cambiar de aplicación

1. Mantener `FLAG_SECURE` en las ventanas que muestran datos del taller o secretos; centralizar su configuración para evitar diferencias entre Activities.
2. Como defensa adicional, cubrir o sustituir el contenido sensible cuando la Activity pasa a `onStop`/segundo plano, antes de que el sistema capture la vista previa de recientes.
3. Al volver a primer plano, evaluar primero la sesión y el umbral; no quitar la cubierta hasta confirmar que la sesión puede continuar o mostrar el login.
4. No colocar secretos en notificaciones, portapapeles, `SavedStateHandle`, argumentos de navegación, logs ni contenido accesible que no corresponda.
5. Revisar todas las ventanas, diálogos y Activities de operaciones sensibles; el diálogo de autenticación no debe exponer la pantalla subyacente en capturas ni transiciones.
6. Validar tanto el gesto de inicio como el botón de recientes y la rotación/recreación de pantalla.

### Paso 9. Integrar el destino principal sin acoplar HU-003

1. Definir un punto de navegación único al menú/tablero que acepte únicamente sesión autenticada.
2. Mientras HU-003 no esté implementada, usar una pantalla claramente provisional, manteniendo el bloqueo y sin afirmar que las áreas operativas ya existen.
3. Quitar la pantalla de ingreso del back stack después de autenticar; el botón atrás desde el menú no debe revelar el login con estado confuso ni permitir retroceder al onboarding.
4. Al bloquear por inactividad, garantizar que el retorno al login no se pueda evadir mediante navegación atrás.
5. Mantener fuera de HU-002 las funciones del menú; probar la ruta con un destino sustituible para integrar el tablero sin reescribir la lógica de autenticación.

### Paso 10. Añadir o ajustar persistencia y migraciones solo si hacen falta

1. Reutilizar `ConfiguracionAccesoLocal.tiempo_bloqueo_segundos` para el umbral configurable si su semántica cubre la decisión acordada.
2. Si hace falta distinguir el tiempo de sesión del tiempo de autorización reciente, documentar la diferencia y añadir el campo solo después de confirmar el requisito.
3. Si se agregan datos persistidos para intentos fallidos, guardar exclusivamente contadores y tiempos necesarios; no guardar contraseñas ni muestras de autenticación.
4. Actualizar entidades, DAO, versión de Room y migración no destructiva si se modifica el esquema.
5. Probar migración desde cada versión soportada y preservar los datos existentes de taller y acceso.
6. No registrar cada autenticación exitosa en una tabla de auditoría salvo que exista un requisito explícito; minimizar datos sensibles almacenados.

### Paso 11. Gestionar fallos y recuperación

1. Diferenciar fallo de credencial, cancelación voluntaria, método temporalmente no disponible y error de almacenamiento/API.
2. Mostrar mensajes accionables que no revelen valores internos ni confirmen información innecesaria.
3. Ante una base local ilegible o un método imposible de verificar, mantener bloqueado el contenido y explicar opciones locales reales. No ofrecer un “modo omitir” ni reconstruir un acceso débil.
4. Para contraseña olvidada, seguir las limitaciones documentadas en HU-001; no afirmar que existe recuperación si no hay mecanismo implementado.
5. Asegurar que los errores y reintentos no permitan repetir sin límite una operación sensible ni disparar múltiples restauraciones/borrados.
6. Registrar fallos técnicos localmente solo con información diagnóstica mínima, sin credenciales, secretos, nombres, teléfonos ni datos de órdenes.

### Paso 12. Probar los criterios y escenarios límite

#### Pruebas de autenticación y dominio

- Método `PASSWORD_APP` válido concede sesión; contraseña errónea no la concede.
- Método con verificador/sal/parámetros ausentes o incompatibles permanece bloqueado y reporta un error controlado.
- Métodos Android delegan al sistema; el éxito concede acceso y cancelación/error lo deniega.
- Biometría no disponible no cierra la aplicación ni cambia automáticamente el método guardado.
- La autorización sensible corresponde a una única acción y expira según el tiempo acordado.

#### Pruebas de ciclo de vida e inactividad

- Instalación sin configuración va a HU-001; configuración completa va al login.
- Inicio de proceso con configuración completa no abre el menú sin autenticación.
- Volver antes del umbral conserva únicamente el comportamiento aprobado; volver al alcanzar o exceder el umbral requiere ingreso.
- Rotación, recreación de Activity y muerte del proceso no restauran una sesión que no deba persistir.
- Cambiar el reloj de pared no extiende el periodo de sesión.
- Actividad en segundo plano que se mantiene viva y vuelve después del umbral queda bloqueada antes de mostrar contenido.
- Ninguna navegación atrás o enlace profundo evita el guard de acceso.

#### Pruebas de operaciones sensibles

- Sin sesión/autorización reciente se deniega la lectura de `CredencialEquipo`.
- Tras autenticar, se puede revelar solo por acción explícita y el secreto se oculta al bloquear o abandonar la vista.
- Exportar, restaurar y borrar exigen autenticación reciente antes de su ejecución.
- Cancelar autenticación o confirmación final no produce cambios, exportaciones ni revelación de secretos.
- Una autorización expirada entre el diálogo y la acción final exige autenticarse nuevamente.

#### Pruebas de privacidad y sistema

- No se muestra contenido sensible en la vista de aplicaciones recientes al enviar la aplicación al fondo.
- Las pantallas sensibles y diálogos conservan la protección de capturas acordada.
- La autenticación y la entrada al área principal funcionan sin red y no hacen solicitudes remotas.
- Contraseñas y secretos no aparecen en logs, bundles, preferencias ni archivos de respaldo por defecto.

#### Pruebas de interfaz

- El método de acceso indicado coincide con el seleccionado en HU-001.
- Los errores de contraseña, cancelación biométrica y bloqueo vencido tienen mensajes entendibles y acciones accesibles.
- Los controles de ingreso no se pueden activar repetidamente durante una operación en curso.
- Validar tamaño de texto, lector de pantalla, foco, contraste y navegación con teclado/controles del sistema.

### Paso 13. Validar el cambio de método desde configuración

El criterio de F-016 también exige cambiar el método desde configuración. Aunque la pantalla de configuración puede pertenecer a una historia posterior, dejar definido el contrato:

1. Pedir autenticación reciente antes de entrar al flujo de cambio.
2. Exigir autenticación actual antes de reemplazar el método guardado.
3. Validar completamente el método nuevo antes de escribirlo.
4. Guardar el cambio de manera atómica; no borrar el método anterior hasta que el nuevo esté confirmado y persistido.
5. Si el cambio falla o se cancela, conservar el método anterior y bloquear cualquier ruta que haya quedado en estado intermedio.
6. Al confirmar el cambio, invalidar sesiones y autorizaciones antiguas y volver a solicitar el nuevo método.
7. Mantener separados el alcance de implementación del cambio de método y el login de HU-002, sin dejar el requisito F-016 sin responsable en el backlog.

### Paso 14. Revisar documentación y definición de terminado

Antes de cerrar HU-002:

- Actualizar el modelo o las decisiones de seguridad si cambió el significado de `tiempo_bloqueo_segundos`.
- Documentar el valor por defecto, las opciones permitidas y cuándo vence una autenticación reciente.
- Registrar qué operaciones sensibles requieren reautenticación y confirmar que cada módulo invoca el guard común.
- Actualizar la documentación de privacidad sobre bloqueo, uso biométrico, recuperación local y vista previa de recientes.
- Revisar que no se hayan añadido permisos de red, cuentas o dependencias remotas para autenticar.

## 6. Criterios de aceptación y evidencia de cierre

La historia está terminada cuando:

1. Los cinco criterios de HU-002 tienen pruebas aprobadas o evidencia manual documentada para el comportamiento del sistema Android.
2. Ninguna pantalla de datos del taller puede mostrarse sin autenticación local válida.
3. El método configurado en HU-001 se verifica de forma coherente y los errores no abren acceso.
4. El bloqueo por inactividad usa un reloj monotónico y el umbral acordado, que puede configurarse según el modelo.
5. Las acciones sensibles enumeradas en HU-002 y F-016 quedan protegidas por una autorización reciente, no solo por controles visuales.
6. El contenido no queda visible en recientes y los secretos no se restauran desde estado guardado ni aparecen en logs.
7. El menú de HU-003 se abre solo tras autenticación correcta; HU-002 no incorpora indebidamente las funcionalidades del tablero.
8. La aplicación funciona sin internet y no crea cuenta ni valida identidad en línea.
9. Fallos de base de datos, biometría, credencial o restauración dejan la aplicación bloqueada y presentan un error entendible.
10. Compilación, pruebas automatizadas y lint pasan; las pruebas manuales en Android documentan versión del sistema, método y resultado.

## 7. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| El contenido se dibuja antes de comprobar la sesión al reanudar | Cubrir la ventana al pasar a segundo plano y validar la sesión antes de retirar la cubierta o renderizar datos. |
| Cambiar la hora del dispositivo alarga el bloqueo | Medir inactividad con reloj monotónico, no con fecha/hora civil. |
| Un booleano persistido deja acceso abierto tras reinicio | Mantener sesión y autorización reciente en memoria; proceso nuevo inicia bloqueado. |
| Biometría cancelada se interpreta como éxito o queda sin respuesta | Mapear explícitamente callbacks de éxito, cancelación y error; ejecutar acciones solo desde el callback exitoso. |
| Una pantalla comprueba acceso, pero un caso de uso futuro lo omite | Hacer obligatorio un guard compartido en la capa de operaciones sensibles y probar denegación directa. |
| El tiempo de autenticación reciente es demasiado largo o se reutiliza | Establecer expiración corta, alcance por acción y revalidación antes de confirmar una operación destructiva. |
| Una vista previa de recientes muestra información privada | Mantener `FLAG_SECURE` y cubrir el contenido sensible al abandonar el primer plano; validar en dispositivo. |
| El borrado de datos o restauración se inicia tras expirar la sesión | Comprobar autorización inmediatamente antes de modificar datos y requerir confirmación explícita separada. |
| El método configurado deja de estar disponible | Mantener bloqueado el acceso y ofrecer solo alternativas explícitamente aprobadas; nunca desactivar autenticación silenciosamente. |

## 8. Estado de implementación

- El launcher público solo redirige a la entrada interna. `AccesoActivity` no está exportada; así, las solicitudes de autenticación reciente no devuelven resultados a otras aplicaciones. `MainActivity` no participa en el flujo de HU-002.
- El ingreso usa el método local persistido por HU-001. La sesión vive solo en memoria, inicia bloqueada tras recrear el proceso y vence a los 300 segundos configurados por HU-001. La medición usa el reloj monotónico del dispositivo.
- La ventana de acceso usa `FLAG_SECURE` para impedir capturas y vistas previas con contenido sensible.
- `ContratoAutenticacionReciente` y `ConsumidorAutorizacionReciente` proporcionan autorizaciones limitadas a acción y solicitud, de un solo uso y con vigencia máxima de 30 segundos. Los módulos que implementen credenciales de equipo, respaldo o borrado deberán integrar este contrato antes de ejecutar sus operaciones; esas funciones todavía no existen en el proyecto.
- El destino tras autenticar es provisional e informa que el menú operativo se incorporará con HU-003. La navegación al tablero real no puede completarse hasta implementar esa historia.
