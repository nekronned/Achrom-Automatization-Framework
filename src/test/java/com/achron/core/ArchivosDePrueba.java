package com.achron.core;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Archivos que se suben en las pruebas (foto, QR, diploma). Se crean en la carpeta temporal del sistema. */
public final class ArchivosDePrueba {

    private ArchivosDePrueba() {
    }

    public static Path imagenPng() {
        try {
            Path archivo = Files.createTempFile("achron-imagen", ".png");
            ImageIO.write(new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB), "png", archivo.toFile());
            archivo.toFile().deleteOnExit();
            return archivo;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la imagen de prueba", e);
        }
    }

    /** Un archivo que no es imagen ni PDF, para probar que el sitio lo rechace. */
    public static Path textoPlano() {
        try {
            Path archivo = Files.createTempFile("achron-notas", ".txt");
            Files.writeString(archivo, "Esto no es un diploma", StandardCharsets.UTF_8);
            archivo.toFile().deleteOnExit();
            return archivo;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el archivo de texto", e);
        }
    }
}
