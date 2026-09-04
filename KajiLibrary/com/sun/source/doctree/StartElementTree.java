package com.sun.source.doctree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * Una etiqueta HTML de apertura. {@link #isSelfClosing} distingue `<br>` de
 * `<br/>`, que el javadoc trata distinto.
 */
public interface StartElementTree extends DocTree {

    Name getName();

    List<? extends DocTree> getAttributes();

    /** Si la etiqueta se cierra sola, como `<br/>`. */
    boolean isSelfClosing();
}
