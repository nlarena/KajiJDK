package javax.annotation.processing;
// Soporte mínimo del round loop de APT (JSR 269, fase 2): una implementación concreta de
// ProcessingEnvironment que el driver de la VM reifica para pasarle al `init(env)` del processor.
// En el MVP no hay Messager/Filer reales todavía, así que ambos devuelven null; un processor que
// sólo imprime (System.out.println) no los necesita.
public class ProcessingEnvironmentImpl implements ProcessingEnvironment {
    public Messager getMessager() { return null; }
    public Filer getFiler() { return null; }
}
