package official.sketchBook.game.util_related.constants;

public class WorldConstants {
    /// Tamanho das tiles em pixels
    public static final int TILE_SIZE_PX = 8;

    public static class PlayerConstants {

        /// Dimensões do jogador
        public static final float
            WIDTH = 16,
            HEIGHT = 16;

        /// Valores de componente de salto
        public static final float
            JUMP_FORCE = 40,
            FALL_SPEED_AFTER_JUMP_CANCEL = 100,
            COYOTE_T = 0.1f,
            JUMP_BUFF_T = 0.2f;

        public static final float
            MAX_SPEED_X = 200,
            MAX_SPEED_Y = 500,
            X_DECELERATION = 999,
            Y_DECELERATION = 0;
    }
}
