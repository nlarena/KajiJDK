package java.applet;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * El navegador, visto desde adentro de un applet.
 *
 * <p>Es lo que el applet usa para pedirle cosas al entorno que lo aloja: mostrar otra página,
 * escribir en la barra de estado, cargar una imagen o un sonido, y encontrar a los **otros
 * applets** de la misma página, que es la forma en que dos applets se hablan entre sí.
 *
 * <p>Los tres métodos de "streams" son un cajón compartido por página: un applet deja un flujo con
 * un nombre y otro applet de la misma página lo puede leer. Nunca fue muy usado.
 *
 * @deprecated el modelo de applets está en desuso desde Java 9 y marcado para borrarse desde 17.
 */
@Deprecated(since = "9", forRemoval = true)
public interface AppletContext {

    /** Un sonido en esa dirección. */
    AudioClip getAudioClip(URL url);

    /** Una imagen en esa dirección; la carga empieza recién cuando alguien la dibuja. */
    Image getImage(URL url);

    /**
     * El applet de esa página que tiene ese nombre.
     *
     * @return el applet, o `null` si no hay ninguno con ese nombre
     */
    Applet getApplet(String name);

    /** Todos los applets de la página, incluido el que pregunta. */
    Enumeration<Applet> getApplets();

    /** Le pide al navegador que reemplace la página actual por ésa. */
    void showDocument(URL url);

    /**
     * Le pide al navegador que muestre esa página en esa ventana o marco.
     *
     * @param target un nombre de marco, o `_self`, `_parent`, `_top` o `_blank`
     */
    void showDocument(URL url, String target);

    /** Escribe en la barra de estado del navegador. */
    void showStatus(String status);

    /**
     * Deja un flujo con ese nombre en el cajón de la página.
     *
     * @throws IOException si el flujo no se pudo guardar
     */
    void setStream(String key, InputStream stream) throws IOException;

    /**
     * El flujo con ese nombre.
     *
     * @return el flujo, o `null` si no hay ninguno
     */
    InputStream getStream(String key);

    /** Los nombres de todos los flujos del cajón. */
    Iterator<String> getStreamKeys();
}
