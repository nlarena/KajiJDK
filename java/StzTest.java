import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

// Comportamiento de java.util.SimpleTimeZone y de los miembros nuevos de java.util.Objects.
//
// Lo que se prueba de SimpleTimeZone son las transiciones: no alcanza con "en julio hay verano",
// hay que mirar el milisegundo justo antes y justo despues del salto, que es donde se rompen las
// implementaciones. Y los dos hemisferios, porque en el sur el verano cruza el fin de anio y la
// comparacion se da vuelta.
public class StzTest {

    private static final int HORA = 3600000;

    // Milisegundos desde la epoca para una fecha UTC. `m` va 1..12.
    //
    // Va escrito aca y no con un Calendar para que la prueba mida SimpleTimeZone y nada mas.
    private static long utc(int y, int m, int d, int h, int min) {
        long yy = y;
        if (m <= 2) {
            yy = yy - 1;
        }
        long era = (yy >= 0 ? yy : yy - 399) / 400;
        long yoe = yy - era * 400;
        int desplazado = m + (m > 2 ? -3 : 9);
        long doy = (153L * desplazado + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        long dias = era * 146097 + doe - 719468;
        return dias * 86400000L + h * 3600000L + min * 60000L;
    }

    // Los Estados Unidos desde 2007: empieza el 2do domingo de marzo a las 2 de la manana de
    // pared, termina el 1er domingo de noviembre a las 2 de pared.
    private static SimpleTimeZone eeuu() {
        return new SimpleTimeZone(-5 * HORA, "EST",
                Calendar.MARCH, 2, Calendar.SUNDAY, 2 * HORA,
                Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2 * HORA);
    }

    // Europa: ultimo domingo de marzo y ultimo de octubre, la hora dada en UTC. El `-1` es la
    // forma de decir "el ultimo".
    private static SimpleTimeZone europa() {
        return new SimpleTimeZone(HORA, "CET",
                Calendar.MARCH, -1, Calendar.SUNDAY, HORA, SimpleTimeZone.UTC_TIME,
                Calendar.OCTOBER, -1, Calendar.SUNDAY, HORA, SimpleTimeZone.UTC_TIME,
                HORA);
    }

    // Hemisferio sur: el verano cruza el fin de anio.
    private static SimpleTimeZone sur() {
        return new SimpleTimeZone(10 * HORA, "AEST",
                Calendar.OCTOBER, 1, Calendar.SUNDAY, 2 * HORA,
                Calendar.APRIL, 1, Calendar.SUNDAY, 3 * HORA);
    }

    public static int run() {
        int r = 0;

        // ---- Estados Unidos --------------------------------------------------------------------
        SimpleTimeZone us = eeuu();
        r = r + (us.getRawOffset() == -5 * HORA ? 1 : 0);
        r = r + (us.getDSTSavings() == HORA ? 2 : 0);
        r = r + (us.useDaylightTime() ? 4 : 0);
        r = r + (us.getOffset(utc(2021, 1, 15, 12, 0)) == -5 * HORA ? 8 : 0);
        r = r + (us.getOffset(utc(2021, 7, 15, 12, 0)) == -4 * HORA ? 16 : 0);
        r = r + (us.getOffset(utc(2021, 12, 15, 12, 0)) == -5 * HORA ? 32 : 0);

        // El salto de marzo: 2021-03-14, 2 de la manana de pared = 07:00 UTC.
        long arranca = utc(2021, 3, 14, 7, 0);
        r = r + (us.getOffset(arranca - 1) == -5 * HORA ? 64 : 0);
        r = r + (us.getOffset(arranca) == -4 * HORA ? 128 : 0);

        // El de noviembre: 2021-11-07, 2 de pared **en verano** = 1 estandar = 06:00 UTC.
        long termina = utc(2021, 11, 7, 6, 0);
        r = r + (us.getOffset(termina - 1) == -4 * HORA ? 256 : 0);
        r = r + (us.getOffset(termina) == -5 * HORA ? 512 : 0);

        // inDaylightTime dice lo mismo por otra via.
        r = r + (us.inDaylightTime(new Date(utc(2021, 7, 15, 12, 0))) ? 1024 : 0);
        r = r + (us.inDaylightTime(new Date(utc(2021, 1, 15, 12, 0))) ? 0 : 2048);

        // La forma por campos, que es la abstracta de TimeZone. El milisegundo del dia va en hora
        // **estandar** local, no de pared: 2021-03-14 a las 02:00 estandar ya es verano.
        r = r + (us.getOffset(1, 2021, Calendar.MARCH, 14, Calendar.SUNDAY, 2 * HORA)
                == -4 * HORA ? 4096 : 0);
        r = r + (us.getOffset(1, 2021, Calendar.MARCH, 14, Calendar.SUNDAY, 2 * HORA - 1)
                == -5 * HORA ? 8192 : 0);

        // ---- Europa, con la hora dada en UTC ----------------------------------------------------
        SimpleTimeZone eu = europa();
        // 2021: ultimo domingo de marzo = 28, ultimo de octubre = 31.
        long euArranca = utc(2021, 3, 28, 1, 0);
        long euTermina = utc(2021, 10, 31, 1, 0);
        r = r + (eu.getOffset(euArranca - 1) == HORA ? 1 : 0);
        r = r + (eu.getOffset(euArranca) == 2 * HORA ? 2 : 0);
        r = r + (eu.getOffset(euTermina - 1) == 2 * HORA ? 4 : 0);
        r = r + (eu.getOffset(euTermina) == HORA ? 8 : 0);
        // 2020: el ultimo domingo de marzo fue el 29 y el de octubre el 25.
        r = r + (eu.getOffset(utc(2020, 3, 29, 1, 0)) == 2 * HORA ? 16 : 0);
        r = r + (eu.getOffset(utc(2020, 3, 29, 0, 59)) == HORA ? 32 : 0);
        r = r + (eu.getOffset(utc(2020, 10, 25, 1, 0)) == HORA ? 64 : 0);

        // ---- hemisferio sur ---------------------------------------------------------------------
        SimpleTimeZone au = sur();
        r = r + (au.getOffset(utc(2021, 1, 15, 0, 0)) == 11 * HORA ? 128 : 0);   // verano
        r = r + (au.getOffset(utc(2021, 6, 15, 0, 0)) == 10 * HORA ? 256 : 0);   // invierno
        r = r + (au.getOffset(utc(2021, 12, 15, 0, 0)) == 11 * HORA ? 512 : 0);  // verano otra vez

        // ---- las cuatro formas de escribir una regla --------------------------------------------
        // El primer domingo en o DESPUES del 8 de marzo -- que es otra forma de decir el 2do.
        SimpleTimeZone despues = new SimpleTimeZone(-5 * HORA, "X");
        despues.setStartRule(Calendar.MARCH, 8, Calendar.SUNDAY, 2 * HORA, true);
        despues.setEndRule(Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2 * HORA, true);
        r = r + (despues.getOffset(utc(2021, 3, 14, 7, 0)) == -4 * HORA ? 1024 : 0);
        r = r + (despues.getOffset(utc(2021, 3, 14, 7, 0) - 1) == -5 * HORA ? 2048 : 0);

        // El ultimo domingo en o ANTES del 31 de octubre.
        SimpleTimeZone antes = new SimpleTimeZone(HORA, "Y");
        antes.setStartRule(Calendar.MARCH, 31, Calendar.SUNDAY, 2 * HORA, false);
        antes.setEndRule(Calendar.OCTOBER, 31, Calendar.SUNDAY, 2 * HORA, false);
        r = r + (antes.getOffset(utc(2021, 3, 28, 1, 0)) == 2 * HORA ? 4096 : 0);

        // Un dia exacto del mes, sin dia de semana.
        SimpleTimeZone exacto = new SimpleTimeZone(0, "Z");
        exacto.setStartRule(Calendar.APRIL, 1, 0);
        exacto.setEndRule(Calendar.SEPTEMBER, 30, 0);
        r = r + (exacto.getOffset(utc(2021, 4, 1, 0, 0)) == HORA ? 8192 : 0);
        r = r + (exacto.getOffset(utc(2021, 3, 31, 23, 59)) == 0 ? 16384 : 0);
        r = r + (exacto.getOffset(utc(2021, 9, 30, 0, 0)) == 0 ? 32768 : 0);

        // ---- startYear ---------------------------------------------------------------------------
        SimpleTimeZone desde = eeuu();
        desde.setStartYear(2010);
        r = r + (desde.getOffset(utc(2005, 7, 15, 12, 0)) == -5 * HORA ? 1 : 0);
        r = r + (desde.getOffset(utc(2021, 7, 15, 12, 0)) == -4 * HORA ? 2 : 0);

        // ---- sin horario de verano ----------------------------------------------------------------
        SimpleTimeZone plana = new SimpleTimeZone(-3 * HORA, "ART");
        r = r + (plana.useDaylightTime() ? 0 : 4);
        r = r + (plana.getDSTSavings() == 0 ? 8 : 0);
        r = r + (plana.getOffset(utc(2021, 7, 15, 12, 0)) == -3 * HORA ? 16 : 0);
        r = r + (plana.inDaylightTime(new Date(0L)) ? 0 : 32);

        // ---- dstSavings a medida ------------------------------------------------------------------
        SimpleTimeZone media = eeuu();
        media.setDSTSavings(1800000);
        r = r + (media.getOffset(utc(2021, 7, 15, 12, 0)) == -5 * HORA + 1800000 ? 64 : 0);

        // ---- identidad ------------------------------------------------------------------------------
        r = r + (eeuu().hasSameRules(eeuu()) ? 128 : 0);
        r = r + (eeuu().hasSameRules(europa()) ? 0 : 256);
        r = r + (eeuu().equals(eeuu()) ? 512 : 0);
        r = r + (eeuu().hashCode() == eeuu().hashCode() ? 1024 : 0);
        SimpleTimeZone otroId = new SimpleTimeZone(-5 * HORA, "OTRO",
                Calendar.MARCH, 2, Calendar.SUNDAY, 2 * HORA,
                Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2 * HORA);
        // Mismas reglas, distinto nombre: hasSameRules si, equals no.
        r = r + (eeuu().hasSameRules(otroId) ? 2048 : 0);
        r = r + (eeuu().equals(otroId) ? 0 : 4096);
        TimeZone comoTz = us;
        r = r + (comoTz.getID().equals("EST") ? 8192 : 0);

        // ---- java.util.Objects --------------------------------------------------------------------
        r = r + (Objects.requireNonNullElse(null, "x").equals("x") ? 1 : 0);
        r = r + (Objects.requireNonNullElse("a", "x").equals("a") ? 2 : 0);
        r = r + Objects.hash("a", "b") * 0;                                  // solo que no reviente
        r = r + (Objects.hash("a", "b") == Objects.hash("a", "b") ? 4 : 0);
        // La trampa conocida: hash(x) NO es hashCode(x), porque el varargs arma un arreglo de uno.
        r = r + (Objects.hash("a") == 31 + "a".hashCode() ? 8 : 0);
        r = r + (Objects.hash("a") == Objects.hashCode("a") ? 0 : 16);

        int[] u1 = { 1, 2 };
        int[] u2 = { 1, 2 };
        r = r + (Objects.deepEquals(u1, u2) ? 32 : 0);
        r = r + (Objects.equals(u1, u2) ? 0 : 64);                            // equals es identidad
        Object[] hondo1 = { u1 };
        Object[] hondo2 = { u2 };
        r = r + (Objects.deepEquals(hondo1, hondo2) ? 128 : 0);
        r = r + (Objects.deepEquals(null, null) ? 256 : 0);
        r = r + (Objects.deepEquals(u1, null) ? 0 : 512);
        r = r + (Objects.deepEquals("a", "a") ? 1024 : 0);

        r = r + (Objects.compare("a", "a", new PorTexto()) == 0 ? 2048 : 0);
        r = r + (Objects.compare(null, null, new PorTexto()) == 0 ? 4096 : 0);
        r = r + (Objects.compare("a", "b", new PorTexto()) < 0 ? 8192 : 0);

        r = r + Objects.checkIndex(2, 5) * 16384;                              // 32768
        r = r + Objects.checkFromToIndex(1, 3, 5) * 65536;                     // 65536
        r = r + Objects.checkFromIndexSize(1, 2, 5) * 131072;                  // 131072
        r = r + (rechaza(0) ? 262144 : 0);                                     // checkIndex(5, 5)
        r = r + (rechaza(1) ? 524288 : 0);                                     // checkIndex(-1, 5)
        r = r + (rechaza(2) ? 1048576 : 0);                                    // from > to
        r = r + (rechaza(3) ? 2097152 : 0);                                    // to > length
        r = r + (rechaza(4) ? 4194304 : 0);                                    // size desbordando
        r = r + (Objects.toIdentityString("x").indexOf("java.lang.String@") == 0 ? 8388608 : 0);

        return r;
    }

    // Cada comprobacion de rango que tiene que fallar. Devuelve true si tiro
    // IndexOutOfBoundsException.
    private static boolean rechaza(int cual) {
        try {
            if (cual == 0) {
                Objects.checkIndex(5, 5);
            } else if (cual == 1) {
                Objects.checkIndex(-1, 5);
            } else if (cual == 2) {
                Objects.checkFromToIndex(3, 1, 5);
            } else if (cual == 3) {
                Objects.checkFromToIndex(1, 9, 5);
            } else {
                // El caso que la forma obvia (`from + size > length`) deja pasar: la suma desborda
                // y da negativa, asi que el chequeo pasaria.
                Objects.checkFromIndexSize(1, 2147483647, 5);
            }
        } catch (IndexOutOfBoundsException e) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}

class PorTexto implements java.util.Comparator<String> {
    public int compare(String a, String b) {
        return a.compareTo(b);
    }
}
