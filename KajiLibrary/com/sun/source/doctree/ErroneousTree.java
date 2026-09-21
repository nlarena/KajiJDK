package com.sun.source.doctree;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Text that could not be parsed, with the diagnostic that explains why.
 *
 * <p>It extends {@link TextTree} on purpose: what was not understood goes on being available as
 * raw text, so a tool may show it even though it cannot interpret it. A tree that simply left out
 * what is broken would lose information the user wrote.
 */
public interface ErroneousTree extends TextTree {

    Diagnostic<JavaFileObject> getDiagnostic();
}
