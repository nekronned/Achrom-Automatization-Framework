package com.achron.pages;

import com.achron.core.Acciones;
import org.openqa.selenium.By;

/** Menu lateral del coach: secciones, notificaciones, tema, idioma y cerrar sesion. */
public class MenuLateral {

    private static final By CAMPANA = By.cssSelector("button:has(svg.lucide-bell)");
    private static final By TEMA = By.cssSelector("button:has(svg.lucide-sun), button:has(svg.lucide-moon)");
    private static final By IDIOMA = By.cssSelector("button:has(svg.lucide-globe)");
    private static final By SALIR = By.cssSelector("button:has(svg.lucide-log-out)");
    private static final By MENU_MOVIL = By.cssSelector("button:has(svg.lucide-menu)");
    public static final By PANEL_NOTIFICACIONES = By.cssSelector("[data-radix-popper-content-wrapper], [role='dialog'][data-state='open']");

    private final Acciones web;

    public MenuLateral(Acciones web) {
        this.web = web;
    }

    /** Clic en el enlace del menu (ruta sin "/", por ejemplo "wallet") y espera la pagina. */
    public boolean ir(String ruta) {
        web.clicVisible(By.cssSelector("a[href='/" + ruta + "']"));
        return web.esperarUrl("/" + ruta);
    }

    public void abrirNotificaciones() {
        web.clicVisible(CAMPANA);
    }

    public void cambiarTema() {
        web.clicVisible(TEMA);
    }

    public void cambiarIdioma(String opcion) {
        web.clicVisible(IDIOMA);
        web.clic(By.xpath("//*[@role='menuitem'][normalize-space()='" + opcion + "']"));
    }

    public void cerrarSesion() {
        web.clicVisible(SALIR);
    }

    public void abrirMenuMovil() {
        web.clicVisible(MENU_MOVIL);
    }

    public String claseDelTema() {
        String clase = web.encontrar(By.tagName("html")).getAttribute("class");
        return clase == null ? "" : clase;
    }
}
