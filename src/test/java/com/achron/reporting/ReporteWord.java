package com.achron.reporting;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import com.achron.core.DriverFactory;
import com.achron.core.Modulo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

/**
 * Reportes Word con Apache POI. Parte de plantilla_reporte.docx (estilos de titulos y tablas).
 * - deModulo: portada, indice, resumen ejecutivo y una seccion por caso con su captura.
 * - consolidado: lo mismo para toda la corrida, sin capturas (estan en el reporte de cada modulo).
 */
public final class ReporteWord {

    private static final String VERDE = "C6EFCE";
    private static final String ROJO = "FFC7CE";
    private static final String AMARILLO = "FFEB9C";
    private static final String GRIS = "D9E1F2";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ReporteWord() {
    }

    public static void deModulo(Modulo modulo, List<Caso> casos, long segundos) {
        XWPFDocument doc = nuevoDocumento(modulo.titulo());
        portada(doc, modulo.titulo(), Configuracion.url() + modulo.ruta(), segundos);
        indice(doc);
        resumenEjecutivo(doc, casos);

        titulo(doc, "Detalle de los casos", "Heading1");
        String categoria = "";
        for (Caso caso : casos) {
            if (!caso.categoria().equals(categoria)) {
                categoria = caso.categoria();
                titulo(doc, categoria, "Heading2");
            }
            detalleDelCaso(doc, caso, true);
        }
        guardar(doc, Configuracion.carpetaReportes().resolve(modulo.nombre())
                .resolve("Reporte_" + modulo.nombre() + "_" + Corrida.id() + ".docx"));
    }

    public static void consolidado(Map<String, List<Caso>> porModulo, Map<String, Long> duraciones, long segundos) {
        XWPFDocument doc = nuevoDocumento("Reporte consolidado");
        portada(doc, "Reporte consolidado de la corrida", Configuracion.url(), segundos);
        indice(doc);

        List<Caso> todos = new java.util.ArrayList<>();
        porModulo.values().forEach(todos::addAll);
        resumenEjecutivo(doc, todos);

        titulo(doc, "Resultado por módulo", "Heading2");
        XWPFTable tabla = tabla(doc, "Módulo", "Casos", "PASÓ", "FALLÓ", "ERROR", "Duración (s)", "Estado");
        for (Map.Entry<String, List<Caso>> e : porModulo.entrySet()) {
            List<Caso> casos = e.getValue();
            int fallaron = Historial.contar(casos, Caso.Resultado.FALLO);
            int errores = Historial.contar(casos, Caso.Resultado.ERROR);
            String estado = errores > 0 ? "ROTA" : fallaron > 0 ? "CON HALLAZGOS" : "OK";
            XWPFTableRow fila = fila(tabla, e.getKey(), String.valueOf(casos.size()), String.valueOf(Historial.contar(casos, Caso.Resultado.PASO)),
                    String.valueOf(fallaron), String.valueOf(errores), String.valueOf(duraciones.getOrDefault(e.getKey(), 0L)), estado);
            fila.getCell(6).setColor(errores > 0 ? ROJO : fallaron > 0 ? AMARILLO : VERDE);
        }

        titulo(doc, "Hallazgos de la corrida", "Heading1");
        XWPFTable hallazgos = tabla(doc, "ID", "Módulo", "Qué pasa", "Severidad");
        for (Caso caso : todos) {
            if (caso.resultado() == Caso.Resultado.FALLO) {
                String[] partes = Hallazgos.linea(caso, "").split(" \\| ");
                fila(hallazgos, partes[0], partes[1], partes[2], partes[3]);
            }
        }

        titulo(doc, "Detalle de los casos", "Heading1");
        for (Map.Entry<String, List<Caso>> e : porModulo.entrySet()) {
            titulo(doc, e.getKey(), "Heading2");
            for (Caso caso : e.getValue()) {
                detalleDelCaso(doc, caso, false);
            }
        }
        guardar(doc, Configuracion.carpetaReportes().resolve("Reporte_Consolidado_" + Corrida.id() + ".docx"));
    }

    // ----- partes del documento -----

    private static void portada(XWPFDocument doc, String titulo, String url, long segundos) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Title");
        p.setSpacingBefore(2400);
        p.createRun().setText("Reporte de pruebas");
        titulo(doc, titulo, "Heading1");

        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("Proyecto", Configuracion.texto("reporte.proyecto"));
        datos.put("Módulo", titulo);
        datos.put("Rol probado", Configuracion.texto("reporte.rol"));
        datos.put("Fecha", LocalDateTime.now().format(FECHA));
        datos.put("Corrida", Corrida.id());
        datos.put("Ambiente", Configuracion.texto("ambiente") + " - " + url);
        datos.put("Navegador", DriverFactory.nombreNavegador() + (Configuracion.siNo("navegador.headless") ? " (sin ventana)" : ""));
        datos.put("Duración", segundos / 60 + " min " + segundos % 60 + " s");
        datos.put("Responsable", Configuracion.texto("reporte.autor"));
        datos.put("Log de la corrida", Corrida.archivoLog().toString());

        XWPFTable tabla = doc.createTable(datos.size(), 2);
        tabla.setStyleID("TableGrid");
        tabla.setWidth("100%");
        int i = 0;
        for (Map.Entry<String, String> dato : datos.entrySet()) {
            celda(tabla.getRow(i).getCell(0), dato.getKey(), true, GRIS);
            celda(tabla.getRow(i).getCell(1), dato.getValue(), false, null);
            i++;
        }
        saltoDePagina(doc);
    }

    private static void indice(XWPFDocument doc) {
        titulo(doc, "Contenido", "Heading1");
        XWPFParagraph p = doc.createParagraph();
        p.getCTP().addNewFldSimple().setInstr("TOC \\o \"1-2\" \\h \\z \\u");
        doc.createParagraph().createRun().setText("(Si el índice sale vacío: clic derecho sobre él y \"Actualizar campo\".)");
        doc.enforceUpdateFields();
        saltoDePagina(doc);
    }

    private static void resumenEjecutivo(XWPFDocument doc, List<Caso> casos) {
        int pasaron = Historial.contar(casos, Caso.Resultado.PASO);
        int fallaron = Historial.contar(casos, Caso.Resultado.FALLO);
        int errores = Historial.contar(casos, Caso.Resultado.ERROR);

        titulo(doc, "Resumen ejecutivo", "Heading1");
        parrafo(doc, casos.size() + " casos: " + pasaron + " pasaron, " + fallaron + " fallaron (hallazgos del sitio) y "
                + errores + " tuvieron error de la automatización. Éxito: " + porcentaje(pasaron, casos.size()) + ".");

        XWPFTable totales = tabla(doc, "Total", "PASÓ", "FALLÓ", "ERROR", "% de éxito");
        fila(totales, String.valueOf(casos.size()), String.valueOf(pasaron), String.valueOf(fallaron), String.valueOf(errores),
                porcentaje(pasaron, casos.size()));

        titulo(doc, "Por categoría", "Heading2");
        tablaAgrupada(doc, casos, "Categoría", true);
        titulo(doc, "Por severidad", "Heading2");
        tablaAgrupada(doc, casos, "Severidad", false);

        titulo(doc, "Lista de casos", "Heading2");
        XWPFTable lista = tabla(doc, "ID", "Categoría", "Caso", "Severidad", "Resultado");
        for (Caso caso : casos) {
            XWPFTableRow fila = fila(lista, caso.id(), caso.categoria(), caso.descripcion(), caso.severidad(), caso.resultadoTexto());
            fila.getCell(4).setColor(color(caso.resultado()));
        }
        saltoDePagina(doc);
    }

    private static void tablaAgrupada(XWPFDocument doc, List<Caso> casos, String columna, boolean porCategoria) {
        Map<String, int[]> grupos = new LinkedHashMap<>();
        for (Caso caso : casos) {
            int[] cuenta = grupos.computeIfAbsent(porCategoria ? caso.categoria() : caso.severidad(), g -> new int[4]);
            cuenta[0]++;
            cuenta[1 + caso.resultado().ordinal()]++;
        }
        XWPFTable tabla = tabla(doc, columna, "Casos", "PASÓ", "FALLÓ", "ERROR");
        for (Map.Entry<String, int[]> g : grupos.entrySet()) {
            int[] c = g.getValue();
            fila(tabla, g.getKey(), String.valueOf(c[0]), String.valueOf(c[1]), String.valueOf(c[2]), String.valueOf(c[3]));
        }
    }

    private static void detalleDelCaso(XWPFDocument doc, Caso caso, boolean conCaptura) {
        titulo(doc, caso.id() + " - " + caso.descripcion() + " (" + caso.resultadoTexto() + ")", "Heading3");
        parrafo(doc, "Módulo: " + caso.modulo() + "   |   Categoría: " + caso.categoria() + "   |   Severidad: " + caso.severidad()
                + "   |   Duración: " + caso.segundos() + " s");
        parrafo(doc, "Dato usado: " + caso.dato());

        XWPFTable tabla = tabla(doc, "Comprobación", "Esperado", "Obtenido", "Resultado");
        for (Caso.Comprobacion c : caso.comprobaciones()) {
            XWPFTableRow fila = fila(tabla, c.que, c.esperado, c.obtenido, c.ok ? "PASÓ" : "FALLÓ");
            fila.getCell(3).setColor(c.ok ? VERDE : ROJO);
        }
        if (caso.resultado() == Caso.Resultado.ERROR) {
            parrafo(doc, "Error de la automatización: " + caso.detalleError());
        }
        parrafo(doc, "Log: " + Corrida.archivoLog().getFileName() + " (líneas con \"| " + caso.id() + " |\").");
        if (conCaptura && caso.captura() != null) {
            imagen(doc, caso.captura(), caso.id());
        }
    }

    // ----- ayudas -----

    private static XWPFDocument nuevoDocumento(String encabezado) {
        try (InputStream plantilla = ReporteWord.class.getClassLoader().getResourceAsStream("plantilla_reporte.docx")) {
            XWPFDocument doc = new XWPFDocument(plantilla);
            while (!doc.getBodyElements().isEmpty()) {
                doc.removeBodyElement(0);
            }
            doc.createHeader(HeaderFooterType.DEFAULT).createParagraph().createRun()
                    .setText(Configuracion.texto("reporte.proyecto") + " - " + encabezado);
            XWPFParagraph pie = doc.createFooter(HeaderFooterType.DEFAULT).createParagraph();
            pie.setAlignment(ParagraphAlignment.RIGHT);
            pie.createRun().setText("Corrida " + Corrida.id() + "   |   Página ");
            pie.getCTP().addNewFldSimple().setInstr("PAGE");
            pie.createRun().setText(" de ");
            pie.getCTP().addNewFldSimple().setInstr("NUMPAGES");
            return doc;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo abrir plantilla_reporte.docx", e);
        }
    }

    private static void titulo(XWPFDocument doc, String texto, String estilo) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle(estilo);
        p.createRun().setText(texto);
    }

    private static void parrafo(XWPFDocument doc, String texto) {
        doc.createParagraph().createRun().setText(texto);
    }

    private static void saltoDePagina(XWPFDocument doc) {
        doc.createParagraph().createRun().addBreak(BreakType.PAGE);
    }

    private static XWPFTable tabla(XWPFDocument doc, String... encabezados) {
        XWPFTable tabla = doc.createTable(1, encabezados.length);
        tabla.setStyleID("TableGrid");
        tabla.setWidth("100%");
        for (int i = 0; i < encabezados.length; i++) {
            celda(tabla.getRow(0).getCell(i), encabezados[i], true, GRIS);
        }
        return tabla;
    }

    private static XWPFTableRow fila(XWPFTable tabla, String... textos) {
        XWPFTableRow fila = tabla.createRow();
        for (int i = 0; i < textos.length; i++) {
            celda(fila.getCell(i), textos[i], false, null);
        }
        return fila;
    }

    private static void celda(XWPFTableCell celda, String texto, boolean negrita, String color) {
        XWPFRun run = celda.getParagraphs().get(0).createRun();
        run.setText(texto == null ? "" : texto);
        run.setBold(negrita);
        run.setFontSize(9);
        if (color != null) {
            celda.setColor(color);
        }
    }

    private static void imagen(XWPFDocument doc, byte[] png, String nombre) {
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
            // ancho de la pagina; si la captura es muy alta se reduce para que quepa en una hoja
            double ancho = 450;
            double alto = ancho * imagen.getHeight() / imagen.getWidth();
            if (alto > 620) {
                ancho = ancho * 620 / alto;
                alto = 620;
            }
            doc.createParagraph().createRun().addPicture(new ByteArrayInputStream(png), Document.PICTURE_TYPE_PNG,
                    nombre + ".png", Units.toEMU(ancho), Units.toEMU(alto));
        } catch (Exception e) {
            parrafo(doc, "(No se pudo insertar la captura: " + e.getMessage() + ")");
        }
    }

    private static String color(Caso.Resultado resultado) {
        return resultado == Caso.Resultado.PASO ? VERDE : resultado == Caso.Resultado.FALLO ? ROJO : AMARILLO;
    }

    private static String porcentaje(int parte, int total) {
        return total == 0 ? "0%" : Math.round(parte * 100.0 / total) + "%";
    }

    private static void guardar(XWPFDocument doc, Path archivo) {
        try {
            Files.createDirectories(archivo.getParent());
            try (OutputStream salida = Files.newOutputStream(archivo)) {
                doc.write(salida);
            }
            doc.close();
            Log.get().info("reporte generado: {}", archivo);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + archivo, e);
        }
    }
}
