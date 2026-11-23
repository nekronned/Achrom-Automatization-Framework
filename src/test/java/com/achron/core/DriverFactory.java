package com.achron.core;

import com.achron.config.Configuracion;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.http.ClientConfig;

/** Crea el navegador segun config.properties (navegador, headless, dejarAbierto). */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver crear() {
        boolean headless = Configuracion.siNo("navegador.headless");
        boolean dejarAbierto = Configuracion.siNo("navegador.dejarAbierto");
        WebDriver driver;

        if (nombreNavegador().equals("edge")) {
            EdgeOptions opciones = new EdgeOptions();
            if (headless) opciones.addArguments("--headless=new");
            if (dejarAbierto) opciones.setExperimentalOption("detach", true);
            driver = new EdgeDriver(EdgeDriverService.createDefaultService(), opciones, limiteDeRespuesta());
        } else {
            ChromeOptions opciones = new ChromeOptions();
            if (headless) opciones.addArguments("--headless=new");
            if (dejarAbierto) opciones.setExperimentalOption("detach", true);
            driver = new ChromeDriver(ChromeDriverService.createDefaultService(), opciones, limiteDeRespuesta());
        }

        if (headless) {
            driver.manage().window().setSize(new Dimension(1920, 1080));
        } else {
            driver.manage().window().maximize();
        }
        driver.manage().timeouts().pageLoadTimeout(Configuracion.espera("larga").multipliedBy(3));
        return driver;
    }

    /** Si el navegador deja de responder, Selenium se rinde en este tiempo en vez de esperar para siempre. */
    private static ClientConfig limiteDeRespuesta() {
        return ClientConfig.defaultConfig().readTimeout(Configuracion.espera("navegador"));
    }

    public static String nombreNavegador() {
        return Configuracion.texto("navegador").toLowerCase();
    }
}
