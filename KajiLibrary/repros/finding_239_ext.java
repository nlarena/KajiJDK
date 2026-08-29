// El "otro lado" de #239/#245: declara los tipos ANIDADOS que finding_239.java nombra.
// Se compila PRIMERO y por separado, para que finding_239 los vea por `-cp`, que es la
// condicion del finding (un tipo anidado de otra unidad de compilacion, leido del classpath).
public class finding_239_ext {
    public interface Kind { int f(); }
    public interface Marker { }
}
