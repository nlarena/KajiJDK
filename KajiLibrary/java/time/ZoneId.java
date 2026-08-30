package java.time;

import java.time.zone.ZoneRulesException;

// KajiLibrary's java.time.ZoneId — the identifier for a time-zone, the base type of ZoneOffset. A
// KajiLibrary subset: only fixed-offset zones are supported. of(id) accepts the offset forms ("Z",
// "+HH:MM", "-HH:MM", …) and returns a ZoneOffset; region-based ids (e.g. "Europe/Paris") need IANA
// tzdb transition rules — a data wall — so they raise ZoneRulesException. The abstract getRules()
// accessor of the JDK is therefore omitted. The package-private constructor limits subclassing to
// java.time (as in the JDK), where ZoneOffset extends it.
public abstract class ZoneId {

    ZoneId() {
    }

    public static ZoneId of(String zoneId) {
        if (zoneId.length() == 0 || zoneId.equals("Z")) {
            return ZoneOffset.of(zoneId);
        }
        char c = zoneId.charAt(0);
        if (c == '+' || c == '-') {
            return ZoneOffset.of(zoneId);
        }
        throw new ZoneRulesException(
            "Region-based zones require time-zone rules (tzdb), unsupported in KajiLibrary: " + zoneId);
    }

    // A KajiLibrary subset: with no OS time-zone lookup, the default zone is UTC.
    public static ZoneId systemDefault() {
        return ZoneOffset.UTC;
    }

    public abstract String getId();

    /**
     * Las reglas de esta zona: los desplazamientos y cuando cambian.
     *
     * <p>Es donde vive el horario de verano. En esta biblioteca las unicas zonas con reglas son las
     * de desplazamiento fijo (`ZoneOffset`), asi que las reglas son constantes.
     */
    public abstract java.time.zone.ZoneRules getRules();

    /**
     * Los identificadores cortos historicos (`EST`, `PST`, ...) mapeados a los largos.
     *
     * <p>Se aceptan **solo** cuando se los pide explicitamente, via `of(id, aliasMap)`: son
     * ambiguos --`CST` es tanto Chicago como Shanghai-- y por eso el JDK dejo de aceptarlos por
     * defecto. Este mapa existe para el codigo viejo que todavia los usa.
     */
    public static final java.util.Map<String, String> SHORT_IDS = ZoneId.shortIds();

    private static java.util.Map<String, String> shortIds() {
        java.util.Map<String, String> m = new java.util.HashMap<String, String>();
        m.put("ACT", "Australia/Darwin");
        m.put("AET", "Australia/Sydney");
        m.put("AGT", "America/Argentina/Buenos_Aires");
        m.put("ART", "Africa/Cairo");
        m.put("AST", "America/Anchorage");
        m.put("BET", "America/Sao_Paulo");
        m.put("BST", "Asia/Dhaka");
        m.put("CAT", "Africa/Harare");
        m.put("CNT", "America/St_Johns");
        m.put("CST", "America/Chicago");
        m.put("CTT", "Asia/Shanghai");
        m.put("EAT", "Africa/Addis_Ababa");
        m.put("ECT", "Europe/Paris");
        m.put("IET", "America/Indiana/Indianapolis");
        m.put("IST", "Asia/Kolkata");
        m.put("JST", "Asia/Tokyo");
        m.put("MIT", "Pacific/Apia");
        m.put("NET", "Asia/Yerevan");
        m.put("NST", "Pacific/Auckland");
        m.put("PLT", "Asia/Karachi");
        m.put("PNT", "America/Phoenix");
        m.put("PRT", "America/Puerto_Rico");
        m.put("PST", "America/Los_Angeles");
        m.put("SST", "Pacific/Guadalcanal");
        m.put("VST", "Asia/Ho_Chi_Minh");
        m.put("EST", "-05:00");
        m.put("MST", "-07:00");
        m.put("HST", "-10:00");
        return java.util.Collections.unmodifiableMap(m);
    }

    /**
     * Como `of(String)`, pero traduciendo antes por `aliasMap`.
     *
     * @throws java.time.DateTimeException si el identificador no se reconoce
     */
    public static ZoneId of(String zoneId, java.util.Map<String, String> aliasMap) {
        if (zoneId == null || aliasMap == null) {
            throw new NullPointerException();
        }
        String real = aliasMap.get(zoneId);
        return ZoneId.of(real != null ? real : zoneId);
    }

    /**
     * Una zona con ese nombre y ese desplazamiento fijo.
     *
     * <p>El prefijo tiene que ser vacio, `GMT`, `UTC` o `UT`: son los unicos que la especificacion
     * admite, porque el identificador resultante tiene que poder volver a parsearse.
     */
    public static ZoneId ofOffset(String prefix, ZoneOffset offset) {
        if (prefix == null || offset == null) {
            throw new NullPointerException();
        }
        if (prefix.length() == 0) {
            return offset;
        }
        if (!prefix.equals("GMT") && !prefix.equals("UTC") && !prefix.equals("UT")) {
            throw new IllegalArgumentException("Invalid prefix, must be GMT, UTC or UT: " + prefix);
        }
        return ZoneId.of(prefix + offset.getId());
    }

    /** La zona que `temporal` tiene, si tiene alguna. */
    public static ZoneId from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        ZoneId z = temporal.query(java.time.temporal.TemporalQueries.zone());
        if (z == null) {
            throw new java.time.DateTimeException(
                    "Unable to obtain ZoneId from TemporalAccessor: " + temporal);
        }
        return z;
    }

    /**
     * Los identificadores de zona disponibles.
     *
     * <p>Esta biblioteca no trae la base de datos de zonas horarias, asi que el conjunto esta
     * **vacio**. Se documenta en vez de fingir: devolver una lista inventada haria fallar a
     * `ZoneId.of` sobre sus propios elementos.
     */
    public static java.util.Set<String> getAvailableZoneIds() {
        return java.util.Collections.unmodifiableSet(new java.util.HashSet<String>());
    }

    /**
     * Esta zona reducida a su forma normal: un `ZoneOffset` si el desplazamiento es fijo.
     *
     * <p>Sirve para comparar: `ZoneId.of("UTC")` y `ZoneOffset.UTC` designan lo mismo y no son
     * `equals`, pero sus normalizadas si.
     */
    public ZoneId normalized() {
        java.time.zone.ZoneRules reglas = this.getRules();
        if (reglas != null && reglas.isFixedOffset()) {
            return reglas.getOffset(java.time.Instant.ofEpochSecond(0L));
        }
        return this;
    }

    /**
     * El nombre de la zona en esa region.
     *
     * <p>Devuelve el identificador para cualquier region: esta biblioteca no trae los nombres
     * localizados de zona. Se documenta en vez de fingir.
     */
    public String getDisplayName(java.time.format.TextStyle style, java.util.Locale locale) {
        if (style == null || locale == null) {
            throw new NullPointerException();
        }
        return this.getId();
    }

    public String toString() {
        return this.getId();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZoneId) {
            ZoneId other = (ZoneId) obj;
            return this.getId().equals(other.getId());
        }
        return false;
    }

    public int hashCode() {
        return this.getId().hashCode();
    }
}
