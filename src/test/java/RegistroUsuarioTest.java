import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del registro de usuarios en https://magenta-platypus-05cee2.netlify.app/
 *
 * Todos los casos estan en la tabla de @CsvSource (ID, categoria, caso, campo, valor):
 *  - REG-01 registra un coach con datos validos (debe funcionar).
 *  - Los demas llenan el formulario con datos validos y cambian SOLO el campo indicado
 *    por un valor invalido. El sitio debe rechazarlo.
 *
 * Cada comprobacion guarda lo ESPERADO y lo OBTENIDO. Al final (@AfterAll) se genera
 * un reporte Word en "reportes de cada modulo/Registro" con una captura de pantalla de cada caso.
 * Los tiempos de espera tambien salen de datos.properties (espera.corta, espera.respuesta, espera.larga).
 *
 * Los datos salen de src/test/resources/datos.properties. El NIT y el correo cambian
 * en cada caso usando el contador "Corridas: N" que se guarda en lee_me.txt.
 */
public class RegistroUsuarioTest {

    // La URL queda fija en el codigo (no viene del properties)
    private static final String URL = "https://magenta-platypus-05cee2.netlify.app/";

    // Si corres con -DdejarAbierto=true, Chrome no se cierra al terminar REG-01
    private static final boolean DEJAR_ABIERTO = Boolean.getBoolean("dejarAbierto");

    private static final List<String> CAMPOS = Arrays.asList("fullName", "cedula", "email", "password", "confirmPassword");

    // Resultados de todos los casos, para el reporte Word
    private static final List<Caso> CASOS = new ArrayList<>();

    // Correo y NIT que se registraron en REG-01 (para probar duplicados)
    private static String emailRegistrado;
    private static String nitRegistrado;

    private WebDriver driver;
    private Properties datos;
    private Caso caso; // el caso que se esta ejecutando ahora
    private boolean dejarEsteAbierto = false;

    @BeforeEach
    void setUp() throws IOException {
        datos = cargarDatos();

        ChromeOptions opciones = new ChromeOptions();
        if (DEJAR_ABIERTO) {
            // "detach" deja la ventana viva aunque la prueba termine
            opciones.setExperimentalOption("detach", true);
        }
        driver = new ChromeDriver(opciones);
        driver.manage().window().maximize();
        // Espera hasta espera.larga a que aparezca cada elemento antes de fallar
        driver.manage().timeouts().implicitlyWait(segundos("espera.larga"));
        driver.get(URL);
    }

    @AfterEach
    void tearDown() {
        if (caso != null) {
            // Evidencia: captura de pantalla del estado final, pase o falle
            caso.foto = capturaPaginaCompleta();
            if (caso.filas.isEmpty()) {
                caso.filas.add(new String[] {"Ejecucion", "la prueba llega a las comprobaciones", "se detuvo antes por un error", "✘"});
            }
            CASOS.add(caso);
        }
        if (driver != null && !dejarEsteAbierto) {
            driver.quit();
        }
    }

    // ---------- Tabla de casos ----------
    // campo "ninguno" = todos los datos validos; campo "todos" = todos los campos con ese valor.
    // valor "(correo del REG-01)" / "(NIT del REG-01)" = reutiliza el dato registrado en REG-01.

    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "REG-01 | Registro           | Registro exitoso (datos validos)  | ninguno         | ''",
            "REG-02 | Correo             | Correo sin arroba                 | email           | pruebacorreo.gmail.com",
            "REG-03 | Correo             | Correo sin dominio                | email           | prueba@",
            "REG-04 | Correo             | Correo con espacios               | email           | prueba correo@gmail.com",
            "REG-05 | Correo             | Correo con doble arroba           | email           | prueba@@gmail.com",
            "REG-06 | NIT                | NIT con letras                    | cedula          | 12345abcd",
            "REG-07 | NIT                | NIT con simbolos                  | cedula          | 123-456*78",
            "REG-08 | Nombre             | Nombre con numeros                | fullName        | Jacobo123",
            "REG-09 | Nombre             | Nombre con simbolos               | fullName        | Jacobo@#$",
            "REG-10 | Contraseña         | Contraseña corta (menos de 8)     | password        | abc12",
            "REG-11 | Contraseña         | Contraseñas no coinciden          | confirmPassword | otraClave99",
            "REG-12 | Campos obligatorios| Formulario vacio                  | todos           | ''",
            "REG-13 | Duplicados         | Correo repetido                   | email           | (correo del REG-01)",
            "REG-14 | Duplicados         | NIT repetido                      | cedula          | (NIT del REG-01)"
    })
    void registro(String id, String categoria, String descripcion, String campo, String valor) {
        // Reemplazar los valores especiales de duplicados por el dato real
        if (valor.equals("(correo del REG-01)")) {
            valor = emailRegistrado != null ? emailRegistrado : leer("email");
        }
        if (valor.equals("(NIT del REG-01)")) {
            valor = nitRegistrado != null ? nitRegistrado : leer("nit");
        }
        caso = new Caso(id, categoria, descripcion, campo.equals("ninguno") ? "datos validos de datos.properties" : campo + " = '" + valor + "'");

        // 1) Llenar el formulario con datos validos
        String[] validos = llenarFormularioValido();

        // 2) Poner el valor invalido en el campo (o campos) que se prueban
        List<String> probados = new ArrayList<>();
        if (campo.equals("todos")) {
            probados.addAll(CAMPOS);
        } else if (!campo.equals("ninguno")) {
            probados.add(campo);
        }
        for (String c : probados) {
            escribir(By.id(c), valor);
        }
        if (campo.equals("password")) {
            escribir(By.id("confirmPassword"), valor); // para que solo falle el largo, no la coincidencia
        }

        // Lo que quedo escrito y el mensaje propio del navegador (se leen antes de enviar,
        // porque si el sitio acepta el dato el formulario desaparece)
        String valoresEscritos = "";
        String mensajesNavegador = "";
        for (String c : probados) {
            WebElement el = driver.findElement(By.id(c));
            valoresEscritos += "'" + el.getDomProperty("value") + "' ";
            mensajesNavegador += el.getDomProperty("validationMessage") + " ";
        }

        // 3) Enviar y esperar la respuesta del sitio
        enviarComoCoach();
        esperarRespuesta();
        String url = driver.getCurrentUrl();
        boolean exito = !textos(By.xpath("//*[contains(text(), 'Registro exitoso')]")).isBlank();
        String avisos = textos(By.cssSelector("li[data-sonner-toast]")).replace("¡Registro exitoso!", "").trim();

        // 4) Comprobaciones: cada una guarda esperado vs obtenido
        if (campo.equals("ninguno")) {
            dejarEsteAbierto = DEJAR_ABIERTO;
            emailRegistrado = validos[0];
            nitRegistrado = validos[1];
            verificar("Cuenta creada", "'Registro exitoso' o /dashboard",
                    (exito ? "aparecio 'Registro exitoso'" : "sin 'Registro exitoso'") + " | url: " + url,
                    exito || url.contains("/dashboard"));
            verificar("Sin errores en el formulario", "ningun mensaje de error",
                    "'" + errores(CAMPOS) + avisos + "'", (errores(CAMPOS) + avisos).isBlank());
        } else {
            String erroresProbados = (mensajesNavegador + errores(probados)).trim();
            List<String> otros = new ArrayList<>(CAMPOS);
            otros.removeAll(probados);
            String erroresOtros = errores(otros);

            verificar("No navega al dashboard", "url sin /dashboard", url, !url.contains("/dashboard"));
            verificar("Sin mensaje de exito", "no aparece 'Registro exitoso'",
                    exito ? "aparecio 'Registro exitoso'" : "no aparecio", !exito);
            verificar("Campo marcado con error", "error en " + probados,
                    erroresProbados.isBlank() ? "ningun error en el campo" : erroresProbados, !erroresProbados.isBlank());
            verificar("Mensaje de error visible", "un texto que explique el error",
                    (erroresProbados + " " + avisos).trim().isBlank() ? "ningun mensaje" : (erroresProbados + " " + avisos).trim(),
                    !(erroresProbados + avisos).isBlank());
            verificar("Otros campos sin error", "sin errores fuera de " + probados,
                    erroresOtros.isBlank() ? "sin errores" : erroresOtros, erroresOtros.isBlank());
            verificar("Valor conservado", "'" + valor + "' en cada campo probado", valoresEscritos.trim(),
                    valoresEscritos.replace("'" + valor + "' ", "").isBlank());
        }

        System.out.println(id + " " + descripcion + " -> " + (caso.paso() ? "PASO" : "FALLO"));

        // 5) JUnit falla si alguna comprobacion no se cumplio, con esperado y obtenido
        assertTrue(caso.paso(), caso.fallos());
    }

    // ---------- Comprobaciones y resultados ----------

    // Guarda una fila (comprobacion, esperado, obtenido, ✔/✘) en el caso actual
    private void verificar(String que, String esperado, String obtenido, boolean ok) {
        caso.filas.add(new String[] {que, esperado, obtenido, ok ? "✔" : "✘"});
    }

    // Datos y resultados de un caso (para el reporte)
    static class Caso {
        String id, categoria, descripcion, dato;
        List<String[]> filas = new ArrayList<>();
        byte[] foto;

        Caso(String id, String categoria, String descripcion, String dato) {
            this.id = id;
            this.categoria = categoria;
            this.descripcion = descripcion;
            this.dato = dato;
        }

        boolean paso() {
            return !filas.isEmpty() && fallos().isEmpty();
        }

        String fallos() {
            String texto = "";
            for (String[] f : filas) {
                if (f[3].equals("✘")) {
                    texto += "\n" + f[0] + " -> esperado: " + f[1] + " | obtenido: " + f[2];
                }
            }
            return texto;
        }
    }

    // ---------- Pasos del formulario ----------

    // Abre el registro y llena todos los campos con datos validos.
    // NIT y correo llevan el numero de corrida para no repetir usuarios. Devuelve {email, nit}.
    private String[] llenarFormularioValido() {
        int corrida = siguienteCorrida();
        String[] partesEmail = leer("email").split("@");
        String nit = String.valueOf(Long.parseLong(leer("nit")) + corrida);
        String email = partesEmail[0] + corrida + "@" + partesEmail[1];

        driver.findElement(By.xpath("//button[normalize-space()='Registrarse']")).click();
        // A veces el clic llega antes de que la pagina este lista: se reintenta hasta 3 veces
        for (int intento = 1; intento <= 3 && !aparece(By.id("fullName")); intento++) {
            driver.findElement(By.xpath("//button[normalize-space()='Regístrate aquí']")).click();
        }

        escribir(By.id("fullName"), leer("nombre"));
        escribir(By.id("cedula"), nit);
        escribir(By.id("email"), email);
        escribir(By.id("password"), leer("contrasena"));
        escribir(By.id("confirmPassword"), leer("contrasena"));
        return new String[] {email, nit};
    }

    // Elige el rol "Soy coach" y pulsa el boton de enviar
    private void enviarComoCoach() {
        clicCentrado(By.xpath("//button[.//span[normalize-space()='Soy coach']]"));
        clicCentrado(By.xpath("//button[@type='submit']"));
    }

    // Espera (espera.respuesta) a que aparezca "Registro exitoso" o la pagina vaya a /dashboard
    private void esperarRespuesta() {
        try {
            new WebDriverWait(driver, segundos("espera.respuesta")).until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("/dashboard"),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'Registro exitoso')]"))));
        } catch (TimeoutException e) {
            // No se registro: es lo esperado en los casos invalidos
        }
    }

    // true si el elemento aparece antes de espera.corta
    private boolean aparece(By localizador) {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            new WebDriverWait(driver, segundos("espera.corta")).until(ExpectedConditions.visibilityOfElementLocated(localizador));
            return true;
        } catch (TimeoutException e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(segundos("espera.larga"));
        }
    }

    // Textos de error en rojo que el sitio pone debajo de cada campo
    private String errores(List<String> campos) {
        String texto = "";
        for (String c : campos) {
            String error = textos(By.xpath("//input[@id='" + c + "']/ancestor::div[contains(@class,'relative')][1]/following-sibling::p"));
            if (!error.isBlank()) {
                texto += c + ": " + error + " ";
            }
        }
        return texto;
    }

    // Une el texto de todos los elementos encontrados (sin esperar si no hay ninguno)
    private String textos(By localizador) {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        String texto = "";
        try {
            for (WebElement e : driver.findElements(localizador)) {
                texto += e.getText().trim() + " ";
            }
        } catch (StaleElementReferenceException e) {
            // La pagina cambio mientras se leia; se usa lo que se alcanzo a leer
        }
        driver.manage().timeouts().implicitlyWait(segundos("espera.larga"));
        return texto.trim();
    }

    private void escribir(By localizador, String texto) {
        WebElement campo = driver.findElement(localizador);
        campo.click();
        // Borrar con Ctrl+A y Retroceso (clear() no avisa a la pagina y esta seguiria viendo el valor viejo)
        campo.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        campo.sendKeys(texto);
    }

    // Lleva el elemento al centro de la pantalla y hace clic; si algo lo tapa, clic por JavaScript
    private void clicCentrado(By localizador) {
        WebElement elemento = driver.findElement(localizador);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", elemento);
        try {
            elemento.click();
        } catch (ElementClickInterceptedException e) {
            js.executeScript("arguments[0].click();", elemento);
        }
    }

    // ---------- Datos ----------

    // Lee la linea "Corridas: N" de lee_me.txt, le suma 1 y la guarda de nuevo.
    // Si la linea no existe, la agrega empezando en 1.
    private static int siguienteCorrida() {
        Path archivo = Paths.get("lee_me.txt");
        try {
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            int corrida = 1;
            boolean encontrada = false;
            for (int i = 0; i < lineas.size(); i++) {
                if (lineas.get(i).startsWith("Corridas:")) {
                    corrida = Integer.parseInt(lineas.get(i).substring("Corridas:".length()).trim()) + 1;
                    lineas.set(i, "Corridas: " + corrida);
                    encontrada = true;
                }
            }
            if (!encontrada) {
                lineas.add("Corridas: " + corrida);
            }
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
            return corrida;
        } catch (IOException e) {
            throw new RuntimeException("No pude leer/escribir lee_me.txt", e);
        }
    }

    // Lee un tiempo de espera (en segundos) de datos.properties
    private Duration segundos(String clave) {
        return Duration.ofSeconds(Long.parseLong(leer(clave)));
    }

    // Captura de TODA la pagina (no solo lo visible), usando una funcion de Chrome (CDP)
    private byte[] capturaPaginaCompleta() {
        ChromeDriver chrome = (ChromeDriver) driver;
        Map<String, Object> medidas = chrome.executeCdpCommand("Page.getLayoutMetrics", new HashMap<>());
        Map<?, ?> contenido = (Map<?, ?>) medidas.get("cssContentSize");
        Map<String, Object> zona = new HashMap<>();
        zona.put("x", 0);
        zona.put("y", 0);
        zona.put("width", contenido.get("width"));
        zona.put("height", contenido.get("height"));
        zona.put("scale", 1);
        Map<String, Object> opciones = new HashMap<>();
        opciones.put("captureBeyondViewport", true);
        opciones.put("clip", zona);
        Map<String, Object> foto = chrome.executeCdpCommand("Page.captureScreenshot", opciones);
        return Base64.getDecoder().decode((String) foto.get("data"));
    }

    private String leer(String clave) {
        String valor = datos.getProperty(clave);
        assertNotNull(valor, "Falta la clave '" + clave + "' en datos.properties");
        return valor.trim();
    }

    // Lee datos.properties en UTF-8 (para que funcionen tildes y la n con virgulilla).
    // Primero lo busca en el classpath; si no esta, lo lee desde la carpeta del proyecto.
    private Properties cargarDatos() throws IOException {
        Properties props = new Properties();
        InputStream entrada = getClass().getClassLoader().getResourceAsStream("datos.properties");
        if (entrada == null) {
            entrada = new FileInputStream("src/test/resources/datos.properties");
        }
        try (InputStreamReader lector = new InputStreamReader(entrada, StandardCharsets.UTF_8)) {
            props.load(lector);
        }
        return props;
    }

    // ---------- Reporte Word (Apache POI) ----------

    @AfterAll
    static void generarReporte() throws Exception {
        if (CASOS.isEmpty()) {
            return;
        }
        int pasaron = 0;
        for (Caso c : CASOS) {
            if (c.paso()) pasaron++;
        }
        String ultimaCorrida = "";
        for (String linea : Files.readAllLines(Paths.get("lee_me.txt"), StandardCharsets.UTF_8)) {
            if (linea.startsWith("Corridas:")) ultimaCorrida = linea.substring("Corridas:".length()).trim();
        }

        XWPFDocument doc = new XWPFDocument();
        parrafo(doc, "Reporte de pruebas - Registro de usuarios", 18, true);
        parrafo(doc, "Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 11, false);
        parrafo(doc, "URL: " + URL, 11, false);
        parrafo(doc, "Corrida (lee_me.txt): " + ultimaCorrida, 11, false);
        parrafo(doc, "Total: " + CASOS.size() + "   Pasaron: " + pasaron + "   Fallaron: " + (CASOS.size() - pasaron), 11, true);

        // Tabla resumen
        parrafo(doc, "Resumen", 14, true);
        XWPFTable resumen = doc.createTable(1, 5);
        fila(resumen.getRow(0), "ID", "Categoria", "Caso", "Dato usado", "Resultado");
        for (Caso c : CASOS) {
            fila(resumen.createRow(), c.id, c.categoria, c.descripcion, c.dato, c.paso() ? "PASÓ" : "FALLÓ");
        }

        // Detalle de cada caso, agrupado por categoria
        String categoriaActual = "";
        for (Caso c : CASOS) {
            if (!c.categoria.equals(categoriaActual)) {
                categoriaActual = c.categoria;
                parrafo(doc, "Categoria: " + categoriaActual, 14, true);
            }
            parrafo(doc, c.id + " - " + c.descripcion + ": " + (c.paso() ? "PASÓ" : "FALLÓ"), 12, true);
            parrafo(doc, "Dato usado: " + c.dato, 11, false);
            XWPFTable tabla = doc.createTable(1, 4);
            fila(tabla.getRow(0), "Comprobacion", "Esperado", "Obtenido", "✔/✘");
            for (String[] f : c.filas) {
                fila(tabla.createRow(), f);
            }
            if (c.foto != null) {
                BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(c.foto));
                int ancho = 450; // ancho de la imagen en puntos (cabe en la pagina)
                int alto = ancho * imagen.getHeight() / imagen.getWidth();
                doc.createParagraph().createRun().addPicture(new ByteArrayInputStream(c.foto),
                        Document.PICTURE_TYPE_PNG, c.id + ".png", Units.toEMU(ancho), Units.toEMU(alto));
            }
        }

        Files.createDirectories(Paths.get("reportes de cada modulo", "Registro"));
        String nombre = "reportes de cada modulo/Registro/Reporte_Registro_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".docx";
        try (FileOutputStream salida = new FileOutputStream(nombre)) {
            doc.write(salida);
        }
        doc.close();
        System.out.println("Reporte generado: " + nombre);
    }

    // Agrega un parrafo de texto al documento
    private static void parrafo(XWPFDocument doc, String texto, int tamano, boolean negrita) {
        XWPFRun run = doc.createParagraph().createRun();
        run.setText(texto);
        run.setFontSize(tamano);
        run.setBold(negrita);
    }

    // Escribe los textos en las celdas de una fila de tabla
    private static void fila(XWPFTableRow fila, String... textos) {
        for (int i = 0; i < textos.length; i++) {
            fila.getCell(i).setText(textos[i]);
        }
    }
}
