package java.awt;

import java.awt.datatransfer.Clipboard;
import java.awt.im.InputMethodHighlight;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * El juego de herramientas de una máquina sin pantalla.
 *
 * <p>La regla es una sola y se aplica en todos lados: **lo que se puede contestar sin pantalla se
 * contesta de verdad; lo que la necesita tira {@link HeadlessException}**. No hay ningún valor
 * inventado. Un tamaño de pantalla que no existe no se aproxima, una fuente que no se puede medir no
 * se estima.
 *
 * <p>Es más de lo que parece. Funcionan la cola de eventos con su hilo de despacho, el modelo de
 * color, la construcción de imágenes desde un productor de píxeles, la preparación y comprobación de
 * imágenes, y las propiedades del escritorio.
 *
 * <p>El portapapeles que devuelve es **privado**: vive dentro de este proceso. No es un sustituto
 * del portapapeles del sistema disfrazado — es un portapapeles de verdad que sirve para mover datos
 * entre partes de la misma aplicación, que es todo lo que se puede hacer sin un escritorio con el que
 * compartir.
 *
 * <p>No es pública: se llega por {@link Toolkit#getDefaultToolkit}.
 */
class HeadlessToolkit extends Toolkit {

    private final EventQueue eventQueue = new EventQueue();
    private final Clipboard clipboard = new Clipboard("System");

    /** El juego de herramientas sin pantalla. */
    HeadlessToolkit() {
    }

    /**
     * El tamaño de la pantalla.
     *
     * @throws HeadlessException siempre: no hay pantalla que medir
     */
    public Dimension getScreenSize() throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * Los puntos por pulgada.
     *
     * @throws HeadlessException siempre, por el mismo motivo
     */
    public int getScreenResolution() throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * El formato de píxel.
     *
     * <p>Ésta **sí** se puede contestar: es el ARGB de ocho bits por canal, que es el formato en el
     * que la biblioteca trabaja con color sin importar en qué pantalla se muestre.
     */
    public ColorModel getColorModel() {
        return ColorModel.getRGBdefault();
    }

    /**
     * Las fuentes instaladas.
     *
     * <p>Las cinco familias lógicas, que son las que existen sin motor tipográfico: son nombres, no
     * archivos, y esta biblioteca los reconoce.
     */
    public String[] getFontList() {
        String[] out = new String[5];
        out[0] = Font.DIALOG;
        out[1] = Font.DIALOG_INPUT;
        out[2] = Font.SANS_SERIF;
        out[3] = Font.SERIF;
        out[4] = Font.MONOSPACED;
        return out;
    }

    /**
     * Las medidas de una fuente.
     *
     * @throws UnsupportedOperationException siempre: medir texto exige leer los glifos del archivo
     *     de la fuente, y esta biblioteca no trae motor tipográfico. Es la misma frontera que parte
     *     a {@link Font} en dos mitades.
     */
    /**
     * Las metricas de la unica fuente de esta VM, sea cual sea la pedida.
     *
     * <p>Ver {@link KajiFontMetrics}: toda fuente se sustituye por la misma cara, y las metricas
     * son las de esa cara, que es lo que el rasterizador efectivamente pinta.
     */
    public FontMetrics getFontMetrics(Font font) {
        return new KajiFontMetrics(font);
    }

    /** No hay nada pendiente de dibujar: no hay pantalla. */
    public void sync() {
    }

    /**
     * Una imagen leída de un archivo.
     *
     * @return `null` si el archivo no se puede leer o no es una imagen que se sepa decodificar
     */
    public Image getImage(String filename) {
        return this.createImage(filename);
    }

    /**
     * Una imagen leída de una dirección.
     *
     * @return `null` si no se puede leer
     */
    public Image getImage(URL url) {
        return this.createImage(url);
    }

    /**
     * Una imagen leída de un archivo.
     *
     * @return `null` siempre: decodificar PNG o JPEG es trabajo de `javax.imageio`, que esta
     *     biblioteca no trae. Devolver una imagen vacía sería peor: quien la dibujara no vería nada
     *     y no sabría por qué.
     */
    public Image createImage(String filename) {
        return null;
    }

    /**
     * Una imagen leída de una dirección.
     *
     * @return `null` siempre, por el mismo motivo
     */
    public Image createImage(URL url) {
        return null;
    }

    /**
     * Una imagen a partir de un productor de píxeles.
     *
     * <p>Ésta **sí** funciona, y es la que importa: no hay nada que decodificar, los píxeles ya
     * vienen dados. Es lo que hace que la tubería de filtros de {@code java.awt.image} sirva de
     * punta a punta.
     *
     * @return la imagen, o `null` si el productor no llegó a entregarla
     */
    public Image createImage(ImageProducer producer) {
        PixelGrabber pg = new PixelGrabber(producer, 0, 0, -1, -1, null, 0, 0);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
            return null;
        }
        int w = pg.getWidth();
        int h = pg.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, (int[]) pg.getPixels(), 0, w);
        return out;
    }

    /**
     * Una imagen decodificada de unos bytes.
     *
     * @return `null` siempre: hace falta un decodificador de formatos de imagen
     */
    public Image createImage(byte[] imagedata, int imageoffset, int imagelength) {
        return null;
    }

    /**
     * Empieza a preparar una imagen.
     *
     * <p>Una imagen que ya está en memoria no necesita prepararse: contesta que sí de una.
     */
    public boolean prepareImage(Image image, int width, int height, ImageObserver observer) {
        return image != null && image.getWidth(observer) >= 0;
    }

    /**
     * Cuánto se preparó de una imagen.
     *
     * <p>Una imagen en memoria está entera desde el principio.
     */
    public int checkImage(Image image, int width, int height, ImageObserver observer) {
        if (image == null) {
            return ImageObserver.ERROR | ImageObserver.ABORT;
        }
        if (image.getWidth(observer) < 0) {
            return ImageObserver.ERROR | ImageObserver.ABORT;
        }
        return ImageObserver.WIDTH | ImageObserver.HEIGHT | ImageObserver.ALLBITS;
    }

    /**
     * Un trabajo de impresión.
     *
     * @return `null` siempre: no hay sistema de impresión al que mandarlo
     */
    public PrintJob getPrintJob(Frame frame, String jobtitle, Properties props) {
        return null;
    }

    /** No hay campanilla que tocar. */
    public void beep() {
    }

    /**
     * El portapapeles.
     *
     * <p>Es uno **privado** de este proceso, no el del sistema: no hay sistema con el que compartir.
     * Sirve igual para mover datos entre partes de la misma aplicación.
     */
    public Clipboard getSystemClipboard() {
        return this.clipboard;
    }

    /** Si admite ese alcance de modalidad. */
    public boolean isModalityTypeSupported(Dialog.ModalityType modalityType) {
        return modalityType == Dialog.ModalityType.MODELESS;
    }

    /** Si admite ese tipo de exclusión. */
    public boolean isModalExclusionTypeSupported(Dialog.ModalExclusionType type) {
        return type == Dialog.ModalExclusionType.NO_EXCLUDE;
    }

    /** La cola de eventos: ésta funciona de verdad, con su hilo de despacho. */
    protected EventQueue getSystemEventQueueImpl() {
        return this.eventQueue;
    }

    /**
     * Cómo dibujar un tramo en composición.
     *
     * @return un mapa vacío: no hay método de entrada que componga nada
     */
    public Map<java.awt.font.TextAttribute, ?> mapInputMethodHighlight(
            InputMethodHighlight highlight) {
        return new HashMap<java.awt.font.TextAttribute, Object>();
    }
}
