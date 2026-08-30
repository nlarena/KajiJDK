package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

// Una zona horaria con reglas de horario de verano expresadas como dos fechas del anio: cuando
// empieza y cuando termina. Es la unica TimeZone concreta y publica del JDK, y la unica forma de
// escribir una zona a mano sin la base de datos IANA.
//
// Lo que la hace util -- y lo que la hace complicada -- es que las reglas no se dan como fechas
// fijas sino como **patrones**, porque las transiciones reales caen en dias de la semana: "el
// segundo domingo de marzo", "el ultimo domingo de octubre". Hay cuatro formas de decirlo, y el
// JDK las codifica en los signos de dos enteros en vez de tener cuatro campos:
//
//   dia   dow   modo               ejemplo
//   ---   ---   ----------------   ----------------------------------------------------------
//    >0     0   DOM                el dia 15 del mes
//    >0    >0   DOW_IN_MONTH       el 2do domingo (dia=2, dow=DOMINGO)
//    <0    >0   DOW_IN_MONTH       el ULTIMO domingo (dia=-1)
//    >0    <0   DOW_GE_DOM         el primer domingo EN O DESPUES del dia 8
//    <0    <0   DOW_LE_DOM         el ultimo domingo EN O ANTES del dia 21
//
// Esa codificacion es historia, no diseno, pero es contrato: los constructores publicos reciben
// esos enteros y hay que decodificarlos igual que el JDK. Los `setStartRule`/`setEndRule` son la
// cara legible de lo mismo.
//
// El otro detalle que se pasa por alto es el **modo de la hora**. `startTime` puede estar dado en
// hora de pared (lo normal), en hora estandar, o en UTC, y los tres significan instantes distintos
// -- justamente porque el reloj salta en ese momento. Aca se normaliza todo a hora **estandar
// local**, que es el reloj en el que llegan los argumentos de `getOffset`.
//
// **Divergencia deliberada**: el JDK acepta `startYear` y aplica la regla solo desde ese anio en
// adelante, pero **no** modela transiciones historicas -- una SimpleTimeZone dice lo mismo para
// 1970 que para 2030. Esta tambien. Para fechas historicas de verdad hace falta la tzdb, y no la
// hay (ver la nota de FixedTimeZone).
public class SimpleTimeZone extends TimeZone {

    // Los tres modos en que se puede dar la hora de una regla.
    public static final int WALL_TIME = 0;
    public static final int STANDARD_TIME = 1;
    public static final int UTC_TIME = 2;

    // Los cuatro modos de regla, decodificados de los signos. Internos.
    private static final int DOM_MODE = 1;
    private static final int DOW_IN_MONTH_MODE = 2;
    private static final int DOW_GE_DOM_MODE = 3;
    private static final int DOW_LE_DOM_MODE = 4;

    private static final int MS_POR_DIA = 86400000;
    private static final int UNA_HORA = 3600000;

    private static final int[] LARGO_MES = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    private int rawOffset;
    private int dstSavings;

    private int startMonth;
    private int startDay;
    private int startDayOfWeek;
    private int startTime;
    private int startTimeMode;

    private int endMonth;
    private int endDay;
    private int endDayOfWeek;
    private int endTime;
    private int endTimeMode;

    // El primer anio en el que rige la regla. Antes de el, la zona es de offset constante.
    private int startYear;

    private boolean useDaylight;
    private int startMode;
    private int endMode;

    // Una zona sin horario de verano: offset constante.
    public SimpleTimeZone(int rawOffset, String ID) {
        this.rawOffset = rawOffset;
        this.setID(ID);
        this.dstSavings = UNA_HORA;
        this.useDaylight = false;
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay,
            int startDayOfWeek, int startTime, int endMonth, int endDay, int endDayOfWeek,
            int endTime) {
        this(rawOffset, ID, startMonth, startDay, startDayOfWeek, startTime, WALL_TIME,
                endMonth, endDay, endDayOfWeek, endTime, WALL_TIME, UNA_HORA);
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay,
            int startDayOfWeek, int startTime, int endMonth, int endDay, int endDayOfWeek,
            int endTime, int dstSavings) {
        this(rawOffset, ID, startMonth, startDay, startDayOfWeek, startTime, WALL_TIME,
                endMonth, endDay, endDayOfWeek, endTime, WALL_TIME, dstSavings);
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay,
            int startDayOfWeek, int startTime, int startTimeMode, int endMonth, int endDay,
            int endDayOfWeek, int endTime, int endTimeMode, int dstSavings) {
        this.rawOffset = rawOffset;
        this.setID(ID);
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime = startTime;
        this.startTimeMode = startTimeMode;
        this.endMonth = endMonth;
        this.endDay = endDay;
        this.endDayOfWeek = endDayOfWeek;
        this.endTime = endTime;
        this.endTimeMode = endTimeMode;
        this.dstSavings = dstSavings;
        this.startYear = 0;
        this.decodeRules();
    }

    // ---- decodificacion de las reglas -----------------------------------------------------------

    // Traduce los signos de (dia, diaDeSemana) a uno de los cuatro modos, **normalizando** los dos
    // campos a positivos donde corresponde. Es destructivo a proposito: el JDK guarda los valores
    // ya decodificados, y `hasSameRules` los compara asi.
    private void decodeRules() {
        this.useDaylight = this.startDay != 0 && this.endDay != 0;
        if (!this.useDaylight) {
            return;
        }
        this.startMode = this.decodeUna(true);
        this.endMode = this.decodeUna(false);
    }

    private int decodeUna(boolean esInicio) {
        int dia = esInicio ? this.startDay : this.endDay;
        int dow = esInicio ? this.startDayOfWeek : this.endDayOfWeek;
        int mes = esInicio ? this.startMonth : this.endMonth;
        if (mes < Calendar.JANUARY || mes > Calendar.DECEMBER) {
            throw new IllegalArgumentException("Illegal month " + mes);
        }
        int modo;
        if (dow == 0) {
            modo = DOM_MODE;
        } else if (dow > 0) {
            modo = DOW_IN_MONTH_MODE;
        } else {
            // dow negativo: "en o despues" si el dia es positivo, "en o antes" si es negativo.
            dow = -dow;
            if (dia > 0) {
                modo = DOW_GE_DOM_MODE;
            } else {
                dia = -dia;
                modo = DOW_LE_DOM_MODE;
            }
        }
        if (dow > Calendar.SATURDAY) {
            throw new IllegalArgumentException("Illegal day of week " + dow);
        }
        if (modo == DOW_IN_MONTH_MODE) {
            if (dia < -5 || dia > 5) {
                throw new IllegalArgumentException("Illegal day of week in month " + dia);
            }
        } else if (dia < 1 || dia > LARGO_MES[mes]) {
            throw new IllegalArgumentException("Illegal day " + dia);
        }
        if (esInicio) {
            this.startDay = dia;
            this.startDayOfWeek = dow;
        } else {
            this.endDay = dia;
            this.endDayOfWeek = dow;
        }
        return modo;
    }

    // ---- las reglas, en su forma legible ---------------------------------------------------------

    public void setStartRule(int startMonth, int startDay, int startDayOfWeek, int startTime) {
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime = startTime;
        this.startTimeMode = WALL_TIME;
        this.decodeRules();
    }

    // Un dia exacto del mes: sin dia de semana.
    public void setStartRule(int startMonth, int startDay, int startTime) {
        this.setStartRule(startMonth, startDay, 0, startTime);
    }

    // El primer `dayOfWeek` en o **despues** del dia (`after`), o el ultimo en o **antes**.
    public void setStartRule(int startMonth, int startDay, int startDayOfWeek, int startTime,
            boolean after) {
        if (after) {
            this.setStartRule(startMonth, startDay, -startDayOfWeek, startTime);
        } else {
            this.setStartRule(startMonth, -startDay, -startDayOfWeek, startTime);
        }
    }

    public void setEndRule(int endMonth, int endDay, int endDayOfWeek, int endTime) {
        this.endMonth = endMonth;
        this.endDay = endDay;
        this.endDayOfWeek = endDayOfWeek;
        this.endTime = endTime;
        this.endTimeMode = WALL_TIME;
        this.decodeRules();
    }

    public void setEndRule(int endMonth, int endDay, int endTime) {
        this.setEndRule(endMonth, endDay, 0, endTime);
    }

    public void setEndRule(int endMonth, int endDay, int endDayOfWeek, int endTime,
            boolean after) {
        if (after) {
            this.setEndRule(endMonth, endDay, -endDayOfWeek, endTime);
        } else {
            this.setEndRule(endMonth, -endDay, -endDayOfWeek, endTime);
        }
    }

    public void setStartYear(int year) {
        this.startYear = year;
    }

    // ---- offsets ---------------------------------------------------------------------------------

    public void setRawOffset(int offsetMillis) {
        this.rawOffset = offsetMillis;
    }

    public int getRawOffset() {
        return this.rawOffset;
    }

    // Cuanto adelanta el reloj durante el horario de verano. Cero si esta zona no lo usa.
    public int getDSTSavings() {
        if (this.useDaylight) {
            return this.dstSavings;
        }
        return 0;
    }

    public void setDSTSavings(int millisSavedDuringDST) {
        if (millisSavedDuringDST <= 0) {
            throw new IllegalArgumentException("Illegal daylight saving value: "
                    + millisSavedDuringDST);
        }
        this.dstSavings = millisSavedDuringDST;
    }

    public boolean useDaylightTime() {
        return this.useDaylight;
    }

    public boolean observesDaylightTime() {
        return this.useDaylight;
    }

    public boolean inDaylightTime(Date date) {
        return this.getOffset(date.getTime()) != this.rawOffset;
    }

    /**
     * El offset total (estandar mas verano) en el instante dado.
     *
     * <p>Se pasa el instante a **hora estandar local** y se delega en la forma por campos, que es
     * donde vive la decision. Ese es el unico paso delicado: los argumentos de la otra forma estan
     * en hora estandar, no de pared, y confundirlas mueve la respuesta una hora entera.
     */
    public int getOffset(long date) {
        long local = date + this.rawOffset;
        long dias = Math.floorDiv(local, (long) MS_POR_DIA);
        int enElDia = (int) Math.floorMod(local, (long) MS_POR_DIA);
        long[] civil = GregorianCalendar.civilDesdeDias(dias);
        int anio = (int) civil[0];
        int mes = (int) civil[1] - 1;
        int dia = (int) civil[2];
        int dow = diaDeSemana(dias);
        int era = GregorianCalendar.AD;
        if (anio < 1) {
            era = GregorianCalendar.BC;
            anio = 1 - anio;
        }
        return this.getOffset(era, anio, mes, dia, dow, enElDia);
    }

    /**
     * El offset total para una fecha dada por campos.
     *
     * <p>`milliseconds` es el milisegundo del dia en **hora estandar local**, que es lo que fija el
     * contrato de TimeZone. Todo el metodo se apoya en eso: las dos reglas se normalizan a ese
     * mismo reloj antes de comparar, y ahi el modo de la hora deja de importar.
     */
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        if (era != GregorianCalendar.AD && era != GregorianCalendar.BC) {
            throw new IllegalArgumentException("Illegal era " + era);
        }
        if (month < Calendar.JANUARY || month > Calendar.DECEMBER) {
            throw new IllegalArgumentException("Illegal month " + month);
        }
        if (dayOfWeek < Calendar.SUNDAY || dayOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException("Illegal day of week " + dayOfWeek);
        }
        if (milliseconds < 0 || milliseconds >= MS_POR_DIA) {
            throw new IllegalArgumentException("Illegal millis " + milliseconds);
        }
        if (!this.useDaylight || era != GregorianCalendar.AD || year < this.startYear) {
            return this.rawOffset;
        }

        // Todo se lleva a "milisegundos desde la epoca, en hora estandar local", que es un orden
        // total y no se rompe cuando el ajuste de la hora empuja el instante fuera del dia.
        long consulta = GregorianCalendar.diasDesdeCivil(year, month + 1, day)
                * (long) MS_POR_DIA + milliseconds;
        long inicio = this.instanteDeRegla(year, true);
        long fin = this.instanteDeRegla(year, false);

        boolean enVerano;
        if (inicio < fin) {
            // Hemisferio norte: el verano cae **dentro** del anio.
            enVerano = consulta >= inicio && consulta < fin;
        } else {
            // Hemisferio sur: el verano cruza el fin de anio, asi que es el complemento.
            enVerano = consulta >= inicio || consulta < fin;
        }
        if (enVerano) {
            return this.rawOffset + this.dstSavings;
        }
        return this.rawOffset;
    }

    // El instante de una transicion en el anio dado, en hora estandar local.
    //
    // El ajuste es lo unico que distingue a las dos reglas: en el arranque el reloj todavia marca
    // hora estandar, asi que la hora de pared ya es la estandar; en el cierre el reloj viene
    // adelantado, asi que hay que restarle lo que adelanta.
    private long instanteDeRegla(int year, boolean esInicio) {
        int mes = esInicio ? this.startMonth : this.endMonth;
        int modo = esInicio ? this.startMode : this.endMode;
        int dia = esInicio ? this.startDay : this.endDay;
        int dow = esInicio ? this.startDayOfWeek : this.endDayOfWeek;
        int hora = esInicio ? this.startTime : this.endTime;
        int modoHora = esInicio ? this.startTimeMode : this.endTimeMode;

        int ajuste = 0;
        if (modoHora == UTC_TIME) {
            ajuste = this.rawOffset;
        } else if (modoHora == WALL_TIME && !esInicio) {
            ajuste = -this.dstSavings;
        }

        int dom = this.diaDelMes(modo, year, mes, dia, dow);
        return GregorianCalendar.diasDesdeCivil(year, mes + 1, dom) * (long) MS_POR_DIA
                + hora + ajuste;
    }

    // Resuelve el patron a un dia concreto del mes.
    private int diaDelMes(int modo, int year, int month, int day, int dayOfWeek) {
        int largo = largoDeMes(year, month);
        if (modo == DOM_MODE) {
            return day;
        }
        if (modo == DOW_IN_MONTH_MODE) {
            if (day > 0) {
                int primero = diaDeSemana(GregorianCalendar.diasDesdeCivil(year, month + 1, 1));
                int salto = (dayOfWeek - primero + 7) % 7;
                int d = 1 + salto + (day - 1) * 7;
                // Si el mes no tiene tantas semanas, vale la ultima -- es lo que hace el JDK con
                // un `5` en un mes que solo tiene cuatro de ese dia.
                while (d > largo) {
                    d = d - 7;
                }
                return d;
            }
            int ultimo = diaDeSemana(GregorianCalendar.diasDesdeCivil(year, month + 1, largo));
            int salto = (ultimo - dayOfWeek + 7) % 7;
            int d = largo - salto + (day + 1) * 7;
            while (d < 1) {
                d = d + 7;
            }
            return d;
        }
        if (modo == DOW_GE_DOM_MODE) {
            int dw = diaDeSemana(GregorianCalendar.diasDesdeCivil(year, month + 1, day));
            return day + (dayOfWeek - dw + 7) % 7;
        }
        // DOW_LE_DOM_MODE
        int dw = diaDeSemana(GregorianCalendar.diasDesdeCivil(year, month + 1, day));
        return day - (dw - dayOfWeek + 7) % 7;
    }

    // El dia de la semana (Calendar.SUNDAY..SATURDAY) de un dia desde la epoca.
    //
    // El 1970-01-01 fue **jueves**, y de ahi sale el `+ 4`: el dia 0 tiene que dar THURSDAY, que
    // vale 5.
    private static int diaDeSemana(long diasDesdeEpoca) {
        return (int) Math.floorMod(diasDesdeEpoca + 4, 7L) + 1;
    }

    private static int largoDeMes(int year, int month) {
        if (month == Calendar.FEBRUARY && esBisiesto(year)) {
            return 29;
        }
        return LARGO_MES[month];
    }

    private static boolean esBisiesto(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    // ---- identidad ---------------------------------------------------------------------------------

    /**
     * Si las dos zonas tienen las **mismas reglas**, aunque se llamen distinto.
     *
     * <p>Es distinto de `equals`, que ademas exige el mismo ID. La distincion importa: dos zonas
     * con nombres distintos y reglas iguales dan la misma hora siempre, y quien solo necesite eso
     * -- convertir instantes -- puede tratarlas como intercambiables.
     */
    public boolean hasSameRules(TimeZone other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone that = (SimpleTimeZone) other;
        if (this.rawOffset != that.rawOffset || this.useDaylight != that.useDaylight) {
            return false;
        }
        if (!this.useDaylight) {
            return true;
        }
        return this.dstSavings == that.dstSavings
                && this.startMode == that.startMode
                && this.startMonth == that.startMonth
                && this.startDay == that.startDay
                && this.startDayOfWeek == that.startDayOfWeek
                && this.startTime == that.startTime
                && this.startTimeMode == that.startTimeMode
                && this.endMode == that.endMode
                && this.endMonth == that.endMonth
                && this.endDay == that.endDay
                && this.endDayOfWeek == that.endDayOfWeek
                && this.endTime == that.endTime
                && this.endTimeMode == that.endTimeMode
                && this.startYear == that.startYear;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone that = (SimpleTimeZone) obj;
        return this.getID().equals(that.getID()) && this.hasSameRules(that);
    }

    public int hashCode() {
        return this.startMonth ^ this.startDay ^ this.startDayOfWeek ^ this.startTime
                ^ this.endMonth ^ this.endDay ^ this.endDayOfWeek ^ this.endTime ^ this.rawOffset;
    }

    public String toString() {
        return this.getClass().getName()
                + "[id=" + this.getID()
                + ",offset=" + this.rawOffset
                + ",dstSavings=" + this.dstSavings
                + ",useDaylight=" + this.useDaylight
                + ",startYear=" + this.startYear
                + ",startMode=" + this.startMode
                + ",startMonth=" + this.startMonth
                + ",startDay=" + this.startDay
                + ",startDayOfWeek=" + this.startDayOfWeek
                + ",startTime=" + this.startTime
                + ",startTimeMode=" + this.startTimeMode
                + ",endMode=" + this.endMode
                + ",endMonth=" + this.endMonth
                + ",endDay=" + this.endDay
                + ",endDayOfWeek=" + this.endDayOfWeek
                + ",endTime=" + this.endTime
                + ",endTimeMode=" + this.endTimeMode
                + "]";
    }
}
