package com.achron.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Datos del modulo que prueba una clase: nombre corto (carpeta de reportes) y titulo del reporte. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Modulo {

    String nombre();

    String titulo();

    /** Pagina principal del modulo, para la portada del reporte. */
    String ruta() default "";
}
