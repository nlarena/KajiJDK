public class RA {
    public static class F {
        private String n;
        protected F(String n) { this.n = n; }
        public String nombre() { return this.n; }
    }
    public void tomar(RA.F f) { }
}
