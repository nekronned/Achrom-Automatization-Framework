package dev.selenium.hello;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegistroUsuarioTest {

    // La URL queda fija en el codigo (no viene del properties)
    private static final String URL = "https://magenta-platypus-05cee2.netlify.app/";

    private WebDriver driver;
    private Properties datos;

    @BeforeEach
    void setUp() throws IOException {
        datos = cargarDatos();

        driver = new ChromeDriver();
        driver.manage().window().maximize();
        // Espera hasta 10 s a que aparezca cada elemento antes de fallar
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.get(URL);
    }

    @Test
    void registrarCoach() {
        String nombre = leer("nombre");
        String nit = leer("nit");
        String email = leer("email");
        String contrasena = leer("contrasena");

        // El formulario exige minimo 8 caracteres en la contrasena
        assertTrue(contrasena.length() >= 8,
                "La contrasena del datos.properties debe tener minimo 8 caracteres");

        driver.findElement(By.xpath("//button[normalize-space()='Register']")).click();
        driver.findElement(By.xpath("//button[normalize-space()='Regístrate aquí']")).click();

        escribir(By.id("fullName"), nombre);
        escribir(By.id("cedula"), nit);
        escribir(By.id("email"), email);
        escribir(By.id("password"), contrasena);
        escribir(By.id("confirmPassword"), contrasena);

        driver.findElement(By.xpath("//button[.//span[normalize-space()='Soy coach']]")).click();
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        // TODO: agregar aqui una validacion del resultado (mensaje de exito, cambio de pagina, etc.)
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ---------- Metodos de apoyo ----------

    private void escribir(By localizador, String texto) {
        WebElement campo = driver.findElement(localizador);
        campo.click();
        campo.clear();
        campo.sendKeys(texto);
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
}
