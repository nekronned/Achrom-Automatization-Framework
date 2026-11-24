package com.achron.tests;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import com.achron.core.Acciones;
import com.achron.core.ArchivosDePrueba;
import com.achron.core.BaseTest;
import com.achron.core.CuentaDePrueba;
import com.achron.core.Modulo;
import com.achron.pages.AsesoradosPage;
import com.achron.pages.AtletaPage;
import com.achron.pages.CarteraPage;
import com.achron.pages.LoginPage;
import com.achron.pages.MenuLateral;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;

/**
 * Lo que hace el coach con sus asesorados. Al empezar se crea un coach con nombre unico y dos atletas
 * que lo eligen: el atleta A registra un pago y le escribe por el chat; B y C registran otro pago cada uno.
 * Los pagos son de prueba: solo se sube una imagen como comprobante, no se mueve dinero.
 */
@Tag("asesorados")
@Modulo(nombre = "Asesorados", titulo = "Coach con asesorados (comprobantes, chat, sesiones y rutinas)", ruta = "clients")
class AsesoradosTest extends BaseTest {

    private static final int ATLETAS = 3; // atletas que se crean en prepararCoachYAtletas
    private static CuentaDePrueba coach;
    private static String atletaA;
    private static String atletaB;
    private static String atletaC;
    private static String mensajeAtleta;

    @BeforeAll
    static void prepararCoachYAtletas() {
        int corrida = Corrida.numero();
        coach = CuentaDePrueba.crearCoach("asesorados", 7, Configuracion.texto("asesorados.coach") + " " + corrida);
        atletaA = Configuracion.texto("asesorados.atleta.a") + " " + corrida;
        atletaB = Configuracion.texto("asesorados.atleta.b") + " " + corrida;
        mensajeAtleta = Configuracion.texto("asesorados.mensaje.atleta") + " " + corrida;
        String monto = Configuracion.texto("asesorados.pago");

        CuentaDePrueba.crearAtleta("atletaa", 8, atletaA, coach.nombre, atleta -> {
            atleta.registrarPago(monto, ArchivosDePrueba.imagenPng());
            atleta.escribirAlCoach(mensajeAtleta);
        });
        CuentaDePrueba.crearAtleta("atletab", 9, atletaB, coach.nombre,
                atleta -> atleta.registrarPago(monto, ArchivosDePrueba.imagenPng()));
        atletaC = Configuracion.texto("asesorados.atleta.c") + " " + corrida;
        CuentaDePrueba.crearAtleta("atletac", 10, atletaC, coach.nombre,
                atleta -> atleta.registrarPago(monto, ArchivosDePrueba.imagenPng()));
    }

    // atleta: A o B. valor: dato del caso ("asesorados.xxx" = de datos.properties)
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "ASE-01 | Lista de asesorados   | El atleta que eligio al coach aparece en su lista   | enLista          | A | -                          | Alta",
            "ASE-02 | Lista de asesorados   | Buscar al asesorado por su nombre                    | buscar           | A | -                          | Media",
            "ASE-03 | Detalle del asesorado | Ver perfil completo abre el detalle                  | detalle          | A | -                          | Alta",
            "ASE-04 | Detalle del asesorado | El panel cuenta a los asesorados del coach           | totalPanel       | A | -                          | Media",
            "ASE-05 | Sesiones              | Programar una sesion con fecha y hora                | sesion           | A | asesorados.sesion.notas    | Alta",
            "ASE-06 | Sesiones              | Programar una sesion sin fecha                       | sesionSinFecha   | A | asesorados.sesion.notas    | Media",
            "ASE-07 | Metodologias          | Asignar una metodologia al asesorado                 | metodologia      | A | asesorados.metodologia     | Alta",
            "ASE-08 | Metodologias          | Confirmar sin elegir metodologia                     | metodologiaVacia | A | -                          | Media",
            "ASE-09 | Rutinas               | Asignar rutina abre la biblioteca en modo asignacion | asignarRutina    | A | -                          | Alta",
            "ASE-10 | Rutinas               | Agregar un ejercicio al plan del asesorado           | agregarEjercicio | A | asesorados.ejercicio       | Alta",
            "ASE-11 | Historial             | El historial muestra lo asignado al asesorado        | historial        | A | asesorados.metodologia     | Media",
            "ASE-12 | Comprobantes          | El comprobante del atleta llega a la bandeja         | bandeja          | A | asesorados.pago            | Alta",
            "ASE-13 | Comprobantes          | Ver comprobante muestra el detalle del pago          | verComprobante   | A | asesorados.pago            | Alta",
            "ASE-14 | Comprobantes          | Aprobar el comprobante                               | aprobar          | A | -                          | Alta",
            "ASE-15 | Comprobantes          | Rechazar el comprobante                              | rechazar         | B | -                          | Alta",
            "ASE-16 | Comprobantes          | Los totales de la cartera cambian al aprobar         | totalesCartera   | A | -                          | Alta",
            "ASE-17 | Mensualidades         | El asesorado al dia aparece en Control de Mensualidades | mensualidades | A | -                          | Media",
            "ASE-18 | Chat                  | El coach ve el mensaje del atleta                    | verMensaje       | A | -                          | Alta",
            "ASE-19 | Chat                  | El coach responde y la respuesta queda guardada      | responder        | A | asesorados.mensaje.coach   | Alta",
            "ASE-20 | Chat                  | Un mensaje vacio no se envia                         | mensajeVacio     | A | -                          | Baja",
            "ASE-21 | Comprobantes          | Rechazar un comprobante sin escribir el motivo       | rechazarSinMotivo | C | -                         | Media",
            "ASE-22 | Sesiones              | Arrepentirse de cancelar deja la sesion programada   | noCancelarSesion | A | -                          | Media",
            "ASE-23 | Sesiones              | Cancelar la sesion la saca de la agenda              | cancelarSesion   | A | -                          | Alta",
            "ASE-24 | Sesiones              | Una sesion en una fecha pasada no se programa        | sesionPasada     | B | asesorados.sesion.notas    | Media",
            "ASE-25 | Objetivos             | El radar muestra a los asesorados del coach          | radar            | A | -                          | Media",
            "ASE-26 | Filtros               | El filtro Activos muestra al asesorado activo        | filtroEstado     | A | Activos                    | Media",
            "ASE-27 | Filtros               | El filtro Inactivos no muestra al asesorado activo   | filtroEstado     | A | Inactivos                  | Media",
            "ASE-28 | Cartera               | El filtro Aprobado solo muestra pagos aprobados      | filtroCartera    | A | Aprobado                   | Media",
            "ASE-29 | Cartera               | Buscar por asesorado en el historial                 | buscarCartera    | A | -                          | Media",
            "ASE-30 | Notificaciones        | La campana avisa del pago que registro el atleta     | campana          | A | asesorados.pago            | Media",
            "ASE-31 | Rutinas               | Crear un entrenamiento y asignarlo al asesorado      | rutinaCompleta   | A | asesorados.rutina          | Alta",
            "ASE-32 | Detalle del asesorado | El coach puede actualizar el peso del asesorado      | pesoDelCoach     | A | -                          | Media"
    })
    void asesorados(String id, String categoria, String descripcion, String accion, String atletaCsv, String valorCsv, String severidad) {
        String atleta = atletaCsv.equals("A") ? atletaA : atletaCsv.equals("B") ? atletaB : atletaC;
        String valor = dato(valorCsv, "asesorados");
        nuevoCaso(id, categoria, descripcion, severidad, "asesorado '" + atleta + "'" + (valor.equals("-") ? "" : ", dato '" + valor + "'"));
        new LoginPage(web).iniciarSesion(coach.email, coach.clave);
        AsesoradosPage asesorados = new AsesoradosPage(web);
        CarteraPage cartera = new CarteraPage(web);

        switch (accion) {
            case "enLista": {
                asesorados.abrirLista();
                boolean esta = asesorados.apareceEnLista(atleta);
                verificar("Aparece en la lista", atleta, esta ? asesorados.fila(atleta) : "no aparece", esta);
                verificar("Estado del asesorado", "Activo", siVacio(asesorados.fila(atleta), "sin fila"), asesorados.fila(atleta).contains("Activo"));
                break;
            }

            case "buscar": {
                asesorados.abrirLista();
                asesorados.apareceEnLista(atleta);
                web.escribir(By.cssSelector("main input[placeholder^='Buscar']"), atleta);
                boolean soloEl = web.esperarQue("corta", d -> web.todos(By.cssSelector("main tbody tr")).size() == 1 && !asesorados.fila(atleta).isEmpty());
                verificar("La busqueda encuentra al asesorado", "una fila con '" + atleta + "'", web.todos(By.cssSelector("main tbody tr")).size() + " fila(s)", soloEl);
                break;
            }

            case "detalle": {
                asesorados.abrirLista();
                asesorados.apareceEnLista(atleta);
                asesorados.verPerfilCompleto(atleta);
                String pagina = web.textoPagina();
                verificar("Abre el detalle", "url /clients/<id>", web.url(), web.url().matches(".*/clients/.+"));
                verificar("Muestra el nombre", "'Perfil de " + atleta + "'", pagina.contains("Perfil de " + atleta) ? "aparece" : "no aparece", pagina.contains("Perfil de " + atleta));
                verificar("Muestra las acciones de gestion", "Historial, Asignar rutina, Asignar Metodología", pagina.contains("Asignar rutina") ? "aparecen" : "no aparecen",
                        pagina.contains("Historial") && pagina.contains("Asignar rutina") && pagina.contains("Asignar Metodología"));
                break;
            }

            case "totalPanel": {
                // el panel carga los numeros despues de mostrarse: se espera a que deje de decir 0
                web.esperarQue("respuesta", d -> web.textoPagina().replace("\n", " ").replace(" ", "").contains("Asesoradostotales:" + ATLETAS));
                String pagina = web.textoPagina().replace("\n", " ");
                int i = pagina.indexOf("Asesorados totales");
                String obtenido = i < 0 ? "no aparece" : pagina.substring(i, Math.min(pagina.length(), i + 40));
                verificar("Asesorados totales", "Asesorados totales: " + ATLETAS, obtenido, obtenido.replace(" ", "").startsWith("Asesoradostotales:" + ATLETAS));
                break;
            }

            case "sesion":
            case "sesionSinFecha": {
                abrirDetalle(asesorados, atleta);
                asesorados.boton("Programar nueva sesión");
                String fecha = accion.equals("sesion") ? LocalDate.now().plusDays(Configuracion.numero("asesorados.sesion.dias")).toString() : "";
                caso.dato("fecha '" + fecha + "', hora '" + Configuracion.texto("asesorados.sesion.hora") + "', notas '" + valor + "'");
                asesorados.llenarSesion(fecha, Configuracion.texto("asesorados.sesion.hora"), valor);
                if (!asesorados.botonDelDialogoHabilitado("Confirmar cita")) {
                    verificar("No deja confirmar", "no se programa sin fecha", "boton deshabilitado", accion.equals("sesionSinFecha"));
                    break;
                }
                asesorados.botonDelDialogo("Confirmar cita");
                String aviso = web.esperarAviso();
                if (accion.equals("sesion")) {
                    verificar("Mensaje de sesion programada", "un aviso de confirmacion", siVacio(aviso, "ningun aviso"), !aviso.isEmpty() && !aviso.toLowerCase().contains("error"));
                    web.recargar();
                    boolean agendada = web.esperarQue("respuesta", d -> !web.textoPagina().contains("Sin sesiones programadas"));
                    verificar("La sesion queda en la agenda", "agenda con 1 sesion o mas", agendada ? "aparece en la agenda" : "Sin sesiones programadas", agendada);
                } else {
                    String errores = (web.mensajeNavegador(By.cssSelector("[role='dialog'] input[type='date']")) + " " + aviso).trim();
                    verificar("No programa la sesion", "sin aviso de exito", siVacio(aviso, "ningun aviso"), !aviso.toLowerCase().contains("program") || aviso.toLowerCase().contains("error"));
                    verificar("Mensaje que pida la fecha", "un texto que pida la fecha", siVacio(errores, "ningun mensaje"), !errores.isEmpty());
                }
                break;
            }

            case "metodologia":
            case "metodologiaVacia": {
                abrirDetalle(asesorados, atleta);
                asesorados.boton("Asignar Metodología");
                if (accion.equals("metodologia")) {
                    asesorados.elegirMetodologia(valor);
                    asesorados.botonDelDialogo("Confirmar Asignación");
                    String aviso = web.esperarAviso();
                    verificar("Mensaje de asignacion", "un aviso de confirmacion", siVacio(aviso, "ningun aviso"), !aviso.isEmpty() && !aviso.toLowerCase().contains("error"));
                } else if (!asesorados.botonDelDialogoHabilitado("Confirmar Asignación")) {
                    verificar("No deja confirmar sin elegir", "boton deshabilitado o mensaje", "boton deshabilitado", true);
                } else {
                    asesorados.botonDelDialogo("Confirmar Asignación");
                    String aviso = web.esperarAviso();
                    verificar("No asigna nada", "sin aviso de exito", siVacio(aviso, "ningun aviso"), aviso.isEmpty() || aviso.toLowerCase().contains("selecciona") || aviso.toLowerCase().contains("error"));
                    verificar("Mensaje que pida elegir", "un texto que pida elegir la metodologia", siVacio(aviso, "ningun mensaje"), !aviso.isEmpty());
                }
                break;
            }

            case "asignarRutina": {
                abrirDetalle(asesorados, atleta);
                asesorados.boton("Asignar rutina");
                boolean modo = web.esperarTexto("Asignando a: " + atleta);
                verificar("Abre la biblioteca", "url /exercises?client=<id>", web.url(), web.url().contains("/exercises?client="));
                verificar("Indica a quien se asigna", "'Asignando a: " + atleta + "'", modo ? "aparece" : "no aparece", modo);
                break;
            }

            case "agregarEjercicio": {
                abrirDetalle(asesorados, atleta);
                asesorados.boton("Asignar rutina");
                web.esperarTexto("Asignando a:");
                asesorados.agregarEjercicio(valor);
                String aviso = web.esperarAviso();
                boolean abrioFormulario = web.hayDialogoAbierto();
                verificar("Agrega el ejercicio al plan", "un aviso o una ventana para completar la rutina",
                        abrioFormulario ? "se abrio una ventana: " + asesorados.textoDialogo() : siVacio(aviso, "no paso nada"),
                        abrioFormulario || (!aviso.isEmpty() && !aviso.toLowerCase().contains("error")));
                break;
            }

            case "historial": {
                abrirDetalle(asesorados, atleta);
                asesorados.boton("Historial");
                web.esperarQue("respuesta", d -> web.hayDialogoAbierto() && !asesorados.textoDialogo().contains("Cargando"));
                String historial = asesorados.textoDialogo();
                verificar("Abre el control de entrenamiento", "ventana 'Control de entrenamiento'", historial.contains("Control de entrenamiento") ? "se abrio" : siVacio(historial, "no se abrio"),
                        historial.contains("Control de entrenamiento"));
                verificar("Muestra la metodologia asignada", valor, historial.contains(valor) ? "aparece" : "no aparece (" + historial + ")", historial.contains(valor));
                break;
            }

            case "bandeja": {
                cartera.abrir();
                String tarjeta = cartera.comprobanteEnBandeja(atleta);
                verificar("El comprobante esta en la bandeja", atleta, siVacio(tarjeta, "no esta"), !tarjeta.isEmpty());
                verificar("Muestra el monto", "$" + valor, siVacio(tarjeta, "sin tarjeta"), tarjeta.contains(valor));
                break;
            }

            case "verComprobante": {
                cartera.abrir();
                cartera.verComprobante(atleta);
                String detalle = web.texto(Acciones.DIALOGO);
                verificar("Muestra el detalle del pago", "asesorado, monto y estado Pendiente", detalle,
                        detalle.contains(atleta) && detalle.contains(valor) && detalle.contains("Pendiente"));
                verificar("Permite aprobar o rechazar", "botones Aprobar y Rechazar", detalle.contains("Aprobar") && detalle.contains("Rechazar") ? "aparecen" : "no aparecen",
                        detalle.contains("Aprobar") && detalle.contains("Rechazar"));
                break;
            }

            case "aprobar":
            case "rechazar": {
                cartera.abrir();
                cartera.verComprobante(atleta);
                if (accion.equals("aprobar")) cartera.aprobar(); else cartera.rechazar(Configuracion.texto("asesorados.motivo.rechazo"));
                String aviso = web.esperarAviso();
                String esperado = accion.equals("aprobar") ? "Aprobado" : "Rechazado";
                web.recargar();
                web.esperarTexto("Historial de movimientos");
                boolean cambio = web.esperarQue("respuesta", d -> cartera.movimiento(atleta).contains(esperado));
                verificar("Mensaje de confirmacion", "un aviso de comprobante " + esperado.toLowerCase(), siVacio(aviso, "ningun aviso"), !aviso.isEmpty());
                verificar("Estado en el historial", esperado, siVacio(cartera.movimiento(atleta), "sin movimiento"), cambio);
                verificar("Sale de la bandeja", "el comprobante ya no esta por revisar", cartera.comprobanteEnBandeja(atleta).isEmpty() ? "ya no esta" : "sigue en la bandeja",
                        cartera.comprobanteEnBandeja(atleta).isEmpty());
                break;
            }

            case "rechazarSinMotivo": {
                cartera.abrir();
                cartera.verComprobante(atleta);
                cartera.abrirRechazo();
                if (!cartera.confirmarRechazoHabilitado()) {
                    verificar("No deja rechazar sin motivo", "boton deshabilitado o mensaje", "boton 'Confirmar rechazo' deshabilitado", true);
                    break;
                }
                cartera.confirmarRechazo();
                String aviso = web.esperarAviso();
                web.recargar();
                web.esperarTexto("Historial de movimientos");
                String fila = cartera.movimiento(atleta);
                verificar("No rechaza sin motivo", "el comprobante sigue Pendiente", siVacio(fila, "sin movimiento"), fila.contains("Pendiente"));
                verificar("Mensaje que pida el motivo", "un texto que pida el motivo", siVacio(aviso, "ningun mensaje"), !aviso.isEmpty() && !aviso.toLowerCase().contains("rechazado"));
                break;
            }

            case "totalesCartera": {
                cartera.abrir();
                String ingresos = cartera.tarjeta("Ingresos confirmados");
                String tasa = cartera.tarjeta("Tasa de pago activo");
                verificar("Ingresos confirmados con el pago aprobado", "$" + Configuracion.texto("asesorados.pago").replace(".", ","), siVacio(ingresos, "no aparece"),
                        ingresos.contains(Configuracion.texto("asesorados.pago").replace(".", ",")));
                verificar("Tasa de pago activo mayor a 0%", "mas de 0%", siVacio(tasa, "no aparece"), !tasa.contains(" 0%") && !tasa.isEmpty());
                break;
            }

            case "mensualidades": {
                String pagina = web.textoPagina();
                int i = pagina.indexOf("Control de Mensualidades");
                String seccion = i < 0 ? "" : pagina.substring(i, Math.min(pagina.length(), i + 250)).replace("\n", " ");
                verificar("Aparece el asesorado con su vencimiento", atleta, siVacio(seccion, "sin seccion"), seccion.contains(atleta));
                break;
            }

            case "verMensaje":
            case "responder":
            case "mensajeVacio": {
                web.irA("chat");
                web.clic(By.xpath("//main//*[contains(text(), '" + atleta + "')]"));
                if (accion.equals("verMensaje")) {
                    boolean visto = web.esperarTexto(mensajeAtleta);
                    verificar("Muestra el mensaje del atleta", mensajeAtleta, visto ? "aparece" : "no aparece", visto);
                } else if (accion.equals("responder")) {
                    String respuesta = valor + " " + Corrida.numero();
                    caso.dato("respuesta '" + respuesta + "'");
                    web.esperarTexto(mensajeAtleta); // que la conversacion termine de cargar antes de escribir
                    web.escribir(AtletaPage.MENSAJE, respuesta);
                    web.clic(AtletaPage.ENVIAR_MENSAJE);
                    boolean enviado = web.esperarTexto(respuesta);
                    verificar("La respuesta aparece en la conversacion", respuesta, enviado ? "aparece" : "no aparece", enviado);
                    web.recargar();
                    web.clic(By.xpath("//main//*[contains(text(), '" + atleta + "')]"));
                    boolean guardado = web.esperarTexto(respuesta);
                    verificar("La respuesta sigue despues de recargar", respuesta, guardado ? "aparece" : "no aparece", guardado);
                } else {
                    web.esperarTexto(mensajeAtleta);
                    int antes = web.texto(By.tagName("main")).length();
                    web.escribir(AtletaPage.MENSAJE, "   ");
                    if (!web.encontrar(AtletaPage.ENVIAR_MENSAJE).isEnabled()) {
                        verificar("No envia un mensaje vacio", "no se puede enviar", "boton de enviar deshabilitado", true);
                        break;
                    }
                    web.clic(AtletaPage.ENVIAR_MENSAJE);
                    boolean cambio = web.esperarQue("corta", d -> web.texto(By.tagName("main")).length() > antes + 3);
                    verificar("No envia un mensaje vacio", "la conversacion no cambia", cambio ? "aparecio un mensaje nuevo" : "no cambio", !cambio);
                }
                break;
            }

            case "noCancelarSesion":
            case "cancelarSesion": {
                abrirDetalle(asesorados, atleta);
                boolean habia = web.esperarQue("respuesta", d -> asesorados.haySesionParaCancelar());
                verificar("Hay una sesion programada (ASE-05)", "'Cancelar esta sesión'", habia ? "aparece" : "no aparece", habia);
                if (!habia) break;
                String pregunta = asesorados.cancelarSesion(accion.equals("cancelarSesion"));
                verificar("Pregunta antes de cancelar", "una pregunta de confirmacion", siVacio(pregunta, "no pregunto"), !pregunta.isEmpty());
                if (accion.equals("cancelarSesion")) web.esperarAviso();
                web.recargar();
                web.esperarTexto("Agenda programada");
                if (accion.equals("cancelarSesion")) {
                    boolean vacia = web.esperarTexto("Sin sesiones programadas");
                    verificar("La sesion sale de la agenda", "Sin sesiones programadas", vacia ? "Sin sesiones programadas" : "la sesion sigue", vacia);
                } else {
                    boolean sigue = web.esperarQue("respuesta", d -> asesorados.haySesionParaCancelar());
                    verificar("La sesion sigue en la agenda", "la sesion sigue", sigue ? "sigue" : "ya no esta", sigue);
                }
                break;
            }

            case "sesionPasada": {
                abrirDetalle(asesorados, atleta);
                asesorados.boton("Programar nueva sesión");
                String fecha = LocalDate.now().minusDays(Configuracion.numero("asesorados.sesion.dias")).toString();
                caso.dato("fecha '" + fecha + "', hora '" + Configuracion.texto("asesorados.sesion.hora") + "'");
                asesorados.llenarSesion(fecha, Configuracion.texto("asesorados.sesion.hora"), valor);
                if (!asesorados.botonDelDialogoHabilitado("Confirmar cita")) {
                    verificar("No deja confirmar una fecha pasada", "boton deshabilitado o mensaje", "boton deshabilitado", true);
                    break;
                }
                asesorados.botonDelDialogo("Confirmar cita");
                String aviso = web.esperarAviso();
                web.recargar();
                web.esperarTexto("Agenda programada");
                boolean programada = web.esperarQue("corta", d -> asesorados.haySesionParaCancelar());
                verificar("No programa la sesion", "Sin sesiones programadas", programada ? "quedo programada (aviso: " + aviso + ")" : "no quedo programada", !programada);
                break;
            }

            case "radar": {
                web.irA("objectives");
                boolean esta = web.esperarTexto(atleta);
                String pagina = web.textoPagina().replace("\n", " ");
                verificar("Aparece el asesorado", atleta, esta ? "aparece" : "no aparece", esta);
                verificar("Cuenta a todos los asesorados", "Total asesorados " + ATLETAS, pagina.contains("Total asesorados " + ATLETAS) ? "Total asesorados " + ATLETAS : "otro numero",
                        pagina.contains("Total asesorados " + ATLETAS));
                break;
            }

            case "filtroEstado": {
                asesorados.abrirLista();
                asesorados.apareceEnLista(atleta);
                asesorados.filtrar(1, valor);
                boolean debeVerse = valor.equals("Activos");
                boolean ok = web.esperarQue("respuesta", d -> asesorados.fila(atleta).isEmpty() != debeVerse);
                verificar("Filtro " + valor, debeVerse ? "el asesorado aparece" : "el asesorado no aparece", asesorados.fila(atleta).isEmpty() ? "no aparece" : "aparece", ok);
                break;
            }

            case "filtroCartera": {
                cartera.abrir();
                web.esperarQue("respuesta", d -> !cartera.movimiento(atletaA).isEmpty() && !cartera.movimiento(atletaB).isEmpty());
                asesorados.filtrar(1, valor);
                boolean ok = web.esperarQue("respuesta", d -> !cartera.movimiento(atletaA).isEmpty() && cartera.movimiento(atletaB).isEmpty());
                verificar("Muestra el pago aprobado (A) y no el rechazado (B)", atletaA + " si, " + atletaB + " no",
                        "A: " + siVacio(cartera.movimiento(atletaA), "no aparece") + " | B: " + siVacio(cartera.movimiento(atletaB), "no aparece"), ok);
                break;
            }

            case "buscarCartera": {
                cartera.abrir();
                web.esperarQue("respuesta", d -> !cartera.movimiento(atletaB).isEmpty());
                web.escribir(By.cssSelector("main input[placeholder^='Buscar por asesorado']"), atleta);
                boolean ok = web.esperarQue("respuesta", d -> !cartera.movimiento(atleta).isEmpty() && cartera.movimiento(atletaB).isEmpty());
                verificar("Solo muestra los pagos del asesorado buscado", atleta, "B: " + siVacio(cartera.movimiento(atletaB), "no aparece") + " | buscado: " + siVacio(cartera.movimiento(atleta), "no aparece"), ok);
                break;
            }

            case "campana": {
                new MenuLateral(web).abrirNotificaciones();
                web.esperarQue("respuesta", d -> web.texto(MenuLateral.PANEL_NOTIFICACIONES).contains(atleta));
                String avisos = web.texto(MenuLateral.PANEL_NOTIFICACIONES);
                verificar("Avisa del pago del atleta con su monto", atleta + " y $" + valor, siVacio(avisos, "sin notificaciones"), avisos.contains(atleta) && avisos.contains(valor));
                break;
            }

            case "rutinaCompleta": {
                String nombre = valor + " " + Corrida.numero();
                caso.dato("entrenamiento '" + nombre + "', ejercicio '" + Configuracion.texto("asesorados.ejercicio") + "'");
                abrirDetalle(asesorados, atleta);
                asesorados.boton("Asignar rutina");
                web.esperarTexto("Asignando a:");
                asesorados.agregarEjercicio(Configuracion.texto("asesorados.ejercicio"));
                asesorados.crearEntrenamiento(nombre);
                String aviso = web.esperarAviso();
                verificar("Crea el entrenamiento sin error", "sin mensaje de error", siVacio(aviso, "ningun aviso"), !aviso.toLowerCase().contains("error"));
                abrirDetalle(asesorados, atleta);
                asesorados.boton("Historial");
                boolean asignada = web.esperarQue("respuesta", d -> asesorados.textoDialogo().contains(nombre));
                verificar("Queda asignado al asesorado", "'" + nombre + "' en el control de entrenamiento", siVacio(asesorados.textoDialogo(), "no se abrio"), asignada);
                break;
            }

            case "pesoDelCoach": {
                // al atleta su panel le dice "Solo tu coach puede actualizar este valor"
                abrirDetalle(asesorados, atleta);
                web.esperarTexto("Agenda programada");
                boolean puede = web.existe(By.xpath("//main//input | //main//button[contains(translate(., 'EDITARPSO', 'editarpso'), 'editar') or contains(translate(., 'PESO', 'peso'), 'peso')]"));
                verificar("El detalle permite actualizar el peso", "un campo o un boton para el peso", puede ? "hay un control" : "no hay ninguno", puede);
                break;
            }

            default:
                throw new IllegalArgumentException("Accion desconocida: " + accion);
        }
        comprobarTodo();
    }

    private void abrirDetalle(AsesoradosPage asesorados, String atleta) {
        asesorados.abrirLista();
        asesorados.apareceEnLista(atleta);
        asesorados.verPerfilCompleto(atleta);
    }
}
