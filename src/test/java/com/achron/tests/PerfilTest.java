package com.achron.tests;

import com.achron.config.Configuracion;
import com.achron.config.Corrida;
import com.achron.core.Acciones;
import com.achron.core.ArchivosDePrueba;
import com.achron.core.BaseTest;
import com.achron.core.CuentaDePrueba;
import com.achron.core.Modulo;
import com.achron.pages.LoginPage;
import com.achron.pages.PerfilPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;

@Tag("perfil")
@Modulo(nombre = "Perfil", titulo = "Perfil del coach", ruta = "profile")
class PerfilTest extends BaseTest {

    private static CuentaDePrueba cuenta;

    @BeforeAll
    static void crearCuenta() {
        cuenta = CuentaDePrueba.crearCoach("perfil", 3);
    }

    // valor "perfil.xxx" = de datos.properties
    @ParameterizedTest(name = "{0} {2}")
    @CsvSource(delimiter = '|', value = {
            "PER-01 | Navegacion                            | Clic en el nombre abre Mi perfil              | navegar         | -                 | ''                   | Alta",
            "PER-02 | Informacion personal - visualizacion  | Muestra los datos del registro                | ver             | -                 | ''                   | Alta",
            "PER-03 | Informacion personal - visualizacion  | El email no se puede modificar                | emailBloqueado  | email             | otro@gmail.com       | Alta",
            "PER-04 | Informacion personal - edicion valida | Cambiar nombre                                | guardar         | fullName          | perfil.nombre        | Alta",
            "PER-05 | Informacion personal - edicion valida | Cambiar telefono                              | guardar         | phone             | perfil.telefono      | Alta",
            "PER-06 | Informacion personal - edicion valida | Cambiar URL de avatar                         | guardar         | avatarUrl         | perfil.avatar        | Alta",
            "PER-07 | Informacion personal - validaciones   | Telefono con letras                           | rechazar        | phone             | 300abc4567           | Alta",
            "PER-08 | Informacion personal - validaciones   | Telefono con simbolos                         | rechazar        | phone             | 300-456#789          | Alta",
            "PER-09 | Informacion personal - validaciones   | Telefono demasiado largo (40 digitos)         | rechazar        | phone             | 3001234567300123456730012345673001234567 | Alta",
            "PER-10 | Informacion personal - validaciones   | Nombre vacio                                  | rechazar        | fullName          | ''                   | Alta",
            "PER-11 | Informacion personal - validaciones   | Nombre con numeros                            | rechazar        | fullName          | Jacobo123            | Alta",
            "PER-12 | Informacion personal - validaciones   | Nombre con simbolos                           | rechazar        | fullName          | Jacobo@#$            | Alta",
            "PER-13 | Informacion personal - validaciones   | URL de avatar invalida                        | rechazar        | avatarUrl         | esto no es una url   | Alta",
            "PER-14 | Perfil publico - edicion valida       | Guardar titular profesional                   | guardar         | professionalTitle | perfil.titulo        | Alta",
            "PER-15 | Perfil publico - edicion valida       | Guardar biografia                             | guardar         | biography         | perfil.biografia     | Alta",
            "PER-16 | Perfil publico - edicion valida       | Guardar metodologia                           | guardar         | methodology       | perfil.metodologia   | Alta",
            "PER-17 | Perfil publico - validaciones         | Biografia de mas de 500 caracteres            | largo           | biography         | 600                  | Media",
            "PER-18 | Cancelar edicion                      | Cambio sin guardar se descarta al recargar    | sinGuardar      | phone             | 3119998888           | Media",
            "PER-19 | Datos bancarios                       | Guardar datos bancarios vacios                | bancoRechazar   | accountNumber     | ''                   | Alta",
            "PER-20 | Datos bancarios                       | Numero de cuenta con letras y simbolos        | bancoRechazar   | accountNumber     | 12ab34-cd            | Alta",
            "PER-21 | Datos bancarios                       | Guardar datos bancarios validos               | bancoGuardar    | accountNumber     | perfil.cuenta        | Alta",
            "PER-22 | Certificaciones                       | Agregar certificacion con nombre y diploma    | certAgregar     | newCertName       | perfil.certificacion | Alta",
            "PER-23 | Certificaciones                       | Subir diploma sin escribir el nombre          | certSinNombre   | newCertName       | ''                   | Media",
            "PER-24 | Certificaciones                       | Subir un archivo que no es PDF ni imagen      | certArchivoMalo | newCertName       | perfil.certificacion | Media",
            "PER-25 | Foto de perfil                        | Subir una foto de perfil                      | foto            | avatar-upload     | -                    | Media",
            "PER-26 | Seguridad                             | Cambiar contraseña abre su formulario         | cambiarClave    | -                 | -                    | Alta",
            "PER-27 | Seguridad                             | Cerrar sesion desde el perfil                 | salir           | -                 | -                    | Alta",
            "PER-28 | Datos bancarios                       | Subir el codigo QR de pago                    | qr              | qr-upload         | -                    | Media"
    })
    void perfil(String id, String categoria, String descripcion, String accion, String campo, String valor, String severidad) {
        valor = dato(valor, "perfil");
        nuevoCaso(id, categoria, descripcion, severidad, campo.equals("-") ? "cuenta creada en el registro" : campo + " = '" + valor + "'");
        new LoginPage(web).iniciarSesion(cuenta.email, cuenta.clave);
        PerfilPage perfil = new PerfilPage(web);
        perfil.abrirDesdeMenu();

        switch (accion) {
            case "navegar": {
                String pagina = web.textoPagina();
                verificar("Abre la pagina de perfil", "url con /profile", web.url(), web.url().contains("/profile"));
                verificar("Pestaña Informacion personal", "visible", web.aparece(PerfilPage.pestana(PerfilPage.INFO_PERSONAL)) ? "visible" : "no visible",
                        web.aparece(PerfilPage.pestana(PerfilPage.INFO_PERSONAL)));
                verificar("Pestaña Perfil publico", "visible", web.aparece(PerfilPage.pestana(PerfilPage.PERFIL_PUBLICO)) ? "visible" : "no visible",
                        web.aparece(PerfilPage.pestana(PerfilPage.PERFIL_PUBLICO)));
                verificar("Muestra el email del usuario", cuenta.email, pagina.contains(cuenta.email) ? cuenta.email : "no aparece", pagina.contains(cuenta.email));
                break;
            }

            case "ver":
                verificar("Nombre del registro", cuenta.nombre, perfil.valor("fullName"), perfil.valor("fullName").equals(cuenta.nombre));
                verificar("Email del registro", cuenta.email, perfil.valor("email"), perfil.valor("email").equals(cuenta.email));
                verificar("Cabecera muestra el nombre", cuenta.nombre, web.textoPagina().contains(cuenta.nombre) ? cuenta.nombre : "no aparece",
                        web.textoPagina().contains(cuenta.nombre));
                break;

            case "emailBloqueado": {
                boolean bloqueado = !web.encontrar(By.id("email")).isEnabled() || web.encontrar(By.id("email")).getAttribute("readonly") != null;
                verificar("Campo email bloqueado", "deshabilitado o solo lectura", bloqueado ? "bloqueado" : "editable", bloqueado);
                if (!bloqueado) perfil.escribir("email", valor);
                verificar("El email no cambia", cuenta.email, perfil.valor("email"), perfil.valor("email").equals(cuenta.email));
                break;
            }

            case "guardar": {
                perfil.abrirPestanaDe(campo);
                perfil.escribir(campo, valor);
                perfil.guardar(campo);
                String aviso = web.esperarAviso();
                String errores = perfil.errores(campo);
                verificar("Mensaje de guardado", "aviso '¡Perfil actualizado!'", siVacio(aviso, "ningun aviso"), aviso.contains("actualizado"));
                verificar("Sin errores", "ningun error", siVacio(errores, "ningun error"), errores.isBlank());
                perfil.recargar(campo);
                String guardado = perfil.esperarValor(campo, valor);
                verificar("Persistencia al recargar", valor, guardado, guardado.equals(valor));
                if (campo.equals("fullName") || campo.equals("professionalTitle")) {
                    boolean enCabecera = web.textoPagina().contains(valor);
                    verificar("Cabecera del perfil muestra el dato", valor, enCabecera ? valor : "no aparece", enCabecera);
                }
                break;
            }

            case "rechazar": {
                perfil.abrirPestanaDe(campo);
                String antes = perfil.valor(campo);
                perfil.escribir(campo, valor);
                perfil.guardar(campo);
                String aviso = web.esperarAviso();
                String errores = String.join(" ", web.mensajeNavegador(By.id(campo)), perfil.errores(campo),
                        aviso.contains("actualizado") ? "" : aviso).trim();
                verificar("Sin mensaje de guardado", "no aparece '¡Perfil actualizado!'", siVacio(aviso, "ningun aviso"), !aviso.contains("actualizado"));
                verificar("Mensaje de error visible", "un texto que explique el error", siVacio(errores, "ningun mensaje"), !errores.isBlank());
                perfil.recargar(campo);
                String guardado = perfil.valor(campo);
                verificar("No guarda el dato invalido", "'" + antes + "' (el valor anterior)", "'" + guardado + "'", !guardado.equals(valor));
                break;
            }

            case "largo": {
                perfil.abrirPestanaDe(campo);
                int cantidad = Integer.parseInt(valor);
                perfil.escribir(campo, "x".repeat(cantidad));
                int largo = perfil.valor(campo).length();
                String contador = web.texto(By.xpath("//*[contains(text(), 'caracteres restantes')]"));
                caso.dato(campo + " = " + cantidad + " letras 'x'");
                verificar("Limite de 500 caracteres", "maximo 500", largo + " caracteres", largo <= 500);
                verificar("Contador no negativo", "0 caracteres restantes o mas", contador, !contador.contains("-"));
                break;
            }

            case "sinGuardar": {
                perfil.abrirPestanaDe(campo);
                String antes = perfil.valor(campo);
                perfil.escribir(campo, valor);
                perfil.recargar(campo); // sin pulsar guardar
                String despues = perfil.valor(campo);
                verificar("El cambio sin guardar se descarta", "'" + antes + "'", "'" + despues + "'", despues.equals(antes));
                break;
            }

            case "bancoGuardar": {
                String banco = Configuracion.texto("perfil.banco");
                String tipo = Configuracion.texto("perfil.tipocuenta");
                String titular = Configuracion.texto("perfil.titular");
                caso.dato(banco + ", " + tipo + ", cuenta '" + valor + "', titular '" + titular + "'");
                perfil.llenarBanco(banco, tipo, valor, titular);
                perfil.guardarBanco();
                String aviso = web.esperarAviso();
                verificar("Mensaje de guardado", "aviso '¡Perfil actualizado!'", siVacio(aviso, "ningun aviso"), aviso.contains("actualizado"));
                perfil.recargar("accountNumber");
                String cuentaGuardada = perfil.esperarValor("accountNumber", valor);
                verificar("Numero de cuenta guardado", valor, cuentaGuardada, cuentaGuardada.equals(valor));
                verificar("Titular guardado", titular, perfil.valor("accountHolder"), perfil.valor("accountHolder").equals(titular));
                verificar("Banco y tipo de cuenta guardados", banco + " y " + tipo, perfil.bancoYTipo(),
                        perfil.bancoYTipo().contains(banco) && perfil.bancoYTipo().contains(tipo));
                break;
            }

            case "bancoRechazar": {
                caso.dato("cuenta '" + valor + "'" + (valor.isEmpty() ? ", sin banco, tipo ni titular" : ", con banco, tipo y titular validos"));
                if (!valor.isEmpty()) {
                    perfil.llenarBanco(Configuracion.texto("perfil.banco"), Configuracion.texto("perfil.tipocuenta"), valor, Configuracion.texto("perfil.titular"));
                }
                perfil.guardarBanco();
                String aviso = web.esperarAviso();
                String errores = (perfil.errores("accountNumber") + " " + (aviso.contains("actualizado") ? "" : aviso)).trim();
                verificar("Sin mensaje de guardado", "no aparece '¡Perfil actualizado!'", siVacio(aviso, "ningun aviso"), !aviso.contains("actualizado"));
                verificar("Mensaje de error visible", "un texto que explique el error", siVacio(errores, "ningun mensaje"), !errores.isBlank());
                if (!valor.isEmpty()) {
                    perfil.recargar("accountNumber");
                    String guardado = perfil.valor("accountNumber");
                    verificar("No guarda la cuenta invalida", "un valor distinto de '" + valor + "'", "'" + guardado + "'", !guardado.equals(valor));
                }
                break;
            }

            case "qr": {
                caso.dato("qr.png (imagen de prueba)");
                int antes = perfil.imagenesEnPagina();
                perfil.subirQr(ArchivosDePrueba.imagenPng());
                String aviso = web.esperarAviso();
                boolean seVe = web.esperarQue("respuesta", d -> perfil.imagenesEnPagina() > antes);
                verificar("Mensaje al subir el QR", "un aviso de QR cargado", siVacio(aviso, "ningun aviso"), !aviso.isEmpty() && !aviso.toLowerCase().contains("error"));
                verificar("El QR se ve en la configuracion", "mas imagenes que antes (" + antes + ")", perfil.imagenesEnPagina() + " imagenes", seVe);
                break;
            }

            case "certAgregar":
            case "certSinNombre":
            case "certArchivoMalo": {
                perfil.abrirPestanaDe("newCertName");
                boolean archivoMalo = accion.equals("certArchivoMalo");
                String nombre = valor.isEmpty() ? "" : valor + " " + Corrida.numero() + (archivoMalo ? " txt" : "");
                caso.dato("nombre = '" + nombre + "', archivo = " + (archivoMalo ? "notas.txt" : "diploma.png"));
                if (!nombre.isEmpty()) perfil.escribir("newCertName", nombre);
                perfil.subirCertificado(archivoMalo ? ArchivosDePrueba.textoPlano() : ArchivosDePrueba.imagenPng());
                String aviso = web.esperarAviso();
                if (accion.equals("certAgregar")) {
                    verificar("Mensaje de agregado", "aviso 'Certificación agregada'", siVacio(aviso, "ningun aviso"), aviso.contains("agregada"));
                    perfil.recargar("newCertName");
                    boolean sigue = web.esperarTexto(nombre);
                    verificar("Aparece en la lista despues de recargar", nombre, sigue ? "aparece" : "no aparece", sigue);
                } else {
                    verificar("No se agrega", "sin aviso 'Certificación agregada'", siVacio(aviso, "ningun aviso"), !aviso.contains("agregada"));
                    verificar("Mensaje que explique el problema", "un aviso de error", siVacio(aviso, "ningun mensaje"), !aviso.isEmpty() && !aviso.contains("agregada"));
                }
                break;
            }

            case "foto": {
                caso.dato("foto.png (imagen de prueba)");
                int antes = perfil.imagenesEnPagina();
                perfil.subirFoto(ArchivosDePrueba.imagenPng());
                String aviso = web.esperarAviso();
                verificar("Mensaje al subir la foto", "un aviso de foto actualizada", siVacio(aviso, "ningun aviso"), !aviso.isEmpty() && !aviso.toLowerCase().contains("error"));
                perfil.recargar("fullName");
                boolean seVe = web.esperarQue("respuesta", d -> perfil.imagenesEnPagina() > antes);
                verificar("La foto se ve en el perfil al recargar", "mas imagenes que antes (" + antes + ")", perfil.imagenesEnPagina() + " imagenes", seVe);
                break;
            }

            case "cambiarClave": {
                perfil.cambiarClave();
                boolean formulario = web.esperarQue("corta", d -> web.existe(Acciones.DIALOGO) || web.existe(By.cssSelector("input[type='password']")));
                verificar("Abre un formulario para cambiar la contraseña", "ventana o pagina con campos de contraseña",
                        formulario ? "se abrio el formulario" : "no hay formulario (url: " + web.url() + ")", formulario);
                break;
            }

            case "salir":
                perfil.cerrarSesion();
                verificar("Vuelve a la pantalla de acceso", "url con /auth", web.esperarUrl("/auth") ? web.url() : "sigue en " + web.url(), web.url().contains("/auth"));
                break;

            default:
                throw new IllegalArgumentException("Accion desconocida: " + accion);
        }
        comprobarTodo();
    }
}
