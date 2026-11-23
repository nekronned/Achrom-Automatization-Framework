package com.achron.reporting;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Crea incidentes (INC) en ServiceNow con su API REST (tabla incident).
 * - Si la corrida queda ROTA (la automatizacion fallo), se crea un INC para revisar las pruebas.
 * - Si servicenow.incidentePorHallazgo=true, ademas uno por cada hallazgo nuevo del sitio.
 * Esta apagado mientras servicenow.habilitado=false: solo escribe en el log lo que habria creado.
 * La contraseña o token no se guardan en el proyecto: se pasan con -Dservicenow.clave=... o con la
 * variable de entorno SERVICENOW_CLAVE.
 */
public final class ServiceNow {

    private ServiceNow() {
    }

    public static void avisarCorridaRota(String resumen) {
        crearIncidente("[Achron] La automatizacion de pruebas quedo ROTA - corrida " + Corrida.id(),
                resumen + "\nLog: " + Corrida.archivoLog(), "2");
    }

    public static void avisarHallazgo(Caso caso) {
        if (!Configuracion.siNo("servicenow.incidentePorHallazgo")) return;
        Caso.Comprobacion fallo = caso.primerFallo();
        String urgencia = caso.severidad().equals("Alta") ? "1" : caso.severidad().equals("Media") ? "2" : "3";
        crearIncidente("[Achron] " + caso.id() + " " + caso.descripcion(),
                "Modulo: " + caso.modulo() + "\nSe esperaba: " + fallo.esperado + "\nSe obtuvo: " + fallo.obtenido
                        + "\nCorrida: " + Corrida.id() + "\nLog: " + Corrida.archivoLog(), urgencia);
    }

    private static void crearIncidente(String titulo, String descripcion, String urgencia) {
        if (!Configuracion.siNo("servicenow.habilitado")) {
            Log.get().info("ServiceNow apagado; se habria creado el incidente: {}", titulo);
            return;
        }
        String cuerpo = "{\"short_description\":\"" + json(titulo) + "\",\"description\":\"" + json(descripcion)
                + "\",\"urgency\":\"" + urgencia + "\",\"impact\":\"" + urgencia
                + "\",\"assignment_group\":\"" + json(Configuracion.texto("servicenow.grupo")) + "\"}";
        String usuarioYClave = Configuracion.texto("servicenow.usuario") + ":" + clave();
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(Configuracion.texto("servicenow.url") + "/api/now/table/incident"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(usuarioYClave.getBytes(StandardCharsets.UTF_8)))
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> respuesta = HttpClient.newHttpClient().send(pedido, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 201) {
                Log.get().info("incidente creado en ServiceNow: {}", titulo);
            } else {
                Log.get().warn("ServiceNow respondio {} al crear '{}': {}", respuesta.statusCode(), titulo, respuesta.body());
            }
        } catch (Exception e) {
            Log.get().warn("no se pudo crear el incidente en ServiceNow: {}", e.getMessage());
        }
    }

    private static String clave() {
        String desdeEntorno = System.getenv("SERVICENOW_CLAVE");
        return desdeEntorno != null ? desdeEntorno : Configuracion.texto("servicenow.clave");
    }

    private static String json(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
