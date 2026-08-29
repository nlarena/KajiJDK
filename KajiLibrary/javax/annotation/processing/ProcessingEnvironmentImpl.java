package javax.annotation.processing;
// Soporte del round loop de APT (JSR 269): una implementación concreta de ProcessingEnvironment que
// el driver de la VM reifica para pasarle al `init(env)` del processor. `getFiler()` entrega un
// KajiFiler (fase 4), la pieza que un processor usa para fabricar los fuentes que genera: cada
// `createSourceFile` empuja el (nombre, StringWriter) por el puente nativo y el round loop lo drena
// para reincorporar lo generado. `getMessager()` sigue en null (un processor imprime por AptTrace).
public class ProcessingEnvironmentImpl implements ProcessingEnvironment {
    public Messager getMessager() { return null; }
    public Filer getFiler() { return new KajiFiler(); }
}
