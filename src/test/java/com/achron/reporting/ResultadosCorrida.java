package com.achron.reporting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Casos de todos los modulos de la corrida, en el orden en que se ejecutaron (para el reporte consolidado). */
public final class ResultadosCorrida {

    private static final Map<String, List<Caso>> CASOS = new LinkedHashMap<>();
    private static final Map<String, Long> SEGUNDOS = new LinkedHashMap<>();

    private ResultadosCorrida() {
    }

    public static synchronized void agregar(Caso caso) {
        CASOS.computeIfAbsent(caso.modulo(), m -> new ArrayList<>()).add(caso);
    }

    public static synchronized List<Caso> de(String modulo) {
        return CASOS.getOrDefault(modulo, List.of());
    }

    public static synchronized void duracion(String modulo, long segundos) {
        SEGUNDOS.put(modulo, segundos);
    }

    public static synchronized Map<String, List<Caso>> todos() {
        return new LinkedHashMap<>(CASOS);
    }

    public static synchronized Map<String, Long> duraciones() {
        return new LinkedHashMap<>(SEGUNDOS);
    }
}
