package com.achron.pages;

import com.achron.core.Acciones;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

/** Partes comunes de Asesorados, Cartera, Coaches, Objetivos, Chat, Asistente IA y Precios. */
public class SeccionesPage {

    public static final By BUSCADOR = By.cssSelector("main input[placeholder^='Buscar']");
    public static final By OPCIONES = By.cssSelector("[role='option']");
    private static final By FILAS_TABLA = By.cssSelector("main table tbody tr");

    private final Acciones web;

    public SeccionesPage(Acciones web) {
        this.web = web;
    }

    public void abrir(String ruta) {
        web.irA(ruta);
        web.esperarUrl("/" + ruta);
        web.encontrar(By.tagName("main"));
    }

    public void buscar(String texto) {
        web.escribir(BUSCADOR, texto);
    }

    public void abrirFiltro(String textoActual) {
        web.clicVisible(By.xpath("//main//*[@role='combobox'][normalize-space()='" + textoActual + "']"));
        web.aparece(OPCIONES);
    }

    public void boton(String texto) {
        web.clicVisible(By.xpath("//main//button[contains(normalize-space(), '" + texto + "')] | //main//a[contains(normalize-space(), '" + texto + "')]"));
    }

    public int filasTabla() {
        return web.todos(FILAS_TABLA).size();
    }

    /** Abre el menu de acciones (...) de una fila de la tabla de coaches y elige una opcion. */
    public void accionDeFila(int fila, String opcion) {
        web.clic(By.xpath("(//main//table/tbody/tr)[" + fila + "]//button"));
        web.clic(By.xpath("//*[@role='menuitem'][normalize-space()='" + opcion + "']"));
    }

    public String filaTexto(int fila) {
        return web.texto(By.xpath("(//main//table/tbody/tr)[" + fila + "]"));
    }

    public double scrollVertical() {
        return ((Number) ((JavascriptExecutor) web.driver()).executeScript("return window.scrollY;")).doubleValue();
    }
}
