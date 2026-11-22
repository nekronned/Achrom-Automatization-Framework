package com.achron.tests;

import com.achron.config.Configuracion;
import com.achron.core.Acciones;
import com.achron.core.BaseTest;
import com.achron.core.CuentaDePrueba;
import com.achron.core.Modulo;
import com.achron.pages.LoginPage;
import com.achron.pages.SeccionesPage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Asesorados, Cartera, Coaches, Objetivos, Chat, Asistente IA y Precios. */
@Tag("secciones")
@Modulo(nombre = "Secciones", titulo = "Secciones (asesorados, cartera, coaches, objetivos, chat, asistente IA y precios)")
class SeccionesTest extends BaseTest {

    private static final Pattern TOTAL_COACHES = Pattern.compile("Coaches de la Plataforma \\((\\d+)\\)");
    private static CuentaDePrueba cuenta;

    @BeforeAll
    static void crearCuenta() {
        cuenta = CuentaDePrueba.crearCoach("secciones", 6);
    }

    // ruta: pagina que se abre. valor: textos separados por ';', boton o filtro, segun la accion
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "SEC-01 | Asesorados   | La pagina muestra su titulo y la lista vacia        | textos         | clients    | Gestiona y haz seguimiento a tus asesorados;No se encontraron asesorados | Alta",
            "SEC-02 | Asesorados   | Buscar un asesorado que no existe                   | buscar         | clients    | No se encontraron                       | Media",
            "SEC-03 | Asesorados   | El filtro de estados tiene opciones                 | opciones       | clients    | Todos los estados                       | Media",
            "SEC-04 | Asesorados   | El filtro de objetivos tiene opciones               | opciones       | clients    | Todos los objetivos                     | Media",
            "SEC-05 | Asesorados   | Limpiar filtros borra la busqueda                   | limpiar        | clients    | Limpiar filtros                         | Media",
            "SEC-06 | Cartera      | La cartera muestra sus tarjetas e historial         | textos         | wallet     | Ingresos confirmados;Pendientes de confirmación;Tasa de pago activo;Historial de movimientos | Alta",
            "SEC-07 | Cartera      | Una cuenta nueva tiene ingresos en cero             | textos         | wallet     | $0,00                                   | Media",
            "SEC-08 | Cartera      | Todos los textos estan traducidos                   | sinClaves      | wallet     | -                                       | Baja",
            "SEC-09 | Cartera      | El filtro del historial tiene opciones              | opciones       | wallet     | Todos                                   | Media",
            "SEC-10 | Coaches      | La tabla de coaches muestra sus columnas            | textos         | coaches    | Nombre;Teléfono;Rol;Plan                | Alta",
            "SEC-11 | Coaches      | Todos los textos estan traducidos                   | sinClaves      | coaches    | -                                       | Baja",
            "SEC-12 | Coaches      | El numero del titulo coincide con las filas         | contarCoaches  | coaches    | -                                       | Media",
            "SEC-13 | Objetivos    | El radar muestra sus indicadores                    | textos         | objectives | Total asesorados;Progreso promedio;Metas alcanzadas | Alta",
            "SEC-14 | Chat         | El chat muestra su titulo y el aviso sin asesorados | textos         | chat       | Mensajes;No se encontraron asesorados activos | Alta",
            "SEC-15 | Chat         | Buscar un asesorado que no existe en el chat        | buscar         | chat       | No se encontraron                       | Media",
            "SEC-16 | Asistente IA | Con plan Starter el asistente pide mejorar el plan  | textos         | assistant  | Asistente Gorilla;Acceso exclusivo pro  | Alta",
            "SEC-17 | Asistente IA | Mejorar mi plan lleva a los planes                  | botonVa        | assistant  | Mejorar mi plan ahora                   | Media",
            "SEC-18 | Asistente IA | Las pestañas Guias y Ejemplos cambian el contenido  | pestanas       | assistant  | Ejemplos                                | Baja",
            "SEC-19 | Precios      | Se muestran los 3 planes con sus precios            | textos         | pricing    | Starter;Gratis;Pro;$29;Elite;$79        | Alta",
            "SEC-20 | Precios      | El plan actual (Starter) esta marcado               | textos         | pricing    | Tu plan actual                          | Media",
            "SEC-21 | Precios      | Preguntas frecuentes muestra informacion            | botonHace      | pricing    | Preguntas frecuentes                    | Baja",
            "SEC-22 | Precios      | Contacto directo muestra una forma de contacto      | botonHace      | pricing    | Contacto directo                        | Baja",
            "SEC-23 | Cartera      | Buscar un asesorado sin movimientos                 | buscarTabla    | wallet     | -                                       | Media",
            "SEC-24 | Coaches      | Ver perfil abre el perfil del coach                 | verPerfilCoach | coaches    | Ver perfil                              | Media",
            "SEC-25 | Coaches      | El perfil de otro coach es de solo lectura          | soloLectura    | coaches    | Ver perfil                              | Alta",
            "SEC-26 | Coaches      | Un coach normal no ve datos privados de otros       | datosPrivados  | coaches    | Ver perfil                              | Alta",
            "SEC-27 | Asistente IA | Abrir una guia muestra su contenido                 | abrirGuia      | assistant  | ¿Cómo empezar con CrossFit?             | Baja"
    })
    void secciones(String id, String categoria, String descripcion, String accion, String ruta, String valor, String severidad) {
        nuevoCaso(id, categoria, descripcion, severidad, "/" + ruta + (valor.equals("-") ? "" : " - '" + valor + "'"));
        new LoginPage(web).iniciarSesion(cuenta.email, cuenta.clave);
        SeccionesPage seccion = new SeccionesPage(web);
        seccion.abrir(ruta);

        switch (accion) {
            case "textos":
                for (String parte : valor.split(";")) {
                    boolean aparece = web.esperarTexto(parte);
                    verificar("Muestra '" + parte + "'", "aparece", aparece ? "aparece" : "no aparece", aparece);
                }
                break;

            case "sinClaves":
                web.esperarQue("corta", d -> !web.texto(By.tagName("main")).isBlank());
                verificarSinClaves();
                break;

            case "buscar": {
                String busqueda = Configuracion.texto("secciones.busqueda");
                caso.dato(caso.dato() + ", busqueda = '" + busqueda + "'");
                seccion.buscar(busqueda);
                boolean mensaje = web.esperarTexto(valor);
                verificar("Muestra que no hay resultados", "'" + valor + "'", mensaje ? "aparece" : "no aparece", mensaje);
                break;
            }

            case "buscarTabla": {
                String busqueda = Configuracion.texto("secciones.busqueda");
                caso.dato(caso.dato() + ", busqueda = '" + busqueda + "'");
                seccion.buscar(busqueda);
                web.esperarQue("corta", d -> seccion.filasTabla() <= 1);
                String tabla = web.texto(By.cssSelector("main table"));
                verificar("No muestra movimientos", "ningun movimiento en la tabla", siVacio(tabla, "tabla vacia"), !tabla.matches(".*\\d{4}-\\d{2}-\\d{2}.*"));
                verificarSinClaves();
                break;
            }

            case "opciones": {
                seccion.abrirFiltro(valor);
                int cantidad = web.todos(SeccionesPage.OPCIONES).size();
                verificar("El filtro tiene opciones para elegir", "2 o mas opciones", cantidad + " opciones: " + web.texto(SeccionesPage.OPCIONES), cantidad >= 2);
                break;
            }

            case "limpiar": {
                seccion.buscar(Configuracion.texto("secciones.busqueda"));
                seccion.boton(valor);
                web.esperarQue("corta", d -> web.valor(SeccionesPage.BUSCADOR).isEmpty());
                String queda = web.valor(SeccionesPage.BUSCADOR);
                verificar("La busqueda queda vacia", "''", "'" + queda + "'", queda.isEmpty());
                break;
            }

            case "contarCoaches": {
                web.esperarQue("respuesta", d -> TOTAL_COACHES.matcher(web.textoPagina()).find() && seccion.filasTabla() > 0);
                Matcher m = TOTAL_COACHES.matcher(web.textoPagina());
                int delTitulo = m.find() ? Integer.parseInt(m.group(1)) : -1;
                int filas = seccion.filasTabla();
                verificar("Numero del titulo igual a las filas", delTitulo + " (del titulo)", filas + " filas en la tabla", delTitulo == filas);
                break;
            }

            case "verPerfilCoach":
            case "soloLectura":
            case "datosPrivados": {
                int fila = Configuracion.numero("coaches.fila");
                web.esperarQue("respuesta", d -> seccion.filasTabla() >= fila);
                String nombreCoach = seccion.filaTexto(fila).split(" ")[0];
                caso.dato(caso.dato() + ", fila " + fila + " (" + nombreCoach + ")");
                seccion.accionDeFila(fila, valor);
                boolean abrio = web.esperarQue("respuesta", d -> d.getCurrentUrl().matches(".*/profile/.+") && web.textoPagina().contains("Perfil de"));
                if (accion.equals("verPerfilCoach")) {
                    verificar("Abre el perfil del coach", "url /profile/<id> con 'Perfil de ...'", web.url(), abrio);
                } else if (accion.equals("soloLectura")) {
                    long editables = web.todos(By.cssSelector("main input:not([type='file']), main textarea")).stream().filter(WebElement::isEnabled).count();
                    boolean botonGuardar = web.existe(By.xpath("//main//button[starts-with(normalize-space(), 'Guardar')]"));
                    verificar("Campos bloqueados", "0 campos editables", editables + " campos editables", abrio && editables == 0);
                    verificar("Sin botones de guardar", "ningun boton 'Guardar'", botonGuardar ? "hay botones de guardar" : "ninguno", abrio && !botonGuardar);
                } else {
                    String telefono = web.existe(By.id("phone")) ? web.valor(By.id("phone")) : "";
                    boolean seccionBancaria = web.textoPagina().contains("Configuración de recaudo");
                    verificar("Telefono de otro coach oculto", "no se muestra", siVacio(telefono, "no se muestra"), abrio && telefono.isBlank());
                    verificar("Datos bancarios de otro coach ocultos", "sin la seccion de recaudo y pagos", seccionBancaria ? "se muestra la seccion de recaudo" : "no se muestra",
                            abrio && !seccionBancaria);
                }
                break;
            }

            case "botonVa":
                seccion.boton(valor);
                verificar("Lleva a los planes", "url con /pricing", web.esperarUrl("/pricing") ? web.url() : "sigue en " + web.url(), web.url().contains("/pricing"));
                break;

            case "botonHace":
            case "abrirGuia": {
                web.esperarTexto(valor);
                String antes = web.textoPagina();
                String urlAntes = web.url();
                double scrollAntes = seccion.scrollVertical();
                if (accion.equals("abrirGuia")) {
                    web.clic(By.xpath("//main//*[normalize-space(text())='" + valor + "']"));
                } else {
                    seccion.boton(valor);
                }
                web.esperarQue("corta", d -> web.hayDialogoAbierto() || !d.getCurrentUrl().equals(urlAntes) || d.getWindowHandles().size() > 1
                        || web.existe(Acciones.AVISO) || !web.textoPagina().equals(antes));
                String aviso = web.texto(Acciones.AVISO);
                boolean dialogo = web.hayDialogoAbierto();
                boolean otraPagina = !web.url().equals(urlAntes) || driver.getWindowHandles().size() > 1;
                boolean cambio = !web.textoPagina().equals(antes);
                boolean seMovio = Math.abs(seccion.scrollVertical() - scrollAntes) > 50;
                String obtenido = dialogo ? "se abrio una ventana" : otraPagina ? "abrio " + web.url() : !aviso.isEmpty() ? "aviso: " + aviso
                        : cambio ? "mostro nuevo contenido" : seMovio ? "bajo a otra parte de la pagina" : "no paso nada";
                verificar("Muestra la informacion", "una ventana, pagina, aviso o seccion nueva", obtenido, dialogo || otraPagina || !aviso.isEmpty() || cambio || seMovio);
                break;
            }

            case "pestanas": {
                web.esperarTexto("Guías");
                String antes = web.textoPagina();
                seccion.boton(valor);
                boolean cambio = web.esperarQue("corta", d -> !web.textoPagina().equals(antes));
                verificar("El contenido cambia", "contenido distinto al de 'Guías'", cambio ? "cambio" : "igual que antes", cambio);
                break;
            }

            default:
                throw new IllegalArgumentException("Accion desconocida: " + accion);
        }
        comprobarTodo();
    }
}
