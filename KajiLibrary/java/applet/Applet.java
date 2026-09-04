package java.applet;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Panel;
import java.net.URL;
import java.util.Locale;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * Un programa chico que corre **adentro de una página web**, dibujado por el navegador.
 *
 * <p>Es un {@link Panel} con un ciclo de vida que maneja el navegador: {@link #init} al cargarlo,
 * {@link #start} cada vez que la página se muestra, {@link #stop} cada vez que se deja de ver,
 * {@link #destroy} al descartarlo. Un applet no tiene `main`: lo que hace lo hace en esos cuatro
 * métodos, y el resto —parámetros, imágenes, sonidos, la barra de estado— se lo pide al navegador a
 * través de su {@link AppletStub}.
 *
 * <p><strong>Sin pantalla no se puede construir.</strong> El constructor tira
 * {@link HeadlessException}, igual que en el JDK: un applet existe para ser mostrado por un
 * navegador, y sin sistema de ventanas no hay navegador ni superficie. Es la misma decisión que en
 * {@code TrayIcon}: de un applet no cuelga ninguna otra clase, así que no hay motivo para divergir
 * del JDK como sí lo hay en {@code Window}. Los métodos de instancia están declarados porque son
 * parte de la clase, pero no existe ninguna instancia desde la que llamarlos.
 *
 * <p>Todo el paquete está en este árbol por una sola razón: {@code java.beans.AppletInitializer} y
 * una de las formas de {@code java.beans.Beans.instantiate} nombran a esta clase, y sin ella
 * `java.beans` no se podía cerrar. Son cuatro tipos chicos y se pudieron escribir enteros.
 *
 * @deprecated el modelo de applets está en desuso desde Java 9 y marcado para borrarse desde 17: los
 *     navegadores dejaron de ejecutarlos.
 */
@Deprecated(since = "9", forRemoval = true)
public class Applet extends Panel {

    private static final long serialVersionUID = -5836846270535785031L;

    /** El representante del navegador, o `null` hasta que el navegador lo ponga. */
    private transient AppletStub stub;

    /** La accesibilidad, armada al primer pedido. */
    AccessibleContext accessibleContext;

    /**
     * Un applet.
     *
     * @throws HeadlessException si no hay pantalla, o sea siempre acá
     */
    public Applet() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
    }

    /**
     * Le pone el representante del navegador.
     *
     * <p>Es `final` y lo llama el navegador, no el applet: es el único enlace entre los dos y un
     * applet que se lo cambiara a sí mismo se quedaría hablando solo.
     */
    public final void setStub(AppletStub stub) {
        this.stub = stub;
    }

    /**
     * Si está corriendo, o sea entre {@link #start} y {@link #stop}.
     *
     * @return `false` también mientras no tenga navegador: sin él no arrancó
     */
    public boolean isActive() {
        return this.stub != null && this.stub.isActive();
    }

    /**
     * La dirección de la página que lo contiene.
     *
     * @throws NullPointerException si todavía no tiene navegador
     */
    public URL getDocumentBase() {
        return this.stub.getDocumentBase();
    }

    /**
     * La dirección de la que se bajó su código.
     *
     * @throws NullPointerException si todavía no tiene navegador
     */
    public URL getCodeBase() {
        return this.stub.getCodeBase();
    }

    /**
     * El valor de un parámetro puesto en el HTML.
     *
     * @return el valor, o `null` si no hay un parámetro con ese nombre
     * @throws NullPointerException si todavía no tiene navegador
     */
    public String getParameter(String name) {
        return this.stub.getParameter(name);
    }

    /**
     * El navegador, visto como contexto.
     *
     * @throws NullPointerException si todavía no tiene navegador
     */
    public AppletContext getAppletContext() {
        return this.stub.getAppletContext();
    }

    /**
     * Le pide al navegador ese tamaño.
     *
     * <p>Redefine el de {@code Component} porque un applet no decide su tamaño: lo decide el
     * navegador, y el pedido tiene que pasar por él.
     */
    public void resize(int width, int height) {
        Dimension d = this.size();
        if (d.width != width || d.height != height) {
            super.resize(width, height);
            if (this.stub != null) {
                this.stub.appletResize(width, height);
            }
        }
    }

    /** Lo mismo, con una dimensión. */
    public void resize(Dimension d) {
        this.resize(d.width, d.height);
    }

    /**
     * Si es raíz de validación.
     *
     * @return `true`: un applet es la raíz de su propio árbol, así que la validación no tiene por
     *     qué subir más allá de él
     */
    public boolean isValidateRoot() {
        return true;
    }

    /** Escribe en la barra de estado del navegador. */
    public void showStatus(String msg) {
        this.getAppletContext().showStatus(msg);
    }

    /** Una imagen en esa dirección; la carga empieza recién cuando alguien la dibuja. */
    public Image getImage(URL url) {
        return this.getAppletContext().getImage(url);
    }

    /**
     * Una imagen en esa dirección relativa a otra.
     *
     * @return la imagen, o `null` si la dirección está mal formada
     */
    public Image getImage(URL url, String name) {
        try {
            return this.getImage(new URL(url, name));
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }

    /**
     * Un sonido en esa dirección, sin necesidad de un applet ni de un navegador.
     *
     * <p>Es `static` porque es la única entrada al sonido de este paquete que sirve fuera de un
     * navegador, y por eso se usaba desde programas comunes. Acá el clip se arma pero no puede
     * sonar: sus tres verbos tiran, con el motivo dicho, porque esta biblioteca no tiene motor de
     * audio. Devolver un clip que "reproduce" en silencio sería prometer un sonido que no hay.
     *
     * <p>Una dirección `null` **se acepta**, igual que en el JDK: el clip es vago y no toca la
     * dirección hasta que alguien lo reproduce, así que acá no hay nada que falle todavía.
     */
    public static final AudioClip newAudioClip(URL url) {
        return new ClipMudo(url);
    }

    /** Un sonido en esa dirección, a través del navegador. */
    public AudioClip getAudioClip(URL url) {
        return this.getAppletContext().getAudioClip(url);
    }

    /**
     * Un sonido en esa dirección relativa a otra.
     *
     * @return el clip, o `null` si la dirección está mal formada
     */
    public AudioClip getAudioClip(URL url, String name) {
        try {
            return this.getAudioClip(new URL(url, name));
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }

    /**
     * Quién lo hizo y para qué.
     *
     * @return `null` de base; cada applet redefine esto con su autor, versión y descripción
     */
    public String getAppletInfo() {
        return null;
    }

    /**
     * El idioma del applet.
     *
     * <p>Un applet sin idioma propio usa el del sistema, no el de su padre como haría un componente
     * común: su "padre" es el navegador, que no es un componente.
     */
    public Locale getLocale() {
        Locale l = super.getLocale();
        if (l == null) {
            return Locale.getDefault();
        }
        return l;
    }

    /**
     * Qué parámetros entiende.
     *
     * @return `null` de base; cada applet redefine esto con filas de {nombre, tipo, descripción}
     */
    public String[][] getParameterInfo() {
        return null;
    }

    /** Reproduce el sonido de esa dirección, una vez. */
    public void play(URL url) {
        AudioClip clip = this.getAudioClip(url);
        if (clip != null) {
            clip.play();
        }
    }

    /** Reproduce el sonido de esa dirección relativa a otra, una vez. */
    public void play(URL url, String name) {
        AudioClip clip = this.getAudioClip(url, name);
        if (clip != null) {
            clip.play();
        }
    }

    /** Lo llama el navegador al cargarlo; el de base no hace nada. */
    public void init() {
    }

    /** Lo llama el navegador cada vez que la página se muestra; el de base no hace nada. */
    public void start() {
    }

    /** Lo llama el navegador cada vez que la página deja de verse; el de base no hace nada. */
    public void stop() {
    }

    /** Lo llama el navegador al descartarlo; el de base no hace nada. */
    public void destroy() {
    }

    /** La accesibilidad del applet. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleApplet();
        }
        return this.accessibleContext;
    }

    /**
     * Un applet, para la accesibilidad, es un marco: la raíz de un árbol de componentes que el
     * usuario ve como una unidad.
     */
    protected class AccessibleApplet extends AccessibleAWTPanel {

        /** Para las subclases. */
        protected AccessibleApplet() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.FRAME;
        }
    }

    /**
     * Un clip que no puede sonar.
     *
     * <p>Guarda la dirección, que es lo único que se sabe de él, y tira en los tres verbos con el
     * motivo: no hay motor de audio. Es lo que corresponde a un sonido que existe pero no se puede
     * reproducir, que es distinto de un sonido que no existe.
     */
    private static final class ClipMudo implements AudioClip {

        private final URL url;

        private ClipMudo(URL url) {
            this.url = url;
        }

        public void play() {
            throw new UnsupportedOperationException(
                    "esta biblioteca no tiene motor de audio; no se puede reproducir " + this.url);
        }

        public void loop() {
            this.play();
        }

        /** Parar algo que nunca sonó no es un error: no hace nada. */
        public void stop() {
        }

        public String toString() {
            return "AudioClip[" + this.url + "]";
        }
    }
}
