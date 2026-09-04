package java.awt.datatransfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El diccionario entre los formatos de Java y los nombres del portapapeles del sistema.
 *
 * <p>Es un {@link FlavorTable} porque la correspondencia no es uno a uno: un texto de Java puede
 * entregarse como varios nombres nativos y un nombre nativo puede corresponder a varias clases de
 * Java. Las listas vienen ordenadas de mejor a peor.
 *
 * <p>La codificación de {@link #encodeDataFlavor} resuelve un problema concreto: el portapapeles del
 * sistema sólo entiende cadenas, y un formato de Java es un tipo MIME con parámetros que puede
 * traer cualquier carácter. Codificarlo con un prefijo conocido —`JAVA_DATAFLAVOR:`— permite que un
 * programa Java reconozca sus propios formatos en un portapapeles compartido y los ignore si son de
 * otro.
 *
 * <p><strong>Esta implementación no tiene un portapapeles nativo detrás.</strong> Arranca con las
 * correspondencias que el JDK trae de fábrica —las de texto, imagen y lista de archivos, que son las
 * mismas en todas las plataformas— y las que se le agreguen a mano. No hay un archivo de
 * correspondencias del sistema que leer, así que lo que hay es lo que se ve: un mapa vivo y
 * modificable, no una lista inventada de nombres de una plataforma que no está.
 */
public final class SystemFlavorMap implements FlavorMap, FlavorTable {

    /** El prefijo con el que se codifica un tipo MIME de Java como nombre nativo. */
    private static final String JAVA_PREFIX = "JAVA_DATAFLAVOR:";

    private static SystemFlavorMap defaultMap;

    private final Map<DataFlavor, List<String>> flavorToNative =
            new LinkedHashMap<DataFlavor, List<String>>();
    private final Map<String, List<DataFlavor>> nativeToFlavor =
            new LinkedHashMap<String, List<DataFlavor>>();

    /** Con las correspondencias de fábrica. */
    private SystemFlavorMap() {
        this.registrar(DataFlavor.stringFlavor, "UNICODE TEXT");
        this.registrar(DataFlavor.stringFlavor, "TEXT");
        this.registrar(DataFlavor.imageFlavor, "IMAGE");
        this.registrar(DataFlavor.javaFileListFlavor, "FILE_NAME");
        this.registrar(DataFlavor.allHtmlFlavor, "HTML");
    }

    /** Ata un formato con un nombre nativo, en los dos sentidos. */
    private void registrar(DataFlavor flavor, String nat) {
        this.agregarNativo(flavor, nat);
        this.agregarFormato(nat, flavor);
    }

    /** Suma un nombre nativo al final de la lista de ese formato. */
    private void agregarNativo(DataFlavor flavor, String nat) {
        List<String> lista = this.flavorToNative.get(flavor);
        if (lista == null) {
            lista = new ArrayList<String>();
            this.flavorToNative.put(flavor, lista);
        }
        if (!lista.contains(nat)) {
            lista.add(nat);
        }
    }

    /** Suma un formato al final de la lista de ese nombre nativo. */
    private void agregarFormato(String nat, DataFlavor flavor) {
        List<DataFlavor> lista = this.nativeToFlavor.get(nat);
        if (lista == null) {
            lista = new ArrayList<DataFlavor>();
            this.nativeToFlavor.put(nat, lista);
        }
        if (!lista.contains(flavor)) {
            lista.add(flavor);
        }
    }

    /** El mapa que usa el sistema; hay uno solo. */
    public static FlavorMap getDefaultFlavorMap() {
        synchronized (SystemFlavorMap.class) {
            if (defaultMap == null) {
                defaultMap = new SystemFlavorMap();
            }
            return defaultMap;
        }
    }

    /**
     * Los nombres nativos que sirven para ese formato, del mejor al peor.
     *
     * <p>Un formato desconocido no da la lista vacía: da su propio tipo MIME codificado. Es lo que
     * permite que dos programas Java intercambien un formato propio a través de un portapapeles que
     * no sabe nada de él.
     *
     * @throws NullPointerException si el formato es `null`
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
     * Los formatos que sirven para ese nombre nativo, del mejor al peor.
     *
     * <p>Un nombre codificado se decodifica de vuelta al formato que representa, aunque nadie lo
     * haya registrado.
     *
     * @throws NullPointerException si el nombre es `null`
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
                // Es un formato de Java pero de una clase que no está acá: no hay nada que ofrecer.
            }
        }
        return out;
    }

    /**
     * El mejor nombre nativo de cada formato.
     *
     * @throws NullPointerException si el arreglo trae un `null`
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
     * El mejor formato de cada nombre nativo.
     *
     * @throws NullPointerException si el arreglo trae un `null`
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
     * Agrega un nombre nativo **al final** de la lista de un formato.
     *
     * <p>Al final y no al principio: lo que ya estaba registrado se prefiere, y lo agregado es el
     * recurso de última.
     *
     * @throws NullPointerException si falta alguno de los dos
     */
    public synchronized void addUnencodedNativeForFlavor(DataFlavor flav, String nat) {
        if (flav == null || nat == null) {
            throw new NullPointerException("null arguments not permitted");
        }
        this.agregarNativo(flav, nat);
    }

    /**
     * Reemplaza la lista de nombres nativos de un formato.
     *
     * @throws NullPointerException si falta el formato o el arreglo trae un `null`
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
     * Agrega un formato al final de la lista de un nombre nativo.
     *
     * @throws NullPointerException si falta alguno de los dos
     */
    public synchronized void addFlavorForUnencodedNative(String nat, DataFlavor flav) {
        if (flav == null || nat == null) {
            throw new NullPointerException("null arguments not permitted");
        }
        this.agregarFormato(nat, flav);
    }

    /**
     * Reemplaza la lista de formatos de un nombre nativo.
     *
     * @throws NullPointerException si falta el nombre o el arreglo trae un `null`
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
     * Codifica un tipo MIME de Java como nombre nativo.
     *
     * @return el nombre codificado, o `null` si el tipo MIME es `null`
     */
    public static String encodeJavaMIMEType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        return JAVA_PREFIX + mimeType;
    }

    /**
     * Codifica un formato como nombre nativo.
     *
     * @return el nombre codificado, o `null` si el formato o su tipo MIME es `null`
     */
    public static String encodeDataFlavor(DataFlavor flav) {
        if (flav == null) {
            return null;
        }
        return encodeJavaMIMEType(flav.getMimeType());
    }

    /** Si ese nombre nativo es un tipo MIME de Java codificado. */
    public static boolean isJavaMIMEType(String str) {
        return str != null && str.startsWith(JAVA_PREFIX, 0);
    }

    /**
     * Decodifica un nombre nativo a un tipo MIME de Java.
     *
     * @return el tipo MIME, o `null` si el nombre no está codificado
     */
    public static String decodeJavaMIMEType(String nat) {
        if (!isJavaMIMEType(nat)) {
            return null;
        }
        return nat.substring(JAVA_PREFIX.length());
    }

    /**
     * Decodifica un nombre nativo a un formato.
     *
     * @return el formato, o `null` si el nombre no está codificado
     * @throws ClassNotFoundException si la clase de representación no se puede cargar
     */
    public static DataFlavor decodeDataFlavor(String nat) throws ClassNotFoundException {
        String mimeType = decodeJavaMIMEType(nat);
        if (mimeType == null) {
            return null;
        }
        return new DataFlavor(mimeType);
    }
}
