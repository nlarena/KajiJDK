package javax.xml.datatype;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.datatype.DatatypeConstants -- las constantes de los tipos de fecha y
 * duracion de XML Schema: los nombres de los tipos, los resultados de comparar, los meses, y el
 * valor que significa "este campo no esta".
 *
 * <p>Es una clase de solo constantes --constructor privado, nada que instanciar-- pero tres de sus
 * grupos dicen cosas que no son obvias y conviene leer antes de usarlas.
 *
 * <h2>{@link #INDETERMINATE}, que es el que sorprende</h2>
 *
 * <p>Comparar dos duraciones no siempre da un resultado. {@link Duration#compare} devuelve
 * {@link #LESSER}, {@link #EQUAL}, {@link #GREATER} <b>o {@link #INDETERMINATE}</b>, y ese cuarto
 * caso no es un error ni un "no se pudo": es la respuesta correcta.
 *
 * <p>El motivo es que un mes no tiene una cantidad fija de dias. {@code P1M} dura 28, 29, 30 o 31
 * dias segun cuando empiece, asi que frente a {@code P30D} no hay un orden: en febrero {@code P1M}
 * es mas corto, en marzo es mas largo. Decir {@code LESSER} o {@code GREATER} seria inventar; decir
 * {@code EQUAL} tambien. La unica respuesta honesta es que no se pueden ordenar.
 *
 * <p>La consecuencia practica pega fuerte: {@code Duration} <b>no</b> implementa
 * {@code Comparable}, y no puede, porque un orden total no existe. Y
 * {@link Duration#isLongerThan} devolviendo false no quiere decir "es mas corta o igual": puede
 * querer decir que no se sabe.
 *
 * <h2>{@link #MAX_TIMEZONE_OFFSET} es el minimo y {@link #MIN_TIMEZONE_OFFSET} el maximo</h2>
 *
 * <p>No es un error de lectura: {@code MAX_TIMEZONE_OFFSET} vale -840 y {@code MIN_TIMEZONE_OFFSET}
 * vale 840. Los nombres estan al reves de los numeros.
 *
 * <p>Lo que se puede afirmar sin adivinar por que, porque esta comprobado contra el JDK 25: el campo
 * de zona horaria se cuenta en minutos <b>tal como se escribe la zona</b> --{@code -03:00} da -180 y
 * {@code +05:30} da 330-- y {@link XMLGregorianCalendar#setTimezone} acepta exactamente el intervalo
 * de -840 a 840, rechazando 841 y -841 con {@link IllegalArgumentException}. O sea que el valor
 * <b>maximo</b> del campo es el que guarda la constante llamada {@code MIN_}, y el <b>minimo</b> el
 * de la llamada {@code MAX_}.
 *
 * <p>Se replican con esos nombres y esos valores porque son API publica y hay codigo que los usa por
 * nombre; darlos vuelta "arreglaria" la lectura y romperia a quien los compare.
 *
 * <h2>{@link #FIELD_UNDEFINED} en vez de null</h2>
 *
 * <p>Los campos de {@link XMLGregorianCalendar} son {@code int}, y un {@code int} no puede ser null.
 * Un {@code gMonth} --el tipo de "mayo, de cualquier anio"-- tiene mes y no tiene anio, asi que hace
 * falta un valor que signifique ausente. Es {@code Integer.MIN_VALUE}, elegido porque no es un anio,
 * ni un mes, ni un dia, ni una hora posible. El que lea {@code getYear()} tiene que compararlo
 * contra esta constante antes de usarlo: no hay excepcion que avise.
 *
 * <h2>Que hay aca</h2>
 *
 * <p>Los treinta y seis miembros publicos, con los mismos valores que el original --comprobados uno
 * por uno contra el JDK 25-- y la clase anidada {@link Field}, que es el testigo de tipo con que
 * {@link Duration#getField} y {@link Duration#isSet} nombran un campo sin usar cadenas.
 */
public final class DatatypeConstants {

    /** No hay nada que instanciar: son todas constantes. */
    private DatatypeConstants() {
    }

    /** Enero, contado desde uno --al reves que {@code java.util.Calendar}, que cuenta desde cero--. */
    public static final int JANUARY = 1;

    /** Febrero. */
    public static final int FEBRUARY = 2;

    /** Marzo. */
    public static final int MARCH = 3;

    /** Abril. */
    public static final int APRIL = 4;

    /** Mayo. */
    public static final int MAY = 5;

    /** Junio. */
    public static final int JUNE = 6;

    /** Julio. */
    public static final int JULY = 7;

    /** Agosto. */
    public static final int AUGUST = 8;

    /** Septiembre. */
    public static final int SEPTEMBER = 9;

    /** Octubre. */
    public static final int OCTOBER = 10;

    /** Noviembre. */
    public static final int NOVEMBER = 11;

    /** Diciembre, que es doce y no once. */
    public static final int DECEMBER = 12;

    /** El primero es menor que el segundo. */
    public static final int LESSER = -1;

    /** Los dos son el mismo valor. */
    public static final int EQUAL = 0;

    /** El primero es mayor que el segundo. */
    public static final int GREATER = 1;

    /**
     * No se pueden ordenar, y eso es la respuesta y no una falla.
     *
     * <p>Ver el encabezado de la clase: pasa cuando la comparacion depende de datos que no estan
     * --de que mes se trata, o de en que zona horaria-- y cualquier orden que se eligiera seria
     * inventado.
     */
    public static final int INDETERMINATE = 2;

    /**
     * El campo no esta puesto.
     *
     * <p>{@code Integer.MIN_VALUE}, escrito como literal porque asi figura en el original.
     */
    public static final int FIELD_UNDEFINED = Integer.MIN_VALUE;

    /** El campo de los anios de una {@link Duration}. */
    public static final Field YEARS = new Field("YEARS", 0);

    /** El campo de los meses. */
    public static final Field MONTHS = new Field("MONTHS", 1);

    /** El campo de los dias. */
    public static final Field DAYS = new Field("DAYS", 2);

    /** El campo de las horas. */
    public static final Field HOURS = new Field("HOURS", 3);

    /** El campo de los minutos. */
    public static final Field MINUTES = new Field("MINUTES", 4);

    /**
     * El campo de los segundos, que es el unico fraccionario.
     *
     * <p>{@link Duration#getField} lo devuelve como {@link java.math.BigDecimal} y no como
     * {@link java.math.BigInteger}, porque {@code PT0.5S} es una duracion valida.
     */
    public static final Field SECONDS = new Field("SECONDS", 5);

    /** El nombre calificado del tipo {@code xs:dateTime}. */
    public static final QName DATETIME =
            new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "dateTime");

    /** El nombre calificado del tipo {@code xs:time}. */
    public static final QName TIME = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "time");

    /** El nombre calificado del tipo {@code xs:date}. */
    public static final QName DATE = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "date");

    /** El nombre calificado del tipo {@code xs:gYearMonth}: un mes de un anio, sin dia. */
    public static final QName GYEARMONTH =
            new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gYearMonth");

    /** El nombre calificado del tipo {@code xs:gMonthDay}: un dia del anio, sin anio. */
    public static final QName GMONTHDAY =
            new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gMonthDay");

    /** El nombre calificado del tipo {@code xs:gYear}. */
    public static final QName GYEAR = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gYear");

    /** El nombre calificado del tipo {@code xs:gMonth}. */
    public static final QName GMONTH = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gMonth");

    /** El nombre calificado del tipo {@code xs:gDay}. */
    public static final QName GDAY = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gDay");

    /** El nombre calificado del tipo {@code xs:duration}, el que puede tener los seis campos. */
    public static final QName DURATION =
            new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "duration");

    /**
     * El nombre calificado de {@code xdt:dayTimeDuration}: dias, horas, minutos y segundos.
     *
     * <p>Es de XPath 2.0 y no de XML Schema, y de ahi que el espacio de nombres sea otro. Existe
     * justamente por lo de {@link #INDETERMINATE}: una duracion sin meses <b>si</b> se puede
     * ordenar, porque un dia siempre dura lo mismo.
     */
    public static final QName DURATION_DAYTIME =
            new QName(XMLConstants.W3C_XPATH_DATATYPE_NS_URI, "dayTimeDuration");

    /**
     * El nombre calificado de {@code xdt:yearMonthDuration}: anios y meses.
     *
     * <p>La otra mitad ordenable: contada en meses, tampoco tiene ambigüedad.
     */
    public static final QName DURATION_YEARMONTH =
            new QName(XMLConstants.W3C_XPATH_DATATYPE_NS_URI, "yearMonthDuration");

    /**
     * El extremo <b>inferior</b> del campo de zona horaria, en minutos: -840, o sea {@code -14:00}.
     *
     * <p>Que la constante llamada "MAX" guarde el minimo esta explicado en el encabezado de la
     * clase; el nombre es el del original y no se toca.
     */
    public static final int MAX_TIMEZONE_OFFSET = -14 * 60;

    /** El extremo <b>superior</b>, en minutos: 840, o sea {@code +14:00}. */
    public static final int MIN_TIMEZONE_OFFSET = 14 * 60;

    /**
     * Uno de los seis campos de una {@link Duration}, como objeto.
     *
     * <p>Existe para que {@link Duration#getField} y {@link Duration#isSet} tomen un campo sin que
     * el llamador pase una cadena que se puede escribir mal. Las seis instancias posibles son las
     * constantes de arriba y no hay forma de crear otras --el constructor es privado--, asi que
     * comparar con {@code ==} es correcto y es lo que hacen las implementaciones.
     *
     * <p>Es de antes de que el lenguaje tuviera {@code enum}; con {@code enum} hoy no se escribiria
     * asi, pero cambiarlo romperia la serializacion y la comparacion por identidad de todo el codigo
     * que ya existe.
     */
    public static final class Field {

        /** El nombre, que es lo unico que se ve desde afuera. */
        private final String str;

        /** El indice, de 0 a 5, en el orden en que los campos van en la representacion lexica. */
        private final int id;

        /**
         * Solo desde aca adentro: las seis instancias son las constantes de la clase envolvente.
         *
         * @param str el nombre
         * @param id el indice
         */
        private Field(String str, int id) {
            this.str = str;
            this.id = id;
        }

        /**
         * El nombre del campo, en mayusculas, igual al de la constante.
         *
         * @return por ejemplo {@code "YEARS"}
         */
        public String toString() {
            return str;
        }

        /**
         * El indice del campo, de {@code YEARS} = 0 a {@code SECONDS} = 5.
         *
         * <p>El orden es el de la representacion lexica {@code PnYnMnDTnHnMnS}, que es lo que lo
         * hace util: sirve de indice en un arreglo de campos sin tener que traducir nada.
         *
         * @return de 0 a 5
         */
        public int getId() {
            return id;
        }
    }
}
