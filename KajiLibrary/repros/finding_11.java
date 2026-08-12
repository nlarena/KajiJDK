// Finding #11 — las llamadas estáticas a utilidades de java.util no resuelven.
// `Objects.requireNonNull(x)` falla con "no se encuentra el símbolo: Objects" aunque el TIPO
// carga bien (`Objects.class` como expresión funciona) y java/util/Objects.class está en el
// classpath de referencia. Igual para Arrays.* y Collections.*. Las utilidades de java.lang
// (Math.max, System.arraycopy) sí andan.
//
// Causa sospechada: la tabla de MIEMBROS de esas clases java.util no carga (el lector de
// classfile tropieza con alguna firma — p.ej. requireNonNull(T, Supplier<String>)/bounds
// genéricos), así que la resolución de miembros no encuentra nada pero el tipo-solo sí.
// Artefacto que dispara el bug: .jdk25_tmp/classes/java.base/java/util/Objects.class
// Estado vivo confirmado: SIGUE FALLANDO (✗).
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_11.java
import java.util.Objects;

public class M {
    void r(Object x) { Objects.requireNonNull(x); }
}
