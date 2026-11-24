# Automatización de pruebas de Achron

Pruebas automáticas del sitio de Achron (el software de gym para coaches) con Selenium y JUnit 5. Cada vez que se corren, el navegador recorre el sitio como lo haría un coach. Al final dejan un reporte Word por módulo, un reporte consolidado, la lista de hallazgos del día en el formato del informe diario, un historial para monitorear y un log de toda la corrida. También queda un reporte visual con Allure.

Este documento explica qué hace cada parte y por qué está así, para leerlo con calma y entender el código.

---

## 1. Qué problema resuelve

Probar Achron a mano tomaba tiempo todos los días:

- abrir el sitio, crear cuentas y probar cada formulario;
- correr cada módulo de prueba por separado y esperar;
- leer cada reporte y copiar a mano los hallazgos al informe diario;
- decidir la severidad de cada hallazgo;
- contar cuántos casos pasaron y fallaron.

Ahora un solo comando (`mvn test`) hace todo eso y deja los resultados listos.

**Qué es Achron:** una aplicación web para coaches deportivos. Sirve para manejar asesorados, la biblioteca de ejercicios y rutinas, los cobros (cartera), el chat, un asistente con IA y el perfil del coach. Las pruebas cubren los roles **coach** y **atleta** (asesorado).

---

## 2. Herramientas y por qué

| Herramienta | Qué es | Por qué esta y no otra |
| --- | --- | --- |
| **Java 11** | El lenguaje del proyecto | Es el más usado con Selenium en equipos de QA y tiene muchos ejemplos. 11 es una versión estable con soporte largo. |
| **Maven** | Descarga las librerías y corre las pruebas | Con un solo archivo (`pom.xml`) cualquier persona baja todo y corre `mvn test`. Gradle haría lo mismo, pero Maven es más común en QA. |
| **Selenium 4** | Controla el navegador (clic, escribir, leer) | Es el estándar. Selenium Manager descarga solo el driver de Chrome; no hay que instalarlo a mano. |
| **JUnit 5** | Organiza y ejecuta las pruebas | Trae `@ParameterizedTest` (una tabla de casos), `@Tag` (correr por módulo), `assertAll` y extensiones para engancharse al ciclo de vida. |
| **Apache POI** | Crea archivos Word desde Java | Permite un reporte con portada, índice, tablas y capturas, en el formato que se entrega. |
| **SLF4J + Logback** | El log | Es la pareja estándar en Java. Deja un archivo por corrida y permite marcar cada línea con el módulo y el caso (MDC). |
| **Allure** | Reporte web interactivo | Muestra la corrida con gráficos, filtros por módulo y severidad, la captura de cada caso y la tendencia entre corridas. Complementa al Word. |

---

## 3. Instalación paso a paso

1. **JDK 11 o más nuevo.** Instalar el JDK (por ejemplo Temurin 21) y revisar en una consola que funcione:
   ```
   java -version
   ```
2. **Maven 3.9 o más nuevo.** Descargarlo de maven.apache.org, descomprimirlo, agregar su carpeta `bin` al PATH y revisar:
   ```
   mvn -v
   ```
3. **Google Chrome** actualizado. El driver lo baja Selenium solo.
4. **VS Code** (opcional) con las extensiones *Extension Pack for Java* y *Maven for Java*.
5. Clonar el proyecto y, dentro de la carpeta, correr `mvn test-compile` para bajar todo y revisar que compila.

---

## 4. Estructura de carpetas

```
AutomatizacionAchrom
├── pom.xml                       versiones y librerías del proyecto
├── README.md                     este documento
├── lee_me.txt                    descripción corta y el contador "Corridas: N"
├── src/test/java/com/achron
│   ├── config
│   │   ├── Configuracion.java    lee config.properties y datos.properties (-D gana)
│   │   └── Corrida.java          número e id de la corrida (C112_2026-11-22_14-05-31)
│   ├── core
│   │   ├── BaseTest.java         base de todas las pruebas: abre y cierra el navegador
│   │   ├── DriverFactory.java    crea Chrome o Edge según la configuración
│   │   ├── Acciones.java         clic, escribir, leer y esperar (esperas explícitas)
│   │   ├── CuentaDePrueba.java   crea las cuentas de prueba (coach y atletas) por clase
│   │   ├── ArchivosDePrueba.java imagen y archivo de texto para subir
│   │   └── Modulo.java           anotación con el nombre y el título del módulo
│   ├── pages                     Page Objects: localizadores y acciones de cada pantalla
│   │   ├── LoginPage, RegistroPage, MenuLateral, PanelPage, PerfilPage,
│   │   ├── EjerciciosPage, BibliotecaPage, InicioPublicoPage, SeccionesPage,
│   │   └── AtletaPage, AsesoradosPage, CarteraPage   (atleta y coach con asesorados)
│   ├── reporting                 todo lo que se genera después de probar
│   │   ├── Caso.java             un caso ejecutado: comprobaciones, captura y resultado
│   │   ├── SeguimientoCasos.java extensión JUnit: log, captura, Allure, reportes al final
│   │   ├── EventosCorrida.java   inicio y fin de toda la corrida, reporte consolidado
│   │   ├── ResultadosCorrida.java casos de todos los módulos de la corrida
│   │   ├── ReporteWord.java      reportes Word (por módulo y consolidado)
│   │   ├── Hallazgos.java        Hallazgos_<fecha>.txt
│   │   ├── Historial.java        historial_corridas.csv e Informe_<fecha>.txt
│   │   ├── Log.java              archivo de log de la corrida
│   │   └── ServiceNow.java       crea incidentes (INC) en ServiceNow; apagado por defecto
│   └── tests                     una clase por módulo
│       ├── RegistroTest, SesionTest, InicioPublicoTest, PanelTest,
│       ├── PerfilTest, EjerciciosTest, BibliotecaTest, SeccionesTest,
│       ├── AsesoradosTest        coach con atletas: comprobantes, chat, sesiones, rutinas
│       └── AtletaTest            rol atleta: registro, elegir coach, panel, perfil, pago, chat y sesiones
├── src/test/resources
│   ├── config.properties         URL, navegador, esperas, rutas, datos del reporte
│   ├── datos.properties          datos de prueba
│   ├── logback-test.xml          formato del log en consola
│   ├── plantilla_reporte.docx    estilos de los reportes Word
│   └── META-INF/services/...     registra EventosCorrida en JUnit
└── reportes de cada modulo       (se crea al correr; no se sube a git)
    ├── <Modulo>/Reporte_<Modulo>_<corrida>.docx
    ├── Reporte_Consolidado_<corrida>.docx
    ├── Hallazgos_<fecha>.txt
    ├── Informe_<fecha>.txt
    ├── historial_corridas.csv
    └── logs/ejecucion_<corrida>.log
```

La regla es simple: **las pruebas dicen qué revisar, las páginas saben dónde está cada cosa y `core` sabe cómo tocar el navegador.**

---

## 5. Conceptos clave

### Page Object Model
Cada pantalla del sitio tiene una clase en `pages` con sus localizadores (cómo encontrar un botón o un campo) y sus acciones (`iniciarSesion`, `abrirCrear`, `guardarBanco`...). Las pruebas no tienen ni un localizador: llaman a la página. Si el sitio cambia un botón, se corrige en un solo lugar. Las páginas no hacen comprobaciones; eso lo decide la prueba.

### BaseTest y el ciclo de vida de JUnit
JUnit ejecuta, en este orden:

1. `@BeforeAll` (una vez por clase): cada clase crea su cuenta de coach con `CuentaDePrueba.crearCoach`. `AsesoradosTest` además crea tres atletas con `CuentaDePrueba.crearAtleta`. Cada atleta elige al coach, registra un pago con un comprobante de prueba (una imagen; no se mueve dinero) y el primero le escribe por el chat. `AtletaTest` crea un coach y el atleta lo registra su primer caso (ATL-01); los demás casos entran con esa cuenta, y los que necesitan al coach (ver el comprobante, responder, programar una sesión) entran como coach en el mismo navegador después de salir de la cuenta del atleta.
2. Por cada caso:
   - `@BeforeEach`: `BaseTest` abre un navegador nuevo.
   - El caso (`@ParameterizedTest`): hace lo que dice la tabla y anota las comprobaciones.
   - `@AfterEach`: `BaseTest` cierra el navegador.
3. `@AfterAll` (una vez por clase): la extensión genera el Word, los hallazgos y el historial.

Un navegador por caso hace que un caso que falla no ensucie al siguiente.

### @ParameterizedTest y @CsvSource
Cada clase tiene arriba una tabla con un caso por fila: ID, categoría, descripción, acción, campo, valor y severidad. JUnit ejecuta el mismo método una vez por fila. Agregar un caso es agregar una fila. Los valores como `perfil.telefono` se buscan en `datos.properties`.

### @Tag
Cada clase tiene una etiqueta (`registro`, `sesion`, `perfil`...) para correr un solo módulo: `mvn test -Dgroups=perfil`.

### Esperas explícitas
El sitio tarda en responder. En vez de "esperar 3 segundos" (lento y poco confiable), `Acciones` espera hasta que pase algo concreto: que aparezca el botón, que cambie la URL o que salga el aviso. El tiempo máximo de cada espera está en `config.properties`. El driver no usa espera implícita.

### verificar(esperado, obtenido) y assertAll
Cada comprobación se guarda con lo que se esperaba y lo que se obtuvo:
```java
verificar("Mensaje de guardado", "aviso '¡Perfil actualizado!'", aviso, aviso.contains("actualizado"));
```
Al final, `comprobarTodo()` usa `assertAll`: JUnit revisa **todas** las comprobaciones y reporta todas las que fallaron, no solo la primera. Lo mismo queda en el Word y en Allure.

### FALLÓ vs ERROR
- **FALLÓ:** el sitio no hizo lo esperado. Es un hallazgo y va a `Hallazgos_<fecha>.txt`.
- **ERROR:** se rompió la prueba (no encontró un botón, se demoró, se cerró el navegador). No es un hallazgo; va al log con el detalle completo.

La extensión lo decide así: si el caso terminó por un `assert`, es FALLÓ; si terminó por otra excepción, o antes de comprobar nada, es ERROR.

### Extensiones y listeners de JUnit
- `SeguimientoCasos` (extensión, en `BaseTest` con `@ExtendWith`) se engancha antes y después de cada caso y de cada clase.
- `EventosCorrida` (listener, registrado en `META-INF/services`) se entera del inicio y el fin de **toda** la corrida.

---

## 6. Qué pasa en una corrida

```
mvn test
  └─ EventosCorrida: "inicio de la corrida C112_..."   (log)
     └─ por cada clase (RegistroTest, SesionTest, ...)
        ├─ @BeforeAll: crear cuenta de coach
        ├─ por cada fila de la tabla
        │   ├─ @BeforeEach: abrir navegador
        │   ├─ el caso: páginas + verificar(...) + comprobarTodo()
        │   ├─ SeguimientoCasos: PASÓ/FALLÓ/ERROR, captura, log, Allure
        │   └─ @AfterEach: cerrar navegador
        └─ SeguimientoCasos (fin de la clase):
            Word del módulo, Hallazgos_<fecha>.txt, historial_corridas.csv, Informe_<fecha>.txt
  └─ EventosCorrida: Reporte_Consolidado_<corrida>.docx + "fin de la corrida ..." (log)
```

1. Maven compila y arranca JUnit.
2. `Corrida` suma 1 al contador de `lee_me.txt` y arma el id de la corrida.
3. Cada clase crea su cuenta y ejecuta sus casos.
4. Después de cada caso queda una línea en el log (`2026-11-22 14:05:40 | Perfil | PER-07 | FALLÓ | 16.2 s`), la captura y los datos en Allure.
5. Al terminar cada clase salen su Word, sus hallazgos y su línea del historial.
6. Al terminar todo sale el reporte consolidado y el resumen de la corrida en el log.

---

## 7. Un caso de principio a fin: REG-03 (correo sin dominio)

1. **La fila de la tabla** en `RegistroTest`:
   `"REG-03 | Correo | Correo sin dominio | email | prueba@ | Alta"`.
   JUnit llama a `registro(id, categoria, descripcion, campo, valor, severidad)` con esos valores.
2. **`nuevoCaso(...)`** (de `BaseTest`) crea el `Caso`, que guarda las comprobaciones.
3. **`RegistroPage.abrir()`** va al inicio, hace clic en "Registrarse" y en "Regístrate aquí". Cada clic pasa por `Acciones.clic`, que espera a que el botón se pueda pulsar.
4. **`registro.llenar(...)`** escribe datos válidos y únicos (con el número de corrida); después `registro.escribir("email", "prueba@")` pone el valor inválido solo en ese campo.
5. Antes de enviar se lee el mensaje del navegador ("Ingresa texto después del signo @"), porque si el sitio acepta el dato el formulario desaparece.
6. **`registro.enviarComoCoach()`** elige "Soy coach" y envía; **`seRegistro()`** espera hasta 5 s a ver si aparece "Registro exitoso" o la URL pasa a `/dashboard`.
7. **Comprobaciones:** no navegó al panel, no salió el mensaje de éxito, el campo quedó con error, hay un mensaje, los demás campos siguen sin error y el valor se conservó.
8. **`comprobarTodo()`**: si alguna no se cumplió, `assertAll` hace fallar el caso con todas las diferencias.
9. **`SeguimientoCasos`** decide el resultado, toma la captura de toda la página, escribe `... | Registro | REG-03 | PASÓ | 6.1 s` en el log y adjunta todo en Allure.
10. Al terminar la clase, REG-03 sale en el Word de Registro y, si hubiera fallado, en `Hallazgos_<fecha>.txt`.

---

## 8. Configuración

Todo lo que puede cambiar está fuera del código. Cualquier valor se puede pisar con `-Dclave=valor` sin tocar el archivo.

**`src/test/resources/config.properties`**

| Clave | Para qué | Ejemplo |
| --- | --- | --- |
| `url` | Sitio que se prueba | `https://magenta-platypus-05cee2.netlify.app/` |
| `ambiente` | Nombre del ambiente en los reportes | `Netlify (pruebas)` |
| `navegador` | `chrome` o `edge` | `chrome` |
| `navegador.headless` | Correr sin abrir ventana | `false` |
| `navegador.dejarAbierto` | No cerrar el navegador (para revisar a mano) | `false` |
| `espera.corta` / `espera.respuesta` / `espera.larga` | Tiempos máximos de espera, en segundos | `3` / `5` / `10` |
| `carpeta.reportes`, `carpeta.logs` | Dónde se guardan los resultados | `reportes de cada modulo` |
| `archivo.corridas` | Dónde va el contador "Corridas: N" | `lee_me.txt` |
| `reporte.proyecto`, `reporte.autor`, `reporte.rol` | Datos de la portada del Word | |

**`src/test/resources/datos.properties`** tiene los datos de prueba: la cuenta base, los datos válidos de cada módulo, los nombres de lo que se crea, etc. Para cambiar un dato se edita la clave; las pruebas lo leen solas.

Si el sitio está lento, basta con subir los tiempos: `mvn test -Despera.respuesta=10`.

---

## 9. Cómo correr

| Qué | Comando |
| --- | --- |
| Todo | `mvn test` |
| Un módulo | `mvn test -Dgroups=perfil` (etiquetas: `registro`, `sesion`, `inicio`, `panel`, `perfil`, `ejercicios`, `biblioteca`, `secciones`, `asesorados`) |
| Una clase | `mvn test -Dtest=PerfilTest` |
| Sin abrir ventanas | `mvn test -Dnavegador.headless=true` |
| Con Edge | `mvn test -Dnavegador=edge` |
| Ver Allure | `mvn allure:serve` (abre el navegador con el reporte) |
| Generar Allure en carpeta | `mvn allure:report` → `target/site/allure-maven-plugin/index.html` |
| Abrir una cuenta para revisar a mano | `mvn test-compile exec:java` (crea un coach nuevo y deja Chrome abierto en su perfil; imprime el correo; no cuenta como corrida) |

Un caso fallado no detiene la corrida (`testFailureIgnore` en el `pom.xml`), así siempre salen todos los reportes. Maven igual imprime al final qué casos fallaron.

---

## 10. Dónde queda cada resultado

| Archivo | Qué tiene |
| --- | --- |
| `reportes de cada modulo/<Modulo>/Reporte_<Modulo>_<corrida>.docx` | Portada, índice, resumen ejecutivo (por categoría y severidad), lista de casos y una sección por caso con esperado vs obtenido, la captura y dónde está en el log |
| `reportes de cada modulo/Reporte_Consolidado_<corrida>.docx` | Toda la corrida: resultado por módulo, hallazgos y detalle de cada caso (sin capturas) |
| `reportes de cada modulo/Hallazgos_<fecha>.txt` | `ID \| Módulo \| Qué pasa \| Severidad \| Nuevo o Ya reportado \| Corrida`, listo para el informe diario |
| `reportes de cada modulo/Informe_<fecha>.txt` | "Lo que terminé hoy" listo para copiar |
| `reportes de cada modulo/historial_corridas.csv` | Una línea por módulo y corrida (monitoreo) |
| `reportes de cada modulo/logs/ejecucion_<corrida>.log` | El log completo de la corrida |
| `target/allure-results` | Datos para Allure (se ven con `mvn allure:serve`) |

El **id de la corrida** (`C112_2026-11-22_14-05-31`) sale en todos: en el nombre del log, en el historial, en los hallazgos, en los Word y en Allure. Así, desde un hallazgo se llega al log (buscando `| PER-07 |`) y al caso en Allure.

---

## 11. Monitoreo y logs

**El log** tiene una línea por evento:
```
2026-11-22 14:05:22 | corrida | - | inicio de la corrida C112_2026-11-22_14-05-22
2026-11-22 14:05:40 | Perfil | - | cuenta de prueba creada: pruebacorreoperfil112@gmail.com
2026-11-22 14:05:56 | Perfil | PER-07 | FALLÓ | 16.2 s
2026-11-22 14:06:10 | Perfil | PER-08 | ERROR | 12.0 s | Telefono con simbolos
org.openqa.selenium.TimeoutException: ...   (detalle completo del error)
2026-11-22 14:20:31 | Perfil | - | fin del modulo | 28 casos | 820 s | CON HALLAZGOS
2026-11-22 14:58:02 | corrida | - | fin de la corrida C112_... | 155 casos, ... | ROTA
```
Para revisar un caso, se busca `| ID |` en el archivo.

**El historial** (`historial_corridas.csv`) tiene `corrida;fecha;hora;modulo;total;pasaron;fallaron;errores_tecnicos;segundos;estado_corrida`. El estado de cada módulo es:

- **OK:** todo pasó.
- **CON HALLAZGOS:** el sitio falló en algo. Es lo normal mientras haya defectos abiertos.
- **ROTA:** alguna prueba tuvo ERROR. Hay que revisar la automatización, no el sitio.

**La hoja Monitoreo del Excel** de entregables lee ese historial y muestra:
- la última corrida y su estado;
- la tendencia de % de fallos del sitio y % de error de la automatización;
- la duración por módulo;
- alertas en rojo o amarillo.

Los umbrales están en la hoja Supuestos:
- más de 5% de ERROR = automatización rota;
- un módulo que dura más de 1,5 veces su promedio = lento;
- si no hubo corrida hoy, sale una alerta.

**Integración con ServiceNow** (`reporting/ServiceNow.java`):

- **Corrida rota:** cuando una corrida queda ROTA, crea un incidente (INC) con el resumen y la ruta del log.
- **Hallazgos nuevos:** con `servicenow.incidentePorHallazgo=true`, crea además un INC por cada hallazgo nuevo.
- **Cómo se configura:** viene apagada (`servicenow.habilitado=false`) y, mientras tanto, solo escribe en el log qué incidente habría creado. Para encenderla:
  1. llenar en `config.properties` los valores `servicenow.url`, `servicenow.usuario` y `servicenow.grupo`;
  2. pasar la clave con `-Dservicenow.clave=...` o en la variable de entorno `SERVICENOW_CLAVE`. Nunca se escribe en el archivo;
  3. poner `servicenow.habilitado=true`.

  Usa la API REST de ServiceNow (`/api/now/table/incident`).

---

## 12. Cómo agregar un caso o un módulo

**Un caso nuevo en un módulo que ya existe**
1. Agregar una fila al final de la tabla `@CsvSource` de la clase, con el siguiente ID libre (por ejemplo `PER-29`) y su severidad.
2. Si la acción ya existe (`guardar`, `rechazar`...), no hace falta más código.
3. Si es una acción nueva, agregar su `case` en el `switch` del método y, si toca algo nuevo de la pantalla, un método en la página correspondiente.
4. Si necesita datos, agregarlos en `datos.properties`.

**Un módulo nuevo**
1. Crear la página en `pages` (localizadores y acciones).
2. Crear `XxxTest` en `tests`, con:
   - que extienda `BaseTest`;
   - `@Tag("xxx")` y `@Modulo(nombre = "Xxx", titulo = "...")`;
   - en `@BeforeAll`, `CuentaDePrueba.crearCoach("xxx", <número libre>)` si necesita sesión;
   - la tabla `@CsvSource` con los IDs del módulo (`XXX-01`...).
3. Los reportes, hallazgos, historial, log y Allure salen solos gracias a `BaseTest`.

---

## 13. Problemas comunes

| Síntoma | Causa probable | Qué hacer |
| --- | --- | --- |
| Muchos ERROR con `TimeoutException` | El sitio está lento o caído | Abrir la URL a mano; subir `espera.respuesta` / `espera.larga` |
| `NoSuchElementException` en un caso que antes pasaba | El sitio cambió un botón o un texto | Corregir el localizador en la página (`pages`) |
| `No se pudo iniciar sesion con ...` | La cuenta no se creó o el login no respondió | Revisar en el log la línea "cuenta de prueba creada" |
| Chrome no abre / versión del driver | Chrome desactualizado o sin internet | Actualizar Chrome; Selenium baja el driver solo |
| El índice del Word sale vacío | Word no actualizó el campo | Clic derecho sobre el índice > Actualizar campo |
| Tildes raras en la consola | La consola de Windows no está en UTF-8 | Correr `chcp 65001` antes; el archivo de log está bien |
| `mvn` no se reconoce | Maven no está en el PATH | Agregar la carpeta `bin` de Maven al PATH |

---

## 14. Convenciones

- **IDs de casos:**
  - `REG` registro
  - `SES` sesión
  - `ASE` coach con asesorados
  - `ATL` rol atleta
  - `INI` página pública
  - `DAS` panel
  - `PER` perfil
  - `EJE` ejercicios
  - `BIB` biblioteca
  - `SEC` secciones

  Los IDs no se cambian ni se reutilizan: los casos nuevos van al final con el siguiente número.
- **Severidad:**
  - **Alta:** el sitio acepta o guarda datos inválidos, o se rompe un flujo principal (registro, sesión, perfil); también que una página principal cargue.
  - **Media:** una función no hace lo que promete, o falta un mensaje en un flujo importante.
  - **Baja:** textos sin traducir, enlaces a "#" o mensajes menores.
- **Nombres:** el código está en español, como el sitio: clases en PascalCase (`PerfilPage`), métodos en camelCase (`guardarBanco`) y constantes en MAYÚSCULAS.
- **Datos que deja la prueba:** el sitio no permite borrar. Lo que se crea lleva "ZZ ... Auto N" o "pruebacorreo...N" para reconocerlo.

---

## 15. Glosario

| Término | Significado |
| --- | --- |
| Caso | Una fila de la tabla: una cosa concreta que se prueba |
| Comprobación | Una revisión dentro de un caso, con esperado y obtenido |
| Hallazgo | Un caso FALLÓ: el sitio no se comportó como se esperaba |
| Corrida | Una ejecución completa de `mvn test`; tiene un id |
| Localizador | Cómo se encuentra un elemento en la página (`By.id("email")`, un CSS o un XPath) |
| Page Object | Clase que representa una pantalla del sitio |
| Espera explícita | Esperar hasta que se cumpla algo concreto, con un tiempo máximo |
| Headless | Correr el navegador sin ventana |
| MDC | Datos que Logback agrega a cada línea del log (módulo y caso) |
| Toast / aviso | El mensaje emergente que muestra el sitio ("¡Perfil actualizado!") |
