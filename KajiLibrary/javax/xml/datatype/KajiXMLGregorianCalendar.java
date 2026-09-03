package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.namespace.QName;

/**
 * La {@link XMLGregorianCalendar} concreta de esta biblioteca.
 *
 * <p>Interna: no es API y quien la use la ve como {@code XMLGregorianCalendar}. Hace de verdad las
 * tres cosas que hacen falta --parsear y escribir las ocho formas lexicas de XML Schema, comparar
 * con normalizacion a UTC, y sumar duraciones-- sin ningun parser de XML de por medio: una fecha
 * lexica es una cadena con digitos, guiones y dos puntos.
 *
 * <h2>Los ocho tipos, y como se distinguen</h2>
 *
 * <p>La clase no tiene un campo que diga de que tipo es: el tipo <b>es</b> el conjunto de campos que
 * estan puestos, y {@link #getXMLSchemaType} lo deduce. Un {@code gMonth} es una instancia con el
 * mes puesto y todo lo demas en {@link DatatypeConstants#FIELD_UNDEFINED}.
 *
 * <p>Es lo que permite que los ocho tipos entren en una sola clase, y tambien lo que hace que
 * armarla a mano con los setters pueda dejarla en un estado que no es ninguno de los ocho. Ahi
 * {@code getXMLSchemaType} y {@link #toXMLFormat} levantan {@link IllegalStateException}, que es lo
 * unico honesto: no hay forma lexica que escribir.
 *
 * <h2>La comparacion y la zona horaria</h2>
 *
 * <p>Dos fechas con zona se llevan a UTC y se comparan campo a campo. Dos sin zona se comparan como
 * estan. Una con y una sin es el caso interesante: la que no tiene zona podria estar en cualquier
 * lugar del intervalo de 28 horas que va de {@code +14:00} a {@code -14:00}, asi que se la compara
 * contra los dos extremos y, si los dos dan lo mismo, ese es el resultado; si no, es
 * {@link DatatypeConstants#INDETERMINATE}.
 *
 * <p>Por eso {@code 2024-05-25T12:00:00} contra {@code 2024-05-25T12:00:00Z} da indeterminado --las
 * 12 en Auckland ya pasaron y las 12 en Honolulu no llegaron-- y contra {@code 2030-...} da menor,
 * porque seis anios son mas que 28 horas.
 */
final class KajiXMLGregorianCalendar extends XMLGregorianCalendar {

    /** Un minuto en milisegundos. */
    private static final long MS_PER_MINUTE = 60000L;

    /** Los miles de millones del anio, o null. Siempre multiplo de mil millones. */
    private BigInteger eon;

    /** El anio sin el eon, o {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int yearValue;

    /** El mes de 1 a 12, o {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int monthValue;

    /** El dia de 1 a 31, o {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int dayValue;

    /** La hora de 0 a 24, o {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int hourValue;

    /** El minuto de 0 a 59, o {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int minuteValue;

    /** El segundo de 0 a 60, o {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int secondValue;

    /** La fraccion de segundo, de 0 inclusive a 1 exclusive, o null. */
    private BigDecimal fractionValue;

    /** La zona en minutos, de -840 a 840, o {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int timezoneValue;

    /** Los valores con que la dejo {@link #reset}: los del momento de construirla. */
    private final int[] initial;

    /** El eon inicial, aparte porque no es un {@code int}. */
    private final BigInteger initialEon;

    /** La fraccion inicial, idem. */
    private final BigDecimal initialFraction;

    /** Una fecha con todos los campos sin definir. */
    KajiXMLGregorianCalendar() {
        clearAll();
        this.initial = snapshot();
        this.initialEon = eon;
        this.initialFraction = fractionValue;
    }

    /**
     * Campo por campo, validando.
     *
     * @throws IllegalArgumentException si algun campo esta fuera de rango o la fecha no existe
     */
    KajiXMLGregorianCalendar(BigInteger yearValue, int monthValue, int dayValue, int hourValue, int minuteValue, int secondValue,
            BigDecimal fractionValue, int timezoneValue) {
        clearAll();
        setYear(yearValue);
        setMonth(monthValue);
        setDay(dayValue);
        setHour(hourValue);
        setMinute(minuteValue);
        setSecond(secondValue);
        setFractionalSecond(fractionValue);
        setTimezone(timezoneValue);
        if (!isValid()) {
            throw new IllegalArgumentException(
                    "Year = " + this.yearValue + ", Month = " + this.monthValue + ", Day = " + this.dayValue
                            + ", Hour = " + this.hourValue + ", Minute = " + this.minuteValue
                            + ", Second = " + this.secondValue
                            + ", fractionalSecond = " + this.fractionValue
                            + ", Timezone = " + this.timezoneValue
                            + " , is not a valid representation of an XML Gregorian Calendar"
                            + " value.");
        }
        this.initial = snapshot();
        this.initialEon = this.eon;
        this.initialFraction = this.fractionValue;
    }

    /** Copia de otro calendario gregoriano; todos los campos quedan definidos. */
    KajiXMLGregorianCalendar(GregorianCalendar cal) {
        clearAll();
        if (cal == null) {
            throw new NullPointerException("cal is null");
        }
        setYear(cal.get(Calendar.YEAR));
        setMonth(cal.get(Calendar.MONTH) - Calendar.JANUARY + DatatypeConstants.JANUARY);
        setDay(cal.get(Calendar.DAY_OF_MONTH));
        setHour(cal.get(Calendar.HOUR_OF_DAY));
        setMinute(cal.get(Calendar.MINUTE));
        setSecond(cal.get(Calendar.SECOND));
        setMillisecond(cal.get(Calendar.MILLISECOND));
        // Un `GregorianCalendar` siempre tiene zona, asi que el resultado siempre es un dateTime
        // completo. El desplazamiento se toma del calendario y no de la zona, para que un momento
        // en horario de verano quede con el desplazamiento que de verdad tenia.
        int offsetMs = cal.getTimeZone().getOffset(cal.getTimeInMillis());
        setTimezone((int) (((long) offsetMs) / MS_PER_MINUTE));
        this.initial = snapshot();
        this.initialEon = this.eon;
        this.initialFraction = this.fractionValue;
    }

    /** Deja todos los campos sin definir, sin tocar los valores iniciales. */
    private void clearAll() {
        eon = null;
        yearValue = DatatypeConstants.FIELD_UNDEFINED;
        monthValue = DatatypeConstants.FIELD_UNDEFINED;
        dayValue = DatatypeConstants.FIELD_UNDEFINED;
        hourValue = DatatypeConstants.FIELD_UNDEFINED;
        minuteValue = DatatypeConstants.FIELD_UNDEFINED;
        secondValue = DatatypeConstants.FIELD_UNDEFINED;
        fractionValue = null;
        timezoneValue = DatatypeConstants.FIELD_UNDEFINED;
    }

    /** Los siete campos enteros, para {@link #reset}. */
    private int[] snapshot() {
        return new int[] {yearValue, monthValue, dayValue, hourValue, minuteValue, secondValue, timezoneValue};
    }

    // ---- parseo de las ocho formas lexicas ----------------------------------------------------

    /**
     * A partir de una de las ocho formas lexicas de XML Schema.
     *
     * <p>Cual es se decide por la forma y no por un parametro, que es como funciona el tipo: dos
     * guiones al principio anuncian que no hay anio, tres que tampoco hay mes, y una {@code T}
     * separa la fecha de la hora. Una cadena que empieza con digitos y tiene dos puntos antes que
     * guiones es una hora suelta.
     *
     * @param lexica la forma lexica; no puede ser null
     * @return la fecha
     * @throws IllegalArgumentException si no es ninguna de las ocho formas
     * @throws NullPointerException si es null
     */
    static KajiXMLGregorianCalendar parse(String lexical) {
        if (lexical == null) {
            throw new NullPointerException("lexicalRepresentation is null");
        }
        Parser a = new Parser(lexical);
        KajiXMLGregorianCalendar c = new KajiXMLGregorianCalendar();
        try {
            if (a.peek("---")) {
                a.consume("---");
                c.setDay(a.fixedInt(2));
                a.optionalTimezone(c);
            } else if (a.peek("--")) {
                a.consume("--");
                c.setMonth(a.fixedInt(2));
                if (a.peek("-")) {
                    a.consume("-");
                    c.setDay(a.fixedInt(2));
                }
                a.optionalTimezone(c);
            } else if (a.hasLooseTime()) {
                a.readTime(c);
                a.optionalTimezone(c);
            } else {
                c.setYear(a.yearValue());
                if (a.peek("-")) {
                    a.consume("-");
                    c.setMonth(a.fixedInt(2));
                    if (a.peek("-")) {
                        a.consume("-");
                        c.setDay(a.fixedInt(2));
                        if (a.peek("T")) {
                            a.consume("T");
                            a.readTime(c);
                        }
                    }
                }
                a.optionalTimezone(c);
            }
            a.requireEnd();
            if (c.nextDay) {
                // `24:00:00` es medianoche del dia siguiente: recien aca, con la fecha completa,
                // se puede correr el dia.
                c.nextDay = false;
                c.shiftDays(1);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "\"" + lexical + "\" is not a valid representation of an XML Gregorian Calendar"
                            + " value: " + e.getMessage());
        }
        if (!c.isValid()) {
            throw new IllegalArgumentException(
                    "\"" + lexical + "\" is not a valid representation of an XML Gregorian Calendar"
                            + " value.");
        }
        return c;
    }

    /** Un cursor sobre la cadena lexica; existe solo para que el parseo se lea de arriba abajo. */
    private static final class Parser {

        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
            this.i = 0;
        }

        boolean peek(String p) {
            return s.startsWith(p, i);
        }

        void consume(String p) {
            if (!peek(p)) {
                throw new IllegalArgumentException("expected '" + p + "'");
            }
            i += p.length();
        }

        /** Si lo que viene es una hora suelta: dos digitos y dos puntos. */
        boolean hasLooseTime() {
            return i + 2 < s.length() && isDigit(s.charAt(i)) && isDigit(s.charAt(i + 1))
                    && s.charAt(i + 2) == ':';
        }

        /** El anio, que puede tener signo y mas de cuatro digitos. */
        BigInteger yearValue() {
            int from = i;
            if (i < s.length() && s.charAt(i) == '-') {
                i++;
            }
            int digits = 0;
            while (i < s.length() && isDigit(s.charAt(i))) {
                i++;
                digits++;
            }
            if (digits < 4) {
                throw new IllegalArgumentException("the year needs at least four digits");
            }
            return new BigInteger(s.substring(from, i));
        }

        /** Un entero de exactamente {@code n} digitos. */
        int fixedInt(int n) {
            if (i + n > s.length()) {
                throw new IllegalArgumentException("expected " + n + " digits");
            }
            int v = 0;
            for (int k = 0; k < n; k++) {
                char c = s.charAt(i + k);
                if (!isDigit(c)) {
                    throw new IllegalArgumentException("expected " + n + " digits");
                }
                v = v * 10 + (c - '0');
            }
            i += n;
            return v;
        }

        /** {@code hh:mm:ss} con fraccion opcional. */
        void readTime(KajiXMLGregorianCalendar c) {
            int h = fixedInt(2);
            consume(":");
            c.setMinute(fixedInt(2));
            consume(":");
            c.setSecond(fixedInt(2));
            if (peek(".")) {
                int from = i;
                i++;
                int digits = 0;
                while (i < s.length() && isDigit(s.charAt(i))) {
                    i++;
                    digits++;
                }
                if (digits == 0) {
                    throw new IllegalArgumentException("the fraction needs at least one digit");
                }
                c.setFractionalSecond(new BigDecimal("0" + s.substring(from, i)));
            }
            if (h == 24) {
                // `24:00:00` es medianoche del dia siguiente y la unica forma en que la hora puede
                // valer 24. Se normaliza aca en vez de guardarla, porque un 24 guardado se filtra
                // despues a la comparacion y a `toXMLFormat`.
                if (c.minuteValue != 0 || c.secondValue != 0
                        || (c.fractionValue != null && c.fractionValue.signum() != 0)) {
                    throw new IllegalArgumentException("hour 24 is only valid as 24:00:00");
                }
                c.setHour(0);
                c.nextDay = true;
            } else {
                c.setHour(h);
            }
        }

        /** {@code Z} o {@code (+|-)hh:mm}, si hay. */
        void optionalTimezone(KajiXMLGregorianCalendar c) {
            if (i >= s.length()) {
                return;
            }
            char ch = s.charAt(i);
            if (ch == 'Z') {
                i++;
                c.setTimezone(0);
                return;
            }
            if (ch != '+' && ch != '-') {
                return;
            }
            int sign = ch == '-' ? -1 : 1;
            i++;
            int h = fixedInt(2);
            consume(":");
            int m = fixedInt(2);
            c.setTimezone(sign * (h * 60 + m));
        }

        void requireEnd() {
            if (i != s.length()) {
                throw new IllegalArgumentException("trailing characters: \"" + s.substring(i) + "\"");
            }
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }
    }

    /** Marca de {@code 24:00:00}: hay que correr un dia despues de tener la fecha completa. */
    private transient boolean nextDay;

    // ---- setters -------------------------------------------------------------------------------

    /** {@inheritDoc} */
    public void clear() {
        clearAll();
    }

    /** {@inheritDoc} */
    public void reset() {
        yearValue = initial[0];
        monthValue = initial[1];
        dayValue = initial[2];
        hourValue = initial[3];
        minuteValue = initial[4];
        secondValue = initial[5];
        timezoneValue = initial[6];
        eon = initialEon;
        fractionValue = initialFraction;
    }

    /** {@inheritDoc} */
    public void setYear(BigInteger year) {
        if (year == null) {
            eon = null;
            yearValue = DatatypeConstants.FIELD_UNDEFINED;
            return;
        }
        // El anio se parte en dos: lo que entra en un `int` --el resto de dividir por mil
        // millones-- y el resto, que se guarda aparte. Es como lo hace el original, y lo que
        // permite que `getYear()` siga siendo un `int` sin ponerle tope al anio.
        BigInteger ONE_BILLION = BigInteger.valueOf(1000000000L);
        BigInteger[] parts = year.divideAndRemainder(ONE_BILLION);
        BigInteger high = parts[0];
        BigInteger low = parts[1];
        if (high.signum() == 0) {
            eon = null;
        } else {
            eon = high.multiply(ONE_BILLION);
        }
        yearValue = low.intValue();
    }

    /** {@inheritDoc} */
    public void setYear(int year) {
        if (year == DatatypeConstants.FIELD_UNDEFINED) {
            eon = null;
            yearValue = DatatypeConstants.FIELD_UNDEFINED;
            return;
        }
        eon = null;
        yearValue = year;
    }

    /** {@inheritDoc} */
    public void setMonth(int month) {
        if (month != DatatypeConstants.FIELD_UNDEFINED
                && (month < DatatypeConstants.JANUARY || month > DatatypeConstants.DECEMBER)) {
            throw new IllegalArgumentException("invalid month: " + month);
        }
        monthValue = month;
    }

    /** {@inheritDoc} */
    public void setDay(int day) {
        if (day != DatatypeConstants.FIELD_UNDEFINED && (day < 1 || day > 31)) {
            throw new IllegalArgumentException("invalid day: " + day);
        }
        dayValue = day;
    }

    /** {@inheritDoc} */
    public void setTimezone(int offset) {
        if (offset != DatatypeConstants.FIELD_UNDEFINED
                && (offset < DatatypeConstants.MAX_TIMEZONE_OFFSET
                        || offset > DatatypeConstants.MIN_TIMEZONE_OFFSET)) {
            // Los nombres de las constantes estan al reves de los numeros; ver DatatypeConstants.
            throw new IllegalArgumentException("invalid timezone: " + offset);
        }
        timezoneValue = offset;
    }

    /** {@inheritDoc} */
    public void setHour(int hour) {
        if (hour != DatatypeConstants.FIELD_UNDEFINED && (hour < 0 || hour > 23)) {
            throw new IllegalArgumentException("invalid hour: " + hour);
        }
        hourValue = hour;
    }

    /** {@inheritDoc} */
    public void setMinute(int minute) {
        if (minute != DatatypeConstants.FIELD_UNDEFINED && (minute < 0 || minute > 59)) {
            throw new IllegalArgumentException("invalid minute: " + minute);
        }
        minuteValue = minute;
    }

    /** {@inheritDoc} */
    public void setSecond(int second) {
        // Sesenta y no cincuenta y nueve: XML Schema deja lugar al segundo intercalar.
        if (second != DatatypeConstants.FIELD_UNDEFINED && (second < 0 || second > 60)) {
            throw new IllegalArgumentException("invalid second: " + second);
        }
        secondValue = second;
    }

    /** {@inheritDoc} */
    public void setMillisecond(int millisecond) {
        if (millisecond == DatatypeConstants.FIELD_UNDEFINED) {
            fractionValue = null;
            return;
        }
        if (millisecond < 0 || millisecond > 999) {
            throw new IllegalArgumentException("invalid millisecond: " + millisecond);
        }
        fractionValue = BigDecimal.valueOf((long) millisecond, 3);
    }

    /** {@inheritDoc} */
    public void setFractionalSecond(BigDecimal fractional) {
        if (fractional != null) {
            if (fractional.signum() < 0 || fractional.compareTo(BigDecimal.ONE) >= 0) {
                throw new IllegalArgumentException(
                        "invalid fractional second: " + fractional
                                + ", must be in [0, 1)");
            }
        }
        fractionValue = fractional;
    }

    // ---- getters -------------------------------------------------------------------------------

    /** {@inheritDoc} */
    public BigInteger getEon() {
        return eon;
    }

    /** {@inheritDoc} */
    public int getYear() {
        return yearValue;
    }

    /** {@inheritDoc} */
    public BigInteger getEonAndYear() {
        if (yearValue == DatatypeConstants.FIELD_UNDEFINED) {
            return null;
        }
        BigInteger v = BigInteger.valueOf((long) yearValue);
        return eon == null ? v : eon.add(v);
    }

    /** {@inheritDoc} */
    public int getMonth() {
        return monthValue;
    }

    /** {@inheritDoc} */
    public int getDay() {
        return dayValue;
    }

    /** {@inheritDoc} */
    public int getTimezone() {
        return timezoneValue;
    }

    /** {@inheritDoc} */
    public int getHour() {
        return hourValue;
    }

    /** {@inheritDoc} */
    public int getMinute() {
        return minuteValue;
    }

    /** {@inheritDoc} */
    public int getSecond() {
        return secondValue;
    }

    /** {@inheritDoc} */
    public BigDecimal getFractionalSecond() {
        return fractionValue;
    }

    // ---- comparacion ---------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Ver el encabezado de la clase para el caso de una con zona y una sin.
     */
    public int compare(XMLGregorianCalendar rhs) {
        if (rhs == null) {
            throw new NullPointerException("rhs is null");
        }
        boolean thisHasTimezone = timezoneValue != DatatypeConstants.FIELD_UNDEFINED;
        boolean otherHasTimezone = rhs.getTimezone() != DatatypeConstants.FIELD_UNDEFINED;

        if (thisHasTimezone == otherHasTimezone) {
            XMLGregorianCalendar a = thisHasTimezone ? normalize() : this;
            XMLGregorianCalendar b = otherHasTimezone ? rhs.normalize() : rhs;
            return fieldByField(a, b);
        }

        // Una tiene zona y la otra no: la que no tiene puede estar en cualquier punto del
        // intervalo de 28 horas que va de +14:00 a -14:00. Se la compara contra los dos extremos.
        XMLGregorianCalendar withTimezone = thisHasTimezone ? this : rhs;
        XMLGregorianCalendar withoutTimezone = thisHasTimezone ? rhs : this;
        XMLGregorianCalendar fixed = withTimezone.normalize();

        XMLGregorianCalendar boundA = withOffset(withoutTimezone, 840).normalize();
        XMLGregorianCalendar boundB = withOffset(withoutTimezone, -840).normalize();

        int c1;
        int c2;
        if (thisHasTimezone) {
            c1 = fieldByField(fixed, boundA);
            c2 = fieldByField(fixed, boundB);
        } else {
            c1 = fieldByField(boundA, fixed);
            c2 = fieldByField(boundB, fixed);
        }
        if (c1 == c2) {
            return c1;
        }
        return DatatypeConstants.INDETERMINATE;
    }

    /** Una copia de {@code c} con la zona puesta en {@code offset}. */
    private static XMLGregorianCalendar withOffset(XMLGregorianCalendar c, int offset) {
        XMLGregorianCalendar copy = (XMLGregorianCalendar) c.clone();
        copy.setTimezone(offset);
        return copy;
    }

    /**
     * Compara campo a campo dos fechas ya normalizadas.
     *
     * <p>Un campo que este en una y no en la otra da {@link DatatypeConstants#INDETERMINATE}: no es
     * que sean distintas, es que no son del mismo tipo y no hay orden entre un {@code gYear} y un
     * {@code gMonth}.
     */
    private static int fieldByField(XMLGregorianCalendar a, XMLGregorianCalendar b) {
        BigInteger yearA = a.getEonAndYear();
        BigInteger yearB = b.getEonAndYear();
        if ((yearA == null) != (yearB == null)) {
            return DatatypeConstants.INDETERMINATE;
        }
        if (yearA != null) {
            int c = yearA.compareTo(yearB);
            if (c != 0) {
                return c < 0 ? DatatypeConstants.LESSER : DatatypeConstants.GREATER;
            }
        }
        int c = compareInt(a.getMonth(), b.getMonth());
        if (c != DatatypeConstants.EQUAL) {
            return c;
        }
        c = compareInt(a.getDay(), b.getDay());
        if (c != DatatypeConstants.EQUAL) {
            return c;
        }
        c = compareInt(a.getHour(), b.getHour());
        if (c != DatatypeConstants.EQUAL) {
            return c;
        }
        c = compareInt(a.getMinute(), b.getMinute());
        if (c != DatatypeConstants.EQUAL) {
            return c;
        }
        c = compareInt(a.getSecond(), b.getSecond());
        if (c != DatatypeConstants.EQUAL) {
            return c;
        }
        BigDecimal fa = a.getFractionalSecond();
        BigDecimal fb = b.getFractionalSecond();
        BigDecimal ca = fa == null ? BigDecimal.ZERO : fa;
        BigDecimal cb = fb == null ? BigDecimal.ZERO : fb;
        int cf = ca.compareTo(cb);
        if (cf < 0) {
            return DatatypeConstants.LESSER;
        }
        if (cf > 0) {
            return DatatypeConstants.GREATER;
        }
        return DatatypeConstants.EQUAL;
    }

    /** Compara dos campos enteros; uno definido y el otro no da indeterminado. */
    private static int compareInt(int a, int b) {
        boolean da = a != DatatypeConstants.FIELD_UNDEFINED;
        boolean db = b != DatatypeConstants.FIELD_UNDEFINED;
        if (da != db) {
            return DatatypeConstants.INDETERMINATE;
        }
        if (!da) {
            return DatatypeConstants.EQUAL;
        }
        if (a < b) {
            return DatatypeConstants.LESSER;
        }
        if (a > b) {
            return DatatypeConstants.GREATER;
        }
        return DatatypeConstants.EQUAL;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sin zona no hay nada que normalizar y se devuelve una copia: suponerle UTC seria inventar
     * el dato que falta, que es justamente el que hace indeterminada la comparacion.
     */
    public XMLGregorianCalendar normalize() {
        if (timezoneValue == DatatypeConstants.FIELD_UNDEFINED) {
            return (XMLGregorianCalendar) clone();
        }
        KajiXMLGregorianCalendar c = (KajiXMLGregorianCalendar) clone();
        if (timezoneValue != 0) {
            // Para ir a UTC se le resta el desplazamiento a la hora local. Se hace sobre los
            // campos y no sobre un instante porque la fecha puede no tener anio: un `gMonthDay`
            // con zona no corresponde a ningun instante y sin embargo se normaliza igual.
            c.shiftMinutes(-timezoneValue);
        }
        c.timezoneValue = 0;
        return c;
    }

    /** Corre la fecha tantos minutos, arrastrando entre los campos que esten definidos. */
    private void shiftMinutes(int minutes) {
        if (minutes == 0 || hourValue == DatatypeConstants.FIELD_UNDEFINED) {
            return;
        }
        int total = hourValue * 60 + (minuteValue == DatatypeConstants.FIELD_UNDEFINED ? 0 : minuteValue) + minutes;
        int elapsedDays = Math.floorDiv(total, 1440);
        int withinDay = Math.floorMod(total, 1440);
        hourValue = withinDay / 60;
        if (minuteValue != DatatypeConstants.FIELD_UNDEFINED) {
            minuteValue = withinDay % 60;
        }
        if (elapsedDays != 0) {
            shiftDays(elapsedDays);
        }
    }

    /** Corre la fecha tantos dias; sin anio y sin mes no hay a donde arrastrar y se deja igual. */
    private void shiftDays(int days) {
        if (days == 0) {
            return;
        }
        if (yearValue == DatatypeConstants.FIELD_UNDEFINED || monthValue == DatatypeConstants.FIELD_UNDEFINED
                || dayValue == DatatypeConstants.FIELD_UNDEFINED) {
            // Un `xs:time` con zona no tiene fecha que correr: la hora da la vuelta y ya. Es lo que
            // hace la especificacion, que define la normalizacion de `time` modulo 24 horas.
            return;
        }
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(yearValue, Calendar.JANUARY + monthValue - DatatypeConstants.JANUARY, dayValue);
        cal.add(Calendar.DAY_OF_MONTH, days);
        setYearFromCalendar(cal);
        monthValue = cal.get(Calendar.MONTH) - Calendar.JANUARY + DatatypeConstants.JANUARY;
        dayValue = cal.get(Calendar.DAY_OF_MONTH);
    }

    /** El anio del calendario, conservando el eon que ya hubiera. */
    private void setYearFromCalendar(Calendar cal) {
        int fresh = cal.get(Calendar.YEAR);
        if (cal.get(Calendar.ERA) == GregorianCalendar.BC) {
            fresh = 1 - fresh;
        }
        yearValue = fresh;
    }

    // ---- forma lexica ---------------------------------------------------------------------------

    /** {@inheritDoc} */
    public String toXMLFormat() {
        QName type = getXMLSchemaType();
        StringBuilder b = new StringBuilder();
        if (type == DatatypeConstants.DATETIME) {
            writeDate(b);
            b.append('T');
            writeTime(b);
        } else if (type == DatatypeConstants.DATE) {
            writeDate(b);
        } else if (type == DatatypeConstants.TIME) {
            writeTime(b);
        } else if (type == DatatypeConstants.GYEARMONTH) {
            writeYear(b);
            b.append('-');
            twoDigits(b, monthValue);
        } else if (type == DatatypeConstants.GMONTHDAY) {
            b.append("--");
            twoDigits(b, monthValue);
            b.append('-');
            twoDigits(b, dayValue);
        } else if (type == DatatypeConstants.GYEAR) {
            writeYear(b);
        } else if (type == DatatypeConstants.GMONTH) {
            b.append("--");
            twoDigits(b, monthValue);
        } else {
            b.append("---");
            twoDigits(b, dayValue);
        }
        writeTimezone(b);
        return b.toString();
    }

    private void writeDate(StringBuilder b) {
        writeYear(b);
        b.append('-');
        twoDigits(b, monthValue);
        b.append('-');
        twoDigits(b, dayValue);
    }

    private void writeTime(StringBuilder b) {
        twoDigits(b, hourValue);
        b.append(':');
        twoDigits(b, minuteValue);
        b.append(':');
        twoDigits(b, secondValue);
        if (fractionValue != null && fractionValue.signum() != 0) {
            // La fraccion se escribe sin el cero de adelante: `.500`, no `0.500`.
            String t = fractionValue.toPlainString();
            b.append(t.substring(t.indexOf('.')));
        }
    }

    /** El anio con al menos cuatro digitos, y el signo adelante si es negativo. */
    private void writeYear(StringBuilder b) {
        BigInteger full = getEonAndYear();
        boolean negative = full.signum() < 0;
        String digits = full.abs().toString();
        if (negative) {
            b.append('-');
        }
        for (int i = digits.length(); i < 4; i++) {
            b.append('0');
        }
        b.append(digits);
    }

    private void writeTimezone(StringBuilder b) {
        if (timezoneValue == DatatypeConstants.FIELD_UNDEFINED) {
            return;
        }
        if (timezoneValue == 0) {
            b.append('Z');
            return;
        }
        int v = timezoneValue;
        if (v < 0) {
            b.append('-');
            v = -v;
        } else {
            b.append('+');
        }
        twoDigits(b, v / 60);
        b.append(':');
        twoDigits(b, v % 60);
    }

    private static void twoDigits(StringBuilder b, int v) {
        if (v < 10) {
            b.append('0');
        }
        b.append(v);
    }

    /** {@inheritDoc} */
    public QName getXMLSchemaType() {
        boolean hasYear = yearValue != DatatypeConstants.FIELD_UNDEFINED;
        boolean hasMonth = monthValue != DatatypeConstants.FIELD_UNDEFINED;
        boolean hasDay = dayValue != DatatypeConstants.FIELD_UNDEFINED;
        boolean hasHour = hourValue != DatatypeConstants.FIELD_UNDEFINED;

        if (hasYear && hasMonth && hasDay && hasHour) {
            return DatatypeConstants.DATETIME;
        }
        if (hasYear && hasMonth && hasDay) {
            return DatatypeConstants.DATE;
        }
        if (!hasYear && !hasMonth && !hasDay && hasHour) {
            return DatatypeConstants.TIME;
        }
        if (hasYear && hasMonth && !hasDay && !hasHour) {
            return DatatypeConstants.GYEARMONTH;
        }
        if (!hasYear && hasMonth && hasDay && !hasHour) {
            return DatatypeConstants.GMONTHDAY;
        }
        if (hasYear && !hasMonth && !hasDay && !hasHour) {
            return DatatypeConstants.GYEAR;
        }
        if (!hasYear && hasMonth && !hasDay && !hasHour) {
            return DatatypeConstants.GMONTH;
        }
        if (!hasYear && !hasMonth && hasDay && !hasHour) {
            return DatatypeConstants.GDAY;
        }
        throw new IllegalStateException(
                "javax.xml.datatype.XMLGregorianCalendar#getXMLSchemaType():"
                        + " this XMLGregorianCalendar does not match one of the eight XML Schema"
                        + " date/time datatypes: year set = " + hasYear
                        + " month set = " + hasMonth
                        + " day set = " + hasDay
                        + " time set = " + hasHour);
    }

    // ---- validez y aritmetica ------------------------------------------------------------------

    /** {@inheritDoc} */
    public boolean isValid() {
        // El anio cero no existe en XML Schema 1.0 y si en 1.1; se sigue a 1.0, que es lo que hace
        // el original: `0000` no es una fecha.
        if (yearValue == 0 && eon == null) {
            return false;
        }
        if (monthValue != DatatypeConstants.FIELD_UNDEFINED
                && (monthValue < DatatypeConstants.JANUARY || monthValue > DatatypeConstants.DECEMBER)) {
            return false;
        }
        if (dayValue != DatatypeConstants.FIELD_UNDEFINED) {
            if (dayValue < 1) {
                return false;
            }
            // El control que ningun setter puede hacer por su cuenta: el 31 de febrero pasa los dos
            // rangos por separado y no existe.
            int limit = monthValue == DatatypeConstants.FIELD_UNDEFINED
                    ? 31
                    : daysInMonth(monthValue, yearValue == DatatypeConstants.FIELD_UNDEFINED ? 2000 : yearValue);
            if (dayValue > limit) {
                return false;
            }
        }
        if (secondValue == 60) {
            // El segundo intercalar solo vale a las 23:59:60 UTC. No se valida mas fino que eso
            // porque cuando hubo un segundo intercalar es una tabla historica que esta biblioteca
            // no tiene, y rechazar los que si existieron seria peor que aceptar de mas.
            return true;
        }
        return true;
    }

    /**
     * Cuantos dias tiene ese mes de ese anio.
     *
     * <p>Con {@code if} y no con {@code switch} porque el generador de bytecode de esta VM todavia
     * no acepta un {@code case} cuya etiqueta sea una constante con nombre --pide un literal
     * entero-- y las de {@link DatatypeConstants} lo son.
     */
    private static int daysInMonth(int monthValue, int yearValue) {
        if (monthValue == DatatypeConstants.FEBRUARY) {
            return isLeapYear(yearValue) ? 29 : 28;
        }
        if (monthValue == DatatypeConstants.APRIL || monthValue == DatatypeConstants.JUNE
                || monthValue == DatatypeConstants.SEPTEMBER || monthValue == DatatypeConstants.NOVEMBER) {
            return 30;
        }
        return 31;
    }

    /** La regla gregoriana. */
    private static boolean isLeapYear(int yearValue) {
        return (yearValue % 4 == 0 && yearValue % 100 != 0) || yearValue % 400 == 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>El ajuste que sorprende: cuando sumar meses deja un dia que no existe en el mes de
     * destino, el dia se <b>recorta</b> al ultimo del mes. El 31 de enero mas un mes es el 28 de
     * febrero. Lo dice la especificacion y es lo que hace que sumar un mes nunca cambie de mes dos
     * veces.
     */
    public void add(Duration duration) {
        if (duration == null) {
            throw new NullPointerException("duration is null");
        }
        int s = duration.getSign();
        if (s == 0) {
            return;
        }

        // Anios y meses juntos, que es como los define la especificacion: se suman en meses y
        // recien despues se recorta el dia.
        long monthsToAdd = 0L;
        BigInteger a = (BigInteger) duration.getField(DatatypeConstants.YEARS);
        if (a != null) {
            monthsToAdd += a.longValue() * 12L;
        }
        BigInteger m = (BigInteger) duration.getField(DatatypeConstants.MONTHS);
        if (m != null) {
            monthsToAdd += m.longValue();
        }
        if (monthsToAdd != 0L && monthValue != DatatypeConstants.FIELD_UNDEFINED) {
            long total = (long) (monthValue - DatatypeConstants.JANUARY) + (long) s * monthsToAdd;
            long extraYears = Math.floorDiv(total, 12L);
            int newMonth = (int) Math.floorMod(total, 12L) + DatatypeConstants.JANUARY;
            monthValue = newMonth;
            if (yearValue != DatatypeConstants.FIELD_UNDEFINED) {
                yearValue = (int) (yearValue + extraYears);
            }
            if (dayValue != DatatypeConstants.FIELD_UNDEFINED) {
                int limit = daysInMonth(
                        monthValue, yearValue == DatatypeConstants.FIELD_UNDEFINED ? 2000 : yearValue);
                if (dayValue > limit) {
                    dayValue = limit;
                }
            }
        }

        // Dias, horas, minutos y segundos: todo eso si tiene largo fijo, asi que se junta en un
        // solo numero de segundos --con la fraccion incluida-- y se suma de una. Separarlo por
        // campo obligaria a arrastrar a mano cuatro veces y es donde se cuelan los errores.
        BigDecimal secondsToAdd = BigDecimal.ZERO;
        BigInteger d = (BigInteger) duration.getField(DatatypeConstants.DAYS);
        if (d != null) {
            secondsToAdd = secondsToAdd.add(new BigDecimal(d).multiply(BigDecimal.valueOf(86400L)));
        }
        BigInteger h = (BigInteger) duration.getField(DatatypeConstants.HOURS);
        if (h != null) {
            secondsToAdd = secondsToAdd.add(new BigDecimal(h).multiply(BigDecimal.valueOf(3600L)));
        }
        BigInteger mi = (BigInteger) duration.getField(DatatypeConstants.MINUTES);
        if (mi != null) {
            secondsToAdd = secondsToAdd.add(new BigDecimal(mi).multiply(BigDecimal.valueOf(60L)));
        }
        BigDecimal sec = (BigDecimal) duration.getField(DatatypeConstants.SECONDS);
        if (sec != null) {
            secondsToAdd = secondsToAdd.add(sec);
        }
        if (s < 0) {
            secondsToAdd = secondsToAdd.negate();
        }
        if (secondsToAdd.signum() == 0) {
            return;
        }

        // La hora actual del dia, en segundos. Un campo sin definir cuenta como cero: en un
        // `xs:date` no hay hora que mover y lo unico que sobrevive es el arrastre a dias.
        BigDecimal withinDay = BigDecimal.ZERO;
        if (hourValue != DatatypeConstants.FIELD_UNDEFINED) {
            withinDay = withinDay.add(BigDecimal.valueOf((long) hourValue * 3600L));
        }
        if (minuteValue != DatatypeConstants.FIELD_UNDEFINED) {
            withinDay = withinDay.add(BigDecimal.valueOf((long) minuteValue * 60L));
        }
        if (secondValue != DatatypeConstants.FIELD_UNDEFINED) {
            withinDay = withinDay.add(BigDecimal.valueOf((long) secondValue));
        }
        if (fractionValue != null) {
            withinDay = withinDay.add(fractionValue);
        }

        BigDecimal total = withinDay.add(secondsToAdd);
        BigDecimal perDay = BigDecimal.valueOf(86400L);
        BigDecimal wholeDays = total.divide(perDay, 0, RoundingMode.FLOOR);
        BigDecimal remainder = total.subtract(wholeDays.multiply(perDay));

        BigDecimal intRemainder = remainder.setScale(0, RoundingMode.FLOOR);
        long secondsRemainder = intRemainder.longValue();
        BigDecimal newFraction = remainder.subtract(intRemainder);

        if (hourValue != DatatypeConstants.FIELD_UNDEFINED) {
            hourValue = (int) (secondsRemainder / 3600L);
        }
        if (minuteValue != DatatypeConstants.FIELD_UNDEFINED) {
            minuteValue = (int) ((secondsRemainder % 3600L) / 60L);
        }
        if (secondValue != DatatypeConstants.FIELD_UNDEFINED) {
            secondValue = (int) (secondsRemainder % 60L);
        }
        if (fractionValue != null || newFraction.signum() != 0) {
            fractionValue = newFraction;
        }
        shiftDaysLong(wholeDays.longValue());
    }

    /** Como {@link #correrMinutos} pero para valores que no entran en un {@code int}. */
    private void shiftMinutesLong(long minutes) {
        if (hourValue == DatatypeConstants.FIELD_UNDEFINED) {
            long days = Math.floorDiv(minutes, 1440L);
            shiftDaysLong(days);
            return;
        }
        long total = (long) hourValue * 60L
                + (minuteValue == DatatypeConstants.FIELD_UNDEFINED ? 0L : (long) minuteValue)
                + minutes;
        long days = Math.floorDiv(total, 1440L);
        int withinDay = (int) Math.floorMod(total, 1440L);
        hourValue = withinDay / 60;
        if (minuteValue != DatatypeConstants.FIELD_UNDEFINED) {
            minuteValue = withinDay % 60;
        }
        shiftDaysLong(days);
    }

    /** Corre tantos dias, de a tramos que entren en un {@code int}. */
    private void shiftDaysLong(long days) {
        long pending = days;
        while (pending != 0L) {
            int segment;
            if (pending > (long) Integer.MAX_VALUE) {
                segment = Integer.MAX_VALUE;
            } else if (pending < (long) Integer.MIN_VALUE) {
                segment = Integer.MIN_VALUE;
            } else {
                segment = (int) pending;
            }
            shiftDays(segment);
            pending -= (long) segment;
        }
    }

    // ---- conversiones --------------------------------------------------------------------------

    /** {@inheritDoc} */
    public GregorianCalendar toGregorianCalendar() {
        return toGregorianCalendar(null, null, null);
    }

    /** {@inheritDoc} */
    public GregorianCalendar toGregorianCalendar(
            TimeZone timezone, Locale aLocale, XMLGregorianCalendar defaults) {
        TimeZone tz = timezone;
        if (timezoneValue != DatatypeConstants.FIELD_UNDEFINED) {
            tz = getTimeZone(DatatypeConstants.FIELD_UNDEFINED);
        } else if (tz == null && defaults != null
                && defaults.getTimezone() != DatatypeConstants.FIELD_UNDEFINED) {
            tz = defaults.getTimeZone(DatatypeConstants.FIELD_UNDEFINED);
        }
        GregorianCalendar cal = tz == null
                ? new GregorianCalendar()
                : new GregorianCalendar(tz);
        cal.clear();

        cal.set(Calendar.YEAR, fieldOrDefault(yearValue, defaults == null
                ? DatatypeConstants.FIELD_UNDEFINED : defaults.getYear(), 1970));
        cal.set(Calendar.MONTH,
                Calendar.JANUARY + fieldOrDefault(monthValue, defaults == null
                        ? DatatypeConstants.FIELD_UNDEFINED : defaults.getMonth(),
                        DatatypeConstants.JANUARY) - DatatypeConstants.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, fieldOrDefault(dayValue, defaults == null
                ? DatatypeConstants.FIELD_UNDEFINED : defaults.getDay(), 1));
        cal.set(Calendar.HOUR_OF_DAY, fieldOrDefault(hourValue, defaults == null
                ? DatatypeConstants.FIELD_UNDEFINED : defaults.getHour(), 0));
        cal.set(Calendar.MINUTE, fieldOrDefault(minuteValue, defaults == null
                ? DatatypeConstants.FIELD_UNDEFINED : defaults.getMinute(), 0));
        cal.set(Calendar.SECOND, fieldOrDefault(secondValue, defaults == null
                ? DatatypeConstants.FIELD_UNDEFINED : defaults.getSecond(), 0));
        BigDecimal f = fractionValue;
        if (f == null && defaults != null) {
            f = defaults.getFractionalSecond();
        }
        cal.set(Calendar.MILLISECOND,
                f == null ? 0 : f.movePointRight(3).intValue());
        return cal;
    }

    /** El campo, o el del calendario de omision, o el de la epoca. */
    private static int fieldOrDefault(int own, int defaultOf, int fromEpoch) {
        if (own != DatatypeConstants.FIELD_UNDEFINED) {
            return own;
        }
        if (defaultOf != DatatypeConstants.FIELD_UNDEFINED) {
            return defaultOf;
        }
        return fromEpoch;
    }

    /** {@inheritDoc} */
    public TimeZone getTimeZone(int defaultZoneoffset) {
        int use = timezoneValue != DatatypeConstants.FIELD_UNDEFINED ? timezoneValue : defaultZoneoffset;
        if (use == DatatypeConstants.FIELD_UNDEFINED) {
            return null;
        }
        StringBuilder id = new StringBuilder("GMT");
        int v = use;
        if (v < 0) {
            id.append('-');
            v = -v;
        } else {
            id.append('+');
        }
        twoDigits(id, v / 60);
        id.append(':');
        twoDigits(id, v % 60);
        return TimeZone.getTimeZone(id.toString());
    }

    /** {@inheritDoc} */
    public Object clone() {
        KajiXMLGregorianCalendar c = new KajiXMLGregorianCalendar();
        c.eon = eon;
        c.yearValue = yearValue;
        c.monthValue = monthValue;
        c.dayValue = dayValue;
        c.hourValue = hourValue;
        c.minuteValue = minuteValue;
        c.secondValue = secondValue;
        c.fractionValue = fractionValue;
        c.timezoneValue = timezoneValue;
        return c;
    }
}
