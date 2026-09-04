package java.applet;

import java.net.URL;

/**
 * Lo que el navegador le cuenta al applet sobre dónde está corriendo.
 *
 * <p>Un applet no sabe nada por sí mismo: ni qué página lo contiene, ni de dónde se bajó, ni qué
 * parámetros le pusieron en el HTML. Todo eso se lo da el navegador a través de este objeto, que le
 * pone con {@link Applet#setStub}. Es un "stub" en el sentido viejo: un representante de algo que
 * está del otro lado.
 *
 * @deprecated el modelo de applets está en desuso desde Java 9 y marcado para borrarse desde 17.
 */
@Deprecated(since = "9", forRemoval = true)
public interface AppletStub {

    /** Si el applet está corriendo, o sea entre {@link Applet#start} y {@link Applet#stop}. */
    boolean isActive();

    /** La dirección de la página que contiene al applet. */
    URL getDocumentBase();

    /** La dirección de la que se bajó el código del applet. */
    URL getCodeBase();

    /**
     * El valor de un parámetro del HTML.
     *
     * @return el valor, o `null` si no hay un parámetro con ese nombre
     */
    String getParameter(String name);

    /** El navegador, visto como contexto del applet. */
    AppletContext getAppletContext();

    /** Le pide al navegador que le dé al applet ese tamaño. */
    void appletResize(int width, int height);
}
