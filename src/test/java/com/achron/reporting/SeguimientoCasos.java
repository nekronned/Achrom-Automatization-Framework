package com.achron.reporting;

import com.achron.config.Corrida;
import com.achron.core.BaseTest;
import com.achron.core.Modulo;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.HasCdp;
import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * Sigue cada caso de una clase de prueba:
 * antes de la clase marca el modulo en el log; despues de cada caso decide PASÓ / FALLÓ / ERROR,
 * toma la captura, escribe el log y llena Allure; al final de la clase genera el reporte Word,
 * los hallazgos y la linea del historial.
 */
public class SeguimientoCasos implements BeforeAllCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterAllCallback {

    private static final Logger LOG = Log.get();
    private static final double ALTO_MAXIMO_CAPTURA = 6000;
    private static final String INICIO_MODULO = "inicioModulo";
    private static final String INICIO_CASO = "inicioCaso";

    @Override
    public void beforeAll(ExtensionContext contexto) {
        Modulo modulo = modulo(contexto);
        MDC.put("modulo", modulo.nombre());
        MDC.put("caso", "-");
        LOG.info("inicio del modulo (corrida {})", Corrida.id());
        contexto.getStore(ExtensionContext.Namespace.GLOBAL).put(INICIO_MODULO, System.currentTimeMillis());
    }

    @Override
    public void beforeTestExecution(ExtensionContext contexto) {
        contexto.getStore(ExtensionContext.Namespace.GLOBAL).put(INICIO_CASO, System.currentTimeMillis());
    }

    @Override
    public void afterTestExecution(ExtensionContext contexto) {
        BaseTest prueba = (BaseTest) contexto.getRequiredTestInstance();
        Caso caso = prueba.caso();
        if (caso == null) {
            LOG.error("el caso '{}' se detuvo antes de crearse", contexto.getDisplayName(), contexto.getExecutionException().orElse(null));
            return;
        }
        MDC.put("caso", caso.id());

        // FALLÓ = el sitio no hizo lo esperado (un assert). ERROR = se rompio la prueba (no encontro algo, se demoro, etc.)
        Throwable error = contexto.getExecutionException().orElse(null);
        if (caso.comprobaciones().isEmpty() || (error != null && !(error instanceof AssertionError))) {
            caso.marcarErrorTecnico(error);
        }
        long inicio = contexto.getStore(ExtensionContext.Namespace.GLOBAL).get(INICIO_CASO, Long.class);
        caso.segundos((System.currentTimeMillis() - inicio) / 1000.0);
        caso.captura(capturaPaginaCompleta(prueba.driver()));

        if (caso.resultado() == Caso.Resultado.ERROR) {
            LOG.error("ERROR | {} s | {}", caso.segundos(), caso.descripcion(), error);
        } else {
            LOG.info("{} | {} s", caso.resultadoTexto(), caso.segundos());
        }
        ResultadosCorrida.agregar(caso);
        llenarAllure(caso);
        MDC.put("caso", "-");
    }

    @Override
    public void afterAll(ExtensionContext contexto) {
        Modulo modulo = modulo(contexto);
        long inicio = contexto.getStore(ExtensionContext.Namespace.GLOBAL).get(INICIO_MODULO, Long.class);
        List<Caso> casos = ResultadosCorrida.de(modulo.nombre());
        long segundos = (System.currentTimeMillis() - inicio) / 1000;
        ResultadosCorrida.duracion(modulo.nombre(), segundos);

        if (!casos.isEmpty()) {
            ReporteWord.deModulo(modulo, casos, segundos);
            Hallazgos.guardar(modulo.nombre(), casos);
            String estado = Historial.registrar(modulo.nombre(), casos, segundos);
            LOG.info("fin del modulo | {} casos | {} s | {}", casos.size(), segundos, estado);
        }
        MDC.remove("caso");
        MDC.remove("modulo");
    }

    private static Modulo modulo(ExtensionContext contexto) {
        Modulo modulo = contexto.getRequiredTestClass().getAnnotation(Modulo.class);
        if (modulo == null) {
            throw new IllegalStateException(contexto.getRequiredTestClass().getSimpleName() + " no tiene la anotacion @Modulo");
        }
        return modulo;
    }

    private static void llenarAllure(Caso caso) {
        Allure.epic("Achron - rol coach");
        Allure.feature(caso.modulo());
        Allure.story(caso.categoria());
        Allure.label("severity", severidadAllure(caso.severidad()));
        Allure.parameter("corrida", Corrida.id());
        Allure.parameter("dato usado", caso.dato());

        StringBuilder tabla = new StringBuilder();
        for (Caso.Comprobacion c : caso.comprobaciones()) {
            tabla.append(c.ok ? "[OK]  " : "[NO]  ").append(c.que).append(System.lineSeparator())
                    .append("      esperado: ").append(c.esperado).append(System.lineSeparator())
                    .append("      obtenido: ").append(c.obtenido).append(System.lineSeparator());
        }
        Allure.addAttachment("Esperado vs obtenido", "text/plain", tabla.toString());
        Allure.addAttachment("Log del caso", "text/plain", Log.lineasDe(caso.id()));
        if (caso.captura() != null) {
            Allure.addAttachment("Captura de la pagina", "image/png", new ByteArrayInputStream(caso.captura()), "png");
        }
    }

    private static String severidadAllure(String severidad) {
        switch (severidad) {
            case "Alta": return "critical";
            case "Media": return "normal";
            default: return "minor";
        }
    }

    /** Captura de toda la pagina (no solo lo visible) usando Chrome DevTools. null si no se pudo. */
    private static byte[] capturaPaginaCompleta(WebDriver driver) {
        try {
            HasCdp chrome = (HasCdp) driver;
            Map<String, Object> medidas = chrome.executeCdpCommand("Page.getLayoutMetrics", new HashMap<>());
            Map<?, ?> contenido = (Map<?, ?>) medidas.get("cssContentSize");
            Map<String, Object> zona = new HashMap<>();
            zona.put("x", 0);
            zona.put("y", 0);
            zona.put("width", contenido.get("width"));
            // tope de alto: una pagina con cientos de tarjetas haria una imagen gigante que puede colgar a Chrome
            double alto = ((Number) contenido.get("height")).doubleValue();
            zona.put("height", Math.min(alto, ALTO_MAXIMO_CAPTURA));
            zona.put("scale", 1);
            Map<String, Object> opciones = new HashMap<>();
            opciones.put("captureBeyondViewport", true);
            opciones.put("clip", zona);
            Map<String, Object> foto = chrome.executeCdpCommand("Page.captureScreenshot", opciones);
            return Base64.getDecoder().decode((String) foto.get("data"));
        } catch (RuntimeException e) {
            LOG.warn("no se pudo tomar la captura: {}", e.getMessage());
            return null;
        }
    }
}
