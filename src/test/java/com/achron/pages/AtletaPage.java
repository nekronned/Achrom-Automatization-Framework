package com.achron.pages;

import com.achron.core.Acciones;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

/**
 * Lo que ve y hace un atleta (asesorado): elegir su coach, su panel (/client-dashboard), sus rutinas,
 * su perfil (/client-profile), el pago de la membresia con comprobante (/client-payment), el chat con el coach
 * y el Asistente IA. Los pagos son de prueba: solo se sube una imagen como comprobante, no se mueve dinero.
 */
public class AtletaPage {

    public static final String PANEL = "client-dashboard";
    public static final String PERFIL = "client-profile";
    public static final String PAGO = "client-payment";

    private static final By MONTO = By.id("amount");
    private static final By COMPROBANTE = By.cssSelector("main input[type='file']");
    public static final By MENSAJE = By.cssSelector("input[placeholder='Escribe un mensaje aquí...']");
    public static final By ENVIAR_MENSAJE = By.cssSelector("main button:has(svg.lucide-send)");
    private static final By REGISTRAR_PAGO = By.xpath("//main//button[normalize-space()='Registrar y enviar']");
    private static final By GUARDAR_FOTO = By.xpath("//*[@role='dialog'][@data-state='open']//button[contains(normalize-space(), 'Confirmar y guardar')]");
    private static final By MENU = By.cssSelector("aside a, nav a");

    private final Acciones web;

    public AtletaPage(Acciones web) {
        this.web = web;
    }

    // ---------- sesion y coach ----------

    /** Inicia sesion como atleta y espera su panel (el del coach es /dashboard, el del atleta /client-dashboard). */
    public void iniciarSesion(String email, String clave) {
        LoginPage login = new LoginPage(web);
        login.abrir();
        login.escribirCredenciales(email, clave);
        for (int intento = 1; intento <= 3; intento++) {
            login.entrar();
            if (web.esperarUrl("/" + PANEL)) {
                web.esperarQue("respuesta", d -> web.textoPagina().contains("Tu panel de inicio") || web.textoPagina().contains("Elige a tu"));
                return;
            }
        }
        throw new IllegalStateException("No se pudo iniciar sesion como atleta con " + email);
    }

    /**
     * Sale de la cuenta en este navegador para entrar con otra (coach o atleta): el sitio guarda la sesion
     * en el almacenamiento del navegador, asi que se borra junto con las cookies y se vuelve al inicio de sesion.
     */
    public void salir() {
        web.irA("auth");
        ((org.openqa.selenium.JavascriptExecutor) web.driver()).executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
        web.driver().manage().deleteAllCookies();
        web.irA("auth");
    }

    /** La tarjeta de un coach en "Elige a tu Guia primate" (el bloque que tiene su nombre y un solo boton "Elegir este coach"). */
    private By tarjetaCoach(String nombreCoach) {
        return By.xpath("//*[normalize-space(text())='" + nombreCoach + "']/ancestor::div[count(.//button[normalize-space()='Elegir este coach']) = 1][1]");
    }

    public boolean coachEnLista(String nombreCoach) {
        return web.esperarQue("respuesta", d -> web.existe(tarjetaCoach(nombreCoach)));
    }

    /** "Ver trayectoria" en la tarjeta del coach: abre su perfil profesional. */
    public void verTrayectoria(String nombreCoach) {
        web.clic(By.xpath("(//*[normalize-space(text())='" + nombreCoach + "']/ancestor::div[count(.//button[normalize-space()='Elegir este coach']) = 1][1]"
                + "//*[self::button or self::a][normalize-space()='Ver trayectoria'])[1]"));
    }

    /** "Elegir este coach" en su tarjeta (abre la ventana de confirmacion). */
    public void pedirElegir(String nombreCoach) {
        web.clic(By.xpath("(//*[normalize-space(text())='" + nombreCoach + "']/ancestor::div[count(.//button[normalize-space()='Elegir este coach']) = 1][1]"
                + "//button[normalize-space()='Elegir este coach'])[1]"));
    }

    public void confirmarElegir() {
        web.clic(By.xpath("//*[@role='dialog'][@data-state='open']//button[normalize-space()='Sí, elegir']"));
        web.esperarTexto("Tu coach");
    }

    /** Cierra la ventana abierta con Escape. */
    public void cerrarVentana() {
        web.encontrar(By.tagName("body")).sendKeys(Keys.ESCAPE);
        web.desaparece(Acciones.DIALOGO);
    }

    /** En "Elige a tu Guia primate" busca la tarjeta del coach por su nombre y lo elige. */
    public void elegirCoach(String nombreCoach) {
        web.esperarUrl("/" + PANEL);
        pedirElegir(nombreCoach);
        confirmarElegir();
    }

    // ---------- panel ----------

    public void abrirPanel() {
        web.irA(PANEL);
        web.esperarTexto("Tu panel de inicio");
    }

    /** Los textos de las opciones del menu lateral del atleta. */
    public List<String> menu() {
        List<String> opciones = new ArrayList<>();
        for (WebElement enlace : web.todos(MENU)) {
            String texto = enlace.getText().trim();
            if (!texto.isEmpty() && !opciones.contains(texto)) {
                opciones.add(texto);
            }
        }
        return opciones;
    }

    /** El texto de una seccion del panel: desde su titulo hasta los siguientes caracteres. */
    public String seccion(String titulo, int largo) {
        String pagina = web.textoPagina();
        int i = pagina.indexOf(titulo);
        return i < 0 ? "" : pagina.substring(i, Math.min(pagina.length(), i + largo)).replace("\n", " · ");
    }

    /** Da clic en un boton, enlace o pestana de main por su texto. */
    public void clicEn(String texto) {
        web.clic(By.xpath("(//main//*[self::button or self::a or @role='tab'][normalize-space()='" + texto + "'])[1]"));
    }

    // ---------- perfil ----------

    public void abrirPerfil() {
        web.irA(PERFIL);
        web.esperarTexto("Mi perfil personal");
        web.esperarQue("respuesta", d -> !web.valor(By.id("fullName")).isEmpty());
        // el resto de los datos llega despues del nombre (si el atleta ya los guardo)
        web.esperarQue("corta", d -> !web.valor(By.id("phone")).isEmpty());
    }

    /** Escribe en un campo del perfil por su id (fullName, phone, gym, height, weight, newPassword, confirmPassword). */
    public void escribirPerfil(String id, String valor) {
        web.escribir(By.id(id), valor);
    }

    public String valorPerfil(String id) {
        return web.valor(By.id(id));
    }

    public void guardarPerfil() {
        web.clic(By.xpath("//main//button[normalize-space()='Guardar mis cambios']"));
    }

    public void actualizarClave() {
        web.clic(By.xpath("//main//button[normalize-space()='Actualizar contraseña']"));
    }

    // ---------- evolucion (fotos) ----------

    public void abrirEvolucion() {
        web.irA(PANEL + "?tab=metrics");
        web.esperarTexto("Mi galería de evolución");
        web.esperarTexto("Próximo registro");
    }

    public int fotosDeEvolucion() {
        return web.todos(By.cssSelector("main img[alt^='Progreso']")).size();
    }

    public void nuevaFoto() {
        web.clic(By.xpath("//main//button[contains(normalize-space(), 'Nueva foto')]"));
        web.encontrar(Acciones.DIALOGO);
    }

    public boolean guardarFotoHabilitado() {
        return web.encontrar(GUARDAR_FOTO).isEnabled();
    }

    public void subirFotoDeEvolucion(Path imagen) {
        web.subirArchivo(By.cssSelector("[role='dialog'][data-state='open'] input[type='file']"), imagen);
        web.clic(GUARDAR_FOTO);
    }

    // ---------- pago ----------

    /** Abre el pago de la membresia y el formulario "Ya realice el pago". */
    public void abrirFormularioDePago() {
        web.irA(PAGO);
        web.esperarTexto("Pago de Membresía");
        web.clic(By.xpath("//main//button[normalize-space()='Ya realicé el pago']"));
        web.encontrar(MONTO);
    }

    public void llenarPago(String monto, Path comprobante) {
        web.escribir(MONTO, monto);
        if (comprobante != null) {
            web.subirArchivo(COMPROBANTE, comprobante);
        }
    }

    public boolean registrarPagoHabilitado() {
        return web.encontrar(REGISTRAR_PAGO).isEnabled();
    }

    public void enviarPago() {
        web.clic(REGISTRAR_PAGO);
    }

    /** Registra un pago manual con su comprobante (no es un pago real: solo se sube una imagen). */
    public void registrarPago(String monto, Path comprobante) {
        abrirFormularioDePago();
        llenarPago(monto, comprobante);
        enviarPago();
        if (!web.esperarTexto("PAGO REGISTRADO")) {
            throw new IllegalStateException("El atleta no pudo registrar el pago de prueba");
        }
    }

    // ---------- chat ----------

    public void abrirChat() {
        web.irA("chat");
        web.encontrar(MENSAJE);
    }

    public void escribirAlCoach(String mensaje) {
        abrirChat();
        web.escribir(MENSAJE, mensaje);
        web.clic(ENVIAR_MENSAJE);
        if (!web.esperarTexto(mensaje)) {
            throw new IllegalStateException("El mensaje del atleta no aparecio en el chat");
        }
    }
}
