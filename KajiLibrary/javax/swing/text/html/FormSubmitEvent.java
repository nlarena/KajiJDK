package javax.swing.text.html;

import java.net.URL;

import javax.swing.event.HyperlinkEvent$EventType;
import javax.swing.text.Element;

/**
 * El envio de un formulario, contado como si fuera un enlace.
 *
 * <h2>Por que es un evento de enlace</h2>
 *
 * <p>Enviar un formulario y seguir un enlace terminan igual: hay que cargar un documento de una
 * direccion. Hacer que el envio llegue por el mismo camino permite que un programa que ya sabe
 * atender enlaces atienda tambien los formularios sin cambiar nada.
 *
 * <p>Lo que agrega es lo unico que el enlace no puede llevar: el metodo ({@code GET} o
 * {@code POST}) y los datos ya armados. En un {@code GET} los datos ademas van pegados a la
 * direccion; en un {@code POST} solo estan aca, y quien no mire {@link #getData} los pierde.
 *
 * <h2>No se construye desde afuera</h2>
 *
 * <p>La clase es final y su constructor no es publico: los crea el {@link FormView} al enviar. Que
 * un programa fabricara uno no tendria sentido, porque no habria formulario del que salieran los
 * datos.
 */
public final class FormSubmitEvent extends HTMLFrameHyperlinkEvent {

    private MethodType method;
    private String data;

    FormSubmitEvent(Object source, HyperlinkEvent$EventType type, URL targetURL,
            Element sourceElement, String targetFrame, MethodType method, String data) {
        super(source, type, targetURL, sourceElement, targetFrame);
        this.method = method;
        this.data = data;
    }

    /** {@code GET} o {@code POST}. */
    public MethodType getMethod() {
        return method;
    }

    /** Los datos del formulario, ya codificados. */
    public String getData() {
        return data;
    }

    /** Los dos metodos que un formulario de HTML puede usar. */
    public enum MethodType {
        GET, POST
    }
}
