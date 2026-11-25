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
import com.achron.pages.RegistroPage;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;

/**
 * El rol atleta (asesorado). Al empezar se crea un coach con nombre unico; el atleta lo registra el primer caso
 * (ATL-01) con el formulario de registro y los demas casos entran con esa cuenta. Algunos casos los completa el coach
 * (ver el comprobante y el mensaje, programar una sesion, asignar una metodologia) para revisar que el atleta lo vea.
 * No se hacen pagos reales: el pago es un registro manual con una imagen como comprobante.
 */
@Tag("atleta")
@Modulo(nombre = "Atleta", titulo = "Rol atleta (registro, coach, panel, perfil, pago, chat y sesiones)", ruta = "client-dashboard")
class AtletaTest extends BaseTest {

    private static CuentaDePrueba coach;
    private static String atleta;
    private static String email;
    private static String nit;
    private static String clave;
    private static String mensajeAtleta;

    @BeforeAll
    static void prepararCoach() {
        int corrida = Corrida.numero();
        coach = CuentaDePrueba.crearCoach("coachatl", 11, Configuracion.texto("atleta.coach") + " " + corrida);
        atleta = Configuracion.texto("atleta.nombre") + " " + corrida;
        String[] partes = Configuracion.texto("email").split("@");
        email = partes[0] + "atleta" + corrida + "@" + partes[1];
        nit = String.valueOf(Long.parseLong(Configuracion.texto("nit")) + corrida * 100L + 12);
        clave = Configuracion.texto("contrasena");
        mensajeAtleta = Configuracion.texto("atleta.mensaje.atleta") + " " + corrida;
    }

    // valor: dato del caso ("atleta.xxx" = de datos.properties, "-" = ninguno)
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "ATL-01 | Registro y coach | Registrarse como atleta con datos validos lleva a su panel       | registro          | -                       | Alta",
            "ATL-02 | Registro y coach | Sin coach el atleta ve 'Elige a tu Guia primate'                  | sinCoach          | -                       | Media",
            "ATL-03 | Registro y coach | El coach de prueba aparece entre los coaches para elegir         | coachEnLista      | -                       | Alta",
            "ATL-04 | Registro y coach | Ver trayectoria abre el perfil profesional del coach             | trayectoria       | -                       | Media",
            "ATL-05 | Registro y coach | Elegir un coach pide confirmacion antes de elegirlo              | confirmarCoach    | -                       | Media",
            "ATL-06 | Registro y coach | Al elegir el coach el panel lo muestra como 'Tu coach'           | elegirCoach       | -                       | Alta",
            "ATL-07 | Panel            | El panel se llama 'Tu panel de inicio'                           | titulo            | -                       | Baja",
            "ATL-08 | Panel            | El menu del atleta tiene Inicio, Mis rutinas, Chat Coach y Asistente IA | menu       | -                       | Media",
            "ATL-09 | Panel            | El peso actual solo lo puede cambiar el coach                    | pesoSoloCoach     | -                       | Media",
            "ATL-10 | Panel            | Ver perfil profesional muestra al coach elegido                  | perfilCoach       | -                       | Media",
            "ATL-11 | Panel            | Sin sesiones programadas dice 'Sin programar'                    | sinSesiones       | -                       | Baja",
            "ATL-12 | Panel            | Ver todos los consejos muestra mas consejos                      | consejos          | -                       | Baja",
            "ATL-13 | Panel            | Un atleta nuevo no tiene asistencia ni intensidad sin datos      | resumenNuevo      | -                       | Media",
            "ATL-14 | Panel            | Mis rutinas sin rutinas dice que el coach esta disenando el plan | rutinasVacias     | -                       | Baja",
            "ATL-15 | Panel            | El atleta no puede entrar al panel del coach (/dashboard)        | protegida         | dashboard               | Alta",
            "ATL-16 | Panel            | El atleta no puede entrar a la cartera del coach (/wallet)       | protegida         | wallet                  | Alta",
            "ATL-17 | Panel            | El Asistente IA esta bloqueado para el plan del coach            | asistente         | -                       | Baja",
            "ATL-18 | Perfil           | El perfil muestra el nombre y el correo del atleta               | perfilDatos       | -                       | Media",
            "ATL-19 | Perfil           | Guardar telefono, gimnasio, altura y peso y que sigan al recargar | perfilGuardar    | -                       | Alta",
            "ATL-20 | Perfil           | Una altura negativa no se guarda                                 | perfilInvalido    | height=atleta.altura.negativa | Media",
            "ATL-21 | Perfil           | Un peso negativo no se guarda                                    | perfilInvalido    | weight=atleta.peso.negativo   | Media",
            "ATL-22 | Perfil           | El nombre vacio no se guarda                                     | perfilInvalido    | fullName=               | Media",
            "ATL-23 | Perfil           | Contrasenas distintas no se cambian                              | claveDistinta     | atleta.clave.distinta   | Alta",
            "ATL-24 | Perfil           | Una contrasena de menos de 6 caracteres no se acepta             | claveCorta        | atleta.clave.corta      | Media",
            "ATL-25 | Pago             | El pago de la membresia muestra al coach y el monto               | pagoPagina        | atleta.pago             | Media",
            "ATL-26 | Pago             | Avisa que el coach no ha configurado sus datos bancarios         | pagoSinBanco      | -                       | Baja",
            "ATL-27 | Pago             | Sin comprobante no se puede registrar el pago                    | pagoSinComprobante | atleta.pago            | Media",
            "ATL-28 | Pago             | Un pago con monto 0 no se registra                               | pagoMontoCero     | atleta.pago.cero        | Media",
            "ATL-29 | Pago             | Registrar el pago con su comprobante                             | pagoRegistrar     | atleta.pago             | Alta",
            "ATL-30 | Pago             | El comprobante del atleta llega a la bandeja del coach           | pagoEnBandeja     | atleta.pago             | Alta",
            "ATL-31 | Chat             | El chat muestra al coach y que no hay mensajes                   | chatVacio         | -                       | Baja",
            "ATL-32 | Chat             | El mensaje del atleta aparece y sigue despues de recargar         | chatEnviar        | -                       | Alta",
            "ATL-33 | Chat             | El coach recibe el mensaje del atleta                            | chatCoachRecibe   | -                       | Alta",
            "ATL-34 | Chat             | La respuesta del coach le llega al atleta                        | chatRespuesta     | atleta.mensaje.coach    | Alta",
            "ATL-35 | Chat             | Un mensaje vacio no se envia                                     | chatVacioNoEnvia  | -                       | Baja",
            "ATL-36 | Sesiones y plan  | La sesion que programa el coach aparece en sus proximas sesiones  | sesionProgramada  | atleta.sesion.notas     | Alta",
            "ATL-37 | Sesiones y plan  | La metodologia que asigna el coach aparece en su panel           | metodologia       | asesorados.metodologia  | Media",
            "ATL-38 | Perfil           | Cambiar la contrasena y entrar con la nueva                      | claveCambiar      | atleta.clave.nueva      | Alta",
            "ATL-39 | Sesiones y plan  | La notificacion de la sesion dice la hora que programo el coach  | sesionHora        | asesorados.sesion.hora  | Alta",
            "ATL-40 | Sesiones y plan  | El atleta confirma la sesion que programo el coach               | sesionConfirmar   | -                       | Media",
            "ATL-41 | Perfil           | El objetivo que elige en su perfil aparece en su panel           | objetivo          | atleta.objetivo         | Media",
            "ATL-42 | Perfil           | El peso que guarda en su perfil aparece en su panel              | pesoEnPanel       | atleta.peso             | Media",
            "ATL-43 | Perfil           | Un telefono con letras no se guarda                              | perfilInvalido    | phone=atleta.telefono.letras | Media",
            "ATL-44 | Perfil           | Subir una foto de perfil                                         | fotoPerfil        | -                       | Media",
            "ATL-45 | Evolucion        | La galeria de un atleta nuevo empieza sin fotos                  | galeriaNueva      | -                       | Media",
            "ATL-46 | Evolucion        | El proximo registro sugerido no es una fecha pasada              | proximoRegistro   | -                       | Baja",
            "ATL-47 | Evolucion        | Sin archivo no se puede guardar una foto de evolucion            | fotoSinArchivo    | -                       | Media",
            "ATL-48 | Evolucion        | La foto de evolucion que sube sigue despues de recargar           | fotoSubir         | -                       | Alta",
            "ATL-49 | Herramientas     | Cambiar a ingles traduce el menu del atleta                      | idioma            | EN - English            | Baja",
            "ATL-50 | Herramientas     | El boton de tema cambia entre oscuro y claro                     | tema              | -                       | Baja",
            "ATL-51 | Herramientas     | Cerrar sesion saca al atleta y su panel queda protegido          | cerrarSesion      | -                       | Alta"
    })
    void atleta(String id, String categoria, String descripcion, String accion, String valorCsv, String severidad) {
        String valor = valorCsv.contains("=") ? valorCsv : dato(dato(valorCsv, "atleta"), "asesorados");
        nuevoCaso(id, categoria, descripcion, severidad, "atleta '" + atleta + "' (" + email + "), coach '" + coach.nombre + "'"
                + (valor.equals("-") ? "" : ", dato '" + valor + "'"));
        AtletaPage pagina = new AtletaPage(web);
        // los casos que empieza el coach no entran primero como atleta
        List<String> empiezaElCoach = List.of("registro", "pagoEnBandeja", "chatCoachRecibe", "chatRespuesta", "sesionProgramada", "metodologia");
        if (!empiezaElCoach.contains(accion)) {
            pagina.iniciarSesion(email, clave);
        }

        switch (accion) {
            case "registro": {
                RegistroPage registro = new RegistroPage(web);
                registro.abrir();
                registro.llenar(atleta, nit, email, clave);
                registro.enviarComoAtleta();
                boolean entro = web.esperarUrl("/" + AtletaPage.PANEL);
                web.esperarTexto(atleta);
                verificar("Entra a su panel", "url /" + AtletaPage.PANEL, web.url(), entro);
                verificar("Muestra su nombre y su rol", atleta + " · Asesorado", web.textoPagina().contains(atleta) && web.textoPagina().contains("Asesorado") ? "aparecen" : "no aparecen",
                        web.textoPagina().contains(atleta) && web.textoPagina().contains("Asesorado"));
                break;
            }

            case "sinCoach": {
                boolean elige = web.esperarTexto("Elige a tu Guía primate");
                verificar("Pide elegir un coach", "'Elige a tu Guía primate'", elige ? "aparece" : "no aparece", elige);
                break;
            }

            case "coachEnLista": {
                boolean esta = pagina.coachEnLista(coach.nombre);
                verificar("El coach esta en la lista", coach.nombre, esta ? "aparece" : "no aparece", esta);
                break;
            }

            case "trayectoria": {
                pagina.coachEnLista(coach.nombre);
                pagina.verTrayectoria(coach.nombre);
                boolean abrio = web.esperarQue("respuesta", d -> web.hayDialogoAbierto() || !web.url().endsWith("/" + AtletaPage.PANEL));
                String texto = web.hayDialogoAbierto() ? web.texto(Acciones.DIALOGO) : web.textoPagina();
                verificar("Abre el perfil del coach", "una ventana o pagina con el perfil", abrio ? "se abrio" : "no paso nada", abrio);
                verificar("Muestra el nombre del coach", coach.nombre, texto.contains(coach.nombre) ? "aparece" : "no aparece", abrio && texto.contains(coach.nombre));
                break;
            }

            case "confirmarCoach": {
                pagina.coachEnLista(coach.nombre);
                pagina.pedirElegir(coach.nombre);
                boolean pregunta = web.aparece(Acciones.DIALOGO);
                String texto = pregunta ? web.texto(Acciones.DIALOGO) : "";
                verificar("Pregunta antes de elegir", "ventana con 'Sí, elegir'", siVacio(texto, "no pregunto"), texto.contains("Sí, elegir"));
                pagina.cerrarVentana();
                web.recargar();
                boolean sigueSinCoach = web.esperarTexto("Elige a tu Guía primate");
                verificar("Si cierra la ventana no elige al coach", "sigue sin coach", sigueSinCoach ? "sigue sin coach" : "quedo con coach", sigueSinCoach);
                break;
            }

            case "elegirCoach": {
                pagina.elegirCoach(coach.nombre);
                web.recargar();
                web.esperarTexto("Tu coach");
                web.esperarQue("respuesta", d -> pagina.seccion("Tu coach", 80).contains(coach.nombre));
                String seccion = pagina.seccion("Tu coach", 80);
                verificar("El panel muestra al coach elegido", "Tu coach · " + coach.nombre, siVacio(seccion, "sin seccion"), seccion.contains(coach.nombre));
                break;
            }

            case "titulo": {
                boolean titulo = web.textoPagina().contains("Tu panel de inicio");
                verificar("Titulo del panel", "Tu panel de inicio", titulo ? "Tu panel de inicio" : "otro", titulo);
                break;
            }

            case "menu": {
                List<String> menu = pagina.menu();
                boolean completo = menu.containsAll(List.of("Inicio", "Mis rutinas", "Chat Coach", "Asistente IA"));
                verificar("Opciones del menu", "Inicio, Mis rutinas, Chat Coach, Asistente IA", String.join(", ", menu), completo);
                break;
            }

            case "pesoSoloCoach": {
                String seccion = pagina.seccion("Peso actual", 90);
                boolean aviso = seccion.contains("Solo tu coach puede actualizar este valor");
                boolean editable = web.existe(By.xpath("//main//*[normalize-space()='Peso actual']/following::input[1][ancestor::*[contains(.,'Solo tu coach')]]"));
                verificar("Dice que solo el coach lo cambia", "'Solo tu coach puede actualizar este valor'", siVacio(seccion, "sin seccion"), aviso);
                verificar("No hay un campo para cambiarlo", "sin campo editable", editable ? "hay un campo" : "sin campo", !editable);
                break;
            }

            case "perfilCoach": {
                pagina.clicEn("Ver perfil profesional");
                boolean abrio = web.aparece(Acciones.DIALOGO);
                String texto = abrio ? web.texto(Acciones.DIALOGO) : "";
                verificar("Abre el perfil del coach", "ventana con el nombre del coach", siVacio(texto, "no se abrio"), texto.contains(coach.nombre));
                break;
            }

            case "sinSesiones": {
                web.esperarQue("respuesta", d -> pagina.seccion("Próximas sesiones", 160).contains(coach.nombre));
                String seccion = pagina.seccion("Próximas sesiones", 160);
                verificar("Sin sesiones programadas", "'Sin programar'", siVacio(seccion, "sin seccion"), seccion.contains("Sin programar"));
                break;
            }

            case "consejos": {
                // se espera a que todo el panel cargue y solo se mira la seccion de consejos: otro texto que llegue tarde no cuenta
                web.esperarTexto("Resumen de progreso");
                String antes = pagina.seccion("Consejos para tu objetivo", 260);
                String url = web.url();
                pagina.clicEn("Ver todos los consejos");
                boolean cambio = web.esperarQue("corta", d -> web.hayDialogoAbierto() || !web.url().equals(url) || !pagina.seccion("Consejos para tu objetivo", 260).equals(antes));
                verificar("Muestra mas consejos", "una ventana, una pagina o mas consejos", cambio ? "cambio la pantalla" : "no paso nada", cambio);
                break;
            }

            case "resumenNuevo": {
                // el panel lateral llega despues del resto ("Cargando sidebar...")
                web.esperarTexto("Resumen de progreso");
                String seccion = pagina.seccion("Resumen de progreso", 90);
                boolean inventado = seccion.contains("90%") || seccion.contains("Alta");
                verificar("Asistencia e intensidad sin datos", "0% o 'sin datos' (el atleta no ha ido a ninguna sesion)", siVacio(seccion, "sin seccion"), !seccion.isEmpty() && !inventado);
                break;
            }

            case "rutinasVacias": {
                web.irA(AtletaPage.PANEL + "?tab=routines");
                boolean mensaje = web.esperarTexto("Tu Coach está diseñando el plan");
                verificar("Explica que no tiene rutinas todavia", "'Tu Coach está diseñando el plan...'", mensaje ? "aparece" : "no aparece", mensaje);
                break;
            }

            case "protegida": {
                web.irA(valor);
                boolean devuelto = web.esperarUrl("/" + AtletaPage.PANEL);
                verificar("No deja entrar a /" + valor, "lo devuelve a /" + AtletaPage.PANEL, web.url(), devuelto);
                break;
            }

            case "asistente": {
                web.irA("assistant");
                boolean bloqueado = web.esperarTexto("Acceso exclusivo pro");
                verificar("El asistente esta bloqueado", "'Acceso exclusivo pro'", bloqueado ? "aparece" : "no aparece", bloqueado);
                break;
            }

            case "perfilDatos": {
                pagina.abrirPerfil();
                String texto = web.textoPagina();
                verificar("Muestra el nombre", atleta, pagina.valorPerfil("fullName"), pagina.valorPerfil("fullName").equals(atleta));
                verificar("Muestra el correo", email, texto.contains(email) ? email : "no aparece", texto.contains(email));
                break;
            }

            case "perfilGuardar": {
                pagina.abrirPerfil();
                String[][] campos = {{"phone", "atleta.telefono"}, {"gym", "atleta.gimnasio"}, {"height", "atleta.altura"}, {"weight", "atleta.peso"}};
                for (String[] campo : campos) {
                    pagina.escribirPerfil(campo[0], Configuracion.texto(campo[1]));
                }
                pagina.guardarPerfil();
                String aviso = web.esperarAviso();
                verificar("Aviso de guardado", "un aviso de exito", siVacio(aviso, "ningun aviso"), !aviso.isEmpty() && !aviso.toLowerCase().contains("error"));
                web.recargar();
                pagina.abrirPerfil();
                for (String[] campo : campos) {
                    String esperado = Configuracion.texto(campo[1]);
                    String obtenido = pagina.valorPerfil(campo[0]);
                    verificar(campo[0] + " sigue despues de recargar", esperado, siVacio(obtenido, "vacio"), obtenido.equals(esperado));
                }
                break;
            }

            case "perfilInvalido": {
                String[] par = valor.split("=", 2);
                String campo = par[0];
                String nuevo = par.length > 1 && !par[1].isEmpty() ? Configuracion.texto(par[1]) : "";
                caso.dato(campo + " = '" + nuevo + "'");
                pagina.abrirPerfil();
                String antes = pagina.valorPerfil(campo);
                pagina.escribirPerfil(campo, nuevo);
                pagina.guardarPerfil();
                String aviso = web.esperarAviso();
                String navegador = web.mensajeNavegador(By.id(campo));
                web.recargar();
                pagina.abrirPerfil();
                String despues = pagina.valorPerfil(campo);
                verificar("No guarda el valor", "sigue '" + antes + "'", siVacio(despues, "vacio"), despues.equals(antes));
                verificar("Dice que el valor no es valido", "un mensaje de error", siVacio((aviso + " " + navegador).trim(), "ningun mensaje"),
                        !navegador.isEmpty() || aviso.toLowerCase().contains("error") || aviso.toLowerCase().contains("válid") || aviso.toLowerCase().contains("obligatori"));
                // si el sitio lo guardo, se deja como estaba para los casos siguientes (el coach busca al atleta por su nombre)
                if (!despues.equals(antes)) {
                    pagina.escribirPerfil(campo, antes);
                    pagina.guardarPerfil();
                    web.esperarAviso();
                }
                break;
            }

            case "claveDistinta":
            case "claveCorta": {
                pagina.abrirPerfil();
                pagina.escribirPerfil("newPassword", valor);
                pagina.escribirPerfil("confirmPassword", accion.equals("claveDistinta") ? clave : valor);
                pagina.actualizarClave();
                String aviso = web.esperarAviso();
                String navegador = (web.mensajeNavegador(By.id("newPassword")) + " " + web.mensajeNavegador(By.id("confirmPassword"))).trim();
                boolean rechazo = !aviso.isEmpty() && !aviso.toLowerCase().contains("actualizada") && !aviso.toLowerCase().contains("éxito") || !navegador.isEmpty();
                verificar("No cambia la contrasena", "un mensaje de error y la contrasena sigue igual", siVacio((aviso + " " + navegador).trim(), "ningun mensaje"), rechazo);
                // la contrasena de siempre sigue funcionando
                pagina.salir();
                pagina.iniciarSesion(email, clave);
                verificar("La contrasena anterior sigue sirviendo", "entra con la contrasena de siempre", web.url(), web.url().contains("/" + AtletaPage.PANEL));
                break;
            }

            case "pagoPagina": {
                web.irA(AtletaPage.PAGO);
                boolean pagina1 = web.esperarTexto("Pago de Membresía");
                web.esperarQue("respuesta", d -> web.textoPagina().contains(coach.nombre));
                String texto = web.textoPagina();
                verificar("Abre el pago de la membresia", "'Pago de Membresía'", pagina1 ? "aparece" : "no aparece", pagina1);
                verificar("Muestra el coach", coach.nombre, texto.contains(coach.nombre) ? "aparece" : "no aparece", texto.contains(coach.nombre));
                verificar("Muestra el monto", "$" + valor, texto.contains("$" + valor) ? "$" + valor : "no aparece", texto.contains("$" + valor));
                break;
            }

            case "pagoSinBanco": {
                web.irA(AtletaPage.PAGO);
                boolean aviso = web.esperarTexto("aún no ha configurado sus datos de pago");
                verificar("Avisa que faltan los datos bancarios", "'El coach aún no ha configurado sus datos de pago...'", aviso ? "aparece" : "no aparece", aviso);
                break;
            }

            case "pagoSinComprobante": {
                pagina.abrirFormularioDePago();
                pagina.llenarPago(valor, null);
                verificar("No deja registrar sin comprobante", "'Registrar y enviar' deshabilitado", pagina.registrarPagoHabilitado() ? "habilitado" : "deshabilitado", !pagina.registrarPagoHabilitado());
                break;
            }

            case "pagoMontoCero": {
                pagina.abrirFormularioDePago();
                pagina.llenarPago(valor, ArchivosDePrueba.imagenPng());
                if (!pagina.registrarPagoHabilitado()) {
                    verificar("No deja registrar un pago de 0", "boton deshabilitado o mensaje", "boton deshabilitado", true);
                    break;
                }
                pagina.enviarPago();
                boolean registrado = web.esperarTexto("PAGO REGISTRADO");
                String navegador = web.mensajeNavegador(By.id("amount"));
                verificar("No registra un pago de 0", "no registra y pide un monto valido", registrado ? "PAGO REGISTRADO" : siVacio(navegador, "no registro"), !registrado);
                break;
            }

            case "pagoRegistrar": {
                pagina.abrirFormularioDePago();
                pagina.llenarPago(valor, ArchivosDePrueba.imagenPng());
                pagina.enviarPago();
                boolean registrado = web.esperarTexto("PAGO REGISTRADO");
                verificar("Registra el pago", "'PAGO REGISTRADO'", registrado ? "aparece" : "no aparece", registrado);
                break;
            }

            case "pagoEnBandeja": {
                new LoginPage(web).iniciarSesion(coach.email, coach.clave);
                CarteraPage cartera = new CarteraPage(web);
                cartera.abrir();
                String tarjeta = cartera.comprobanteEnBandeja(atleta);
                verificar("El comprobante esta en la bandeja del coach", atleta, siVacio(tarjeta, "no esta"), !tarjeta.isEmpty());
                verificar("Con el monto del pago", "$" + valor, siVacio(tarjeta, "sin tarjeta"), tarjeta.contains(valor));
                break;
            }

            case "chatVacio": {
                pagina.abrirChat();
                web.esperarQue("respuesta", d -> web.textoPagina().contains("No hay mensajes previos"));
                String texto = web.textoPagina();
                verificar("Muestra al coach", coach.nombre, texto.contains(coach.nombre) ? "aparece" : "no aparece", texto.contains(coach.nombre));
                verificar("Dice que no hay mensajes", "'No hay mensajes previos'", texto.contains("No hay mensajes previos") ? "aparece" : "no aparece", texto.contains("No hay mensajes previos"));
                break;
            }

            case "chatEnviar": {
                pagina.escribirAlCoach(mensajeAtleta);
                web.recargar();
                boolean guardado = web.esperarTexto(mensajeAtleta);
                verificar("El mensaje sigue despues de recargar", mensajeAtleta, guardado ? "aparece" : "no aparece", guardado);
                break;
            }

            case "chatCoachRecibe":
            case "chatRespuesta": {
                new LoginPage(web).iniciarSesion(coach.email, coach.clave);
                web.irA("chat");
                web.clic(By.xpath("//main//*[contains(text(), '" + atleta + "')]"));
                boolean visto = web.esperarTexto(mensajeAtleta);
                if (accion.equals("chatCoachRecibe")) {
                    verificar("El coach ve el mensaje", mensajeAtleta, visto ? "aparece" : "no aparece", visto);
                    break;
                }
                String respuesta = valor + " " + Corrida.numero();
                caso.dato("respuesta '" + respuesta + "'");
                web.escribir(AtletaPage.MENSAJE, respuesta);
                web.clic(AtletaPage.ENVIAR_MENSAJE);
                web.esperarTexto(respuesta);
                pagina.salir();
                pagina.iniciarSesion(email, clave);
                pagina.abrirChat();
                boolean llego = web.esperarTexto(respuesta);
                verificar("La respuesta le llega al atleta", respuesta, llego ? "aparece" : "no aparece", llego);
                break;
            }

            case "chatVacioNoEnvia": {
                pagina.abrirChat();
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
                break;
            }

            case "sesionProgramada": {
                new LoginPage(web).iniciarSesion(coach.email, coach.clave);
                AsesoradosPage asesorados = new AsesoradosPage(web);
                asesorados.abrirLista();
                asesorados.apareceEnLista(atleta);
                asesorados.verPerfilCompleto(atleta);
                asesorados.boton("Programar nueva sesión");
                String fecha = LocalDate.now().plusDays(Configuracion.numero("asesorados.sesion.dias")).toString();
                caso.dato("fecha '" + fecha + "', hora '" + Configuracion.texto("asesorados.sesion.hora") + "', notas '" + valor + "'");
                asesorados.llenarSesion(fecha, Configuracion.texto("asesorados.sesion.hora"), valor);
                asesorados.botonDelDialogo("Confirmar cita");
                web.esperarAviso();
                pagina.salir();
                pagina.iniciarSesion(email, clave);
                // la seccion tiene que estar cargada (nombra al coach) y ya no decir 'Sin programar'
                boolean programada = web.esperarQue("respuesta", d -> {
                    String sesiones = pagina.seccion("Próximas sesiones", 160);
                    return sesiones.contains(coach.nombre) && !sesiones.contains("Sin programar");
                });
                verificar("La sesion aparece en sus proximas sesiones", "una sesion con " + coach.nombre + " (no 'Sin programar')", siVacio(pagina.seccion("Próximas sesiones", 160), "sin seccion"), programada);
                break;
            }

            case "metodologia": {
                new LoginPage(web).iniciarSesion(coach.email, coach.clave);
                AsesoradosPage asesorados = new AsesoradosPage(web);
                asesorados.abrirLista();
                asesorados.apareceEnLista(atleta);
                asesorados.verPerfilCompleto(atleta);
                asesorados.boton("Asignar Metodología");
                asesorados.elegirMetodologia(valor);
                asesorados.botonDelDialogo("Confirmar Asignación");
                web.esperarAviso();
                pagina.salir();
                pagina.iniciarSesion(email, clave);
                web.clic(By.xpath("(//main//*[normalize-space()='Metodologías'])[last()]"));
                boolean ve = web.esperarTexto(valor);
                verificar("La metodologia aparece en su panel", valor, ve ? "aparece" : "no aparece", ve);
                break;
            }

            case "claveCambiar": {
                pagina.abrirPerfil();
                pagina.escribirPerfil("newPassword", valor);
                pagina.escribirPerfil("confirmPassword", valor);
                pagina.actualizarClave();
                String aviso = web.esperarAviso();
                verificar("Aviso de contrasena actualizada", "un aviso de exito", siVacio(aviso, "ningun aviso"), !aviso.isEmpty() && !aviso.toLowerCase().contains("error"));
                pagina.salir();
                try {
                    pagina.iniciarSesion(email, valor);
                    clave = valor;
                    verificar("Entra con la contrasena nueva", "entra a su panel", web.url(), true);
                } catch (IllegalStateException e) {
                    verificar("Entra con la contrasena nueva", "entra a su panel", "no pudo entrar", false);
                }
                break;
            }

            case "sesionHora": {
                new MenuLateral(web).abrirNotificaciones();
                web.esperarQue("respuesta", d -> web.texto(MenuLateral.PANEL_NOTIFICACIONES).contains("Sesión Programada"));
                String avisos = web.texto(MenuLateral.PANEL_NOTIFICACIONES);
                verificar("Le avisa de la sesion programada", "'Nueva Sesión Programada'", siVacio(avisos, "sin notificaciones"), avisos.contains("Sesión Programada"));
                verificar("Con la misma hora que puso el coach", "a las " + valor, siVacio(avisos, "sin notificaciones"), avisos.contains("a las " + valor));
                break;
            }

            case "sesionConfirmar": {
                By confirmar = By.xpath("(//main//*[contains(normalize-space(text()), 'Haz clic para confirmar')])[1]");
                web.clic(confirmar);
                // si el clic abre una ventana, se confirma ahi
                String ventana = web.aparece(Acciones.DIALOGO) ? web.texto(Acciones.DIALOGO) : "";
                By si = By.xpath("(//*[@role='dialog'][@data-state='open']//button[contains(translate(normalize-space(), 'CONFIRMAR', 'confirmar'), 'confirmar')])[1]");
                if (!ventana.isEmpty() && web.existe(si)) web.clic(si);
                String aviso = web.esperarAviso();
                web.recargar();
                web.esperarTexto("Resumen de progreso");
                boolean confirmada = web.esperarQue("respuesta", d -> pagina.seccion("Próximas sesiones", 200).contains(coach.nombre) && !web.existe(confirmar));
                verificar("La sesion queda confirmada", "ya no pide confirmar (tampoco al recargar)", "ventana: " + siVacio(ventana, "ninguna") + " | aviso: " + siVacio(aviso, "ninguno") + " | " + pagina.seccion("Próximas sesiones", 110), confirmada);
                break;
            }

            case "objetivo": {
                pagina.abrirPerfil();
                web.clic(By.xpath("(//main//*[@role='combobox'])[1]"));
                web.clic(By.xpath("//*[@role='option'][normalize-space()='" + valor + "']"));
                pagina.guardarPerfil();
                String aviso = web.esperarAviso();
                pagina.abrirPanel();
                // el panel lo muestra abreviado ("perder grasa")
                String enPanel = Configuracion.texto("atleta.objetivo.panel");
                boolean aparece = web.esperarQue("respuesta", d -> pagina.seccion("Tu objetivo", 60).toLowerCase().contains(enPanel));
                verificar("Aviso de guardado", "un aviso de exito", siVacio(aviso, "ningun aviso"), !aviso.isEmpty() && !aviso.toLowerCase().contains("error"));
                verificar("El panel muestra el objetivo", "Tu objetivo · " + enPanel, siVacio(pagina.seccion("Tu objetivo", 60), "sin seccion"), aparece);
                break;
            }

            case "pesoEnPanel": {
                pagina.abrirPanel();
                boolean aparece = web.esperarQue("respuesta", d -> pagina.seccion("Peso actual", 30).contains(valor));
                verificar("El panel muestra el peso guardado en el perfil (ATL-19)", "Peso actual · " + valor, siVacio(pagina.seccion("Peso actual", 80), "sin seccion"), aparece);
                break;
            }

            case "fotoPerfil": {
                pagina.abrirPerfil();
                web.subirArchivo(By.id("avatar-upload"), ArchivosDePrueba.imagenPng());
                String aviso = web.esperarAviso();
                web.recargar();
                pagina.abrirPerfil();
                boolean foto = web.esperarQue("respuesta", d -> web.existe(By.cssSelector("main img")));
                verificar("La foto queda en el perfil despues de recargar", "una imagen en el perfil", (foto ? "aparece" : "no aparece") + " | aviso: " + siVacio(aviso, "ninguno"), foto);
                break;
            }

            case "galeriaNueva": {
                pagina.abrirEvolucion();
                int fotos = pagina.fotosDeEvolucion();
                verificar("Sin fotos antes de subir la primera", "0 fotos", fotos + " fotos", fotos == 0);
                break;
            }

            case "proximoRegistro": {
                pagina.abrirEvolucion();
                String seccion = pagina.seccion("Próximo registro", 60);
                String anio = seccion.replaceAll("(?s).*Sugerido:.*?(20\\d\\d).*", "$1");
                boolean vigente = anio.matches("20\\d\\d") && Integer.parseInt(anio) >= LocalDate.now().getYear();
                verificar("La fecha sugerida no es de un ano pasado", "un ano desde " + LocalDate.now().getYear(), siVacio(seccion, "sin seccion"), vigente);
                break;
            }

            case "fotoSinArchivo": {
                pagina.abrirEvolucion();
                pagina.nuevaFoto();
                verificar("No deja guardar sin archivo", "'Confirmar y guardar' deshabilitado", pagina.guardarFotoHabilitado() ? "habilitado" : "deshabilitado", !pagina.guardarFotoHabilitado());
                break;
            }

            case "fotoSubir": {
                pagina.abrirEvolucion();
                int antes = pagina.fotosDeEvolucion();
                pagina.nuevaFoto();
                pagina.subirFotoDeEvolucion(ArchivosDePrueba.imagenPng());
                String aviso = web.esperarAviso();
                verificar("La foto se puede guardar", "un aviso de exito", siVacio(aviso, "ningun aviso"), !aviso.isEmpty() && !aviso.toLowerCase().contains("error"));
                pagina.abrirEvolucion();
                boolean sigue = web.esperarQue("respuesta", d -> pagina.fotosDeEvolucion() > antes);
                verificar("La foto sigue despues de recargar", (antes + 1) + " fotos", pagina.fotosDeEvolucion() + " fotos", sigue);
                break;
            }

            case "idioma": {
                web.esperarTexto("Mis rutinas");
                new MenuLateral(web).cambiarIdioma(valor);
                boolean traducido = web.esperarQue("respuesta", d -> !pagina.menu().isEmpty() && !pagina.menu().contains("Mis rutinas"));
                verificar("El menu cambia de idioma", "el menu en ingles", String.join(", ", pagina.menu()), traducido);
                break;
            }

            case "tema": {
                MenuLateral menu = new MenuLateral(web);
                String antes = menu.claseDelTema();
                menu.cambiarTema();
                boolean cambio = web.esperarQue("corta", d -> !menu.claseDelTema().equals(antes));
                verificar("Cambia el tema", "distinto de '" + antes + "'", menu.claseDelTema(), cambio);
                break;
            }

            case "cerrarSesion": {
                new MenuLateral(web).cerrarSesion();
                boolean salio = web.esperarQue("respuesta", d -> !d.getCurrentUrl().contains("/" + AtletaPage.PANEL));
                verificar("Sale de su panel", "otra pagina", web.url(), salio);
                web.irA(AtletaPage.PANEL);
                boolean protegido = web.esperarQue("respuesta", d -> !d.getCurrentUrl().contains("/" + AtletaPage.PANEL));
                verificar("El panel pide iniciar sesion", "lo saca de /" + AtletaPage.PANEL, web.url(), protegido);
                break;
            }

            default:
                throw new IllegalArgumentException("Accion desconocida: " + accion);
        }
        comprobarTodo();
    }
}
