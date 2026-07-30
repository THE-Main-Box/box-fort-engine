package official.sketchBook.game.util_related.constants;

public class GameConfigConstants {
    public static float
        FPS_TARGET,
        UPDATE_TIME_SCALE;

    public static float
        ANIMATION_UPDATE_RATE,
        PASSENGER_POSITION_MASS_CALC_RATE
    ;

    static {
        UPDATE_TIME_SCALE = 1f;

        UPDATE_TARGET_FPS(60);
    }

    public static void UPDATE_TARGET_FPS(float newFPS) {
        FPS_TARGET = newFPS;
        ANIMATION_UPDATE_RATE = FPS_TARGET / 2;
        PASSENGER_POSITION_MASS_CALC_RATE = FPS_TARGET / 2;
    }
}
