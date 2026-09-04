public class MfProbe {
    static void p(String pat, Object[] args) {
        try { System.out.println(">" + java.text.MessageFormat.format(pat, args) + "<"); }
        catch (Exception e) { System.out.println("EX " + e.getClass().getName() + ": " + e.getMessage()); }
    }
    public static void main(String[] a) {
        p("{ 0 }", new Object[]{"X"});
        p("llaves {} sueltas", new Object[]{"X"});
        p("a {5} b {0}", new Object[]{"X","Y","Z","W","V","U"});
        p("a ''b'' {0}", new Object[]{"X"});
        p("esc '{0}' {1}", new Object[]{"X","Y"});
        p("out {9}", new Object[]{"X"});
        System.out.println("charset=" + java.nio.charset.Charset.defaultCharset().name());
        System.out.println("zone=" + java.time.ZoneOffset.UTC);
        System.out.println("iso=" + java.time.ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(0), java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        System.out.println("iso2=" + java.time.ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(1000,123456), java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
}
