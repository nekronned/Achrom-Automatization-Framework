package com.achron.tests;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import com.achron.core.Acciones;
import com.achron.core.BaseTest;
import com.achron.core.Modulo;
import com.achron.pages.RegistroPage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * REG-01 registra un coach con datos validos. Los demas llenan todo bien y cambian SOLO el campo
 * indicado por un valor invalido; el sitio debe rechazarlo.
 */
@Tag("registro")
@Modulo(nombre = "Registro", titulo = "Registro de usuarios")
class RegistroTest extends BaseTest {

    // correo y NIT del REG-01, para probar duplicados
    private static String emailRegistrado;
    private static String nitRegistrado;

    // campo "ninguno" = todo valido; "todos" = todos los campos con ese valor
    // valor "(correo del REG-01)" / "(NIT del REG-01)" = el dato que se registro en REG-01
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "REG-01 | Registro            | Registro exitoso (datos validos) | ninguno         | ''                      | Alta",
            "REG-02 | Correo              | Correo sin arroba                | email           | pruebacorreo.gmail.com  | Alta",
            "REG-03 | Correo              | Correo sin dominio               | email           | prueba@                 | Alta",
            "REG-04 | Correo              | Correo con espacios              | email           | prueba correo@gmail.com | Alta",
            "REG-05 | Correo              | Correo con doble arroba          | email           | prueba@@gmail.com       | Alta",
            "REG-06 | NIT                 | NIT con letras                   | cedula          | 12345abcd               | Alta",
            "REG-07 | NIT                 | NIT con simbolos                 | cedula          | 123-456*78              | Alta",
            "REG-08 | Nombre              | Nombre con numeros               | fullName        | Jacobo123               | Alta",
            "REG-09 | Nombre              | Nombre con simbolos              | fullName        | Jacobo@#$               | Alta",
            "REG-10 | Contraseña          | Contraseña corta (menos de 8)    | password        | abc12                   | Alta",
            "REG-11 | Contraseña          | Contraseñas no coinciden         | confirmPassword | otraClave99             | Alta",
            "REG-12 | Campos obligatorios | Formulario vacio                 | todos           | ''                      | Alta",
            "REG-13 | Duplicados          | Correo repetido                  | email           | (correo del REG-01)     | Alta",
            "REG-14 | Duplicados          | NIT repetido                     | cedula          | (NIT del REG-01)        | Alta"
    })
    void registro(String id, String categoria, String descripcion, String campo, String valor, String severidad) {
        if (valor.equals("(correo del REG-01)")) valor = emailRegistrado != null ? emailRegistrado : Configuracion.texto("email");
        if (valor.equals("(NIT del REG-01)")) valor = nitRegistrado != null ? nitRegistrado : Configuracion.texto("nit");
        nuevoCaso(id, categoria, descripcion, severidad, campo.equals("ninguno") ? "datos validos de datos.properties" : campo + " = '" + valor + "'");

        // datos validos y unicos para este caso (numero de corrida + numero del caso)
        int numeroCaso = Integer.parseInt(id.substring(4));
        String[] partes = Configuracion.texto("email").split("@");
        String email = partes[0] + "reg" + Corrida.numero() + "c" + numeroCaso + "@" + partes[1];
        String nit = String.valueOf(Long.parseLong(Configuracion.texto("nit")) + Corrida.numero() * 100L + 20 + numeroCaso);

        RegistroPage registro = new RegistroPage(web);
        registro.abrir();
        registro.llenar(Configuracion.texto("nombre"), nit, email, Configuracion.texto("contrasena"));

        List<String> probados = new ArrayList<>();
        if (campo.equals("todos")) probados.addAll(RegistroPage.CAMPOS);
        else if (!campo.equals("ninguno")) probados.add(campo);
        for (String c : probados) {
            registro.escribir(c, valor);
        }
        if (campo.equals("password")) {
            registro.escribir("confirmPassword", valor); // asi solo falla el largo, no la coincidencia
        }

        // lo escrito y el mensaje del navegador se leen antes de enviar: si el sitio acepta, el formulario desaparece
        StringBuilder escritos = new StringBuilder();
        StringBuilder mensajesNavegador = new StringBuilder();
        for (String c : probados) {
            escritos.append("'").append(registro.valor(c)).append("' ");
            mensajesNavegador.append(registro.mensajeNavegador(c)).append(' ');
        }

        registro.enviarComoCoach();
        boolean seRegistro = registro.seRegistro();
        String url = web.url();
        boolean exito = registro.registroExitosoVisible();
        String aviso = web.texto(Acciones.AVISO);
        String avisos = aviso.contains("Registro exitoso") ? "" : aviso; // el aviso de exito no cuenta como error

        if (campo.equals("ninguno")) {
            emailRegistrado = email;
            nitRegistrado = nit;
            verificar("Cuenta creada", "'Registro exitoso' o /dashboard", (exito ? "aparecio 'Registro exitoso'" : "sin 'Registro exitoso'") + " | url: " + url, seRegistro);
            String errores = errores(registro, RegistroPage.CAMPOS) + avisos;
            verificar("Sin errores en el formulario", "ningun mensaje de error", siVacio(errores, "ninguno"), errores.isBlank());
        } else {
            String erroresProbados = (mensajesNavegador + errores(registro, probados)).trim();
            List<String> otros = new ArrayList<>(RegistroPage.CAMPOS);
            otros.removeAll(probados);
            String erroresOtros = errores(registro, otros);
            String todosLosMensajes = (erroresProbados + " " + avisos).trim();

            verificar("No navega al panel", "url sin /dashboard", url, !url.contains("/dashboard"));
            verificar("Sin mensaje de exito", "no aparece 'Registro exitoso'", exito ? "aparecio 'Registro exitoso'" : "no aparecio", !exito);
            verificar("Campo marcado con error", "error en " + probados, siVacio(erroresProbados, "ningun error en el campo"), !erroresProbados.isBlank());
            verificar("Mensaje de error visible", "un texto que explique el error", siVacio(todosLosMensajes, "ningun mensaje"), !todosLosMensajes.isBlank());
            verificar("Otros campos sin error", "sin errores fuera de " + probados, siVacio(erroresOtros, "sin errores"), erroresOtros.isBlank());
            verificar("Valor conservado", "'" + valor + "' en cada campo probado", escritos.toString().trim(),
                    escritos.toString().replace("'" + valor + "' ", "").isBlank());
        }
        comprobarTodo();
    }

    private static String errores(RegistroPage registro, List<String> campos) {
        StringBuilder texto = new StringBuilder();
        for (String c : campos) {
            String error = registro.errorDe(c);
            if (!error.isBlank()) texto.append(c).append(": ").append(error).append(' ');
        }
        return texto.toString();
    }
}
