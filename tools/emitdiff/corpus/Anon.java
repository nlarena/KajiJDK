// Captura de la instancia envolvente (`this$0`) por USO (§8.1.3): una clase interna/local/anónima
// solo materializa el campo `this$0` si su cuerpo realmente usa el enclosing. El diferencial residual
// contra javac queda en el **idiom del constructor** (javac hace `Objects.requireNonNull(this$0)` y
// pone las capturas *antes* del `super()`; nuestro emisor las pone después y omite el chequeo) y en el
// contador de nombres de clases locales (`$1L2` vs `$2L2`) — ambos ajenos a esta captura por uso.
public class Anon {
  interface R { void run(); }

  int f = 7;

  // Clase anónima que NO usa la instancia envolvente: no debe capturar this$0 (§8.1.3).
  R ignores() {
    return new R() { public void run() { int x = 1; } };
  }

  // Clase anónima que SÍ lee un campo del enclosing: captura this$0.
  R usesField() {
    return new R() { public void run() { int x = f; } };
  }

  // Clase local que NO usa la instancia envolvente: captura solo su local (val$p), no this$0.
  R localIgnores(int p) {
    class L implements R { public void run() { int x = p; } }
    return new L();
  }

  // Clase local que SÍ usa el enclosing: captura val$p y this$0.
  R localUses(int p) {
    class L2 implements R { public void run() { int x = f + p; } }
    return new L2();
  }
}
