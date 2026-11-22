package com.achron.reporting;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * historial_corridas.csv: una linea por modulo y corrida (es lo que se usa para el monitoreo).
 * Tambien rehace Informe_<fecha>.txt, el resumen para "Lo que terminé hoy" del informe diario.
 */
public final class Historial {

    public static final String ENCABEZADO = "corrida;fecha;hora;modulo;total;pasaron;fallaron;errores_tecnicos;segundos;estado_corrida";

    private Historial() {
    }

    /**
     * Agrega la linea del modulo y devuelve el estado:
     * ROTA = alguna prueba se rompio (ERROR); CON HALLAZGOS = el sitio fallo en algo; OK = todo paso.
     */
    public static synchronized String registrar(String modulo, List<Caso> casos, long segundos) {
        int pasaron = contar(casos, Caso.Resultado.PASO);
        int fallaron = contar(casos, Caso.Resultado.FALLO);
        int errores = contar(casos, Caso.Resultado.ERROR);
        String estado = errores > 0 ? "ROTA" : fallaron > 0 ? "CON HALLAZGOS" : "OK";

        Path archivo = Configuracion.carpetaReportes().resolve("historial_corridas.csv");
        List<String> lineas = new ArrayList<>();
        if (!Files.exists(archivo)) {
            lineas.add(ENCABEZADO);
        }
        lineas.add(String.join(";", Corrida.id(), LocalDate.now().toString(), LocalTime.now().withNano(0).toString(), modulo,
                String.valueOf(casos.size()), String.valueOf(pasaron), String.valueOf(fallaron), String.valueOf(errores),
                String.valueOf(segundos), estado));
        try {
            Files.createDirectories(archivo.getParent());
            Files.write(archivo, lineas, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            escribirInformeDelDia(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir " + archivo, e);
        }
        return estado;
    }

    public static int contar(List<Caso> casos, Caso.Resultado resultado) {
        return (int) casos.stream().filter(c -> c.resultado() == resultado).count();
    }

    /** La ultima corrida de hoy de cada modulo, lista para copiar en el informe diario. */
    private static void escribirInformeDelDia(Path historial) throws IOException {
        String hoy = LocalDate.now().toString();
        Map<String, String> ultimaPorModulo = new TreeMap<>();
        for (String linea : Files.readAllLines(historial, StandardCharsets.UTF_8)) {
            String[] p = linea.split(";");
            if (p.length == 10 && p[1].equals(hoy)) {
                ultimaPorModulo.put(p[3], "Pruebas automáticas de " + p[3] + ": " + p[4] + " casos, " + p[5] + " pasan, "
                        + p[6] + " fallan, " + p[7] + " errores (" + p[8] + " s) | Reporte_" + p[3] + " de la corrida " + p[0]);
            }
        }
        List<String> informe = new ArrayList<>();
        informe.add("1. Lo que terminé hoy (Tarea | Commit, PR o captura)");
        informe.addAll(ultimaPorModulo.values());
        informe.add("");
        informe.add("3. Hallazgos de pruebas: ver Hallazgos_" + hoy + ".txt");
        Files.write(historial.resolveSibling("Informe_" + hoy + ".txt"), informe, StandardCharsets.UTF_8);
    }
}
