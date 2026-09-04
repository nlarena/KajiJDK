import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.List;

/** ZoneRules.of(...) con listas explicitas: la tercera representacion. */
public class ZoneOfTest {

    public static int run() {
        int i = 0;
        ZoneOffset menos5 = ZoneOffset.ofTotalSeconds(-5 * 3600);
        ZoneOffset menos4 = ZoneOffset.ofTotalSeconds(-4 * 3600);

        // -- sin transiciones ni reglas: es fijo
        List<ZoneOffsetTransition> vacio = new ArrayList<ZoneOffsetTransition>();
        List<java.time.zone.ZoneOffsetTransitionRule> sinReglas =
                new ArrayList<java.time.zone.ZoneOffsetTransitionRule>();
        ZoneRules fijo = ZoneRules.of(menos5, menos5, vacio, vacio, sinReglas);
        if (!fijo.isFixedOffset()) { return i; } i++;
        if (!fijo.getOffset(Instant.ofEpochSecond(0L)).equals(menos5)) { return i; } i++;
        if (!fijo.getOffset(Instant.ofEpochSecond(1700000000L)).equals(menos5)) { return i; } i++;
        if (!fijo.getTransitions().isEmpty()) { return i; } i++;
        if (!fijo.getTransitionRules().isEmpty()) { return i; } i++;

        // -- una transicion: -05:00 pasa a -04:00 en 2020-03-08T02:00 local
        LocalDateTime cuando = LocalDateTime.of(2020, 3, 8, 2, 0);
        ZoneOffsetTransition t = ZoneOffsetTransition.of(cuando, menos5, menos4);
        List<ZoneOffsetTransition> unas = new ArrayList<ZoneOffsetTransition>();
        unas.add(t);
        ZoneRules r = ZoneRules.of(menos5, menos5, vacio, unas, sinReglas);

        if (r.isFixedOffset()) { return i; } i++;
        if (r.getTransitions().size() != 1) { return i; } i++;
        if (!r.getTransitions().get(0).equals(t)) { return i; } i++;

        long antes = t.toEpochSecond() - 1L;
        long despues = t.toEpochSecond() + 1L;
        if (!r.getOffset(Instant.ofEpochSecond(antes)).equals(menos5)) { return i; } i++;
        if (!r.getOffset(Instant.ofEpochSecond(despues)).equals(menos4)) { return i; } i++;
        if (!r.getOffset(Instant.ofEpochSecond(t.toEpochSecond())).equals(menos4)) { return i; } i++;

        // -- la transicion es un hueco: 02:00 no existe
        if (!t.isGap()) { return i; } i++;
        if (t.isOverlap()) { return i; } i++;
        if (!r.getValidOffsets(cuando).isEmpty()) { return i; } i++;
        if (r.getValidOffsets(LocalDateTime.of(2020, 1, 1, 12, 0)).size() != 1) { return i; } i++;
        if (r.getTransition(cuando) == null) { return i; } i++;
        if (r.getTransition(LocalDateTime.of(2020, 1, 1, 12, 0)) != null) { return i; } i++;

        // -- isValidOffset
        if (r.isValidOffset(cuando, menos5)) { return i; } i++;
        if (!r.isValidOffset(LocalDateTime.of(2020, 1, 1, 12, 0), menos5)) { return i; } i++;

        // -- next / previous
        ZoneOffsetTransition sig = r.nextTransition(Instant.ofEpochSecond(antes));
        if (sig == null || !sig.equals(t)) { return i; } i++;
        if (r.nextTransition(Instant.ofEpochSecond(despues)) != null) { return i; } i++;
        ZoneOffsetTransition prev = r.previousTransition(Instant.ofEpochSecond(despues));
        if (prev == null || !prev.equals(t)) { return i; } i++;
        if (r.previousTransition(Instant.ofEpochSecond(antes)) != null) { return i; } i++;

        // -- estandar y horario de verano
        if (!r.getStandardOffset(Instant.ofEpochSecond(despues)).equals(menos5)) { return i; } i++;
        if (!r.getDaylightSavings(Instant.ofEpochSecond(despues)).equals(Duration.ofHours(1))) { return i; } i++;
        if (!r.isDaylightSavings(Instant.ofEpochSecond(despues))) { return i; } i++;
        if (r.isDaylightSavings(Instant.ofEpochSecond(antes))) { return i; } i++;

        // -- las listas se copian: mutar la del llamador no cambia las reglas
        unas.clear();
        if (r.getTransitions().size() != 1) { return i; } i++;

        // -- igualdad por contenido, no por identidad
        List<ZoneOffsetTransition> otras = new ArrayList<ZoneOffsetTransition>();
        otras.add(t);
        ZoneRules r2 = ZoneRules.of(menos5, menos5, vacio, otras, sinReglas);
        if (!r.equals(r2)) { return i; } i++;
        if (r.hashCode() != r2.hashCode()) { return i; } i++;
        if (r.equals(fijo)) { return i; } i++;

        // -- null es NPE
        boolean npe = false;
        try { ZoneRules.of(menos5, menos5, null, vacio, sinReglas); } catch (NullPointerException e) { npe = true; }
        if (!npe) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) { System.out.println(run()); }
}
