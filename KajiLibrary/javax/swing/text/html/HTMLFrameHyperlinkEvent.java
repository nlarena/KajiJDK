package javax.swing.text.html;

import java.awt.event.InputEvent;
import java.net.URL;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent$EventType;
import javax.swing.text.Element;

/**
 * Un enlace que hay que abrir en un marco determinado.
 *
 * <h2>Que agrega</h2>
 *
 * <p>Un {@link HyperlinkEvent} comun dice a donde ir. Este dice ademas <em>donde</em> mostrarlo: el
 * valor del atributo <code>target</code>, que puede ser el nombre de un marco o una de las palabras
 * reservadas (<code>_self</code>, <code>_parent</code>, <code>_top</code>, <code>_blank</code>).
 *
 * <p>Quien maneje el evento tiene que mirar ese destino. Si lo ignora y carga el documento en el
 * marco donde se hizo clic, un enlace con <code>target="_top"</code> va a reemplazar el marco en
 * lugar de la ventana entera, que es justo lo contrario de lo que pide la pagina.
 */
public class HTMLFrameHyperlinkEvent extends HyperlinkEvent {

    private String targetFrame;

    /** Un evento con esa direccion y ese destino. */
    public HTMLFrameHyperlinkEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            String targetFrame) {
        super(source, type, targetURL);
        this.targetFrame = targetFrame;
    }

    /** Un evento con direccion, descripcion y destino. */
    public HTMLFrameHyperlinkEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            String desc, String targetFrame) {
        super(source, type, targetURL, desc);
        this.targetFrame = targetFrame;
    }

    /** Un evento que ademas sabe de que elemento del documento salio. */
    public HTMLFrameHyperlinkEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            Element sourceElement, String targetFrame) {
        super(source, type, targetURL, null, sourceElement);
        this.targetFrame = targetFrame;
    }

    /** Un evento con descripcion y elemento de origen. */
    public HTMLFrameHyperlinkEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            String desc, Element sourceElement, String targetFrame) {
        super(source, type, targetURL, desc, sourceElement);
        this.targetFrame = targetFrame;
    }

    /** Un evento completo, con el evento de entrada que lo provoco. */
    public HTMLFrameHyperlinkEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            String desc, Element sourceElement, InputEvent inputEvent, String targetFrame) {
        super(source, type, targetURL, desc, sourceElement, inputEvent);
        this.targetFrame = targetFrame;
    }

    /** El marco donde hay que mostrar el documento; ver la nota de la clase. */
    public String getTarget() {
        return targetFrame;
    }
}
