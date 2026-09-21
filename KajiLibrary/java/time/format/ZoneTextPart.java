package java.time.format;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalQueries;
import java.util.Set;

// The zone's NAME: "Pacific Standard Time", or in Spanish "hora estandar del Pacifico".
//
// ===============================================================================================
// WHY THIS IS POSSIBLE AND THE NAME TABLE IS NOT NEEDED
// ===============================================================================================
//
// A zone name is CLDR data: a table per zone and per language, and per whether the instant falls in
// summer time as well. This library does not ship it, and that is why the four forms of
// `appendZoneText` were absent: writing `Europe/Paris` where a name goes is not being incomplete,
// it is saying "this is the name" when it is not.
//
// What changed is a measurement, not a decision. `ZoneId.of("Europe/Madrid")` THROWS in this
// library --there is no zone database: see `ZoneId.getAvailableZoneIds`, which returns an empty
// set-- so the only zone a formatter can ever receive here is a `ZoneOffset`. And for a
// `ZoneOffset` the JDK uses no table at all: it writes the identifier as it stands, `+05:00`, or
// `Z` for zero, in any language and in any style. Measured against JDK 25 in this library's six
// locales and in both styles.
//
// **So this is complete for what the library can represent, not for what the API promises.** The
// day `ZoneId.of` accepts region identifiers, this part starts lying for every named zone and the
// CLDR table has to be brought in. It is written here so that that day it is seen at once.
//
// `appendGenericZoneText` is the version that does not tell summer time apart --"Pacific Time"
// instead of "Pacific Standard Time". Over a `ZoneOffset` both give the same, for the same reason:
// there is no name to vary.
final class ZoneTextPart extends Part {

    private final TextStyle style;
    private final Set<ZoneId> preferred;
    private final boolean generic;

    ZoneTextPart(TextStyle style, Set<ZoneId> preferred, boolean generic) {
        this.style = style;
        this.preferred = preferred;
        this.generic = generic;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        ZoneId z = ctx.query(TemporalQueries.zoneId());
        if (z == null) {
            return ctx.missingOrThrow("ZoneText");
        }
        if (z instanceof ZoneOffset) {
            out.append(z.getId());
            return true;
        }
        // A named zone. It cannot get here --`ZoneId.of` does not build them-- and if one day it
        // does, all there is is the identifier, which is not the name. It is written all the same,
        // which is the JDK's fallback when the locale has no name for that zone.
        out.append(z.getId());
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        // The only thing that can be recognized is the only thing that can be written: an offset.
        // Recognizing names would need the same CLDR table, and backwards at that.
        return new ZoneIdPart(ZoneIdPart.ZONE_OR_OFFSET).parse(ctx, text, pos);
    }

    // Neither the style nor the set of preferred zones changes what comes out over an offset: the
    // first because there is no name to shorten, the second because it only serves to break ties
    // between zones that share a name. They are kept all the same --they are part of what was asked
    // for-- and they show up here. The JDK writes `ZoneText(...)` for the generic version too; the
    // name is copied.
    @Override
    public String toString() {
        return "ZoneText(" + this.style + ")";
    }
}
