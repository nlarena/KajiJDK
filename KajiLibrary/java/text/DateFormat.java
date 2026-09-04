package java.text;

import java.io.InvalidObjectException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * La base abstracta de los formateadores de fecha y hora.
 *
 * <p><b>Sobre la nota vieja del proyecto.</b> Este archivo faltaba porque
 * "{@code DateFormat}/{@code SimpleDateFormat} están bloqueadas — toda su API se apoya en
 * {@code java.util.Date}, {@code Calendar} y {@code TimeZone}, que no existen". Ya no es cierto:
 * los tres existen y están completos, así que la clase se pudo escribir entera. No quedó ni un
 * miembro afuera por falta de dependencias.
 *
 * <p><b>Qué hace realmente esta clase.</b> No formatea: reparte. Un {@code Date} es un instante —un
 * número de milisegundos— y no sabe nada de años ni de meses; el que sabe es el {@link Calendar},
 * que traduce ese instante a campos según una zona horaria y un calendario. Por eso el
 * {@code calendar} es un campo {@code protected} y no un detalle interno: cambiarlo cambia el
 * resultado, y {@link #setTimeZone} no es más que un atajo para tocarlo.
 *
 * <p>El {@code numberFormat} está por el mismo motivo un escalón más abajo: los campos de una fecha
 * se escriben con dígitos, y qué dígitos usa cada locale lo decide un {@link NumberFormat}.
 *
 * <p><b>Los estilos van al revés de lo que uno espera</b>: {@code FULL} es 0 y {@code SHORT} es 3,
 * así que el estilo "más grande" es el número más chico. Está así en la API original y no se puede
 * cambiar; conviene tenerlo presente al leer cualquier comparación entre estilos.
 *
 * @implNote Los patrones por locale y estilo salen de {@code PatronesLocales}, que cubre seis
 *           locales; uno desconocido cae en ROOT, igual que en el JDK cuando no tiene datos. Los
 *           nombres —meses, días, am/pm— salen de {@link DateFormatSymbols}.
 */
public abstract class DateFormat extends Format {

    public static final int ERA_FIELD = 0;
    public static final int YEAR_FIELD = 1;
    public static final int MONTH_FIELD = 2;
    public static final int DATE_FIELD = 3;
    public static final int HOUR_OF_DAY1_FIELD = 4;
    public static final int HOUR_OF_DAY0_FIELD = 5;
    public static final int MINUTE_FIELD = 6;
    public static final int SECOND_FIELD = 7;
    public static final int MILLISECOND_FIELD = 8;
    public static final int DAY_OF_WEEK_FIELD = 9;
    public static final int DAY_OF_YEAR_FIELD = 10;
    public static final int DAY_OF_WEEK_IN_MONTH_FIELD = 11;
    public static final int WEEK_OF_YEAR_FIELD = 12;
    public static final int WEEK_OF_MONTH_FIELD = 13;
    public static final int AM_PM_FIELD = 14;
    public static final int HOUR1_FIELD = 15;
    public static final int HOUR0_FIELD = 16;
    public static final int TIMEZONE_FIELD = 17;

    public static final int FULL = 0;
    public static final int LONG = 1;
    public static final int MEDIUM = 2;
    public static final int SHORT = 3;
    public static final int DEFAULT = 2;

    /**
     * La clave con que un formateador de fechas marca cada campo del texto que produjo.
     *
     * <p>A diferencia de {@link java.text.NumberFormat.Field}, ésta lleva además el campo de {@link Calendar}
     * equivalente: es la que hace de puente entre las dos formas de nombrar un campo de fecha —la
     * de {@code java.text} y la de {@code java.util}— y por eso tiene {@link #getCalendarField()} y
     * {@link #ofCalendarField(int)}, que {@code java.text.NumberFormat.Field} no necesita.
     */
    public static class Field extends java.text.Format.Field {

        private static final Map<String, java.text.DateFormat.Field> POR_NOMBRE =
                new HashMap<String, java.text.DateFormat.Field>();

        // Índice por campo de Calendar, para ofCalendarField. Un arreglo y no un Map porque las
        // claves son los enteros densos 0..FIELD_COUNT y el arreglo ES el índice.
        private static final java.text.DateFormat.Field[] POR_CAMPO = new java.text.DateFormat.Field[Calendar.FIELD_COUNT];

        private final int calendarField;

        protected Field(String name, int calendarField) {
            super(name);
            this.calendarField = calendarField;
            if (this.getClass() == java.text.DateFormat.Field.class) {
                POR_NOMBRE.put(name, this);
                if (calendarField >= 0 && calendarField < Calendar.FIELD_COUNT) {
                    POR_CAMPO[calendarField] = this;
                }
            }
        }

        /**
         * La clave que corresponde a un campo de {@link Calendar}.
         *
         * @throws IllegalArgumentException si el entero no nombra un campo de Calendar. Se lanza en
         *         lugar de devolver {@code null} porque un campo inexistente es un error del
         *         llamador, no un "no hay dato".
         */
        public static java.text.DateFormat.Field ofCalendarField(int calendarField) {
            if (calendarField < 0 || calendarField >= Calendar.FIELD_COUNT) {
                throw new IllegalArgumentException("Unknown Calendar constant " + calendarField);
            }
            return POR_CAMPO[calendarField];
        }

        public int getCalendarField() {
            return this.calendarField;
        }

        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != java.text.DateFormat.Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }
            java.text.DateFormat.Field f = POR_NOMBRE.get(this.getName());
            if (f != null) {
                return f;
            }
            throw new InvalidObjectException("unknown attribute name");
        }

        public static final java.text.DateFormat.Field ERA = new java.text.DateFormat.Field("era", Calendar.ERA);
        public static final java.text.DateFormat.Field YEAR = new java.text.DateFormat.Field("year", Calendar.YEAR);
        public static final java.text.DateFormat.Field MONTH = new java.text.DateFormat.Field("month", Calendar.MONTH);
        public static final java.text.DateFormat.Field DAY_OF_MONTH =
                new java.text.DateFormat.Field("day of month", Calendar.DAY_OF_MONTH);
        public static final java.text.DateFormat.Field HOUR_OF_DAY1 =
                new java.text.DateFormat.Field("hour of day 1", -1);
        public static final java.text.DateFormat.Field HOUR_OF_DAY0 =
                new java.text.DateFormat.Field("hour of day", Calendar.HOUR_OF_DAY);
        public static final java.text.DateFormat.Field MINUTE = new java.text.DateFormat.Field("minute", Calendar.MINUTE);
        public static final java.text.DateFormat.Field SECOND = new java.text.DateFormat.Field("second", Calendar.SECOND);
        public static final java.text.DateFormat.Field MILLISECOND =
                new java.text.DateFormat.Field("millisecond", Calendar.MILLISECOND);
        public static final java.text.DateFormat.Field DAY_OF_WEEK =
                new java.text.DateFormat.Field("day of week", Calendar.DAY_OF_WEEK);
        public static final java.text.DateFormat.Field DAY_OF_YEAR =
                new java.text.DateFormat.Field("day of year", Calendar.DAY_OF_YEAR);
        public static final java.text.DateFormat.Field DAY_OF_WEEK_IN_MONTH =
                new java.text.DateFormat.Field("day of week in month", Calendar.DAY_OF_WEEK_IN_MONTH);
        public static final java.text.DateFormat.Field WEEK_OF_YEAR =
                new java.text.DateFormat.Field("week of year", Calendar.WEEK_OF_YEAR);
        public static final java.text.DateFormat.Field WEEK_OF_MONTH =
                new java.text.DateFormat.Field("week of month", Calendar.WEEK_OF_MONTH);
        public static final java.text.DateFormat.Field AM_PM = new java.text.DateFormat.Field("am pm", Calendar.AM_PM);
        public static final java.text.DateFormat.Field HOUR1 = new java.text.DateFormat.Field("hour 1", -1);
        public static final java.text.DateFormat.Field HOUR0 = new java.text.DateFormat.Field("hour", Calendar.HOUR);
        public static final java.text.DateFormat.Field TIME_ZONE = new java.text.DateFormat.Field("time zone", -1);
    }

    /**
     * El calendario que traduce el instante a campos. Es {@code protected} porque una subclase lo
     * lee directamente para formatear, y porque cambiarlo es la forma documentada de cambiar la
     * zona horaria o el sistema calendárico.
     */
    protected Calendar calendar;

    /** Con qué se escriben los dígitos de cada campo. */
    protected NumberFormat numberFormat;

    protected DateFormat() {
    }

    public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        if (obj instanceof Date) {
            return this.format((Date) obj, toAppendTo, fieldPosition);
        }
        if (obj instanceof Number) {
            // Un Number se interpreta como milisegundos desde la época. No es una conveniencia
            // caprichosa: es lo que hace que un MessageFormat con {0,date} acepte el long crudo.
            return this.format(new Date(((Number) obj).longValue()), toAppendTo, fieldPosition);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Date");
    }

    public abstract StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition);

    public final String format(Date date) {
        return this.format(date, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public Date parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Date result = this.parse(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("Unparseable date: \"" + source + "\"", pos.getErrorIndex());
        }
        return result;
    }

    public abstract Date parse(String source, ParsePosition pos);

    public Object parseObject(String source, ParsePosition pos) {
        return this.parse(source, pos);
    }

    // ---- fábricas ----

    public static final DateFormat getTimeInstance() {
        return DateFormat.getTimeInstance(DateFormat.DEFAULT, Locale.getDefault());
    }

    public static final DateFormat getTimeInstance(int style) {
        return DateFormat.getTimeInstance(style, Locale.getDefault());
    }

    public static final DateFormat getTimeInstance(int style, Locale aLocale) {
        return new SimpleDateFormat(PatronesLocales.hora(DateFormat.verificar(style), aLocale), aLocale);
    }

    public static final DateFormat getDateInstance() {
        return DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
    }

    public static final DateFormat getDateInstance(int style) {
        return DateFormat.getDateInstance(style, Locale.getDefault());
    }

    public static final DateFormat getDateInstance(int style, Locale aLocale) {
        return new SimpleDateFormat(PatronesLocales.fecha(DateFormat.verificar(style), aLocale), aLocale);
    }

    public static final DateFormat getDateTimeInstance() {
        return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT,
                Locale.getDefault());
    }

    public static final DateFormat getDateTimeInstance(int dateStyle, int timeStyle) {
        return DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.getDefault());
    }

    public static final DateFormat getDateTimeInstance(int dateStyle, int timeStyle, Locale aLocale) {
        return new SimpleDateFormat(PatronesLocales.fechaHora(DateFormat.verificar(dateStyle),
                DateFormat.verificar(timeStyle), aLocale), aLocale);
    }

    /** Fecha y hora, las dos en estilo SHORT: el "dame algo corto" de la API. */
    public static final DateFormat getInstance() {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    }

    private static int verificar(int style) {
        if (style < DateFormat.FULL || style > DateFormat.SHORT) {
            throw new IllegalArgumentException("Illegal date/time style " + style);
        }
        return style;
    }

    /**
     * Los locales con datos propios. Son los mismos seis de {@link NumberFormat}: patrones y
     * nombres salen de las dos tablas del paquete, y las dos cubren las mismas filas.
     */
    public static Locale[] getAvailableLocales() {
        return DecimalFormatSymbols.getAvailableLocales();
    }

    // ---- estado ----

    public void setCalendar(Calendar newCalendar) {
        this.calendar = newCalendar;
    }

    public Calendar getCalendar() {
        return this.calendar;
    }

    public void setNumberFormat(NumberFormat newNumberFormat) {
        this.numberFormat = newNumberFormat;
    }

    public NumberFormat getNumberFormat() {
        return this.numberFormat;
    }

    // La zona no se guarda acá: vive en el calendario, que es el único que la usa. Tener una copia
    // sería tener dos verdades, y la que manda al formatear siempre sería la del calendario.
    public void setTimeZone(TimeZone zone) {
        this.calendar.setTimeZone(zone);
    }

    public TimeZone getTimeZone() {
        return this.calendar.getTimeZone();
    }

    public void setLenient(boolean lenient) {
        this.calendar.setLenient(lenient);
    }

    public boolean isLenient() {
        return this.calendar.isLenient();
    }

    public int hashCode() {
        return this.numberFormat.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        DateFormat other = (DateFormat) obj;
        return this.calendar.getFirstDayOfWeek() == other.calendar.getFirstDayOfWeek()
                && this.calendar.getMinimalDaysInFirstWeek() == other.calendar.getMinimalDaysInFirstWeek()
                && this.calendar.isLenient() == other.calendar.isLenient()
                // Por ID y no por equals: nuestro java.util.TimeZone no redefine equals, así que
                // dos instancias de la MISMA zona pedidas por separado saldrían distintas y dos
                // formateadores idénticos nunca serían iguales.
                && this.calendar.getTimeZone().getID().equals(other.calendar.getTimeZone().getID())
                && this.numberFormat.equals(other.numberFormat);
    }
}
