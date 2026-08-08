// Finding #18 — un enum en un paquete NOMBRADO, SIN constructor explícito, sintetiza un
// constructor degenerado `public ()V` en vez del correcto `private (String, int)` (name + ordinal).
// El mismo enum en el paquete DEFAULT sale bien, y agregar un constructor explícito (aunque vacío)
// también lo arregla. O sea: el camino de síntesis del ctor implícito del enum falla solo para
// enums de paquete nombrado.
//
// Esperado (javac real): `private Repro18(String, int)`.
// Síntoma del bug:       `public repro.Repro18();` — ctor público sin args (queda instanciable con
//                        `new`, y le falta el (String,int) que usan values()/valueOf).
// Verificar con:         javap -p KajiLibrary/repros/repro/Repro18.class | grep 'Repro18('
// Workaround (KajiLibrary): un constructor explícito vacío en Month/DayOfWeek/ChronoField/ChronoUnit.
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_18.java   (luego javap -p el .class)
package repro;

public enum Repro18 {

    A, B;

    public int getValue() {
        return this.ordinal() + 1;
    }
}
