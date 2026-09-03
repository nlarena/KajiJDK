package java.sql;

/**
 * KajiLibrary's java.sql.Timestamp -- fecha, hora y **nanosegundos**.
 *
 * <p>Los nanosegundos son la razon de ser de la clase, y tambien de todos sus problemas. Se guardan
 * en un campo aparte porque `java.util.Date` solo llega al milisegundo; el heredado guarda los
 * segundos enteros y el campo nuevo la fraccion. Por eso {@link #getTime} tiene que reconstruir los
 * milisegundos sumando la fraccion.
 *
 * <p>De esa herencia sale la asimetria famosa: `equals` con un `java.util.Date` que representa el
 * mismo instante da `false` --el otro no puede tener nanos-- pero `compareTo` con el mismo objeto da
 * cero. Es una violacion del contrato de `Comparable`, esta documentada en el JDK desde siempre, y se
 * reproduce aca porque cambiarla romperia el codigo que la conoce.
 */
public class Timestamp extends java.util.Date {

    // La fraccion de segundo, de 0 a 999999999. El heredado guarda solo los segundos enteros.
    private int nanos;

    /**
     * @deprecated usar {@link #Timestamp(long)} o {@link #valueOf(java.time.LocalDateTime)}
     */
    @Deprecated
    public Timestamp(int year, int month, int date, int hour, int minute, int second, int nano) {
        super(year, month, date, hour, minute, second);
        if (nano > 999999999 || nano < 0) {
            throw new IllegalArgumentException("nanos out of range");
        }
        this.nanos = nano;
    }

    /** El instante de esos milisegundos; la fraccion se reparte entre los dos campos. */
    public Timestamp(long time) {
        super(time);
        this.repartir(time);
    }

    public void setTime(long time) {
        super.setTime(time);
        this.repartir(time);
    }

    // El heredado se queda con los segundos enteros y aca va la fraccion. El `+ 1000` es por los
    // instantes anteriores a 1970: el resto de un negativo es negativo, y los nanos no pueden serlo.
    private void repartir(long time) {
        int milis = (int) (time % 1000);
        if (milis < 0) {
            milis = milis + 1000;
        }
        this.nanos = milis * 1000000;
        super.setTime(time - milis);
    }

    /** Los milisegundos del instante, sumando la fraccion. */
    public long getTime() {
        return super.getTime() + (this.nanos / 1000000);
    }

    /** La fraccion de segundo, en nanosegundos. */
    public int getNanos() {
        return this.nanos;
    }

    /** Fija la fraccion de segundo. */
    public void setNanos(int n) {
        if (n > 999999999 || n < 0) {
            throw new IllegalArgumentException("nanos out of range");
        }
        this.nanos = n;
    }

    /**
     * El instante escrito `yyyy-mm-dd hh:mm:ss[.f...]`.
     *
     * @throws IllegalArgumentException si no tiene esa forma
     */
    public static Timestamp valueOf(String s) {
        if (s == null) {
            throw new IllegalArgumentException("null");
        }
        int espacio = s.indexOf(' ');
        if (espacio < 0) {
            throw new IllegalArgumentException(s);
        }
        Date fecha = Date.valueOf(s.substring(0, espacio));
        String resto = s.substring(espacio + 1, s.length());
        int punto = resto.indexOf('.');
        String horaSola = punto < 0 ? resto : resto.substring(0, punto);
        Time hora = Time.valueOf(horaSola);
        int nanos = 0;
        if (punto >= 0) {
            String fraccion = resto.substring(punto + 1, resto.length());
            if (fraccion.length() == 0 || fraccion.length() > 9) {
                throw new IllegalArgumentException(s);
            }
            // Se completa a nueve digitos: `.5` es medio segundo, no cinco nanosegundos.
            StringBuilder sb = new StringBuilder(fraccion);
            while (sb.length() < 9) {
                sb.append('0');
            }
            try {
                nanos = Integer.parseInt(sb.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(s);
            }
        }
        return new Timestamp(fecha.getYear(), fecha.getMonth(), fecha.getDate(), hora.getHours(),
                hora.getMinutes(), hora.getSeconds(), nanos);
    }

    /** El instante de ese {@link java.time.LocalDateTime}. */
    public static Timestamp valueOf(java.time.LocalDateTime dateTime) {
        return new Timestamp(dateTime.getYear() - 1900, dateTime.getMonthValue() - 1,
                dateTime.getDayOfMonth(), dateTime.getHour(), dateTime.getMinute(),
                dateTime.getSecond(), dateTime.getNano());
    }

    /** Este instante como {@link java.time.LocalDateTime}. */
    public java.time.LocalDateTime toLocalDateTime() {
        return java.time.LocalDateTime.of(this.getYear() + 1900, this.getMonth() + 1,
                this.getDate(), this.getHours(), this.getMinutes(), this.getSeconds(), this.nanos);
    }

    /** El instante de ese {@link java.time.Instant}, sin perder los nanosegundos. */
    public static Timestamp from(java.time.Instant instant) {
        Timestamp t = new Timestamp(instant.getEpochSecond() * 1000);
        t.setNanos(instant.getNano());
        return t;
    }

    /** Este instante como {@link java.time.Instant}, con los nanosegundos. */
    public java.time.Instant toInstant() {
        return java.time.Instant.ofEpochSecond(super.getTime() / 1000, this.nanos);
    }

    /** `yyyy-mm-dd hh:mm:ss.fffffffff`, sin los ceros finales de la fraccion. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(new Date(super.getTime()).toString());
        sb.append(' ');
        int hora = this.getHours();
        int minuto = this.getMinutes();
        int segundo = this.getSeconds();
        if (hora < 10) {
            sb.append('0');
        }
        sb.append(hora);
        sb.append(':');
        if (minuto < 10) {
            sb.append('0');
        }
        sb.append(minuto);
        sb.append(':');
        if (segundo < 10) {
            sb.append('0');
        }
        sb.append(segundo);
        sb.append('.');
        // Nueve digitos, y despues se recortan los ceros -- pero queda al menos uno, porque
        // `2020-01-01 00:00:00.` no seria un instante bien escrito.
        StringBuilder frac = new StringBuilder();
        frac.append(this.nanos);
        while (frac.length() < 9) {
            frac.insert(0, '0');
        }
        int fin = 9;
        while (fin > 1 && frac.charAt(fin - 1) == '0') {
            fin = fin - 1;
        }
        sb.append(frac.substring(0, fin));
        return sb.toString();
    }

    /** Si son el mismo instante, nanosegundos incluidos. */
    public boolean equals(Timestamp ts) {
        if (ts == null) {
            return false;
        }
        return super.getTime() == ts.getTimeInterno() && this.nanos == ts.nanos;
    }

    long getTimeInterno() {
        return super.getTime();
    }

    /**
     * Si `ts` es un `Timestamp` y son el mismo instante.
     *
     * <p>Devuelve `false` para un `java.util.Date` que represente el mismo instante, aunque
     * {@link #compareTo} devuelva cero. Es la asimetria documentada de esta clase.
     */
    public boolean equals(Object ts) {
        if (ts instanceof Timestamp) {
            return this.equals((Timestamp) ts);
        }
        return false;
    }

    public int hashCode() {
        return (int) (super.getTime() ^ (super.getTime() >>> 32));
    }

    /** Si este instante es anterior a `ts`. */
    public boolean before(Timestamp ts) {
        return this.compareTo(ts) < 0;
    }

    /** Si es posterior. */
    public boolean after(Timestamp ts) {
        return this.compareTo(ts) > 0;
    }

    public int compareTo(Timestamp ts) {
        long a = super.getTime();
        long b = ts.getTimeInterno();
        if (a != b) {
            return a < b ? -1 : 1;
        }
        if (this.nanos == ts.nanos) {
            return 0;
        }
        return this.nanos < ts.nanos ? -1 : 1;
    }

    /** Compara contra un `java.util.Date`, que no tiene nanosegundos. */
    public int compareTo(java.util.Date o) {
        if (o instanceof Timestamp) {
            return this.compareTo((Timestamp) o);
        }
        long a = this.getTime();
        long b = o.getTime();
        if (a == b) {
            return 0;
        }
        return a < b ? -1 : 1;
    }
}
