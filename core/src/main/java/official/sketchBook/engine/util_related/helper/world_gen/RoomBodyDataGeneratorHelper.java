package official.sketchBook.engine.util_related.helper.world_gen;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.game.util_related.enumerators.TileType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static official.sketchBook.game.util_related.constants.WorldC.TILE_SIZE_PX;

public class RoomBodyDataGeneratorHelper {

    /// Registro de factories por tipo de tile
    private static final Map<TileType, TileBodyFactory> tileFactories = new HashMap<>();

    /// Inicializa as factories padrão
    static {
        register(TileType.BLOCK, TileFactory::createBlockBody);
    }

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
    public static Body createBoxBodyForTiles(
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

    public static List<Body> buildTileMergedBodies(TileType[][] tiles, World world) {
        int rows = tiles.length;
        int cols = tiles[0].length;
        boolean[][] visited = new boolean[rows][cols];
        List<Body> bodies = new ArrayList<>();

        // Percorre o mapa de cima para baixo
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {

                // Verifica se ainda não visitou e se é sólida e se devemos criar ela
                if (!visited[y][x] && tiles[y][x].isSolid() && shouldCreateBody(x, y, tiles)) {
                    TileType currentType = tiles[y][x];

                    int width = 1;
                    int height = 1;

                    // Se a tile pode ser merged, tenta encontrar o maior retângulo
                    if (currentType.isMergeable()) {
                        // Encontra a largura
                        while (x + width < cols &&
                            tiles[y][x + width] == currentType &&
                            !visited[y][x + width]
                        ) {
                            width++;
                        }

                        // Encontra a altura
                        boolean done = false;
                        while (!done &&
                            y + height < rows
                        ) {
                            for (int dx = 0; dx < width; dx++) {
                                if (tiles[y + height][x + dx] != currentType ||
                                    visited[y + height][x + dx]
                                ) {
                                    done = true;
                                    break;
                                }
                            }
                            if (!done) {
                                height++;
                            }
                        }
                    }
                    // Se não pode ser merged, deixa width/height como 1

                    // Marca como visitado
                    for (int dy = 0; dy < height; dy++) {
                        for (int dx = 0; dx < width; dx++) {
                            visited[y + dy][x + dx] = true;
                        }
                    }

                    // Obtém a factory e cria as bodies
                    TileBodyFactory factory = getFactory(currentType);
                    List<Body> createdBodies = factory.createBodies(
                        world,
                        currentType,
                        x, y,
                        width, height,
                        rows
                    );

                    bodies.addAll(createdBodies);
                }
            }
        }

        return bodies;
    }

    /**
     * Converte um mapa de inteiros para um mapa de tipos de tiles
     * Cada ID é convertido para seu TileType correspondente
     *
     * @param tileIds matriz bidimensional com IDs de tiles
     * @return matriz bidimensional de TileType
     */
    public static TileType[][] convertToTileTypeMap(int[][] tileIds) {
        int rows = tileIds.length;
        int cols = tileIds[0].length;
        TileType[][] tiles = new TileType[rows][cols];

        /// Percorre cada ID e obtém seu TileType correspondente
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                tiles[y][x] = TileType.fromId(tileIds[y][x]);
            }
        }

        return tiles;
    }

    /**
     * Converte um mapa de tipos de tiles para um mapa de inteiros
     * Cada TileType é convertido para seu ID correspondente
     *
     * @param tiles matriz bidimensional de TileType
     * @return matriz bidimensional com IDs de tiles
     */
    public static int[][] convertToIntMap(TileType[][] tiles) {
        int rows = tiles.length;
        int cols = tiles[0].length;
        int[][] tileIds = new int[rows][cols];

        /// Percorre cada TileType e obtém seu ID correspondente
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                tileIds[y][x] = tiles[y][x].getId();
            }
        }

        return tileIds;
    }

    /// Registra uma factory customizada para um tipo de tile
    public static void register(TileType tileType, TileBodyFactory factory) {
        tileFactories.put(tileType, factory);
    }

    /// Obtém a factory para um tipo de tile
    private static TileBodyFactory getFactory(TileType type) {
        return tileFactories.getOrDefault(type, TileFactory::createBlockBody);
    }
}
