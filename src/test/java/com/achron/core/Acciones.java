package com.achron.core;

import com.achron.config.Configuracion;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Lo que se hace con la pagina: buscar, hacer clic, escribir y esperar.
 * Solo usa esperas explicitas (WebDriverWait); el driver no tiene espera implicita.
 */
public class Acciones {

    /** Aviso emergente (toast) que el sitio muestra despues de guardar o cuando hay un error. */
    public static final By AVISO = By.cssSelector("li[role='status'][data-state='open'], li[data-sonner-toast]");
    /** Ventana (dialogo) abierta. */
    public static final By DIALOGO = By.cssSelector("[role='dialog'][data-state='open']");
    /** Textos de error en rojo dentro de una ventana. */
    public static final By ERRORES_DIALOGO = By.cssSelector("[role='dialog'] p[class*='text-red'], [role='dialog'] .text-destructive");

    private static final Pattern CLAVE_SIN_TRADUCIR =
            Pattern.compile("\\b[a-z]+_[a-z_]+\\.[a-z_]+\\b|\\b[a-z]+\\.[a-z]+_[a-z_]+\\b");

    private final WebDriver driver;

    public Acciones(WebDriver driver) {
        this.driver = driver;
    }

    public WebDriver driver() {
        return driver;
    }

    // ----- navegar -----

    public void irA(String ruta) {
        driver.get(Configuracion.url() + ruta);
    }

    public String url() {
        return driver.getCurrentUrl();
    }

    public void recargar() {
        driver.navigate().refresh();
    }

    // ----- buscar -----

    public WebElement encontrar(By localizador) {
        return espera("larga").until(ExpectedConditions.presenceOfElementLocated(localizador));
    }

    /** Todos los que haya en este momento, sin esperar. */
    public List<WebElement> todos(By localizador) {
        return driver.findElements(localizador);
    }

    public boolean existe(By localizador) {
        return !todos(localizador).isEmpty();
    }

    // ----- actuar -----

    public void clic(By localizador) {
        clic(espera("larga").until(ExpectedConditions.elementToBeClickable(localizador)));
    }

    /** Clic en el primero que se vea (el menu lateral, por ejemplo, existe dos veces: movil y escritorio). */
    public void clicVisible(By localizador) {
        encontrar(localizador);
        for (WebElement elemento : todos(localizador)) {
            if (elemento.isDisplayed()) {
                clic(elemento);
                return;
            }
        }
        throw new NoSuchElementException("No hay ningun elemento visible para " + localizador);
    }

    /** Lleva el elemento al centro de la pantalla y hace clic; si algo lo tapa, clic por JavaScript. */
    public void clic(WebElement elemento) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", elemento);
        try {
            elemento.click();
        } catch (ElementClickInterceptedException e) {
            js.executeScript("arguments[0].click();", elemento);
        }
    }

    /**
     * Borra con Ctrl+A y Retroceso y escribe. clear() no sirve en este sitio:
     * React no se entera y sigue usando el valor viejo.
     */
    public void escribir(By localizador, String texto) {
        clic(localizador);
        WebElement campo = encontrar(localizador);
        campo.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        campo.sendKeys(texto);
    }

    /**
     * Pone el valor directo en el campo y avisa a React. Se usa en fechas y horas, porque al escribirlas
     * con el teclado el formato depende del idioma del navegador.
     */
    public void ponerValor(By campo, String valor) {
        ((JavascriptExecutor) driver).executeScript(
                "var e = arguments[0]; var p = Object.getPrototypeOf(e);"
                        + "Object.getOwnPropertyDescriptor(p, 'value').set.call(e, arguments[1]);"
                        + "e.dispatchEvent(new Event('input', {bubbles: true})); e.dispatchEvent(new Event('change', {bubbles: true}));",
                encontrar(campo), valor);
    }

    public void subirArchivo(By campoArchivo, Path archivo) {
        encontrar(campoArchivo).sendKeys(archivo.toAbsolutePath().toString());
    }

    // ----- leer -----

    /** Texto de todos los elementos que haya, en una sola linea ("" si no hay ninguno). */
    public String texto(By localizador) {
        StringBuilder texto = new StringBuilder();
        try {
            for (WebElement elemento : todos(localizador)) {
                texto.append(elemento.getText().trim().replace("\n", " ")).append(' ');
            }
        } catch (StaleElementReferenceException e) {
            // la pagina cambio mientras se leia: se devuelve lo que alcanzo a leer
        }
        return texto.toString().trim();
    }

    public String textoPagina() {
        return encontrar(By.tagName("body")).getText();
    }

    public String valor(By campo) {
        return encontrar(campo).getDomProperty("value");
    }

    /** Mensaje de validacion del navegador ("Incluye un signo @...") o "" si el campo ya no esta. */
    public String mensajeNavegador(By campo) {
        try {
            List<WebElement> elementos = todos(campo);
            return elementos.isEmpty() ? "" : elementos.get(0).getDomProperty("validationMessage");
        } catch (StaleElementReferenceException e) {
            return "";
        }
    }

    /** Textos que parecen claves internas sin traducir, como "coaches_page.subtitle". */
    public String clavesSinTraducir() {
        Matcher m = CLAVE_SIN_TRADUCIR.matcher(textoPagina());
        List<String> claves = new ArrayList<>();
        while (m.find()) {
            claves.add(m.group());
        }
        return String.join(" ", claves);
    }

    // ----- esperar -----

    public WebDriverWait espera(String tipo) {
        return new WebDriverWait(driver, Configuracion.espera(tipo));
    }

    /** Espera la condicion hasta el tiempo indicado; true si se cumplio. */
    public boolean esperarQue(String tipo, Function<WebDriver, Boolean> condicion) {
        try {
            espera(tipo).ignoring(StaleElementReferenceException.class).until(condicion);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean esperarUrl(String parte) {
        return esperarQue("respuesta", d -> d.getCurrentUrl().contains(parte));
    }

    public boolean esperarTexto(String texto) {
        return esperarQue("respuesta", d -> d.findElement(By.tagName("body")).getText().contains(texto));
    }

    public boolean aparece(By localizador) {
        return esperarQue("corta", d -> d.findElements(localizador).stream().anyMatch(WebElement::isDisplayed));
    }

    public boolean desaparece(By localizador) {
        return esperarQue("corta", d -> d.findElements(localizador).stream().noneMatch(WebElement::isDisplayed));
    }

    /**
     * Espera a que salga un aviso y a que su texto deje de cambiar (el titulo y la descripcion
     * a veces llegan en momentos distintos). Devuelve "" si no salio ninguno.
     */
    public String esperarAviso() {
        if (!esperarQue("respuesta", d -> !d.findElements(AVISO).isEmpty())) {
            return "";
        }
        String[] anterior = {""};
        esperarQue("corta", d -> {
            String actual = texto(AVISO);
            boolean estable = !actual.isEmpty() && actual.equals(anterior[0]);
            anterior[0] = actual;
            return estable;
        });
        return anterior[0];
    }

    public boolean hayDialogoAbierto() {
        return existe(DIALOGO);
    }
}
