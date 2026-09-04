package com.sun.source.util;

import javax.lang.model.element.VariableElement;

/**
 * De donde saca el compilador el nombre de un parametro cuando el {@code .class} no lo trae.
 *
 * <h2>Por que puede no traerlo</h2>
 *
 * <p>Porque guardar los nombres de los parametros es opcional: {@code javac} solo los emite con
 * {@code -parameters}. Sin eso, una clase compilada expone {@code arg0}, {@code arg1} — que
 * compila igual pero es inservible para una herramienta que genere codigo o documentacion.
 *
 * <p>Este enganche deja completarlos desde otro lado: un archivo de metadatos, el fuente si esta a
 * mano, una convencion. Devolver {@code null} es decir "no se", y ahi queda el {@code argN}.
 */
public interface ParameterNameProvider {

    /** El nombre de ese parametro, o {@code null} si no se sabe. */
    CharSequence getParameterName(VariableElement parameter);
}
