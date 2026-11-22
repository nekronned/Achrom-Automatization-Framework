package com.achron.pages;

import com.achron.core.Acciones;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Biblioteca de entrenamiento, pestaña "Movimientos" (/exercises). */
public class EjerciciosPage {

    public static final By BUSCADOR = By.cssSelector("main input[placeholder='Buscar ejercicio...']");
    private static final By NOMBRES = By.cssSelector("main h3");
    private static final By TARJETAS = By.cssSelector("main .gorilla-card");
    private static final By CREAR = By.xpath("//button[normalize-space()='Crear Ejercicio']");
    private static final By GUARDAR_EDICION = By.cssSelector("[role='dialog'][data-state='open'] button[type='submit']");

    private final Acciones web;

    public EjerciciosPage(Acciones web) {
        this.web = web;
    }

    public void esperarLista() {
        web.encontrar(NOMBRES);
    }

    public void recargar() {
        web.recargar();
        esperarLista();
    }

    public void buscar(String texto) {
        web.escribir(BUSCADOR, texto);
    }

    /** Espera a que la lista cumpla la condicion despues de buscar o filtrar. */
    public List<String> nombresCuando(java.util.function.Predicate<List<String>> condicion) {
        web.esperarQue("corta", d -> condicion.test(nombres()));
        return nombres();
    }

    public List<String> nombres() {
        List<String> nombres = new ArrayList<>();
        for (WebElement h : web.todos(NOMBRES)) {
            nombres.add(h.getText().trim());
        }
        return nombres;
    }

    public List<WebElement> tarjetas() {
        return web.todos(TARJETAS);
    }

    public void filtrarPorCategoria(String categoria) {
        web.clic(By.cssSelector("main [role='combobox']"));
        web.clic(By.xpath("//*[@role='option'][normalize-space()='" + categoria + "']"));
    }

    public int cuantosSeLlaman(String nombre) {
        return (int) nombres().stream().filter(n -> n.equals(nombre.trim())).count();
    }

    public boolean apareceEnLista(String nombre) {
        return web.esperarQue("respuesta", d -> cuantosSeLlaman(nombre) > 0);
    }

    public void abrirCrear() {
        web.clic(CREAR);
        web.encontrar(By.id("name"));
    }

    public void llenarCreacion(String nombre, String musculo, String equipo) {
        web.escribir(By.id("name"), nombre);
        web.escribir(By.id("muscle"), musculo);
        web.escribir(By.id("equip"), equipo);
    }

    public void botonDelDialogo(String texto) {
        web.clic(By.xpath("//*[@role='dialog'][@data-state='open']//button[normalize-space()='" + texto + "']"));
    }

    public void abrirEditar(String nombre) {
        web.clic(botonDeTarjeta(nombre, "Editar"));
        web.encontrar(By.id("edit-name"));
    }

    public void guardarEdicion() {
        web.clic(GUARDAR_EDICION);
    }

    public String textoBotonGuardarEdicion() {
        return web.texto(GUARDAR_EDICION);
    }

    public WebElement tarjeta(String nombre) {
        return web.encontrar(By.xpath("//main//h3[normalize-space()='" + nombre + "']/ancestor::div[contains(@class,'gorilla-card')]"));
    }

    public void verDetalles(String nombre) {
        web.clic(botonDeTarjeta(nombre, "Detalles"));
    }

    private static By botonDeTarjeta(String nombre, String boton) {
        return By.xpath("//main//h3[normalize-space()='" + nombre + "']/ancestor::div[contains(@class,'gorilla-card')]//button[normalize-space()='" + boton + "']");
    }
}
