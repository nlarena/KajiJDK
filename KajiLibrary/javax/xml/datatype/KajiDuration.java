package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * La {@link Duration} concreta de esta biblioteca.
 *
 * <p>Interna: no es parte de la API, no es publica, y quien la use la ve como {@code Duration}. Lo
 * que esta clase aporta es que las duraciones de esta biblioteca <b>calculan de verdad</b> --parsean
 * su forma lexica, suman, multiplican, normalizan y comparan-- sin que haga falta ningun parser de
 * XML: una duracion es aritmetica sobre seis numeros.
 *
 * <h2>Como se guarda</h2>
 *
 * <p>Un signo y seis magnitudes no negativas, cada una de las cuales puede ser null --el campo no
 * esta puesto--. Es la representacion que pide la API: {@link Duration#getField} devuelve null para
 * un campo ausente y {@link Duration#getSign} da el signo aparte, asi que guardar seis numeros con
 * signo obligaria a reconstruir esa forma en cada consulta.
 *
 * <h2>La comparacion, que es lo mas delicado</h2>
 *
 * <p>{@link #compare} implementa el algoritmo normativo de XML Schema, que es indirecto y vale
 * conocerlo: se le suman las dos duraciones a <b>cuatro instantes de referencia</b> fijados por la
 * especificacion --1696-09-01, 1697-02-01, 1903-03-01 y 1903-07-01-- y, si las cuatro comparaciones
 * coinciden, ese es el resultado; si no, es {@link DatatypeConstants#INDETERMINATE}.
 *
 * <p>Los cuatro no son arbitrarios: entre ellos cubren un febrero de 28 dias y uno de 29, y un mes
 * de 30 y uno de 31. O sea que son un contraejemplo para cada forma en que un mes puede medir
 * distinto. Si las cuatro dan lo mismo, ninguna eleccion de mes cambia el resultado, y ahi el orden
 * si existe.
 *
 * <p>Por eso {@code P1M} contra {@code P30D} da indeterminado --en un febrero de 28 dias es mas
 * corta y en marzo es mas larga-- y {@code PT60S} contra {@code PT1M} da igual.
 */
final class KajiDuration extends Duration {

    /** Los cuatro instantes de referencia de la especificacion, como {@code aaaammdd}. */
    private static final int[][] REFERENCES = {
        {1696, 9, 1}, {1697, 2, 1}, {1903, 3, 1}, {1903, 7, 1},
    };

    /** -1, 0 o 1. Cero solo si todos los campos puestos valen cero. */
    private final int sign;

    /** Los anios, no negativos, o null si el campo no esta. */
    private final BigInteger years;

    /** Los meses, no negativos, o null. */
    private final BigInteger months;

    /** Los dias, no negativos, o null. */
    private final BigInteger days;

    /** Las horas, no negativas, o null. */
    private final BigInteger hours;

    /** Los minutos, no negativos, o null. */
    private final BigInteger minutes;

    /** Los segundos, no negativos y con fraccion, o null. */
    private final BigDecimal seconds;

    /**
     * Campo por campo, que es el constructor que todos los demas terminan usando.
     *
     * @param positiva el signo pedido
     * @param anios los anios, o null
     * @param meses los meses, o null
     * @param dias los dias, o null
     * @param horas las horas, o null
     * @param minutos los minutos, o null
     * @param segundos los segundos, o null
     * @throws IllegalArgumentException si estan los seis en null o si alguno es negativo
     */
    KajiDuration(boolean positive, BigInteger years, BigInteger months, BigInteger days,
            BigInteger hours, BigInteger minutes, BigDecimal seconds) {
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;

        if (years == null && months == null && days == null
                && hours == null && minutes == null && seconds == null) {
            throw new IllegalArgumentException(
                    "all the fields are null, at least one field must be non-null");
        }
        requireNonNegative(years, "years");
        requireNonNegative(months, "months");
        requireNonNegative(days, "days");
        requireNonNegative(hours, "hours");
        requireNonNegative(minutes, "minutes");
        if (seconds != null && seconds.signum() < 0) {
            throw new IllegalArgumentException("seconds is negative: " + seconds);
        }

        // El signo cero no se pide: se deduce. Una duracion de todos ceros es la misma la pidan
        // positiva o negativa, y que `getSign()` contestara 1 para `-P0D` seria una diferencia
        // observable entre dos objetos que representan lo mismo.
        if (isAllZero()) {
            this.sign = 0;
        } else {
            this.sign = positive ? 1 : -1;
        }
    }

    /** Un campo entero tiene que ser no negativo: el signo va aparte. */
    private static void requireNonNegative(BigInteger v, String fieldName) {
        if (v != null && v.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " is negative: " + v);
        }
    }

    /** Si todos los campos puestos valen cero. */
    private boolean isAllZero() {
        return isZero(years) && isZero(months) && isZero(days)
                && isZero(hours) && isZero(minutes)
                && (seconds == null || seconds.signum() == 0);
    }

    /** Null cuenta como cero para decidir el signo. */
    private static boolean isZero(BigInteger v) {
        return v == null || v.signum() == 0;
    }

    // ---- las tres formas de construir ---------------------------------------------------------

    /**
     * A partir de la forma lexica {@code -?PnYnMnDTnHnMnS}.
     *
     * <p>El parseo es a mano y no con una expresion regular, por dos motivos concretos. El primero
     * es que hacen falta mensajes de error que digan <b>que</b> esta mal --una expresion que no
     * casa solo puede decir que no casa--. El segundo es que dos de las reglas no se expresan
     * comodo en una expresion: que tiene que haber al menos un campo, y que la {@code T} no puede
     * estar sola.
     *
     * @param lexica la forma lexica; no puede ser null
     * @return la duracion
     * @throws IllegalArgumentException si la forma esta mal
     * @throws NullPointerException si es null
     */
    static KajiDuration parse(String lexical) {
        if (lexical == null) {
            throw new NullPointerException("lexicalRepresentation is null");
        }
        int n = lexical.length();
        int i = 0;
        boolean positive = true;
        if (i < n && lexical.charAt(i) == '-') {
            positive = false;
            i++;
        }
        if (i >= n || lexical.charAt(i) != 'P') {
            throw new IllegalArgumentException(badFormat(lexical, "missing 'P'"));
        }
        i++;

        BigInteger[] datePart = new BigInteger[3];   // Y, M, D
        BigInteger[] hourValue = new BigInteger[2];    // H, M
        BigDecimal[] secs = new BigDecimal[1];

        // La parte de fecha: los designadores tienen que venir en orden y no repetirse.
        String dateDesignators = "YMD";
        int next = 0;
        boolean sawSomething = false;
        while (i < n && lexical.charAt(i) != 'T') {
            int end = endOfNumber(lexical, i);
            if (end == i) {
                throw new IllegalArgumentException(badFormat(lexical, "expected a number"));
            }
            if (end >= n) {
                throw new IllegalArgumentException(badFormat(lexical, "number without a designator"));
            }
            char d = lexical.charAt(end);
            int pos = dateDesignators.indexOf(d);
            if (pos < 0 || pos < next) {
                throw new IllegalArgumentException(
                        badFormat(lexical, "unexpected designator '" + d + "'"));
            }
            datePart[pos] = intOf(lexical.substring(i, end), lexical);
            next = pos + 1;
            sawSomething = true;
            i = end + 1;
        }

        if (i < n && lexical.charAt(i) == 'T') {
            i++;
            if (i >= n) {
                // La `T` anuncia que viene una parte de tiempo; sin nada detras es una forma
                // invalida y no una parte de tiempo vacia.
                throw new IllegalArgumentException(badFormat(lexical, "'T' without a time part"));
            }
            String timeDesignators = "HMS";
            next = 0;
            while (i < n) {
                int end = endOfNumber(lexical, i);
                if (end == i) {
                    throw new IllegalArgumentException(badFormat(lexical, "expected a number"));
                }
                if (end >= n) {
                    throw new IllegalArgumentException(
                            badFormat(lexical, "number without a designator"));
                }
                char d = lexical.charAt(end);
                int pos = timeDesignators.indexOf(d);
                if (pos < 0 || pos < next) {
                    throw new IllegalArgumentException(
                            badFormat(lexical, "unexpected designator '" + d + "'"));
                }
                String text = lexical.substring(i, end);
                if (pos == 2) {
                    secs[0] = decimalOf(text, lexical);
                } else {
                    if (text.indexOf('.') >= 0) {
                        // Solo los segundos pueden tener fraccion; media hora se escribe `PT30M`.
                        throw new IllegalArgumentException(
                                badFormat(lexical, "only seconds may have a fraction"));
                    }
                    hourValue[pos] = intOf(text, lexical);
                }
                next = pos + 1;
                sawSomething = true;
                i = end + 1;
            }
        }

        if (!sawSomething) {
            throw new IllegalArgumentException(badFormat(lexical, "no fields"));
        }
        return new KajiDuration(
                positive, datePart[0], datePart[1], datePart[2], hourValue[0], hourValue[1], secs[0]);
    }

    /** Hasta donde llega el numero que empieza en {@code i} (digitos y a lo sumo un punto). */
    private static int endOfNumber(String s, int i) {
        int j = i;
        while (j < s.length()) {
            char c = s.charAt(j);
            if ((c >= '0' && c <= '9') || c == '.') {
                j++;
            } else {
                break;
            }
        }
        return j;
    }

    /** Un campo entero, con el error del contrato si no lo es. */
    private static BigInteger intOf(String text, String lexical) {
        try {
            return new BigInteger(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(badFormat(lexical, "'" + text + "' is not a number"));
        }
    }

    /** El campo de segundos, que si puede tener fraccion. */
    private static BigDecimal decimalOf(String text, String lexical) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(badFormat(lexical, "'" + text + "' is not a number"));
        }
    }

    /** El mensaje de forma lexica invalida, con el motivo concreto. */
    private static String badFormat(String lexical, String reason) {
        return "\"" + lexical + "\" is not a valid representation of an XML Schema duration: "
                + reason;
    }

    /**
     * A partir de una cantidad de milisegundos, con los seis campos puestos.
     *
     * <p>Los anios y los meses no salen de dividir: salen de <b>contar sobre el calendario</b>
     * desde la epoca. Es la unica forma correcta, porque un mes no tiene un largo fijo y cualquier
     * divisor que se elija --30 dias, 30,44 dias-- da un resultado que no corresponde a ninguna
     * fecha real.
     *
     * <p>El truco que hace la cuenta simple: la epoca es el <b>1</b> de enero, asi que el dia del
     * mes de destino siempre es mayor o igual que 1 y el mes siempre es mayor o igual que enero. La
     * resta campo a campo nunca pide prestado, y no hace falta el ajuste que normalmente lleva una
     * diferencia de fechas.
     *
     * @param milisegundos los milisegundos, con signo
     * @return la duracion
     */
    static KajiDuration fromMillis(long milliseconds) {
        boolean positive = milliseconds >= 0;
        long remainder = milliseconds < 0 ? -milliseconds : milliseconds;

        long millis = remainder % 1000L;
        remainder /= 1000L;
        long secs = remainder % 60L;
        remainder /= 60L;
        long min = remainder % 60L;
        remainder /= 60L;
        long hrs = remainder % 24L;
        long totalDays = remainder / 24L;

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(0L);
        // `Calendar.add` toma un `int`; para valores enormes se suma de a tramos.
        long pending = totalDays;
        while (pending > 0L) {
            int segment = pending > (long) Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pending;
            cal.add(Calendar.DAY_OF_MONTH, segment);
            pending -= (long) segment;
        }

        int years = cal.get(Calendar.YEAR) - 1970;
        int months = cal.get(Calendar.MONTH) - Calendar.JANUARY;
        int days = cal.get(Calendar.DAY_OF_MONTH) - 1;

        return new KajiDuration(
                positive,
                BigInteger.valueOf((long) years),
                BigInteger.valueOf((long) months),
                BigInteger.valueOf((long) days),
                BigInteger.valueOf(hrs),
                BigInteger.valueOf(min),
                // Escala tres: los milisegundos son la fraccion, y guardarla asi hace que
                // `toString` escriba `1.000S` como el original y no `1S`.
                BigDecimal.valueOf(secs * 1000L + millis, 3));
    }

    // ---- los seis accesores enteros, que la implementacion de referencia redefine ------------

    // La clase abstracta documenta que `getYears()` y sus hermanos contestan FIELD_UNDEFINED
    // cuando el campo no esta, y asi esta escrita en `Duration`. La implementacion de referencia
    // --Xerces, la que trae el JDK-- los redefine y contesta **cero**: comprobado contra
    // `H:/jdk-25.0.2`, donde `newDuration("P1M").getDays()` da 0 y no -2147483648, mientras que
    // `getField(DAYS)` da null y `isSet(DAYS)` da false en las dos.
    //
    // O sea que el JDK se aparta ahi de su propio contrato. Se replica el comportamiento de la
    // implementacion de referencia y no el del javadoc, por una razon concreta: el codigo que se
    // escribio contra el JDK anda contra esto, y el que quiera distinguir "ausente" de "cero"
    // tiene `isSet` y `getField`, que si dicen la verdad en las dos.

    /** {@inheritDoc} */
    public int getYears() {
        return intOrZero(DatatypeConstants.YEARS);
    }

    /** {@inheritDoc} */
    public int getMonths() {
        return intOrZero(DatatypeConstants.MONTHS);
    }

    /** {@inheritDoc} */
    public int getDays() {
        return intOrZero(DatatypeConstants.DAYS);
    }

    /** {@inheritDoc} */
    public int getHours() {
        return intOrZero(DatatypeConstants.HOURS);
    }

    /** {@inheritDoc} */
    public int getMinutes() {
        return intOrZero(DatatypeConstants.MINUTES);
    }

    /** {@inheritDoc} */
    public int getSeconds() {
        return intOrZero(DatatypeConstants.SECONDS);
    }

    /** El campo como entero, o cero si no esta puesto. */
    private int intOrZero(javax.xml.datatype.DatatypeConstants.Field fieldId) {
        Number n = getField(fieldId);
        return n == null ? 0 : n.intValue();
    }

    // ---- lo que pide la clase abstracta -------------------------------------------------------

    /** {@inheritDoc} */
    public int getSign() {
        return sign;
    }

    /** {@inheritDoc} */
    public Number getField(javax.xml.datatype.DatatypeConstants.Field field) {
        if (field == null) {
            throw new NullPointerException("field is null");
        }
        if (field == DatatypeConstants.YEARS) {
            return years;
        }
        if (field == DatatypeConstants.MONTHS) {
            return months;
        }
        if (field == DatatypeConstants.DAYS) {
            return days;
        }
        if (field == DatatypeConstants.HOURS) {
            return hours;
        }
        if (field == DatatypeConstants.MINUTES) {
            return minutes;
        }
        if (field == DatatypeConstants.SECONDS) {
            return seconds;
        }
        throw new IllegalArgumentException("unknown field: " + field);
    }

    /** {@inheritDoc} */
    public boolean isSet(javax.xml.datatype.DatatypeConstants.Field field) {
        return getField(field) != null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Suma campo a campo con los signos aplicados, y despues exige que todos los resultados
     * tengan el mismo signo. Esa exigencia es lo que hace que {@code P1M + (-P30D)} levante: daria
     * un mes positivo y treinta dias negativos, y no hay forma de escribir eso como una duracion
     * --que es un signo y seis magnitudes-- ni de saber cual seria su signo sin elegir un mes.
     *
     * <p>Un campo queda puesto en el resultado si estaba puesto en alguno de los dos sumandos.
     */
    public Duration add(Duration rhs) {
        if (rhs == null) {
            throw new NullPointerException("rhs is null");
        }
        BigDecimal[] sum = new BigDecimal[6];
        boolean[] wasSet = new boolean[6];
        for (int i = 0; i < 6; i++) {
            javax.xml.datatype.DatatypeConstants.Field fieldId = fieldByIndex(i);
            BigDecimal a = withSign(this, fieldId);
            BigDecimal b = withSign(rhs, fieldId);
            sum[i] = a.add(b);
            wasSet[i] = isSet(fieldId) || rhs.isSet(fieldId);
        }

        // Todos los campos no nulos del resultado tienen que apuntar para el mismo lado.
        int resultSign = 0;
        for (int i = 0; i < 6; i++) {
            int s = sum[i].signum();
            if (s == 0) {
                continue;
            }
            if (resultSign == 0) {
                resultSign = s;
            } else if (resultSign != s) {
                throw new IllegalStateException(
                        this + " + " + rhs + " is not a valid duration:"
                                + " the result would have fields of both signs");
            }
        }
        boolean positive = resultSign >= 0;

        return new KajiDuration(
                positive,
                wasSet[0] ? sum[0].abs().toBigInteger() : null,
                wasSet[1] ? sum[1].abs().toBigInteger() : null,
                wasSet[2] ? sum[2].abs().toBigInteger() : null,
                wasSet[3] ? sum[3].abs().toBigInteger() : null,
                wasSet[4] ? sum[4].abs().toBigInteger() : null,
                wasSet[5] ? sum[5].abs() : null);
    }

    /** El campo con el signo de la duracion aplicado; cero si no esta puesto. */
    private static BigDecimal withSign(Duration d, javax.xml.datatype.DatatypeConstants.Field fieldId) {
        Number n = d.getField(fieldId);
        if (n == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal v = n instanceof BigDecimal ? (BigDecimal) n : new BigDecimal((BigInteger) n);
        return d.getSign() < 0 ? v.negate() : v;
    }

    /** El campo que corresponde al indice 0..5. */
    private static javax.xml.datatype.DatatypeConstants.Field fieldByIndex(int i) {
        switch (i) {
            case 0: return DatatypeConstants.YEARS;
            case 1: return DatatypeConstants.MONTHS;
            case 2: return DatatypeConstants.DAYS;
            case 3: return DatatypeConstants.HOURS;
            case 4: return DatatypeConstants.MINUTES;
            default: return DatatypeConstants.SECONDS;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>El orden --anios, meses, dias, horas, minutos, segundos-- lo fija la especificacion y no
     * es intercambiable: sumarle un mes y un dia al 31 de enero da el 29 de febrero, y sumarle un
     * dia y un mes da el 1 de marzo. Un orden fijo es lo unico que hace la operacion reproducible.
     */
    public void addTo(Calendar calendar) {
        if (calendar == null) {
            throw new NullPointerException("calendar is null");
        }
        int s = sign;
        if (s == 0) {
            return;
        }
        addField(calendar, Calendar.YEAR, years, s);
        addField(calendar, Calendar.MONTH, months, s);
        addField(calendar, Calendar.DAY_OF_MONTH, days, s);
        addField(calendar, Calendar.HOUR_OF_DAY, hours, s);
        addField(calendar, Calendar.MINUTE, minutes, s);
        if (seconds != null) {
            // Los segundos se suman en milisegundos para no perder la fraccion, que es justamente
            // lo que distingue `PT0.5S` de `PT0S`.
            long millis = seconds.movePointRight(3).setScale(0, RoundingMode.DOWN).longValue();
            calendar.setTimeInMillis(calendar.getTimeInMillis() + (long) s * millis);
        }
    }

    /** Suma un campo entero al calendario, respetando el signo de la duracion. */
    private static void addField(Calendar cal, int fieldId, BigInteger value, int sign) {
        if (value == null || value.signum() == 0) {
            return;
        }
        long pending = value.longValue();
        while (pending > 0L) {
            int segment = pending > (long) Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pending;
            cal.add(fieldId, sign * segment);
            pending -= (long) segment;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>La fraccion que quede en un campo baja al siguiente --medio anio son seis meses, medio dia
     * son doce horas-- con una sola excepcion: de los meses no se puede bajar a los dias, porque no
     * hay una equivalencia fija. Multiplicar {@code P1M} por {@code 0.5} levanta, y esa es la misma
     * ambigüedad que hace falta {@link DatatypeConstants#INDETERMINATE}.
     */
    public Duration multiply(BigDecimal factor) {
        if (factor == null) {
            throw new NullPointerException("factor is null");
        }
        boolean positive = (sign >= 0) == (factor.signum() >= 0);
        BigDecimal f = factor.abs();

        BigDecimal[] v = new BigDecimal[6];
        for (int i = 0; i < 6; i++) {
            Number n = getField(fieldByIndex(i));
            if (n == null) {
                v[i] = null;
            } else if (n instanceof BigDecimal) {
                v[i] = ((BigDecimal) n).multiply(f);
            } else {
                v[i] = new BigDecimal((BigInteger) n).multiply(f);
            }
        }

        // Las fracciones bajan de un campo al siguiente, de arriba hacia abajo.
        BigInteger[] ints = new BigInteger[6];
        BigDecimal carry = BigDecimal.ZERO;
        for (int i = 0; i < 5; i++) {
            if (v[i] == null) {
                if (carry.signum() != 0) {
                    // El arrastre no tiene donde ir: el campo de destino no existe en esta
                    // duracion, asi que se lo empuja al siguiente que si exista.
                    carry = carry.multiply(BigDecimal.valueOf(factorTo(i + 1)));
                }
                continue;
            }
            BigDecimal total = v[i].add(carry);
            BigDecimal integerPart = total.setScale(0, RoundingMode.DOWN);
            BigDecimal fractionValue = total.subtract(integerPart);
            ints[i] = integerPart.toBigInteger();
            if (fractionValue.signum() != 0) {
                if (i == 1) {
                    throw new IllegalStateException(
                            "multiplying " + this + " by " + factor
                                    + " leaves a fraction of a month, which cannot be converted"
                                    + " to days: a month has no fixed number of days");
                }
                carry = fractionValue.multiply(BigDecimal.valueOf(factorTo(i + 1)));
            } else {
                carry = BigDecimal.ZERO;
            }
        }
        BigDecimal secs = v[5];
        if (secs != null) {
            secs = secs.add(carry);
        } else if (carry.signum() != 0) {
            // Lo mismo que arriba: si no hay campo de segundos, la fraccion se pierde. No se
            // inventa un campo que la duracion no tenia.
            carry = BigDecimal.ZERO;
        }

        return new KajiDuration(
                positive, ints[0], ints[1], ints[2], ints[3], ints[4], secs);
    }

    /** Cuantas unidades del campo {@code i} entran en una del campo {@code i-1}. */
    private static long factorTo(int i) {
        switch (i) {
            case 1: return 12L;   // meses en un anio
            case 3: return 24L;   // horas en un dia
            case 4: return 60L;   // minutos en una hora
            case 5: return 60L;   // segundos en un minuto
            default: return 1L;   // de meses a dias no hay factor: ese caso levanta antes
        }
    }

    /** {@inheritDoc} */
    public Duration negate() {
        return new KajiDuration(sign >= 0 ? false : true,
                years, months, days, hours, minutes, seconds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Fijado el punto de partida, un mes ya tiene una cantidad de dias: se le suman los anios y
     * los meses al calendario, se mide cuantos dias se movio, y esos dias reemplazan a los dos
     * campos. El resultado no tiene meses, asi que se puede comparar con cualquier otro sin dar
     * indeterminado.
     */
    public Duration normalizeWith(Calendar startTimeInstant) {
        if (startTimeInstant == null) {
            throw new NullPointerException("startTimeInstant is null");
        }
        Calendar cal = new GregorianCalendar();
        cal.setTimeZone(startTimeInstant.getTimeZone());
        cal.setTimeInMillis(startTimeInstant.getTimeInMillis());
        long from = cal.getTimeInMillis();

        addField(cal, Calendar.YEAR, years, 1);
        addField(cal, Calendar.MONTH, months, 1);
        long millisOfYearsAndMonths = cal.getTimeInMillis() - from;

        BigInteger extraDays = BigInteger.valueOf(millisOfYearsAndMonths / 86400000L);
        BigInteger totalDays = days == null ? extraDays : days.add(extraDays);

        return new KajiDuration(
                sign >= 0, null, null, totalDays, hours, minutes, seconds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>El algoritmo de los cuatro instantes de referencia; ver el encabezado de la clase.
     */
    public int compare(Duration duration) {
        if (duration == null) {
            throw new NullPointerException("duration is null");
        }
        int accumulated = 0;
        boolean first = true;
        for (int i = 0; i < REFERENCES.length; i++) {
            int[] ref = REFERENCES[i];
            long onThis = instantPlus(ref, this);
            long onOther = instantPlus(ref, duration);
            int c;
            if (onThis < onOther) {
                c = DatatypeConstants.LESSER;
            } else if (onThis > onOther) {
                c = DatatypeConstants.GREATER;
            } else {
                c = DatatypeConstants.EQUAL;
            }
            if (first) {
                accumulated = c;
                first = false;
            } else if (c != accumulated) {
                return DatatypeConstants.INDETERMINATE;
            }
        }
        return accumulated;
    }

    /** El instante que resulta de sumarle {@code d} a la fecha de referencia, en milisegundos. */
    private static long instantPlus(int[] ref, Duration d) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        // Los meses de REFERENCIAS van de 1 a 12 y los de `Calendar` desde `JANUARY`, que es cero.
        cal.set(ref[0], Calendar.JANUARY + ref[1] - 1, ref[2], 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        d.addTo(cal);
        return cal.getTimeInMillis();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Se hashea el instante que resulta de sumarle la duracion al primero de los cuatro
     * instantes de referencia. Es lo unico coherente con {@link Duration#equals}, que esta definido
     * por {@link #compare}: dos duraciones iguales dan el mismo instante en <b>los cuatro</b>
     * --por definicion de la comparacion-- asi que en particular dan el mismo en ese.
     *
     * <p>Dos duraciones que no son iguales pueden coincidir en ese instante y colisionar, que es lo
     * que un hash puede hacer. Lo que no puede pasar es lo contrario, que es lo que importa.
     */
    public int hashCode() {
        long instant = instantPlus(REFERENCES[0], this);
        return (int) (instant ^ (instant >>> 32));
    }
}
