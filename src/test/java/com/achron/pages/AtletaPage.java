package com.achron.pages;

import com.achron.core.Acciones;
import java.nio.file.Path;
import org.openqa.selenium.By;

/**
 * Lo que hace un atleta (asesorado) para que el coach tenga con que trabajar:
 * elegir su coach, registrar un pago con comprobante y escribirle por el chat.
 */
public class AtletaPage {

    private static final By MONTO = By.id("amount");
    private static final By COMPROBANTE = By.cssSelector("main input[type='file']");
    public static final By MENSAJE = By.cssSelector("input[placeholder='Escribe un mensaje aquí...']");
    public static final By ENVIAR_MENSAJE = By.cssSelector("main button:has(svg.lucide-send)");

    private final Acciones web;

    public AtletaPage(Acciones web) {
        this.web = web;
    }

    /** En "Elige a tu Guía primate" busca la tarjeta del coach por su nombre y lo elige. */
    public void elegirCoach(String nombreCoach) {
        web.esperarUrl("/client-dashboard");
        web.clic(By.xpath("//*[normalize-space(text())='" + nombreCoach + "']/ancestor::div[.//button[normalize-space()='Elegir este coach']][1]"
                + "//button[normalize-space()='Elegir este coach']"));
        web.clic(By.xpath("//*[@role='dialog'][@data-state='open']//button[normalize-space()='Sí, elegir']"));
        web.esperarTexto("Tu coach");
    }

    /** Registra un pago manual con su comprobante (no es un pago real: solo se sube una imagen). */
    public void registrarPago(String monto, Path comprobante) {
        web.irA("client-payment");
        web.clic(By.xpath("//main//button[normalize-space()='Ya realicé el pago']"));
        web.escribir(MONTO, monto);
        web.subirArchivo(COMPROBANTE, comprobante);
        web.clic(By.xpath("//main//button[normalize-space()='Registrar y enviar']"));
        if (!web.esperarTexto("PAGO REGISTRADO")) {
            throw new IllegalStateException("El atleta no pudo registrar el pago de prueba");
        }
    }

    public void escribirAlCoach(String mensaje) {
        web.irA("chat");
        web.escribir(MENSAJE, mensaje);
        web.clic(ENVIAR_MENSAJE);
        if (!web.esperarTexto(mensaje)) {
            throw new IllegalStateException("El mensaje del atleta no aparecio en el chat");
        }
    }
}
