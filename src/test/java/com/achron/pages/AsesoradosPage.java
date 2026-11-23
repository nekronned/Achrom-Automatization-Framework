package com.achron.pages;

import com.achron.core.Acciones;
import org.openqa.selenium.By;

/** Lista de asesorados (/clients), detalle de un asesorado y sus ventanas de gestion. */
public class AsesoradosPage {

    private static final By FECHA = By.cssSelector("[role='dialog'][data-state='open'] input[type='date']");
    private static final By HORA = By.cssSelector("[role='dialog'][data-state='open'] input[type='time']");
    private static final By NOTAS = By.cssSelector("[role='dialog'][data-state='open'] textarea");

    private final Acciones web;

    public AsesoradosPage(Acciones web) {
        this.web = web;
    }

    public void abrirLista() {
        web.irA("clients");
        web.esperarUrl("/clients");
    }

    public String fila(String nombre) {
        return web.texto(filaDe(nombre));
    }

    public boolean apareceEnLista(String nombre) {
        return web.esperarQue("respuesta", d -> !fila(nombre).isEmpty());
    }

    /** Menu (...) de la fila del asesorado > "Ver perfil completo". */
    public void verPerfilCompleto(String nombre) {
        web.clic(By.xpath("//main//tr[.//*[contains(normalize-space(), '" + nombre + "')]]//button"));
        web.clic(By.xpath("//*[@role='menuitem'][normalize-space()='Ver perfil completo']"));
        web.esperarTexto("Perfil de " + nombre);
    }

    public void boton(String texto) {
        web.clic(By.xpath("//main//button[normalize-space()='" + texto + "']"));
    }

    public void botonDelDialogo(String texto) {
        web.clic(By.xpath("//*[@role='dialog'][@data-state='open']//button[normalize-space()='" + texto + "']"));
    }

    public boolean botonDelDialogoHabilitado(String texto) {
        return web.encontrar(By.xpath("//*[@role='dialog'][@data-state='open']//button[normalize-space()='" + texto + "']")).isEnabled();
    }

    /** fecha en formato yyyy-MM-dd y hora HH:mm, como los pide el campo del navegador. */
    public void llenarSesion(String fecha, String hora, String notas) {
        if (!fecha.isEmpty()) web.ponerValor(FECHA, fecha);
        if (!hora.isEmpty()) web.ponerValor(HORA, hora);
        web.escribir(NOTAS, notas);
    }

    public void elegirMetodologia(String nombre) {
        web.clic(By.xpath("//*[@role='dialog'][@data-state='open']//*[@role='combobox']"));
        web.clic(By.xpath("//*[@role='option'][normalize-space()='" + nombre + "']"));
    }

    public String textoDialogo() {
        return web.texto(Acciones.DIALOGO);
    }

    /** En la biblioteca en modo asignacion: boton "Agregar" de la tarjeta del ejercicio. */
    public void agregarEjercicio(String ejercicio) {
        web.clic(By.xpath("//main//h3[normalize-space()='" + ejercicio + "']/ancestor::div[contains(@class,'gorilla-card')]//button[normalize-space()='Agregar']"));
    }

    private static By filaDe(String nombre) {
        return By.xpath("//main//tr[.//*[contains(normalize-space(), '" + nombre + "')]]");
    }
}
