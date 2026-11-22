package com.achron.tests;

import com.achron.core.Acciones;
import com.achron.core.BaseTest;
import com.achron.core.CuentaDePrueba;
import com.achron.core.Modulo;
import com.achron.pages.LoginPage;
import com.achron.pages.MenuLateral;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;

@Tag("sesion")
@Modulo(nombre = "Sesion", titulo = "Inicio y cierre de sesión", ruta = "auth")
class SesionTest extends BaseTest {

    private static CuentaDePrueba cuenta;

    @BeforeAll
    static void crearCuenta() {
        cuenta = CuentaDePrueba.crearCoach("sesion", 1);
    }

    // email / clave: CUENTA = la cuenta creada para esta clase; "sesion.xxx" = de datos.properties
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "SES-01 | Iniciar sesion       | Inicio de sesion con datos correctos               | entrar            | CUENTA                    | CUENTA                       | Alta",
            "SES-02 | Errores de inicio    | Contraseña incorrecta                              | entrarMal         | CUENTA                    | sesion.contrasena.incorrecta | Alta",
            "SES-03 | Errores de inicio    | Correo no registrado                               | entrarMal         | sesion.email.noregistrado | CUENTA                       | Alta",
            "SES-04 | Errores de inicio    | Correo con formato invalido                        | entrarMal         | sesion.email.invalido     | CUENTA                       | Alta",
            "SES-05 | Errores de inicio    | Campos vacios                                      | entrarMal         | ''                        | ''                           | Alta",
            "SES-06 | Cerrar sesion        | Cerrar sesion desde el menu lateral                | salir             | CUENTA                    | CUENTA                       | Alta",
            "SES-07 | Cerrar sesion        | Despues de salir, volver atras no entra            | salirYVolver      | CUENTA                    | CUENTA                       | Alta",
            "SES-08 | Paginas protegidas   | Abrir /dashboard sin sesion pide iniciar sesion    | protegida         | ''                        | ''                           | Alta",
            "SES-09 | Recuperar contraseña | Abre la pantalla de recuperacion y se puede volver | recuperar         | ''                        | ''                           | Media",
            "SES-10 | Recuperar contraseña | Recuperar con correo invalido                      | recuperarInvalido | sesion.email.invalido     | ''                           | Media",
            "SES-11 | Contraseña           | Boton del ojo muestra y oculta la contraseña       | ojo               | ''                        | CUENTA                       | Baja",
            "SES-12 | Acceso con Google    | Entrar con Google lleva a la pagina de Google      | google            | ''                        | ''                           | Media"
    })
    void sesion(String id, String categoria, String descripcion, String accion, String email, String clave, String severidad) {
        email = email.equals("CUENTA") ? cuenta.email : dato(email, "sesion");
        clave = clave.equals("CUENTA") ? cuenta.clave : dato(clave, "sesion");
        nuevoCaso(id, categoria, descripcion, severidad,
                "email = '" + email + "'" + (clave.isEmpty() ? "" : ", contraseña = " + (clave.equals(cuenta.clave) ? "(la de la cuenta)" : "'" + clave + "'")));
        LoginPage login = new LoginPage(web);

        switch (accion) {
            case "protegida":
                web.irA("dashboard");
                web.esperarQue("corta", d -> d.getCurrentUrl().contains("/auth"));
                verificar("No muestra el panel sin sesion", "redirige a /auth", web.url(), web.url().contains("/auth"));
                break;

            case "entrar":
                login.abrir();
                login.escribirCredenciales(email, clave);
                login.entrar();
                verificar("Entra al panel", "url con /dashboard", web.esperarUrl("/dashboard") ? web.url() : "sigue en " + web.url(), web.url().contains("/dashboard"));
                verificar("Muestra el panel de control", "'Panel de control'", web.esperarTexto("Panel de control") ? "aparece" : "no aparece",
                        web.textoPagina().contains("Panel de control"));
                break;

            case "entrarMal": {
                login.abrir();
                login.escribirCredenciales(email, clave);
                login.entrar();
                // se lee el aviso apenas sale: dura pocos segundos
                web.esperarQue("respuesta", d -> d.getCurrentUrl().contains("/dashboard") || web.existe(Acciones.AVISO));
                String aviso = web.esperarAviso();
                boolean entro = web.url().contains("/dashboard");
                String errores = String.join(" ", web.mensajeNavegador(LoginPage.EMAIL), web.mensajeNavegador(LoginPage.CLAVE),
                        aviso, web.texto(By.cssSelector("p[class*='text-red'], .text-destructive"))).trim();
                verificar("No entra al panel", "se queda en /auth", web.url(), !entro);
                verificar("Mensaje de error visible", "un texto que explique el error", siVacio(errores, "ningun mensaje"), !errores.isEmpty());
                break;
            }

            case "salir":
            case "salirYVolver":
                login.iniciarSesion(email, clave);
                new MenuLateral(web).cerrarSesion();
                verificar("Vuelve a la pantalla de acceso", "url con /auth", web.esperarUrl("/auth") ? web.url() : "sigue en " + web.url(), web.url().contains("/auth"));
                if (accion.equals("salirYVolver")) {
                    driver.navigate().back();
                    web.esperarQue("corta", d -> d.getCurrentUrl().contains("/auth"));
                    boolean veElPanel = web.url().contains("/dashboard") && web.textoPagina().contains("Panel de control");
                    verificar("Volver atras no muestra el panel", "sigue pidiendo iniciar sesion", web.url(), !veElPanel);
                }
                break;

            case "recuperar":
                login.abrir();
                login.abrirRecuperacion();
                boolean abierta = web.esperarTexto("Recuperación") && web.textoPagina().contains("Enviar código");
                verificar("Abre la recuperacion", "'Recuperación' y boton 'Enviar código'", abierta ? "aparece" : "no aparece", abierta);
                login.cancelarRecuperacion();
                verificar("Cancelar vuelve al acceso", "'Acceder'", web.esperarTexto("Acceder") ? "vuelve a Acceder" : "no vuelve", web.textoPagina().contains("Acceder"));
                break;

            case "recuperarInvalido": {
                login.abrir();
                login.abrirRecuperacion();
                web.escribir(LoginPage.EMAIL, email);
                login.enviarCodigo();
                String aviso = web.esperarAviso();
                String errores = String.join(" ", web.mensajeNavegador(LoginPage.EMAIL), aviso,
                        web.texto(By.cssSelector("p[class*='text-red'], .text-destructive"))).trim();
                verificar("Mensaje de error visible", "un texto que explique que el correo es invalido", siVacio(errores, "ningun mensaje"), !errores.isEmpty());
                verificar("No dice que envio el codigo", "sin aviso de codigo enviado", siVacio(aviso, "ningun aviso"), !aviso.toLowerCase().contains("enviado"));
                break;
            }

            case "ojo": {
                login.abrir();
                web.escribir(LoginPage.CLAVE, clave);
                String antes = login.tipoCampoClave();
                login.ojo();
                String despues = login.tipoCampoClave();
                login.ojo();
                String otraVez = login.tipoCampoClave();
                verificar("Al inicio esta oculta", "type=password", "type=" + antes, "password".equals(antes));
                verificar("El ojo la muestra", "type=text", "type=" + despues, "text".equals(despues));
                verificar("Otro clic la oculta", "type=password", "type=" + otraVez, "password".equals(otraVez));
                break;
            }

            case "google":
                login.abrir();
                login.entrarConGoogle();
                boolean google = web.esperarQue("respuesta", d -> d.getCurrentUrl().contains("accounts.google.com"));
                verificar("Lleva al inicio de sesion de Google", "url de accounts.google.com", web.url(), google);
                break;

            default:
                throw new IllegalArgumentException("Accion desconocida: " + accion);
        }
        comprobarTodo();
    }
}
