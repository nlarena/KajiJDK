// long en un CAMPO: putfield/getfield de 8 bytes. `tag` es un int DESPUÉS del long;
// si el layout no fuera width-aware, escribir `tag` pisaría los 8 bytes de `val` y
// `return b.val` no daría 42. Que dé 42 prueba que `val` ocupa 2 slots y `tag` quedó
// más allá, sin solaparse.
class LongField {
    static long run() {
        Lbox b = new Lbox();
        b.val = 42L;     // putfield long (8 bytes en offset 8..15)
        b.tag = 7;       // putfield int  (offset 16, porque val ocupa 2 slots)
        return b.val;    // 42L si el write de tag no pisó val
    }
}
