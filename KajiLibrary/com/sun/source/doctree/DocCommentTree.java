package com.sun.source.doctree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * El comentario entero, y la raiz de todo arbol de este paquete.
 *
 * <p>La particion en {@link #getFirstSentence} y {@link #getBody} no es decorativa: la primera
 * oracion es la que javadoc muestra en las tablas de resumen, asi que donde termina cambia lo que
 * se ve. {@link #getFullBody} es la concatenacion, para quien no necesita esa distincion.
 */
public interface DocCommentTree extends DocTree {

    /** La primera oracion, que es lo que aparece en las tablas de resumen. */
    List<? extends DocTree> getFirstSentence();

    /** La primera oracion y el cuerpo, concatenados. */
    default List<? extends DocTree> getFullBody() {
        List<DocTree> completo = new ArrayList<DocTree>();
        completo.addAll(getFirstSentence());
        completo.addAll(getBody());
        return completo;
    }

    /** El cuerpo, sin la primera oracion. */
    List<? extends DocTree> getBody();

    /** Los tags de bloque, en el orden en que se escribieron. */
    List<? extends DocTree> getBlockTags();

    /** Lo que hay antes del contenido en un archivo suelto: el `<!DOCTYPE>`, el `<head>`. */
    default List<? extends DocTree> getPreamble() {
        return Collections.<DocTree>emptyList();
    }

    /** Lo que hay despues del contenido en un archivo suelto. */
    default List<? extends DocTree> getPostamble() {
        return Collections.<DocTree>emptyList();
    }
}
