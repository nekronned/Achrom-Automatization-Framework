package com.achron.reporting;

import com.achron.config.Corrida;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;

/**
 * Inicio y fin de toda la corrida (todas las clases). Se registra en
 * META-INF/services/org.junit.platform.launcher.TestExecutionListener.
 * Al terminar escribe el resumen en el log y el reporte Word consolidado.
 */
public class EventosCorrida implements TestExecutionListener {

    private static final Path HISTORIA_ALLURE = Paths.get("target", "site", "allure-maven-plugin", "history");
    private static final Path RESULTADOS_ALLURE = Paths.get("target", "allure-results", "history");

    @Override
    public void testPlanExecutionStarted(TestPlan plan) {
        Log.get().info("inicio de la corrida {}", Corrida.id());
        conservarHistoriaDeAllure();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan plan) {
        Logger log = Log.get();
        long segundos = Duration.between(Corrida.inicio(), LocalDateTime.now()).getSeconds();
        List<Caso> todos = ResultadosCorrida.todos().values().stream().flatMap(List::stream).collect(java.util.stream.Collectors.toList());
        if (todos.isEmpty()) {
            log.info("fin de la corrida {} | no se ejecuto ningun caso", Corrida.id());
            return;
        }
        int fallaron = Historial.contar(todos, Caso.Resultado.FALLO);
        int errores = Historial.contar(todos, Caso.Resultado.ERROR);
        String estado = errores > 0 ? "ROTA" : fallaron > 0 ? "CON HALLAZGOS" : "OK";
        ReporteWord.consolidado(ResultadosCorrida.todos(), ResultadosCorrida.duraciones(), segundos);
        log.info("fin de la corrida {} | {} casos, {} pasan, {} fallan, {} errores | {} s | {}", Corrida.id(), todos.size(),
                Historial.contar(todos, Caso.Resultado.PASO), fallaron, errores, segundos, estado);
        if (errores > 0) {
            log.warn("ALERTA: hubo {} errores de la automatizacion; revisar las lineas ERROR de este log", errores);
        }
    }

    /** Copia la historia del ultimo reporte Allure para que se vean las tendencias entre corridas. */
    private static void conservarHistoriaDeAllure() {
        if (!Files.isDirectory(HISTORIA_ALLURE)) return;
        try (Stream<Path> archivos = Files.list(HISTORIA_ALLURE)) {
            Files.createDirectories(RESULTADOS_ALLURE);
            for (Path archivo : (Iterable<Path>) archivos::iterator) {
                Files.copy(archivo, RESULTADOS_ALLURE.resolve(archivo.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Log.get().warn("no se pudo copiar la historia de Allure: {}", e.getMessage());
        }
    }
}
