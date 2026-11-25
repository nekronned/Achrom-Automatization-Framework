package com.achron.tests;

import com.achron.core.BaseTest;
import com.achron.core.CuentaDePrueba;
import com.achron.core.Modulo;
import com.achron.pages.LoginPage;
import com.achron.pages.MenuLateral;
import com.achron.pages.PanelPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;

@Tag("panel")
@Modulo(nombre = "Panel", titulo = "Panel de control y menú", ruta = "dashboard")
class PanelTest extends BaseTest {

    private static CuentaDePrueba cuenta;

    @BeforeAll
    static void crearCuenta() {
        cuenta = CuentaDePrueba.crearCoach("panel", 2);
    }

    // objetivo: enlace, boton o pestaña sobre el que se actua; valor: lo que se espera ver
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "DAS-01 | Contenido del panel | El panel muestra sus secciones principales      | contenido       | -                      | Panel de control            | Alta",
            "DAS-02 | Contenido del panel | Una cuenta nueva empieza con 0 asesorados       | ceros           | -                      | -                           | Alta",
            "DAS-03 | Menu lateral        | Cartera abre su pagina                          | menu            | wallet                 | CARTERA                     | Alta",
            "DAS-04 | Menu lateral        | Asesorados abre su pagina                       | menu            | clients                | Asesorados                  | Alta",
            "DAS-05 | Menu lateral        | Coaches abre su pagina                          | menu            | coaches                | Coaches de la Plataforma    | Alta",
            "DAS-06 | Menu lateral        | Ejercicios abre su pagina                       | menu            | exercises              | Biblioteca de entrenamiento | Alta",
            "DAS-07 | Menu lateral        | Objetivos abre su pagina                        | menu            | objectives             | Radar de objetivos          | Alta",
            "DAS-08 | Menu lateral        | Chat abre su pagina                             | menu            | chat                   | Mensajes                    | Alta",
            "DAS-09 | Menu lateral        | Asistente IA abre su pagina                     | menu            | assistant              | Asistente Gorilla           | Alta",
            "DAS-10 | Menu lateral        | Inicio vuelve al panel                          | menuInicio      | dashboard              | Panel de control            | Alta",
            "DAS-11 | Botones del panel   | Subir de nivel abre los planes                  | botonPagina     | Subir de nivel         | Planes diseñados para ti    | Media",
            "DAS-12 | Acciones rapidas    | Añadir nuevo asesorado abre un formulario       | accionRapida    | Añadir nuevo asesorado | -                           | Media",
            "DAS-13 | Acciones rapidas    | Crear nueva rutina abre el formulario de rutina | accionRapida    | Crear nueva rutina     | Diseñar nueva rutina        | Media",
            "DAS-14 | Acciones rapidas    | Programar sesion abre un formulario             | accionRapida    | Programar sesión       | -                           | Media",
            "DAS-15 | Calendario          | Elegir un dia muestra las sesiones de ese dia   | calendario      | panel.dia              | -                           | Media",
            "DAS-16 | Herramientas        | La campana muestra las notificaciones           | campana         | -                      | Notificaciones              | Baja",
            "DAS-17 | Herramientas        | El boton de tema cambia entre oscuro y claro    | tema            | -                      | -                           | Baja",
            "DAS-18 | Idioma              | Cambiar a ingles traduce el panel               | idioma          | panel.idioma           | panel.titulo.ingles         | Media",
            "DAS-19 | Calendario          | Pasar al mes siguiente                          | mes             | siguiente              | -                           | Media",
            "DAS-20 | Calendario          | Volver al mes anterior                          | mes             | anterior               | -                           | Media",
            "DAS-21 | Mensualidades       | Pestaña Semana de Control de Mensualidades      | mensualidades   | Semana                 | -                           | Media",
            "DAS-22 | Mensualidades       | Pestaña Inactivos de Control de Mensualidades   | mensualidades   | Inactivos              | -                           | Media",
            "DAS-23 | Idioma              | Cambiar a aleman traduce el menu                | idiomaMenu      | DE - Deutsch           | -                           | Baja",
            "DAS-24 | Idioma              | Cambiar a frances traduce el menu               | idiomaMenu      | FR - Français          | -                           | Baja",
            "DAS-25 | Idioma              | Cambiar a portugues traduce el menu             | idiomaMenu      | PT - Português         | -                           | Baja",
            "DAS-26 | Pantalla pequeña    | En celular el boton de menu muestra el menu     | menuMovil       | -                      | -                           | Media",
            "DAS-27 | Contenido del panel | Una cuenta nueva no tiene entrenamientos en el grafico | grafico   | -                      | -                           | Media"
    })
    void panel(String id, String categoria, String descripcion, String accion, String objetivoCsv, String valorCsv, String severidad) {
        String objetivo = dato(objetivoCsv, "panel");
        String valor = dato(valorCsv, "panel");
        nuevoCaso(id, categoria, descripcion, severidad, objetivo.equals("-") ? "-" : "'" + objetivo + "'");
        new LoginPage(web).iniciarSesion(cuenta.email, cuenta.clave);
        PanelPage panel = new PanelPage(web);
        MenuLateral menu = new MenuLateral(web);

        switch (accion) {
            case "contenido": {
                String pagina = web.textoPagina();
                for (String parte : new String[] {valor, "Ingresos", "Asesorados totales", "Próximas sesiones", "Acciones rápidas"}) {
                    verificar("Muestra '" + parte + "'", "aparece", pagina.contains(parte) ? "aparece" : "no aparece", pagina.contains(parte));
                }
                verificarSinClaves();
                break;
            }

            case "ceros": {
                String pagina = web.textoPagina().replace("\n", " ");
                int i = pagina.indexOf("Asesorados totales");
                String obtenido = i < 0 ? "no aparece" : pagina.substring(i, Math.min(pagina.length(), i + 45));
                verificar("Asesorados totales en 0", "Asesorados totales: 0", obtenido, obtenido.replace(" ", "").startsWith("Asesoradostotales:0"));
                break;
            }

            case "menu":
            case "menuInicio": {
                if (accion.equals("menuInicio")) menu.ir("wallet");
                boolean llego = menu.ir(objetivo);
                web.esperarTexto(valor);
                verificar("Abre la pagina", "url con /" + objetivo, web.url(), llego);
                verificar("Muestra su titulo", "'" + valor + "'", web.textoPagina().contains(valor) ? "aparece" : "no aparece", web.textoPagina().contains(valor));
                verificarSinClaves();
                break;
            }

            case "botonPagina":
                panel.botonDelPanel(objetivo);
                verificar("Abre la pagina correcta", "'" + valor + "'", (web.esperarTexto(valor) ? "aparece" : "no aparece") + " (" + web.url() + ")",
                        web.textoPagina().contains(valor));
                break;

            case "accionRapida": {
                String urlAntes = web.url();
                panel.botonDelPanel(objetivo);
                String aviso = web.esperarAviso();
                boolean formulario = web.hayDialogoAbierto() || (!valor.equals("-") && web.textoPagina().contains(valor));
                String obtenido = formulario ? "se abrio el formulario" : !aviso.isEmpty() ? "aviso: " + aviso
                        : !web.url().equals(urlAntes) ? "fue a " + web.url() + " sin formulario" : "no paso nada";
                verificar("Abre el formulario de la accion", "una ventana o pagina para " + objetivo.toLowerCase(), obtenido, formulario);
                verificar("No dice que esta en desarrollo", "sin aviso 'en desarrollo'", siVacio(aviso, "ningun aviso"), !aviso.toLowerCase().contains("desarrollo"));
                break;
            }

            case "calendario": {
                panel.elegirDia(objetivo);
                String esperado = "Sesiones del " + objetivo + " de";
                web.esperarQue("corta", d -> panel.sesionesDelDia().startsWith(esperado));
                verificar("Muestra las sesiones del dia elegido", "'" + esperado + " ...'", siVacio(panel.sesionesDelDia(), "no aparece"),
                        panel.sesionesDelDia().startsWith(esperado));
                break;
            }

            case "mes": {
                String antes = panel.mesDelCalendario();
                if (objetivo.equals("siguiente")) panel.mesSiguiente(); else panel.mesAnterior();
                boolean cambio = web.esperarQue("corta", d -> !panel.mesDelCalendario().equals(antes));
                verificar("El calendario cambia de mes", "un mes distinto de '" + antes + "'", panel.mesDelCalendario(), cambio);
                break;
            }

            case "mensualidades": {
                panel.elegirPestanaMensualidades(objetivo);
                web.esperarQue("corta", d -> "active".equals(panel.estadoPestana(objetivo)));
                String estado = panel.estadoPestana(objetivo);
                verificar("La pestaña queda seleccionada", "estado 'active'", "estado '" + estado + "'", "active".equals(estado));
                verificar("Muestra el contenido de la pestaña", "lista o mensaje de la pestaña", web.textoPagina().contains("Sin vencimientos") ? "Sin vencimientos programados" : "otra informacion",
                        web.textoPagina().contains("Control de Mensualidades"));
                break;
            }

            case "campana": {
                menu.abrirNotificaciones();
                web.esperarQue("corta", d -> web.texto(MenuLateral.PANEL_NOTIFICACIONES).contains(valor));
                String abierto = web.texto(MenuLateral.PANEL_NOTIFICACIONES);
                verificar("Abre el panel de notificaciones", "'" + valor + "'", siVacio(abierto, "no se abrio nada"), abierto.contains(valor));
                break;
            }

            case "tema": {
                String antes = menu.claseDelTema();
                menu.cambiarTema();
                web.esperarQue("corta", d -> !menu.claseDelTema().equals(antes));
                String despues = menu.claseDelTema();
                verificar("Cambia el tema", "la clase 'dark' cambia", "antes: '" + antes + "' | despues: '" + despues + "'", antes.contains("dark") != despues.contains("dark"));
                break;
            }

            case "idioma":
                menu.cambiarIdioma(objetivo);
                web.esperarTexto(valor);
                verificar("El titulo cambia de idioma", "'" + valor + "'", web.textoPagina().contains(valor) ? "aparece" : "no aparece", web.textoPagina().contains(valor));
                verificar("El menu cambia de idioma", "'Wallet' en el menu", web.textoPagina().contains("Wallet") ? "aparece" : "no aparece", web.textoPagina().contains("Wallet"));
                break;

            case "idiomaMenu": {
                menu.cambiarIdioma(objetivo);
                By enlaceCartera = By.cssSelector("a[href='/wallet']");
                boolean traducido = web.esperarQue("corta", d -> !web.texto(enlaceCartera).contains("Cartera") && !web.texto(enlaceCartera).isBlank());
                verificar("El menu cambia de idioma", "'Cartera' traducido", siVacio(web.texto(enlaceCartera), "sin texto"), traducido);
                verificarSinClaves();
                break;
            }

            case "menuMovil": {
                driver.manage().window().setSize(new Dimension(400, 850));
                web.recargar();
                web.encontrar(By.tagName("main"));
                By enlaceCartera = By.cssSelector("a[href='/wallet']");
                boolean ocultoAntes = web.todos(enlaceCartera).stream().noneMatch(e -> e.isDisplayed());
                menu.abrirMenuMovil();
                boolean visibleDespues = web.aparece(enlaceCartera);
                verificar("En celular el menu empieza cerrado", "enlaces ocultos", ocultoAntes ? "ocultos" : "visibles", ocultoAntes);
                verificar("El boton de menu lo abre", "enlace 'Cartera' visible", visibleDespues ? "visible" : "no visible", visibleDespues);
                break;
            }

            case "grafico": {
                String valores = panel.valoresDelGraficoDeActividad();
                boolean enCero = !valores.isEmpty() && !valores.matches("(?s).*:\\s*[1-9].*");
                verificar("Actividad de alumnos en 0", "Esta semana : 0 y Semana pasada : 0", siVacio(valores, "no se pudo leer el grafico"), enCero);
                break;
            }

            default:
                throw new IllegalArgumentException("Accion desconocida: " + accion);
        }
        comprobarTodo();
    }
}
