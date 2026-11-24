package com.achron.pages;

import com.achron.core.Acciones;
import org.openqa.selenium.By;

/**
 * Pestañas Metodologías, Métodos y Rutinas de la biblioteca (/exercises).
 * tipo: "Metodologías", "Métodos", "Bloque" o "Rutina" (los dos ultimos estan en la pestaña Rutinas).
 */
public class BibliotecaPage {

    private static final By PESTANA_ACTIVA = By.cssSelector("[role='tabpanel'][data-state='active']");
    private static final String DIALOGO = "[role='dialog'][data-state='open'] ";

    private final Acciones web;

    public BibliotecaPage(Acciones web) {
        this.web = web;
    }

    public void abrirPestana(String tipo) {
        String pestana = tipo.equals("Bloque") || tipo.equals("Rutina") ? "Rutinas" : tipo;
        By boton = By.xpath("//*[@role='tab'][normalize-space()='" + pestana + "']");
        web.clic(boton);
        web.esperarQue("corta", d -> "active".equals(d.findElement(boton).getAttribute("data-state")));
        web.encontrar(PESTANA_ACTIVA);
    }

    public void abrirFormulario(String tipo) {
        String texto;
        switch (tipo) {
            case "Metodologías": texto = "Crear Metodología"; break;
            case "Métodos": texto = "Crear método"; break;
            case "Bloque": texto = "Crear bloque"; break;
            default: texto = "Crear rutina";
        }
        web.clicVisible(By.xpath("//main//button[normalize-space()='" + texto + "']"));
        web.encontrar(Acciones.DIALOGO);
    }

    public By campoNombre(String tipo) {
        switch (tipo) {
            case "Metodologías": return By.cssSelector(DIALOGO + "#name");
            case "Métodos": return By.cssSelector(DIALOGO + "#label");
            case "Bloque": return By.cssSelector(DIALOGO + "input");
            default: return By.cssSelector(DIALOGO + "input[placeholder^='Ej: PUSH']");
        }
    }

    public void guardar(String tipo) {
        web.clic(botonGuardar(tipo));
    }

    /** Algunos formularios dejan el boton de guardar deshabilitado mientras falte el nombre. */
    public boolean guardarHabilitado(String tipo) {
        return web.encontrar(botonGuardar(tipo)).isEnabled();
    }

    private static By botonGuardar(String tipo) {
        String texto;
        switch (tipo) {
            case "Metodologías": texto = "Guardar Metodología"; break;
            case "Métodos": texto = "Guardar método"; break;
            case "Bloque": texto = "Crear bloque"; break;
            default: texto = "Guardar rutina maestra";
        }
        return By.xpath("//*[@role='dialog'][@data-state='open']//button[normalize-space()='" + texto + "']");
    }

    /** La rutina pide al menos un bloque de estructura antes de guardar. */
    public void agregarBloqueDeEstructura() {
        web.clic(By.xpath("//*[@role='dialog'][@data-state='open']//button[normalize-space()='Agregar bloque']"));
    }

    /** En el bloque de la rutina: abre "Importar movimiento" y elige el ejercicio. */
    public void importarEjercicio(String ejercicio) {
        web.clic(By.xpath("(//*[@role='dialog'][@data-state='open']//button[@aria-haspopup='dialog'])[1]"));
        web.clic(By.xpath("//*[@role='dialog'][.//h2[normalize-space()='Importar movimiento']]//span[normalize-space()='" + ejercicio + "']"));
    }

    /** Buscador de la pestaña activa ("Buscar metodología...", "Buscar método...", "Buscar rutina..."). */
    public void buscar(String texto) {
        web.escribir(By.cssSelector("main input[placeholder^='Buscar']"), texto);
    }

    public int tarjetas() {
        return web.todos(By.cssSelector("[role='tabpanel'][data-state='active'] .gorilla-card")).size();
    }

    /** true si la tarjeta con ese titulo muestra el icono de borrar. Solo lo mira: nunca lo pulsa. */
    public boolean ofreceBorrar(String titulo) {
        return web.existe(By.xpath("//*[@role='tabpanel'][@data-state='active']//h3[normalize-space()='" + titulo + "']"
                + "/ancestor::div[contains(@class, 'relative')][1]//*[name()='svg' and contains(@class, 'lucide-trash')]"));
    }

    public String textoPestana() {
        return web.texto(PESTANA_ACTIVA);
    }

    public boolean esperarEnPestana(String texto) {
        return web.esperarQue("respuesta", d -> textoPestana().contains(texto));
    }

    public void verPrimerDetalle() {
        web.clicVisible(By.xpath("//*[@role='tabpanel'][@data-state='active']//*[normalize-space(text())='Ver detalles']"));
    }
}
