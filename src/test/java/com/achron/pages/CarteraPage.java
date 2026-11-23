package com.achron.pages;

import com.achron.core.Acciones;
import org.openqa.selenium.By;

/** Cartera (/wallet): bandeja de comprobantes por aprobar e historial de movimientos. */
public class CarteraPage {

    private static final By MOTIVO = By.cssSelector("[role='dialog'][data-state='open'] textarea");

    /** Tarjeta de un comprobante en la bandeja: el bloque que tiene el nombre y un solo boton "Ver comprobante". */
    private static final String TARJETA = "//main//*[contains(text(), '%s')][not(ancestor::table)]"
            + "/ancestor::div[count(.//button[normalize-space()='Ver comprobante']) = 1][1]";

    private final Acciones web;

    public CarteraPage(Acciones web) {
        this.web = web;
    }

    public void abrir() {
        web.irA("wallet");
        web.esperarTexto("Bandeja de aprobaciones");
    }

    /** Tarjeta del comprobante del asesorado en la bandeja ("" si no esta). */
    public String comprobanteEnBandeja(String asesorado) {
        return web.texto(tarjetaEnBandeja(asesorado));
    }

    public void verComprobante(String asesorado) {
        web.clic(By.xpath(TARJETA.replace("%s", asesorado) + "//button[normalize-space()='Ver comprobante']"));
        web.encontrar(Acciones.DIALOGO);
    }

    public void aprobar() {
        web.clic(botonDeVentana("Aprobar"));
    }

    /** Rechazar abre otra ventana que pide el motivo; se escribe y se confirma. */
    public void rechazar(String motivo) {
        web.clic(botonDeVentana("Rechazar"));
        web.escribir(MOTIVO, motivo);
        web.clic(botonDeVentana("Confirmar rechazo"));
    }

    /** Para el caso de rechazar sin motivo: abre la ventana del motivo sin escribir nada. */
    public void abrirRechazo() {
        web.clic(botonDeVentana("Rechazar"));
        web.encontrar(MOTIVO);
    }

    public boolean confirmarRechazoHabilitado() {
        return web.encontrar(botonDeVentana("Confirmar rechazo")).isEnabled();
    }

    public void confirmarRechazo() {
        web.clic(botonDeVentana("Confirmar rechazo"));
    }

    private static By botonDeVentana(String texto) {
        return By.xpath("(//*[@role='dialog'][@data-state='open']//button[normalize-space()='" + texto + "'])[last()]");
    }

    /** Fila del asesorado en "Historial de movimientos". */
    public String movimiento(String asesorado) {
        return web.texto(By.xpath("//main//table//tr[.//*[contains(normalize-space(), '" + asesorado + "')]]"));
    }

    /** Tarjeta de totales (titulo y valor), por ejemplo "Ingresos confirmados". */
    public String tarjeta(String titulo) {
        return web.texto(By.xpath("//main//*[normalize-space(text())='" + titulo + "']/ancestor::div[contains(., '$') or contains(., '%')][1]"));
    }

    private static By tarjetaEnBandeja(String asesorado) {
        return By.xpath(TARJETA.replace("%s", asesorado));
    }
}
