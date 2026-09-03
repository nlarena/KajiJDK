package java.sql;

/**
 * KajiLibrary's java.sql.Date -- una fecha **sin hora**, para una columna `DATE`.
 *
 * <p>Hereda de `java.util.Date`, que si tiene hora, y por eso el contrato pide que los milisegundos
 * de la hora esten en **cero** en la zona horaria por omision. Un `java.sql.Date` con hora adentro no
 * es un error que salte: es un valor que compara mal contra el mismo dia guardado bien.
 *
 * <p>De ahi que {@link #getHours} y compania **fallen** en vez de devolver cero. Cero seria una
 * respuesta, y la respuesta correcta es que la pregunta no aplica -- una fecha SQL no tiene hora, y
 * decir "medianoche" invitaria a hacer cuentas con eso.
 *
 * <p>Lo mismo {@link #toInstant}: un instante es un punto en la linea del tiempo y una fecha no lo
 * es; para convertir hace falta una zona horaria, que esta clase no tiene. El JDK tambien falla aca,
 * por la misma razon.
 */
public class Date extends java.util.Date {

    /**
     * La fecha de ese anio (desde 1900), mes (desde cero) y dia.
     *
     * @deprecated usar {@link #Date(long)} o {@link #valueOf(java.time.LocalDate)}
     */
    @Deprecated
    public Date(int year, int month, int day) {
        super(year, month, day);
    }

    /** La fecha de ese instante en milisegundos. */
    public Date(long date) {
        super(date);
    }

    public void setTime(long date) {
        super.setTime(date);
    }

    /**
     * La fecha escrita `yyyy-[m]m-[d]d`.
     *
     * @throws IllegalArgumentException si no tiene esa forma
     */
    public static Date valueOf(String s) {
        if (s == null) {
            throw new IllegalArgumentException("null");
        }
        int primera = s.indexOf('-');
        int segunda = primera < 0 ? -1 : s.indexOf('-', primera + 1);
        if (primera <= 0 || segunda <= primera + 1 || segunda == s.length() - 1) {
            throw new IllegalArgumentException(s);
        }
        int anio;
        int mes;
        int dia;
        try {
            anio = Integer.parseInt(s.substring(0, primera));
            mes = Integer.parseInt(s.substring(primera + 1, segunda));
            dia = Integer.parseInt(s.substring(segunda + 1, s.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(s);
        }
        return new Date(anio - 1900, mes - 1, dia);
    }

    /** La fecha de ese {@link java.time.LocalDate}. */
    public static Date valueOf(java.time.LocalDate date) {
        return new Date(date.getYear() - 1900, date.getMonthValue() - 1, date.getDayOfMonth());
    }

    /** Esta fecha como {@link java.time.LocalDate}. */
    public java.time.LocalDate toLocalDate() {
        return java.time.LocalDate.of(this.getYear() + 1900, this.getMonth() + 1, this.getDate());
    }

    /** `yyyy-mm-dd`, con ceros a la izquierda. */
    public String toString() {
        int anio = this.getYear() + 1900;
        int mes = this.getMonth() + 1;
        int dia = this.getDate();
        StringBuilder sb = new StringBuilder();
        sb.append(anio);
        sb.append('-');
        if (mes < 10) {
            sb.append('0');
        }
        sb.append(mes);
        sb.append('-');
        if (dia < 10) {
            sb.append('0');
        }
        sb.append(dia);
        return sb.toString();
    }

    // ---- lo que no aplica ----------------------------------------------------------------------------

    /** @throws IllegalArgumentException siempre: una fecha SQL no tiene hora */
    public int getHours() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public int getMinutes() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public int getSeconds() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public void setHours(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public void setMinutes(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException siempre */
    public void setSeconds(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws UnsupportedOperationException siempre: falta la zona horaria */
    public java.time.Instant toInstant() {
        throw new UnsupportedOperationException();
    }
}
