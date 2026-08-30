package java.util;

// KajiLibrary's java.util.Date (finding #267).
//
// It exists because `jakarta.persistence.Query` binds parameters of it, next to the Calendar
// overloads -- but unlike Calendar this one is not a type slot: a Date IS just a `long`, so the
// whole of its non-deprecated surface can be written honestly.
//
// What it deliberately does NOT have: the year/month/day/hours/minutes/seconds accessors and the
// `Date(int, int, int)` constructors. Every one of them is deprecated in the JDK precisely because
// it reads a wall-clock field out of an instant, which cannot be done without a TimeZone -- and
// TimeZone does not exist here. `toString()` is the same problem and is answered the only way that
// stays true: the instant, not a rendering of it in a zone we do not have.
//
// A missing member is a legal subset; a member that lies is not.
public class Date implements Comparable<Date> {

    /** Milliseconds since the epoch. The whole state of a Date. */
    private long fastTime;

    /** Now. */
    public Date() {
        this.fastTime = System.currentTimeMillis();
    }

    /** The instant {@code date} milliseconds after the epoch. */
    public Date(long date) {
        this.fastTime = date;
    }

    /**
     * La fecha con esos campos, en la **zona local**, con la hora en cero.
     *
     * <p>Todos estos constructores y los get/set de abajo estan **desaconsejados desde Java 1.1**, y
     * la razon esta a la vista en la firma: el `year` es el año menos 1900 y el `month` va de 0 a 11.
     * Dos convenciones distintas en la misma llamada, y las dos sorprenden. `new Date(99, 11, 31)` es
     * el 31 de diciembre de 1999.
     *
     * <p>Se implementan igual porque son parte del contrato y hay codigo que los usa. Se apoyan en
     * `GregorianCalendar`, que es donde vive el calendario de verdad -- reescribir la aritmetica de
     * fechas aca seria tener dos, y que se contradigan.
     */
    public Date(int year, int month, int date) {
        this(year, month, date, 0, 0, 0);
    }

    /** Idem, con hora y minuto. */
    public Date(int year, int month, int date, int hrs, int min) {
        this(year, month, date, hrs, min, 0);
    }

    /** Idem, con segundos. */
    public Date(int year, int month, int date, int hrs, int min, int sec) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.set(year + 1900, month, date, hrs, min, sec);
        this.fastTime = cal.getTimeInMillis();
    }

    /**
     * La fecha que representa `s`.
     *
     * @deprecated Depende del formato del texto, que nunca estuvo bien especificado. Se delega en
     *             {@link #parse(String)}, que documenta que reconoce.
     */
    public Date(String s) {
        this.fastTime = Date.parse(s);
    }

    /** La fecha equivalente a `instant`. */
    public static Date from(java.time.Instant instant) {
        if (instant == null) {
            throw new NullPointerException();
        }
        return new Date(instant.toEpochMilli());
    }

    /** Este instante, como `Instant`. */
    public java.time.Instant toInstant() {
        return java.time.Instant.ofEpochMilli(this.fastTime);
    }

    // ---- los campos, en la zona local -----------------------------------------------------------
    //
    // Cada uno arma un `GregorianCalendar` sobre el instante y lee el campo. Es lo que hace el JDK, y
    // es caro: seis llamadas seguidas construyen seis calendarios. La alternativa --cachear uno-- lo
    // volveria mutable compartido, que es peor en una clase que ya es mutable. Estos metodos estan
    // desaconsejados desde 1997; optimizarlos seria invitar a usarlos.

    private GregorianCalendar cal() {
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(this.fastTime);
        return c;
    }

    private void setCampo(int campo, int valor) {
        GregorianCalendar c = this.cal();
        c.set(campo, valor);
        this.fastTime = c.getTimeInMillis();
    }

    /** El año menos 1900. */
    public int getYear() {
        return this.cal().get(Calendar.YEAR) - 1900;
    }

    /** Pone el año, dado como año menos 1900. */
    public void setYear(int year) {
        this.setCampo(Calendar.YEAR, year + 1900);
    }

    /** El mes, de 0 (enero) a 11. */
    public int getMonth() {
        return this.cal().get(Calendar.MONTH);
    }

    public void setMonth(int month) {
        this.setCampo(Calendar.MONTH, month);
    }

    /** El dia del mes, de 1 a 31. */
    public int getDate() {
        return this.cal().get(Calendar.DAY_OF_MONTH);
    }

    public void setDate(int date) {
        this.setCampo(Calendar.DAY_OF_MONTH, date);
    }

    /**
     * El dia de la semana, de 0 (domingo) a 6.
     *
     * <p>Ojo con la resta: `Calendar.DAY_OF_WEEK` numera desde 1 y este metodo desde 0. Es la clase
     * de descuido que da un dia corrido en produccion y no en la prueba.
     */
    public int getDay() {
        return this.cal().get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
    }

    /** La hora, de 0 a 23. */
    public int getHours() {
        return this.cal().get(Calendar.HOUR_OF_DAY);
    }

    public void setHours(int hours) {
        this.setCampo(Calendar.HOUR_OF_DAY, hours);
    }

    public int getMinutes() {
        return this.cal().get(Calendar.MINUTE);
    }

    public void setMinutes(int minutes) {
        this.setCampo(Calendar.MINUTE, minutes);
    }

    public int getSeconds() {
        return this.cal().get(Calendar.SECOND);
    }

    public void setSeconds(int seconds) {
        this.setCampo(Calendar.SECOND, seconds);
    }

    /**
     * Los minutos que hay que **restarle** a UTC para llegar a la hora local de este instante.
     *
     * <p>El signo esta al reves de lo que uno diria: para UTC-3 devuelve **180**, no -180. Es asi
     * desde 1995 y no se puede arreglar sin romper a todo el que lo use.
     */
    public int getTimezoneOffset() {
        GregorianCalendar c = this.cal();
        return -(c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)) / 60000;
    }

    /**
     * Los milisegundos desde la epoca para esa fecha **en UTC**.
     *
     * <p>Es el hermano UTC de los constructores de arriba, con las mismas dos convenciones raras
     * (año menos 1900, mes desde 0).
     */
    public static long UTC(int year, int month, int date, int hrs, int min, int sec) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year + 1900, month, date, hrs, min, sec);
        return cal.getTimeInMillis();
    }

    /**
     * Interpreta `s` como fecha y devuelve los milisegundos desde la epoca.
     *
     * <p>Reconoce la forma que produce {@link #toString()} --`EEE MMM d HH:mm:ss zzz yyyy`-- y la de
     * {@link #toGMTString()}. **No** intenta cubrir las decenas de formas sueltas que acepta el JDK:
     * su javadoc las describe en prosa, y una descripcion en prosa no es una especificacion. Antes
     * que adivinar mal en silencio, lo que no encaja se rechaza.
     *
     * @throws IllegalArgumentException si no se reconoce el formato
     */
    public static long parse(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        String[] partes = s.trim().split(" +");
        // `EEE MMM d HH:mm:ss zzz yyyy` -- lo que devuelve toString().
        if (partes.length == 6 && partes[3].indexOf(':') >= 0) {
            int mes = mesPorNombre(partes[1]);
            int dia = Integer.parseInt(partes[2]);
            String[] hms = partes[3].split(":");
            int anio = Integer.parseInt(partes[5]);
            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone(partes[4]));
            cal.clear();
            cal.set(anio, mes, dia, Integer.parseInt(hms[0]), Integer.parseInt(hms[1]),
                    Integer.parseInt(hms[2]));
            return cal.getTimeInMillis();
        }
        // `d MMM yyyy HH:mm:ss GMT` -- lo que devuelve toGMTString().
        if (partes.length == 5 && "GMT".equals(partes[4])) {
            int dia = Integer.parseInt(partes[0]);
            int mes = mesPorNombre(partes[1]);
            int anio = Integer.parseInt(partes[2]);
            String[] hms = partes[3].split(":");
            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.clear();
            cal.set(anio, mes, dia, Integer.parseInt(hms[0]), Integer.parseInt(hms[1]),
                    Integer.parseInt(hms[2]));
            return cal.getTimeInMillis();
        }
        throw new IllegalArgumentException(s);
    }

    private static int mesPorNombre(String nombre) {
        String[] meses = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                       "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int i = 0;
        while (i < meses.length) {
            if (meses[i].equalsIgnoreCase(nombre)) {
                return i;
            }
            i = i + 1;
        }
        throw new IllegalArgumentException(nombre);
    }

    /**
     * `d MMM yyyy HH:mm:ss GMT`.
     *
     * @deprecated El nombre miente por partida doble: no es GMT sino UTC, y el formato no es el de
     *             ningun estandar. Se conserva porque {@link #parse(String)} lo tiene que leer.
     */
    public String toGMTString() {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(this.fastTime);
        String[] meses = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                       "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        StringBuilder sb = new StringBuilder();
        sb.append(cal.get(Calendar.DAY_OF_MONTH));
        sb.append(' ');
        sb.append(meses[cal.get(Calendar.MONTH)]);
        sb.append(' ');
        sb.append(cal.get(Calendar.YEAR));
        sb.append(' ');
        dosDigitos(sb, cal.get(Calendar.HOUR_OF_DAY));
        sb.append(':');
        dosDigitos(sb, cal.get(Calendar.MINUTE));
        sb.append(':');
        dosDigitos(sb, cal.get(Calendar.SECOND));
        sb.append(" GMT");
        return sb.toString();
    }

    /**
     * La fecha en el formato de la region por defecto.
     *
     * @deprecated En esta biblioteca devuelve la misma forma que {@link #toGMTString()} pero en hora
     *             local, porque no hay un formateador sensible a la region al que delegarle. Se
     *             documenta en vez de fingir: el JDK usa `DateFormat`, que aca no existe.
     */
    public String toLocaleString() {
        GregorianCalendar cal = this.cal();
        String[] meses = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                       "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        StringBuilder sb = new StringBuilder();
        sb.append(cal.get(Calendar.DAY_OF_MONTH));
        sb.append(' ');
        sb.append(meses[cal.get(Calendar.MONTH)]);
        sb.append(' ');
        sb.append(cal.get(Calendar.YEAR));
        sb.append(' ');
        dosDigitos(sb, cal.get(Calendar.HOUR_OF_DAY));
        sb.append(':');
        dosDigitos(sb, cal.get(Calendar.MINUTE));
        sb.append(':');
        dosDigitos(sb, cal.get(Calendar.SECOND));
        return sb.toString();
    }

    private static void dosDigitos(StringBuilder sb, int n) {
        if (n < 10) {
            sb.append('0');
        }
        sb.append(n);
    }

    public long getTime() {
        return this.fastTime;
    }

    public void setTime(long time) {
        this.fastTime = time;
    }

    public boolean before(Date when) {
        return this.fastTime < when.fastTime;
    }

    public boolean after(Date when) {
        return this.fastTime > when.fastTime;
    }

    @Override
    public int compareTo(Date other) {
        if (this.fastTime < other.fastTime) {
            return -1;
        }
        if (this.fastTime > other.fastTime) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Date && this.fastTime == ((Date) other).fastTime;
    }

    @Override
    public int hashCode() {
        return (int) (this.fastTime ^ (this.fastTime >>> 32));
    }

    /**
     * The instant in milliseconds since the epoch.
     *
     * <p>NOT the JDK's format. The JDK renders {@code "EEE MMM dd HH:mm:ss zzz yyyy"} in the
     * default time zone, and there is no TimeZone here to render it in -- so rather than print a
     * wall clock that would silently be UTC and claim otherwise, this prints the one thing a Date
     * actually knows.
     */
    @Override
    public String toString() {
        return "Date(" + this.fastTime + ")";
    }
}
