package java.awt.datatransfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The dictionary between Java's formats and the system clipboard's names.
 *
 * <p>It is a {@link FlavorTable} because the correspondence is not one to one: a Java text may be
 * handed over as several native names and a native name may correspond to several Java classes. The
 * lists come ordered from best to worst.
 *
 * <p>{@link #encodeDataFlavor}'s encoding settles a concrete problem: the system clipboard
 * understands only strings, and a Java format is a MIME type with parameters that may carry any
 * character. Encoding it with a known prefix —`JAVA_DATAFLAVOR:`— lets a Java program recognize its
 * own formats on a shared clipboard and ignore them if they belong to another.
 *
 * <p><strong>This implementation has no native clipboard behind it.</strong> It starts with the
 * correspondences the JDK ships out of the box —those of text, image and file list, which are the
 * same on every platform— and whatever is added by hand. There is no system correspondence file to
 * read, so what is there is what can be seen: a live, modifiable map, not an invented list of names
 * from a platform that is not there.
 */
public final class SystemFlavorMap implements FlavorMap, FlavorTable {

    /** The prefix a Java MIME type is encoded as a native name with. */
    private static final String JAVA_PREFIX = "JAVA_DATAFLAVOR:";

    private static SystemFlavorMap defaultMap;

    private final Map<DataFlavor, List<String>> flavorToNative =
            new LinkedHashMap<DataFlavor, List<String>>();
    private final Map<String, List<DataFlavor>> nativeToFlavor =
            new LinkedHashMap<String, List<DataFlavor>>();

    /** With the out-of-the-box correspondences. */
    private SystemFlavorMap() {
        this.registrar(DataFlavor.stringFlavor, "UNICODE TEXT");
        this.registrar(DataFlavor.stringFlavor, "TEXT");
        this.registrar(DataFlavor.imageFlavor, "IMAGE");
        this.registrar(DataFlavor.javaFileListFlavor, "FILE_NAME");
        this.registrar(DataFlavor.allHtmlFlavor, "HTML");
    }

    /** Binds a format to a native name, in both directions. */
    private void registrar(DataFlavor flavor, String nat) {
        this.addNative(flavor, nat);
        this.addFlavor(nat, flavor);
    }

    /** Adds a native name at the end of that format's list. */
    private void addNative(DataFlavor flavor, String nat) {
        List<String> lista = this.flavorToNative.get(flavor);
        if (lista == null) {
            lista = new ArrayList<String>();
            this.flavorToNative.put(flavor, lista);
        }
        if (!lista.contains(nat)) {
            lista.add(nat);
        }
    }

    /** Adds a format at the end of that native name's list. */
    private void addFlavor(String nat, DataFlavor flavor) {
        List<DataFlavor> lista = this.nativeToFlavor.get(nat);
        if (lista == null) {
            lista = new ArrayList<DataFlavor>();
            this.nativeToFlavor.put(nat, lista);
        }
        if (!lista.contains(flavor)) {
            lista.add(flavor);
        }
    }

    /** The map the system uses; there is only one. */
    public static FlavorMap getDefaultFlavorMap() {
        synchronized (SystemFlavorMap.class) {
            if (defaultMap == null) {
                defaultMap = new SystemFlavorMap();
            }
            return defaultMap;
        }
    }

    /**
     * The native names that serve that format, from best to worst.
     *
     * <p>An unknown format does not give the empty list: it gives its own MIME type encoded. That is
     * what lets two Java programs exchange a format of their own through a clipboard that knows
     * nothing about it.
     *
     * @throws NullPointerException if the format is `null`
     */
    public synchronized List<String> getNativesForFlavor(DataFlavor flav) {
        if (flav == null) {
            throw new NullPointerException("flav");
        }
        List<String> lista = this.flavorToNative.get(flav);
        if (lista != null && !lista.isEmpty()) {
            return new ArrayList<String>(lista);
        }
        List<String> out = new ArrayList<String>();
        out.add(encodeDataFlavor(flav));
        return out;
    }

    /**
     * The formats that serve that native name, from best to worst.
     *
     * <p>An encoded name is decoded back into the format it represents, even if nobody registered
     * it.
     *
     * @throws NullPointerException if the name is `null`
     */
    public synchronized List<DataFlavor> getFlavorsForNative(String nat) {
        if (nat == null) {
            throw new NullPointerException("nat");
        }
        List<DataFlavor> lista = this.nativeToFlavor.get(nat);
        if (lista != null && !lista.isEmpty()) {
            return new ArrayList<DataFlavor>(lista);
        }
        List<DataFlavor> out = new ArrayList<DataFlavor>();
        if (isJavaMIMEType(nat)) {
            try {
                out.add(decodeDataFlavor(nat));
            } catch (ClassNotFoundException e) {
                // It is a Java format but of a class that is not here: there is nothing to offer.
            }
        }
        return out;
    }

    /**
     * The best native name of each format.
     *
     * @throws NullPointerException if the array carries a `null`
     */
    public synchronized Map<DataFlavor, String> getNativesForFlavors(DataFlavor[] flavors) {
        Map<DataFlavor, String> out = new HashMap<DataFlavor, String>();
        DataFlavor[] cuales = flavors;
        if (cuales == null) {
            cuales = this.flavorToNative.keySet().toArray(new DataFlavor[0]);
        }
        for (int i = 0; i < cuales.length; i++) {
            List<String> lista = this.getNativesForFlavor(cuales[i]);
            if (!lista.isEmpty()) {
                out.put(cuales[i], lista.get(0));
            }
        }
        return out;
    }

    /**
     * The best format of each native name.
     *
     * @throws NullPointerException if the array carries a `null`
     */
    public synchronized Map<String, DataFlavor> getFlavorsForNatives(String[] natives) {
        Map<String, DataFlavor> out = new HashMap<String, DataFlavor>();
        String[] cuales = natives;
        if (cuales == null) {
            cuales = this.nativeToFlavor.keySet().toArray(new String[0]);
        }
        for (int i = 0; i < cuales.length; i++) {
            List<DataFlavor> lista = this.getFlavorsForNative(cuales[i]);
            if (!lista.isEmpty()) {
                out.put(cuales[i], lista.get(0));
            }
        }
        return out;
    }

    /**
     * Adds a native name **at the end** of a format's list.
     *
     * <p>At the end and not at the start: what was already registered is preferred, and what is added
     * is the last resort.
     *
     * @throws NullPointerException if either of the two is missing
     */
    public synchronized void addUnencodedNativeForFlavor(DataFlavor flav, String nat) {
        if (flav == null || nat == null) {
            throw new NullPointerException("null arguments not permitted");
        }
        this.addNative(flav, nat);
    }

    /**
     * Replaces a format's list of native names.
     *
     * @throws NullPointerException if the format is missing or the array carries a `null`
     */
    public synchronized void setNativesForFlavor(DataFlavor flav, String[] natives) {
        if (flav == null || natives == null) {
            throw new NullPointerException("null arguments not permitted");
        }
        List<String> lista = new ArrayList<String>();
        for (int i = 0; i < natives.length; i++) {
            if (natives[i] == null) {
                throw new NullPointerException("null arguments not permitted");
            }
            if (!lista.contains(natives[i])) {
                lista.add(natives[i]);
            }
        }
        this.flavorToNative.put(flav, lista);
    }

    /**
     * Adds a format at the end of a native name's list.
     *
     * @throws NullPointerException if either of the two is missing
     */
    public synchronized void addFlavorForUnencodedNative(String nat, DataFlavor flav) {
        if (flav == null || nat == null) {
            throw new NullPointerException("null arguments not permitted");
        }
        this.addFlavor(nat, flav);
    }

    /**
     * Replaces a native name's list of formats.
     *
     * @throws NullPointerException if the name is missing or the array carries a `null`
     */
    public synchronized void setFlavorsForNative(String nat, DataFlavor[] flavors) {
        if (nat == null || flavors == null) {
            throw new NullPointerException("null arguments not permitted");
        }
        List<DataFlavor> lista = new ArrayList<DataFlavor>();
        for (int i = 0; i < flavors.length; i++) {
            if (flavors[i] == null) {
                throw new NullPointerException("null arguments not permitted");
            }
            if (!lista.contains(flavors[i])) {
                lista.add(flavors[i]);
            }
        }
        this.nativeToFlavor.put(nat, lista);
    }

    /**
     * Encodes a Java MIME type as a native name.
     *
     * @return the encoded name, or `null` if the MIME type is `null`
     */
    public static String encodeJavaMIMEType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        return JAVA_PREFIX + mimeType;
    }

    /**
     * Encodes a format as a native name.
     *
     * @return the encoded name, or `null` if the format or its MIME type is `null`
     */
    public static String encodeDataFlavor(DataFlavor flav) {
        if (flav == null) {
            return null;
        }
        return encodeJavaMIMEType(flav.getMimeType());
    }

    /** Whether that native name is an encoded Java MIME type. */
    public static boolean isJavaMIMEType(String str) {
        return str != null && str.startsWith(JAVA_PREFIX, 0);
    }

    /**
     * Decodes a native name into a Java MIME type.
     *
     * @return the MIME type, or `null` if the name is not encoded
     */
    public static String decodeJavaMIMEType(String nat) {
        if (!isJavaMIMEType(nat)) {
            return null;
        }
        return nat.substring(JAVA_PREFIX.length());
    }

    /**
     * Decodes a native name into a format.
     *
     * @return the format, or `null` if the name is not encoded
     * @throws ClassNotFoundException if the representation class cannot be loaded
     */
    public static DataFlavor decodeDataFlavor(String nat) throws ClassNotFoundException {
        String mimeType = decodeJavaMIMEType(nat);
        if (mimeType == null) {
            return null;
        }
        return new DataFlavor(mimeType);
    }
}
