package java.awt;

import java.io.IOException;
import java.net.URL;

/**
 * La pantalla de bienvenida que la JVM muestra **antes** de que arranque el programa.
 *
 * <p>La particularidad es cuándo aparece: la muestra el lanzador, con la opción `-splash:` o el
 * atributo `SplashScreen-Image` del manifiesto, antes de cargar ninguna clase. Por eso no se puede
 * crear una: para cuando el programa corre, o ya está o no va a estar nunca. Lo único que se puede
 * hacer es dibujarle encima —una barra de progreso— y cerrarla.
 *
 * <p><strong>Acá nunca hay ninguna.</strong> {@link #getSplashScreen} tira
 * {@link HeadlessException}, no devuelve `null`, y la diferencia es la de siempre: `null` significa
 * "no se pidió ninguna", y lo que pasa acá es que **no hay dónde mostrarla**. Son dos situaciones
 * distintas y el JDK las distingue igual. Los métodos de instancia están declarados porque son parte
 * de la clase, pero no hay forma de llegar a ellos: no existe ninguna instancia.
 */
public final class SplashScreen {

    /** El identificador nativo de la ventana; siempre 0 acá. */
    private final long splashPtr;

    /** Si se cerró. */
    private boolean cerrada;

    /** La imagen que muestra. */
    private URL imagen;

    /** La arma el lanzador, nadie más. */
    SplashScreen(long ptr) {
        this.splashPtr = ptr;
    }

    /**
     * La pantalla de bienvenida de este programa.
     *
     * @return la pantalla, o `null` si el programa no arrancó con una
     * @throws HeadlessException siempre acá: sin pantalla no hay dónde mostrarla
     */
    public static SplashScreen getSplashScreen() {
        synchronized (SplashScreen.class) {
            if (GraphicsEnvironment.isHeadless()) {
                throw new HeadlessException();
            }
            return null;
        }
    }

    /**
     * Cambia la imagen que muestra.
     *
     * <p>El tamaño de la ventana se ajusta a la imagen nueva y la ventana se recentra, que es lo que
     * hace que valga la pena: sirve para una animación de arranque.
     *
     * @throws NullPointerException si la dirección es `null`
     * @throws IOException si la imagen no se puede leer
     * @throws IllegalStateException si la pantalla ya se cerró
     */
    public void setImageURL(URL imageURL) throws NullPointerException, IOException,
            IllegalStateException {
        this.comprobarViva();
        if (imageURL == null) {
            throw new NullPointerException("imageURL");
        }
        this.imagen = imageURL;
    }

    /**
     * De dónde salió la imagen.
     *
     * @throws IllegalStateException si la pantalla ya se cerró
     */
    public URL getImageURL() throws IllegalStateException {
        this.comprobarViva();
        return this.imagen;
    }

    /**
     * Dónde está y cuánto mide, en coordenadas de pantalla.
     *
     * @throws IllegalStateException si la pantalla ya se cerró
     */
    public Rectangle getBounds() throws IllegalStateException {
        this.comprobarViva();
        throw new IllegalStateException("no splash screen available");
    }

    /**
     * Cuánto mide.
     *
     * @throws IllegalStateException si la pantalla ya se cerró
     */
    public Dimension getSize() throws IllegalStateException {
        return this.getBounds().getSize();
    }

    /**
     * Un contexto para dibujarle encima.
     *
     * <p>Lo que se dibuje va sobre una capa **transparente** arriba de la imagen, así que dibujar no
     * borra lo que había: por eso hace falta {@link #update} para que se vea.
     *
     * @throws IllegalStateException si la pantalla ya se cerró
     */
    public Graphics2D createGraphics() throws IllegalStateException {
        this.comprobarViva();
        throw new IllegalStateException("no splash screen available");
    }

    /**
     * Muestra lo que se dibujó desde la última vez.
     *
     * @throws IllegalStateException si la pantalla ya se cerró
     */
    public void update() throws IllegalStateException {
        this.comprobarViva();
    }

    /**
     * La cierra y suelta sus recursos.
     *
     * <p>Después de esto la instancia queda inservible: todos los demás métodos tiran.
     *
     * @throws IllegalStateException si ya estaba cerrada
     */
    public void close() throws IllegalStateException {
        this.comprobarViva();
        this.cerrada = true;
    }

    /** Marca que se cerró desde afuera —al mostrarse la primera ventana del programa—. */
    void markClosed() {
        this.cerrada = true;
    }

    /** Si sigue en pantalla. */
    public boolean isVisible() {
        return !this.cerrada && this.splashPtr != 0;
    }

    /** Tira si ya se cerró. */
    private void comprobarViva() {
        if (this.cerrada) {
            throw new IllegalStateException("no splash screen available");
        }
    }
}
