package official.sketchBook.game.util_related.constants;

import static official.sketchBook.game.util_related.constants.WorldC.TILE_SIZE_PX;

public class RenderingC {
    /// Taxa de fps que tentaremos seguir
    public static float FPS_TARGET;

    /// Quantidade de tiles que podemos visualizar na largura
    public static final int TILES_VIEW_WIDTH = 20;

    /// Quantidade de tiles que podemos visualizar na altura
    public static final int TILES_VIEW_HEIGHT = 12;

    /// Tamanho da largura da janela em metros
    public static final float VIRTUAL_WIDTH_PX = TILE_SIZE_PX * TILES_VIEW_WIDTH;
    /// Tamanho da altura da janela em metros
    public static final float VIRTUAL_HEIGHT_PX = TILE_SIZE_PX * TILES_VIEW_HEIGHT;

    static {
        FPS_TARGET = 60;
    }
}
