package official.sketchBook.game.util_related.constants;

import static official.sketchBook.game.util_related.constants.WorldConstants.TILE_SIZE_PX;

public class RenderingConstants {
    /// Taxa de fps que tentaremos seguir
    public static float FPS_TARGET;

    /// Quantidade de tiles que podemos visualizar na largura
    public static final int TILES_VIEW_WIDTH = 78;

    /// Quantidade de tiles que podemos visualizar na altura
    public static final int TILES_VIEW_HEIGHT = 42;

    /// Tamanho da largura da janela em metros
    public static final float VIRTUAL_WIDTH_PX = TILE_SIZE_PX * TILES_VIEW_WIDTH;
    /// Tamanho da altura da janela em metros
    public static final float VIRTUAL_HEIGHT_PX = TILE_SIZE_PX * TILES_VIEW_HEIGHT;

    public static float zoom;

    static {
        FPS_TARGET = 60;

        updateZoom(1);
    }

    ///Como o zoom é o único valor que será alterado de fato, junto da escala, podemos fazer isso daqui
    public static void updateZoom(float newZoom){
        if(newZoom < 0) return;
        zoom = newZoom;
    }
}
