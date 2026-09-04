package javax.print.attribute.standard;

import java.util.HashMap;
import java.util.Vector;
import javax.print.attribute.Attribute;
import javax.print.attribute.Size2DSyntax;

/**
 * Las medidas de un tamano de papel, y el registro que las conecta con los nombres de
 * {@link MediaSizeName}.
 *
 * <p>La clase misma es un {@link Size2DSyntax} con una restriccion --{@code x} no puede ser mayor
 * que {@code y}, o sea que el papel se declara siempre de pie-- y un nombre opcional. Lo que la
 * hace distinta del resto del paquete es el <b>registro estatico</b>: las clases anidadas de abajo
 * declaran los noventa y cuatro tamanos de norma, y sus constructores se van anotando en dos
 * tablas.
 *
 * <ul>
 * <li>Un mapa nombre {@code ->} medidas, que es el que contesta {@link #getMediaSizeForName}. Solo
 *     entran los que se declararon con un {@link MediaSizeName}, y solo el primero que reclama cada
 *     nombre.</li>
 * <li>Una lista de todos, incluso los anonimos, que es sobre la que busca {@link #findMedia}.</li>
 * </ul>
 *
 * <p>Que el registro se llene desde los constructores tiene una consecuencia: hay que forzar la
 * carga de las clases anidadas antes de contestar cualquiera de las dos consultas, porque si nadie
 * toco {@code MediaSize.ISO} todavia, sus constantes no existen y las tablas estan vacias. De eso
 * se encarga el bloque estatico del final. La recursion que eso arma --{@code ISO} inicializa
 * {@code MediaSize}, que toca {@code ISO}-- es legal y la JVM la corta sola: el segundo acceso
 * desde el mismo hilo pasa de largo.
 *
 * <p>Estas tablas son <b>datos de norma</b> (ISO 216, JIS P 0138, ANSI), no de locale. No dependen
 * del CLDR ni de ninguna impresora, asi que van completas y con los numeros exactos.
 *
 * <p>Que un tamano este aca no significa que ninguna impresora lo tenga cargado: eso lo contesta
 * {@code javax.print}, no este paquete.
 */
public class MediaSize extends Size2DSyntax implements Attribute {

    private static final long serialVersionUID = -1967958664615414771L;

    private MediaSizeName mediaName;

    // Las dos tablas del registro. Se inicializan aca arriba, antes del bloque estatico del final,
    // porque los constructores de las clases anidadas escriben en ellas mientras ese bloque corre.
    private static HashMap<MediaSizeName, MediaSize> mediaMap = new HashMap<MediaSizeName, MediaSize>(100, 10);

    private static Vector<MediaSize> sizeVector = new Vector<MediaSize>(100, 10);

    /** Un tamano anonimo: entra en la busqueda de {@link #findMedia} pero no reclama nombre. */
    public MediaSize(float x, float y, int units) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("X dimension > Y dimension");
        }
        sizeVector.add(this);
    }

    public MediaSize(int x, int y, int units) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("X dimension > Y dimension");
        }
        sizeVector.add(this);
    }

    /**
     * Un tamano con nombre. Si ese nombre ya estaba tomado el objeto se construye igual pero no se
     * registra en ningun lado --ni siquiera en la lista de busqueda-- y su
     * {@link #getMediaSizeName} queda en null: el primero que reclama un nombre se lo queda.
     */
    public MediaSize(float x, float y, int units, MediaSizeName media) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("X dimension > Y dimension");
        }
        if (media != null && mediaMap.get(media) == null) {
            this.mediaName = media;
            mediaMap.put(media, this);
            sizeVector.add(this);
        }
    }

    public MediaSize(int x, int y, int units, MediaSizeName media) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("X dimension > Y dimension");
        }
        if (media != null && mediaMap.get(media) == null) {
            this.mediaName = media;
            mediaMap.put(media, this);
            sizeVector.add(this);
        }
    }

    /** El nombre con el que se registro, o null si es anonimo. */
    public MediaSizeName getMediaSizeName() {
        return this.mediaName;
    }

    /** Las medidas de un nombre, o null si ese nombre no tiene ninguna registrada. */
    public static MediaSize getMediaSizeForName(MediaSizeName media) {
        return mediaMap.get(media);
    }

    /**
     * El nombre del tamano registrado mas parecido a las medidas dadas.
     *
     * <p>Nunca devuelve "no encontre": elige el de <b>menor distancia euclidea</b> entre los dos
     * pares de medidas y corta apenas encuentra una coincidencia exacta. Dos consecuencias que
     * conviene saber antes de confiar en el resultado: unas medidas absurdas igual devuelven algo,
     * y el resultado puede ser {@code null} sin que haya fallado nada --si el mas cercano resulta
     * ser uno de los sobres japoneses, que no tienen nombre.
     *
     * <p>El candidato inicial es A4, asi que con la lista vacia eso es lo que sale.
     */
    public static MediaSizeName findMedia(float x, float y, int units) {
        MediaSize match = MediaSize.ISO.A4;
        if (x <= 0.0f || y <= 0.0f || units < 1) {
            throw new IllegalArgumentException("args must be +ve values");
        }
        double ls = x * x + y * y;
        for (int i = 0; i < sizeVector.size(); i++) {
            MediaSize mediaSize = sizeVector.elementAt(i);
            float[] dim = mediaSize.getSize(units);
            if (x == dim[0] && y == dim[1]) {
                match = mediaSize;
                break;
            }
            double diffx = x - dim[0];
            double diffy = y - dim[1];
            double tmpLs = diffx * diffx + diffy * diffy;
            if (tmpLs < ls) {
                ls = tmpLs;
                match = mediaSize;
            }
        }
        return match.getMediaSizeName();
    }

    /** Mismas medidas y ademas ser un {@code MediaSize}: el nombre no entra en la comparacion. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof MediaSize;
    }

    public final Class<? extends Attribute> getCategory() {
        return MediaSize.class;
    }

    public final String getName() {
        return "media-size";
    }

    /**
     * Los tamanos de la norma ISO 216: las series A, B y C mas el sobre DL.

     * <p>Toda la serie A sale de partir A0 --un metro cuadrado-- por la mitad del lado largo, una y
     * otra vez, con la proporcion raiz de dos que hace que la mitad de una hoja sea semejante a la
     * hoja. La serie B son las medias geometricas entre dos A consecutivos y la C, los sobres para
     * meter una A sin doblarla. Los numeros van en milimetros porque asi los define la norma.

     * <p>{@code C0}, {@code C1} y {@code C2} existen como {@link MediaSizeName} pero no tienen
     * constante aca: nadie imprime en un sobre de un metro.
     */
    public static final class ISO {

        private ISO() {
        }

        public static final MediaSize A0 =
            new MediaSize(841, 1189, Size2DSyntax.MM, MediaSizeName.ISO_A0);

        public static final MediaSize A1 =
            new MediaSize(594, 841, Size2DSyntax.MM, MediaSizeName.ISO_A1);

        public static final MediaSize A2 =
            new MediaSize(420, 594, Size2DSyntax.MM, MediaSizeName.ISO_A2);

        public static final MediaSize A3 =
            new MediaSize(297, 420, Size2DSyntax.MM, MediaSizeName.ISO_A3);

        public static final MediaSize A4 =
            new MediaSize(210, 297, Size2DSyntax.MM, MediaSizeName.ISO_A4);

        public static final MediaSize A5 =
            new MediaSize(148, 210, Size2DSyntax.MM, MediaSizeName.ISO_A5);

        public static final MediaSize A6 =
            new MediaSize(105, 148, Size2DSyntax.MM, MediaSizeName.ISO_A6);

        public static final MediaSize A7 =
            new MediaSize(74, 105, Size2DSyntax.MM, MediaSizeName.ISO_A7);

        public static final MediaSize A8 =
            new MediaSize(52, 74, Size2DSyntax.MM, MediaSizeName.ISO_A8);

        public static final MediaSize A9 =
            new MediaSize(37, 52, Size2DSyntax.MM, MediaSizeName.ISO_A9);

        public static final MediaSize A10 =
            new MediaSize(26, 37, Size2DSyntax.MM, MediaSizeName.ISO_A10);

        public static final MediaSize B0 =
            new MediaSize(1000, 1414, Size2DSyntax.MM, MediaSizeName.ISO_B0);

        public static final MediaSize B1 =
            new MediaSize(707, 1000, Size2DSyntax.MM, MediaSizeName.ISO_B1);

        public static final MediaSize B2 =
            new MediaSize(500, 707, Size2DSyntax.MM, MediaSizeName.ISO_B2);

        public static final MediaSize B3 =
            new MediaSize(353, 500, Size2DSyntax.MM, MediaSizeName.ISO_B3);

        public static final MediaSize B4 =
            new MediaSize(250, 353, Size2DSyntax.MM, MediaSizeName.ISO_B4);

        public static final MediaSize B5 =
            new MediaSize(176, 250, Size2DSyntax.MM, MediaSizeName.ISO_B5);

        public static final MediaSize B6 =
            new MediaSize(125, 176, Size2DSyntax.MM, MediaSizeName.ISO_B6);

        public static final MediaSize B7 =
            new MediaSize(88, 125, Size2DSyntax.MM, MediaSizeName.ISO_B7);

        public static final MediaSize B8 =
            new MediaSize(62, 88, Size2DSyntax.MM, MediaSizeName.ISO_B8);

        public static final MediaSize B9 =
            new MediaSize(44, 62, Size2DSyntax.MM, MediaSizeName.ISO_B9);

        public static final MediaSize B10 =
            new MediaSize(31, 44, Size2DSyntax.MM, MediaSizeName.ISO_B10);

        public static final MediaSize C3 =
            new MediaSize(324, 458, Size2DSyntax.MM, MediaSizeName.ISO_C3);

        public static final MediaSize C4 =
            new MediaSize(229, 324, Size2DSyntax.MM, MediaSizeName.ISO_C4);

        public static final MediaSize C5 =
            new MediaSize(162, 229, Size2DSyntax.MM, MediaSizeName.ISO_C5);

        public static final MediaSize C6 =
            new MediaSize(114, 162, Size2DSyntax.MM, MediaSizeName.ISO_C6);

        public static final MediaSize DESIGNATED_LONG =
            new MediaSize(110, 220, Size2DSyntax.MM, MediaSizeName.ISO_DESIGNATED_LONG);
    }

    /**
     * Los tamanos japoneses de la norma JIS P 0138.

     * <p>Las B japonesas <b>no</b> son las B de ISO: JIS las define como la media aritmetica entre
     * dos A y no la geometrica, asi que JIS B4 mide 257x364 y no 250x353. Es la clase de detalle que
     * hace fallar un trabajo en silencio si uno confunde las tablas.

     * <p>Las series CHOU, KAKU y YOU son sobres, y son las unicas entradas del paquete que no tienen
     * {@link MediaSizeName}: {@code getMediaSizeName()} devuelve {@code null} para ellas, aunque
     * {@link MediaSize#findMedia} igual las considere.
     */
    public static final class JIS {

        private JIS() {
        }

        public static final MediaSize B0 =
            new MediaSize(1030, 1456, Size2DSyntax.MM, MediaSizeName.JIS_B0);

        public static final MediaSize B1 =
            new MediaSize(728, 1030, Size2DSyntax.MM, MediaSizeName.JIS_B1);

        public static final MediaSize B2 =
            new MediaSize(515, 728, Size2DSyntax.MM, MediaSizeName.JIS_B2);

        public static final MediaSize B3 =
            new MediaSize(364, 515, Size2DSyntax.MM, MediaSizeName.JIS_B3);

        public static final MediaSize B4 =
            new MediaSize(257, 364, Size2DSyntax.MM, MediaSizeName.JIS_B4);

        public static final MediaSize B5 =
            new MediaSize(182, 257, Size2DSyntax.MM, MediaSizeName.JIS_B5);

        public static final MediaSize B6 =
            new MediaSize(128, 182, Size2DSyntax.MM, MediaSizeName.JIS_B6);

        public static final MediaSize B7 =
            new MediaSize(91, 128, Size2DSyntax.MM, MediaSizeName.JIS_B7);

        public static final MediaSize B8 =
            new MediaSize(64, 91, Size2DSyntax.MM, MediaSizeName.JIS_B8);

        public static final MediaSize B9 =
            new MediaSize(45, 64, Size2DSyntax.MM, MediaSizeName.JIS_B9);

        public static final MediaSize B10 =
            new MediaSize(32, 45, Size2DSyntax.MM, MediaSizeName.JIS_B10);

        public static final MediaSize CHOU_1 =
            new MediaSize(142, 332, Size2DSyntax.MM);

        public static final MediaSize CHOU_2 =
            new MediaSize(119, 277, Size2DSyntax.MM);

        public static final MediaSize CHOU_3 =
            new MediaSize(120, 235, Size2DSyntax.MM);

        public static final MediaSize CHOU_4 =
            new MediaSize(90, 205, Size2DSyntax.MM);

        public static final MediaSize CHOU_30 =
            new MediaSize(92, 235, Size2DSyntax.MM);

        public static final MediaSize CHOU_40 =
            new MediaSize(90, 225, Size2DSyntax.MM);

        public static final MediaSize KAKU_0 =
            new MediaSize(287, 382, Size2DSyntax.MM);

        public static final MediaSize KAKU_1 =
            new MediaSize(270, 382, Size2DSyntax.MM);

        public static final MediaSize KAKU_2 =
            new MediaSize(240, 332, Size2DSyntax.MM);

        public static final MediaSize KAKU_3 =
            new MediaSize(216, 277, Size2DSyntax.MM);

        public static final MediaSize KAKU_4 =
            new MediaSize(197, 267, Size2DSyntax.MM);

        public static final MediaSize KAKU_5 =
            new MediaSize(190, 240, Size2DSyntax.MM);

        public static final MediaSize KAKU_6 =
            new MediaSize(162, 229, Size2DSyntax.MM);

        public static final MediaSize KAKU_7 =
            new MediaSize(142, 205, Size2DSyntax.MM);

        public static final MediaSize KAKU_8 =
            new MediaSize(119, 197, Size2DSyntax.MM);

        public static final MediaSize KAKU_20 =
            new MediaSize(229, 324, Size2DSyntax.MM);

        public static final MediaSize KAKU_A4 =
            new MediaSize(228, 312, Size2DSyntax.MM);

        public static final MediaSize YOU_1 =
            new MediaSize(120, 176, Size2DSyntax.MM);

        public static final MediaSize YOU_2 =
            new MediaSize(114, 162, Size2DSyntax.MM);

        public static final MediaSize YOU_3 =
            new MediaSize(98, 148, Size2DSyntax.MM);

        public static final MediaSize YOU_4 =
            new MediaSize(105, 235, Size2DSyntax.MM);

        public static final MediaSize YOU_5 =
            new MediaSize(95, 217, Size2DSyntax.MM);

        public static final MediaSize YOU_6 =
            new MediaSize(98, 190, Size2DSyntax.MM);

        public static final MediaSize YOU_7 =
            new MediaSize(92, 165, Size2DSyntax.MM);
    }

    /**
     * Los tamanos de America del Norte, incluidos los sobres numerados.

     * <p>Estan definidos en pulgadas y por eso se declaran en pulgadas: {@code 8.5x11} da
     * 215900x279400 micrometros exactos, mientras que escribirlo en milimetros perderia decimas.

     * <p>Los nombres de las constantes tienen la mayuscula de la X inconsistente
     * --{@code NA_9x11_ENVELOPE} contra {@code NA_10X15_ENVELOPE}-- porque asi estan en el JDK y
     * cambiarlo romperia el codigo que las usa.
     */
    public static final class NA {

        private NA() {
        }

        public static final MediaSize LETTER =
            new MediaSize(8.5f, 11.0f, Size2DSyntax.INCH, MediaSizeName.NA_LETTER);

        public static final MediaSize LEGAL =
            new MediaSize(8.5f, 14.0f, Size2DSyntax.INCH, MediaSizeName.NA_LEGAL);

        public static final MediaSize NA_5X7 =
            new MediaSize(5f, 7f, Size2DSyntax.INCH, MediaSizeName.NA_5X7);

        public static final MediaSize NA_8X10 =
            new MediaSize(8f, 10f, Size2DSyntax.INCH, MediaSizeName.NA_8X10);

        public static final MediaSize NA_NUMBER_9_ENVELOPE =
            new MediaSize(3.875f, 8.875f, Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_9_ENVELOPE);

        public static final MediaSize NA_NUMBER_10_ENVELOPE =
            new MediaSize(4.125f, 9.5f, Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_10_ENVELOPE);

        public static final MediaSize NA_NUMBER_11_ENVELOPE =
            new MediaSize(4.5f, 10.375f, Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_11_ENVELOPE);

        public static final MediaSize NA_NUMBER_12_ENVELOPE =
            new MediaSize(4.75f, 11.0f, Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_12_ENVELOPE);

        public static final MediaSize NA_NUMBER_14_ENVELOPE =
            new MediaSize(5f, 11.5f, Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_14_ENVELOPE);

        public static final MediaSize NA_6X9_ENVELOPE =
            new MediaSize(6f, 9f, Size2DSyntax.INCH, MediaSizeName.NA_6X9_ENVELOPE);

        public static final MediaSize NA_7X9_ENVELOPE =
            new MediaSize(7f, 9f, Size2DSyntax.INCH, MediaSizeName.NA_7X9_ENVELOPE);

        public static final MediaSize NA_9x11_ENVELOPE =
            new MediaSize(9f, 11f, Size2DSyntax.INCH, MediaSizeName.NA_9X11_ENVELOPE);

        public static final MediaSize NA_9x12_ENVELOPE =
            new MediaSize(9f, 12f, Size2DSyntax.INCH, MediaSizeName.NA_9X12_ENVELOPE);

        public static final MediaSize NA_10x13_ENVELOPE =
            new MediaSize(10f, 13f, Size2DSyntax.INCH, MediaSizeName.NA_10X13_ENVELOPE);

        public static final MediaSize NA_10x14_ENVELOPE =
            new MediaSize(10f, 14f, Size2DSyntax.INCH, MediaSizeName.NA_10X14_ENVELOPE);

        public static final MediaSize NA_10X15_ENVELOPE =
            new MediaSize(10f, 15f, Size2DSyntax.INCH, MediaSizeName.NA_10X15_ENVELOPE);
    }

    /**
     * Los cinco tamanos de plano de ingenieria de ANSI, de la A a la E.

     * <p>Cada uno es el doble del anterior girado noventa grados, arrancando de la carta: A es
     * 8.5x11, B es 11x17, y asi. Ojo con el choque de nombres: esta {@code A} es la carta
     * norteamericana y no tiene nada que ver con {@code ISO.A4}.
     */
    public static final class Engineering {

        private Engineering() {
        }

        public static final MediaSize A =
            new MediaSize(8.5f, 11f, Size2DSyntax.INCH, MediaSizeName.A);

        public static final MediaSize B =
            new MediaSize(11f, 17f, Size2DSyntax.INCH, MediaSizeName.B);

        public static final MediaSize C =
            new MediaSize(17f, 22f, Size2DSyntax.INCH, MediaSizeName.C);

        public static final MediaSize D =
            new MediaSize(22f, 34f, Size2DSyntax.INCH, MediaSizeName.D);

        public static final MediaSize E =
            new MediaSize(34f, 44f, Size2DSyntax.INCH, MediaSizeName.E);
    }

    /**
     * Los que no entran en ninguna norma: los formatos de oficina heredados y algunos
     * sobres sueltos.

     * <p>{@code LEDGER} y {@code TABLOID} miden exactamente lo mismo (11x17 pulgadas) y son dos
     * constantes distintas porque el nombre dice la orientacion con la que se usa cada uno --por eso
     * {@link MediaSize#findMedia} con esas medidas devuelve uno de los dos y no el otro.
     */
    public static final class Other {

        private Other() {
        }

        public static final MediaSize EXECUTIVE =
            new MediaSize(7.25f, 10.5f, Size2DSyntax.INCH, MediaSizeName.EXECUTIVE);

        public static final MediaSize LEDGER =
            new MediaSize(11f, 17f, Size2DSyntax.INCH, MediaSizeName.LEDGER);

        public static final MediaSize TABLOID =
            new MediaSize(11f, 17f, Size2DSyntax.INCH, MediaSizeName.TABLOID);

        public static final MediaSize INVOICE =
            new MediaSize(5.5f, 8.5f, Size2DSyntax.INCH, MediaSizeName.INVOICE);

        public static final MediaSize FOLIO =
            new MediaSize(8.5f, 13f, Size2DSyntax.INCH, MediaSizeName.FOLIO);

        public static final MediaSize QUARTO =
            new MediaSize(8.5f, 10.83f, Size2DSyntax.INCH, MediaSizeName.QUARTO);

        public static final MediaSize ITALY_ENVELOPE =
            new MediaSize(110f, 230f, Size2DSyntax.MM, MediaSizeName.ITALY_ENVELOPE);

        public static final MediaSize MONARCH_ENVELOPE =
            new MediaSize(3.87f, 7.5f, Size2DSyntax.INCH, MediaSizeName.MONARCH_ENVELOPE);

        public static final MediaSize PERSONAL_ENVELOPE =
            new MediaSize(3.625f, 6.5f, Size2DSyntax.INCH, MediaSizeName.PERSONAL_ENVELOPE);

        public static final MediaSize JAPANESE_POSTCARD =
            new MediaSize(100f, 148f, Size2DSyntax.MM, MediaSizeName.JAPANESE_POSTCARD);

        public static final MediaSize JAPANESE_DOUBLE_POSTCARD =
            new MediaSize(148f, 200f, Size2DSyntax.MM, MediaSizeName.JAPANESE_DOUBLE_POSTCARD);
    }

    // Forzar la carga de las cinco clases anidadas. Sin esto, preguntar por un tamano antes de
    // haber nombrado ninguna de ellas contestaria contra tablas vacias. Referenciar una sola
    // alcanza: cada una arrastra a MediaSize, que arrastra a la siguiente.
    static {
        MediaSize[] forzar = {ISO.A4, JIS.B4, NA.LETTER, Engineering.A, Other.EXECUTIVE};
    }
}
