package java.util;

import java.time.ZonedDateTime;

// El calendario gregoriano: la implementacion concreta de `Calendar`.
//
// Traduce en las dos direcciones entre un instante —milisegundos desde 1970-01-01T00:00:00Z— y
// los campos civiles (año, mes, dia, hora...) en una zona horaria. Esa traduccion es todo lo que
// hace, y es menos obvia de lo que parece: los meses tienen largos distintos, los años bisiestos
// siguen tres reglas encadenadas, y la zona corre el instante antes de partirlo.
//
// **A KajiLibrary subset, y esto hay que saberlo antes de usarla con fechas antiguas:** el
// calendario es **proleptico**, o sea que aplica las reglas gregorianas hacia atras hasta el
// infinito. El JDK cambia a juliano antes del 15 de octubre de 1582 —los diez dias que el papa
// Gregorio borro— y expone ese corte con `setGregorianChange`. Aca ese corte no existe:
// `getGregorianChange()` devuelve el instante mas antiguo posible y `setGregorianChange` lo
// rechaza en vez de fingir. Para cualquier fecha posterior a 1582 no hay diferencia; para una
// anterior, esta clase da la fecha proleptica y el JDK la juliana.
//
// La aritmetica de dias es la de Howard Hinnant: exacta, sin tablas y sin bucles, sobre un
// calendario que empieza el año en marzo para que el dia bisiesto quede al final.
public class GregorianCalendar extends Calendar {

    // La era anterior al año 1.
    public static final int BC = 0;

    // La era del año 1 en adelante.
    public static final int AD = 1;

    private static final long MS_POR_DIA = 86400000L;
    private static final long MS_POR_HORA = 3600000L;
    private static final long MS_POR_MINUTO = 60000L;

    // Largo de cada mes, y de febrero en año bisiesto.
    private static final int[] LARGO_MES = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    // Un calendario con la fecha y hora actuales, en la zona y el locale por defecto.
    public GregorianCalendar() {
        this(TimeZone.getDefault(), Locale.getDefault());
    }

    public GregorianCalendar(TimeZone zone) {
        this(zone, Locale.getDefault());
    }

    public GregorianCalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }

    public GregorianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        this.setTimeInMillis(System.currentTimeMillis());
    }

    // Un calendario en la fecha dada, a medianoche. `month` es 0-based, como en todo Calendar.
    public GregorianCalendar(int year, int month, int dayOfMonth) {
        this(year, month, dayOfMonth, 0, 0, 0);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        this(year, month, dayOfMonth, hourOfDay, minute, 0);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute,
                             int second) {
        super(TimeZone.getDefault(), Locale.getDefault());
        this.set(YEAR, year);
        this.set(MONTH, month);
        this.set(DAY_OF_MONTH, dayOfMonth);
        this.set(HOUR_OF_DAY, hourOfDay);
        this.set(MINUTE, minute);
        this.set(SECOND, second);
        this.set(MILLISECOND, 0);
    }

    // ---- la aritmetica de dias ---------------------------------------------------------------

    // Dias desde 1970-01-01 para una fecha civil. `m` es 1..12.
    //
    // El truco es correr el año para que empiece en marzo: asi el 29 de febrero queda al FINAL
    // del año y el largo de los meses se vuelve una progresion regular, que es lo que permite
    // calcular el dia del año con una sola formula en vez de una tabla.
    static long diasDesdeCivil(long y, int m, int d) {
        long yy = y;
        if (m <= 2) {
            yy = yy - 1;
        }
        long era = (yy >= 0 ? yy : yy - 399) / 400;
        long yoe = yy - era * 400;
        int desplazado = m + (m > 2 ? -3 : 9);
        long doy = (153L * desplazado + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    // La inversa: fecha civil de un dia desde 1970-01-01. Devuelve { año, mes 1..12, dia }.
    static long[] civilDesdeDias(long z) {
        long zz = z + 719468;
        long era = (zz >= 0 ? zz : zz - 146096) / 146097;
        long doe = zz - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp + (mp < 10 ? 3 : -9);
        if (m <= 2) {
            y = y + 1;
        }
        long[] out = new long[3];
        out[0] = y;
        out[1] = m;
        out[2] = d;
        return out;
    }

    // Si `year` es bisiesto: divisible por 4, salvo los seculares que no lo son por 400.
    public boolean isLeapYear(int year) {
        if (year % 4 != 0) {
            return false;
        }
        if (year % 100 != 0) {
            return true;
        }
        return year % 400 == 0;
    }

    // Dias del mes `month` (0-based) del año dado.
    private int diasDelMes(int year, int month) {
        if (month == 1 && this.isLeapYear(year)) {
            return 29;
        }
        return LARGO_MES[month];
    }

    // ---- las dos traducciones ----------------------------------------------------------------

    // Instante -> campos.
    protected void computeFields() {
        int offset = this.getTimeZone().getOffset(this.time);
        long local = this.time + offset;
        long dias = Math.floorDiv(local, MS_POR_DIA);
        int msDia = (int) Math.floorMod(local, MS_POR_DIA);

        long[] ymd = civilDesdeDias(dias);
        int año = (int) ymd[0];
        int mes = (int) ymd[1] - 1;
        int dia = (int) ymd[2];

        if (año > 0) {
            this.fields[ERA] = AD;
            this.fields[YEAR] = año;
        } else {
            this.fields[ERA] = BC;
            this.fields[YEAR] = 1 - año;
        }
        this.fields[MONTH] = mes;
        this.fields[DAY_OF_MONTH] = dia;

        // El 1970-01-01 fue jueves, y THURSDAY vale 5 con SUNDAY = 1.
        this.fields[DAY_OF_WEEK] = (int) Math.floorMod(dias + 4, 7L) + 1;

        long primeroDelAño = diasDesdeCivil(ymd[0], 1, 1);
        int diaDelAño = (int) (dias - primeroDelAño) + 1;
        this.fields[DAY_OF_YEAR] = diaDelAño;
        this.fields[DAY_OF_WEEK_IN_MONTH] = (dia - 1) / 7 + 1;

        int dowPrimeroDelAño = (int) Math.floorMod(primeroDelAño + 4, 7L) + 1;
        this.fields[WEEK_OF_YEAR] = numeroDeSemana(diaDelAño, dowPrimeroDelAño);

        long primeroDelMes = diasDesdeCivil(ymd[0], (int) ymd[1], 1);
        int dowPrimeroDelMes = (int) Math.floorMod(primeroDelMes + 4, 7L) + 1;
        this.fields[WEEK_OF_MONTH] = numeroDeSemana(dia, dowPrimeroDelMes);

        int hora = msDia / (int) MS_POR_HORA;
        this.fields[HOUR_OF_DAY] = hora;
        this.fields[AM_PM] = hora < 12 ? 0 : 1;
        this.fields[HOUR] = hora % 12;
        this.fields[MINUTE] = (msDia / (int) MS_POR_MINUTO) % 60;
        this.fields[SECOND] = (msDia / 1000) % 60;
        this.fields[MILLISECOND] = msDia % 1000;
        this.fields[ZONE_OFFSET] = this.getTimeZone().getRawOffset();
        this.fields[DST_OFFSET] = offset - this.getTimeZone().getRawOffset();

        int i = 0;
        while (i < this.fields.length) {
            this.isSet[i] = true;
            i = i + 1;
        }
    }

    // El numero de semana de `diaDelPeriodo` sabiendo que dia de la semana cayo el primero.
    //
    // Las dos convenciones configurables entran aca: `firstDayOfWeek` decide donde se corta la
    // semana, y `minimalDaysInFirstWeek` decide si los primeros dias sueltos cuentan como semana
    // 1 o como la ultima del periodo anterior (y entonces esto devuelve 0).
    private int numeroDeSemana(int diaDelPeriodo, int dowDelPrimero) {
        int corrimiento = Math.floorMod(dowDelPrimero - this.getFirstDayOfWeek(), 7);
        int semana = (diaDelPeriodo + corrimiento - 1) / 7 + 1;
        if (7 - corrimiento < this.getMinimalDaysInFirstWeek()) {
            semana = semana - 1;
        }
        return semana;
    }

    // Campos -> instante.
    protected void computeTime() {
        int año = this.fields[YEAR];
        if (this.isSet[ERA] && this.fields[ERA] == BC) {
            año = 1 - año;
        }
        int mes = this.fields[MONTH];
        // Un mes fuera de 0..11 desborda al año: `set(MONTH, 12)` es enero del siguiente. Es el
        // modo `lenient`, que es el de por defecto.
        año = año + Math.floorDiv(mes, 12);
        mes = Math.floorMod(mes, 12);

        int dia = this.isSet[DAY_OF_MONTH] ? this.fields[DAY_OF_MONTH] : 1;

        int hora;
        if (this.isSet[HOUR_OF_DAY]) {
            hora = this.fields[HOUR_OF_DAY];
        } else if (this.isSet[HOUR]) {
            hora = this.fields[HOUR] + (this.isSet[AM_PM] && this.fields[AM_PM] == 1 ? 12 : 0);
        } else {
            hora = 0;
        }

        long dias = diasDesdeCivil(año, mes + 1, dia);
        long local = dias * MS_POR_DIA
            + hora * MS_POR_HORA
            + this.fields[MINUTE] * MS_POR_MINUTO
            + this.fields[SECOND] * 1000L
            + this.fields[MILLISECOND];
        this.time = local - this.getTimeZone().getRawOffset();
    }

    // ---- aritmetica sobre campos --------------------------------------------------------------

    // Suma `amount` al campo, propagando a los mas grandes.
    public void add(int field, int amount) {
        if (amount == 0) {
            return;
        }
        this.complete();
        if (field == YEAR || field == MONTH) {
            int año = this.get(YEAR);
            int mes = this.get(MONTH);
            int dia = this.get(DAY_OF_MONTH);
            if (field == YEAR) {
                año = año + amount;
            } else {
                int total = año * 12 + mes + amount;
                año = Math.floorDiv(total, 12);
                mes = Math.floorMod(total, 12);
            }
            // El recorte va ANTES de escribir los campos, no despues.
            //
            // Si se escribe "31 de febrero" y recien despues se mira, `computeTime` ya lo
            // convirtio en el 2 de marzo y no queda rastro de que hubo desborde: el dia 2 es
            // perfectamente valido en marzo. El JDK da 29 de febrero, y esa es la semantica que
            // importa — sumar un mes no deberia saltar dos.
            int max = this.diasDelMes(año, mes);
            if (dia > max) {
                dia = max;
            }
            this.set(YEAR, año);
            this.set(MONTH, mes);
            this.set(DAY_OF_MONTH, dia);
            return;
        }
        long delta;
        if (field == DAY_OF_MONTH || field == DAY_OF_YEAR || field == DAY_OF_WEEK
                || field == DAY_OF_WEEK_IN_MONTH) {
            delta = (long) amount * MS_POR_DIA;
        } else if (field == WEEK_OF_YEAR || field == WEEK_OF_MONTH) {
            delta = (long) amount * 7 * MS_POR_DIA;
        } else if (field == HOUR || field == HOUR_OF_DAY) {
            delta = (long) amount * MS_POR_HORA;
        } else if (field == MINUTE) {
            delta = (long) amount * MS_POR_MINUTO;
        } else if (field == SECOND) {
            delta = (long) amount * 1000L;
        } else if (field == MILLISECOND) {
            delta = amount;
        } else {
            throw new IllegalArgumentException("" + field);
        }
        this.setTimeInMillis(this.getTimeInMillis() + delta);
    }

    // Suma 1 (o resta 1) al campo SIN tocar los mas grandes.
    public void roll(int field, boolean up) {
        this.roll(field, up ? 1 : -1);
    }

    // Suma `amount` al campo sin tocar los mas grandes, dando la vuelta dentro de su rango.
    public void roll(int field, int amount) {
        if (amount == 0) {
            return;
        }
        this.complete();
        int min = this.getActualMinimum(field);
        int max = this.getActualMaximum(field);
        int rango = max - min + 1;
        int valor = this.get(field);
        int nuevo = Math.floorMod(valor - min + amount, rango) + min;
        if (field == YEAR || field == MONTH) {
            // Mismo cuidado que en `add`: recortar el dia antes de escribir, no despues.
            int año = field == YEAR ? nuevo : this.get(YEAR);
            int mes = field == MONTH ? nuevo : this.get(MONTH);
            int dia = this.get(DAY_OF_MONTH);
            int max = this.diasDelMes(año, mes);
            if (dia > max) {
                dia = max;
            }
            this.set(YEAR, año);
            this.set(MONTH, mes);
            this.set(DAY_OF_MONTH, dia);
            return;
        }
        this.set(field, nuevo);
    }

    // ---- rangos de los campos ------------------------------------------------------------------

    public int getMinimum(int field) {
        if (field == ERA) {
            return BC;
        }
        if (field == YEAR) {
            return 1;
        }
        if (field == MONTH || field == HOUR || field == HOUR_OF_DAY || field == MINUTE
                || field == SECOND || field == MILLISECOND || field == AM_PM) {
            return 0;
        }
        if (field == ZONE_OFFSET) {
            return -50400000;
        }
        if (field == DST_OFFSET) {
            return 0;
        }
        return 1;
    }

    public int getMaximum(int field) {
        if (field == ERA) {
            return AD;
        }
        if (field == YEAR) {
            return 292278994;
        }
        if (field == MONTH) {
            return 11;
        }
        if (field == WEEK_OF_YEAR) {
            return 53;
        }
        if (field == WEEK_OF_MONTH) {
            return 6;
        }
        if (field == DAY_OF_MONTH) {
            return 31;
        }
        if (field == DAY_OF_YEAR) {
            return 366;
        }
        if (field == DAY_OF_WEEK) {
            return 7;
        }
        if (field == DAY_OF_WEEK_IN_MONTH) {
            return 6;
        }
        if (field == AM_PM) {
            return 1;
        }
        if (field == HOUR) {
            return 11;
        }
        if (field == HOUR_OF_DAY) {
            return 23;
        }
        if (field == MINUTE || field == SECOND) {
            return 59;
        }
        if (field == MILLISECOND) {
            return 999;
        }
        if (field == ZONE_OFFSET) {
            return 50400000;
        }
        return 7200000;
    }

    // El mayor valor que el campo alcanza en TODOS los casos.
    //
    // Distinto de `getMaximum`: DAY_OF_MONTH llega a 31 en algun mes, pero 28 es el unico que
    // esta garantizado en todos. Un codigo que quiera un dia valido para cualquier mes tiene que
    // usar este.
    public int getLeastMaximum(int field) {
        if (field == DAY_OF_MONTH) {
            return 28;
        }
        if (field == DAY_OF_YEAR) {
            return 365;
        }
        if (field == WEEK_OF_YEAR) {
            return 52;
        }
        if (field == WEEK_OF_MONTH) {
            return 4;
        }
        if (field == DAY_OF_WEEK_IN_MONTH) {
            return 4;
        }
        return this.getMaximum(field);
    }

    public int getGreatestMinimum(int field) {
        return this.getMinimum(field);
    }

    public int getActualMinimum(int field) {
        return this.getMinimum(field);
    }

    // El mayor valor del campo EN ESTA fecha: es aca donde DAY_OF_MONTH devuelve 28, 29, 30 o 31.
    public int getActualMaximum(int field) {
        this.complete();
        if (field == DAY_OF_MONTH) {
            return this.diasDelMes(this.get(YEAR), this.get(MONTH));
        }
        if (field == DAY_OF_YEAR) {
            return this.isLeapYear(this.get(YEAR)) ? 366 : 365;
        }
        if (field == DAY_OF_WEEK_IN_MONTH) {
            return (this.diasDelMes(this.get(YEAR), this.get(MONTH)) - 1) / 7 + 1;
        }
        return this.getMaximum(field);
    }

    // ---- el corte juliano/gregoriano, que aca no existe ----------------------------------------

    // Rechaza cambiar el corte.
    //
    // A KajiLibrary subset: el calendario es proleptico, sin corte. Lanzar es preferible a
    // aceptar la llamada y seguir dando fechas prolepticas, que es lo que haria un no-op: el
    // llamador creeria tener fechas julianas y no las tendria.
    public void setGregorianChange(Date date) {
        throw new UnsupportedOperationException(
            "KajiLibrary usa un calendario gregoriano proleptico, sin corte juliano");
    }

    // El instante del corte. Al ser proleptico, el mas antiguo representable.
    public final Date getGregorianChange() {
        return new Date(-9223372036854775808L);
    }

    public String getCalendarType() {
        return "gregory";
    }

    // ---- fecha por semana ISO -----------------------------------------------------------------

    public final boolean isWeekDateSupported() {
        return true;
    }

    // El año al que pertenece la semana de esta fecha, que no siempre es el año calendario: el 1
    // de enero puede caer en la ultima semana del año anterior.
    public int getWeekYear() {
        this.complete();
        int semana = this.get(WEEK_OF_YEAR);
        int mes = this.get(MONTH);
        if (semana >= 52 && mes == 0) {
            return this.get(YEAR) - 1;
        }
        if (semana == 1 && mes == 11) {
            return this.get(YEAR) + 1;
        }
        return this.get(YEAR);
    }

    public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) {
        if (dayOfWeek < SUNDAY || dayOfWeek > SATURDAY) {
            throw new IllegalArgumentException("invalid dayOfWeek: " + dayOfWeek);
        }
        this.set(YEAR, weekYear);
        this.set(MONTH, 0);
        this.set(DAY_OF_MONTH, 1);
        this.complete();
        int dowDelPrimero = this.get(DAY_OF_WEEK);
        int corrimiento = Math.floorMod(dowDelPrimero - this.getFirstDayOfWeek(), 7);
        int diaDelAño = (weekOfYear - 1) * 7 + Math.floorMod(dayOfWeek - this.getFirstDayOfWeek(), 7)
            - corrimiento + 1;
        if (7 - corrimiento < this.getMinimalDaysInFirstWeek()) {
            diaDelAño = diaDelAño + 7;
        }
        this.set(DAY_OF_MONTH, 1);
        this.setTimeInMillis(this.getTimeInMillis() + (long) (diaDelAño - 1) * MS_POR_DIA);
    }

    public int getWeeksInWeekYear() {
        this.complete();
        int año = this.getWeekYear();
        GregorianCalendar fin = new GregorianCalendar(año, 11, 31);
        fin.setFirstDayOfWeek(this.getFirstDayOfWeek());
        fin.setMinimalDaysInFirstWeek(this.getMinimalDaysInFirstWeek());
        int semana = fin.get(WEEK_OF_YEAR);
        if (semana == 1) {
            return 52;
        }
        return semana;
    }

    // ---- igualdad, copia y puentes con java.time ----------------------------------------------

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GregorianCalendar)) {
            return false;
        }
        GregorianCalendar that = (GregorianCalendar) obj;
        return this.getTimeInMillis() == that.getTimeInMillis()
            && this.getTimeZone().equals(that.getTimeZone());
    }

    public int hashCode() {
        long t = this.getTimeInMillis();
        return (int) (t ^ (t >>> 32));
    }

    public Object clone() {
        GregorianCalendar copia = new GregorianCalendar(this.getTimeZone(), Locale.getDefault());
        copia.setTimeInMillis(this.getTimeInMillis());
        copia.setFirstDayOfWeek(this.getFirstDayOfWeek());
        copia.setMinimalDaysInFirstWeek(this.getMinimalDaysInFirstWeek());
        return copia;
    }

    // Esta fecha como ZonedDateTime.
    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.ofInstant(this.toInstant(), this.getTimeZone().toZoneId());
    }

    // Un calendario en el instante y la zona del ZonedDateTime dado.
    public static GregorianCalendar from(ZonedDateTime zdt) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone(zdt.getZone()),
            Locale.getDefault());
        cal.setTimeInMillis(zdt.toInstant().toEpochMilli());
        return cal;
    }
}
