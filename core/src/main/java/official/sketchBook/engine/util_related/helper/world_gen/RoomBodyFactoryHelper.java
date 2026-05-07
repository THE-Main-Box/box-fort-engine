package official.sketchBook.engine.util_related.helper.world_gen;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.game.util_related.enumerators.TileBodyType;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.game.util_related.body.world_gen.RoomBodyFactory.getFactory;

public class RoomBodyFactoryHelper {

    /**
     * Valida se existem tiles ao redor do mesmo tipo, se sim nós não criamos.
     * Validamos também se são sólidos,
     * já que não podemos criar um corpo para uma tile caso esteja entre tiles sólidas do mesmo tipo
     *
     * @param tiles mapa bidimensional da lista de tiles existentes
     * @param x     coordenada X do mapa onde a tile se encontra
     * @param y     coordenada Y do mapa onde a tile se encontra
     */
    private static boolean shouldCreateBody(int x, int y, TileBodyType[][] tiles) {
        TileBodyType current = tiles[y][x];

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

    /**
     * Cria as bodies de uma sala num world, recebendo uma lista bidimensional contendo os dados das Body da tile
     *
     * @param tiles dados das bodys das tiles
     * @param world mundo onde iremos instanciar as body
     * @return Retornamos uma lista contendo uma referência das bodies que criamos
     */
    public static List<Body> buildWorldTileBodies(TileBodyType[][] tiles, World world) {
        //Obtém a quantidade das linhas, também conhecida como a altura
        int rows = tiles.length;
        //Obtém a quantidade das colunas, também conhecida como largura do mundo
        int cols = tiles[0].length;

        //Matriz contendo uma validação para saber se já visitamos aquela tile
        boolean[][] visited = new boolean[rows][cols];

        //Lista das bodies que criamos
        List<Body> bodies = new ArrayList<>();

        /*
         *   Vale lembrar que temos uma body para cada tipo de body de tile,
         *  ou seja uma slope corresponderá a uma body diferente de um bloco quadrado comum
         */

        // Percorre o mapa de cima para baixo
        for (int y = 0; y < rows; y++) {    //Da esquerda para direita
            for (int x = 0; x < cols; x++) {

                // Verifica se ainda não visitou e se é sólida e se devemos criar ela
                if (!visited[y][x]
                    && tiles[y][x].isSolid()
                    && shouldCreateBody(
                    x,
                    y,
                    tiles)
                ) {
                    //Obtemos uma referencia do tipo atual
                    TileBodyType currentType = tiles[y][x];

                    //Dimensões padrão
                    int width = 1,
                        height = 1;

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
                        x,
                        y,
                        width,
                        height,
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
     * Cada ID é convertido para seu tipo de corpo de tile correspondente
     *
     * @param tileIds matriz bidimensional com IDs de tiles
     * @return matriz bidimensional contendo os dados da body de cada tile
     */
    public static TileBodyType[][] convertToBodyTypeMap(int[][] tileIds) {
        int rows = tileIds.length;
        int cols = tileIds[0].length;
        TileBodyType[][] tiles = new TileBodyType[rows][cols];

        /// Percorre cada ID e obtém seu TileType correspondente
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                tiles[y][x] = TileBodyType.fromId(tileIds[y][x]);
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
    public static int[][] convertToIntMap(TileBodyType[][] tiles) {
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
}
