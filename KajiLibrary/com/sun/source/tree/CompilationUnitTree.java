package com.sun.source.tree;

import java.util.List;
import javax.tools.JavaFileObject;

/**
 * Un archivo fuente entero, y la raiz de todo arbol de este paquete.
 */
public interface CompilationUnitTree extends Tree {

    /** La declaracion de modulo si este archivo es un `module-info.java`, si no `null`. */
    default ModuleTree getModule() {
        return null;
    }

    List<? extends AnnotationTree> getPackageAnnotations();

    ExpressionTree getPackageName();

    PackageTree getPackage();

    List<? extends ImportTree> getImports();

    List<? extends Tree> getTypeDecls();

    JavaFileObject getSourceFile();

    /** El traductor de posiciones a linea y columna; ver {@link LineMap}. */
    LineMap getLineMap();
}
