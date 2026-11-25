package com.achron.pages;

import com.achron.core.Acciones;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;

/** Panel de control (/dashboard). */
public class PanelPage {

    private static final Pattern MES = Pattern.compile(
            "(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre) \\d{4}");
    private static final By MES_SIGUIENTE = By.cssSelector("main button[aria-label='Go to next month']");
    private static final By MES_ANTERIOR = By.cssSelector("main button[aria-label='Go to previous month']");

    private final Acciones web;

    public PanelPage(Acciones web) {
        this.web = web;
    }

    public void botonDelPanel(String texto) {
        web.clicVisible(By.xpath("//main//button[normalize-space()='" + texto + "'] | //main//a[normalize-space()='" + texto + "']"));
    }

    public void elegirDia(String dia) {
        web.clicVisible(By.xpath("//main//button[normalize-space()='" + dia + "']"));
    }

    /** Pestaña de "Control de Mensualidades" (Próximos, Semana, Inactivos). */
    public void elegirPestanaMensualidades(String nombre) {
        web.clic(pestanaMensualidades(nombre));
    }

    /** "active" o "inactive": asi marca el sitio la pestaña elegida. */
    public String estadoPestana(String nombre) {
        return web.encontrar(pestanaMensualidades(nombre)).getAttribute("data-state");
    }

    private static By pestanaMensualidades(String nombre) {
        return By.xpath("//main//button[@role='tab'][normalize-space()='" + nombre + "']");
    }

    public String mesDelCalendario() {
        Matcher m = MES.matcher(web.texto(By.tagName("main")));
        return m.find() ? m.group() : "";
    }

    public void mesSiguiente() {
        web.clic(MES_SIGUIENTE);
    }

    public void mesAnterior() {
        web.clic(MES_ANTERIOR);
    }

    /** Texto "Sesiones del ..." del calendario. */
    public String sesionesDelDia() {
        String pagina = web.textoPagina();
        int i = pagina.indexOf("Sesiones del");
        return i < 0 ? "" : pagina.substring(i, Math.min(pagina.length(), i + 35)).replace("\n", " ");
    }

    /** Pasa el mouse por el grafico "Actividad de Alumnos" y devuelve lo que dice su etiqueta en cada punto ("" si no sale). */
    public String valoresDelGraficoDeActividad() {
        org.openqa.selenium.By grafico = org.openqa.selenium.By.xpath("(//main//*[starts-with(normalize-space(text()), 'Actividad de Alumnos')]/following::*[name()='svg' and contains(@class, 'recharts-surface')])[1]");
        org.openqa.selenium.WebElement svg = web.encontrar(grafico);
        ((org.openqa.selenium.JavascriptExecutor) web.driver()).executeScript("arguments[0].scrollIntoView({block: 'center'});", svg);
        StringBuilder valores = new StringBuilder();
        int ancho = svg.getSize().getWidth();
        for (int i = 1; i <= 7; i++) {
            new org.openqa.selenium.interactions.Actions(web.driver()).moveToElement(svg, -ancho / 2 + ancho * i / 8, 0).perform();
            org.openqa.selenium.By etiqueta = org.openqa.selenium.By.cssSelector(".recharts-tooltip-wrapper");
            web.esperarQue("corta", d -> web.texto(etiqueta).contains("semana"));
            String texto = web.texto(etiqueta);
            if (!texto.isEmpty() && valores.indexOf(texto) < 0) valores.append(texto).append(" | ");
        }
        return valores.toString().trim();
    }
}
