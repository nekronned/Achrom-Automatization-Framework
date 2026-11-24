package com.achron.tests;

import com.achron.config.Corrida;
import com.achron.core.Acciones;
import com.achron.core.BaseTest;
import com.achron.core.CuentaDePrueba;
import com.achron.core.Modulo;
import com.achron.pages.BibliotecaPage;
import com.achron.pages.LoginPage;
import com.achron.pages.MenuLateral;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pestañas Metodologías, Métodos y Rutinas de la biblioteca (Movimientos esta en EjerciciosTest).
 * Lo que se crea aqui ("ZZ ... Auto N") se queda: el sitio muestra un boton de borrar en todas las metodologias,
 * tambien en las del sistema y de otros coaches, y las pruebas nunca lo pulsan para no borrar datos compartidos.
 */
@Tag("biblioteca")
@Modulo(nombre = "Biblioteca", titulo = "Biblioteca (metodologías, métodos y rutinas)", ruta = "exercises")
class BibliotecaTest extends BaseTest {

    private static CuentaDePrueba cuenta;

    @BeforeAll
    static void crearCuenta() {
        cuenta = CuentaDePrueba.crearCoach("biblioteca", 5);
    }

    // tipo: Metodologías | Métodos | Bloque | Rutina. valor "biblioteca.xxx" = nombre de datos.properties + numero de corrida
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "BIB-01 | Metodologias | La lista muestra las metodologias            | lista      | Metodologías | AMRAP                  | Alta",
            "BIB-02 | Metodologias | Crear metodologia con nombre                 | crear      | Metodologías | biblioteca.metodologia | Alta",
            "BIB-03 | Metodologias | Crear metodologia sin nombre                 | crearVacio | Metodologías | ''                     | Media",
            "BIB-04 | Metodologias | Ver detalles de una metodologia              | detalles   | Metodologías | -                      | Media",
            "BIB-05 | Metodos      | La lista muestra los metodos                 | lista      | Métodos      | Drop Set               | Alta",
            "BIB-06 | Metodos      | Crear metodo con nombre                      | crear      | Métodos      | biblioteca.metodo      | Alta",
            "BIB-07 | Metodos      | Crear metodo sin nombre                      | crearVacio | Métodos      | ''                     | Media",
            "BIB-08 | Metodos      | Ver detalles de un metodo                    | detalles   | Métodos      | -                      | Media",
            "BIB-09 | Rutinas      | La pestaña Rutinas muestra bloques y rutinas | lista      | Rutina       | Rutinas sueltas        | Alta",
            "BIB-10 | Rutinas      | Crear bloque con nombre                      | crear      | Bloque       | biblioteca.bloque      | Alta",
            "BIB-11 | Rutinas      | Crear bloque sin nombre                      | crearVacio | Bloque       | ''                     | Media",
            "BIB-12 | Rutinas      | Crear rutina con nombre                      | crear      | Rutina       | biblioteca.rutina      | Alta",
            "BIB-13 | Rutinas      | Crear rutina sin nombre                      | crearVacio | Rutina       | ''                     | Media",
            "BIB-14 | Metodologias | No ofrece borrar las metodologias del sistema | sinBorrar | Metodologías | biblioteca.sistema     | Alta",
            "BIB-15 | Metodologias | Buscar muestra solo las coincidencias        | buscar     | Metodologías | biblioteca.sistema     | Media",
            "BIB-16 | Metodos      | Buscar muestra solo las coincidencias        | buscar     | Métodos      | biblioteca.busqueda.metodo | Media",
            "BIB-17 | Rutinas      | Crear rutina con un ejercicio                | crearConEjercicio | Rutina | biblioteca.rutina      | Alta"
    })
    void biblioteca(String id, String categoria, String descripcion, String accion, String tipo, String valor, String severidad) {
        if (valor.equals("biblioteca.sistema") || valor.equals("biblioteca.busqueda.metodo")) {
            valor = dato(valor, "biblioteca"); // nombres que ya existen en el sitio
        } else if (valor.startsWith("biblioteca.")) {
            valor = dato(valor, "biblioteca") + " " + Corrida.numero();
        }
        nuevoCaso(id, categoria, descripcion, severidad, tipo + (valor.equals("-") ? "" : ": nombre = '" + valor + "'"));
        new LoginPage(web).iniciarSesion(cuenta.email, cuenta.clave);
        new MenuLateral(web).ir("exercises");
        BibliotecaPage biblioteca = new BibliotecaPage(web);
        biblioteca.abrirPestana(tipo);

        switch (accion) {
            case "lista": {
                String esperado = valor;
                boolean aparece = biblioteca.esperarEnPestana(esperado);
                verificar("Muestra el contenido de la pestaña", "'" + esperado + "'", aparece ? "aparece" : "no aparece", aparece);
                break;
            }

            case "crear": {
                biblioteca.abrirFormulario(tipo);
                web.escribir(biblioteca.campoNombre(tipo), valor);
                if (tipo.equals("Rutina")) biblioteca.agregarBloqueDeEstructura();
                biblioteca.guardar(tipo);
                String aviso = web.esperarAviso();
                verificar("Mensaje de exito", "un aviso de creado/guardado", siVacio(aviso, "ningun aviso"), esExito(aviso));
                verificar("La ventana se cierra", "ventana cerrada", web.desaparece(Acciones.DIALOGO) ? "cerrada" : "sigue abierta", !web.hayDialogoAbierto());
                boolean enLista = biblioteca.esperarEnPestana(valor);
                verificar("Aparece en la lista", valor, enLista ? "aparece" : "no aparece", enLista);
                web.recargar();
                biblioteca.abrirPestana(tipo);
                boolean despues = biblioteca.esperarEnPestana(valor);
                verificar("Persistencia al recargar", valor + " sigue en la lista", despues ? "aparece" : "no aparece", despues);
                break;
            }

            case "crearVacio": {
                biblioteca.abrirFormulario(tipo);
                if (!biblioteca.guardarHabilitado(tipo)) {
                    // el sitio no deja guardar sin nombre: eso cuenta como rechazo
                    verificar("No se crea", "no se puede guardar sin nombre", "boton de guardar deshabilitado", true);
                    verificar("El formulario indica que falta el nombre", "boton deshabilitado o mensaje", "boton de guardar deshabilitado", true);
                    break;
                }
                biblioteca.guardar(tipo);
                String aviso = web.esperarAviso();
                String errores = String.join(" ", web.mensajeNavegador(biblioteca.campoNombre(tipo)),
                        web.texto(Acciones.ERRORES_DIALOGO),
                        esExito(aviso) ? "" : aviso).trim();
                verificar("No se crea", "sin aviso de creado/guardado", siVacio(aviso, "ningun aviso"), !esExito(aviso));
                verificar("Mensaje de error visible", "un texto que pida el nombre", siVacio(errores, "ningun mensaje"), !errores.isBlank());
                break;
            }

            case "detalles": {
                String antes = biblioteca.textoPestana();
                String urlAntes = web.url();
                biblioteca.verPrimerDetalle();
                web.esperarQue("corta", d -> web.hayDialogoAbierto() || !d.getCurrentUrl().equals(urlAntes) || !biblioteca.textoPestana().equals(antes));
                boolean dialogo = web.hayDialogoAbierto();
                boolean otraPagina = !web.url().equals(urlAntes);
                boolean cambio = !biblioteca.textoPestana().equals(antes);
                String obtenido = dialogo ? "se abrio una ventana: " + web.texto(Acciones.DIALOGO) : otraPagina ? "fue a " + web.url()
                        : cambio ? "la lista mostro mas informacion" : "no paso nada";
                verificar("Se abre el detalle", "una ventana, pagina o seccion con el detalle", obtenido, dialogo || otraPagina || cambio);
                break;
            }

            case "sinBorrar": {
                biblioteca.esperarEnPestana(valor);
                boolean ofrece = biblioteca.ofreceBorrar(valor);
                verificar("'" + valor + "' no se puede borrar desde una cuenta de coach", "sin boton de borrar", ofrece ? "muestra el boton de borrar" : "sin boton", !ofrece);
                break;
            }

            case "buscar": {
                String buscado = valor;
                biblioteca.esperarEnPestana(buscado);
                int antes = biblioteca.tarjetas();
                biblioteca.buscar(buscado);
                boolean filtro = web.esperarQue("respuesta", d -> biblioteca.tarjetas() > 0 && biblioteca.tarjetas() < antes && biblioteca.textoPestana().contains(buscado));
                verificar("Solo quedan las coincidencias", "menos de " + antes + " tarjetas, con '" + buscado + "'", biblioteca.tarjetas() + " tarjetas", filtro);
                break;
            }

            case "crearConEjercicio": {
                String nombre = valor + " con ejercicio";
                String ejercicio = com.achron.config.Configuracion.texto("asesorados.ejercicio");
                caso.dato("rutina '" + nombre + "', ejercicio '" + ejercicio + "'");
                biblioteca.abrirFormulario(tipo);
                web.escribir(biblioteca.campoNombre(tipo), nombre);
                biblioteca.agregarBloqueDeEstructura();
                biblioteca.importarEjercicio(ejercicio);
                biblioteca.guardar(tipo);
                String aviso = web.esperarAviso();
                verificar("Mensaje de exito", "un aviso de creado/guardado", siVacio(aviso, "ningun aviso"), esExito(aviso));
                web.recargar();
                biblioteca.abrirPestana(tipo);
                boolean despues = biblioteca.esperarEnPestana(nombre);
                verificar("Aparece en la lista despues de recargar", nombre, despues ? "aparece" : "no aparece", despues);
                break;
            }

            default:
                throw new IllegalArgumentException("Accion desconocida: " + accion);
        }
        comprobarTodo();
    }

    /** El sitio usa varias palabras para decir que salio bien ("creado", "asignada", "guardado"...). */
    private static boolean esExito(String aviso) {
        String a = aviso.toLowerCase();
        return a.contains("cread") || a.contains("asignad") || a.contains("guardad") || a.contains("agregad") || a.contains("éxito");
    }
}
