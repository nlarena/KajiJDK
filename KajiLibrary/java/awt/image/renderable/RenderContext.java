package java.awt.image.renderable;

import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

/**
 * KajiLibrary's java.awt.image.renderable.RenderContext -- que parte, a que escala y con que
 * criterio.
 *
 * <p>Las tres cosas que hacen falta para convertir una {@link RenderableImage} en pixeles:
 *
 * <ul>
 *   <li>la <b>transformacion</b>, que es la que fija la resolucion. No hay un campo "escala": el
 *       tamano sale de cuanto agranda la matriz, y por eso el mismo objeto sirve para escalar, rotar
 *       y sesgar sin tener un metodo para cada cosa;
 *   <li>el <b>area de interes</b>, para no calcular lo que no se va a ver. Es un {@link Shape} y no
 *       un rectangulo porque una region rotada no es un rectangulo;
 *   <li>las <b>preferencias</b>, que dicen si se prefiere calidad o velocidad.
 * </ul>
 *
 * <h2>Los cuatro metodos de transformacion, y dos que son un error de tipeo</h2>
 *
 * <p>{@link #concatenateTransform} compone <b>despues</b> --se aplica primero lo que ya habia-- y
 * {@link #preConcatenateTransform} compone antes. La diferencia importa porque componer matrices no
 * conmuta: rotar y despues mover no es lo mismo que mover y despues rotar.
 *
 * <p>{@link #concetenateTransform} y {@link #preConcetenateTransform} son los mismos metodos con el
 * nombre mal escrito. Salieron asi en 1999, y estan obsoletos desde que se agregaron los correctos.
 * Siguen porque hay codigo compilado que los llama: sacarlos no arreglaria nada y rompería eso.
 */
public class RenderContext implements Cloneable {

    private AffineTransform usr2dev;

    private Shape aoi;

    private RenderingHints hints;

    /**
     * Todo explicito.
     *
     * <p>La transformacion se copia, para que moverla afuera no mueva la del contexto.
     *
     * @throws NullPointerException si la transformacion es null. No hay defensa contra eso ni la
     *     habia en el JDK, y esta bien que no la haya: un contexto sin transformacion no tiene
     *     resolucion, y sustituirla por la identidad en silencio produciria una renderizacion a
     *     escala 1 que nadie pidio
     */
    public RenderContext(AffineTransform usr2dev, Shape aoi, RenderingHints hints) {
        this.hints = hints;
        this.aoi = aoi;
        this.usr2dev = (AffineTransform) usr2dev.clone();
    }

    /** Solo la transformacion: toda la imagen y sin preferencias. */
    public RenderContext(AffineTransform usr2dev) {
        this(usr2dev, null, null);
    }

    /** Sin area de interes. */
    public RenderContext(AffineTransform usr2dev, RenderingHints hints) {
        this(usr2dev, null, hints);
    }

    /** Sin preferencias. */
    public RenderContext(AffineTransform usr2dev, Shape aoi) {
        this(usr2dev, aoi, null);
    }

    /** Las preferencias de calidad contra velocidad, o null. */
    public RenderingHints getRenderingHints() {
        return this.hints;
    }

    /** Ver {@link #getRenderingHints}. */
    public void setRenderingHints(RenderingHints hints) {
        this.hints = hints;
    }

    /** La transformacion de coordenadas de usuario a dispositivo. Se guarda una copia. */
    public void setTransform(AffineTransform newTransform) {
        this.usr2dev = (AffineTransform) newTransform.clone();
    }

    /** Compone {@code modTransform} <b>antes</b> de la que ya habia. */
    public void preConcatenateTransform(AffineTransform modTransform) {
        this.usr2dev.preConcatenate(modTransform);
    }

    /**
     * Igual que {@link #preConcatenateTransform}.
     *
     * @deprecated el nombre esta mal escrito; ver la nota de la clase
     */
    @Deprecated
    public void preConcetenateTransform(AffineTransform modTransform) {
        preConcatenateTransform(modTransform);
    }

    /** Compone {@code modTransform} <b>despues</b> de la que ya habia. */
    public void concatenateTransform(AffineTransform modTransform) {
        this.usr2dev.concatenate(modTransform);
    }

    /**
     * Igual que {@link #concatenateTransform}.
     *
     * @deprecated el nombre esta mal escrito; ver la nota de la clase
     */
    @Deprecated
    public void concetenateTransform(AffineTransform modTransform) {
        concatenateTransform(modTransform);
    }

    /** La transformacion. Se devuelve una copia, para que nadie la mueva por atras. */
    public AffineTransform getTransform() {
        return (AffineTransform) this.usr2dev.clone();
    }

    /** La region que interesa, o null para toda la imagen. */
    public void setAreaOfInterest(Shape newAoi) {
        this.aoi = newAoi;
    }

    /** Ver {@link #setAreaOfInterest}. */
    public Shape getAreaOfInterest() {
        return this.aoi;
    }

    /**
     * Una copia.
     *
     * <p>La transformacion se copia de verdad --es mutable y compartirla arruinaria las dos
     * copias--; el area de interes y las preferencias se comparten, que es lo que hace el JDK.
     */
    public Object clone() {
        RenderContext copy = new RenderContext(this.usr2dev, this.aoi, this.hints);
        return copy;
    }
}
