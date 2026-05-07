package official.sketchBook.engine.util_related.helper.world_gen;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.game.util_related.enumerators.TileBodyType;

import java.util.List;

/// Interface para estratégia de criação de bodies
@FunctionalInterface
public interface TileBodyFactory {
    /**
     * Cria bodies para uma tile (pode ser 1 ou múltiplas)
     *
     * @param world mundo físico
     * @param type tipo da tile
     * @param mapX coordenada X no mapa
     * @param mapY coordenada Y no mapa
     * @param width largura em tiles
     * @param height altura em tiles
     * @param totalRows total de linhas do mapa
     * @return lista de bodies criadas (geralmente 1, mas pode ser mais)
     */
    List<Body> createBodies(
        World world,
        TileBodyType type,
        int mapX,
        int mapY,
        int width,
        int height,
        int totalRows
    );
}
