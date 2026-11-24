package com.achron.pages;

import com.achron.core.Acciones;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

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

    private static final By CANCELAR_SESION = By.xpath("//main//button[contains(normalize-space(), 'Cancelar esta sesión')]");

    public boolean haySesionParaCancelar() {
        return web.existe(CANCELAR_SESION);
    }

    /** "Cancelar esta sesión" pregunta con un aviso del navegador: se acepta o se rechaza. Devuelve la pregunta ("" si no salio). */
    public String cancelarSesion(boolean aceptar) {
        web.clic(CANCELAR_SESION);
        if (!web.esperarQue("corta", d -> ExpectedConditions.alertIsPresent().apply(d) != null)) {
            return "";
        }
        Alert aviso = web.driver().switchTo().alert();
        String pregunta = aviso.getText();
        if (aceptar) aviso.accept(); else aviso.dismiss();
        return pregunta;
    }

    /** Elige una opcion en la lista desplegable numero n (desde 1) de la pagina. */
    public void filtrar(int n, String opcion) {
        web.clic(By.xpath("(//main//*[@role='combobox'])[" + n + "]"));
        web.clic(By.xpath("//*[@role='option'][normalize-space()='" + opcion + "']"));
    }

    /** En la ventana "Añadir a rutina": crea un entrenamiento nuevo con ese nombre y continua. */
    public void crearEntrenamiento(String nombre) {
        web.clic(By.xpath("//*[@role='dialog'][@data-state='open']//button[contains(normalize-space(), 'Crear entrenamiento nuevo')]"));
        web.escribir(By.cssSelector("[role='dialog'][data-state='open'] input[placeholder^='Ej: WOD']"), nombre);
        botonDelDialogo("Continuar");
    }

    private static By filaDe(String nombre) {
        return By.xpath("//main//tr[.//*[contains(normalize-space(), '" + nombre + "')]]");
    }
}
