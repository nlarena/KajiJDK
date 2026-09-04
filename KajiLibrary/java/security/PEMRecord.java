package java.security;

// Un bloque PEM tal como salio del archivo: su etiqueta, su cuerpo en base64 sin decodificar, y lo
// que venia escrito antes del "-----BEGIN".
//
// ===============================================================================================
// POR QUE ESTA CLASE SI Y PEMDecoder / PEMEncoder NO
// ===============================================================================================
//
// Las tres llegaron juntas al JDK 25, pero no piden lo mismo. Un `PEMDecoder` tiene que convertir
// los bytes en una `PrivateKey` o un `Certificate`, y para eso necesita una `KeyFactory` o una
// `CertificateFactory` que sepan el algoritmo. **En esta biblioteca no hay ningun proveedor
// registrado**, asi que todo `decode` terminaria tirando; y `withDecryption` ademas necesitaria
// cifrado simetrico, que tampoco hay. Un `PEMEncoder` arrastra lo mismo por el lado de
// `withEncryption`.
//
// Este registro, en cambio, **no decodifica nada**: guarda el texto tal cual. Es la unica parte de
// la API de PEM que se puede cumplir entera y de verdad, asi que es la unica que esta.
//
// ===============================================================================================
// DOS COSAS QUE SORPRENDEN Y SON CORRECTAS
// ===============================================================================================
//
// `content` **no** es el contenido decodificado: es el base64 crudo, sin los guiones ni los saltos.
// Y `leadingData` guarda lo que hubiera antes del bloque --comentarios, la salida de una
// herramienta-- que hay que conservar porque a veces esta firmado junto con el resto.
//
// Como todo `record` con un componente de arreglo, `equals` compara `leadingData` **por
// referencia**, no por contenido, y ni el constructor ni {@link #leadingData()} lo copian. Se deja
// asi porque es exactamente lo que hace el JDK: cambiarlo daria una clase con otra semantica y con
// el mismo nombre, que es peor que la sorpresa.
public record PEMRecord(String type, String content, byte[] leadingData) implements DEREncodable {

    // Cuantos caracteres de base64 entran en una linea. Es lo que fija el RFC 7468 y lo que espera
    // cualquier herramienta que despues lea el archivo.
    private static final int POR_LINEA = 64;

    /**
     * El `type` es **solo la etiqueta**: "CERTIFICATE", no la linea entera.
     *
     * <p>Por eso se rechaza lo que parezca sintaxis de PEM ya armada. Sin ese control, un
     * {@code new PEMRecord("BEGIN CERTIFICATE", ...)} produciria un
     * {@code -----BEGIN BEGIN CERTIFICATE-----} que ningun lector acepta, y el error aparecceria
     * recien al intentar leer el archivo. No se valida nada mas: mayusculas, minusculas o etiquetas
     * inventadas pasan, porque el registro no es quien decide que etiquetas existen.
     *
     * <p>Va escrito como constructor canonico completo y no en la forma compacta
     * ({@code public PEMRecord { ... }}) porque **nuestro javac todavia no parsea la forma
     * compacta**; ver el hallazgo #403 en COMPILER_FINDINGS.md. Las dos formas son equivalentes
     * para el lenguaje: la compacta solo ahorra escribir las tres asignaciones finales.
     */
    public PEMRecord(String type, String content, byte[] leadingData) {
        if (type == null) {
            throw new NullPointerException("\"type\" cannot be null.");
        }
        if (content == null) {
            throw new NullPointerException("\"content\" cannot be null.");
        }
        if (type.startsWith("-") || type.startsWith("BEGIN ") || type.startsWith("END ")) {
            throw new IllegalArgumentException("PEM syntax labels found.  "
                    + "Only the PEM type identifier is allowed");
        }
        this.type = type;
        this.content = content;
        this.leadingData = leadingData;
    }

    /**
     * `leadingData` queda en `null`, que no es lo mismo que un arreglo vacio: significa "no habia
     * nada antes del bloque", no "habia cero bytes".
     */
    public PEMRecord(String type, String content) {
        this(type, content, null);
    }

    /**
     * El bloque PEM armado, listo para escribir.
     *
     * <p>Separa con {@link System#lineSeparator()} y no con un "\n" fijo porque es lo que hace el
     * JDK: el resultado esta pensado para ir a un archivo de texto de la plataforma. Eso quiere
     * decir que **el texto que sale depende del sistema**, y una prueba que lo compare tiene que
     * armar lo esperado con el mismo separador en vez de escribirlo a mano.
     *
     * <p>Siempre hay al menos una linea de contenido, aunque `content` sea vacio: un bloque sin
     * ninguna linea en el medio no es lo que produce ninguna otra herramienta.
     *
     * <p>No incluye `leadingData`. Eso es lo que se guardo de **antes** del bloque; el bloque es
     * esto.
     */
    @Override
    public String toString() {
        String sep = System.lineSeparator();
        StringBuilder s = new StringBuilder();
        s.append("-----BEGIN ").append(type).append("-----").append(sep);
        int i = 0;
        do {
            int fin = Math.min(i + POR_LINEA, content.length());
            s.append(content, i, fin).append(sep);
            i = fin;
        } while (i < content.length());
        s.append("-----END ").append(type).append("-----").append(sep);
        return s.toString();
    }
}
