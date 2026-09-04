package javax.imageio.plugins.tiff;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Los metadatos que devuelve {@link TIFFDirectory#getAsMetadata}.
 *
 * <p>No es publica a proposito: el JDK tampoco expone la suya, y el unico modo de conseguir una es por
 * ese metodo. Lo que aporta es traducir en las dos direcciones entre un {@link TIFFDirectory} y los
 * dos formatos de arbol.
 *
 * <h2>El arbol nativo es el directorio</h2>
 *
 * <p>{@code javax_imageio_tiff_image_1.0} es una transcripcion directa: la raiz tiene un
 * {@code TIFFIFD} y ahi cuelgan los campos. No se pierde nada.
 *
 * <h2>El arbol estandar es una interpretacion</h2>
 *
 * <p>{@code javax_imageio_1.0} no tiene campos de TIFF sino conceptos --cuantos canales, si hay alfa,
 * que tan grande es un pixel-- y hay que <b>deducirlos</b>. Las reglas estan en cada
 * {@code getStandardXxxNode} y siguen a las del JDK, incluidas las que sorprenden: una imagen sin
 * {@code BitsPerSample} declara un bit por muestra, salvo que este comprimida con JPEG, donde declara
 * tres muestras de ocho.
 *
 * <p>La vuelta --{@link #mergeTree} con el formato estandar-- solo aplica los nodos que se pueden
 * traducir sin inventar; los demas se ignoran, porque el formato estandar dice cosas que un TIFF no
 * tiene donde guardar.
 */
final class TIFFMetadata extends IIOMetadata {

    /** Los milimetros de una pulgada, para pasar resolucion a tamano de pixel. */
    private static final float MM_PER_INCH = 25.4f;

    /** Los milimetros de un centimetro. */
    private static final float MM_PER_CM = 10.0f;

    /** Las etiquetas de texto que van al nodo {@code Text}, en el orden en que se escriben. */
    private static final int[] TEXT_TAGS = {
        BaselineTIFFTagSet.TAG_DOCUMENT_NAME,
        BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION,
        BaselineTIFFTagSet.TAG_MAKE,
        BaselineTIFFTagSet.TAG_MODEL,
        BaselineTIFFTagSet.TAG_PAGE_NAME,
        BaselineTIFFTagSet.TAG_SOFTWARE,
        BaselineTIFFTagSet.TAG_ARTIST,
        BaselineTIFFTagSet.TAG_HOST_COMPUTER,
        BaselineTIFFTagSet.TAG_INK_NAMES,
        BaselineTIFFTagSet.TAG_COPYRIGHT,
    };

    /** El directorio del que salen todas las respuestas. */
    private TIFFDirectory dir;

    /** Con ese directorio adentro. */
    TIFFMetadata(TIFFDirectory dir) {
        super(true, TIFFDirectory.NATIVE_FORMAT, null, null, null);
        this.dir = dir;
    }

    /** El directorio, sin copiar. */
    TIFFDirectory getDirectory() {
        return this.dir;
    }

    /** Se pueden modificar. */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * El esquema del formato.
     *
     * <p>Del estandar hay; del nativo de TIFF no se publica ninguno, igual que en el JDK, y pedirlo es
     * un {@link IllegalStateException}: el formato existe --{@link #getMetadataFormatNames} lo
     * lista-- pero no hay objeto que lo describa.
     */
    @Override
    public IIOMetadataFormat getMetadataFormat(String formatName) {
        if (TIFFDirectory.NATIVE_FORMAT.equals(formatName)) {
            throw new IllegalStateException("Can't obtain format");
        }
        return super.getMetadataFormat(formatName);
    }

    /**
     * El arbol en ese formato.
     *
     * @throws IllegalArgumentException si el formato no es ninguno de los dos
     */
    @Override
    public Node getAsTree(String formatName) {
        if (TIFFDirectory.NATIVE_FORMAT.equals(formatName)) {
            IIOMetadataNode root = new IIOMetadataNode(TIFFDirectory.NATIVE_FORMAT);
            root.appendChild(this.dir.getAsIFDNode());
            return root;
        }
        if (IIOMetadataFormatImpl.standardMetadataFormatName.equals(formatName)) {
            return getStandardTree();
        }
        throw new IllegalArgumentException("Not a recognized format!");
    }

    /**
     * Combina ese arbol con lo que ya hay.
     *
     * @throws IllegalArgumentException si el formato no es ninguno de los dos
     * @throws IIOInvalidTreeException si el arbol no cumple el formato
     */
    @Override
    public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException {
        if (root == null) {
            throw new IllegalArgumentException("root == null!");
        }
        if (TIFFDirectory.NATIVE_FORMAT.equals(formatName)) {
            if (!TIFFDirectory.NATIVE_FORMAT.equals(root.getNodeName())) {
                throw new IIOInvalidTreeException("Root must be " + TIFFDirectory.NATIVE_FORMAT, root);
            }
            Node ifd = firstElement(root);
            if (ifd == null || !"TIFFIFD".equals(ifd.getNodeName())) {
                throw new IIOInvalidTreeException("Root must have a TIFFIFD child", root);
            }
            TIFFDirectory merged = TIFFDirectory.fromIFDNode(ifd, this.dir.getParentTag());
            TIFFTagSet[] sets = merged.getTagSets();
            int i = 0;
            while (i < sets.length) {
                this.dir.addTagSet(sets[i]);
                i = i + 1;
            }
            TIFFField[] fields = merged.getTIFFFields();
            i = 0;
            while (i < fields.length) {
                this.dir.addTIFFField(fields[i]);
                i = i + 1;
            }
            return;
        }
        if (IIOMetadataFormatImpl.standardMetadataFormatName.equals(formatName)) {
            mergeStandardTree(root);
            return;
        }
        throw new IllegalArgumentException("Not a recognized format!");
    }

    /** Deja el directorio sin campos. Los conjuntos y la etiqueta padre quedan. */
    @Override
    public void reset() {
        this.dir.removeTIFFFields();
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");
        int photometric = intValue(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION, -1);
        if (photometric != -1) {
            String space = colorSpaceType(photometric);
            if (space != null) {
                IIOMetadataNode type = new IIOMetadataNode("ColorSpaceType");
                type.setAttribute("name", space);
                chroma.appendChild(type);
            }
            IIOMetadataNode black = new IIOMetadataNode("BlackIsZero");
            black.setAttribute("value",
                photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO
                    ? "FALSE" : "TRUE");
            chroma.appendChild(black);
        }
        int channels = -1;
        if (photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR) {
            // Una paleta se guarda con una muestra por pixel pero se ve en color: los canales que
            // declara el formato estandar son los del color, no los del archivo.
            channels = 3;
        } else {
            int samples = intValue(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL, -1);
            if (samples != -1) {
                channels = samples;
            } else {
                TIFFField bits = this.dir.getTIFFField(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE);
                if (bits != null) {
                    channels = bits.getCount();
                }
            }
        }
        if (channels != -1) {
            IIOMetadataNode number = new IIOMetadataNode("NumChannels");
            number.setAttribute("value", Integer.toString(channels));
            chroma.appendChild(number);
        }
        return chroma;
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression = new IIOMetadataNode("Compression");
        int scheme = intValue(BaselineTIFFTagSet.TAG_COMPRESSION, -1);
        String name = compressionName(scheme);
        if (name != null) {
            IIOMetadataNode typeName = new IIOMetadataNode("CompressionTypeName");
            typeName.setAttribute("value", name);
            compression.appendChild(typeName);
            IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
            lossless.setAttribute("value", isLossy(scheme) ? "FALSE" : "TRUE");
            compression.appendChild(lossless);
        }
        IIOMetadataNode scans = new IIOMetadataNode("NumProgressiveScans");
        scans.setAttribute("value", "1");
        compression.appendChild(scans);
        return compression;
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode data = new IIOMetadataNode("Data");
        IIOMetadataNode planar = new IIOMetadataNode("PlanarConfiguration");
        planar.setAttribute("value",
            intValue(BaselineTIFFTagSet.TAG_PLANAR_CONFIGURATION, 1)
                == BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR
                ? "PlaneInterleaved" : "PixelInterleaved");
        data.appendChild(planar);
        int photometric = intValue(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION, -1);
        if (photometric != -1) {
            String format;
            if (photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR) {
                format = "Index";
            } else {
                format = sampleFormat(intValue(BaselineTIFFTagSet.TAG_SAMPLE_FORMAT,
                    BaselineTIFFTagSet.SAMPLE_FORMAT_UNSIGNED_INTEGER));
            }
            if (format != null) {
                IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
                sampleFormat.setAttribute("value", format);
                data.appendChild(sampleFormat);
            }
        }
        int[] bits = bitsPerSample();
        StringBuilder bitsText = new StringBuilder();
        StringBuilder msbText = new StringBuilder();
        int i = 0;
        while (i < bits.length) {
            if (i > 0) {
                bitsText.append(' ');
                msbText.append(' ');
            }
            bitsText.append(bits[i]);
            msbText.append(bits[i] - 1);
            i = i + 1;
        }
        IIOMetadataNode bitsNode = new IIOMetadataNode("BitsPerSample");
        bitsNode.setAttribute("value", bitsText.toString());
        data.appendChild(bitsNode);
        IIOMetadataNode msbNode = new IIOMetadataNode("SampleMSB");
        msbNode.setAttribute("value", msbText.toString());
        data.appendChild(msbNode);
        return data;
    }

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");
        TIFFField xres = this.dir.getTIFFField(BaselineTIFFTagSet.TAG_X_RESOLUTION);
        TIFFField yres = this.dir.getTIFFField(BaselineTIFFTagSet.TAG_Y_RESOLUTION);
        int unit = intValue(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT,
            BaselineTIFFTagSet.RESOLUTION_UNIT_INCH);
        if (xres != null && yres != null) {
            // El alto y el ancho de un pixel salen de la resolucion invertida, asi que la proporcion
            // es la de las resoluciones al reves.
            IIOMetadataNode aspect = new IIOMetadataNode("PixelAspectRatio");
            aspect.setAttribute("value",
                Float.toString((float) (yres.getAsDouble(0) / xres.getAsDouble(0))));
            dimension.appendChild(aspect);
        }
        if (unit != BaselineTIFFTagSet.RESOLUTION_UNIT_NONE) {
            float millimetres = unit == BaselineTIFFTagSet.RESOLUTION_UNIT_INCH
                ? MM_PER_INCH : MM_PER_CM;
            if (xres != null) {
                IIOMetadataNode horizontal = new IIOMetadataNode("HorizontalPixelSize");
                horizontal.setAttribute("value",
                    Float.toString((float) (millimetres / xres.getAsDouble(0))));
                dimension.appendChild(horizontal);
            }
            if (yres != null) {
                IIOMetadataNode vertical = new IIOMetadataNode("VerticalPixelSize");
                vertical.setAttribute("value",
                    Float.toString((float) (millimetres / yres.getAsDouble(0))));
                dimension.appendChild(vertical);
            }
        }
        String orientation = orientationName(intValue(BaselineTIFFTagSet.TAG_ORIENTATION, -1));
        if (orientation != null) {
            IIOMetadataNode node = new IIOMetadataNode("ImageOrientation");
            node.setAttribute("value", orientation);
            dimension.appendChild(node);
        }
        return dimension;
    }

    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document = new IIOMetadataNode("Document");
        IIOMetadataNode version = new IIOMetadataNode("FormatVersion");
        version.setAttribute("value", "6.0");
        document.appendChild(version);
        TIFFField dateTime = this.dir.getTIFFField(BaselineTIFFTagSet.TAG_DATE_TIME);
        if (dateTime != null && dateTime.getCount() > 0
            && dateTime.getType() == TIFFTag.TIFF_ASCII) {
            IIOMetadataNode created = creationTime(dateTime.getAsString(0));
            if (created != null) {
                document.appendChild(created);
            }
        }
        return document;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        IIOMetadataNode text = new IIOMetadataNode("Text");
        int i = 0;
        while (i < TEXT_TAGS.length) {
            TIFFField field = this.dir.getTIFFField(TEXT_TAGS[i]);
            if (field != null && field.getType() == TIFFTag.TIFF_ASCII && field.getCount() > 0) {
                IIOMetadataNode entry = new IIOMetadataNode("TextEntry");
                entry.setAttribute("keyword", field.getTag().getName());
                entry.setAttribute("value", field.getAsString(0));
                text.appendChild(entry);
            }
            i = i + 1;
        }
        if (text.getChildNodes().getLength() == 0) {
            // Sin textos el nodo no va: el formato estandar pide al menos una entrada adentro.
            return null;
        }
        return text;
    }

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        IIOMetadataNode transparency = new IIOMetadataNode("Transparency");
        IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
        int extra = intValue(BaselineTIFFTagSet.TAG_EXTRA_SAMPLES, -1);
        String value;
        if (extra == BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
            value = "premultiplied";
        } else if (extra == BaselineTIFFTagSet.EXTRA_SAMPLES_UNASSOCIATED_ALPHA) {
            value = "nonpremultiplied";
        } else {
            value = "none";
        }
        alpha.setAttribute("value", value);
        transparency.appendChild(alpha);
        return transparency;
    }

    /**
     * Aplica los nodos del arbol estandar que se pueden traducir a campos de TIFF.
     *
     * <p>Lo que no tiene donde guardarse en un TIFF se ignora en silencio; es la unica salida honesta,
     * porque inventar una etiqueta para un concepto que el formato no tiene seria escribir un archivo
     * que dice algo que nadie mas va a poder leer igual.
     */
    private void mergeStandardTree(Node root) throws IIOInvalidTreeException {
        if (!IIOMetadataFormatImpl.standardMetadataFormatName.equals(root.getNodeName())) {
            throw new IIOInvalidTreeException("Root must be "
                + IIOMetadataFormatImpl.standardMetadataFormatName, root);
        }
        NodeList sections = root.getChildNodes();
        int i = 0;
        while (i < sections.getLength()) {
            Node section = sections.item(i);
            if (section.getNodeType() == Node.ELEMENT_NODE) {
                String name = section.getNodeName();
                if ("Chroma".equals(name)) {
                    mergeChroma(section);
                } else if ("Compression".equals(name)) {
                    mergeCompression(section);
                } else if ("Data".equals(name)) {
                    mergeData(section);
                } else if ("Dimension".equals(name)) {
                    mergeDimension(section);
                } else if ("Text".equals(name)) {
                    mergeText(section);
                } else if ("Transparency".equals(name)) {
                    mergeTransparency(section);
                }
            }
            i = i + 1;
        }
    }

    /** {@code NumChannels} vuelve a ser {@code SamplesPerPixel}. */
    private void mergeChroma(Node chroma) {
        String channels = childAttribute(chroma, "NumChannels", "value");
        if (channels != null) {
            int value = parseIntOr(channels, -1);
            if (value > 0) {
                setShort(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL, value);
            }
        }
    }

    /** {@code CompressionTypeName} vuelve a ser el numero de esquema. */
    private void mergeCompression(Node compression) {
        String name = childAttribute(compression, "CompressionTypeName", "value");
        if (name != null) {
            int scheme = compressionScheme(name);
            if (scheme != -1) {
                setShort(BaselineTIFFTagSet.TAG_COMPRESSION, scheme);
            }
        }
    }

    /** {@code BitsPerSample} y {@code PlanarConfiguration} vuelven a sus etiquetas. */
    private void mergeData(Node data) {
        String planar = childAttribute(data, "PlanarConfiguration", "value");
        if ("PlaneInterleaved".equals(planar)) {
            setShort(BaselineTIFFTagSet.TAG_PLANAR_CONFIGURATION,
                BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR);
        } else if ("PixelInterleaved".equals(planar)) {
            setShort(BaselineTIFFTagSet.TAG_PLANAR_CONFIGURATION,
                BaselineTIFFTagSet.PLANAR_CONFIGURATION_CHUNKY);
        }
        String bits = childAttribute(data, "BitsPerSample", "value");
        if (bits != null) {
            String[] pieces = bits.trim().split("\\s+");
            char[] values = new char[pieces.length];
            int i = 0;
            while (i < pieces.length) {
                int value = parseIntOr(pieces[i], -1);
                if (value < 0) {
                    return;
                }
                values[i] = (char) value;
                i = i + 1;
            }
            TIFFTag tag = this.dir.getTag(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE);
            if (tag != null) {
                this.dir.addTIFFField(
                    new TIFFField(tag, TIFFTag.TIFF_SHORT, values.length, values));
            }
        }
    }

    /** El tamano de pixel vuelve a ser resolucion; la orientacion, su numero. */
    private void mergeDimension(Node dimension) {
        String horizontal = childAttribute(dimension, "HorizontalPixelSize", "value");
        String vertical = childAttribute(dimension, "VerticalPixelSize", "value");
        if (horizontal != null || vertical != null) {
            setShort(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT,
                BaselineTIFFTagSet.RESOLUTION_UNIT_CENTIMETER);
            if (horizontal != null) {
                setResolution(BaselineTIFFTagSet.TAG_X_RESOLUTION, horizontal);
            }
            if (vertical != null) {
                setResolution(BaselineTIFFTagSet.TAG_Y_RESOLUTION, vertical);
            }
        }
        String orientation = childAttribute(dimension, "ImageOrientation", "value");
        if (orientation != null) {
            int value = orientationNumber(orientation);
            if (value != -1) {
                setShort(BaselineTIFFTagSet.TAG_ORIENTATION, value);
            }
        }
    }

    /** Cada {@code TextEntry} vuelve a la etiqueta de texto que se llama igual. */
    private void mergeText(Node text) {
        NodeList entries = text.getChildNodes();
        int i = 0;
        while (i < entries.getLength()) {
            Node entry = entries.item(i);
            if (entry.getNodeType() == Node.ELEMENT_NODE && "TextEntry".equals(entry.getNodeName())) {
                String keyword = attribute(entry, "keyword");
                String value = attribute(entry, "value");
                if (keyword != null && value != null) {
                    int number = textTagFor(keyword);
                    if (number != -1) {
                        TIFFTag tag = this.dir.getTag(number);
                        if (tag != null) {
                            this.dir.addTIFFField(new TIFFField(tag, TIFFTag.TIFF_ASCII, 1,
                                new String[] { value }));
                        }
                    }
                }
            }
            i = i + 1;
        }
    }

    /** {@code Alpha} vuelve a ser {@code ExtraSamples}. */
    private void mergeTransparency(Node transparency) {
        String alpha = childAttribute(transparency, "Alpha", "value");
        if ("premultiplied".equals(alpha)) {
            setShort(BaselineTIFFTagSet.TAG_EXTRA_SAMPLES,
                BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA);
        } else if ("nonpremultiplied".equals(alpha)) {
            setShort(BaselineTIFFTagSet.TAG_EXTRA_SAMPLES,
                BaselineTIFFTagSet.EXTRA_SAMPLES_UNASSOCIATED_ALPHA);
        }
    }

    /** Guarda un valor corto en esa etiqueta, si el directorio la conoce. */
    private void setShort(int tagNumber, int value) {
        TIFFTag tag = this.dir.getTag(tagNumber);
        if (tag != null && tag.isDataTypeOK(TIFFTag.TIFF_SHORT)) {
            this.dir.addTIFFField(
                new TIFFField(tag, TIFFTag.TIFF_SHORT, 1, new char[] { (char) value }));
        }
    }

    /** Guarda una resolucion en puntos por centimetro, a partir del tamano de pixel en milimetros. */
    private void setResolution(int tagNumber, String millimetresText) {
        float millimetres;
        try {
            millimetres = Float.parseFloat(millimetresText);
        } catch (NumberFormatException e) {
            return;
        }
        if (millimetres <= 0.0f) {
            return;
        }
        TIFFTag tag = this.dir.getTag(tagNumber);
        if (tag == null || !tag.isDataTypeOK(TIFFTag.TIFF_RATIONAL)) {
            return;
        }
        long perCentimetre = Math.round(MM_PER_CM / millimetres);
        this.dir.addTIFFField(new TIFFField(tag, TIFFTag.TIFF_RATIONAL, 1,
            new long[][] { { perCentimetre, 1L } }));
    }

    /** Cuantos bits tiene cada muestra, con los valores por omision del formato. */
    private int[] bitsPerSample() {
        TIFFField field = this.dir.getTIFFField(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE);
        int[] bits;
        if (field != null && field.getCount() > 0) {
            bits = new int[field.getCount()];
            int i = 0;
            while (i < bits.length) {
                bits[i] = field.getAsInt(i);
                i = i + 1;
            }
        } else {
            int compression = intValue(BaselineTIFFTagSet.TAG_COMPRESSION, -1);
            if (compression == BaselineTIFFTagSet.COMPRESSION_JPEG
                || compression == BaselineTIFFTagSet.COMPRESSION_OLD_JPEG) {
                // JPEG no lleva BitsPerSample propio: son siempre tres muestras de ocho bits.
                bits = new int[] { 8, 8, 8 };
            } else {
                // El valor por omision del formato es un bit: un TIFF sin la etiqueta es bitonal.
                bits = new int[] { 1 };
            }
        }
        if (intValue(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION, -1)
            == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR
            && bits.length == 1) {
            bits = new int[] { bits[0], bits[0], bits[0] };
        }
        return bits;
    }

    /** El nodo {@code ImageCreationTime} de un {@code DateTime} de TIFF, o null si no se entiende. */
    private static IIOMetadataNode creationTime(String dateTime) {
        // El formato es "AAAA:MM:DD hh:mm:ss", de largo fijo; los campos se copian tal cual, con los
        // ceros por delante que traigan.
        if (dateTime == null || dateTime.length() < 19) {
            return null;
        }
        IIOMetadataNode created = new IIOMetadataNode("ImageCreationTime");
        created.setAttribute("year", dateTime.substring(0, 4));
        created.setAttribute("month", dateTime.substring(5, 7));
        created.setAttribute("day", dateTime.substring(8, 10));
        created.setAttribute("hour", dateTime.substring(11, 13));
        created.setAttribute("minute", dateTime.substring(14, 16));
        created.setAttribute("second", dateTime.substring(17, 19));
        return created;
    }

    /** El espacio de color que corresponde a esa interpretacion fotometrica, o null. */
    private static String colorSpaceType(int photometric) {
        if (photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO
            || photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO
            || photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_TRANSPARENCY_MASK) {
            return "GRAY";
        }
        if (photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_RGB
            || photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR) {
            return "RGB";
        }
        if (photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_CMYK) {
            return "CMYK";
        }
        if (photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_Y_CB_CR) {
            return "YCbCr";
        }
        if (photometric == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_CIELAB) {
            return "Lab";
        }
        return null;
    }

    /** Como se llama ese esquema de compresion en el formato estandar, o null si no se sabe. */
    private static String compressionName(int scheme) {
        if (scheme == BaselineTIFFTagSet.COMPRESSION_NONE) {
            return "None";
        }
        if (scheme == BaselineTIFFTagSet.COMPRESSION_CCITT_RLE) {
            return "CCITT RLE";
        }
        if (scheme == BaselineTIFFTagSet.COMPRESSION_CCITT_T_4) {
            return "CCITT T.4";
        }
        if (scheme == BaselineTIFFTagSet.COMPRESSION_CCITT_T_6) {
            return "CCITT T.6";
        }
        if (scheme == BaselineTIFFTagSet.COMPRESSION_LZW) {
            return "LZW";
        }
        if (scheme == BaselineTIFFTagSet.COMPRESSION_OLD_JPEG) {
            return "Old JPEG";
        }
        if (scheme == BaselineTIFFTagSet.COMPRESSION_JPEG) {
            return "JPEG";
        }
        if (scheme == BaselineTIFFTagSet.COMPRESSION_ZLIB) {
            return "ZLib";
        }
        if (scheme == BaselineTIFFTagSet.COMPRESSION_PACKBITS) {
            return "PackBits";
        }
        if (scheme == BaselineTIFFTagSet.COMPRESSION_DEFLATE) {
            return "Deflate";
        }
        return null;
    }

    /** El numero del esquema que se llama asi, o -1. La inversa de {@link #compressionName}. */
    private static int compressionScheme(String name) {
        int scheme = BaselineTIFFTagSet.COMPRESSION_NONE;
        while (scheme <= BaselineTIFFTagSet.COMPRESSION_DEFLATE) {
            if (name.equals(compressionName(scheme))) {
                return scheme;
            }
            scheme = scheme + 1;
        }
        return -1;
    }

    /** Si ese esquema pierde informacion. */
    private static boolean isLossy(int scheme) {
        return scheme == BaselineTIFFTagSet.COMPRESSION_JPEG
            || scheme == BaselineTIFFTagSet.COMPRESSION_OLD_JPEG;
    }

    /** Como se llama ese formato de muestra en el formato estandar, o null. */
    private static String sampleFormat(int format) {
        if (format == BaselineTIFFTagSet.SAMPLE_FORMAT_UNSIGNED_INTEGER) {
            return "UnsignedIntegral";
        }
        if (format == BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER) {
            return "SignedIntegral";
        }
        if (format == BaselineTIFFTagSet.SAMPLE_FORMAT_FLOATING_POINT) {
            return "Real";
        }
        return null;
    }

    /** Los ocho nombres de orientacion del formato estandar, en el orden de TIFF. */
    private static final String[] ORIENTATIONS = {
        "Normal", "FlipH", "Rotate180", "FlipV", "FlipHRotate90", "Rotate270", "FlipVRotate90",
        "Rotate90",
    };

    /** Como se llama esa orientacion, o null si el numero no es uno de los ocho. */
    private static String orientationName(int orientation) {
        if (orientation < 1 || orientation > ORIENTATIONS.length) {
            return null;
        }
        return ORIENTATIONS[orientation - 1];
    }

    /** El numero de la orientacion que se llama asi, o -1. */
    private static int orientationNumber(String name) {
        int i = 0;
        while (i < ORIENTATIONS.length) {
            if (ORIENTATIONS[i].equals(name)) {
                return i + 1;
            }
            i = i + 1;
        }
        return -1;
    }

    /** El numero de la etiqueta de texto que se llama asi, o -1. */
    private int textTagFor(String keyword) {
        int i = 0;
        while (i < TEXT_TAGS.length) {
            TIFFTag tag = this.dir.getTag(TEXT_TAGS[i]);
            if (tag != null && keyword.equals(tag.getName())) {
                return TEXT_TAGS[i];
            }
            i = i + 1;
        }
        return -1;
    }

    /** El primer valor de esa etiqueta como entero, o el que se pase si no esta. */
    private int intValue(int tagNumber, int fallback) {
        TIFFField field = this.dir.getTIFFField(tagNumber);
        if (field == null || field.getCount() == 0) {
            return fallback;
        }
        try {
            return field.getAsInt(0);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** El atributo de ese hijo, o null si el hijo o el atributo no estan. */
    private static String childAttribute(Node parent, String childName, String attributeName) {
        NodeList children = parent.getChildNodes();
        int i = 0;
        while (i < children.getLength()) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && childName.equals(child.getNodeName())) {
                return attribute(child, attributeName);
            }
            i = i + 1;
        }
        return null;
    }

    /** El valor de ese atributo, o null si no esta. */
    private static String attribute(Node node, String name) {
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null) {
            return null;
        }
        Node attr = attrs.getNamedItem(name);
        if (attr == null) {
            return null;
        }
        return attr.getNodeValue();
    }

    /** El entero que dice ese texto, o el de reserva si no es un entero. */
    private static int parseIntOr(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** El primer hijo que sea un elemento, o null. */
    private static Node firstElement(Node node) {
        NodeList children = node.getChildNodes();
        int i = 0;
        while (i < children.getLength()) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return children.item(i);
            }
            i = i + 1;
        }
        return null;
    }
}
