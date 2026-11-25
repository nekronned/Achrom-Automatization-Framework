package com.achron.tests;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import com.achron.core.BaseTest;
import com.achron.core.CuentaDePrueba;
import com.achron.core.Modulo;
import com.achron.pages.AtletaPage;
import com.achron.pages.BibliotecaPage;
import com.achron.pages.LoginPage;
import com.achron.pages.RutasPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;

/**
 * Las rutas del sitio: cuales piden sesion, cuales son de cada rol, los accesos /login y /client-login,
 * la pagina que no existe y el constructor de rutinas. La lista de rutas sale del codigo publico del sitio.
 */
@Tag("rutas")
@Modulo(nombre = "Rutas", titulo = "Rutas, permisos por rol y accesos", ruta = "")
class RutasTest extends BaseTest {

    private static CuentaDePrueba coach;
    private static CuentaDePrueba atleta;

    @BeforeAll
    static void crearCuentas() {
        coach = CuentaDePrueba.crearCoach("rutasc", 13, Configuracion.texto("rutas.coach") + " " + Corrida.numero());
        atleta = CuentaDePrueba.crearAtleta("rutasa", 14, Configuracion.texto("rutas.atleta") + " " + Corrida.numero(), coach.nombre, a -> { });
    }

    // quien: - (sin sesion), COACH o ATLETA. esperado: la ruta en la que debe quedar
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "RUT-01 | Sin sesion   | Cartera pide iniciar sesion                        | ruta        | -      | wallet            | /auth             | Alta",
            "RUT-02 | Sin sesion   | Asesorados pide iniciar sesion                     | ruta        | -      | clients           | /auth             | Alta",
            "RUT-03 | Sin sesion   | Coaches pide iniciar sesion                        | ruta        | -      | coaches           | /auth             | Alta",
            "RUT-04 | Sin sesion   | Ejercicios pide iniciar sesion                     | ruta        | -      | exercises         | /auth             | Alta",
            "RUT-05 | Sin sesion   | Objetivos pide iniciar sesion                      | ruta        | -      | objectives        | /auth             | Alta",
            "RUT-06 | Sin sesion   | Chat pide iniciar sesion                           | ruta        | -      | chat              | /auth             | Alta",
            "RUT-07 | Sin sesion   | Asistente IA pide iniciar sesion                   | ruta        | -      | assistant         | /auth             | Alta",
            "RUT-08 | Sin sesion   | Perfil del coach pide iniciar sesion               | ruta        | -      | profile           | /auth             | Alta",
            "RUT-09 | Sin sesion   | Nueva rutina pide iniciar sesion                   | ruta        | -      | routines/new      | /auth             | Alta",
            "RUT-10 | Sin sesion   | Constructor de rutinas pide iniciar sesion         | ruta        | -      | routine-builder/1 | /auth             | Alta",
            "RUT-11 | Sin sesion   | Panel del atleta pide iniciar sesion               | ruta        | -      | client-dashboard  | /auth             | Alta",
            "RUT-12 | Sin sesion   | Perfil del atleta pide iniciar sesion              | ruta        | -      | client-profile    | /auth             | Alta",
            "RUT-13 | Sin sesion   | Pago de membresia pide iniciar sesion              | ruta        | -      | client-payment    | /auth             | Media",
            "RUT-14 | Sin sesion   | Precios no muestra el menu privado del coach       | sinMenu     | -      | pricing           | -                 | Media",
            "RUT-15 | Rol coach    | El coach no entra al panel del atleta              | ruta        | COACH  | client-dashboard  | /dashboard        | Alta",
            "RUT-16 | Rol coach    | El coach no entra al perfil del atleta             | ruta        | COACH  | client-profile    | /dashboard        | Alta",
            "RUT-17 | Rol coach    | El coach no entra al pago del atleta               | ruta        | COACH  | client-payment    | /dashboard        | Media",
            "RUT-18 | Rol coach    | Con sesion, la pagina de acceso lleva a su panel   | ruta        | COACH  | auth              | /dashboard        | Media",
            "RUT-19 | Rol atleta   | El atleta no entra a Asesorados                    | ruta        | ATLETA | clients           | /client-dashboard | Alta",
            "RUT-20 | Rol atleta   | El atleta no entra a Coaches                       | ruta        | ATLETA | coaches           | /client-dashboard | Alta",
            "RUT-21 | Rol atleta   | El atleta no entra a la biblioteca del coach       | ruta        | ATLETA | exercises         | /client-dashboard | Alta",
            "RUT-22 | Rol atleta   | El atleta no entra a Objetivos                     | ruta        | ATLETA | objectives        | /client-dashboard | Alta",
            "RUT-23 | Rol atleta   | El atleta no entra al perfil del coach             | ruta        | ATLETA | profile           | /client-dashboard | Alta",
            "RUT-24 | Rol atleta   | El atleta no entra a crear rutinas                 | ruta        | ATLETA | routines/new      | /client-dashboard | Alta",
            "RUT-25 | Rol atleta   | El atleta no entra al constructor de rutinas       | ruta        | ATLETA | routine-builder/1 | /client-dashboard | Alta",
            "RUT-26 | Rol atleta   | Con sesion, la pagina de acceso lleva a su panel   | ruta        | ATLETA | auth              | /client-dashboard | Media",
            "RUT-27 | Pagina 404   | Una pagina que no existe lo avisa y deja volver    | noExiste    | -      | no-existe-123     | /                 | Baja",
            "RUT-28 | Pagina 404   | El aviso de pagina inexistente esta en espanol     | noExisteEs  | -      | no-existe-123     | -                 | Baja",
            "RUT-29 | Accesos      | El coach entra por /login                          | acceso      | COACH  | login             | /dashboard        | Alta",
            "RUT-30 | Accesos      | /login no deja entrar con la contrasena incorrecta | accesoMal   | COACH  | login             | -                 | Alta",
            "RUT-31 | Accesos      | El atleta entra por /client-login                  | acceso      | ATLETA | client-login      | /client-dashboard | Alta",
            "RUT-32 | Accesos      | /client-login no deja entrar con la contrasena incorrecta | accesoMal | ATLETA | client-login    | -                 | Alta",
            "RUT-33 | Nueva rutina | Crear una rutina desde /routines/new               | rutina      | COACH  | routines/new      | rutas.rutina      | Alta",
            "RUT-34 | Nueva rutina | Sin nombre no se crea la rutina                    | rutinaVacia | COACH  | routines/new      | -                 | Media",
            "RUT-35 | Nueva rutina | Una rutina que no existe no se queda cargando      | constructor | COACH  | routine-builder/1 | -                 | Media"
    })
    void rutas(String id, String categoria, String descripcion, String accion, String quien, String ruta, String esperado, String severidad) {
        nuevoCaso(id, categoria, descripcion, severidad, "ruta /" + ruta + ", " + (quien.equals("-") ? "sin sesion" : quien.toLowerCase()));
        RutasPage rutas = new RutasPage(web);
        boolean entraAntes = !accion.startsWith("acceso");
        if (entraAntes && quien.equals("COACH")) new LoginPage(web).iniciarSesion(coach.email, coach.clave);
        if (entraAntes && quien.equals("ATLETA")) new AtletaPage(web).iniciarSesion(atleta.email, atleta.clave);

        switch (accion) {
            case "ruta": {
                String quedo = rutas.abrirYEsperar(ruta, esperado);
                verificar("A donde lleva /" + ruta, esperado, quedo, quedo.equals(esperado));
                break;
            }

            case "sinMenu": {
                web.irA(ruta);
                web.esperarTexto("Planes diseñados para ti");
                boolean menu = web.todos(By.cssSelector("a[href='/wallet']")).stream().anyMatch(e -> e.isDisplayed());
                verificar("Sin sesion no se ve el menu del coach", "sin menu (Cartera, Asesorados...)", menu ? "muestra el menu" : "sin menu", !menu);
                break;
            }

            case "noExiste": {
                web.irA(ruta);
                boolean avisa = web.esperarTexto("404");
                verificar("Avisa que no existe", "404", avisa ? "404" : "no avisa", avisa);
                web.clic(By.cssSelector("a[href='/']"));
                boolean volvio = web.esperarQue("respuesta", d -> rutas.rutaActual().equals(esperado));
                verificar("El enlace vuelve al inicio", esperado, rutas.rutaActual(), volvio);
                break;
            }

            case "noExisteEs": {
                web.irA(ruta);
                web.esperarTexto("404");
                String texto = web.textoPagina().replace("\n", " ");
                verificar("Texto en el idioma del sitio", "un mensaje en espanol", texto, !texto.contains("Page not found") && !texto.contains("Return to Home"));
                break;
            }

            case "acceso": {
                web.irA(ruta);
                rutas.entrar(quien.equals("COACH") ? coach.email : atleta.email, quien.equals("COACH") ? coach.clave : atleta.clave);
                boolean entro = web.esperarQue("larga", d -> rutas.rutaActual().equals(esperado));
                verificar("Entra a su panel", esperado, rutas.rutaActual() + (entro ? "" : " | aviso: " + siVacio(web.esperarAviso(), "ninguno")), entro);
                break;
            }

            case "accesoMal": {
                web.irA(ruta);
                rutas.entrar(quien.equals("COACH") ? coach.email : atleta.email, Configuracion.texto("sesion.contrasena.incorrecta"));
                String aviso = rutas.avisoDeAcceso();
                String texto = web.texto(By.cssSelector("[role='alert'], .text-destructive, p[class*='text-red']"));
                verificar("No entra", "sigue en /" + ruta, rutas.rutaActual(), rutas.rutaActual().equals("/" + ruta));
                verificar("Dice que los datos no son correctos", "un mensaje de error", siVacio((aviso + " " + texto).trim(), "ningun mensaje"), !(aviso + texto).isBlank());
                break;
            }

            case "rutina": {
                String nombre = Configuracion.texto(esperado) + " " + Corrida.numero();
                caso.dato("rutina '" + nombre + "'");
                web.irA(ruta);
                rutas.nombreDeRutina(nombre);
                rutas.agregarBloque();
                rutas.guardarRutina();
                String aviso = web.esperarAviso();
                verificar("Sin mensaje de error", "un aviso de exito", siVacio(aviso, "ningun aviso"), !aviso.isEmpty() && !aviso.toLowerCase().contains("error"));
                web.irA("exercises");
                BibliotecaPage biblioteca = new BibliotecaPage(web);
                biblioteca.abrirPestana("Rutina");
                boolean aparece = biblioteca.esperarEnPestana(nombre);
                verificar("Aparece en la biblioteca", nombre, aparece ? "aparece" : "no aparece", aparece);
                break;
            }

            case "rutinaVacia": {
                web.irA(ruta);
                rutas.agregarBloque();
                if (!rutas.guardarRutinaHabilitado()) {
                    verificar("No deja guardar sin nombre", "boton deshabilitado o mensaje", "boton deshabilitado", true);
                    break;
                }
                rutas.guardarRutina();
                String aviso = web.esperarAviso();
                String error = web.texto(By.cssSelector("main p[class*='destructive'], main p[class*='text-red']"));
                verificar("No crea la rutina y pide el nombre", "un mensaje que pida el nombre", siVacio((aviso + " " + error).trim(), "ningun mensaje"),
                        !error.isBlank() || aviso.toLowerCase().contains("error") || aviso.toLowerCase().contains("nombre"));
                break;
            }

            case "constructor": {
                web.irA(ruta);
                web.esperarTexto("Crear Nueva Rutina");
                boolean termino = web.esperarQue("larga", d -> !web.textoPagina().contains("Cargando constructor"));
                verificar("Termina de cargar o avisa que no existe", "un mensaje o el formulario", termino ? "termino de cargar" : "sigue en 'Cargando constructor...'", termino);
                break;
            }

            default:
                throw new IllegalArgumentException("Accion desconocida: " + accion);
        }
        comprobarTodo();
    }
}
