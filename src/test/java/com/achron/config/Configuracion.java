package com.achron.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

/**
 * Lee config.properties (como se corre) y datos.properties (con que datos).
 * Un -Dclave=valor de Maven siempre gana sobre lo que diga el archivo.
 */
public final class Configuracion {

    private static final Properties VALORES = new Properties();

    static {
        cargar("config.properties");
        cargar("datos.properties");
    }

    private Configuracion() {
    }

    public static String texto(String clave) {
        String valor = System.getProperty(clave, VALORES.getProperty(clave));
        if (valor == null) {
            throw new IllegalStateException("Falta la clave '" + clave + "' en config.properties o datos.properties");
        }
        return valor.trim();
    }

    public static boolean siNo(String clave) {
        return Boolean.parseBoolean(texto(clave));
    }

    public static int numero(String clave) {
        return Integer.parseInt(texto(clave));
    }

    /** espera("corta"), espera("respuesta") o espera("larga"), en segundos en el archivo. */
    public static Duration espera(String tipo) {
        return Duration.ofSeconds(Long.parseLong(texto("espera." + tipo)));
    }

    public static String url() {
        return texto("url");
    }

    public static Path carpetaReportes() {
        return Paths.get(texto("carpeta.reportes"));
    }

    public static Path carpetaLogs() {
        return Paths.get(texto("carpeta.logs"));
    }

    private static void cargar(String archivo) {
        try (InputStream entrada = Configuracion.class.getClassLoader().getResourceAsStream(archivo)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontro " + archivo + " en src/test/resources");
            }
            VALORES.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo, e);
        }
    }
}
