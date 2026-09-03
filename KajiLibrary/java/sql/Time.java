package java.sql;

/**
 * KajiLibrary's java.sql.Time -- una hora **sin fecha**, para una columna `TIME`.
 *
 * <p>El reverso exacto de {@link Date}: hereda de `java.util.Date` y el contrato pide que la parte de
 * fecha sea el 1 de enero de 1970. Y por la misma razon {@link #getYear} y compania fallan -- "1970"
 * seria una respuesta inventada a una pregunta que no aplica.
 */
public class Time extends java.util.Date {

    /**
     * La hora de esa hora, minuto y segundo.
     *
     * @deprecated usar {@link #Time(long)} o {@link #valueOf(java.time.LocalTime)}
     */
    @Deprecated
    public Time(int hour, int minute, int second) {
        super(70, 0, 1, hour, minute, second);
    }

    /** La hora de ese instante en milisegundos. */
    public Time(long time) {
        super(time);
    }

    public void setTime(long time) {
        super.setTime(time);
    }

    /**
     * La hora escrita `hh:mm:ss`.
     *
     * @throws IllegalArgumentException si no tiene esa forma
     */
    public static Time valueOf(String s) {
        if (s == null) {
            throw new IllegalArgumentException("null");
        }
        int primera = s.indexOf(':');
        int segunda = primera < 0 ? -1 : s.indexOf(':', primera + 1);
        if (primera <= 0 || segunda <= primera + 1 || segunda == s.length() - 1) {
            throw new IllegalArgumentException(s);
        }
        int hora;
        int minuto;
        int segundo;
        try {
            hora = Integer.parseInt(s.substring(0, primera));
            minuto = Integer.parseInt(s.substring(primera + 1, segunda));
            segundo = Integer.parseInt(s.substring(segunda + 1, s.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(s);
        }
        return new Time(hora, minuto, segundo);
    }

    /** La hora de ese {@link java.time.LocalTime}. */
    public static Time valueOf(java.time.LocalTime time) {
        return new Time(time.getHour(), time.getMinute(), time.getSecond());
    }

    /** Esta hora como {@link java.time.LocalTime}. */
    public java.time.LocalTime toLocalTime() {
        return java.time.LocalTime.of(this.getHours(), this.getMinutes(), this.getSeconds());
    }

    /** `hh:mm:ss`, con ceros a la izquierda. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        dosDigitos(sb, this.getHours());
        sb.append(':');
        dosDigitos(sb, this.getMinutes());
        sb.append(':');
        dosDigitos(sb, this.getSeconds());
        return sb.toString();
    }

    private static void dosDigitos(StringBuilder sb, int v) {
        if (v < 10) {
            sb.append('0');
        }
        sb.append(v);
    }

    // ---- lo que no aplica ----------------------------------------------------------------------------

    /** @throws IllegalArgumentException siempre: una hora SQL no tiene fecha */
    public int getYear() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public int getMonth() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public int getDay() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public int getDate() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public void setYear(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public void setMonth(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public void setDate(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws UnsupportedOperationException siempre: falta la fecha */
    public java.time.Instant toInstant() {
        throw new UnsupportedOperationException();
    }
}
