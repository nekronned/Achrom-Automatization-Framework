package com.achron.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.achron.config.Configuracion;
import com.achron.reporting.Caso;
import com.achron.reporting.SeguimientoCasos;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.openqa.selenium.WebDriver;

/**
 * Base de todas las clases de prueba: abre un navegador por caso y lo cierra al final.
 * SeguimientoCasos se encarga de la captura, el log, Allure y los reportes.
 */
@ExtendWith(SeguimientoCasos.class)
public abstract class BaseTest {

    protected WebDriver driver;
    protected Acciones web;
    protected Caso caso;

    @BeforeEach
    void abrirNavegador() {
        driver = DriverFactory.crear();
        web = new Acciones(driver);
    }

    @AfterEach
    void cerrarNavegador() {
        if (driver != null && !Configuracion.siNo("navegador.dejarAbierto")) {
            driver.quit();
        }
    }

    /** Crea el caso que se va a llenar con comprobaciones. */
    protected Caso nuevoCaso(String id, String categoria, String descripcion, String severidad, String dato) {
        Modulo modulo = getClass().getAnnotation(Modulo.class);
        caso = new Caso(id, modulo.nombre(), categoria, descripcion, severidad, dato);
        return caso;
    }

    protected void verificar(String que, String esperado, String obtenido, boolean ok) {
        caso.verificar(que, esperado, obtenido, ok);
    }

    /** Hace fallar el caso en JUnit con todas las comprobaciones que no se cumplieron. */
    protected void comprobarTodo() {
        Stream<Executable> revisiones = caso.comprobaciones().stream()
                .map(c -> () -> assertTrue(c.ok, caso.id() + " | " + c.que + " -> esperado: " + c.esperado + " | obtenido: " + c.obtenido));
        assertAll(caso.id() + " " + caso.descripcion(), revisiones);
    }

    /** "ejercicio.nombre" se busca en datos.properties; cualquier otro valor se usa tal cual. */
    protected static String dato(String valor, String prefijo) {
        return valor.startsWith(prefijo + ".") ? Configuracion.texto(valor) : valor;
    }

    /** Revisa que la pagina no muestre claves internas sin traducir (ej: "coaches_page.subtitle"). */
    protected void verificarSinClaves() {
        String claves = web.clavesSinTraducir();
        verificar("Sin textos sin traducir", "ninguna clave interna (ej: 'algo_page.titulo')", siVacio(claves, "ninguna"), claves.isEmpty());
    }

    /** Para mostrar en el reporte "ningun mensaje" en vez de un texto vacio. */
    protected static String siVacio(String texto, String reemplazo) {
        return texto == null || texto.isBlank() ? reemplazo : texto;
    }

    public WebDriver driver() {
        return driver;
    }

    public Caso caso() {
        return caso;
    }
}
