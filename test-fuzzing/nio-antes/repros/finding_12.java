// Finding #12 — un enum anidado en una INTERFAZ se compila degenerado.
// Esperado: I$E igual que un enum anidado en clase (constantes, $VALUES, values/valueOf,
// static{}, ctor privado (String,int), final, extends Enum<E>).
// Sìntoma: I$E sale con solo `public ()V`, sin constantes ni maquinaria.
public interface I {
    enum E { A, B }
}
