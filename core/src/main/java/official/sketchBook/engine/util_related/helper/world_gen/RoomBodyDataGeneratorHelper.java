package official.sketchBook.engine.util_related.helper.world_gen;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.game.util_related.enumerators.TileType;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.ALL;
import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.ENVIRONMENT;
import static official.sketchBook.game.util_related.constants.WorldC.TILE_SIZE_PX;

public class RoomBodyDataGeneratorHelper {
    /**
     * Valida se existem tiles ao redor do mesmo tipo, se sim nós não criamos.
     * Validamos também se são sólidos,
     * já que não podemos criar um corpo para uma tile caso esteja entre tiles sólidas do mesmo tipo
     *
     * @param tiles mapa bidimensional da lista de tiles existentes
     * @param x     coordenada X do mapa onde a tile se encontra
     * @param y     coordenada Y do mapa onde a tile se encontra
     */
    private static boolean shouldCreateBody(int x, int y, TileType[][] tiles) {
        TileType current = tiles[y][x];

        //  Verifica se existe uma tile em cima e se é do mesmo tipo
        boolean up = y + 1 < tiles.length
            &&
            tiles[y + 1][x] == current;
        //  Verifica se existe uma tile em baixo e se ela é do mesmo tipo
        boolean down = y - 1 >= 0
            && tiles[y - 1][x] == current;
        //  Verifica se existe uma tile à esquerda e se ela é do mesmo tipo
        boolean left = x - 1 >= 0
            && tiles[y][x - 1] == current;
        //  Verifica se existe uma tile à direita e se ela é do mesmo tipo
        boolean right = x + 1 < tiles[0].length
            && tiles[y][x + 1] == current;

        //  Apenas permitimos a criação da tile caso ela não esteja contida entre outras do mesmo tipo
        return !(up && down && left && right);
    }

    /// Cria uma body padrão para as tiles
    private static Body createBoxBodyForTiles(
        World world,
        TileType type,
        BodyDef.BodyType bodyType,
        int x,
        int y,
        int rotation,
        int width,
        int height,
        int totalRows,
        short categoryBit,
        short maskBit
    ) {
        //Dimensão padrão para as tiles
        float tileSize = TILE_SIZE_PX;
        //Encontra a posição y invertida em relação ao mundo
        //A coordenada y é invertida no world, portanto precisamos inverter ela para manter coerente
        int inverseYCord = (totalRows - 1) - y;

        /*
         * Tentamos encontrar a coordenada nos dois eixos, x e y.
         * Porém precisamos que essa coordenada seja o centro do objeto
         * e não leve em conta algum offset como no sistema padrão desta engine
         *
         * TODO: Vale considerar o pequeno offset do eixo y,
         *   esse offset para suavização
         *   talvez acabe causando inconsistências,
         *   importante analisar
         */

        //Obtém a posição do mundo no eixo x
        float worldX = (x + width / 2f) * tileSize;
        //Obtém a posição do mundo no eixo y
        float worldY = (inverseYCord - (height / 2f - 0.5f)) * tileSize;

        //  Criamos uma body quadrada padrão para tiles,
        //  se quisermos criar em formatos diferentes,
        //  vale a pena considerar manipular o formato da box

        return BodyCreatorHelper.createBox(
            world,                              //Mundo físico
            new Vector2(                        //Coordenadas
                worldX,
                worldY
            ),
            rotation,                           //  Rotação
            (width * tileSize),                 //  Largura por index e tamanho de tile
            (height * tileSize),                //  Altura por index e tamanho de tile
            bodyType,                           //  Tipo de corpo estático padrão
            type.getDensity(),                  //  Densidade do corpo
            type.getFriction(),                 //  Fricção pro box2d
            type.getRestt(),                    //  Restituição de força de colisão
            categoryBit,                        //  Quem a tile é na colisão
            maskBit                             //  Com quem a tile pode colidir na colisão
        );
    }

}
