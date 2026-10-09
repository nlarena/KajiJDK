package com.sun.source.doctree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * An opening HTML tag. {@link #isSelfClosing} tells `<br>` from
 * `<br/>`, which javadoc treats differently.
 */
public interface StartElementTree extends DocTree {

    Name getName();

    List<? extends DocTree> getAttributes();

    /** Whether the tag closes itself, such as `<br/>`. */
    boolean isSelfClosing();
}
