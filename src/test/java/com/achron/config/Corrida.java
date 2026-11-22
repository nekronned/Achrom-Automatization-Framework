package com.achron.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Datos de la corrida actual (una corrida = una ejecucion de Maven).
 * El id ("C112_2026-11-22_14-05-31") sale en el log, el historial, los hallazgos,
 * los reportes Word y Allure, para poder cruzar todo.
 */
public final class Corrida {

    private static final LocalDateTime INICIO = LocalDateTime.now();
    private static final int NUMERO = siguienteNumero();
    private static final String ID = "C" + NUMERO + "_" + INICIO.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

    private Corrida() {
    }

    public static int numero() {
        return NUMERO;
    }

    public static String id() {
        return ID;
    }

    public static LocalDateTime inicio() {
        return INICIO;
    }

    public static Path archivoLog() {
        return Configuracion.carpetaLogs().resolve("ejecucion_" + ID + ".log");
    }

    /** Suma 1 a la linea "Corridas: N" del archivo de corridas y devuelve el nuevo numero. */
    private static synchronized int siguienteNumero() {
        Path archivo = Paths.get(Configuracion.texto("archivo.corridas"));
        try {
            List<String> lineas = Files.exists(archivo)
                    ? Files.readAllLines(archivo, StandardCharsets.UTF_8)
                    : new java.util.ArrayList<>();
            int numero = 1;
            boolean encontrada = false;
            for (int i = 0; i < lineas.size(); i++) {
                if (lineas.get(i).startsWith("Corridas:")) {
                    numero = Integer.parseInt(lineas.get(i).substring("Corridas:".length()).trim()) + 1;
                    lineas.set(i, "Corridas: " + numero);
                    encontrada = true;
                }
            }
            if (!encontrada) {
                lineas.add("Corridas: " + numero);
            }
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
            return numero;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo actualizar " + archivo, e);
        }
    }
}
