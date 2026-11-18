import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
import org.junit.jupiter.api.BeforeAll;
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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del modulo PERFIL en https://magenta-platypus-05cee2.netlify.app/
 *
 * Antes de todo (@BeforeAll) se crea una cuenta nueva con el formulario de registro
 * (correo y NIT unicos con el contador "Corridas: N" de lee_me.txt).
 * En cada caso se inicia sesion con esa cuenta, se hace clic en el nombre del usuario
 * (abre "Mi perfil") y se ejecuta la accion de la tabla de casos.
 *
 * Cada comprobacion guarda lo ESPERADO y lo OBTENIDO. Al final (@AfterAll) se genera
 * un reporte Word en "reportes de cada modulo/Perfil" con una captura de cada caso.
 * Datos validos y tiempos de espera salen de src/test/resources/datos.properties.
 */
public class PerfilTest {

    private static final String URL = "https://magenta-platypus-05cee2.netlify.app/";

    // Campos que estan en la pestaña "PERFIL PÚBLICO" (los demas estan en "INFORMACIÓN PERSONAL")
    private static final List<String> CAMPOS_PUBLICOS = Arrays.asList("professionalTitle", "biography", "methodology");

    private static final List<Caso> CASOS = new ArrayList<>();
    private static Properties datos;

    // Cuenta creada en @BeforeAll para estas pruebas
    private static String nombreCuenta;
    private static String emailCuenta;
    private static String contrasenaCuenta;

    private WebDriver driver;
    private Caso caso;

    // ---------- Preparacion ----------

    @BeforeAll
    static void crearCuenta() throws IOException {
        datos = cargarDatos();
        int corrida = siguienteCorrida();
        String[] partesEmail = leer("email").split("@");
        nombreCuenta = leer("nombre");
        emailCuenta = partesEmail[0] + "perfil" + corrida + "@" + partesEmail[1];
        contrasenaCuenta = leer("contrasena");
        String nit = String.valueOf(Long.parseLong(leer("nit")) + corrida);

        WebDriver d = new ChromeDriver();
        try {
            d.manage().timeouts().implicitlyWait(segundos("espera.larga"));
            d.get(URL);
            d.findElement(By.xpath("//button[normalize-space()='Registrarse']")).click();
            for (int intento = 1; intento <= 3 && d.findElements(By.id("fullName")).isEmpty(); intento++) {
                d.findElement(By.xpath("//button[normalize-space()='Regístrate aquí']")).click();
            }
            d.findElement(By.id("fullName")).sendKeys(nombreCuenta);
            d.findElement(By.id("cedula")).sendKeys(nit);
            d.findElement(By.id("email")).sendKeys(emailCuenta);
            d.findElement(By.id("password")).sendKeys(contrasenaCuenta);
            d.findElement(By.id("confirmPassword")).sendKeys(contrasenaCuenta);
            JavascriptExecutor js = (JavascriptExecutor) d;
            js.executeScript("arguments[0].click();", d.findElement(By.xpath("//button[.//span[normalize-space()='Soy coach']]")));
            js.executeScript("arguments[0].click();", d.findElement(By.xpath("//button[@type='submit']")));
            new WebDriverWait(d, segundos("espera.larga")).until(ExpectedConditions.urlContains("/dashboard"));
            System.out.println("Cuenta para pruebas de perfil: " + emailCuenta);
        } finally {
            d.quit();
        }
    }

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(segundos("espera.larga"));
        driver.get(URL);
    }

    @AfterEach
    void tearDown() {
        if (caso != null) {
            // Evidencia: captura de toda la pagina, pase o falle
            caso.foto = capturaPaginaCompleta();
            if (caso.filas.isEmpty()) {
                caso.filas.add(new String[] {"Ejecucion", "la prueba llega a las comprobaciones", "se detuvo antes por un error", "✘"});
            }
            CASOS.add(caso);
        }
        if (driver != null) {
            driver.quit();
        }
    }

    // ---------- Tabla de casos ----------
    // accion: navegar | ver | emailBloqueado | guardar | rechazar | largo | sinGuardar
    // valor "perfil.xxx" = se toma de datos.properties

    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "PER-01 | Navegacion                            | Clic en el nombre abre Mi perfil          | navegar        | -                 | ''",
            "PER-02 | Informacion personal - visualizacion  | Muestra los datos del registro            | ver            | -                 | ''",
            "PER-03 | Informacion personal - visualizacion  | El email no se puede modificar            | emailBloqueado | email             | otro@gmail.com",
            "PER-04 | Informacion personal - edicion valida | Cambiar nombre                            | guardar        | fullName          | perfil.nombre",
            "PER-05 | Informacion personal - edicion valida | Cambiar telefono                          | guardar        | phone             | perfil.telefono",
            "PER-06 | Informacion personal - edicion valida | Cambiar URL de avatar                     | guardar        | avatarUrl         | perfil.avatar",
            "PER-07 | Informacion personal - validaciones   | Telefono con letras                       | rechazar       | phone             | 300abc4567",
            "PER-08 | Informacion personal - validaciones   | Telefono con simbolos                     | rechazar       | phone             | 300-456#789",
            "PER-09 | Informacion personal - validaciones   | Telefono demasiado largo (40 digitos)     | rechazar       | phone             | 3001234567300123456730012345673001234567",
            "PER-10 | Informacion personal - validaciones   | Nombre vacio                              | rechazar       | fullName          | ''",
            "PER-11 | Informacion personal - validaciones   | Nombre con numeros                        | rechazar       | fullName          | Jacobo123",
            "PER-12 | Informacion personal - validaciones   | Nombre con simbolos                       | rechazar       | fullName          | Jacobo@#$",
            "PER-13 | Informacion personal - validaciones   | URL de avatar invalida                    | rechazar       | avatarUrl         | esto no es una url",
            "PER-14 | Perfil publico - edicion valida       | Guardar titular profesional               | guardar        | professionalTitle | perfil.titulo",
            "PER-15 | Perfil publico - edicion valida       | Guardar biografia                         | guardar        | biography         | perfil.biografia",
            "PER-16 | Perfil publico - edicion valida       | Guardar metodologia                       | guardar        | methodology       | perfil.metodologia",
            "PER-17 | Perfil publico - validaciones         | Biografia de mas de 500 caracteres        | largo          | biography         | 600",
            "PER-18 | Cancelar edicion                      | Cambio sin guardar se descarta al recargar | sinGuardar    | phone             | 3119998888"
    })
    void perfil(String id, String categoria, String descripcion, String accion, String campo, String valor) {
        if (valor.startsWith("perfil.")) {
            valor = leer(valor);
        }
        caso = new Caso(id, categoria, descripcion, campo.equals("-") ? "cuenta creada en el registro" : campo + " = '" + valor + "'");

        iniciarSesion();
        // Navegacion: clic en el nombre del usuario (enlace a /profile en el menu)
        clicCentrado(By.cssSelector("a[href='/profile']"));
        esperarDatosCargados();

        if (accion.equals("navegar")) {
            String url = driver.getCurrentUrl();
            verificar("Abre la pagina de perfil", "url con /profile", url, url.contains("/profile"));
            verificar("Pestaña Informacion personal", "visible", visible(pestana("INFORMACIÓN PERSONAL")) ? "visible" : "no visible", visible(pestana("INFORMACIÓN PERSONAL")));
            verificar("Pestaña Perfil publico", "visible", visible(pestana("PERFIL PÚBLICO")) ? "visible" : "no visible", visible(pestana("PERFIL PÚBLICO")));
            verificar("Muestra el email del usuario", emailCuenta, textoPagina().contains(emailCuenta) ? emailCuenta : "no aparece", textoPagina().contains(emailCuenta));

        } else if (accion.equals("ver")) {
            String nombre = valorDe("fullName");
            String email = valorDe("email");
            verificar("Nombre del registro", nombreCuenta, nombre, nombre.equals(nombreCuenta));
            verificar("Email del registro", emailCuenta, email, email.equals(emailCuenta));
            verificar("Cabecera muestra el nombre", nombreCuenta, textoPagina().contains(nombreCuenta) ? nombreCuenta : "no aparece", textoPagina().contains(nombreCuenta));

        } else if (accion.equals("emailBloqueado")) {
            WebElement email = driver.findElement(By.id("email"));
            boolean bloqueado = !email.isEnabled() || email.getAttribute("readonly") != null;
            verificar("Campo email bloqueado", "deshabilitado o solo lectura", bloqueado ? "bloqueado" : "editable", bloqueado);
            if (!bloqueado) {
                escribir(By.id("email"), valor);
            }
            verificar("El email no cambia", emailCuenta, valorDe("email"), valorDe("email").equals(emailCuenta));

        } else if (accion.equals("guardar")) {
            abrirPestanaDe(campo);
            escribir(By.id(campo), valor);
            guardar(campo);
            String aviso = esperarAviso();
            String errores = errores(campo);
            verificar("Mensaje de guardado", "aviso '¡Perfil actualizado!'", aviso.isBlank() ? "ningun aviso" : aviso, aviso.contains("actualizado"));
            verificar("Sin errores", "ningun error", errores.isBlank() ? "ningun error" : errores, errores.isBlank());

            recargar(campo);
            String guardado = esperarValor(campo, valor);
            verificar("Persistencia al recargar", valor, guardado, guardado.equals(valor));
            if (campo.equals("fullName") || campo.equals("professionalTitle")) {
                verificar("Cabecera del perfil muestra el dato", valor, textoPagina().contains(valor) ? valor : "no aparece", textoPagina().contains(valor));
            }

        } else if (accion.equals("rechazar")) {
            abrirPestanaDe(campo);
            String valorAntes = valorDe(campo);
            escribir(By.id(campo), valor);
            guardar(campo);
            String aviso = esperarAviso();
            String errores = (driver.findElement(By.id(campo)).getDomProperty("validationMessage") + " " + errores(campo)
                    + " " + aviso.replaceAll(".*actualizado.*", "")).trim();
            verificar("Sin mensaje de guardado", "no aparece '¡Perfil actualizado!'", aviso.isBlank() ? "ningun aviso" : aviso, !aviso.contains("actualizado"));
            verificar("Mensaje de error visible", "un texto que explique el error", errores.isBlank() ? "ningun mensaje" : errores, !errores.isBlank());

            recargar(campo);
            String guardado = valorDe(campo);
            verificar("No guarda el dato invalido", "'" + valorAntes + "' (el valor anterior)", "'" + guardado + "'", !guardado.equals(valor));

        } else if (accion.equals("largo")) {
            abrirPestanaDe(campo);
            int cantidad = Integer.parseInt(valor);
            escribir(By.id(campo), "x".repeat(cantidad));
            int largo = valorDe(campo).length();
            String contador = textos(By.xpath("//*[contains(text(), 'caracteres restantes')]"));
            caso.dato = campo + " = " + cantidad + " letras 'x'";
            verificar("Limite de 500 caracteres", "maximo 500", largo + " caracteres", largo <= 500);
            verificar("Contador no negativo", "0 caracteres restantes o mas", contador, !contador.contains("-"));

        } else if (accion.equals("sinGuardar")) {
            abrirPestanaDe(campo);
            String valorAntes = valorDe(campo);
            escribir(By.id(campo), valor);
            recargar(campo); // se recarga SIN pulsar guardar
            String despues = valorDe(campo);
            verificar("El cambio sin guardar se descarta", "'" + valorAntes + "'", "'" + despues + "'", despues.equals(valorAntes));
        }

        System.out.println(id + " " + descripcion + " -> " + (caso.paso() ? "PASO" : "FALLO"));
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

    // ---------- Pasos en la pagina ----------

    // Inicia sesion con la cuenta creada en @BeforeAll y espera el dashboard
    private void iniciarSesion() {
        driver.findElement(By.xpath("//button[normalize-space()='Ingresar']")).click();
        escribir(By.id("email"), emailCuenta);
        escribir(By.id("password"), contrasenaCuenta);
        // A veces el primer clic en "Entrar" no hace nada: se reintenta hasta 3 veces
        for (int intento = 1; intento <= 3; intento++) {
            clicCentrado(By.xpath("//button[normalize-space()='Entrar']"));
            try {
                new WebDriverWait(driver, segundos("espera.respuesta")).until(ExpectedConditions.urlContains("/dashboard"));
                return;
            } catch (TimeoutException e) {
                // sigue en /auth: se vuelve a intentar
            }
        }
        throw new AssertionError("No se pudo iniciar sesion con " + emailCuenta);
    }

    // Espera a que el perfil cargue los datos del usuario (el email aparece lleno)
    private void esperarDatosCargados() {
        new WebDriverWait(driver, segundos("espera.larga")).until(ExpectedConditions.attributeToBeNotEmpty(driver.findElement(By.id("email")), "value"));
    }

    // Abre la pestaña donde esta el campo (Perfil publico o Informacion personal)
    private void abrirPestanaDe(String campo) {
        if (CAMPOS_PUBLICOS.contains(campo)) {
            clicCentrado(pestana("PERFIL PÚBLICO"));
        } else {
            clicCentrado(pestana("INFORMACIÓN PERSONAL"));
        }
        driver.findElement(By.id(campo)); // espera a que el campo aparezca
    }

    // Pulsa el boton de guardar de la pestaña del campo
    private void guardar(String campo) {
        if (CAMPOS_PUBLICOS.contains(campo)) {
            clicCentrado(By.xpath("//button[normalize-space()='Guardar perfil público']"));
        } else {
            clicCentrado(By.xpath("//button[normalize-space()='Guardar cambios']"));
        }
    }

    // Recarga la pagina, espera los datos y vuelve a la pestaña del campo
    private void recargar(String campo) {
        driver.navigate().refresh();
        esperarDatosCargados();
        abrirPestanaDe(campo);
    }

    // Boton de una pestaña. Se compara en minusculas porque la pagina solo las MUESTRA en mayusculas.
    private By pestana(String texto) {
        return By.xpath("//button[translate(normalize-space(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚ', 'abcdefghijklmnopqrstuvwxyzáéíóú')='"
                + texto.toLowerCase() + "']");
    }

    // Espera (espera.respuesta) a que aparezca un aviso y devuelve su texto ("" si no aparece)
    private String esperarAviso() {
        By aviso = By.cssSelector("li[role='status'][data-state='open']");
        try {
            new WebDriverWait(driver, segundos("espera.respuesta")).until(ExpectedConditions.visibilityOfElementLocated(aviso));
        } catch (TimeoutException e) {
            return "";
        }
        return textos(aviso);
    }

    // Espera (espera.corta) a que el campo tenga el valor esperado; devuelve el valor que tenga al final
    private String esperarValor(String campo, String esperado) {
        try {
            new WebDriverWait(driver, segundos("espera.corta")).until(ExpectedConditions.attributeToBe(By.id(campo), "value", esperado));
        } catch (TimeoutException e) {
            // Se devuelve lo que haya para mostrarlo en el reporte
        }
        return valorDe(campo);
    }

    private String valorDe(String campo) {
        return driver.findElement(By.id(campo)).getDomProperty("value");
    }

    // Textos de error en rojo dentro de la pagina, mas los campos que el navegador marca invalidos
    private String errores(String campo) {
        String rojos = textos(By.cssSelector("main p[class*='text-red'], main .text-destructive"));
        String invalidos = driver.findElements(By.cssSelector("#" + campo + ":invalid")).isEmpty() ? "" : campo + " invalido";
        return (rojos + " " + invalidos).trim();
    }

    private String textoPagina() {
        return driver.findElement(By.tagName("main")).getText();
    }

    private boolean visible(By localizador) {
        return !textos(localizador).isBlank();
    }

    // Une el texto de todos los elementos encontrados (sin esperar si no hay ninguno)
    private String textos(By localizador) {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        String texto = "";
        try {
            for (WebElement e : driver.findElements(localizador)) {
                texto += e.getText().trim().replace("\n", " ") + " ";
            }
        } catch (StaleElementReferenceException e) {
            // La pagina cambio mientras se leia; se usa lo que se alcanzo a leer
        }
        driver.manage().timeouts().implicitlyWait(segundos("espera.larga"));
        return texto.trim();
    }

    private void escribir(By localizador, String texto) {
        clicCentrado(localizador); // centra el campo y hace clic (evita que algo lo tape)
        WebElement campo = driver.findElement(localizador);
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

    // ---------- Datos ----------

    // Lee un tiempo de espera (en segundos) de datos.properties
    private static Duration segundos(String clave) {
        return Duration.ofSeconds(Long.parseLong(leer(clave)));
    }

    private static String leer(String clave) {
        String valor = datos.getProperty(clave);
        assertNotNull(valor, "Falta la clave '" + clave + "' en datos.properties");
        return valor.trim();
    }

    // Lee datos.properties en UTF-8 desde el classpath (src/test/resources)
    private static Properties cargarDatos() throws IOException {
        Properties props = new Properties();
        InputStream entrada = PerfilTest.class.getClassLoader().getResourceAsStream("datos.properties");
        try (InputStreamReader lector = new InputStreamReader(entrada, StandardCharsets.UTF_8)) {
            props.load(lector);
        }
        return props;
    }

    // Lee la linea "Corridas: N" de lee_me.txt, le suma 1 y la guarda de nuevo.
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
        parrafo(doc, "Reporte de pruebas - Perfil de usuario", 18, true);
        parrafo(doc, "Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 11, false);
        parrafo(doc, "URL: " + URL, 11, false);
        parrafo(doc, "Corrida (lee_me.txt): " + ultimaCorrida + "   Cuenta usada: " + emailCuenta, 11, false);
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

        Files.createDirectories(Paths.get("reportes de cada modulo", "Perfil"));
        String nombre = "reportes de cada modulo/Perfil/Reporte_Perfil_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".docx";
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
