// Finding #17 — el override genérico no unifica una variable de tipo a nivel MÉTODO en el retorno.
// Para `<R> R foo(R x)` en una clase que implementa `interface I<T> { <R> R foo(R x); }`, el chequeo
// de retorno covariante compara el `R` de la impl contra el `R` de la interfaz sin unificarlos.
// #9 arregló las variables de tipo de CLASE; ésta es la análoga a nivel MÉTODO en posición de
// retorno *pelado*. Un método que devuelve un tipo parametrizado que apenas *contiene* R
// (p.ej. `<R> Stream<R> map(...)`) sí anda — solo el retorno `R` pelado falla.
//
// Esperado (javac real): OK.
// Síntoma del bug:       "el retorno de `foo` no es compatible con el de I: R no es un subtipo de R".
// Familia: #9. Bloquea `Stream.collect(Collector)` (`<R,A> R collect`), por eso `Collectors` está
// escrito y gate-clean pero todavía no se puede consumir.
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_17.java
public class Finding17 {

    interface Box<T> {
        <R> R unwrap(R x);
    }

    static class BoxImpl<T> implements Box<T> {
        public <R> R unwrap(R x) {
            return x;
        }
    }
}
