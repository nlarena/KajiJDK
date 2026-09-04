public interface RI {
    public static class Attr {
        private String n;
        protected Attr(String n) { this.n = n; }
        protected String nombre() { return this.n; }
    }
}
