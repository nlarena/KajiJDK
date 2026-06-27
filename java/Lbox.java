// Objeto con un campo long (categoría-2 → 2 slots / 8 bytes en el heap).
class Lbox {
    long val;
    int tag;   // campo DESPUÉS del long: prueba que el offset se corrió bien
}
