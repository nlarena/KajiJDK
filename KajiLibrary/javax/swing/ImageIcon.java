package javax.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.net.URL;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * Un icono hecho de una imagen.
 *
 * <h2>Carga antes de devolver, y por eso mide</h2>
 *
 * <p>Una {@link Image} de AWT se carga <em>de a poco</em>: al crearla no se sabe cuanto mide, y las
 * medidas van llegando mientras se lee el archivo. Un icono no puede darse ese lujo -- lo primero
 * que le preguntan es {@link #getIconWidth} -- asi que esta clase espera a que la imagen termine de
 * cargar antes de contestar.
 *
 * <p>Quien espera es un {@link MediaTracker}, y hay <strong>uno solo para toda la aplicacion</strong>
 * ({@link #tracker}): cada icono se anota con un identificador propio, espera lo suyo y se
 * desanota. Compartirlo es lo que evita crear un rastreador por icono en una pantalla con cien.
 *
 * <h2>La descripcion no se dibuja</h2>
 *
 * <p>{@link #setDescription} es texto para quien no ve la imagen -- un lector de pantalla --. No
 * aparece en ningun lado; su unico uso es la accesibilidad.
 *
 * <h2>Sin pantalla</h2>
 *
 * <p>Cargar una imagen no necesita pantalla: se lee, se decodifica y se sabe cuanto mide. Lo que
 * necesita pantalla es dibujarla, y {@link #paintIcon} recibe el {@link Graphics} de quien sea --
 * una imagen en memoria sirve.
 */
public class ImageIcon implements Icon, Serializable, Accessible {

    /** La imagen. */
    transient Image image;

    /** Como quedo la carga; ver {@link MediaTracker}. */
    transient int loadStatus = 0;

    ImageObserver imageObserver;

    String description = null;

    /** El componente al que se le cuelga el rastreador; no se dibuja nunca. */
    protected static final Component component = new ComponenteDeCarga();

    /** El rastreador compartido; ver la nota de la clase. */
    protected static final MediaTracker tracker = new MediaTracker(component);

    int width = -1;
    int height = -1;

    private static int siguienteId = 0;

    /** Un componente que existe solo para colgarle el rastreador. */
    private static class ComponenteDeCarga extends Component {
    }

    /** Desde un archivo, con descripcion. */
    public ImageIcon(String filename, String description) {
        image = Toolkit.getDefaultToolkit().getImage(filename);
        this.description = description;
        if (image == null) {
            // Sin decodificador el toolkit devuelve nulo donde el JDK devuelve una imagen que
            // despues falla al cargar. El resultado que se ve es el mismo, y se anota igual:
            // medidas en -1 y la carga en ERRORED. Ver la nota de la clase.
            loadStatus = MediaTracker.ERRORED;
            return;
        }
        loadImage(image);
    }

    /** Desde un archivo; la descripcion es el nombre del archivo. */
    public ImageIcon(String filename) {
        this(filename, filename);
    }

    /** Desde una direccion, con descripcion. */
    public ImageIcon(URL location, String description) {
        image = Toolkit.getDefaultToolkit().getImage(location);
        this.description = description;
        if (image == null) {
            // Sin decodificador el toolkit devuelve nulo donde el JDK devuelve una imagen que
            // despues falla al cargar. El resultado que se ve es el mismo, y se anota igual:
            // medidas en -1 y la carga en ERRORED. Ver la nota de la clase.
            loadStatus = MediaTracker.ERRORED;
            return;
        }
        loadImage(image);
    }

    /** Desde una direccion; la descripcion es la direccion. */
    public ImageIcon(URL location) {
        this(location, location.toExternalForm());
    }

    /** De una imagen ya hecha, con descripcion. */
    public ImageIcon(Image image, String description) {
        this(image);
        this.description = description;
    }

    /**
     * De una imagen ya hecha.
     *
     * <p>Si la imagen trae una descripcion adentro -- se la puede poner con
     * {@code setProperty("comment", ...)} -- se la usa.
     */
    public ImageIcon(Image image) {
        this.image = image;
        Object o = image.getProperty("comment", imageObserver);
        if (o instanceof String) {
            description = (String) o;
        }
        loadImage(image);
    }

    /** De los bytes de un archivo de imagen, con descripcion. */
    public ImageIcon(byte[] imageData, String description) {
        this.image = Toolkit.getDefaultToolkit().createImage(imageData);
        if (image == null) {
            return;
        }
        this.description = description;
        loadImage(image);
    }

    /** De los bytes; la descripcion sale de la imagen si la trae. */
    public ImageIcon(byte[] imageData) {
        this.image = Toolkit.getDefaultToolkit().createImage(imageData);
        if (image == null) {
            return;
        }
        Object o = image.getProperty("comment", imageObserver);
        if (o instanceof String) {
            description = (String) o;
        }
        loadImage(image);
    }

    /** Un icono vacio, para llenarlo despues con {@link #setImage}. */
    public ImageIcon() {
    }

    /**
     * Espera a que la imagen termine de cargar y anota cuanto mide.
     *
     * <p>El identificador tiene que ser distinto por icono: el rastreador es uno solo y dos iconos
     * con el mismo numero se esperarian entre si. Ver la nota de la clase.
     */
    protected void loadImage(Image image) {
        MediaTracker mTracker = tracker;
        int id = proximoId();
        synchronized (mTracker) {
            mTracker.addImage(image, id);
            try {
                mTracker.waitForID(id, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                loadStatus = mTracker.statusID(id, false);
                mTracker.removeImage(image, id);
            }
        }
        width = image.getWidth(imageObserver);
        height = image.getHeight(imageObserver);
    }

    private static synchronized int proximoId() {
        siguienteId = siguienteId + 1;
        return siguienteId;
    }

    /** Como quedo la carga; las constantes son las de {@link MediaTracker}. */
    public int getImageLoadStatus() {
        return loadStatus;
    }

    public Image getImage() {
        return image;
    }

    /** Cambia la imagen; vuelve a esperar y a medir. */
    public void setImage(Image image) {
        this.image = image;
        loadImage(image);
    }

    /** El texto para quien no ve la imagen; ver la nota de la clase. */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** Dibuja la imagen en esa posicion. */
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        if (imageObserver == null) {
            g.drawImage(image, x, y, c);
        } else {
            g.drawImage(image, x, y, imageObserver);
        }
    }

    /** El ancho, o -1 si la imagen no se pudo cargar. */
    public int getIconWidth() {
        return width;
    }

    /** El alto, o -1 si la imagen no se pudo cargar. */
    public int getIconHeight() {
        return height;
    }

    /**
     * Quien se entera de que la imagen avanzo.
     *
     * <p>Hace falta para las imagenes que siguen cambiando despues de cargadas -- un GIF animado --:
     * sin observador se dibuja el primer cuadro y ahi queda.
     */
    public void setImageObserver(ImageObserver observer) {
        imageObserver = observer;
    }

    public ImageObserver getImageObserver() {
        return imageObserver;
    }

    public String toString() {
        if (description != null) {
            return description;
        }
        return super.toString();
    }

    private AccessibleContext accessibleContext = null;

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
