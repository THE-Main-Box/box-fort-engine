package official.sketchBook.engine.world_gen;

import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.game.util_related.body.world_gen.RoomBodyDataFactory;

import java.util.HashMap;
import java.util.Map;

public class PlayableRoom {

    /// Referência ao mundo físico do box2d
    private final World physicsWorld;
    /// Flag que dita se podemos ou acessar o world
    private final boolean physicsWorldExists;

    /// Lista de referência a todas as tiles que estamos usando
    private final Map<Integer, TileModel> tileIdMap;

    /// Grade contendo o id dos tipos de body
    private int[][] bodyIdGrid;
    /// Grade contendo os id das tile
    private int[][] grid;
    /// Dimensões da grid
    private int gridWidth, gridHeight;

    public boolean disposed = false;

    public PlayableRoom(World physicsWorld) {
        //Inicializamos com o world
        this.physicsWorld = physicsWorld;
        //Validamos se podemos usar o world
        this.physicsWorldExists = physicsWorld != null;

        this.tileIdMap = new HashMap<>();
    }


    /// Inicia a sala
    public void initRoomGrid(int[][] grid) {
        //Atualiza a grid e seus valores, junto com campos que determinam suas dimensões
        this.grid = grid;
        this.gridHeight = grid.length;
        this.gridWidth = grid[0].length;

        //Se pudermos acessar o world inicializamos a grid de id de corpos
        if (!physicsWorldExists) return;
        this.bodyIdGrid = new int[gridHeight][gridWidth];
    }

    /**
     * Insere no map de tiles uma nova tile com os dados passados
     *
     * @param id         id único da tile, servirá para identificar ela mais pra frente
     * @param bodyTypeId id que se refere ao tipo de corpo que a tile terá no mundo físico
     */
    public void insertNewTile(
        int id,
        int bodyTypeId
    ) {
        //Se não tivermos inserido uma tile com id passado prosseguimos
        if (tileIdMap.containsKey(id)) return;
        //Inserimos o modelo dentro do map
        tileIdMap.put(
            id,
            new TileModel(      //Iniciamos um modelo novo
                id,             //Passamos o id do modelo
                bodyTypeId      //Passamos o id do tipo de corpo
            )
        );

    }

    /// Cria as bodies das tiles
    public void createTileBodies() {
        if (!physicsWorldExists) return;
        prepareBodyIdGrid();
        RoomBodyDataFactory.createRoomBodies(
            bodyIdGrid,
            physicsWorld
        );

    }

    /// Atualiza a grid de id de body
    private void prepareBodyIdGrid() {
        for (int h = 0; h < grid.length; h++) {
            for (int w = 0; w < grid[0].length; w++) {

                //Obtemos a tile da coordenada passada
                TileModel currentTile = tileIdMap.get(
                    grid[h][w]
                );

                //Se houver uma tile e sua id de body tiver sido passada
                if (currentTile != null && currentTile.getBodyId() != null) {
                    bodyIdGrid[h][w] = currentTile.getBodyId();
                }

            }
        }
    }

    /**
     * Imprime o conteúdo da matriz formatado por linhas e colunas.
     * Útil para debug de posicionamento e verificação de IDs.
     */
    public void printMatrixContent(int[][] grid) {
        if (grid == null || grid.length == 0) {
            System.out.println("A matriz está vazia ou nula.");
            return;
        }

        System.out.println("--- Início do Debug da Matrix (" + grid.length + "x" + grid[0].length + ") ---");

        for (int h = 0; h < grid.length; h++) {
            // Início da linha: um colchete para organizar visualmente
            System.out.print("[ ");

            for (int w = 0; w < grid[h].length; w++) {
                // Imprime o valor seguido de um espaço ou tabulação para manter alinhado
                // Use \t (tab) se os IDs tiverem muitos dígitos diferentes
                System.out.print(grid[h][w] + " ");
            }

            // Fim da linha: fecha o colchete e pula para a próxima linha do console
            System.out.println("]");
        }

        System.out.println("--- Fim do Debug da Matrix ---");
    }

    public void dispose() {
        if (disposed) return;

        disposeLists();

        disposed = true;
    }

    /// Limpa todas as listas
    private void disposeLists() {
        tileIdMap.clear();
    }

    public Map<Integer, TileModel> getTileIdMap() {
        return tileIdMap;
    }

    public int[][] getBodyIdGrid() {
        return bodyIdGrid;
    }

    public int[][] getGrid() {
        return grid;
    }
}
