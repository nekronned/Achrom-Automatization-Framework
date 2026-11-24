package com.achron.herramientas;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import com.achron.core.Acciones;
import com.achron.core.DriverFactory;
import com.achron.pages.PerfilPage;
import com.achron.pages.RegistroPage;
import org.openqa.selenium.WebDriver;

/**
 * Crea un coach nuevo y deja el navegador abierto en su perfil, para revisar el sitio a mano.
 * No es una prueba: no genera reportes. Usa (y suma) el contador de corridas de lee_me.txt para el correo y el NIT.
 *
 * Se corre con: mvn test-compile exec:java (ver README, seccion "Abrir una cuenta para revisar a mano").
 */
public final class AbrirCuentaCoach {

    /** Desfase del NIT: los modulos usan 1 a 9; este queda aparte para no chocar con ninguna cuenta de las pruebas. */
    private static final int DESFASE_NIT = 99;

    private AbrirCuentaCoach() {
    }

    public static void main(String[] args) {
        System.setProperty("navegador.dejarAbierto", "true");
        System.setProperty("navegador.headless", "false");

        // Igual que las cuentas de las pruebas: el correo y el NIT llevan el numero de corrida (lee_me.txt), asi no se repiten
        int corrida = Corrida.numero();
        String[] partes = Configuracion.texto("email").split("@");
        String email = partes[0] + "manual" + corrida + "@" + partes[1];
        String nit = String.valueOf(Long.parseLong(Configuracion.texto("nit")) + corrida * 100L + DESFASE_NIT);
        String clave = Configuracion.texto("contrasena");

        WebDriver driver = DriverFactory.crear();
        Acciones web = new Acciones(driver);
        RegistroPage registro = new RegistroPage(web);
        registro.abrir();
        registro.llenar(Configuracion.texto("nombre"), nit, email, clave);
        registro.enviarComoCoach();
        if (!web.esperarQue("larga", d -> d.getCurrentUrl().contains("/dashboard"))) {
            throw new IllegalStateException("No se pudo crear la cuenta " + email);
        }
        new PerfilPage(web).abrirDesdeMenu();

        System.out.println();
        System.out.println("Cuenta de coach abierta en el navegador");
        System.out.println("  Correo: " + email);
        System.out.println("  Clave:  la de 'contrasena' en datos.properties");
        System.out.println("  NIT:    " + nit);
        System.exit(0);
    }
}
