// Compactación + reescritura de puntero interno. Precargamos los mirrors asignando
// los warm-ups a locales (sin descartar → sin `pop`, que no está implementado), así
// la basura `dead` queda por encima de todos los mirrors y h/h.pet pueden deslizarse.
class CompactRef {
    static int run() {
        Holder warm1 = new Holder();  // warm-up: carga el mirror de Holder
        Animal warm2 = new Animal();  // warm-up: carga el mirror de Animal
        Animal dead = new Animal();   // basura (por encima de los mirrors)
        Holder h = new Holder();      // vivo
        h.pet = new Animal();         // vivo, alcanzable vía h.pet
        dead = h.pet;                 // 'dead' sin dueño → hueco
        return h.pet.legs;            // al compactar, h y h.pet se mueven; h.pet se reescribe → 4
    }
}
