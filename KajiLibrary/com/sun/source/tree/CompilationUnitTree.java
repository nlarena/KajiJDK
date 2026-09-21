package com.sun.source.tree;

import java.util.List;
import javax.tools.JavaFileObject;

/**
 * A whole source file, and the root of every tree of this package.
 */
public interface CompilationUnitTree extends Tree {

    /** The module declaration if this file is a `module-info.java`, otherwise `null`. */
    default ModuleTree getModule() {
        return null;
    }

    List<? extends AnnotationTree> getPackageAnnotations();

    ExpressionTree getPackageName();

    PackageTree getPackage();

    List<? extends ImportTree> getImports();

    List<? extends Tree> getTypeDecls();

    JavaFileObject getSourceFile();

    /** The translator of positions into line and column; see {@link LineMap}. */
    LineMap getLineMap();
}
