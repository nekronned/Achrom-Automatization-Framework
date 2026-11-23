package com.achron.reporting;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Hallazgos_<fecha>.txt: los casos FALLADOS del dia en el formato del informe diario.
 * Una linea: ID | Módulo | Qué pasa | Severidad | Nuevo o Ya reportado | Corrida.
 * Los ERROR no van aqui (no son fallas del sitio); quedan en el log.
 */
public final class Hallazgos {

    public static final String ENCABEZADO = "ID | Módulo | Qué pasa | Severidad | Estado | Corrida";

    private Hallazgos() {
    }

    public static synchronized void guardar(String modulo, List<Caso> casos) {
        Path carpeta = Configuracion.carpetaReportes();
        String hoy = LocalDate.now().toString();
        Path archivo = carpeta.resolve("Hallazgos_" + hoy + ".txt");
        try {
            Files.createDirectories(carpeta);
            Set<String> yaReportados = idsDelUltimoDiaAnterior(carpeta, hoy);

            // si el modulo ya se corrio hoy, se reemplazan sus lineas
            List<String> lineas = new ArrayList<>();
            lineas.add(ENCABEZADO);
            if (Files.exists(archivo)) {
                for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                    if (!linea.equals(ENCABEZADO) && !linea.startsWith("ID |") && !linea.contains(" | " + modulo + " | ")) {
                        lineas.add(linea);
                    }
                }
            }
            for (Caso caso : casos) {
                if (caso.resultado() == Caso.Resultado.FALLO) {
                    boolean nuevo = !yaReportados.contains(caso.id());
                    lineas.add(linea(caso, nuevo ? "Nuevo" : "Ya reportado"));
                    if (nuevo) ServiceNow.avisarHallazgo(caso);
                }
            }
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir " + archivo, e);
        }
    }

    static String linea(Caso caso, String estado) {
        Caso.Comprobacion fallo = caso.primerFallo();
        // un texto sin traducir siempre es Baja, aunque el caso sea importante
        String severidad = fallo.que.contains("traducir") ? "Baja" : caso.severidad();
        String quePasa = caso.descripcion() + ": se esperaba " + fallo.esperado + ", se obtuvo " + recortar(fallo.obtenido);
        return String.join(" | ", caso.id(), caso.modulo(), quePasa, severidad, estado, Corrida.id());
    }

    private static Set<String> idsDelUltimoDiaAnterior(Path carpeta, String hoy) throws IOException {
        Path anterior = null;
        try (Stream<Path> archivos = Files.list(carpeta)) {
            for (Path p : (Iterable<Path>) archivos::iterator) {
                String nombre = p.getFileName().toString();
                if (nombre.startsWith("Hallazgos_") && nombre.compareTo("Hallazgos_" + hoy) < 0
                        && (anterior == null || nombre.compareTo(anterior.getFileName().toString()) > 0)) {
                    anterior = p;
                }
            }
        }
        Set<String> ids = new HashSet<>();
        if (anterior != null) {
            for (String linea : Files.readAllLines(anterior, StandardCharsets.UTF_8)) {
                ids.add(linea.split(" \\| ")[0].trim());
            }
        }
        return ids;
    }

    private static String recortar(String texto) {
        String limpio = texto.replace("\n", " ").replace("|", "/").trim();
        return limpio.length() > 120 ? limpio.substring(0, 117) + "..." : limpio;
    }
}
