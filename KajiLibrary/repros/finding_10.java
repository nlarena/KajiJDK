// Finding #10 — clase anónima CAPTURADORA mal compilada.  ESTADO VIVO: ✅ ARREGLADO.
// Una anónima que captura la instancia envolvente (referencia un miembro de instancia externo)
// no recibía `this$0` (su ctor salía `()V` en vez de `(EnclosingClass)`) y su .class anidado no
// se emitía. Las anónimas NO capturadoras (en método static) funcionaban.
//
// Esperado (javac real): emite Enclosing$1.class con ctor (Enclosing).
// Se conserva como test de regresión (debe compilar y emitir el .class anidado).
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_10.java
public class Enclosing {
    int n = 3;
    Runnable make() {
        return new Runnable() {
            public void run() { int x = n; }   // captura `n` del enclosing
        };
    }
}
