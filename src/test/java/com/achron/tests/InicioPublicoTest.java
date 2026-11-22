package com.achron.tests;

import com.achron.core.BaseTest;
import com.achron.core.Modulo;
import com.achron.pages.InicioPublicoPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.WebElement;

/** Pagina publica: no necesita cuenta, es lo que ve cualquier visitante. */
@Tag("inicio")
@Modulo(nombre = "Inicio", titulo = "Página pública (inicio)")
class InicioPublicoTest extends BaseTest {

    // objetivo: texto del enlace/boton o id de la seccion; valor: lo que se espera ver o se escribe
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "INI-01 | Contenido              | La pagina carga con titulo y secciones         | contenido     | -                         | -                              | Alta",
            "INI-02 | Menu con anclas        | Inicio lleva a su seccion                      | ancla         | Inicio                    | home                           | Baja",
            "INI-03 | Menu con anclas        | Plataforma lleva a su seccion                  | ancla         | Plataforma                | features                       | Baja",
            "INI-04 | Menu con anclas        | Reseñas lleva a su seccion                     | ancla         | Reseñas                   | testimonials                   | Baja",
            "INI-05 | Menu con anclas        | Precios lleva a su seccion                     | ancla         | Precios                   | pricing                        | Baja",
            "INI-06 | Menu con anclas        | Contacto lleva a su seccion                    | ancla         | Contacto                  | contact                        | Baja",
            "INI-07 | Botones de acceso      | Ingresar abre el inicio de sesion              | acceso        | Ingresar                  | Acceder                        | Alta",
            "INI-08 | Botones de acceso      | Registrarse abre el formulario de registro     | acceso        | Registrarse               | Nuevo Registro                 | Media",
            "INI-09 | Botones de acceso      | Empieza ahora abre el acceso                   | acceso        | Empieza ahora             | Acceder                        | Alta",
            "INI-10 | Botones de acceso      | Comienza tu prueba gratis abre el registro     | acceso        | Comienza tu prueba gratis | Nuevo Registro                 | Media",
            "INI-11 | Enlaces del pie        | Blog lleva a una pagina real                   | enlacePie     | Blog                      | -                              | Baja",
            "INI-12 | Enlaces del pie        | Tutoriales lleva a una pagina real             | enlacePie     | Tutoriales                | -                              | Baja",
            "INI-13 | Enlaces del pie        | Terminos de Servicio lleva a una pagina real   | enlacePie     | Términos de Servicio      | -                              | Baja",
            "INI-14 | Enlaces del pie        | Politica de Privacidad lleva a una pagina real | enlacePie     | Política de Privacidad    | -                              | Baja",
            "INI-15 | Formulario de contacto | Enviar el formulario vacio                     | contactoVacio | -                         | -                              | Baja",
            "INI-16 | Formulario de contacto | Correo invalido en el contacto                 | contactoEmail | -                         | inicio.contacto.email.invalido | Baja",
            "INI-17 | Idioma                 | Cambiar a ingles traduce la pagina             | idioma        | EN - English              | -                              | Media"
    })
    void inicio(String id, String categoria, String descripcion, String accion, String objetivo, String valor, String severidad) {
        valor = dato(valor, "inicio");
        nuevoCaso(id, categoria, descripcion, severidad, objetivo.equals("-") ? "-" : "'" + objetivo + "'");
        InicioPublicoPage inicio = new InicioPublicoPage(web);
        inicio.abrir();

        switch (accion) {
            case "contenido":
                String pagina = web.textoPagina();
                verificar("Titulo principal", InicioPublicoPage.TITULO, pagina.contains(InicioPublicoPage.TITULO) ? "aparece" : "no aparece",
                        pagina.contains(InicioPublicoPage.TITULO));
                for (String seccion : new String[] {"home", "features", "testimonials", "pricing", "contact"}) {
                    boolean existe = inicio.existeSeccion(seccion);
                    verificar("Seccion #" + seccion, "existe", existe ? "existe" : "no existe", existe);
                }
                break;

            case "ancla": {
                String seccion = valor;
                inicio.irAAncla(objetivo);
                boolean enPantalla = web.esperarQue("respuesta", d -> inicio.seccionEnPantalla(seccion));
                verificar("La url apunta a la seccion", "#" + seccion, web.url(), web.url().endsWith("#" + seccion));
                verificar("La seccion queda en pantalla", "la seccion empieza en la mitad de arriba de la pantalla",
                        "empieza a " + Math.round(inicio.posicionDeSeccion(seccion)) + " px del borde de arriba", enPantalla);
                break;
            }

            case "acceso": {
                inicio.botonOEnlace(objetivo);
                boolean llego = web.esperarUrl("/auth");
                web.esperarQue("corta", d -> web.textoPagina().contains("Nuevo Registro") || web.textoPagina().contains("Acceder"));
                String texto = web.textoPagina();
                String pantalla = texto.contains("Nuevo Registro") ? "registro (Nuevo Registro)" : texto.contains("Acceder") ? "inicio de sesion (Acceder)" : "otra";
                verificar("Lleva a /auth", "url con /auth", web.url(), llego);
                verificar("Muestra la pantalla correcta", "'" + valor + "'", pantalla, texto.contains(valor));
                break;
            }

            case "enlacePie": {
                WebElement enlace = inicio.enlaceDelPie(objetivo);
                String href = enlace.getAttribute("href");
                String urlAntes = web.url();
                web.clic(enlace);
                boolean cambio = web.esperarQue("corta", d -> !d.getCurrentUrl().replace("#", "").equals(urlAntes.replace("#", "")));
                verificar("El enlace tiene destino", "una direccion real (no '#')", href, href != null && !href.endsWith("#"));
                verificar("Al hacer clic cambia de pagina", "otra pagina", web.url(), cambio);
                break;
            }

            case "contactoVacio": {
                inicio.enviarContacto();
                String aviso = web.esperarAviso();
                String errores = String.join(" ", web.mensajeNavegador(InicioPublicoPage.CONTACTO_NOMBRE), web.mensajeNavegador(InicioPublicoPage.CONTACTO_EMAIL),
                        web.mensajeNavegador(InicioPublicoPage.CONTACTO_MENSAJE), web.texto(InicioPublicoPage.ERRORES_CONTACTO)).trim();
                verificar("No dice que se envio", "sin aviso de mensaje enviado", siVacio(aviso, "ningun aviso"), !aviso.toLowerCase().contains("enviado"));
                verificar("Mensaje de error visible", "un texto que pida llenar los campos", siVacio(errores, "ningun mensaje"), !errores.isBlank());
                break;
            }

            case "contactoEmail": {
                web.escribir(InicioPublicoPage.CONTACTO_NOMBRE, dato("inicio.contacto.nombre", "inicio"));
                web.escribir(InicioPublicoPage.CONTACTO_EMAIL, valor);
                web.escribir(InicioPublicoPage.CONTACTO_MENSAJE, dato("inicio.contacto.mensaje", "inicio"));
                caso.dato("email = '" + valor + "'");
                inicio.enviarContacto();
                String aviso = web.esperarAviso();
                String errores = (web.mensajeNavegador(InicioPublicoPage.CONTACTO_EMAIL) + " " + web.texto(InicioPublicoPage.ERRORES_CONTACTO)).trim();
                verificar("No dice que se envio", "sin aviso de mensaje enviado", siVacio(aviso, "ningun aviso"), !aviso.toLowerCase().contains("enviado"));
                verificar("Mensaje de error visible", "un texto que explique que el correo es invalido", siVacio(errores, "ningun mensaje"), !errores.isBlank());
                break;
            }

            case "idioma":
                inicio.cambiarIdioma(objetivo);
                boolean cambio = web.esperarQue("corta", d -> !web.textoPagina().contains(InicioPublicoPage.TITULO));
                verificar("El titulo cambia de idioma", "titulo en ingles (sin '" + InicioPublicoPage.TITULO + "')", cambio ? "cambio" : "sigue en español", cambio);
                break;

            default:
                throw new IllegalArgumentException("Accion desconocida: " + accion);
        }
        comprobarTodo();
    }
}
