public class Base {
    int valor = 7;
    public class Inner {
        public int leer() { return Base.this.valor; }
    }
}
