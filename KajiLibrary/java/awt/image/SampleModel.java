package java.awt.image;

/**
 * Qué es un píxel dentro de un {@link DataBuffer}: cuántas bandas tiene y dónde está cada una.
 *
 * <p>Es la segunda de las tres capas de `java.awt.image` y la que da sentido a la primera. El
 * buffer es un arreglo de números sin estructura; este modelo dice que el número 17 es la banda
 * verde del píxel (5,2). Lo que **no** dice es qué color es ese verde — eso lo dice un
 * {@link ColorModel}.
 *
 * <p>La separación es lo que permite leer la misma memoria de dos formas sin copiarla, y es lo que
 * hace que un recorte de una imagen sea un modelo nuevo sobre el mismo buffer en vez de una copia.
 *
 * <h2>Muestras contra elementos de datos</h2>
 *
 * <p>Hay dos familias de accesores y confundirlas es el error clásico:
 *
 * <ul>
 * <li>Las **muestras** ({@code getSample}, {@code getPixel}) dan los valores de cada banda ya
 *     desempaquetados, siempre como `int`, `float` o `double`. Es lo que uno quiere para hacer
 *     cuentas.</li>
 * <li>Los **elementos de datos** ({@code getDataElements}) dan la representación cruda, en el tipo
 *     del buffer y con el empaquetado que el modelo use. Es lo que uno quiere para **copiar** de
 *     una imagen a otra del mismo tipo, porque no desempaqueta ni vuelve a empaquetar.</li>
 * </ul>
 *
 * <p>De ahí que `getDataElements` devuelva `Object`: el tipo real es `byte[]`, `short[]`, `int[]`,
 * `float[]` o `double[]` según {@link #getTransferType}, y no hay un supertipo común de arreglos de
 * primitivos.
 *
 * <h2>Qué implementa una subclase</h2>
 *
 * <p>Sólo seis miembros: {@code getSample}, {@code setSample}, las dos formas de un solo píxel de
 * {@code getDataElements}/{@code setDataElements}, y los tres `create...` más
 * {@code getSampleSize}. Todo el resto de esta clase está escrito en términos de ésos — los
 * rectángulos son bucles sobre el píxel, y las variantes `float`/`double` son conversiones. Una
 * subclase puede redefinirlos para ir más rápido, pero no tiene que hacerlo para ser correcta.
 */
public abstract class SampleModel {

    /** El ancho en píxeles. */
    protected int width;

    /** El alto en píxeles. */
    protected int height;

    /** Cuántas bandas tiene cada píxel. */
    protected int numBands;

    /** El tipo de los datos del buffer: una constante `DataBuffer.TYPE_`. */
    protected int dataType;

    /**
     * Un modelo de `w` por `h` con `numBands` bandas sobre datos de ese tipo.
     *
     * @throws IllegalArgumentException si el ancho o el alto no son positivos, si su producto se
     *     pasa de `Integer.MAX_VALUE`, o si el tipo no es uno de los seis
     */
    public SampleModel(int dataType, int w, int h, int numBands) {
        long size = (long) w * h;
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") must be > 0");
        }
        if (size >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Dimensions (width=" + w + " height=" + h
                    + ") are too large");
        }
        if (dataType < DataBuffer.TYPE_BYTE
                || (dataType > DataBuffer.TYPE_DOUBLE && dataType != DataBuffer.TYPE_UNDEFINED)) {
            throw new IllegalArgumentException("Unsupported dataType: " + dataType);
        }
        if (numBands <= 0) {
            throw new IllegalArgumentException("Number of bands must be > 0");
        }
        this.dataType = dataType;
        this.width = w;
        this.height = h;
        this.numBands = numBands;
    }

    /** El ancho. */
    public final int getWidth() {
        return this.width;
    }

    /** El alto. */
    public final int getHeight() {
        return this.height;
    }

    /** Cuántas bandas. */
    public final int getNumBands() {
        return this.numBands;
    }

    /** Cuántos elementos del buffer ocupa un píxel. Ver la nota sobre elementos de datos. */
    public abstract int getNumDataElements();

    /** El tipo de los datos del buffer. */
    public final int getDataType() {
        return this.dataType;
    }

    /**
     * El tipo de los arreglos de {@link #getDataElements}.
     *
     * <p>Por omisión es el mismo que el del buffer, y casi siempre lo es. Se separa porque un
     * modelo empaquetado puede transferir en un tipo más ancho que el que guarda -- por ejemplo,
     * guardar en `byte[]` y transferir en `int` cuando un píxel no entra en un byte.
     */
    public int getTransferType() {
        return this.dataType;
    }

    // ---- un píxel, por muestras -----------------------------------------------------------

    /**
     * Las bandas de ese píxel.
     *
     * @param iArray dónde dejarlas, o nulo para que se reserve
     * @throws ArrayIndexOutOfBoundsException si el píxel cae fuera del modelo
     */
    public int[] getPixel(int x, int y, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[this.numBands] : iArray;
        for (int i = 0; i < this.numBands; i++) {
            out[i] = this.getSample(x, y, i, data);
        }
        return out;
    }

    /** Como {@link #getPixel(int, int, int[], DataBuffer)}, en `float`. */
    public float[] getPixel(int x, int y, float[] fArray, DataBuffer data) {
        float[] out = fArray == null ? new float[this.numBands] : fArray;
        for (int i = 0; i < this.numBands; i++) {
            out[i] = this.getSampleFloat(x, y, i, data);
        }
        return out;
    }

    /** Como {@link #getPixel(int, int, int[], DataBuffer)}, en `double`. */
    public double[] getPixel(int x, int y, double[] dArray, DataBuffer data) {
        double[] out = dArray == null ? new double[this.numBands] : dArray;
        for (int i = 0; i < this.numBands; i++) {
            out[i] = this.getSampleDouble(x, y, i, data);
        }
        return out;
    }

    /** Escribe las bandas de ese píxel. */
    public void setPixel(int x, int y, int[] iArray, DataBuffer data) {
        for (int i = 0; i < this.numBands; i++) {
            this.setSample(x, y, i, iArray[i], data);
        }
    }

    /** Escribe las bandas de ese píxel, desde `float`. */
    public void setPixel(int x, int y, float[] fArray, DataBuffer data) {
        for (int i = 0; i < this.numBands; i++) {
            this.setSample(x, y, i, fArray[i], data);
        }
    }

    /** Escribe las bandas de ese píxel, desde `double`. */
    public void setPixel(int x, int y, double[] dArray, DataBuffer data) {
        for (int i = 0; i < this.numBands; i++) {
            this.setSample(x, y, i, dArray[i], data);
        }
    }

    // ---- un rectángulo, por muestras ------------------------------------------------------
    //
    // El orden es por filas y dentro de cada píxel por banda, que es el que espera cualquier
    // código que recorra el arreglo linealmente.

    /** Las bandas de todos los píxeles de ese rectángulo. */
    public int[] getPixels(int x, int y, int w, int h, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[w * h * this.numBands] : iArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    out[k] = this.getSample(i, j, b, data);
                    k = k + 1;
                }
            }
        }
        return out;
    }

    /** Como el anterior, en `float`. */
    public float[] getPixels(int x, int y, int w, int h, float[] fArray, DataBuffer data) {
        float[] out = fArray == null ? new float[w * h * this.numBands] : fArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    out[k] = this.getSampleFloat(i, j, b, data);
                    k = k + 1;
                }
            }
        }
        return out;
    }

    /** Como el anterior, en `double`. */
    public double[] getPixels(int x, int y, int w, int h, double[] dArray, DataBuffer data) {
        double[] out = dArray == null ? new double[w * h * this.numBands] : dArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    out[k] = this.getSampleDouble(i, j, b, data);
                    k = k + 1;
                }
            }
        }
        return out;
    }

    /** Escribe las bandas de todos los píxeles de ese rectángulo. */
    public void setPixels(int x, int y, int w, int h, int[] iArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    this.setSample(i, j, b, iArray[k], data);
                    k = k + 1;
                }
            }
        }
    }

    /** Como el anterior, desde `float`. */
    public void setPixels(int x, int y, int w, int h, float[] fArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    this.setSample(i, j, b, fArray[k], data);
                    k = k + 1;
                }
            }
        }
    }

    /** Como el anterior, desde `double`. */
    public void setPixels(int x, int y, int w, int h, double[] dArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    this.setSample(i, j, b, dArray[k], data);
                    k = k + 1;
                }
            }
        }
    }

    // ---- una banda ------------------------------------------------------------------------

    /**
     * El valor de una banda de un píxel.
     *
     * @throws ArrayIndexOutOfBoundsException si el píxel o la banda caen fuera
     */
    public abstract int getSample(int x, int y, int b, DataBuffer data);

    /** El valor de una banda, como `float`. */
    public float getSampleFloat(int x, int y, int b, DataBuffer data) {
        return this.getSample(x, y, b, data);
    }

    /** El valor de una banda, como `double`. */
    public double getSampleDouble(int x, int y, int b, DataBuffer data) {
        return this.getSample(x, y, b, data);
    }

    /** Escribe el valor de una banda. */
    public abstract void setSample(int x, int y, int b, int s, DataBuffer data);

    /** Escribe el valor de una banda desde un `float`. En un modelo entero se trunca. */
    public void setSample(int x, int y, int b, float s, DataBuffer data) {
        this.setSample(x, y, b, (int) s, data);
    }

    /** Escribe el valor de una banda desde un `double`. En un modelo entero se trunca. */
    public void setSample(int x, int y, int b, double s, DataBuffer data) {
        this.setSample(x, y, b, (int) s, data);
    }

    /** Los valores de una banda en todo un rectángulo. */
    public int[] getSamples(int x, int y, int w, int h, int b, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[w * h] : iArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                out[k] = this.getSample(i, j, b, data);
                k = k + 1;
            }
        }
        return out;
    }

    /** Como el anterior, en `float`. */
    public float[] getSamples(int x, int y, int w, int h, int b, float[] fArray, DataBuffer data) {
        float[] out = fArray == null ? new float[w * h] : fArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                out[k] = this.getSampleFloat(i, j, b, data);
                k = k + 1;
            }
        }
        return out;
    }

    /** Como el anterior, en `double`. */
    public double[] getSamples(int x, int y, int w, int h, int b, double[] dArray,
            DataBuffer data) {
        double[] out = dArray == null ? new double[w * h] : dArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                out[k] = this.getSampleDouble(i, j, b, data);
                k = k + 1;
            }
        }
        return out;
    }

    /** Escribe los valores de una banda en todo un rectángulo. */
    public void setSamples(int x, int y, int w, int h, int b, int[] iArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                this.setSample(i, j, b, iArray[k], data);
                k = k + 1;
            }
        }
    }

    /** Como el anterior, desde `float`. */
    public void setSamples(int x, int y, int w, int h, int b, float[] fArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                this.setSample(i, j, b, fArray[k], data);
                k = k + 1;
            }
        }
    }

    /** Como el anterior, desde `double`. */
    public void setSamples(int x, int y, int w, int h, int b, double[] dArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                this.setSample(i, j, b, dArray[k], data);
                k = k + 1;
            }
        }
    }

    // ---- elementos de datos ---------------------------------------------------------------

    /**
     * La representación cruda de un píxel. Ver la nota de la clase.
     *
     * @param obj dónde dejarla — un arreglo del tipo de {@link #getTransferType} — o nulo
     */
    public abstract Object getDataElements(int x, int y, Object obj, DataBuffer data);

    /**
     * La representación cruda de un rectángulo.
     *
     * <p>Reserva el arreglo del tipo que corresponda y delega píxel por píxel. Una subclase que
     * pueda copiar bloques enteros lo redefine; ésta es la versión correcta, no la rápida.
     *
     * @throws IllegalArgumentException si el tipo de transferencia no es uno de los seis
     */
    public Object getDataElements(int x, int y, int w, int h, Object obj, DataBuffer data) {
        int n = this.getNumDataElements();
        int total = w * h * n;
        int tipo = this.getTransferType();
        Object destino = obj;
        if (tipo == DataBuffer.TYPE_BYTE) {
            byte[] out = destino == null ? new byte[total] : (byte[]) destino;
            byte[] uno = null;
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    uno = (byte[]) this.getDataElements(i, j, uno, data);
                    System.arraycopy(uno, 0, out, k, n);
                    k = k + n;
                }
            }
            return out;
        }
        if (tipo == DataBuffer.TYPE_USHORT || tipo == DataBuffer.TYPE_SHORT) {
            short[] out = destino == null ? new short[total] : (short[]) destino;
            short[] uno = null;
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    uno = (short[]) this.getDataElements(i, j, uno, data);
                    System.arraycopy(uno, 0, out, k, n);
                    k = k + n;
                }
            }
            return out;
        }
        if (tipo == DataBuffer.TYPE_INT) {
            int[] out = destino == null ? new int[total] : (int[]) destino;
            int[] uno = null;
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    uno = (int[]) this.getDataElements(i, j, uno, data);
                    System.arraycopy(uno, 0, out, k, n);
                    k = k + n;
                }
            }
            return out;
        }
        if (tipo == DataBuffer.TYPE_FLOAT) {
            float[] out = destino == null ? new float[total] : (float[]) destino;
            float[] uno = null;
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    uno = (float[]) this.getDataElements(i, j, uno, data);
                    System.arraycopy(uno, 0, out, k, n);
                    k = k + n;
                }
            }
            return out;
        }
        if (tipo == DataBuffer.TYPE_DOUBLE) {
            double[] out = destino == null ? new double[total] : (double[]) destino;
            double[] uno = null;
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    uno = (double[]) this.getDataElements(i, j, uno, data);
                    System.arraycopy(uno, 0, out, k, n);
                    k = k + n;
                }
            }
            return out;
        }
        throw new IllegalArgumentException("Unsupported transfer type: " + tipo);
    }

    /** Escribe la representación cruda de un píxel. */
    public abstract void setDataElements(int x, int y, Object obj, DataBuffer data);

    /**
     * Escribe la representación cruda de un rectángulo.
     *
     * @throws IllegalArgumentException si el tipo de transferencia no es uno de los seis
     */
    public void setDataElements(int x, int y, int w, int h, Object obj, DataBuffer data) {
        int n = this.getNumDataElements();
        int tipo = this.getTransferType();
        if (tipo == DataBuffer.TYPE_BYTE) {
            byte[] src = (byte[]) obj;
            byte[] uno = new byte[n];
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    System.arraycopy(src, k, uno, 0, n);
                    this.setDataElements(i, j, uno, data);
                    k = k + n;
                }
            }
            return;
        }
        if (tipo == DataBuffer.TYPE_USHORT || tipo == DataBuffer.TYPE_SHORT) {
            short[] src = (short[]) obj;
            short[] uno = new short[n];
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    System.arraycopy(src, k, uno, 0, n);
                    this.setDataElements(i, j, uno, data);
                    k = k + n;
                }
            }
            return;
        }
        if (tipo == DataBuffer.TYPE_INT) {
            int[] src = (int[]) obj;
            int[] uno = new int[n];
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    System.arraycopy(src, k, uno, 0, n);
                    this.setDataElements(i, j, uno, data);
                    k = k + n;
                }
            }
            return;
        }
        if (tipo == DataBuffer.TYPE_FLOAT) {
            float[] src = (float[]) obj;
            float[] uno = new float[n];
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    System.arraycopy(src, k, uno, 0, n);
                    this.setDataElements(i, j, uno, data);
                    k = k + n;
                }
            }
            return;
        }
        if (tipo == DataBuffer.TYPE_DOUBLE) {
            double[] src = (double[]) obj;
            double[] uno = new double[n];
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    System.arraycopy(src, k, uno, 0, n);
                    this.setDataElements(i, j, uno, data);
                    k = k + n;
                }
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported transfer type: " + tipo);
    }

    // ---- fábricas -------------------------------------------------------------------------

    /** Un modelo igual a éste pero de otro tamaño. */
    public abstract SampleModel createCompatibleSampleModel(int w, int h);

    /**
     * Un modelo con sólo algunas de las bandas de éste.
     *
     * <p>Es lo que permite mirar el canal rojo de una imagen RGB **sin copiarla**: el modelo nuevo
     * apunta al mismo buffer y sólo cambia qué se considera un píxel.
     */
    public abstract SampleModel createSubsetSampleModel(int[] bands);

    /** Un buffer del tamaño y el tipo que este modelo necesita. */
    public abstract DataBuffer createDataBuffer();

    /** Cuántos bits usa cada banda. */
    public abstract int[] getSampleSize();

    /** Cuántos bits usa esa banda. */
    public abstract int getSampleSize(int band);
}
