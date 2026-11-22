package com.achron.pages;

import com.achron.core.Acciones;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

/** Pagina publica de inicio (lo que ve cualquier visitante). */
public class InicioPublicoPage {

    public static final String TITULO = "App inteligente para coaches";
    public static final By CONTACTO_NOMBRE = By.cssSelector("#contact input[placeholder='Tu nombre']");
    public static final By CONTACTO_EMAIL = By.cssSelector("#contact input[type='email']");
    public static final By CONTACTO_MENSAJE = By.cssSelector("#contact textarea");
    public static final By ERRORES_CONTACTO = By.cssSelector("#contact p[class*='text-red'], #contact .text-destructive");
    private static final By ENVIAR_CONTACTO = By.xpath("//button[normalize-space()='Enviar mensaje']");

    private final Acciones web;

    public InicioPublicoPage(Acciones web) {
        this.web = web;
    }

    public void abrir() {
        web.irA("");
        web.encontrar(By.xpath("//*[contains(text(), '" + TITULO + "')]"));
    }

    public boolean existeSeccion(String id) {
        return web.existe(By.id(id));
    }

    public void irAAncla(String texto) {
        web.clicVisible(By.xpath("//a[normalize-space()='" + texto + "' and starts-with(@href,'#')]"));
    }

    /** Distancia en px entre la parte de arriba de la ventana y la seccion. */
    public double posicionDeSeccion(String id) {
        Number arriba = (Number) ((JavascriptExecutor) web.driver()).executeScript(
                "var s = document.getElementById(arguments[0]); return s ? s.getBoundingClientRect().top : 99999;", id);
        return arriba.doubleValue();
    }

    /** true si la seccion se ve en la pantalla (su parte de arriba queda en la mitad superior de la ventana). */
    public boolean seccionEnPantalla(String id) {
        return (Boolean) ((JavascriptExecutor) web.driver()).executeScript(
                "var s = document.getElementById(arguments[0]); if (!s) return false;"
                        + "var r = s.getBoundingClientRect(); return r.top > -50 && r.top < window.innerHeight * 0.6 && r.bottom > 0;", id);
    }

    public void botonOEnlace(String texto) {
        web.clicVisible(By.xpath("//a[normalize-space()='" + texto + "'] | //button[normalize-space()='" + texto + "']"));
    }

    public WebElement enlaceDelPie(String texto) {
        return web.encontrar(By.xpath("//footer//a[normalize-space()='" + texto + "']"));
    }

    public void enviarContacto() {
        web.clic(ENVIAR_CONTACTO);
    }

    public void cambiarIdioma(String opcion) {
        web.clicVisible(By.xpath("//button[normalize-space()='ES' or normalize-space()='es']"));
        web.clic(By.xpath("//*[@role='menuitem'][normalize-space()='" + opcion + "']"));
    }
}
