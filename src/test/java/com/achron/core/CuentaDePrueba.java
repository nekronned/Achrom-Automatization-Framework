package com.achron.core;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import com.achron.pages.RegistroPage;
import com.achron.reporting.Log;
import org.openqa.selenium.WebDriver;

/**
 * Cuenta de coach nueva para una clase de prueba. Se crea una vez por clase (@BeforeAll)
 * con el formulario de registro; el correo y el NIT llevan el numero de corrida para no repetirse.
 */
public final class CuentaDePrueba {

    public final String nombre;
    public final String email;
    public final String clave;
    public final String nit;

    private CuentaDePrueba(String nombre, String email, String clave, String nit) {
        this.nombre = nombre;
        this.email = email;
        this.clave = clave;
        this.nit = nit;
    }

    /** @param sufijo diferencia la cuenta de cada modulo, por ejemplo "perfil" o "sesion" (y su NIT). */
    public static CuentaDePrueba crearCoach(String sufijo, int desfaseNit) {
        return crearCoach(sufijo, desfaseNit, Configuracion.texto("nombre"));
    }

    /** Coach con un nombre propio (para que un atleta lo encuentre en la lista de coaches). */
    public static CuentaDePrueba crearCoach(String sufijo, int desfaseNit, String nombre) {
        return registrar(sufijo, desfaseNit, nombre, true, null);
    }

    /**
     * Atleta nuevo que elige al coach indicado. El registro deja la sesion abierta, asi que despues
     * se ejecutan las acciones del atleta (registrar un pago, escribir al coach) sin volver a entrar.
     */
    public static CuentaDePrueba crearAtleta(String sufijo, int desfaseNit, String nombre, String nombreCoach,
                                             java.util.function.Consumer<com.achron.pages.AtletaPage> acciones) {
        return registrar(sufijo, desfaseNit, nombre, false, atleta -> {
            atleta.elegirCoach(nombreCoach);
            acciones.accept(atleta);
        });
    }

    private static CuentaDePrueba registrar(String sufijo, int desfaseNit, String nombre, boolean esCoach,
                                            java.util.function.Consumer<com.achron.pages.AtletaPage> despues) {
        String[] partes = Configuracion.texto("email").split("@");
        String email = partes[0] + sufijo + Corrida.numero() + "@" + partes[1];
        String nit = String.valueOf(Long.parseLong(Configuracion.texto("nit")) + Corrida.numero() * 100L + desfaseNit);
        CuentaDePrueba cuenta = new CuentaDePrueba(nombre, email, Configuracion.texto("contrasena"), nit);

        WebDriver driver = DriverFactory.crear();
        try {
            Acciones web = new Acciones(driver);
            RegistroPage registro = new RegistroPage(web);
            registro.abrir();
            registro.llenar(cuenta.nombre, nit, email, cuenta.clave);
            if (esCoach) registro.enviarComoCoach(); else registro.enviarComoAtleta();
            String panel = esCoach ? "/dashboard" : "/client-dashboard";
            if (!web.esperarQue("larga", d -> d.getCurrentUrl().contains(panel))) {
                throw new IllegalStateException("No se pudo crear la cuenta " + email);
            }
            if (despues != null) {
                despues.accept(new com.achron.pages.AtletaPage(web));
            }
            Log.get().info("cuenta de prueba creada ({}): {}", esCoach ? "coach" : "atleta", email);
        } finally {
            driver.quit();
        }
        return cuenta;
    }
}
