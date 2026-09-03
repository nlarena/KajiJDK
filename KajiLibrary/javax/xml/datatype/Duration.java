package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.datatype.Duration -- una cantidad de tiempo escrita como
 * {@code PnYnMnDTnHnMnS}, con la particularidad de que dos de ellas no siempre se pueden comparar.
 *
 * <h2>Por que no implementa {@code Comparable}</h2>
 *
 * <p>Es la decision de diseño que explica casi toda la clase. Una duracion tiene seis campos, y dos
 * de ellos --anios y meses-- miden en una unidad que <b>no tiene un largo fijo</b>. {@code P1M} son
 * 28 dias si empieza el 1 de febrero de 2023 y 31 si empieza el 1 de marzo. Asi que la pregunta
 * "{@code P1M} es mas larga que {@code P30D}?" no tiene una respuesta: tiene dos, segun cuando.
 *
 * <p>Por eso {@link #compare} devuelve cuatro valores y no tres, y el cuarto,
 * {@link DatatypeConstants#INDETERMINATE}, es una respuesta correcta y no un error. Y por eso esta
 * clase no puede implementar {@code Comparable}: no hay orden total que implementar.
 *
 * <p>La trampa que se cobra sola: <b>{@code !isLongerThan(d)} no quiere decir "es mas corta o
 * igual"</b>. Quiere decir "no es mas larga", que incluye "no se sabe". Un codigo que ordene
 * duraciones con eso da resultados distintos segun el orden en que le lleguen.
 *
 * <p>Y una mas, que sorprende igual: {@link #equals} <b>tampoco</b> es una comparacion campo a
 * campo. Esta definido como {@code compare(otra) == EQUAL}, asi que {@code P1M} y {@code P30D} no
 * son iguales --dan {@code INDETERMINATE}--, pero {@code PT60S} y {@code PT1M} si lo son.
 *
 * <h2>El signo esta afuera de los campos</h2>
 *
 * <p>Una duracion es un signo mas seis magnitudes, y no seis numeros con signo. {@link #getField}
 * devuelve siempre valores no negativos y {@link #getSign} devuelve -1, 0 o 1 aparte. Es lo que dice
 * XML Schema --{@code -P1Y2M} es "menos (un anio y dos meses)", no "menos un anio, mas dos meses"--
 * y hace que {@code P1Y2M} y {@code -P1Y2M} tengan los mismos campos.
 *
 * <h2>Los campos pueden no estar</h2>
 *
 * <p>{@code P1Y} no es lo mismo que {@code P1Y0M0DT0H0M0S}: el primero tiene un solo campo puesto y
 * el resto ausentes. {@link #getField} devuelve null para un campo ausente e {@link #isSet} lo dice
 * sin ambigüedad; {@link #getYears} y sus hermanos, que devuelven {@code int}, contestan
 * {@link DatatypeConstants#FIELD_UNDEFINED} en ese caso. Que campos esten puestos es tambien lo que
 * decide {@link #getXMLSchemaType}.
 *
 * <h2>Que hay aca</h2>
 *
 * <p>La clase entera, con la misma division que el original entre lo abstracto y lo concreto: los
 * diez metodos abstractos son los que dependen de como se guarden los campos, y los demas estan
 * escritos <b>en terminos de esos diez</b> --{@code subtract} es {@code add(rhs.negate())},
 * {@code multiply(int)} es {@code multiply(BigDecimal)}, {@code equals} es {@code compare}--, asi
 * que una subclase que implemente los diez recibe los otros catorce funcionando de verdad.
 *
 * <p>Esta biblioteca trae esa subclase: {@link DatatypeFactory#newInstance()} devuelve una fabrica
 * que produce duraciones reales, con parseo de la forma lexica, aritmetica y comparacion. No hace
 * falta ningun parser de XML para eso --una duracion es una cadena con numeros y letras, no un
 * documento-- asi que aca no hay nada recortado.
 */
public abstract class Duration {

    /**
     * Para las subclases.
     *
     * <p>Publico como en el original, aunque la clase sea abstracta: hay codigo que la extiende
     * desde otro paquete.
     */
    public Duration() {
    }

    /**
     * Cual de los tres tipos de duracion de XML es esta, segun que campos tenga puestos.
     *
     * <p>Las tres formas legales, y no hay mas:
     *
     * <ul>
     *   <li>los seis campos puestos: {@link DatatypeConstants#DURATION}, el {@code xs:duration} de
     *       XML Schema;
     *   <li>dias, horas, minutos y segundos, sin anios ni meses:
     *       {@link DatatypeConstants#DURATION_DAYTIME};
     *   <li>anios y meses, sin nada mas: {@link DatatypeConstants#DURATION_YEARMONTH}.
     * </ul>
     *
     * <p>Las dos ultimas son de XPath 2.0 y existen justamente porque <b>si</b> son ordenables: sin
     * meses todo se cuenta en segundos, y sin dias todo se cuenta en meses. Es
     * {@link DatatypeConstants#DURATION} la que puede dar {@link DatatypeConstants#INDETERMINATE}.
     *
     * <p>Cualquier otra combinacion --{@code P1Y1D}, por ejemplo, con anios y dias pero sin
     * meses-- no es ninguno de los tres tipos y levanta. Es un valor construible pero sin nombre en
     * el sistema de tipos de XML.
     *
     * @return uno de los tres nombres calificados
     * @throws IllegalStateException si los campos puestos no forman ninguno de los tres
     */
    public QName getXMLSchemaType() {
        boolean hasYears = isSet(DatatypeConstants.YEARS);
        boolean hasMonths = isSet(DatatypeConstants.MONTHS);
        boolean hasDays = isSet(DatatypeConstants.DAYS);
        boolean hasHours = isSet(DatatypeConstants.HOURS);
        boolean hasMinutes = isSet(DatatypeConstants.MINUTES);
        boolean hasSeconds = isSet(DatatypeConstants.SECONDS);

        if (hasYears && hasMonths && hasDays && hasHours && hasMinutes && hasSeconds) {
            return DatatypeConstants.DURATION;
        }
        if (!hasYears && !hasMonths && hasDays && hasHours && hasMinutes && hasSeconds) {
            return DatatypeConstants.DURATION_DAYTIME;
        }
        if (hasYears && hasMonths && !hasDays && !hasHours && !hasMinutes && !hasSeconds) {
            return DatatypeConstants.DURATION_YEARMONTH;
        }
        throw new IllegalStateException(
                "javax.xml.datatype.Duration#getXMLSchemaType():"
                        + " this Duration does not match one of the XML Schema date/time datatypes:"
                        + " year set = " + hasYears
                        + " month set = " + hasMonths
                        + " day set = " + hasDays
                        + " hour set = " + hasHours
                        + " minute set = " + hasMinutes
                        + " second set = " + hasSeconds);
    }

    /**
     * El signo de la duracion entera: -1, 0 o 1.
     *
     * <p>Cero solo cuando todos los campos puestos valen cero.
     *
     * @return -1, 0 o 1
     */
    public abstract int getSign();

    /**
     * Los anios, siempre no negativos.
     *
     * @return los anios, o {@link DatatypeConstants#FIELD_UNDEFINED} si el campo no esta
     */
    public int getYears() {
        return valueAsInt(DatatypeConstants.YEARS);
    }

    /**
     * Los meses, siempre no negativos.
     *
     * @return los meses, o {@link DatatypeConstants#FIELD_UNDEFINED} si el campo no esta
     */
    public int getMonths() {
        return valueAsInt(DatatypeConstants.MONTHS);
    }

    /**
     * Los dias, siempre no negativos.
     *
     * @return los dias, o {@link DatatypeConstants#FIELD_UNDEFINED} si el campo no esta
     */
    public int getDays() {
        return valueAsInt(DatatypeConstants.DAYS);
    }

    /**
     * Las horas, siempre no negativas.
     *
     * @return las horas, o {@link DatatypeConstants#FIELD_UNDEFINED} si el campo no esta
     */
    public int getHours() {
        return valueAsInt(DatatypeConstants.HOURS);
    }

    /**
     * Los minutos, siempre no negativos.
     *
     * @return los minutos, o {@link DatatypeConstants#FIELD_UNDEFINED} si el campo no esta
     */
    public int getMinutes() {
        return valueAsInt(DatatypeConstants.MINUTES);
    }

    /**
     * Los segundos <b>enteros</b>: la parte fraccionaria se pierde aca.
     *
     * <p>{@code PT1.5S} da 1. Para no perderla hay que pedir
     * {@code getField(DatatypeConstants.SECONDS)}, que devuelve el {@link BigDecimal} completo.
     *
     * @return los segundos, o {@link DatatypeConstants#FIELD_UNDEFINED} si el campo no esta
     */
    public int getSeconds() {
        return valueAsInt(DatatypeConstants.SECONDS);
    }

    /**
     * Un campo como {@code int}, con {@link DatatypeConstants#FIELD_UNDEFINED} para el ausente.
     *
     * @param campo cual
     * @return el valor entero
     */
    private int valueAsInt(javax.xml.datatype.DatatypeConstants.Field fieldId) {
        Number n = getField(fieldId);
        if (n == null) {
            return DatatypeConstants.FIELD_UNDEFINED;
        }
        return n.intValue();
    }

    /**
     * Cuantos milisegundos dura esta duracion <b>si empieza en este instante</b>.
     *
     * <p>Que haga falta un instante de partida es toda la historia de esta clase en una firma: sin
     * el, la pregunta no tiene respuesta. La cuenta es literal --se copia el calendario, se le suma
     * la duracion, y se restan los dos instantes--, asi que el resultado sale bien tambien cuando
     * hay cambio de horario de verano en el medio.
     *
     * <p>El calendario que se pasa <b>no</b> se toca: se trabaja sobre una copia.
     *
     * <p>La copia se arma con el instante y la zona horaria del original y no con {@code clone()},
     * que es lo que hace el JDK: el {@link Calendar} de esta biblioteca todavia no es
     * {@code Cloneable}. La diferencia se nota en un solo caso --un {@code Calendar} de otra
     * subclase que no sea gregoriana, cuyo tipo la copia pierde--; para todo lo que esta API
     * modela, que es el calendario gregoriano, el resultado es el mismo.
     *
     * @param startInstant el instante de partida; se usan tambien su zona horaria y su calendario
     * @return los milisegundos, con signo
     * @throws NullPointerException si {@code startInstant} es null
     */
    public long getTimeInMillis(Calendar startInstant) {
        Calendar copy = new GregorianCalendar();
        copy.setTimeZone(startInstant.getTimeZone());
        copy.setTimeInMillis(startInstant.getTimeInMillis());
        addTo(copy);
        return copy.getTimeInMillis() - startInstant.getTimeInMillis();
    }

    /**
     * Lo mismo, partiendo de una {@link Date}.
     *
     * <p>Usa un {@link GregorianCalendar} con la zona horaria por omision, porque una {@code Date}
     * no trae ninguna. Si eso importa --y con meses de por medio importa-- conviene la version que
     * toma un {@link Calendar}.
     *
     * @param startInstant el instante de partida
     * @return los milisegundos, con signo
     * @throws NullPointerException si {@code startInstant} es null
     */
    public long getTimeInMillis(Date startInstant) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(startInstant);
        addTo(cal);
        return cal.getTimeInMillis() - startInstant.getTime();
    }

    /**
     * El valor de un campo, o null si el campo no esta puesto.
     *
     * <p>El tipo del resultado depende del campo: {@link BigInteger} para los cinco primeros y
     * {@link BigDecimal} para {@link DatatypeConstants#SECONDS}, que es el unico fraccionario.
     * Siempre no negativo: el signo esta en {@link #getSign}.
     *
     * @param field cual campo
     * @return el valor, o null
     * @throws NullPointerException si {@code field} es null
     */
    public abstract Number getField(javax.xml.datatype.DatatypeConstants.Field field);

    /**
     * Si el campo esta puesto.
     *
     * <p>Distinto de "vale cero": {@code P0Y} tiene los anios puestos en cero, y {@code P1D} los
     * tiene ausentes.
     *
     * @param field cual campo
     * @return true si esta puesto
     * @throws NullPointerException si {@code field} es null
     */
    public abstract boolean isSet(javax.xml.datatype.DatatypeConstants.Field field);

    /**
     * La suma de las dos duraciones.
     *
     * <p>No siempre existe, y el motivo es el de siempre: sumar {@code P1M} y {@code -P30D} daria
     * una duracion cuyo signo depende del mes, y no hay forma de escribir eso. En ese caso levanta.
     *
     * @param rhs la otra; no puede ser null
     * @return la suma
     * @throws IllegalStateException si el resultado tendria campos de los dos signos
     * @throws NullPointerException si {@code rhs} es null
     */
    public abstract Duration add(Duration rhs);

    /**
     * Le suma esta duracion al calendario, en el lugar.
     *
     * <p>El orden importa y esta fijado por la especificacion: primero anios, despues meses, dias,
     * horas, minutos y segundos. Sumar un mes y despues un dia no da lo mismo que al reves cuando se
     * empieza el 31 de enero, asi que un orden fijo es lo unico que hace la operacion reproducible.
     *
     * @param calendar el calendario a modificar; no puede ser null
     * @throws NullPointerException si {@code calendar} es null
     */
    public abstract void addTo(Calendar calendar);

    /**
     * Le suma esta duracion a la fecha, en el lugar.
     *
     * <p>Pasa por un {@link GregorianCalendar} con la zona horaria por omision, con la misma
     * salvedad que {@link #getTimeInMillis(Date)}.
     *
     * @param date la fecha a modificar; no puede ser null
     * @throws NullPointerException si {@code date} es null
     */
    public void addTo(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        addTo(cal);
        date.setTime(cal.getTimeInMillis());
    }

    /**
     * La resta, que es {@code add(rhs.negate())} y nada mas.
     *
     * @param rhs la que se resta; no puede ser null
     * @return la diferencia
     * @throws IllegalStateException si la suma equivalente no se puede representar
     * @throws NullPointerException si {@code rhs} es null
     */
    public Duration subtract(Duration rhs) {
        return add(rhs.negate());
    }

    /**
     * La duracion multiplicada por un entero.
     *
     * @param factor por cuanto
     * @return el producto
     */
    public Duration multiply(int factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    /**
     * La duracion multiplicada por un decimal.
     *
     * <p>La parte fraccionaria que quede en un campo no fraccionario <b>baja al campo siguiente</b>:
     * medio anio son seis meses, medio dia son doce horas. Con una excepcion que sorprende y que es
     * la de siempre: de los meses no se puede bajar a los dias, porque no hay una equivalencia fija.
     * Multiplicar {@code P1M} por {@code 0.5} levanta.
     *
     * @param factor por cuanto; no puede ser null
     * @return el producto
     * @throws IllegalStateException si quedaria una fraccion de mes
     * @throws NullPointerException si {@code factor} es null
     */
    public abstract Duration multiply(BigDecimal factor);

    /**
     * La misma duracion con el signo cambiado; los campos no se tocan.
     *
     * @return la opuesta
     */
    public abstract Duration negate();

    /**
     * La misma duracion con los anios y meses convertidos a dias, usando este calendario de
     * referencia.
     *
     * <p>Es la operacion que <b>saca</b> la ambigüedad: fijado el punto de partida, un mes ya tiene
     * una cantidad de dias, asi que el resultado es una duracion sin meses --y por lo tanto
     * comparable con cualquier otra igual--.
     *
     * @param startTimeInstant el punto de referencia; no puede ser null
     * @return la duracion normalizada
     * @throws NullPointerException si {@code startTimeInstant} es null
     */
    public abstract Duration normalizeWith(Calendar startTimeInstant);

    /**
     * Compara las dos duraciones, y puede contestar que no se pueden comparar.
     *
     * <p>Los cuatro resultados son {@link DatatypeConstants#LESSER},
     * {@link DatatypeConstants#EQUAL}, {@link DatatypeConstants#GREATER} y
     * {@link DatatypeConstants#INDETERMINATE}. El ultimo no es un error: ver el encabezado de la
     * clase.
     *
     * <p>La definicion de la especificacion es indirecta y vale conocerla, porque explica por que
     * hay casos indeterminados y otros que no: se le suman las dos duraciones a cuatro instantes
     * elegidos --1696-09-01, 1697-02-01, 1903-03-01 y 1903-07-01, que cubren todas las
     * combinaciones de largo de febrero y de mes de 30 y 31 dias-- y, si las cuatro comparaciones
     * coinciden, ese es el resultado; si no, es indeterminado.
     *
     * @param duration la otra; no puede ser null
     * @return uno de los cuatro
     * @throws NullPointerException si {@code duration} es null
     */
    public abstract int compare(Duration duration);

    /**
     * Si esta duracion es estrictamente mas larga que la otra.
     *
     * <p><b>Cuidado</b>: false no quiere decir "es mas corta o igual". Con
     * {@link DatatypeConstants#INDETERMINATE} las dos direcciones dan false a la vez.
     *
     * @param duration la otra; no puede ser null
     * @return true solo si la comparacion dio {@link DatatypeConstants#GREATER}
     * @throws NullPointerException si {@code duration} es null
     */
    public boolean isLongerThan(Duration duration) {
        return compare(duration) == DatatypeConstants.GREATER;
    }

    /**
     * Si esta duracion es estrictamente mas corta que la otra, con la misma salvedad.
     *
     * @param duration la otra; no puede ser null
     * @return true solo si la comparacion dio {@link DatatypeConstants#LESSER}
     * @throws NullPointerException si {@code duration} es null
     */
    public boolean isShorterThan(Duration duration) {
        return compare(duration) == DatatypeConstants.LESSER;
    }

    /**
     * Dos duraciones son iguales si {@link #compare} dice {@link DatatypeConstants#EQUAL}.
     *
     * <p>No es una comparacion campo a campo, y las dos consecuencias van en direcciones opuestas:
     * {@code PT60S} y {@code PT1M} <b>son</b> iguales aunque tengan campos distintos, y {@code P1M}
     * y {@code P30D} <b>no</b> lo son porque la comparacion da indeterminado.
     *
     * <p>Lo que queda incomodo, y esta asi en el original: {@code equals} no es transitivo en
     * presencia de duraciones indeterminadas, asi que un {@code HashSet} de duraciones con meses no
     * se comporta como uno espera. Es el precio de que el tipo no tenga orden total.
     *
     * @param duration el otro objeto
     * @return true si es una {@code Duration} que compara igual
     */
    public boolean equals(Object duration) {
        if (duration == this) {
            return true;
        }
        if (!(duration instanceof Duration)) {
            return false;
        }
        return compare((Duration) duration) == DatatypeConstants.EQUAL;
    }

    /**
     * El hash, que la subclase tiene que dar.
     *
     * <p>Es abstracto justamente porque {@link #equals} esta definido en terminos de
     * {@link #compare}: no hay una formula sobre los campos que sea coherente con eso, y la
     * subclase es la unica que sabe como normalizar antes de hashear.
     *
     * @return el hash
     */
    public abstract int hashCode();

    /**
     * La duracion en la forma lexica de XML Schema: {@code PnYnMnDTnHnMnS}.
     *
     * <p>Solo salen los campos puestos, la {@code T} aparece unicamente si hay algun campo de
     * tiempo, y el signo va adelante de la {@code P}. Los segundos se escriben sin ceros de mas.
     *
     * @return la representacion lexica
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (getSign() < 0) {
            buf.append('-');
        }
        buf.append('P');

        BigInteger years = (BigInteger) getField(DatatypeConstants.YEARS);
        if (years != null) {
            buf.append(years).append('Y');
        }
        BigInteger months = (BigInteger) getField(DatatypeConstants.MONTHS);
        if (months != null) {
            buf.append(months).append('M');
        }
        BigInteger days = (BigInteger) getField(DatatypeConstants.DAYS);
        if (days != null) {
            buf.append(days).append('D');
        }

        BigInteger hours = (BigInteger) getField(DatatypeConstants.HOURS);
        BigInteger minutes = (BigInteger) getField(DatatypeConstants.MINUTES);
        BigDecimal seconds = (BigDecimal) getField(DatatypeConstants.SECONDS);
        if (hours != null || minutes != null || seconds != null) {
            buf.append('T');
            if (hours != null) {
                buf.append(hours).append('H');
            }
            if (minutes != null) {
                buf.append(minutes).append('M');
            }
            if (seconds != null) {
                buf.append(asText(seconds)).append('S');
            }
        }
        return buf.toString();
    }

    /**
     * Un {@link BigDecimal} no negativo escrito sin notacion cientifica.
     *
     * <p>Hace falta escribirlo a mano porque {@code toString()} de {@code BigDecimal} puede sacar
     * un exponente --{@code 1E+2}-- y eso no es una forma lexica valida de XML Schema. Se arma
     * insertando el punto en el valor sin escala, que es la definicion misma de la escala.
     *
     * @param bd el numero, que aca siempre viene no negativo
     * @return el texto
     */
    private String asText(BigDecimal bd) {
        String ints = bd.unscaledValue().toString();
        int scaleFactor = bd.scale();
        if (scaleFactor == 0) {
            return ints;
        }
        int cut = ints.length() - scaleFactor;
        if (cut == 0) {
            return "0." + ints;
        }
        if (cut > 0) {
            StringBuilder buf = new StringBuilder(ints);
            buf.insert(cut, '.');
            return buf.toString();
        }
        StringBuilder buf = new StringBuilder();
        buf.append("0.");
        for (int i = 0; i < -cut; i++) {
            buf.append('0');
        }
        buf.append(ints);
        return buf.toString();
    }
}
