package javax.xml.datatype;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * KajiLibrary's javax.xml.datatype.DatatypeFactory -- de donde salen las {@link Duration} y las
 * {@link XMLGregorianCalendar}.
 *
 * <p>Es una fabrica conectable como las demas de JAXP: la aplicacion pide un tipo abstracto y la
 * implementacion se descubre en tiempo de ejecucion. {@link #newInstance()} mira, en este orden, la
 * propiedad de sistema {@link #DATATYPEFACTORY_PROPERTY}, el archivo
 * {@code $java.home/conf/jaxp.properties}, los proveedores declarados via {@link ServiceLoader}, y
 * por ultimo la implementacion de la plataforma.
 *
 * <h2>Aca el ultimo escalon existe, y esa es la diferencia con el resto de la pila</h2>
 *
 * <p>Las otras fabricas de esta biblioteca --{@link javax.xml.transform.TransformerFactory},
 * {@link javax.xml.stream.XMLInputFactory}-- fallan cuando llegan al final, porque lo que tendrian
 * que devolver es un procesador de XSLT o un parser de XML y esta biblioteca no trae ninguno.
 *
 * <p>Esta no. Una {@code Duration} es aritmetica sobre seis numeros y una
 * {@code XMLGregorianCalendar} es un calendario con campos opcionales: <b>ninguna de las dos
 * necesita leer un documento</b>. Su forma lexica, {@code P1Y2M3DT4H5M6S} o
 * {@code 2024-05-25T12:00:00-03:00}, es una cadena con numeros y letras, no XML. Asi que
 * {@link #newDefaultInstance()} devuelve una implementacion de verdad, y {@link #newInstance()} la
 * encuentra: las duraciones y las fechas de esta biblioteca calculan.
 *
 * <h2>Lo abstracto y lo concreto</h2>
 *
 * <p>La division es la misma del original y vale entenderla. Los siete metodos abstractos son los
 * <b>generales</b>: los que toman {@link BigInteger} y {@link BigDecimal}, que es lo unico que no
 * pierde precision. Los doce concretos son <b>atajos</b> escritos en terminos de aquellos: el que
 * toma {@code int} convierte y delega, {@code newDurationDayTime(long)} arma la duracion completa y
 * se queda con los campos de dia y tiempo, {@code newXMLGregorianCalendarDate} llama al general con
 * los campos de hora en {@link DatatypeConstants#FIELD_UNDEFINED}.
 *
 * <p>Asi que una implementacion que escriba los siete recibe los doce funcionando, y --lo que
 * importa mas-- los doce se comportan igual en cualquier implementacion, porque estan escritos una
 * sola vez y aca.
 */
public abstract class DatatypeFactory {

    /**
     * La propiedad de sistema con que se enchufa otra implementacion:
     * {@code javax.xml.datatype.DatatypeFactory}.
     */
    public static final String DATATYPEFACTORY_PROPERTY = "javax.xml.datatype.DatatypeFactory";

    /**
     * El nombre de la clase de la implementacion de la plataforma.
     *
     * <p>Es la de Xerces en el JDK; aca es la nuestra. La constante existe porque es parte de la API
     * publica --hay codigo que la compara-- pero es una cadena informativa y no un punto de
     * extension: cambiarla no cambia lo que devuelve {@link #newDefaultInstance()}.
     */
    public static final String DATATYPEFACTORY_IMPLEMENTATION_CLASS =
            "javax.xml.datatype.KajiDatatypeFactory";

    /** Para las subclases; no hay estado que inicializar. */
    protected DatatypeFactory() {
    }

    // ---- descubrimiento ---------------------------------------------------------------------

    /**
     * La implementacion de la plataforma, sin mirar la configuracion.
     *
     * <p>Se saltea los escalones de {@link #newInstance()} a proposito: existe para que una pieza
     * que necesita la implementacion de referencia --y no la que la aplicacion haya enchufado-- la
     * pueda pedir.
     *
     * <p>A diferencia de las otras fabricas de esta biblioteca, aca hay una y este metodo la
     * devuelve. Ver el encabezado de la clase.
     *
     * @return la implementacion de la plataforma
     */
    public static DatatypeFactory newDefaultInstance() {
        return new KajiDatatypeFactory();
    }

    /**
     * La fabrica configurada, buscada en los cuatro escalones del encabezado.
     *
     * @return la fabrica encontrada; nunca null
     * @throws DatatypeConfigurationException si un escalon nombra una clase que no se puede cargar
     */
    public static DatatypeFactory newInstance() throws DatatypeConfigurationException {
        // 1. La propiedad de sistema.
        String className = null;
        try {
            className = System.getProperty(DATATYPEFACTORY_PROPERTY);
        } catch (SecurityException ignored) {
            // Sin permiso para leerla es lo mismo que no estar puesta.
        }
        if (className != null && className.length() > 0) {
            return instantiate(className, null);
        }

        // 2. $java.home/conf/jaxp.properties.
        className = fromJaxpProperties();
        if (className != null && className.length() > 0) {
            return instantiate(className, null);
        }

        // 3. Los proveedores declarados en el classpath.
        DatatypeFactory fromService = fromServiceLoader();
        if (fromService != null) {
            return fromService;
        }

        // 4. La implementacion de la plataforma, que aca si existe.
        return newDefaultInstance();
    }

    /**
     * Una fabrica de una clase nombrada, sin descubrimiento ninguno.
     *
     * @param factoryClassName el nombre completo de la clase; no puede ser null
     * @param classLoader con que cargarla; null usa el que corresponda por omision
     * @return la fabrica
     * @throws DatatypeConfigurationException si la clase no esta o no se puede instanciar
     */
    public static DatatypeFactory newInstance(String factoryClassName, ClassLoader classLoader)
            throws DatatypeConfigurationException {
        if (factoryClassName == null) {
            throw new DatatypeConfigurationException(
                    "Provider for " + DATATYPEFACTORY_PROPERTY + " cannot be found");
        }
        return instantiate(factoryClassName, classLoader);
    }

    /**
     * Carga e instancia la clase nombrada, con los dos mensajes de error que el contrato distingue.
     *
     * <p>Se separan porque se arreglan distinto: **not found** es un jar que falta, **could not be
     * instantiated** es una clase que esta pero no sirve.
     */
    private static DatatypeFactory instantiate(String className, ClassLoader loader)
            throws DatatypeConfigurationException {
        Class<?> cls;
        try {
            if (loader == null) {
                cls = Class.forName(className);
            } else {
                cls = Class.forName(className, false, loader);
            }
        } catch (ClassNotFoundException e) {
            throw new DatatypeConfigurationException("Provider " + className + " not found", e);
        }
        Object obj;
        try {
            obj = cls.newInstance();
        } catch (Exception e) {
            throw new DatatypeConfigurationException(
                    "Provider " + className + " could not be instantiated: " + e, e);
        }
        if (!(obj instanceof DatatypeFactory)) {
            throw new DatatypeConfigurationException(
                    "Provider " + className + " could not be instantiated: "
                            + className + " cannot be cast to " + DATATYPEFACTORY_PROPERTY);
        }
        return (DatatypeFactory) obj;
    }

    /**
     * El nombre de clase que declare {@code $java.home/conf/jaxp.properties}, o null.
     *
     * <p>Cualquier fallo de lectura devuelve null en vez de propagar: el archivo es opcional, y que
     * no se pueda leer es la ausencia de configuracion y no un error.
     */
    private static String fromJaxpProperties() {
        try {
            String home = System.getProperty("java.home");
            if (home == null) {
                return null;
            }
            File f = new File(new File(new File(home), "conf"), "jaxp.properties");
            if (!f.exists()) {
                return null;
            }
            Properties props = new Properties();
            InputStream in = new FileInputStream(f);
            try {
                props.load(in);
            } finally {
                in.close();
            }
            return props.getProperty(DATATYPEFACTORY_PROPERTY);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * La primera fabrica que declare un proveedor del classpath, o null.
     *
     * <p>Hoy siempre da null, y no por un atajo de aca: el {@link ServiceLoader} de esta biblioteca
     * no puede enumerar {@code META-INF/services} porque nuestro {@code ClassLoader} no tiene
     * recursos. La maquinaria esta enchufada donde va.
     */
    private static DatatypeFactory fromServiceLoader() {
        try {
            ServiceLoader<DatatypeFactory> sl = ServiceLoader.load(DatatypeFactory.class);
            Iterator<DatatypeFactory> it = sl.iterator();
            if (it.hasNext()) {
                return it.next();
            }
        } catch (Throwable ignored) {
            // Un proveedor roto no puede impedir que se pruebe el escalon siguiente.
        }
        return null;
    }

    // ---- duraciones -------------------------------------------------------------------------

    /**
     * Una duracion a partir de su forma lexica {@code PnYnMnDTnHnMnS}.
     *
     * <p>Solo aparecen los campos que se escriban: {@code P1Y} deja los otros cinco ausentes, que no
     * es lo mismo que ponerlos en cero. Ver {@link Duration#isSet}.
     *
     * @param lexicalRepresentation la forma lexica; no puede ser null
     * @return la duracion
     * @throws IllegalArgumentException si no es una forma lexica valida
     * @throws UnsupportedOperationException si la implementacion no la soporta
     */
    public abstract Duration newDuration(String lexicalRepresentation);

    /**
     * Una duracion de tantos milisegundos, con los seis campos puestos.
     *
     * <p>Los anios y los meses salen de contar sobre el calendario desde la epoca, que es la unica
     * forma de repartir milisegundos en campos de largo variable.
     *
     * @param durationInMilliseconds los milisegundos, con signo
     * @return la duracion
     */
    public abstract Duration newDuration(long durationInMilliseconds);

    /**
     * Una duracion campo por campo; null en un campo lo deja ausente.
     *
     * <p>Es el constructor general y el que los demas terminan llamando. Los valores tienen que ser
     * no negativos: el signo va aparte, en {@code isPositive}.
     *
     * @param isPositive el signo
     * @param years los anios, o null
     * @param months los meses, o null
     * @param days los dias, o null
     * @param hours las horas, o null
     * @param minutes los minutos, o null
     * @param seconds los segundos, con fraccion, o null
     * @return la duracion
     * @throws IllegalArgumentException si todos los campos son null o si alguno es negativo
     */
    public abstract Duration newDuration(
            boolean isPositive,
            BigInteger years,
            BigInteger months,
            BigInteger days,
            BigInteger hours,
            BigInteger minutes,
            BigDecimal seconds);

    /**
     * Lo mismo con {@code int}, donde {@link DatatypeConstants#FIELD_UNDEFINED} deja el campo
     * ausente.
     *
     * @param isPositive el signo
     * @param years los anios
     * @param months los meses
     * @param days los dias
     * @param hours las horas
     * @param minutes los minutos
     * @param seconds los segundos, sin fraccion
     * @return la duracion
     * @throws IllegalArgumentException si todos estan indefinidos o si alguno es negativo
     */
    public Duration newDuration(
            final boolean isPositive,
            final int years,
            final int months,
            final int days,
            final int hours,
            final int minutes,
            final int seconds) {
        return newDuration(
                isPositive,
                toInt(years),
                toInt(months),
                toInt(days),
                toInt(hours),
                toInt(minutes),
                seconds != DatatypeConstants.FIELD_UNDEFINED
                        ? BigDecimal.valueOf((long) seconds) : null);
    }

    /**
     * Una {@code xdt:dayTimeDuration} a partir de su forma lexica.
     *
     * <p>Es una duracion sin anios ni meses, y esa restriccion es toda la gracia del tipo: sin meses
     * la comparacion nunca da {@link DatatypeConstants#INDETERMINATE}, porque un dia siempre dura lo
     * mismo. De ahi que el control sea sobre la <b>forma lexica</b> --que no tenga {@code Y} ni
     * {@code M} antes de la {@code T}-- y no sobre los campos: una cadena con anios se rechaza aca y
     * no despues.
     *
     * @param lexicalRepresentation la forma lexica, {@code PnDTnHnMnS}; no puede ser null
     * @return la duracion
     * @throws IllegalArgumentException si tiene anios o meses, o si la forma esta mal
     * @throws NullPointerException si es null
     */
    public Duration newDurationDayTime(final String lexicalRepresentation) {
        if (lexicalRepresentation == null) {
            throw new NullPointerException(
                    "Trying to create an xdt:dayTimeDuration with an invalid"
                            + " lexical representation of \"null\"");
        }
        if (!isDayTimeForm(lexicalRepresentation)) {
            throw new IllegalArgumentException(
                    "Trying to create an xdt:dayTimeDuration with an invalid"
                            + " lexical representation of \"" + lexicalRepresentation
                            + "\", data model requires PnDTnHnMnS.");
        }
        return newDuration(lexicalRepresentation);
    }

    /**
     * Una {@code xdt:dayTimeDuration} de tantos milisegundos.
     *
     * <p>Arma la duracion completa y se queda con los cuatro campos de dia y tiempo; los de anio y
     * mes se descartan, no se suman a los dias.
     *
     * @param durationInMilliseconds los milisegundos, con signo
     * @return la duracion
     */
    public Duration newDurationDayTime(final long durationInMilliseconds) {
        Duration complete = newDuration(durationInMilliseconds);
        BigInteger days = (BigInteger) complete.getField(DatatypeConstants.DAYS);
        BigInteger hours = (BigInteger) complete.getField(DatatypeConstants.HOURS);
        BigInteger minutes = (BigInteger) complete.getField(DatatypeConstants.MINUTES);
        BigDecimal seconds = (BigDecimal) complete.getField(DatatypeConstants.SECONDS);
        return newDuration(
                complete.getSign() != -1,
                null,
                null,
                days != null ? days : BigInteger.ZERO,
                hours != null ? hours : BigInteger.ZERO,
                minutes != null ? minutes : BigInteger.ZERO,
                seconds != null ? seconds : BigDecimal.ZERO);
    }

    /**
     * Una {@code xdt:dayTimeDuration} campo por campo.
     *
     * @param isPositive el signo
     * @param day los dias, o null
     * @param hour las horas, o null
     * @param minute los minutos, o null
     * @param second los segundos, o null
     * @return la duracion
     * @throws IllegalArgumentException si todos son null o si alguno es negativo
     */
    public Duration newDurationDayTime(
            final boolean isPositive,
            final BigInteger day,
            final BigInteger hour,
            final BigInteger minute,
            final BigInteger second) {
        return newDuration(
                isPositive, null, null, day, hour, minute,
                second != null ? new BigDecimal(second) : null);
    }

    /**
     * Lo mismo con {@code int}.
     *
     * @param isPositive el signo
     * @param day los dias
     * @param hour las horas
     * @param minute los minutos
     * @param second los segundos
     * @return la duracion
     * @throws IllegalArgumentException si alguno es negativo
     */
    public Duration newDurationDayTime(
            final boolean isPositive,
            final int day,
            final int hour,
            final int minute,
            final int second) {
        return newDurationDayTime(
                isPositive,
                BigInteger.valueOf((long) day),
                BigInteger.valueOf((long) hour),
                BigInteger.valueOf((long) minute),
                BigInteger.valueOf((long) second));
    }

    /**
     * Una {@code xdt:yearMonthDuration} a partir de su forma lexica.
     *
     * <p>La otra mitad ordenable: solo anios y meses. Contada en meses tampoco tiene ambigüedad.
     *
     * @param lexicalRepresentation la forma lexica, {@code PnYnM}; no puede ser null
     * @return la duracion
     * @throws IllegalArgumentException si tiene dias u hora, o si la forma esta mal
     * @throws NullPointerException si es null
     */
    public Duration newDurationYearMonth(final String lexicalRepresentation) {
        if (lexicalRepresentation == null) {
            throw new NullPointerException(
                    "Trying to create an xdt:yearMonthDuration with an invalid"
                            + " lexical representation of \"null\"");
        }
        if (!isYearMonthForm(lexicalRepresentation)) {
            throw new IllegalArgumentException(
                    "Trying to create an xdt:yearMonthDuration with an invalid"
                            + " lexical representation of \"" + lexicalRepresentation
                            + "\", data model requires PnYnM.");
        }
        return newDuration(lexicalRepresentation);
    }

    /**
     * Una {@code xdt:yearMonthDuration} de tantos milisegundos.
     *
     * <p>Arma la duracion completa y se queda con los anios y los meses; los dias y el tiempo se
     * descartan. Por eso {@code newDurationYearMonth} de un dia entero da {@code P0Y0M} y no una
     * fraccion de mes.
     *
     * @param durationInMilliseconds los milisegundos, con signo
     * @return la duracion
     */
    public Duration newDurationYearMonth(final long durationInMilliseconds) {
        Duration complete = newDuration(durationInMilliseconds);
        BigInteger years = (BigInteger) complete.getField(DatatypeConstants.YEARS);
        BigInteger months = (BigInteger) complete.getField(DatatypeConstants.MONTHS);
        return newDurationYearMonth(
                complete.getSign() != -1,
                years != null ? years : BigInteger.ZERO,
                months != null ? months : BigInteger.ZERO);
    }

    /**
     * Una {@code xdt:yearMonthDuration} campo por campo.
     *
     * @param isPositive el signo
     * @param year los anios, o null
     * @param month los meses, o null
     * @return la duracion
     * @throws IllegalArgumentException si los dos son null o si alguno es negativo
     */
    public Duration newDurationYearMonth(
            final boolean isPositive, final BigInteger year, final BigInteger month) {
        return newDuration(isPositive, year, month, null, null, null, null);
    }

    /**
     * Lo mismo con {@code int}.
     *
     * @param isPositive el signo
     * @param year los anios
     * @param month los meses
     * @return la duracion
     * @throws IllegalArgumentException si alguno es negativo
     */
    public Duration newDurationYearMonth(
            final boolean isPositive, final int year, final int month) {
        return newDurationYearMonth(
                isPositive, BigInteger.valueOf((long) year), BigInteger.valueOf((long) month));
    }

    /**
     * La forma lexica de una {@code dayTimeDuration}: {@code [^YM]*[DT][^Y]*}.
     *
     * <p>La expresion no es la que uno escribiria de memoria y vale leerla despacio. Pide tres
     * cosas: que haya una {@code D} o una {@code T}, que antes no haya ni {@code Y} ni {@code M}, y
     * que despues no haya {@code Y}. La asimetria --{@code M} prohibida antes pero permitida
     * despues-- es justamente el punto: la {@code M} de <b>meses</b> va antes de la {@code T} y esta
     * prohibida, y la de <b>minutos</b> va despues y es legal. Una expresion simetrica rechazaria
     * {@code PT1M}, que es una duracion de dia-tiempo perfectamente valida.
     *
     * <p>Es la del JDK, y esta comprobada contra el: se corrieron las ocho formas de la tabla de
     * {@code XmlDatatypeDurTest} --{@code PT1M}, {@code P1DT1M}, {@code PT1H}, {@code P1D},
     * {@code PT0.5S}, {@code -P1DT2H}, {@code P1M}, {@code P1Y}-- contra
     * {@code java.exe} y las ocho respuestas coinciden.
     */
    private static final java.util.regex.Pattern FORMA_DAYTIME =
            java.util.regex.Pattern.compile("[^YM]*[DT][^Y]*");

    /**
     * La forma lexica de una {@code yearMonthDuration}: {@code [^DT]*}.
     *
     * <p>Aca alcanza con prohibir la {@code D} y la {@code T}, porque ninguna de las dos aparece
     * nunca en una duracion de anios y meses, y sin {@code T} no hay minutos con que confundir la
     * {@code M}.
     */
    private static final java.util.regex.Pattern FORMA_YEARMONTH =
            java.util.regex.Pattern.compile("[^DT]*");

    /** Si la forma lexica es la de una {@code dayTimeDuration}. */
    private static boolean isDayTimeForm(String lexical) {
        return FORMA_DAYTIME.matcher(lexical).matches();
    }

    /** Si la forma lexica es la de una {@code yearMonthDuration}. */
    private static boolean isYearMonthForm(String lexical) {
        return FORMA_YEARMONTH.matcher(lexical).matches();
    }

    /** {@link DatatypeConstants#FIELD_UNDEFINED} se vuelve null; el resto, un {@link BigInteger}. */
    private static BigInteger toInt(int v) {
        return v != DatatypeConstants.FIELD_UNDEFINED ? BigInteger.valueOf((long) v) : null;
    }

    // ---- fechas ------------------------------------------------------------------------------

    /**
     * Una fecha con todos los campos sin definir.
     *
     * <p>Para llenarla despues con los setters; ver {@link XMLGregorianCalendar#clear}.
     *
     * @return la fecha vacia
     */
    public abstract XMLGregorianCalendar newXMLGregorianCalendar();

    /**
     * Una fecha a partir de su forma lexica.
     *
     * <p>Acepta las ocho de XML Schema --{@code dateTime}, {@code date}, {@code time},
     * {@code gYearMonth}, {@code gMonthDay}, {@code gYear}, {@code gMonth} y {@code gDay}-- y decide
     * cual es por la forma. Los campos que el tipo no tenga quedan en
     * {@link DatatypeConstants#FIELD_UNDEFINED}.
     *
     * @param lexicalRepresentation la forma lexica; no puede ser null
     * @return la fecha
     * @throws IllegalArgumentException si no es ninguna de las ocho formas
     * @throws NullPointerException si es null
     */
    public abstract XMLGregorianCalendar newXMLGregorianCalendar(String lexicalRepresentation);

    /**
     * Una fecha copiada de un {@link GregorianCalendar}.
     *
     * <p>Todos los campos quedan definidos, incluida la zona horaria: un {@code GregorianCalendar}
     * siempre tiene una, asi que el resultado es siempre un {@code xs:dateTime} completo.
     *
     * @param cal el calendario; no puede ser null
     * @return la fecha
     * @throws NullPointerException si es null
     */
    public abstract XMLGregorianCalendar newXMLGregorianCalendar(GregorianCalendar cal);

    /**
     * Una fecha campo por campo, con el anio sin tope.
     *
     * <p>Es el constructor general. {@link DatatypeConstants#FIELD_UNDEFINED} --o null para el
     * anio y la fraccion-- deja el campo sin definir, que es como se arman los tipos parciales.
     *
     * @param year el anio, o null
     * @param month el mes de 1 a 12, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param day el dia de 1 a 31, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param hour la hora de 0 a 23, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param minute el minuto de 0 a 59, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param second el segundo de 0 a 60, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param fractionalSecond la fraccion, de 0 inclusive a 1 exclusive, o null
     * @param timezone los minutos de desplazamiento, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @return la fecha
     * @throws IllegalArgumentException si algun campo esta fuera de rango
     */
    public abstract XMLGregorianCalendar newXMLGregorianCalendar(
            BigInteger year,
            int month,
            int day,
            int hour,
            int minute,
            int second,
            BigDecimal fractionalSecond,
            int timezone);

    /**
     * Lo mismo con el anio como {@code int} y la fraccion como milisegundos.
     *
     * @param year el anio, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param month el mes
     * @param day el dia
     * @param hour la hora
     * @param minute el minuto
     * @param second el segundo
     * @param millisecond los milisegundos de 0 a 1000, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param timezone los minutos de desplazamiento
     * @return la fecha
     * @throws IllegalArgumentException si algun campo esta fuera de rango
     */
    public XMLGregorianCalendar newXMLGregorianCalendar(
            final int year,
            final int month,
            final int day,
            final int hour,
            final int minute,
            final int second,
            final int millisecond,
            final int timezone) {
        BigInteger realYear = toInt(year);
        BigDecimal fractionValue = null;
        if (millisecond != DatatypeConstants.FIELD_UNDEFINED) {
            if (millisecond < 0 || millisecond > 1000) {
                throw new IllegalArgumentException(
                        "javax.xml.datatype.DatatypeFactory#newXMLGregorianCalendar("
                                + "int year, int month, int day, int hour, int minute,"
                                + " int second, int millisecond, int timezone)"
                                + " with invalid millisecond: " + millisecond);
            }
            // Escala tres: los milisegundos son la fraccion con tres decimales, y guardarlos asi
            // hace que `toXMLFormat` escriba `.500` y no `.5`, que es lo que hace el original.
            fractionValue = BigDecimal.valueOf((long) millisecond, 3);
        }
        return newXMLGregorianCalendar(
                realYear, month, day, hour, minute, second, fractionValue, timezone);
    }

    /**
     * Un {@code xs:date}: anio, mes, dia y zona, sin hora.
     *
     * @param year el anio
     * @param month el mes
     * @param day el dia
     * @param timezone los minutos de desplazamiento, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @return la fecha
     * @throws IllegalArgumentException si algun campo esta fuera de rango
     */
    public XMLGregorianCalendar newXMLGregorianCalendarDate(
            final int year, final int month, final int day, final int timezone) {
        return newXMLGregorianCalendar(
                year,
                month,
                day,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                timezone);
    }

    /**
     * Un {@code xs:time}: hora, minuto, segundo y zona, sin fecha.
     *
     * @param hours la hora
     * @param minutes el minuto
     * @param seconds el segundo
     * @param timezone los minutos de desplazamiento, o {@link DatatypeConstants#FIELD_UNDEFINED}
     * @return la hora
     * @throws IllegalArgumentException si algun campo esta fuera de rango
     */
    public XMLGregorianCalendar newXMLGregorianCalendarTime(
            final int hours, final int minutes, final int seconds, final int timezone) {
        return newXMLGregorianCalendar(
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                hours,
                minutes,
                seconds,
                DatatypeConstants.FIELD_UNDEFINED,
                timezone);
    }

    /**
     * Un {@code xs:time} con la fraccion de segundo completa.
     *
     * @param hours la hora
     * @param minutes el minuto
     * @param seconds el segundo
     * @param fractionalSecond la fraccion, de 0 inclusive a 1 exclusive, o null
     * @param timezone los minutos de desplazamiento
     * @return la hora
     * @throws IllegalArgumentException si algun campo esta fuera de rango
     */
    public XMLGregorianCalendar newXMLGregorianCalendarTime(
            final int hours,
            final int minutes,
            final int seconds,
            final BigDecimal fractionalSecond,
            final int timezone) {
        return newXMLGregorianCalendar(
                null,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                hours,
                minutes,
                seconds,
                fractionalSecond,
                timezone);
    }

    /**
     * Un {@code xs:time} con la fraccion dada en milisegundos.
     *
     * @param hours la hora
     * @param minutes el minuto
     * @param seconds el segundo
     * @param milliseconds los milisegundos
     * @param timezone los minutos de desplazamiento
     * @return la hora
     * @throws IllegalArgumentException si algun campo esta fuera de rango
     */
    public XMLGregorianCalendar newXMLGregorianCalendarTime(
            final int hours,
            final int minutes,
            final int seconds,
            final int milliseconds,
            final int timezone) {
        return newXMLGregorianCalendar(
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                hours,
                minutes,
                seconds,
                milliseconds,
                timezone);
    }
}
