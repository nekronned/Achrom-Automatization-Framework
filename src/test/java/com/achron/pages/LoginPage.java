package com.achron.pages;

import com.achron.core.Acciones;
import org.openqa.selenium.By;

/** Pantalla de acceso (/auth): iniciar sesion y recuperar contraseña. */
public class LoginPage {

    public static final By EMAIL = By.id("email");
    public static final By CLAVE = By.id("password");
    private static final By ENTRAR = By.xpath("//button[normalize-space()='Entrar']");
    private static final By OLVIDE = By.xpath("//button[normalize-space()='¿Olvidaste tu contraseña?']");
    private static final By ENVIAR_CODIGO = By.xpath("//button[normalize-space()='Enviar código']");
    private static final By CANCELAR_RECUPERACION = By.xpath("//button[normalize-space()='Cancelar y volver']");
    private static final By OJO = By.xpath("//input[@id='password']/ancestor::div[contains(@class,'relative')][1]//button");
    private static final By GOOGLE = By.xpath("//button[normalize-space()='Entrar con Google']");

    private final Acciones web;

    public LoginPage(Acciones web) {
        this.web = web;
    }

    public void abrir() {
        web.irA("auth");
        // a veces el sitio entrega la pagina en blanco: se recarga una vez y queda anotado en el log
        if (!web.esperarQue("respuesta", d -> web.existe(EMAIL))) {
            com.achron.reporting.Log.get().warn("la pagina de inicio de sesion no cargo ({}); se recarga", web.url());
            web.recargar();
        }
        web.encontrar(EMAIL);
    }

    public void escribirCredenciales(String email, String clave) {
        web.escribir(EMAIL, email);
        web.escribir(CLAVE, clave);
    }

    public void entrar() {
        web.clic(ENTRAR);
    }

    /** Inicia sesion y espera el panel. Si el sitio no responde al primer clic, lo intenta 3 veces. */
    public void iniciarSesion(String email, String clave) {
        abrir();
        escribirCredenciales(email, clave);
        for (int intento = 1; intento <= 3; intento++) {
            entrar();
            if (web.esperarUrl("/dashboard")) {
                web.esperarTexto("Panel de control");
                return;
            }
        }
        throw new IllegalStateException("No se pudo iniciar sesion con " + email);
    }

    public void abrirRecuperacion() {
        web.clic(OLVIDE);
    }

    public void enviarCodigo() {
        web.clic(ENVIAR_CODIGO);
    }

    public void cancelarRecuperacion() {
        web.clic(CANCELAR_RECUPERACION);
    }

    public void ojo() {
        web.clic(OJO);
    }

    public String tipoCampoClave() {
        return web.encontrar(CLAVE).getAttribute("type");
    }

    public void entrarConGoogle() {
        web.clic(GOOGLE);
    }
}
