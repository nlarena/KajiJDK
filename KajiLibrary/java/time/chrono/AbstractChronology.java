package java.time.chrono;

import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Map;

// KajiLibrary's java.time.chrono.AbstractChronology — the base class for Chronology implementations,
// supplying the identity/order plumbing (compare and equals by id, string form = id) so concrete
// chronologies only implement their calendar rules. A KajiLibrary subset of the JDK class.
public abstract class AbstractChronology implements Chronology {

    protected AbstractChronology() {
    }

    public int compareTo(Chronology other) {
        return this.getId().compareTo(other.getId());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AbstractChronology) {
            return this.compareTo((AbstractChronology) obj) == 0;
        }
        return false;
    }

    public int hashCode() {
        return this.getClass().hashCode() ^ this.getId().hashCode();
    }

    public String toString() {
        return this.getId();
    }

    /**
     * Reconstruye una fecha a partir de los campos sueltos que dejo un parseo.
     *
     * <p>El mapa **se consume**: los campos que se usan se sacan, y lo que queda son los que no se
     * entendieron. Es la convencion del JDK y no un detalle: el que llama despues comprueba que el
     * mapa quedo vacio, y si no lo esta sabe exactamente que sobro.
     *
     * <p>El orden en que se prueban las combinaciones no es arbitrario, va de la mas especifica a la
     * mas general: `EPOCH_DAY` sola ya dice todo; `ERA` + `YEAR_OF_ERA` se convierten a `YEAR` antes
     * de mirar el mes, porque el anio de la era no sirve para nada solo; y `DAY_OF_YEAR` se prueba
     * despues de mes+dia porque si estan los dos, mes y dia son los que el usuario escribio.
     *
     * <p>**Un subconjunto del resolvedor del JDK**, y conviene decir cual: aca se resuelven
     * `EPOCH_DAY`, `PROLEPTIC_MONTH`, `ERA`/`YEAR_OF_ERA` y las dos formas de fecha
     * --anio+mes+dia y anio+dia-del-anio--. Las combinaciones por semana --`ALIGNED_WEEK_OF_MONTH`
     * con `DAY_OF_WEEK`, y las de `WeekFields`-- no; con esos campos el mapa vuelve sin resolver en
     * vez de resolverse mal.
     */
    public ChronoLocalDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        if (fieldValues == null) {
            throw new NullPointerException("fieldValues");
        }
        if (resolverStyle == null) {
            throw new NullPointerException("resolverStyle");
        }
        boolean laxo = resolverStyle == ResolverStyle.LENIENT;

        // El dia epoch designa la fecha por si solo: no hay nada que combinar ni que validar.
        Long epochDay = fieldValues.remove(ChronoField.EPOCH_DAY);
        if (epochDay != null) {
            return this.dateEpochDay(epochDay.longValue());
        }

        // El mes proleptico es anio y mes juntos en un solo numero: se lo parte antes de seguir.
        Long prolepticMonth = fieldValues.remove(ChronoField.PROLEPTIC_MONTH);
        if (prolepticMonth != null) {
            long pm = prolepticMonth.longValue();
            if (!laxo) {
                ChronoField.PROLEPTIC_MONTH.checkValidValue(pm);
            }
            long anio = Math.floorDiv(pm, 12L);
            long mes = Math.floorMod(pm, 12L) + 1L;
            fieldValues.put(ChronoField.YEAR, Long.valueOf(anio));
            fieldValues.put(ChronoField.MONTH_OF_YEAR, Long.valueOf(mes));
        }

        // La era mas el anio de la era dan el anio proleptico, que es el unico con el que se cuenta.
        Long eraValue = fieldValues.remove(ChronoField.ERA);
        Long yearOfEra = fieldValues.remove(ChronoField.YEAR_OF_ERA);
        if (yearOfEra != null && !fieldValues.containsKey(ChronoField.YEAR)) {
            int yoe = (int) yearOfEra.longValue();
            Era era;
            if (eraValue != null) {
                era = this.eraOf((int) eraValue.longValue());
            } else {
                // Sin era escrita, la ultima de la lista: es la corriente, que es lo que alguien que
                // escribe un anio sin era quiere decir.
                List2 lista = new List2(this.eras());
                era = lista.ultima();
            }
            fieldValues.put(ChronoField.YEAR,
                    Long.valueOf((long) this.prolepticYear(era, yoe)));
        }

        Long year = fieldValues.remove(ChronoField.YEAR);
        if (year == null) {
            // Sin anio no hay fecha. Los campos que se sacaron se devuelven: el que llama tiene que
            // poder ver que habia, no un mapa a medio vaciar.
            if (eraValue != null) {
                fieldValues.put(ChronoField.ERA, eraValue);
            }
            if (yearOfEra != null) {
                fieldValues.put(ChronoField.YEAR_OF_ERA, yearOfEra);
            }
            return null;
        }
        int anio = (int) year.longValue();

        Long month = fieldValues.remove(ChronoField.MONTH_OF_YEAR);
        Long dayOfMonth = fieldValues.remove(ChronoField.DAY_OF_MONTH);
        if (month != null && dayOfMonth != null) {
            long m = month.longValue();
            long d = dayOfMonth.longValue();
            if (laxo) {
                // Laxo: los desbordes se arrastran. `2011-02-31` es el 3 de marzo, y un mes 14 es
                // febrero del anio siguiente. Se construye el primer dia y se suma.
                ChronoLocalDate base = this.date(anio, 1, 1);
                ChronoLocalDate conMes = base.plus(m - 1L, java.time.temporal.ChronoUnit.MONTHS);
                return conMes.plus(d - 1L, java.time.temporal.ChronoUnit.DAYS);
            }
            if (resolverStyle == ResolverStyle.SMART) {
                // Sensato: el mes tiene que existir, pero un dia que se pasa se recorta al ultimo
                // del mes. Es lo que hace que `31 de febrero` sea el 28 y no un error.
                ChronoField.DAY_OF_MONTH.checkValidValue(d);
                ChronoLocalDate primero = this.date(anio, (int) m, 1);
                int largo = primero.lengthOfMonth();
                long dia = d > (long) largo ? (long) largo : d;
                return this.date(anio, (int) m, (int) dia);
            }
            return this.date(anio, (int) m, (int) d);
        }

        Long dayOfYear = fieldValues.remove(ChronoField.DAY_OF_YEAR);
        if (dayOfYear != null) {
            if (laxo) {
                ChronoLocalDate base = this.dateYearDay(anio, 1);
                return base.plus(dayOfYear.longValue() - 1L, java.time.temporal.ChronoUnit.DAYS);
            }
            return this.dateYearDay(anio, (int) dayOfYear.longValue());
        }

        // Habia anio pero no alcanzo para una fecha: se devuelve lo que se saco, por lo mismo de
        // arriba.
        if (month != null) {
            fieldValues.put(ChronoField.MONTH_OF_YEAR, month);
        }
        if (dayOfMonth != null) {
            fieldValues.put(ChronoField.DAY_OF_MONTH, dayOfMonth);
        }
        fieldValues.put(ChronoField.YEAR, year);
        return null;
    }
}

// Un envoltorio de tres lineas para tomar el ultimo elemento de la lista de eras sin encadenar por un
// intermedio de tipo interfaz, que se pierde en silencio (#108).
final class List2 {

    private final java.util.List<Era> lista;

    List2(java.util.List<Era> lista) {
        this.lista = lista;
    }

    Era ultima() {
        int n = this.lista.size();
        return this.lista.get(n - 1);
    }
}
