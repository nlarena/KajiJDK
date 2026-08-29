// Parte de SuperProbe (#265): un `default`, que vive en una SUPERINTERFAZ y no en la clase.
public interface SuperProbe_I {
    default int g() { return 2; }
}
