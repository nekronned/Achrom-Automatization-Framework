package com.achron.reporting;

import java.util.ArrayList;
import java.util.List;

/** Un caso de prueba ya ejecutado: sus comprobaciones (esperado vs obtenido), la captura y el resultado. */
public class Caso {

    public enum Resultado { PASO, FALLO, ERROR }

    private final String id;
    private final String modulo;
    private final String categoria;
    private final String descripcion;
    private final String severidad;
    private String dato;
    private final List<Comprobacion> comprobaciones = new ArrayList<>();
    private byte[] captura;
    private boolean errorTecnico;
    private String detalleError = "";
    private double segundos;

    public Caso(String id, String modulo, String categoria, String descripcion, String severidad, String dato) {
        this.id = id;
        this.modulo = modulo;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.severidad = severidad;
        this.dato = dato;
    }

    /** Una comprobacion: que se reviso, que se esperaba, que se obtuvo y si coincidio. */
    public static class Comprobacion {
        public final String que;
        public final String esperado;
        public final String obtenido;
        public final boolean ok;

        Comprobacion(String que, String esperado, String obtenido, boolean ok) {
            this.que = que;
            this.esperado = esperado;
            this.obtenido = obtenido;
            this.ok = ok;
        }
    }

    public void verificar(String que, String esperado, String obtenido, boolean ok) {
        comprobaciones.add(new Comprobacion(que, esperado, obtenido, ok));
    }

    public Resultado resultado() {
        if (errorTecnico) return Resultado.ERROR;
        if (comprobaciones.isEmpty()) return Resultado.ERROR;
        return comprobaciones.stream().allMatch(c -> c.ok) ? Resultado.PASO : Resultado.FALLO;
    }

    public String resultadoTexto() {
        switch (resultado()) {
            case PASO: return "PASÓ";
            case FALLO: return "FALLÓ";
            default: return "ERROR";
        }
    }

    /** Primera comprobacion que no se cumplio (para la linea de hallazgos), o null. */
    public Comprobacion primerFallo() {
        return comprobaciones.stream().filter(c -> !c.ok).findFirst().orElse(null);
    }

    public void marcarErrorTecnico(Throwable error) {
        errorTecnico = true;
        detalleError = error == null ? "La prueba se detuvo antes de comprobar" : error.toString();
    }

    public String id() { return id; }
    public String modulo() { return modulo; }
    public String categoria() { return categoria; }
    public String descripcion() { return descripcion; }
    public String severidad() { return severidad; }
    public String dato() { return dato; }
    public void dato(String dato) { this.dato = dato; }
    public List<Comprobacion> comprobaciones() { return comprobaciones; }
    public byte[] captura() { return captura; }
    public void captura(byte[] captura) { this.captura = captura; }
    public String detalleError() { return detalleError; }
    public double segundos() { return segundos; }
    public void segundos(double segundos) { this.segundos = segundos; }
}
