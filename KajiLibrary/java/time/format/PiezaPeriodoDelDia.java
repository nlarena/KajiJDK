package java.time.format;

import java.time.temporal.ChronoField;

// El periodo del dia escrito con palabras: "in the morning", "nachmittags", "madrugada".
//
// ===============================================================================================
// EN QUE SE DIFERENCIA DE AM/PM
// ===============================================================================================
//
// En que no parte el dia en dos sino en los pedazos que ese idioma nombra, que no son los mismos ni
// son parejos: el aleman tiene siete, el espanol cinco, el ingles seis. Y algunos duran un minuto
// --`noon` es exactamente las doce-- mientras el de al lado dura seis horas.
//
// Esa es la razon de que exista: `2 PM` es una traduccion literal que en la mitad de los idiomas no
// se dice, y "a las dos de la tarde" si. Los datos estan en `PeriodosDelDia`, extraidos del JDK.
//
// **Al parsear se resuelve al punto medio del tramo.** "In the morning" no dice que hora es, asi que
// el JDK toma el medio de su rango --el minuto 360 para un tramo de 1 a 720-- y eso es lo que queda
// puesto. Es una convencion, no una deduccion, y es la del JDK.
final class PiezaPeriodoDelDia extends Pieza {

    private final TextStyle estilo;

    PiezaPeriodoDelDia(TextStyle estilo) {
        this.estilo = estilo;
    }

    boolean imprimir(CtxImprimir ctx, StringBuilder salida) {
        Long valor = ctx.valor(ChronoField.MINUTE_OF_DAY);
        if (valor == null) {
            return ctx.faltaOTira("DayPeriod");
        }
        salida.append(PeriodosDelDia.nombre(ctx.locale, this.estilo, (int) valor.longValue()));
        return true;
    }

    int parsear(CtxParseo ctx, String texto, int pos) {
        String[] nombres = PeriodosDelDia.nombres(ctx.locale, this.estilo);
        // Del mas largo al mas corto: en aleman `nachmittags` empieza con `nachts` mal recortado, y
        // quedarse con el primero que encaje daria el periodo equivocado.
        int mejor = -1;
        int largoMejor = -1;
        for (int i = 0; i < nombres.length; i++) {
            String n = nombres[i];
            if (n.length() > largoMejor
                    && texto.regionMatches(!ctx.sensible, pos, n, 0, n.length())) {
                mejor = i;
                largoMejor = n.length();
            }
        }
        if (mejor < 0) {
            return ~pos;
        }
        int medio = PeriodosDelDia.medio(ctx.locale, this.estilo, mejor);
        ctx.poner(ChronoField.MINUTE_OF_DAY, (long) medio);
        return pos + largoMejor;
    }

    @Override
    public String toString() {
        return "DayPeriod(" + this.estilo + ")";
    }
}
