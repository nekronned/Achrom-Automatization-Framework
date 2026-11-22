package com.achron.reporting;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.achron.config.Corrida;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log de la automatizacion. La consola se configura en logback-test.xml; el archivo de la corrida
 * se agrega aqui porque su nombre lleva el id de la corrida (reportes de cada modulo/logs/ejecucion_<id>.log).
 * Cada linea queda: fecha hora | modulo | caso | mensaje.
 */
public final class Log {

    private static final Logger LOG = configurar();

    private Log() {
    }

    public static Logger get() {
        return LOG;
    }

    /** Lineas del log de esta corrida que pertenecen a un caso (para adjuntarlas en Allure). */
    public static String lineasDe(String idCaso) {
        Path archivo = Corrida.archivoLog();
        try {
            if (!Files.exists(archivo)) return "";
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            return lineas.stream().filter(l -> l.contains("| " + idCaso + " |")).collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Logger configurar() {
        LoggerContext contexto = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder formato = new PatternLayoutEncoder();
        formato.setContext(contexto);
        formato.setPattern("%d{yyyy-MM-dd HH:mm:ss} | %X{modulo:-corrida} | %X{caso:--} | %msg%n%ex");
        formato.setCharset(StandardCharsets.UTF_8);
        formato.start();

        FileAppender<ILoggingEvent> archivo = new FileAppender<>();
        archivo.setContext(contexto);
        archivo.setName("archivo-de-la-corrida");
        archivo.setFile(Corrida.archivoLog().toString());
        archivo.setEncoder(formato);
        archivo.start();

        ch.qos.logback.classic.Logger logger = contexto.getLogger("achron");
        logger.addAppender(archivo);
        return logger;
    }
}
