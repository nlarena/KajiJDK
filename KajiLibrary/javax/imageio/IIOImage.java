package javax.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.List;
import javax.imageio.metadata.IIOMetadata;

/**
 * KajiLibrary's javax.imageio.IIOImage -- una imagen con sus miniaturas y sus metadatos.
 *
 * <p>Lo que se lee o se escribe de una vez: el pixel, las vistas previas que el formato traiga
 * incrustadas, y la informacion asociada.
 *
 * <h2>Imagen o raster, nunca los dos</h2>
 *
 * <p>Es la parte que define la clase. Lleva <b>o</b> una {@link RenderedImage} <b>o</b> un
 * {@link Raster}, y {@link #hasRaster} dice cual. Poner uno pone el otro en null.
 *
 * <p>La diferencia es que una imagen sabe interpretar sus pixeles --tiene modelo de color-- y un
 * raster son numeros crudos. El raster existe para los formatos cuyos datos <b>no son colores</b>: una
 * imagen medica en unidades Hounsfield, una banda satelital en reflectancia. Forzar un modelo de color
 * ahi seria inventar.
 *
 * <p>Pedir el que no es no falla: devuelve null.
 *
 * <h2>Las miniaturas no se copian</h2>
 *
 * <p>La lista se guarda por referencia, y {@link #getThumbnails} la devuelve tal cual. Modificarla
 * despues cambia lo que la imagen tiene. Es lo que hace el JDK.
 */
public class IIOImage {

    /** El pixel como imagen, o null si hay raster. */
    protected RenderedImage image;

    /** El pixel como numeros crudos, o null si hay imagen. */
    protected Raster raster;

    /** Las vistas previas, o null. */
    protected List<? extends BufferedImage> thumbnails = null;

    /** La informacion asociada, o null. */
    protected IIOMetadata metadata;

    /**
     * Con una imagen.
     *
     * @param thumbnails las vistas previas, o null; no se copia
     * @param metadata la informacion asociada, o null
     * @throws IllegalArgumentException si la imagen es null
     */
    public IIOImage(RenderedImage image, List<? extends BufferedImage> thumbnails,
                    IIOMetadata metadata) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        this.image = image;
        this.raster = null;
        this.thumbnails = thumbnails;
        this.metadata = metadata;
    }

    /**
     * Con un raster. Ver la nota de la clase sobre cuando corresponde.
     *
     * @throws IllegalArgumentException si el raster es null
     */
    public IIOImage(Raster raster, List<? extends BufferedImage> thumbnails,
                    IIOMetadata metadata) {
        if (raster == null) {
            throw new IllegalArgumentException("raster == null!");
        }
        this.raster = raster;
        this.image = null;
        this.thumbnails = thumbnails;
        this.metadata = metadata;
    }

    /** La imagen, o null si lo que hay es un raster. */
    public RenderedImage getRenderedImage() {
        return this.image;
    }

    /** La pone, y saca el raster. */
    public void setRenderedImage(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        this.image = image;
        this.raster = null;
    }

    /** Cual de los dos lleva. Ver la nota de la clase. */
    public boolean hasRaster() {
        return this.raster != null;
    }

    /** El raster, o null si lo que hay es una imagen. */
    public Raster getRaster() {
        return this.raster;
    }

    /** Lo pone, y saca la imagen. */
    public void setRaster(Raster raster) {
        if (raster == null) {
            throw new IllegalArgumentException("raster == null!");
        }
        this.raster = raster;
        this.image = null;
    }

    /** Cuantas vistas previas. */
    public int getNumThumbnails() {
        if (this.thumbnails == null) {
            return 0;
        }
        return this.thumbnails.size();
    }

    /**
     * Una vista previa.
     *
     * @throws IndexOutOfBoundsException si no existe, o si no hay ninguna
     */
    public BufferedImage getThumbnail(int index) {
        if (this.thumbnails == null) {
            throw new IndexOutOfBoundsException("No thumbnails available!");
        }
        return this.thumbnails.get(index);
    }

    /** Las vistas previas, sin copiar. Ver la nota de la clase. */
    public List<? extends BufferedImage> getThumbnails() {
        return this.thumbnails;
    }

    /** Las cambia; tampoco copia. */
    public void setThumbnails(List<? extends BufferedImage> thumbnails) {
        this.thumbnails = thumbnails;
    }

    /** La informacion asociada, o null. */
    public IIOMetadata getMetadata() {
        return this.metadata;
    }

    /** La cambia. */
    public void setMetadata(IIOMetadata metadata) {
        this.metadata = metadata;
    }
}
