package com.achron.pages;

import com.achron.core.Acciones;
import java.nio.file.Path;
import java.util.List;
import org.openqa.selenium.By;

/** Mi perfil (/profile): informacion personal, perfil publico, datos bancarios, certificaciones y seguridad. */
public class PerfilPage {

    /** Campos que estan en la pestaña "PERFIL PÚBLICO"; los demas estan en "INFORMACIÓN PERSONAL". */
    public static final List<String> CAMPOS_PUBLICOS = List.of("professionalTitle", "biography", "methodology", "newCertName");

    public static final String INFO_PERSONAL = "INFORMACIÓN PERSONAL";
    public static final String PERFIL_PUBLICO = "PERFIL PÚBLICO";

    private static final By EMAIL = By.id("email");
    private static final By CERTIFICADO = By.id("cert-file-upload");
    private static final By AVATAR = By.id("avatar-upload");
    private static final By QR = By.id("qr-upload");
    private static final By GUARDAR_BANCO = By.xpath("//button[normalize-space()='Guardar configuración bancaria']");
    private static final By CAMBIAR_CLAVE = By.xpath("//button[normalize-space()='Cambiar contraseña']");
    private static final By CERRAR_SESION = By.xpath("//main//button[normalize-space()='Cerrar sesión']");
    private static final By TEXTO_ROJO = By.cssSelector("main p[class*='text-red'], main .text-destructive");

    private final Acciones web;

    public PerfilPage(Acciones web) {
        this.web = web;
    }

    /** Entra por el nombre del usuario en el menu y espera a que carguen sus datos. */
    public void abrirDesdeMenu() {
        web.clicVisible(By.cssSelector("a[href='/profile']"));
        esperarDatos();
    }

    public void esperarDatos() {
        web.esperarQue("larga", d -> !web.valor(EMAIL).isEmpty());
    }

    /** Boton de la pestaña. Se compara en minusculas porque el sitio solo las muestra en mayusculas. */
    public static By pestana(String texto) {
        return By.xpath("//button[translate(normalize-space(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚ', 'abcdefghijklmnopqrstuvwxyzáéíóú')='"
                + texto.toLowerCase() + "']");
    }

    public void abrirPestanaDe(String campo) {
        web.clic(pestana(CAMPOS_PUBLICOS.contains(campo) ? PERFIL_PUBLICO : INFO_PERSONAL));
        web.encontrar(By.id(campo));
    }

    public void escribir(String campo, String valor) {
        web.escribir(By.id(campo), valor);
    }

    public String valor(String campo) {
        return web.valor(By.id(campo));
    }

    /** Espera a que el campo tenga el valor indicado y devuelve el que tenga al final. */
    public String esperarValor(String campo, String esperado) {
        web.esperarQue("corta", d -> esperado.equals(valor(campo)));
        return valor(campo);
    }

    public void guardar(String campo) {
        String boton = CAMPOS_PUBLICOS.contains(campo) ? "Guardar perfil público" : "Guardar cambios";
        web.clic(By.xpath("//button[normalize-space()='" + boton + "']"));
    }

    /** Recarga, espera los datos y vuelve a la pestaña del campo. */
    public void recargar(String campo) {
        web.recargar();
        esperarDatos();
        abrirPestanaDe(campo);
    }

    /** Errores visibles: texto rojo de la pagina y el campo marcado como invalido por el navegador. */
    public String errores(String campo) {
        String invalido = web.existe(By.cssSelector("#" + campo + ":invalid")) ? campo + " invalido" : "";
        return (web.texto(TEXTO_ROJO) + " " + invalido).trim();
    }

    public void llenarBanco(String banco, String tipo, String cuenta, String titular) {
        web.clic(By.xpath("(//main//*[@role='combobox'])[1]"));
        web.clic(opcion(banco));
        web.clic(By.xpath("(//main//*[@role='combobox'])[2]"));
        web.clic(opcion(tipo));
        escribir("accountNumber", cuenta);
        escribir("accountHolder", titular);
    }

    public void guardarBanco() {
        web.clic(GUARDAR_BANCO);
    }

    public String bancoYTipo() {
        return web.texto(By.cssSelector("main [role='combobox']"));
    }

    public void subirCertificado(Path archivo) {
        web.subirArchivo(CERTIFICADO, archivo);
    }

    public void subirFoto(Path archivo) {
        web.subirArchivo(AVATAR, archivo);
    }

    public void subirQr(Path archivo) {
        web.subirArchivo(QR, archivo);
    }

    public int imagenesEnPagina() {
        return web.todos(By.cssSelector("main img")).size();
    }

    public void cambiarClave() {
        web.clic(CAMBIAR_CLAVE);
    }

    public void cerrarSesion() {
        web.clic(CERRAR_SESION);
    }

    private static By opcion(String texto) {
        return By.xpath("//*[@role='option'][normalize-space()='" + texto + "']");
    }
}
