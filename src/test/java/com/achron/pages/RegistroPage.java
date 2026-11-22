package com.achron.pages;

import com.achron.core.Acciones;
import java.util.List;
import org.openqa.selenium.By;

/** Formulario "Nuevo Registro" (se llega desde Registrarse > Regístrate aquí). */
public class RegistroPage {

    public static final List<String> CAMPOS = List.of("fullName", "cedula", "email", "password", "confirmPassword");

    private static final By BOTON_REGISTRARSE = By.xpath("//button[normalize-space()='Registrarse']");
    private static final By ENLACE_REGISTRATE = By.xpath("//button[normalize-space()='Regístrate aquí']");
    private static final By NOMBRE = By.id("fullName");
    private static final By SOY_COACH = By.xpath("//button[.//span[normalize-space()='Soy coach']]");
    private static final By ENVIAR = By.cssSelector("button[type='submit']");
    private static final By REGISTRO_EXITOSO = By.xpath("//*[contains(text(), 'Registro exitoso')]");

    private final Acciones web;

    public RegistroPage(Acciones web) {
        this.web = web;
    }

    public void abrir() {
        web.irA("");
        web.clic(BOTON_REGISTRARSE);
        // a veces el primer clic llega antes de que la pantalla de acceso este lista
        for (int intento = 1; intento <= 3 && !web.aparece(NOMBRE); intento++) {
            web.clic(ENLACE_REGISTRATE);
        }
    }

    public void llenar(String nombre, String nit, String email, String clave) {
        escribir("fullName", nombre);
        escribir("cedula", nit);
        escribir("email", email);
        escribir("password", clave);
        escribir("confirmPassword", clave);
    }

    public void escribir(String campo, String valor) {
        web.escribir(By.id(campo), valor);
    }

    public String valor(String campo) {
        return web.valor(By.id(campo));
    }

    public String mensajeNavegador(String campo) {
        return web.mensajeNavegador(By.id(campo));
    }

    public void enviarComoCoach() {
        web.clic(SOY_COACH);
        web.clic(ENVIAR);
    }

    /** true si sale "Registro exitoso" o la pagina pasa al panel. */
    public boolean seRegistro() {
        return web.esperarQue("respuesta", d -> d.getCurrentUrl().contains("/dashboard")
                || d.findElements(REGISTRO_EXITOSO).stream().anyMatch(e -> e.isDisplayed()));
    }

    public boolean registroExitosoVisible() {
        return !web.texto(REGISTRO_EXITOSO).isEmpty();
    }

    /** Texto rojo que el sitio pone debajo del campo. */
    public String errorDe(String campo) {
        return web.texto(By.xpath("//input[@id='" + campo + "']/ancestor::div[contains(@class,'relative')][1]/following-sibling::p"));
    }
}
