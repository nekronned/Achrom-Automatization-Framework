package com.achron.pages;

import com.achron.config.Configuracion;
import com.achron.core.Acciones;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Paginas a las que se entra escribiendo la direccion: accesos (/login, /client-login), 404 y constructor de rutinas. */
public class RutasPage {

    private static final By CORREO = By.cssSelector("input[type='email']");
    private static final By CLAVE = By.cssSelector("input[type='password']");
    private static final By NOMBRE_RUTINA = By.cssSelector("main input[name='name']");
    private static final By GUARDAR_RUTINA = By.xpath("//main//button[contains(normalize-space(), 'Guardar rutina')]");

    private final Acciones web;

    public RutasPage(Acciones web) {
        this.web = web;
    }

    /** La ruta en la que quedo el navegador, sin el dominio ni los parametros ("/auth", "/client-dashboard"). */
    public String rutaActual() {
        String url = web.url().replace(Configuracion.url(), "/");
        int parametros = url.indexOf('?');
        return parametros < 0 ? url : url.substring(0, parametros);
    }

    /** Abre la ruta y espera a que el sitio termine en la esperada; devuelve en cual quedo. */
    public String abrirYEsperar(String ruta, String esperada) {
        web.irA(ruta);
        web.esperarQue("respuesta", d -> rutaActual().equals(esperada));
        return rutaActual();
    }

    public void entrar(String correo, String clave) {
        web.escribir(CORREO, correo);
        web.escribir(CLAVE, clave);
        web.encontrar(CLAVE).sendKeys(Keys.ENTER);
    }

    /** El mensaje despues de entrar: /login avisa con una alerta del navegador (se acepta) y /client-login con un aviso del sitio. */
    public String avisoDeAcceso() {
        boolean alerta = web.esperarQue("corta", d -> ExpectedConditions.alertIsPresent().apply(d) != null);
        if (!alerta) {
            return web.esperarAviso();
        }
        Alert aviso = web.driver().switchTo().alert();
        String texto = aviso.getText();
        aviso.accept();
        return texto;
    }

    public void nombreDeRutina(String nombre) {
        web.escribir(NOMBRE_RUTINA, nombre);
    }

    public void agregarBloque() {
        web.clic(By.xpath("//main//button[normalize-space()='Agregar bloque']"));
    }

    public boolean guardarRutinaHabilitado() {
        return web.encontrar(GUARDAR_RUTINA).isEnabled();
    }

    public void guardarRutina() {
        web.clic(GUARDAR_RUTINA);
    }
}
