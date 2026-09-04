package com.sun.source.doctree;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Texto que no se pudo parsear, con el diagnostico que explica por que.
 *
 * <p>Extiende {@link TextTree} a proposito: lo que no se entendio sigue estando disponible como
 * texto crudo, asi que una herramienta puede mostrarlo aunque no pueda interpretarlo. Un arbol que
 * simplemente omitiera lo roto perderia informacion que el usuario escribio.
 */
public interface ErroneousTree extends TextTree {

    Diagnostic<JavaFileObject> getDiagnostic();
}
