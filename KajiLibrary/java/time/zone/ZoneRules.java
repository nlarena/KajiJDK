package java.time.zone;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

// KajiLibrary's java.time.zone.ZoneRules — the rules that say what a zone's offset IS at a given
// moment. This is the engine the whole package exists for.
//
// Two directions, and they are not symmetric:
//
//   instant -> offset   always exactly one answer. Binary search the transition list, fall through
//                       to the recurring rules for years past the tabulated data.
//   local   -> offset   ZERO answers inside a gap, TWO inside an overlap. That asymmetry is why
//                       `getValidOffsets` returns a List and not a ZoneOffset.
//
// The data is a documented subset of tzdb — see TzData for which zones and why.
public final class ZoneRules {

    // -1 marks "not a tabulated zone": a fixed-offset rule set, built by of(ZoneOffset).
    private final int zone;
    private final int fixedOffset;

    // ---- la tercera forma: las listas que el llamador dio ------------------------------------------
    //
    // `of(ZoneOffset, ZoneOffset, List, List, List)` construye reglas que **no** salen de la tabla:
    // las transiciones y las reglas recurrentes vienen de afuera. Se guardan aca y se leen por los
    // mismos accesores privados que leen la tabla, asi que los diecisiete metodos publicos no
    // distinguen una forma de la otra. Ramificarlos uno por uno habria sido la otra manera, y la
    // manera de que dos de ellos se desincronicen.
    //
    // `null` = esta no es esa forma.
    private final ZoneOffsetTransition[] transiciones;
    private final ZoneOffsetTransitionRule[] reglas;
    // Las transiciones del desplazamiento **estandar**: cuando la zona cambio su hora base, no su
    // horario de verano. Casi siempre vacia, y por eso se guarda aparte de las otras.
    private final ZoneOffsetTransition[] transicionesEstandar;
    private final int estandarBase;

    private ZoneRules(int zone, int fixedOffset) {
        this.zone = zone;
        this.fixedOffset = fixedOffset;
        this.transiciones = null;
        this.reglas = null;
        this.transicionesEstandar = null;
        this.estandarBase = fixedOffset;
    }

    private ZoneRules(int estandarBase, int muroBase, ZoneOffsetTransition[] transicionesEstandar,
            ZoneOffsetTransition[] transiciones, ZoneOffsetTransitionRule[] reglas) {
        // -2 y no -1: `-1` ya significa "desplazamiento fijo", y `isFixedOffset` lo usa. Un juego de
        // reglas dado por listas **no** es fijo aunque las listas vengan vacias... salvo que lo sea, y
        // eso lo decide `isFixedOffset` mirando las listas, no el marcador.
        this.zone = -2;
        this.fixedOffset = muroBase;
        this.estandarBase = estandarBase;
        this.transicionesEstandar = transicionesEstandar;
        this.transiciones = transiciones;
        this.reglas = reglas;
    }

    public static ZoneRules of(ZoneOffset offset) {
        return new ZoneRules(-1, offset.getTotalSeconds());
    }

    /**
     * Un juego de reglas armado con **listas explicitas** en vez de con la tabla embebida.
     *
     * <p>Es la fabrica que usa quien tiene sus propios datos de zona: un lector de tzdb, una prueba
     * que quiere una zona controlada, o un `ZoneRulesProvider` propio. Las tres listas dicen cosas
     * distintas y conviene no confundirlas:
     *
     * <ul>
     *   <li>`standardOffsetTransitionList` -- cuando la zona cambio su hora **base**. Es rarisimo
     *       (un pais que se cambia de huso) y por eso casi siempre va vacia.
     *   <li>`transitionList` -- los cambios **historicos** ya ocurridos, con fecha exacta.
     *   <li>`lastRules` -- las reglas **recurrentes** que rigen de ahi en adelante, sin fecha de fin.
     *       Son las que hacen que la zona siga teniendo respuesta para un ano que todavia no paso.
     * </ul>
     *
     * <p>Las listas se **copian**: quien las pasa puede seguir usando las suyas sin que estas reglas
     * cambien debajo. Un juego de reglas que mutara no serviria para nada -- `ZoneRules` se comparte
     * entre todos los `ZonedDateTime` de esa zona.
     *
     * @throws NullPointerException si algun argumento es `null`
     */
    public static ZoneRules of(ZoneOffset baseStandardOffset, ZoneOffset baseWallOffset,
            List<ZoneOffsetTransition> standardOffsetTransitionList,
            List<ZoneOffsetTransition> transitionList,
            List<ZoneOffsetTransitionRule> lastRules) {
        if (baseStandardOffset == null || baseWallOffset == null
                || standardOffsetTransitionList == null || transitionList == null
                || lastRules == null) {
            throw new NullPointerException();
        }
        return new ZoneRules(baseStandardOffset.getTotalSeconds(),
                baseWallOffset.getTotalSeconds(),
                copiarTransiciones(standardOffsetTransitionList),
                copiarTransiciones(transitionList),
                copiarReglas(lastRules));
    }

    private static ZoneOffsetTransition[] copiarTransiciones(List<ZoneOffsetTransition> xs) {
        ZoneOffsetTransition[] out = new ZoneOffsetTransition[xs.size()];
        int i = 0;
        while (i < out.length) {
            out[i] = xs.get(i);
            i = i + 1;
        }
        return out;
    }

    private static ZoneOffsetTransitionRule[] copiarReglas(List<ZoneOffsetTransitionRule> xs) {
        ZoneOffsetTransitionRule[] out = new ZoneOffsetTransitionRule[xs.size()];
        int i = 0;
        while (i < out.length) {
            out[i] = xs.get(i);
            i = i + 1;
        }
        return out;
    }

    // ---- la capa de datos, comun a las tres formas --------------------------------------------------
    //
    // Aca es donde se decide de donde salen los numeros. Todo lo de arriba pregunta por estos cinco.

    private int cantTransiciones() {
        if (this.transiciones != null) {
            return this.transiciones.length;
        }
        return this.zone >= 0 ? TzData.transitionCount(this.zone) : 0;
    }

    private int cantReglas() {
        if (this.reglas != null) {
            return this.reglas.length;
        }
        return this.zone >= 0 ? TzData.ruleCount(this.zone) : 0;
    }

    private long epocaDe(int i) {
        if (this.transiciones != null) {
            return this.transiciones[i].toEpochSecond();
        }
        return TzData.transitionEpoch(this.zone, i);
    }

    private int antesDe(int i) {
        if (this.transiciones != null) {
            return this.transiciones[i].getOffsetBefore().getTotalSeconds();
        }
        return TzData.transitionBefore(this.zone, i);
    }

    private int despuesDe(int i) {
        if (this.transiciones != null) {
            return this.transiciones[i].getOffsetAfter().getTotalSeconds();
        }
        return TzData.transitionAfter(this.zone, i);
    }

    // Package-private: the provider builds these from the embedded table.
    static ZoneRules ofZone(int zoneIndex) {
        return new ZoneRules(zoneIndex, 0);
    }

    public boolean isFixedOffset() {
        // La forma por listas es fija si no tiene ni transiciones ni reglas -- que es exactamente la
        // misma prueba que se le hace a una zona tabulada. Por eso la pregunta se hace sobre los
        // contadores y no sobre el marcador: `-2` no dice nada de si la zona cambia o no.
        if (this.zone == -1) {
            return true;
        }
        return this.cantTransiciones() == 0 && this.cantReglas() == 0;
    }

    /**
     * El desplazamiento vigente para esa fecha y hora **locales**.
     *
     * <p>La diferencia con la version de `Instant` es toda la dificultad de las zonas horarias: un
     * instante tiene **siempre exactamente una** respuesta, y una hora local puede tener dos --la
     * hora que se repite cuando el reloj se atrasa-- o ninguna --la que se saltea cuando se
     * adelanta--.
     *
     * <p>Este metodo devuelve **una sola**, y el contrato dice cual: en un solapamiento, la de
     * **antes** del cambio; en un hueco, la de **antes** tambien. Es una simplificacion deliberada
     * del JDK, y por eso existe `getValidOffsets`, que devuelve la lista entera. Quien necesite
     * distinguir los tres casos tiene que mirar `isGap`/`isOverlap` de la transicion.
     */
    public ZoneOffset getOffset(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            throw new NullPointerException("localDateTime");
        }
        if (this.zone == -1) {
            return ZoneOffset.ofTotalSeconds(this.fixedOffset);
        }
        // Se prueba con el desplazamiento de antes de la primera transicion y se avanza mientras la
        // hora local caiga despues del cambio. Comparar en local y no en instante es justamente lo
        // que hace que un hueco y un solapamiento den la respuesta de "antes".
        int count = this.cantTransiciones();
        if (count == 0) {
            return this.getOffset(java.time.Instant.ofEpochSecond(0L));
        }
        int resultado = this.antesDe(0);
        int i = 0;
        while (i < count) {
            int antes = this.antesDe(i);
            long epochCambio = this.epocaDe(i);
            // El instante del cambio, leido en el reloj de pared de **antes**.
            long localDelCambio = epochCambio + (long) antes;
            long localPedido = ZoneMath.toEpochSecond(localDateTime, 0);
            if (localPedido < localDelCambio) {
                return ZoneOffset.ofTotalSeconds(antes);
            }
            resultado = this.despuesDe(i);
            i = i + 1;
        }
        return ZoneOffset.ofTotalSeconds(resultado);
    }

    // The offset in force at an instant. Exactly one answer, always.
    public ZoneOffset getOffset(Instant instant) {
        return ZoneOffset.ofTotalSeconds(this.offsetSecondsAt(instant.getEpochSecond()));
    }

    private int offsetSecondsAt(long epochSecond) {
        int result = this.fixedOffset;
        if (this.zone != -1) {
            int count = this.cantTransiciones();
            if (count == 0) {
                // Sin transiciones: la tabulada no sabe nada y da cero; la de listas tiene su
                // desplazamiento de muro base, que es justamente lo que el llamador dijo.
                result = this.transiciones != null ? this.fixedOffset : 0;
                if (this.cantReglas() > 0) {
                    result = this.offsetFromRules(epochSecond, result);
                }
            } else if (epochSecond < this.epocaDe(0)) {
                result = this.antesDe(0);
            } else {
                // Last transition at or before the instant.
                int lo = 0;
                int hi = count - 1;
                int found = 0;
                while (lo <= hi) {
                    int mid = (lo + hi) / 2;
                    if (this.epocaDe(mid) <= epochSecond) {
                        found = mid;
                        lo = mid + 1;
                    } else {
                        hi = mid - 1;
                    }
                }
                result = this.despuesDe(found);
                // Past the tabulated data the recurring rules take over.
                if (found == count - 1 && this.cantReglas() > 0) {
                    result = this.offsetFromRules(epochSecond, result);
                }
            }
        }
        return result;
    }

    // Walk the recurring rules for the instant's year (and the previous one, since a southern
    // hemisphere rule set puts the year's first transition in the middle of summer).
    private int offsetFromRules(long epochSecond, int fallback) {
        LocalDateTime approx = ZoneMath.ofEpochSecond(epochSecond, 0);
        int year = approx.getYear();
        int result = fallback;
        long best = Long.MIN_VALUE + 1L;
        int y = year - 1;
        while (y <= year + 1) {
            int i = 0;
            while (i < this.cantReglas()) {
                ZoneOffsetTransition t = this.rule(i).createTransition(y);
                long at = t.toEpochSecond();
                if (at <= epochSecond && at > best) {
                    best = at;
                    result = t.getOffsetAfter().getTotalSeconds();
                }
                i = i + 1;
            }
            y = y + 1;
        }
        return result;
    }

    private ZoneOffsetTransitionRule rule(int i) {
        if (this.reglas != null) {
            return this.reglas[i];
        }
        return new ZoneOffsetTransitionRule(
                TzData.ruleField(this.zone, i, 0),
                TzData.ruleField(this.zone, i, 1),
                TzData.ruleField(this.zone, i, 2),
                TzData.ruleField(this.zone, i, 3),
                TzData.ruleField(this.zone, i, 4) != 0,
                TzData.ruleField(this.zone, i, 5),
                TzData.ruleField(this.zone, i, 6),
                TzData.ruleField(this.zone, i, 7),
                TzData.ruleField(this.zone, i, 8));
    }

    // The offsets a LOCAL reading could mean: one normally, none in a gap, two in an overlap.
    public List<ZoneOffset> getValidOffsets(LocalDateTime localDateTime) {
        List<ZoneOffset> result = new ArrayList<ZoneOffset>();
        ZoneOffsetTransition trans = this.getTransition(localDateTime);
        if (trans == null) {
            result.add(ZoneOffset.ofTotalSeconds(this.offsetSecondsForLocal(localDateTime)));
        } else if (trans.isOverlap()) {
            result.add(trans.getOffsetBefore());
            result.add(trans.getOffsetAfter());
        }
        return result;
    }

    // The transition straddling this local reading, or null when the reading is unambiguous.
    public ZoneOffsetTransition getTransition(LocalDateTime localDateTime) {
        ZoneOffsetTransition found = null;
        if (this.zone != -1) {
            int count = this.cantTransiciones();
            int i = 0;
            while (i < count) {
                ZoneOffsetTransition t = this.transition(i);
                LocalDateTime before = t.getDateTimeBefore();
                LocalDateTime after = t.getDateTimeAfter();
                LocalDateTime low = before;
                LocalDateTime high = after;
                if (t.isOverlap()) {
                    low = after;
                    high = before;
                }
                if (localDateTime.compareTo(low) >= 0 && localDateTime.compareTo(high) < 0) {
                    found = t;
                    i = count;
                } else {
                    i = i + 1;
                }
            }
        }
        return found;
    }

    private int offsetSecondsForLocal(LocalDateTime localDateTime) {
        // A local reading is resolved by trying the offsets around it: take the offset in force at
        // the instant the reading would name under a first guess, then confirm.
        int guess = this.offsetSecondsAt(ZoneMath.toEpochSecond(localDateTime, 0));
        long epoch = ZoneMath.toEpochSecond(localDateTime, guess);
        return this.offsetSecondsAt(epoch);
    }

    public boolean isValidOffset(LocalDateTime localDateTime, ZoneOffset offset) {
        List<ZoneOffset> valid = this.getValidOffsets(localDateTime);
        boolean ok = false;
        int i = 0;
        while (i < valid.size()) {
            ZoneOffset candidate = valid.get(i);
            if (candidate.getTotalSeconds() == offset.getTotalSeconds()) {
                ok = true;
            }
            i = i + 1;
        }
        return ok;
    }

    // The offset ignoring daylight saving — what the zone would use all year.
    public ZoneOffset getStandardOffset(Instant instant) {
        // La forma por listas lo sabe de verdad: arranca en el estandar base y lo corre en cada
        // transicion **de estandar** que ya haya pasado. Es la unica de las tres que puede contestar
        // esto sin adivinar.
        if (this.transicionesEstandar != null) {
            int result = this.estandarBase;
            long epoch = instant.getEpochSecond();
            int i = 0;
            while (i < this.transicionesEstandar.length) {
                if (this.transicionesEstandar[i].toEpochSecond() > epoch) {
                    break;
                }
                result = this.transicionesEstandar[i].getOffsetAfter().getTotalSeconds();
                i = i + 1;
            }
            return ZoneOffset.ofTotalSeconds(result);
        }
        ZoneOffset result = ZoneOffset.ofTotalSeconds(this.fixedOffset);
        if (this.zone >= 0) {
            int ruleCount = TzData.ruleCount(this.zone);
            if (ruleCount > 0) {
                result = this.rule(0).getStandardOffset();
            } else {
                result = this.getOffset(instant);
            }
        }
        return result;
    }

    public Duration getDaylightSavings(Instant instant) {
        int actual = this.offsetSecondsAt(instant.getEpochSecond());
        int standard = this.getStandardOffset(instant).getTotalSeconds();
        return Duration.ofSeconds((long) (actual - standard));
    }

    public boolean isDaylightSavings(Instant instant) {
        int actual = this.offsetSecondsAt(instant.getEpochSecond());
        int standard = this.getStandardOffset(instant).getTotalSeconds();
        return actual != standard;
    }

    public ZoneOffsetTransition nextTransition(Instant instant) {
        ZoneOffsetTransition found = null;
        if (this.zone != -1) {
            long epoch = instant.getEpochSecond();
            int count = this.cantTransiciones();
            int i = 0;
            while (i < count) {
                if (this.epocaDe(i) > epoch) {
                    found = this.transition(i);
                    i = count;
                } else {
                    i = i + 1;
                }
            }
        }
        return found;
    }

    public ZoneOffsetTransition previousTransition(Instant instant) {
        ZoneOffsetTransition found = null;
        if (this.zone != -1) {
            long epoch = instant.getEpochSecond();
            int i = this.cantTransiciones() - 1;
            while (i >= 0) {
                if (this.epocaDe(i) < epoch) {
                    found = this.transition(i);
                    i = -1;
                } else {
                    i = i - 1;
                }
            }
        }
        return found;
    }

    private ZoneOffsetTransition transition(int i) {
        if (this.transiciones != null) {
            return this.transiciones[i];
        }
        return ZoneOffsetTransition.ofRaw(TzData.transitionEpoch(this.zone, i),
                TzData.transitionBefore(this.zone, i), TzData.transitionAfter(this.zone, i));
    }

    public List<ZoneOffsetTransition> getTransitions() {
        List<ZoneOffsetTransition> out = new ArrayList<ZoneOffsetTransition>();
        int i = 0;
        while (i < this.cantTransiciones()) {
            out.add(this.transition(i));
            i = i + 1;
        }
        return out;
    }

    public List<ZoneOffsetTransitionRule> getTransitionRules() {
        List<ZoneOffsetTransitionRule> out = new ArrayList<ZoneOffsetTransitionRule>();
        int i = 0;
        while (i < this.cantReglas()) {
            out.add(this.rule(i));
            i = i + 1;
        }
        return out;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ZoneRules) {
            ZoneRules o = (ZoneRules) other;
            if (this.zone != o.zone || this.fixedOffset != o.fixedOffset) {
                return false;
            }
            // Dos juegos armados por listas son iguales si sus listas lo son. Compararlos por
            // identidad --que es lo que hacia el `zone == zone` solo-- diria que dos `of(...)` con
            // los mismos datos son distintos, y no lo son.
            if (this.zone == -2) {
                return this.estandarBase == o.estandarBase
                        && this.getTransitions().equals(o.getTransitions())
                        && this.getTransitionRules().equals(o.getTransitionRules());
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        int h = this.zone ^ this.fixedOffset;
        if (this.zone == -2) {
            h = h ^ this.estandarBase ^ this.getTransitions().hashCode();
        }
        return h;
    }

    public String toString() {
        if (this.zone == -1) {
            return "ZoneRules[fixed=" + ZoneOffset.ofTotalSeconds(this.fixedOffset).toString() + "]";
        }
        if (this.zone == -2) {
            return "ZoneRules[" + this.cantTransiciones() + " transiciones, "
                    + this.cantReglas() + " reglas]";
        }
        return "ZoneRules[" + TzData.zoneIds()[this.zone] + "]";
    }
}
