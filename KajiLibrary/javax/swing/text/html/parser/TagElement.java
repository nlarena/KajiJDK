package javax.swing.text.html.parser;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML$Tag;

/**
 * Una etiqueta encontrada en el documento, atada a su elemento de la DTD.
 *
 * <h2>El puente entre las dos mitades</h2>
 *
 * <p>El analizador trabaja con {@link Element}, que viene de la DTD y sabe de reglas. El documento
 * trabaja con {@link HTML.Tag}, que sabe de como se muestra. Esta clase es la que junta los dos:
 * dado un elemento, busca la etiqueta que le corresponde, y si no hay ninguna arma una
 * {@link HTML.UnknownTag}.
 *
 * <h2>Etiquetas que nadie escribio</h2>
 *
 * <p>{@link #fictional} marca las que el analizador invento para cerrar el arbol: el
 * <code>&lt;p&gt;</code> que falta antes de un texto suelto, el <code>&lt;/li&gt;</code> que el
 * autor no puso. Quien reciba la etiqueta puede querer tratarlas distinto, por ejemplo al volver a
 * escribir el documento tal como estaba.
 */
public class TagElement {

    private final Element elem;
    private final HTML$Tag htmlTag;
    private final boolean insertedByErrorRecovery;

    /** Una etiqueta real para ese elemento. */
    public TagElement(Element elem) {
        this(elem, false);
    }

    /** Una etiqueta para ese elemento, real o inventada. */
    public TagElement(Element elem, boolean fictional) {
        this.elem = elem;
        htmlTag = HTML.getTag(elem.getName()) == null
                ? new HTML.UnknownTag(elem.getName()) : HTML.getTag(elem.getName());
        insertedByErrorRecovery = fictional;
    }

    /** Si corta la linea; lo contesta la etiqueta, no el elemento. */
    public boolean breaksFlow() {
        return htmlTag.breaksFlow();
    }

    public boolean isPreformatted() {
        return htmlTag.isPreformatted();
    }

    public Element getElement() {
        return elem;
    }

    public HTML$Tag getHTMLTag() {
        return htmlTag;
    }

    /** Si la invento el analizador; ver la nota de la clase. */
    public boolean fictional() {
        return insertedByErrorRecovery;
    }
}
