package java.awt.image;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Un {@link Raster} en el que además se puede **escribir**.
 *
 * <p>Toda la geometría —la traducción del modelo de muestras, los recortes que comparten datos, las
 * comprobaciones de borde— ya está en la clase padre. Acá se agregan los métodos que escriben, que
 * son el espejo exacto de los que leen, más dos que no tienen espejo: {@link #setRect} y
 * {@link #setDataElements(int, int, Raster)}, que copian de otro ráster.
 *
 * <p>Que la escritura viva en una subclase no es un detalle de organización. Un método que recibe un
 * `Raster` declara que sólo va a leerlo, y un recorte de sólo lectura sobre datos escribibles es una
 * vista honesta: el tipo dice lo que el que lo tiene puede hacer, no lo que hay abajo.
 *
 * <p>La diferencia entre los dos métodos de copia está en el borde. {@link #setRect} **recorta**: lo
 * que caiga afuera se descarta en silencio, porque copiar una imagen contra una esquina es lo normal
 * y no un error. {@link #setDataElements(int, int, Raster)} **tira**, porque copia píxeles crudos y
 * ahí un desborde sería un error de cuenta, no un recorte querido.
 */
public class WritableRaster extends Raster {

    /**
     * Un ráster escribible con un buffer nuevo, del tamaño del modelo, ubicado en `origin`.
     *
     * @throws RasterFormatException si el tamaño resultante es vacío
     */
    protected WritableRaster(SampleModel sampleModel, Point origin) {
        this(sampleModel, sampleModel.createDataBuffer(),
                new Rectangle(origin.x, origin.y, sampleModel.getWidth(), sampleModel.getHeight()),
                origin, null);
    }

    /**
     * Un ráster escribible sobre el buffer dado, del tamaño del modelo, ubicado en `origin`.
     *
     * @throws RasterFormatException si el tamaño resultante es vacío
     */
    protected WritableRaster(SampleModel sampleModel, DataBuffer dataBuffer, Point origin) {
        this(sampleModel, dataBuffer,
                new Rectangle(origin.x, origin.y, sampleModel.getWidth(), sampleModel.getHeight()),
                origin, null);
    }

    /**
     * El constructor general: región, traducción y padre dados por separado.
     *
     * @throws NullPointerException si falta cualquiera de los cuatro primeros
     * @throws RasterFormatException si la región es vacía
     */
    protected WritableRaster(SampleModel sampleModel, DataBuffer dataBuffer, Rectangle aRegion,
            Point sampleModelTranslate, WritableRaster parent) {
        super(sampleModel, dataBuffer, aRegion, sampleModelTranslate, parent);
    }

    /**
     * El ráster escribible del que éste es un recorte, o `null`.
     *
     * <p>Se hereda el campo `parent` de {@link Raster}; acá se devuelve con su tipo verdadero,
     * porque un hijo escribible sólo se arma sobre un padre escribible.
     */
    public WritableRaster getWritableParent() {
        return (WritableRaster) this.parent;
    }

    /**
     * El mismo ráster mudado a otras coordenadas, **sobre los mismos datos**.
     *
     * @throws RasterFormatException si las coordenadas nuevas se pasan de `int`
     */
    public WritableRaster createWritableTranslatedChild(int childMinX, int childMinY) {
        return this.createWritableChild(this.minX, this.minY, this.width, this.height, childMinX,
                childMinY, null);
    }

    /**
     * Un recorte escribible sobre los **mismos datos**, opcionalmente con menos bandas.
     *
     * <p>Escribir en el hijo cambia al padre: no hay copia de por medio.
     *
     * @throws RasterFormatException si el rectángulo pedido no cae dentro de éste
     */
    public WritableRaster createWritableChild(int parentX, int parentY, int w, int h,
            int childMinX, int childMinY, int[] bandList) {
        if (parentX < this.minX) {
            throw new RasterFormatException("parentX lies outside raster");
        }
        if (parentY < this.minY) {
            throw new RasterFormatException("parentY lies outside raster");
        }
        if (parentX + w < parentX || parentX + w > this.minX + this.width) {
            throw new RasterFormatException("(parentX + width) is outside raster");
        }
        if (parentY + h < parentY || parentY + h > this.minY + this.height) {
            throw new RasterFormatException("(parentY + height) is outside raster");
        }
        SampleModel subSampleModel;
        if (bandList == null) {
            subSampleModel = this.sampleModel;
        } else {
            subSampleModel = this.sampleModel.createSubsetSampleModel(bandList);
        }
        int deltaX = childMinX - parentX;
        int deltaY = childMinY - parentY;
        return new WritableRaster(subSampleModel, this.dataBuffer,
                new Rectangle(childMinX, childMinY, w, h),
                new Point(this.sampleModelTranslateX + deltaX,
                        this.sampleModelTranslateY + deltaY),
                this);
    }

    /**
     * Comprueba que un punto caiga adentro.
     *
     * @throws ArrayIndexOutOfBoundsException si no cae
     */
    private void checkPoint(int x, int y) {
        if (x < this.minX || y < this.minY || x >= this.minX + this.width
                || y >= this.minY + this.height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
    }

    /**
     * Comprueba que un rectángulo caiga adentro.
     *
     * @throws ArrayIndexOutOfBoundsException si no cae
     */
    private void checkRect(int x, int y, int w, int h) {
        if (x < this.minX || y < this.minY || x + w > this.minX + this.width
                || y + h > this.minY + this.height || x + w < x || y + h < y) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
    }

    /**
     * Escribe un píxel crudo, sin desempaquetar.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public void setDataElements(int x, int y, Object inData) {
        this.checkPoint(x, y);
        this.sampleModel.setDataElements(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, inData, this.dataBuffer);
    }

    /**
     * Copia otro ráster acá, en crudo.
     *
     * <p>`(x, y)` es a dónde va el ángulo del ráster de origen. Si algo no entra, **tira**: son
     * píxeles crudos y un desborde sería un error de cuenta.
     *
     * @throws ArrayIndexOutOfBoundsException si el origen no entra entero
     */
    public void setDataElements(int x, int y, Raster inRaster) {
        int dstOffX = x + inRaster.getMinX();
        int dstOffY = y + inRaster.getMinY();
        int w = inRaster.getWidth();
        int h = inRaster.getHeight();
        if (dstOffX < this.minX || dstOffY < this.minY
                || dstOffX + w > this.minX + this.width
                || dstOffY + h > this.minY + this.height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
        int srcOffX = inRaster.getMinX();
        int srcOffY = inRaster.getMinY();
        // Fila por fila y no todo junto: el rectangulo entero podria no entrar en memoria, y de a
        // una fila el arreglo temporal se reusa.
        Object tdata = null;
        for (int startY = 0; startY < h; startY++) {
            tdata = inRaster.getDataElements(srcOffX, srcOffY + startY, w, 1, tdata);
            this.setDataElements(dstOffX, dstOffY + startY, w, 1, tdata);
        }
    }

    /**
     * Escribe los píxeles crudos de un rectángulo.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public void setDataElements(int x, int y, int w, int h, Object inData) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setDataElements(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, inData, this.dataBuffer);
    }

    /**
     * Copia otro ráster acá, píxel por píxel, con el ángulo en el mismo lugar.
     *
     * <p>Lo que caiga afuera se descarta.
     */
    public void setRect(Raster srcRaster) {
        this.setRect(0, 0, srcRaster);
    }

    /**
     * Copia otro ráster acá, corrido `(dx, dy)`.
     *
     * <p><strong>Recorta</strong>: lo que caiga afuera se descarta en silencio. Es lo contrario de
     * {@link #setDataElements(int, int, Raster)}, y a propósito — pegar una imagen contra una
     * esquina es lo normal y no un error.
     *
     * <p>La copia pasa por valores de banda, no por elementos crudos, así que origen y destino
     * pueden tener disposiciones distintas mientras coincidan las bandas.
     */
    public void setRect(int dx, int dy, Raster srcRaster) {
        int w = srcRaster.getWidth();
        int h = srcRaster.getHeight();
        int srcOffX = srcRaster.getMinX();
        int srcOffY = srcRaster.getMinY();
        int dstOffX = dx + srcOffX;
        int dstOffY = dy + srcOffY;
        if (dstOffX < this.minX) {
            int skipX = this.minX - dstOffX;
            w = w - skipX;
            srcOffX = srcOffX + skipX;
            dstOffX = this.minX;
        }
        if (dstOffY < this.minY) {
            int skipY = this.minY - dstOffY;
            h = h - skipY;
            srcOffY = srcOffY + skipY;
            dstOffY = this.minY;
        }
        if (dstOffX + w > this.minX + this.width) {
            w = this.minX + this.width - dstOffX;
        }
        if (dstOffY + h > this.minY + this.height) {
            h = this.minY + this.height - dstOffY;
        }
        if (w <= 0 || h <= 0) {
            return;
        }
        // Los tipos enteros pasan por int y los de coma por su propio tipo: convertir un float a int
        // para copiarlo perderia la parte decimal en una operacion que no deberia perder nada.
        int tipo = srcRaster.getSampleModel().getDataType();
        if (tipo == DataBuffer.TYPE_FLOAT) {
            float[] fData = null;
            for (int startY = 0; startY < h; startY++) {
                fData = srcRaster.getPixels(srcOffX, srcOffY + startY, w, 1, fData);
                this.setPixels(dstOffX, dstOffY + startY, w, 1, fData);
            }
            return;
        }
        if (tipo == DataBuffer.TYPE_DOUBLE) {
            double[] dData = null;
            for (int startY = 0; startY < h; startY++) {
                dData = srcRaster.getPixels(srcOffX, srcOffY + startY, w, 1, dData);
                this.setPixels(dstOffX, dstOffY + startY, w, 1, dData);
            }
            return;
        }
        int[] iData = null;
        for (int startY = 0; startY < h; startY++) {
            iData = srcRaster.getPixels(srcOffX, srcOffY + startY, w, 1, iData);
            this.setPixels(dstOffX, dstOffY + startY, w, 1, iData);
        }
    }

    /**
     * Escribe todas las bandas de un píxel.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public void setPixel(int x, int y, int[] iArray) {
        this.checkPoint(x, y);
        this.sampleModel.setPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, iArray, this.dataBuffer);
    }

    /**
     * Como el anterior, desde `float`.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public void setPixel(int x, int y, float[] fArray) {
        this.checkPoint(x, y);
        this.sampleModel.setPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, fArray, this.dataBuffer);
    }

    /**
     * Como el anterior, desde `double`.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public void setPixel(int x, int y, double[] dArray) {
        this.checkPoint(x, y);
        this.sampleModel.setPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, dArray, this.dataBuffer);
    }

    /**
     * Escribe los píxeles de un rectángulo.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public void setPixels(int x, int y, int w, int h, int[] iArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, iArray, this.dataBuffer);
    }

    /**
     * Como el anterior, desde `float`.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public void setPixels(int x, int y, int w, int h, float[] fArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, fArray, this.dataBuffer);
    }

    /**
     * Como el anterior, desde `double`.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public void setPixels(int x, int y, int w, int h, double[] dArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, dArray, this.dataBuffer);
    }

    /**
     * Escribe una banda de un píxel.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public void setSample(int x, int y, int b, int s) {
        this.checkPoint(x, y);
        this.sampleModel.setSample(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, s, this.dataBuffer);
    }

    /**
     * Como el anterior, desde `float`.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public void setSample(int x, int y, int b, float s) {
        this.checkPoint(x, y);
        this.sampleModel.setSample(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, s, this.dataBuffer);
    }

    /**
     * Como el anterior, desde `double`.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public void setSample(int x, int y, int b, double s) {
        this.checkPoint(x, y);
        this.sampleModel.setSample(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, s, this.dataBuffer);
    }

    /**
     * Escribe una banda en un rectángulo.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public void setSamples(int x, int y, int w, int h, int b, int[] iArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, iArray, this.dataBuffer);
    }

    /**
     * Como el anterior, desde `float`.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public void setSamples(int x, int y, int w, int h, int b, float[] fArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, fArray, this.dataBuffer);
    }

    /**
     * Como el anterior, desde `double`.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public void setSamples(int x, int y, int w, int h, int b, double[] dArray) {
        this.checkRect(x, y, w, h);
        this.sampleModel.setSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, dArray, this.dataBuffer);
    }
}
