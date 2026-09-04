import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.ExifParentTIFFTagSet;
import javax.imageio.plugins.tiff.ExifTIFFTagSet;
import javax.imageio.plugins.tiff.FaxTIFFTagSet;
import javax.imageio.plugins.tiff.GeoTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFImageReadParam;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.plugins.tiff.TIFFTagSet;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * javax.imageio.plugins.tiff: campos, directorios y el viaje de ida y vuelta a metadatos.
 *
 * <p>Lo que se mide aca es lo que cuesta caro equivocarse: que arreglo lleva cada tipo, cuando un
 * {@code getAsXxx} es un cast y cuando convierte, y que el arbol nativo vuelva a ser el mismo
 * directorio.
 */
public class TiffTest {

    /** El conjunto basico, que es donde estan casi todas las etiquetas del test. */
    private static final TIFFTagSet BASE = BaselineTIFFTagSet.getInstance();

    /** -1 si todo dio bien; si no, el indice del primer caso que fallo. */
    public static int run() {
        int n = 0;

        // --- nombres y numeros de tipo
        if (!"Rational".equals(TIFFField.getTypeName(TIFFTag.TIFF_RATIONAL))) return n; n++;
        if (!"IFDPointer".equals(TIFFField.getTypeName(TIFFTag.TIFF_IFD_POINTER))) return n; n++;
        if (TIFFField.getTypeByName("SRational") != TIFFTag.TIFF_SRATIONAL) return n; n++;
        // Distingue mayusculas.
        if (TIFFField.getTypeByName("SRATIONAL") != -1) return n; n++;
        if (TIFFField.getTypeByName("Nada") != -1) return n; n++;
        if (!throwsIAE(() -> TIFFField.getTypeName(0))) return n; n++;
        if (!throwsIAE(() -> TIFFField.getTypeName(14))) return n; n++;

        // --- que clase de arreglo pide cada tipo
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_BYTE, 2) instanceof byte[])) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_UNDEFINED, 2) instanceof byte[])) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_ASCII, 2) instanceof String[])) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_SHORT, 2) instanceof char[])) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_SSHORT, 2) instanceof short[])) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_LONG, 2) instanceof long[])) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_IFD_POINTER, 1) instanceof long[])) return n; n++;
        // Un puntero apunta a un solo directorio.
        if (!throwsIAE(() -> TIFFField.createArrayForType(TIFFTag.TIFF_IFD_POINTER, 2))) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_SLONG, 2) instanceof int[])) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_RATIONAL, 2) instanceof long[][])) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_SRATIONAL, 2) instanceof int[][])) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_FLOAT, 2) instanceof float[])) return n; n++;
        if (!(TIFFField.createArrayForType(TIFFTag.TIFF_DOUBLE, 2) instanceof double[])) return n; n++;
        if (((long[][]) TIFFField.createArrayForType(TIFFTag.TIFF_RATIONAL, 2))[1].length != 2) return n; n++;
        if (!throwsIAE(() -> TIFFField.createArrayForType(TIFFTag.TIFF_LONG, -1))) return n; n++;
        if (!throwsIAE(() -> TIFFField.createArrayForType(99, 1))) return n; n++;

        // --- el constructor de cuatro argumentos valida tipo, cantidad, clase y largo
        TIFFTag width = BASE.getTag(BaselineTIFFTagSet.TAG_IMAGE_WIDTH);
        if (width == null) return n; n++;
        if (!throwsIAE(() -> new TIFFField(width, TIFFTag.TIFF_RATIONAL, 1, new long[1][2]))) return n; n++;
        if (!throwsIAE(() -> new TIFFField(width, 99, 1, new int[1]))) return n; n++;
        if (!throwsIAE(() -> new TIFFField(width, TIFFTag.TIFF_SHORT, 1, new int[1]))) return n; n++;
        // El largo del arreglo tiene que ser exactamente la cantidad, para arriba y para abajo.
        if (!throwsIAE(() -> new TIFFField(width, TIFFTag.TIFF_SHORT, 3, new char[2]))) return n; n++;
        if (!throwsIAE(() -> new TIFFField(width, TIFFTag.TIFF_SHORT, 1, new char[5]))) return n; n++;
        if (!throwsIAE(() -> new TIFFField(width, TIFFTag.TIFF_SHORT, -1, new char[1]))) return n; n++;
        if (!throwsNPE(() -> new TIFFField(width, TIFFTag.TIFF_SHORT, 1, (Object) null))) return n; n++;
        if (!throwsNPE(() -> new TIFFField(null, TIFFTag.TIFF_SHORT, 1, new char[1]))) return n; n++;

        // --- el constructor de un valor elige el tipo antes de mirar la etiqueta
        if (new TIFFField(width, 7L).getType() != TIFFTag.TIFF_SHORT) return n; n++;
        if (new TIFFField(width, 70000L).getType() != TIFFTag.TIFF_LONG) return n; n++;
        if (new TIFFField(width, 7L).getCount() != 1) return n; n++;
        TIFFTag onlyLong = new TIFFTag("SoloLargo", 60000, 1 << TIFFTag.TIFF_LONG);
        if (!throwsIAE(() -> new TIFFField(onlyLong, 7L))) return n; n++;
        if (new TIFFField(onlyLong, 70000L).getAsLong(0) != 70000L) return n; n++;
        if (!throwsIAE(() -> new TIFFField(width, -1L))) return n; n++;
        if (!throwsIAE(() -> new TIFFField(width, 0x1FFFFFFFFL))) return n; n++;
        if (!throwsNPE(() -> new TIFFField(null, 5L))) return n; n++;

        // --- el constructor de puntero
        TIFFTag exifPointer = ExifParentTIFFTagSet.getInstance()
            .getTag(ExifParentTIFFTagSet.TAG_EXIF_IFD_POINTER);
        if (exifPointer == null || !exifPointer.isIFDPointer()) return n; n++;
        TIFFDirectory sub = new TIFFDirectory(new TIFFTagSet[] { ExifTIFFTagSet.getInstance() },
            exifPointer);
        TIFFField pointer = new TIFFField(exifPointer, TIFFTag.TIFF_LONG, 100L, sub);
        if (!pointer.hasDirectory()) return n; n++;
        if (pointer.getDirectory() != sub) return n; n++;
        if (pointer.getAsLong(0) != 100L) return n; n++;
        if (!throwsNPE(() -> new TIFFField(exifPointer, TIFFTag.TIFF_LONG, 100L, null))) return n; n++;
        if (!throwsIAE(() -> new TIFFField(width, TIFFTag.TIFF_SHORT, 100L, sub))) return n; n++;
        // Los primeros ocho bytes son la cabecera: ningun directorio empieza en cero.
        if (!throwsIAE(() -> new TIFFField(exifPointer, TIFFTag.TIFF_LONG, 0L, sub))) return n; n++;
        if (new TIFFField(width, 5L).hasDirectory()) return n; n++;
        if (new TIFFField(width, 5L).getDirectory() != null) return n; n++;

        // --- los plurales son casts
        TIFFField bytes = anon(TIFFTag.TIFF_BYTE, new byte[] { -1, 2 });
        if (!throwsCCE(() -> bytes.getAsLongs())) return n; n++;
        if (!throwsCCE(() -> bytes.getAsInts())) return n; n++;
        if (!throwsCCE(() -> bytes.getAsFloats())) return n; n++;
        if (!throwsCCE(() -> bytes.getAsDoubles())) return n; n++;
        if (bytes.getAsBytes().length != 2) return n; n++;
        TIFFField rational = anon(TIFFTag.TIFF_RATIONAL, new long[][] { { 72, 1 } });
        if (!throwsCCE(() -> rational.getAsDoubles())) return n; n++;
        if (!throwsCCE(() -> rational.getAsInts())) return n; n++;
        if (rational.getAsRationals()[0][0] != 72) return n; n++;
        if (!throwsCCE(() -> rational.getAsSRationals())) return n; n++;
        TIFFField longs = anon(TIFFTag.TIFF_LONG, new long[] { 4294967295L });
        if (!throwsCCE(() -> longs.getAsInts())) return n; n++;
        if (longs.getAsLongs()[0] != 4294967295L) return n; n++;
        if (!throwsCCE(() -> longs.getAsString(0))) return n; n++;
        if (!throwsCCE(() -> longs.getAsRational(0))) return n; n++;
        if (!throwsCCE(() -> longs.getAsSRational(0))) return n; n++;

        // --- getAsInts es el unico plural que convierte
        TIFFField chars = anon(TIFFTag.TIFF_SHORT, new char[] { 'A', 65535 });
        if (chars.getAsInts()[1] != 65535) return n; n++;
        if (!throwsCCE(() -> chars.getAsLongs())) return n; n++;
        TIFFField sshorts = anon(TIFFTag.TIFF_SSHORT, new short[] { -5 });
        if (sshorts.getAsInts()[0] != -5) return n; n++;
        TIFFField slongs = anon(TIFFTag.TIFF_SLONG, new int[] { -7 });
        if (slongs.getAsInts()[0] != -7) return n; n++;
        if (!throwsCCE(() -> slongs.getAsLongs())) return n; n++;

        // --- los de un indice si convierten, y el signo depende del tipo
        if (bytes.getAsInt(0) != 255) return n; n++;
        if (!"255".equals(bytes.getValueAsString(0))) return n; n++;
        TIFFField sbytes = anon(TIFFTag.TIFF_SBYTE, new byte[] { -1 });
        if (sbytes.getAsInt(0) != -1) return n; n++;
        if (!"-1".equals(sbytes.getValueAsString(0))) return n; n++;
        if (rational.getAsDouble(0) != 72.0) return n; n++;
        if (!"72/1".equals(rational.getValueAsString(0))) return n; n++;
        TIFFField srational = anon(TIFFTag.TIFF_SRATIONAL, new int[][] { { -1, 2 } });
        if (srational.getAsDouble(0) != -0.5) return n; n++;
        if (!"-1/2".equals(srational.getValueAsString(0))) return n; n++;
        // Se trunca, no se redondea.
        TIFFField floats = anon(TIFFTag.TIFF_FLOAT, new float[] { 2.75f });
        if (floats.getAsLong(0) != 2L) return n; n++;
        if (floats.getAsDouble(0) != 2.75) return n; n++;
        if (anon(TIFFTag.TIFF_RATIONAL, new long[][] { { 7, 2 } }).getAsLong(0) != 3L) return n; n++;
        if (longs.getAsInt(0) != -1) return n; n++;
        if (longs.getAsDouble(0) != 4294967295.0) return n; n++;
        // Un texto se parsea si le piden un numero.
        TIFFField ascii = anon(TIFFTag.TIFF_ASCII, new String[] { "42" });
        if (ascii.getAsLong(0) != 42L) return n; n++;
        if (ascii.getAsDouble(0) != 42.0) return n; n++;
        if (!"42".equals(ascii.getValueAsString(0))) return n; n++;
        TIFFField words = anon(TIFFTag.TIFF_ASCII, new String[] { "hola" });
        if (!throwsNFE(() -> words.getAsLong(0))) return n; n++;
        if (!"hola".equals(words.getAsString(0))) return n; n++;

        // --- isIntegral
        if (!bytes.isIntegral()) return n; n++;
        if (!anon(TIFFTag.TIFF_UNDEFINED, new byte[] { 1 }).isIntegral()) return n; n++;
        if (words.isIntegral()) return n; n++;
        if (rational.isIntegral()) return n; n++;
        if (floats.isIntegral()) return n; n++;
        if (anon(TIFFTag.TIFF_DOUBLE, new double[] { 1 }).isIntegral()) return n; n++;

        // --- el arbol nativo: plural mas un nodo por valor
        Node node = anon(TIFFTag.TIFF_LONG, new long[] { 5, 6 }).getAsNativeNode();
        if (!"TIFFField".equals(node.getNodeName())) return n; n++;
        if (!"60004".equals(attribute(node, "number"))) return n; n++;
        Node values = firstElement(node);
        if (!"TIFFLongs".equals(values.getNodeName())) return n; n++;
        if (countElements(values) != 2) return n; n++;
        if (!"6".equals(attribute(values.getLastChild(), "value"))) return n; n++;
        // UNDEFINED es la excepcion: un solo nodo con los bytes separados por comas y sin signo.
        Node undefined = anon(TIFFTag.TIFF_UNDEFINED, new byte[] { -1, 2 }).getAsNativeNode();
        Node container = firstElement(undefined);
        if (!"TIFFUndefined".equals(container.getNodeName())) return n; n++;
        if (!"255,2".equals(attribute(container, "value"))) return n; n++;

        // --- y vuelve
        TIFFField back = TIFFField.createFromMetadataNode(null,
            anon(TIFFTag.TIFF_LONG, new long[] { 5, 6 }).getAsNativeNode());
        if (back.getType() != TIFFTag.TIFF_LONG) return n; n++;
        if (back.getCount() != 2) return n; n++;
        if (back.getAsLong(1) != 6L) return n; n++;
        if (!TIFFTag.UNKNOWN_TAG_NAME.equals(back.getTag().getName())) return n; n++;
        if (back.getTag().getCount() != -1) return n; n++;
        TIFFField backUndefined = TIFFField.createFromMetadataNode(null, undefined);
        if (backUndefined.getType() != TIFFTag.TIFF_UNDEFINED) return n; n++;
        if (backUndefined.getCount() != 2) return n; n++;
        if (backUndefined.getAsInt(0) != 255) return n; n++;
        TIFFField backRational = TIFFField.createFromMetadataNode(null, rational.getAsNativeNode());
        if (backRational.getAsRational(0)[1] != 1L) return n; n++;
        // Con conjunto, el numero se resuelve y la etiqueta tiene nombre.
        TIFFField named = TIFFField.createFromMetadataNode(BASE,
            new TIFFField(width, 100L).getAsNativeNode());
        if (!"ImageWidth".equals(named.getTag().getName())) return n; n++;
        if (named.getAsInt(0) != 100) return n; n++;

        // --- el arbol mal formado
        if (!throwsIAE(() -> TIFFField.createFromMetadataNode(null, null))) return n; n++;
        if (!throwsIAE(() -> TIFFField.createFromMetadataNode(null,
            new IIOMetadataNode("Otro")))) return n; n++;
        if (!throwsIAE(() -> TIFFField.createFromMetadataNode(null,
            fieldNode("1", "TIFFNadas", "TIFFNada", "1")))) return n; n++;
        // Sin valores adentro no hay datos, y eso tambien es un argumento invalido.
        if (!throwsIAE(() -> TIFFField.createFromMetadataNode(null,
            fieldNode("1", "TIFFLongs", "TIFFLong")))) return n; n++;
        if (!throwsNFE(() -> TIFFField.createFromMetadataNode(null,
            fieldNode("abc", "TIFFLongs", "TIFFLong", "1")))) return n; n++;
        if (!throwsNFE(() -> TIFFField.createFromMetadataNode(null,
            fieldNode("1", "TIFFLongs", "TIFFLong", "xx")))) return n; n++;
        // El tipo del arbol tiene que servirle a la etiqueta que el conjunto resuelva.
        if (!throwsIAE(() -> TIFFField.createFromMetadataNode(BASE,
            fieldNode("256", "TIFFAsciis", "TIFFAscii", "x")))) return n; n++;

        // --- clone copia los datos
        TIFFField original = anon(TIFFTag.TIFF_RATIONAL, new long[][] { { 72, 1 } });
        TIFFField copy;
        try {
            copy = original.clone();
        } catch (CloneNotSupportedException e) {
            return n;
        }
        if (copy.getData() == original.getData()) return n; n++;
        if (copy.getAsRational(0)[0] != 72L) return n; n++;
        if (copy.getTag() != original.getTag()) return n; n++;

        // --- el directorio
        TIFFDirectory dir = new TIFFDirectory(new TIFFTagSet[] { BASE }, null);
        if (dir.getTagSets().length != 1) return n; n++;
        if (dir.getParentTag() != null) return n; n++;
        if (dir.getNumTIFFFields() != 0) return n; n++;
        if (dir.containsTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH)) return n; n++;
        if (dir.getTag(BaselineTIFFTagSet.TAG_IMAGE_WIDTH) == null) return n; n++;
        if (dir.getTag(60000) != null) return n; n++;
        if (dir.getTIFFField(999) != null) return n; n++;
        if (!throwsNPE(() -> dir.addTIFFField(null))) return n; n++;
        if (!throwsNPE(() -> new TIFFDirectory(null, null))) return n; n++;
        if (!throwsNPE(() -> dir.addTagSet(null))) return n; n++;
        if (!throwsNPE(() -> dir.removeTagSet(null))) return n; n++;

        dir.addTIFFField(new TIFFField(BASE.getTag(BaselineTIFFTagSet.TAG_IMAGE_LENGTH), 200L));
        dir.addTIFFField(new TIFFField(width, 100L));
        dir.addTIFFField(new TIFFField(onlyLong, 70000L));
        if (dir.getNumTIFFFields() != 3) return n; n++;
        if (!dir.containsTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH)) return n; n++;
        // Salen ordenados por numero de etiqueta, que es como se escriben al archivo.
        TIFFField[] all = dir.getTIFFFields();
        if (all.length != 3) return n; n++;
        if (all[0].getTagNumber() != BaselineTIFFTagSet.TAG_IMAGE_WIDTH) return n; n++;
        if (all[1].getTagNumber() != BaselineTIFFTagSet.TAG_IMAGE_LENGTH) return n; n++;
        if (all[2].getTagNumber() != 60000) return n; n++;
        // Un segundo campo con la misma etiqueta reemplaza al anterior.
        dir.addTIFFField(new TIFFField(width, 500L));
        if (dir.getNumTIFFFields() != 3) return n; n++;
        if (dir.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH).getAsInt(0) != 500) return n; n++;
        dir.removeTIFFField(BaselineTIFFTagSet.TAG_IMAGE_LENGTH);
        if (dir.getNumTIFFFields() != 2) return n; n++;
        dir.removeTIFFField(12345);
        if (dir.getNumTIFFFields() != 2) return n; n++;

        // --- los conjuntos se consultan en orden y se pueden agregar despues
        TIFFDirectory empty = new TIFFDirectory(new TIFFTagSet[] {}, null);
        if (empty.getTagSets().length != 0) return n; n++;
        if (empty.getTag(BaselineTIFFTagSet.TAG_IMAGE_WIDTH) != null) return n; n++;
        empty.addTagSet(BASE);
        empty.addTagSet(BASE);
        if (empty.getTagSets().length != 1) return n; n++;
        if (empty.getTag(BaselineTIFFTagSet.TAG_IMAGE_WIDTH) == null) return n; n++;
        empty.addTagSet(FaxTIFFTagSet.getInstance());
        if (empty.getTag(FaxTIFFTagSet.TAG_BAD_FAX_LINES) == null) return n; n++;
        empty.removeTagSet(BASE);
        if (empty.getTagSets().length != 1) return n; n++;
        if (empty.getTag(BaselineTIFFTagSet.TAG_IMAGE_WIDTH) != null) return n; n++;
        if (empty.getTagSets() == empty.getTagSets()) return n; n++;

        TIFFDirectory withParent = new TIFFDirectory(new TIFFTagSet[] {}, exifPointer);
        if (!"ExifIFDPointer".equals(withParent.getParentTag().getName())) return n; n++;

        // --- ida y vuelta por metadatos
        IIOMetadata metadata = dir.getAsMetadata();
        if (!"javax_imageio_tiff_image_1.0".equals(metadata.getNativeMetadataFormatName())) return n; n++;
        if (!metadata.isStandardMetadataFormatSupported()) return n; n++;
        if (metadata.isReadOnly()) return n; n++;
        if (metadata.getExtraMetadataFormatNames() != null) return n; n++;
        String[] formats = metadata.getMetadataFormatNames();
        if (formats.length != 2) return n; n++;
        if (!"javax_imageio_tiff_image_1.0".equals(formats[0])) return n; n++;
        if (!"javax_imageio_1.0".equals(formats[1])) return n; n++;
        // Del formato nativo de TIFF no se publica esquema, aunque el formato este declarado.
        if (!throwsISE(() -> metadata.getMetadataFormat("javax_imageio_tiff_image_1.0"))) return n; n++;
        if (metadata.getMetadataFormat("javax_imageio_1.0") == null) return n; n++;
        if (!throwsIAE(() -> metadata.getMetadataFormat("nope"))) return n; n++;
        if (!throwsIAE(() -> metadata.getAsTree("nope"))) return n; n++;

        Node root = metadata.getAsTree("javax_imageio_tiff_image_1.0");
        if (!"javax_imageio_tiff_image_1.0".equals(root.getNodeName())) return n; n++;
        Node ifd = firstElement(root);
        if (!"TIFFIFD".equals(ifd.getNodeName())) return n; n++;
        if (!"javax.imageio.plugins.tiff.BaselineTIFFTagSet".equals(attribute(ifd, "tagSets"))) return n; n++;
        if (countElements(ifd) != 2) return n; n++;

        TIFFDirectory rebuilt;
        try {
            rebuilt = TIFFDirectory.createFromMetadata(metadata);
        } catch (Exception e) {
            return n;
        }
        if (rebuilt.getNumTIFFFields() != 2) return n; n++;
        if (rebuilt.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH).getAsInt(0) != 500) return n; n++;
        if (rebuilt.getTagSets().length != 1) return n; n++;
        // El numero vuelve a resolverse contra el conjunto que el arbol nombra.
        if (!"ImageWidth".equals(
            rebuilt.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH).getTag().getName())) return n; n++;
        if (!throwsNPE(() -> uncheckedCreate(null))) return n; n++;

        // --- los metadatos son una foto: cambiar el directorio no los toca
        IIOMetadata snapshot = dir.getAsMetadata();
        dir.removeTIFFFields();
        if (dir.getNumTIFFFields() != 0) return n; n++;
        try {
            if (TIFFDirectory.createFromMetadata(snapshot).getNumTIFFFields() != 2) return n;
        } catch (Exception e) {
            return n;
        }
        n++;

        // --- un directorio anidado se guarda como TIFFIFD adentro del padre
        TIFFDirectory exif = new TIFFDirectory(new TIFFTagSet[] { ExifTIFFTagSet.getInstance() },
            exifPointer);
        exif.addTIFFField(new TIFFField(
            ExifTIFFTagSet.getInstance().getTag(ExifTIFFTagSet.TAG_EXPOSURE_TIME),
            TIFFTag.TIFF_RATIONAL, 1, new long[][] { { 1, 60 } }));
        TIFFDirectory top = new TIFFDirectory(
            new TIFFTagSet[] { ExifParentTIFFTagSet.getInstance() }, null);
        top.addTIFFField(new TIFFField(exifPointer, TIFFTag.TIFF_LONG, 8L, exif));
        Node topIfd = firstElement(top.getAsMetadata().getAsTree("javax_imageio_tiff_image_1.0"));
        Node nested = firstElement(topIfd);
        if (!"TIFFIFD".equals(nested.getNodeName())) return n; n++;
        if (!"34665".equals(attribute(nested, "parentTagNumber"))) return n; n++;
        if (!"ExifIFDPointer".equals(attribute(nested, "parentTagName"))) return n; n++;
        TIFFDirectory backTop;
        try {
            backTop = TIFFDirectory.createFromMetadata(top.getAsMetadata());
        } catch (Exception e) {
            return n;
        }
        TIFFField backPointer = backTop.getTIFFField(ExifParentTIFFTagSet.TAG_EXIF_IFD_POINTER);
        if (backPointer == null || !backPointer.hasDirectory()) return n; n++;
        if (backPointer.getDirectory().getNumTIFFFields() != 1) return n; n++;
        if (!"ExifIFDPointer".equals(backPointer.getDirectory().getParentTag().getName())) return n; n++;
        if (backPointer.getDirectory().getTIFFField(ExifTIFFTagSet.TAG_EXPOSURE_TIME)
            .getAsDouble(0) != 1.0 / 60.0) return n; n++;

        // --- el arbol estandar se deduce de los campos
        TIFFDirectory rgb = new TIFFDirectory(new TIFFTagSet[] { BASE }, null);
        rgb.addTIFFField(shorts(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE, 8, 8, 8));
        rgb.addTIFFField(new TIFFField(BASE.getTag(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL), 3L));
        rgb.addTIFFField(new TIFFField(BASE.getTag(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION),
            (long) BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_RGB));
        rgb.addTIFFField(new TIFFField(BASE.getTag(BaselineTIFFTagSet.TAG_COMPRESSION),
            (long) BaselineTIFFTagSet.COMPRESSION_LZW));
        rgb.addTIFFField(rat(BaselineTIFFTagSet.TAG_X_RESOLUTION, 300, 1));
        rgb.addTIFFField(rat(BaselineTIFFTagSet.TAG_Y_RESOLUTION, 150, 1));
        rgb.addTIFFField(new TIFFField(BASE.getTag(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT),
            (long) BaselineTIFFTagSet.RESOLUTION_UNIT_INCH));
        rgb.addTIFFField(ascii(BaselineTIFFTagSet.TAG_SOFTWARE, "KajiJDK"));
        rgb.addTIFFField(ascii(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION, "una foto"));
        rgb.addTIFFField(ascii(BaselineTIFFTagSet.TAG_DATE_TIME, "2020:01:02 03:04:05"));
        Node std = rgb.getAsMetadata().getAsTree("javax_imageio_1.0");
        if (!"javax_imageio_1.0".equals(std.getNodeName())) return n; n++;
        if (!"RGB".equals(deep(std, "Chroma", "ColorSpaceType", "name"))) return n; n++;
        if (!"TRUE".equals(deep(std, "Chroma", "BlackIsZero", "value"))) return n; n++;
        if (!"3".equals(deep(std, "Chroma", "NumChannels", "value"))) return n; n++;
        if (!"LZW".equals(deep(std, "Compression", "CompressionTypeName", "value"))) return n; n++;
        if (!"TRUE".equals(deep(std, "Compression", "Lossless", "value"))) return n; n++;
        if (!"1".equals(deep(std, "Compression", "NumProgressiveScans", "value"))) return n; n++;
        if (!"PixelInterleaved".equals(deep(std, "Data", "PlanarConfiguration", "value"))) return n; n++;
        if (!"UnsignedIntegral".equals(deep(std, "Data", "SampleFormat", "value"))) return n; n++;
        if (!"8 8 8".equals(deep(std, "Data", "BitsPerSample", "value"))) return n; n++;
        if (!"7 7 7".equals(deep(std, "Data", "SampleMSB", "value"))) return n; n++;
        // El pixel mide lo inverso de la resolucion, asi que la proporcion es la de las resoluciones
        // dada vuelta.
        if (!"0.5".equals(deep(std, "Dimension", "PixelAspectRatio", "value"))) return n; n++;
        if (!"6.0".equals(deep(std, "Document", "FormatVersion", "value"))) return n; n++;
        if (!"2020".equals(deep(std, "Document", "ImageCreationTime", "year"))) return n; n++;
        if (!"01".equals(deep(std, "Document", "ImageCreationTime", "month"))) return n; n++;
        if (!"05".equals(deep(std, "Document", "ImageCreationTime", "second"))) return n; n++;
        if (!"none".equals(deep(std, "Transparency", "Alpha", "value"))) return n; n++;
        Node text = child(std, "Text");
        if (countElements(text) != 2) return n; n++;
        // Van en orden de numero de etiqueta: ImageDescription (270) antes que Software (305).
        if (!"ImageDescription".equals(attribute(firstElement(text), "keyword"))) return n; n++;
        if (!"KajiJDK".equals(attribute(text.getLastChild(), "value"))) return n; n++;

        // --- los valores por omision del formato estandar
        TIFFDirectory bare = new TIFFDirectory(new TIFFTagSet[] { BASE }, null);
        Node bareStd = bare.getAsMetadata().getAsTree("javax_imageio_1.0");
        if (countElements(child(bareStd, "Chroma")) != 0) return n; n++;
        if (child(bareStd, "Text") != null) return n; n++;
        // Un TIFF sin BitsPerSample es bitonal.
        if (!"1".equals(deep(bareStd, "Data", "BitsPerSample", "value"))) return n; n++;
        if (deep(bareStd, "Data", "SampleFormat", "value") != null) return n; n++;
        // Salvo comprimido con JPEG, que son siempre tres muestras de ocho bits.
        TIFFDirectory jpeg = new TIFFDirectory(new TIFFTagSet[] { BASE }, null);
        jpeg.addTIFFField(new TIFFField(BASE.getTag(BaselineTIFFTagSet.TAG_COMPRESSION),
            (long) BaselineTIFFTagSet.COMPRESSION_JPEG));
        Node jpegStd = jpeg.getAsMetadata().getAsTree("javax_imageio_1.0");
        if (!"8 8 8".equals(deep(jpegStd, "Data", "BitsPerSample", "value"))) return n; n++;
        if (!"FALSE".equals(deep(jpegStd, "Compression", "Lossless", "value"))) return n; n++;
        // Una paleta se ve en color aunque guarde una muestra por pixel.
        TIFFDirectory palette = new TIFFDirectory(new TIFFTagSet[] { BASE }, null);
        palette.addTIFFField(new TIFFField(
            BASE.getTag(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION),
            (long) BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR));
        palette.addTIFFField(shorts(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE, 4));
        palette.addTIFFField(new TIFFField(BASE.getTag(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL), 1L));
        Node paletteStd = palette.getAsMetadata().getAsTree("javax_imageio_1.0");
        if (!"3".equals(deep(paletteStd, "Chroma", "NumChannels", "value"))) return n; n++;
        if (!"Index".equals(deep(paletteStd, "Data", "SampleFormat", "value"))) return n; n++;
        if (!"4 4 4".equals(deep(paletteStd, "Data", "BitsPerSample", "value"))) return n; n++;
        // Alfa y orientacion.
        TIFFDirectory alpha = new TIFFDirectory(new TIFFTagSet[] { BASE }, null);
        alpha.addTIFFField(new TIFFField(BASE.getTag(BaselineTIFFTagSet.TAG_EXTRA_SAMPLES),
            (long) BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA));
        alpha.addTIFFField(new TIFFField(BASE.getTag(BaselineTIFFTagSet.TAG_ORIENTATION), 6L));
        Node alphaStd = alpha.getAsMetadata().getAsTree("javax_imageio_1.0");
        if (!"premultiplied".equals(deep(alphaStd, "Transparency", "Alpha", "value"))) return n; n++;
        if (!"Rotate270".equals(deep(alphaStd, "Dimension", "ImageOrientation", "value"))) return n; n++;

        // --- reset vacia el directorio de los metadatos
        IIOMetadata resettable = rgb.getAsMetadata();
        resettable.reset();
        Node afterReset = firstElement(resettable.getAsTree("javax_imageio_tiff_image_1.0"));
        if (countElements(afterReset) != 0) return n; n++;
        if (rgb.getNumTIFFFields() == 0) return n; n++;

        // --- TIFFImageReadParam
        TIFFImageReadParam param = new TIFFImageReadParam();
        if (param.getAllowedTagSets().size() != 4) return n; n++;
        if (param.getAllowedTagSets().get(0) != BASE) return n; n++;
        if (param.getAllowedTagSets().get(1) != FaxTIFFTagSet.getInstance()) return n; n++;
        if (param.getAllowedTagSets().get(2) != ExifParentTIFFTagSet.getInstance()) return n; n++;
        if (param.getAllowedTagSets().get(3) != GeoTIFFTagSet.getInstance()) return n; n++;
        if (param.getReadUnknownTags()) return n; n++;
        if (!throwsIAE(() -> param.addAllowedTagSet(null))) return n; n++;
        if (!throwsIAE(() -> param.removeAllowedTagSet(null))) return n; n++;
        param.addAllowedTagSet(ExifTIFFTagSet.getInstance());
        param.addAllowedTagSet(ExifTIFFTagSet.getInstance());
        if (param.getAllowedTagSets().size() != 5) return n; n++;
        param.removeAllowedTagSet(BASE);
        if (param.getAllowedTagSets().size() != 4) return n; n++;
        param.setReadUnknownTags(true);
        if (!param.getReadUnknownTags()) return n; n++;
        // Hereda de ImageReadParam, asi que el recorte y el submuestreo siguen estando.
        param.setSourceProgressivePasses(0, 3);
        if (param.getSourceNumProgressivePasses() != 3) return n; n++;

        return -1;
    }

    /** Lo que hacemos distinto del JDK a proposito. -1 si todo dio bien. */
    public static int runKaji() {
        int n = 0;

        // Nuestra copia de un directorio es una copia entera. La del JDK comparte el mapa de las
        // etiquetas de numero alto, y vaciar el original le vacia a la copia esos campos.
        TIFFDirectory dir = new TIFFDirectory(new TIFFTagSet[] {}, null);
        TIFFTag low = new TIFFTag("Baja", 300, 1 << TIFFTag.TIFF_SHORT);
        TIFFTag high = new TIFFTag("Alta", 60000, 1 << TIFFTag.TIFF_SHORT);
        dir.addTIFFField(new TIFFField(low, 1L));
        dir.addTIFFField(new TIFFField(high, 2L));
        TIFFDirectory copy;
        try {
            copy = dir.clone();
        } catch (CloneNotSupportedException e) {
            return n;
        }
        dir.removeTIFFFields();
        if (copy.getNumTIFFFields() != 2) return n; n++;
        if (copy.getTIFFField(60000) == null) return n; n++;
        // Y los campos de la copia son campos propios.
        if (copy.getTIFFField(300) == null) return n; n++;

        // La copia de un campo racional tiene sus propios pares. El JDK copia el arreglo de afuera y
        // comparte los de adentro, asi que tocar un par de la copia le cambia el valor al original.
        TIFFTag rationalTag = new TIFFTag("R", 60005, 1 << TIFFTag.TIFF_RATIONAL);
        TIFFField original = new TIFFField(rationalTag, TIFFTag.TIFF_RATIONAL, 1,
            new long[][] { { 72, 1 } });
        TIFFField clone;
        try {
            clone = original.clone();
        } catch (CloneNotSupportedException e) {
            return n;
        }
        if (clone.getAsRationals()[0] == original.getAsRationals()[0]) return n; n++;
        clone.getAsRational(0)[0] = 9L;
        if (original.getAsRational(0)[0] != 72L) return n; n++;

        // getAllowedTagSets devuelve una copia; el JDK devuelve la lista viva.
        TIFFImageReadParam param = new TIFFImageReadParam();
        if (param.getAllowedTagSets() == param.getAllowedTagSets()) return n; n++;
        param.getAllowedTagSets().clear();
        if (param.getAllowedTagSets().size() != 4) return n; n++;

        return -1;
    }

    /** Un campo con una etiqueta inventada que acepta solo ese tipo. */
    private static TIFFField anon(int type, Object data) {
        TIFFTag tag = new TIFFTag("T" + type, 60000 + type, 1 << type);
        return new TIFFField(tag, type, java.lang.reflect.Array.getLength(data), data);
    }

    /** Un campo de shorts en esa etiqueta del conjunto basico. */
    private static TIFFField shorts(int tagNumber, int... values) {
        char[] data = new char[values.length];
        int i = 0;
        while (i < values.length) {
            data[i] = (char) values[i];
            i = i + 1;
        }
        return new TIFFField(BASE.getTag(tagNumber), TIFFTag.TIFF_SHORT, data.length, data);
    }

    /** Un campo racional. */
    private static TIFFField rat(int tagNumber, long numerator, long denominator) {
        return new TIFFField(BASE.getTag(tagNumber), TIFFTag.TIFF_RATIONAL, 1,
            new long[][] { { numerator, denominator } });
    }

    /** Un campo de texto. */
    private static TIFFField ascii(int tagNumber, String value) {
        return new TIFFField(BASE.getTag(tagNumber), TIFFTag.TIFF_ASCII, 1,
            new String[] { value });
    }

    /** Un nodo {@code TIFFField} armado a mano. */
    private static Node fieldNode(String number, String plural, String single, String... values) {
        IIOMetadataNode field = new IIOMetadataNode("TIFFField");
        field.setAttribute("number", number);
        IIOMetadataNode container = new IIOMetadataNode(plural);
        int i = 0;
        while (i < values.length) {
            IIOMetadataNode value = new IIOMetadataNode(single);
            value.setAttribute("value", values[i]);
            container.appendChild(value);
            i = i + 1;
        }
        field.appendChild(container);
        return field;
    }

    /** {@link TIFFDirectory#createFromMetadata} sin la excepcion declarada, para probar el null. */
    private static void uncheckedCreate(IIOMetadata metadata) {
        try {
            TIFFDirectory.createFromMetadata(metadata);
        } catch (javax.imageio.metadata.IIOInvalidTreeException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /** El atributo de un nieto: {@code raiz > seccion > nodo[atributo]}. */
    private static String deep(Node root, String section, String name, String attributeName) {
        Node sectionNode = child(root, section);
        if (sectionNode == null) {
            return null;
        }
        Node node = child(sectionNode, name);
        if (node == null) {
            return null;
        }
        return attribute(node, attributeName);
    }

    /** El primer hijo elemento con ese nombre, o null. */
    private static Node child(Node parent, String name) {
        NodeList children = parent.getChildNodes();
        int i = 0;
        while (i < children.getLength()) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                return node;
            }
            i = i + 1;
        }
        return null;
    }

    /** El valor de ese atributo, o null. */
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

    /** Cuantos hijos elemento tiene. */
    private static int countElements(Node node) {
        NodeList children = node.getChildNodes();
        int total = 0;
        int i = 0;
        while (i < children.getLength()) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                total = total + 1;
            }
            i = i + 1;
        }
        return total;
    }

    /** Si eso lanza {@link IllegalArgumentException}. */
    private static boolean throwsIAE(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Si eso lanza {@link NullPointerException}. */
    private static boolean throwsNPE(Runnable action) {
        try {
            action.run();
            return false;
        } catch (NullPointerException e) {
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Si eso lanza {@link ClassCastException}. */
    private static boolean throwsCCE(Runnable action) {
        try {
            action.run();
            return false;
        } catch (ClassCastException e) {
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Si eso lanza {@link NumberFormatException}. */
    private static boolean throwsNFE(Runnable action) {
        try {
            action.run();
            return false;
        } catch (NumberFormatException e) {
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Si eso lanza {@link IllegalStateException}. */
    private static boolean throwsISE(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalStateException e) {
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("run = " + run());
        System.out.println("runKaji = " + runKaji());
    }
}
