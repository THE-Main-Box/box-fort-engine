package official.sketchBook.game.util_related.constants;

public class PhysicsConstants {

    /// Pixels Per Meters, constante que determina o quantos pixels correspondem a 1 metro
    public static final float PPM = 100;

    /// Iterações de velocidade para o box2d
    public static int VELOCITY_ITERATIONS;
    /// Iterações de posição para o box2d
    public static int POSITION_ITERATIONS;

    /// Acumulador máximo para evitar travamento acidental
    public static final float MAX_ACCUMULATOR = 0.1f;// Evita travar o PC se o frame demorar muito

    static {
        VELOCITY_ITERATIONS = 8;
        POSITION_ITERATIONS = 3;

    }

    public static float toMeters(float pixels) {
        return pixels / PPM;
    }

    public static float toPixels(float meters) {
        return meters * PPM;
    }

}
