package com.achron.tests;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import com.achron.core.Acciones;
import com.achron.core.BaseTest;
import com.achron.core.CuentaDePrueba;
import com.achron.core.Modulo;
import com.achron.pages.EjerciciosPage;
import com.achron.pages.LoginPage;
import com.achron.pages.MenuLateral;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Pestaña "Movimientos" de la biblioteca. El sitio no permite borrar ejercicios: los que crea
 * esta clase ("ZZ Prueba Auto N...") se quedan en la biblioteca.
 */
@Tag("ejercicios")
@Modulo(nombre = "Ejercicios", titulo = "Ejercicios (Movimientos)", ruta = "exercises")
class EjerciciosTest extends BaseTest {

    private static CuentaDePrueba cuenta;
    private static String ejercicioCreado;

    @BeforeAll
    static void crearCuenta() {
        cuenta = CuentaDePrueba.crearCoach("ejercicios", 4);
    }

    // valor "ejercicio.xxx" = de datos.properties; "(300 caracteres)" y "(solo simbolos)" cambian en cada corrida
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "EJE-01 | Navegacion          | El menu Ejercicios abre la biblioteca            | navegar             | -          | ''                    | Alta",
            "EJE-02 | Listado             | La lista muestra ejercicios con musculo y equipo | listar              | -          | ''                    | Alta",
            "EJE-03 | Busqueda y filtro   | Buscar muestra solo coincidencias                | buscar              | -          | ejercicio.busqueda    | Media",
            "EJE-04 | Busqueda y filtro   | Buscar en MAYUSCULAS da el mismo resultado       | buscar              | -          | SENTADILLA            | Media",
            "EJE-05 | Busqueda y filtro   | Busqueda sin resultados muestra un mensaje       | buscarSinResultados | -          | zzz-no-existe-123     | Media",
            "EJE-06 | Busqueda y filtro   | Filtrar por categoria                            | filtrar             | -          | ejercicio.categoria   | Media",
            "EJE-07 | Crear valido        | Crear ejercicio con datos validos                | crear               | -          | ejercicio.nombre      | Alta",
            "EJE-08 | Crear validaciones  | Nombre vacio                                     | crearInvalido       | name       | ''                    | Alta",
            "EJE-09 | Crear validaciones  | Musculo vacio                                    | crearInvalido       | muscle     | ''                    | Alta",
            "EJE-10 | Crear validaciones  | Equipo vacio                                     | crearInvalido       | equip      | ''                    | Alta",
            "EJE-11 | Crear validaciones  | Nombre solo con simbolos                         | crearInvalido       | name       | (solo simbolos)       | Alta",
            "EJE-12 | Crear validaciones  | Nombre de 300 caracteres                         | crearInvalido       | name       | (300 caracteres)      | Alta",
            "EJE-13 | Crear validaciones  | Nombre repetido (ya existe)                      | crearInvalido       | name       | ejercicio.existente   | Alta",
            "EJE-14 | Cancelar            | Cancelar la creacion no agrega el ejercicio      | cancelarCrear       | -          | ejercicio.nombre      | Media",
            "EJE-15 | Detalle             | Detalles muestra la informacion del ejercicio    | detalles            | -          | ejercicio.existente   | Media",
            "EJE-16 | Editar              | Editar descripcion y video del ejercicio creado  | editar              | -          | ejercicio.descripcion | Alta",
            "EJE-17 | Editar              | El boton guardar del editor tiene texto legible  | textoBotonEditar    | -          | ejercicio.existente   | Baja",
            "EJE-18 | Editar validaciones | URL de video invalida                            | editarInvalido      | edit-video | esto no es una url    | Alta"
    })
    void ejercicios(String id, String categoria, String descripcion, String accion, String campo, String valor, String severidad) {
        valor = valorDelCaso(dato(valor, "ejercicio"));
        nuevoCaso(id, categoria, descripcion, severidad, campo.equals("-") ? "'" + valor + "'" : campo + " = '" + valor + "'");
        new LoginPage(web).iniciarSesion(cuenta.email, cuenta.clave);
        new MenuLateral(web).ir("exercises");
        EjerciciosPage ejercicios = new EjerciciosPage(web);
        ejercicios.esperarLista();
        String musculo = Configuracion.texto("ejercicio.musculo");
        String equipo = Configuracion.texto("ejercicio.equipo");

        switch (accion) {
            case "navegar": {
                String pagina = web.textoPagina();
                verificar("Abre la seccion", "url con /exercises", web.url(), web.url().contains("/exercises"));
                verificar("Titulo de la pagina", "Biblioteca de entrenamiento", pagina.contains("Biblioteca de entrenamiento") ? "aparece" : "no aparece",
                        pagina.contains("Biblioteca de entrenamiento"));
                verificar("Boton Crear Ejercicio", "visible", web.existe(By.xpath("//button[normalize-space()='Crear Ejercicio']")) ? "visible" : "no visible",
                        web.existe(By.xpath("//button[normalize-space()='Crear Ejercicio']")));
                verificar("Buscador", "visible", web.existe(EjerciciosPage.BUSCADOR) ? "visible" : "no visible", web.existe(EjerciciosPage.BUSCADOR));
                break;
            }

            case "listar": {
                List<WebElement> tarjetas = ejercicios.tarjetas();
                String primera = tarjetas.isEmpty() ? "" : tarjetas.get(0).getText();
                verificar("Hay ejercicios en la lista", "1 o mas", tarjetas.size() + " ejercicios", !tarjetas.isEmpty());
                verificar("La tarjeta muestra nombre, musculo y equipo", "3 datos", primera.replace("\n", " | "), primera.split("\n").length >= 3);
                break;
            }

            case "buscar": {
                String buscado = valor.toLowerCase();
                ejercicios.buscar(valor);
                List<String> nombres = ejercicios.nombresCuando(l -> !l.isEmpty() && l.stream().allMatch(n -> n.toLowerCase().contains(buscado)));
                String noCoinciden = nombres.stream().filter(n -> !n.toLowerCase().contains(buscado)).collect(Collectors.joining("; "));
                verificar("Hay resultados", "1 o mas", nombres.size() + " resultados: " + nombres, !nombres.isEmpty());
                verificar("Todos contienen el texto buscado", "solo nombres con '" + valor + "'", noCoinciden.isEmpty() ? "todos coinciden" : "no coinciden: " + noCoinciden,
                        noCoinciden.isEmpty());
                break;
            }

            case "buscarSinResultados": {
                ejercicios.buscar(valor);
                List<String> nombres = ejercicios.nombresCuando(List::isEmpty);
                boolean mensaje = web.esperarTexto("No se encontraron");
                verificar("Sin resultados", "0 ejercicios", nombres.size() + " ejercicios", nombres.isEmpty());
                verificar("Mensaje de lista vacia", "'No se encontraron movimientos'", mensaje ? "aparece" : "no aparece", mensaje);
                break;
            }

            case "filtrar": {
                String filtro = valor;
                ejercicios.filtrarPorCategoria(filtro);
                web.esperarQue("corta", d -> ejercicios.tarjetas().stream().allMatch(t -> musculoDe(t).equals(filtro)));
                List<WebElement> tarjetas = ejercicios.tarjetas();
                String otros = tarjetas.stream().filter(t -> !musculoDe(t).equals(filtro)).map(t -> t.getText().split("\n")[0]).collect(Collectors.joining("; "));
                verificar("Hay resultados", "1 o mas", tarjetas.size() + " ejercicios", !tarjetas.isEmpty());
                verificar("Todos son de la categoria", "solo '" + filtro + "'", otros.isEmpty() ? "todos son '" + filtro + "'" : "otros: " + otros, otros.isEmpty());
                break;
            }

            case "crear": {
                String nombre = valor + " " + Corrida.numero();
                caso.dato("nombre = '" + nombre + "', musculo = '" + musculo + "', equipo = '" + equipo + "'");
                ejercicios.abrirCrear();
                ejercicios.llenarCreacion(nombre, musculo, equipo);
                ejercicios.botonDelDialogo("Guardar Ejercicio");
                String aviso = web.esperarAviso();
                verificar("Mensaje de creado", "aviso 'Ejercicio creado'", siVacio(aviso, "ningun aviso"), aviso.contains("Ejercicio creado"));
                verificar("La ventana se cierra", "ventana cerrada", web.desaparece(Acciones.DIALOGO) ? "cerrada" : "sigue abierta", !web.hayDialogoAbierto());
                boolean enLista = ejercicios.apareceEnLista(nombre);
                verificar("Aparece en la lista sin recargar", nombre, enLista ? "aparece" : "no aparece", enLista);
                ejercicioCreado = nombre;
                ejercicios.recargar();
                verificar("Persistencia al recargar", nombre + " sigue en la lista", ejercicios.apareceEnLista(nombre) ? "aparece" : "no aparece",
                        ejercicios.cuantosSeLlaman(nombre) > 0);
                break;
            }

            case "crearInvalido": {
                String nombreValido = Configuracion.texto("ejercicio.nombre") + " invalido " + Corrida.numero() + " " + id;
                String nombreUsado = campo.equals("name") ? valor : nombreValido;
                int antes = ejercicios.cuantosSeLlaman(nombreUsado);
                ejercicios.abrirCrear();
                ejercicios.llenarCreacion(nombreValido, musculo, equipo);
                web.escribir(By.id(campo), valor);
                ejercicios.botonDelDialogo("Guardar Ejercicio");
                String aviso = web.esperarAviso();
                boolean creado = aviso.contains("Ejercicio creado");
                String errores = String.join(" ", web.mensajeNavegador(By.id(campo)), web.texto(Acciones.ERRORES_DIALOGO), creado ? "" : aviso).trim();
                verificar("No se crea", "sin aviso 'Ejercicio creado'", siVacio(aviso, "ningun aviso"), !creado);
                verificar("Mensaje de error visible", "un texto que explique el error", siVacio(errores, "ningun mensaje"), !errores.isBlank());
                ejercicios.recargar();
                if (!nombreUsado.isBlank()) {
                    int despues = ejercicios.cuantosSeLlaman(nombreUsado);
                    verificar("No se agrega a la lista", antes + " ejercicio(s) con ese nombre (igual que antes)", despues + " ejercicio(s)", despues == antes);
                }
                break;
            }

            case "cancelarCrear": {
                String nombre = valor + " cancelado " + Corrida.numero();
                caso.dato("nombre = '" + nombre + "'");
                ejercicios.abrirCrear();
                ejercicios.llenarCreacion(nombre, musculo, equipo);
                ejercicios.botonDelDialogo("Cancelar");
                verificar("La ventana se cierra", "ventana cerrada", web.desaparece(Acciones.DIALOGO) ? "cerrada" : "sigue abierta", !web.hayDialogoAbierto());
                ejercicios.recargar();
                verificar("No se agrega a la lista", "0 ejercicios con ese nombre", ejercicios.cuantosSeLlaman(nombre) + " ejercicio(s)", ejercicios.cuantosSeLlaman(nombre) == 0);
                break;
            }

            case "detalles": {
                String urlAntes = web.url();
                String tarjetaAntes = ejercicios.tarjeta(valor).getText();
                ejercicios.verDetalles(valor);
                boolean dialogo = web.aparece(Acciones.DIALOGO);
                boolean otraPagina = !web.url().equals(urlAntes);
                boolean cambioTarjeta = !otraPagina && !ejercicios.tarjeta(valor).getText().equals(tarjetaAntes);
                String obtenido = dialogo ? "se abrio una ventana: " + web.texto(Acciones.DIALOGO) : otraPagina ? "fue a " + web.url()
                        : cambioTarjeta ? "la tarjeta cambio" : "no paso nada";
                verificar("Se abre el detalle", "una ventana, pagina o seccion con el detalle", obtenido, dialogo || otraPagina || cambioTarjeta);
                break;
            }

            case "editar": {
                requiereEjercicioCreado();
                String video = Configuracion.texto("ejercicio.video");
                caso.dato(ejercicioCreado + ": descripcion = '" + valor + "', video = '" + video + "'");
                ejercicios.abrirEditar(ejercicioCreado);
                web.escribir(By.id("edit-description"), valor);
                web.escribir(By.id("edit-video"), video);
                ejercicios.guardarEdicion();
                String aviso = web.esperarAviso();
                verificar("Mensaje de guardado", "un aviso de confirmacion", siVacio(aviso, "ningun aviso"), !aviso.isEmpty());
                verificar("La ventana se cierra", "ventana cerrada", web.desaparece(Acciones.DIALOGO) ? "cerrada" : "sigue abierta", !web.hayDialogoAbierto());
                ejercicios.recargar();
                String tarjeta = ejercicios.tarjeta(ejercicioCreado).getText();
                verificar("La tarjeta muestra la descripcion", valor, tarjeta.contains(valor) ? valor : tarjeta.replace("\n", " | "), tarjeta.contains(valor));
                ejercicios.abrirEditar(ejercicioCreado);
                String descripcionGuardada = web.valor(By.id("edit-description"));
                String videoGuardado = web.valor(By.id("edit-video"));
                verificar("Descripcion guardada", valor, descripcionGuardada, descripcionGuardada.equals(valor));
                verificar("Video guardado", video, videoGuardado, videoGuardado.equals(video));
                break;
            }

            case "textoBotonEditar": {
                ejercicios.abrirEditar(valor);
                String texto = ejercicios.textoBotonGuardarEdicion();
                caso.dato("editor de '" + valor + "'");
                verificar("Texto del boton guardar", "un texto en español (sin claves internas como 'exercises_page.')", "'" + texto + "'",
                        !texto.isBlank() && !texto.contains("_"));
                break;
            }

            case "editarInvalido": {
                requiereEjercicioCreado();
                caso.dato(ejercicioCreado + ": " + campo + " = '" + valor + "'");
                ejercicios.abrirEditar(ejercicioCreado);
                String antes = web.valor(By.id(campo));
                web.escribir(By.id(campo), valor);
                ejercicios.guardarEdicion();
                String aviso = web.esperarAviso();
                String errores = (web.mensajeNavegador(By.id(campo)) + " " + web.texto(Acciones.ERRORES_DIALOGO)).trim();
                verificar("No se guarda", "sin aviso de guardado", siVacio(aviso, "ningun aviso"), aviso.isEmpty());
                verificar("Mensaje de error visible", "un texto que explique el error", siVacio(errores, "ningun mensaje"), !errores.isBlank());
                ejercicios.recargar();
                ejercicios.abrirEditar(ejercicioCreado);
                String guardado = web.valor(By.id(campo));
                verificar("No guarda el dato invalido", "'" + antes + "' (el valor anterior)", "'" + guardado + "'", !guardado.equals(valor));
                break;
            }

            default:
                throw new IllegalArgumentException("Accion desconocida: " + accion);
        }
        comprobarTodo();
    }

    /** Valores invalidos que cambian en cada corrida: si se repitieran, el sitio los rechazaria por nombre repetido. */
    private static String valorDelCaso(String valor) {
        if (valor.equals("(300 caracteres)")) return ("ZZ" + Corrida.numero() + " ").repeat(100).substring(0, 300);
        if (valor.equals("(solo simbolos)")) return "@#$" + aSimbolos(Corrida.numero());
        return valor;
    }

    /** 0->!, 1->@, 2->#, ... para un nombre "solo simbolos" que no se repita. */
    private static String aSimbolos(int numero) {
        StringBuilder texto = new StringBuilder();
        for (char c : String.valueOf(numero).toCharArray()) {
            texto.append("!@#$%^&*()".charAt(c - '0'));
        }
        return texto.toString();
    }

    private static String musculoDe(WebElement tarjeta) {
        String[] lineas = tarjeta.getText().split("\n");
        return lineas.length > 1 ? lineas[1] : "";
    }

    private static void requiereEjercicioCreado() {
        if (ejercicioCreado == null) {
            throw new IllegalStateException("Este caso necesita el ejercicio creado en EJE-07");
        }
    }
}
