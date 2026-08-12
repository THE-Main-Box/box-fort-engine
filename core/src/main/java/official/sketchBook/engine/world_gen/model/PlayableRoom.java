package official.sketchBook.engine.world_gen.model;

import official.sketchBook.engine.game_object_related.base_game_object.BaseRoomGameObject;

import java.util.*;

public class PlayableRoom {

    /// Id da sala
    protected final int roomId;

    /// Referência a posição da grid de world
    protected float
        roomXPos,
        roomYPos;

    /// Dimensões da grid
    public int
        gridWidth,
        gridHeight;

    /// Dimensões da sala vindas da grid
    public int
        roomWidthPx,
        roomHeightPx;

    /// Grade em camadas
    protected int[][][] grid;

    /// Lista de referência a todas as tiles que estamos usando
    public Set<Integer> registeredTileIds;

    /// Lista de referência a objetos presentes na sala, são apenas referencias,
    /// e não são gerenciados aqui dentro
    public List<BaseRoomGameObject> roomGameObjectList;

    public boolean disposed = false;

    public PlayableRoom(
        int id,
        float roomX,
        float roomY
    ) {
        this.roomId = id;                                   //Id da sala
        this.roomXPos = roomX;                              //Posição que iremos começar a gerar a sala no eixo x
        this.roomYPos = roomY;                              //Posição que iremos começar a gerar a sala no eixo y

        this.registeredTileIds = new HashSet<>();               //Inicializamos o set
        this.roomGameObjectList = new ArrayList<>();            //Iniciamos o map
    }

    public void registerTileId(int id) {
        if (!registeredTileIds.add(id)) {
            throw new IllegalArgumentException("já possuímos uma tile marcada com esse id");
        }
    }

    /**
     * Inicializa a grid de uma sala jogável
     *
     * @param roomTileGrid grid contendo os ids das tiles, em camadas, registradas na sala previamente
     */
    public void initRoomGrid(
        int[][][] roomTileGrid,
        int pixelsPerTile
    ) {
        //Atualiza a grid completamente
        this.grid = roomTileGrid;

        /*
        A primeira lista da grid são as camadas, é uma lista em 3 dimensões,
         portanto isso me permite muita coisa futuramente,
         mas agora estaremos lidando com 2d apenas...

        Como a grid é uma lista de lista,
         a length é a altura, pois se refere à primeira lista,
          ou seja à quantidade de linhas.

          Já [0].length se refere à quantidade de colunas,
           pois estamos acessando a primeira linha
          */

        //Determina a altura da grid obtendo a altura da primeira camada
        this.gridHeight = roomTileGrid[0]
            .length;

        //Determina a largura da grid obtendo a largura da primeira camada
        this.gridWidth = roomTileGrid[0][0].length;

        //Determina a largura da sala em pixels com base na constante de dimensões do sistema de grid
        this.roomWidthPx = this.gridWidth * pixelsPerTile;

        //Determina a altura da sala em pixels com base na constante de dimensões do sistema de grid
        this.roomHeightPx = this.gridHeight * pixelsPerTile;
    }

    public void addNewRoomGameObject(BaseRoomGameObject roomObject) {
        this.roomGameObjectList.add(roomObject);
    }

    public void removeRoomObject(BaseRoomGameObject roomObjectToRemove) {
        this.roomGameObjectList.remove(roomObjectToRemove);
    }

    /**
     * Altera o id de uma tile em uma posi��o espec�fica da grid.
     * �nico ponto de escrita individual da grid ? qualquer sistema que
     * precise modificar uma tile em runtime (escava��o, portas, etc)
     * passa por aqui, nunca acessando o array diretamente.
     *
     * @throws IllegalArgumentException se o id n�o foi registrado previamente
     *                                  via addNewTileModel (evita tile "fantasma" sem TileModel associado)
     */
    public void setGridDataAt(int z, int y, int x, int tileId) {
        if (tileId != 0 && !registeredTileIds.contains(tileId)) {
            throw new IllegalArgumentException(
                "tentativa de setar tile id " + tileId + " sem tile registrada"
            );
        }
        grid[z][y][x] = tileId;
    }

    public int getGridDataAt(int z, int y, int x) {
        return grid[z][y][x];
    }

    /**
     * Imprime no console o conte�do de uma camada espec�fica da grid, formatado
     * como matriz. �til pra debug visual r�pido sem precisar acessar o array
     * diretamente de fora.
     *
     * @param z �ndice da camada a ser impressa
     * @throws IndexOutOfBoundsException se z estiver fora do intervalo de camadas existentes
     */
    public void printMatrixContentAtLayer(int z) {
        int layerCount = getLayerCount();

        if (layerCount == 0) {
            System.out.println("A grid ainda n�o foi inicializada.");
            return;
        }

        if (z < 0 || z >= layerCount) {
            throw new IndexOutOfBoundsException(
                "camada " + z + " fora do intervalo (0 a " + (layerCount - 1) + ")"
            );
        }

        System.out.println("--- In�cio do Debug da Matrix [camada " + z + "] (" + gridHeight + "x" + gridWidth + ") ---");

        for (int y = 0; y < gridHeight; y++) {
            StringBuilder line = new StringBuilder("[");

            for (int x = 0; x < gridWidth; x++) {
                line.append(getGridDataAt(z, y, x)).append(",");
            }

            line.append("]");
            System.out.println(line);
        }

        System.out.println("--- Fim do Debug da Matrix [camada " + z + "] ---");
    }

    /**
     * Imprime todas as camadas da grid, uma ap�s a outra.
     */
    public void printMatrixContentOfAllLayers() {
        int layerCount = getLayerCount();

        for (int z = 0; z < layerCount; z++) {
            printMatrixContentAtLayer(z);
        }
    }

    public void dispose() {
        if (disposed) return;

        executeDispose();

        disposed = true;
    }

    protected void executeDispose() {
        disposeLists();

        registeredTileIds = null;
        roomGameObjectList = null;
    }

    /// Limpa todas as listas
    protected void disposeLists() {
        clearTileIdRegister();
        clearObjectList();
    }

    public void clearObjectList(){
        roomGameObjectList.clear();
    }

    public void clearTileIdRegister(){
        registeredTileIds.clear();
    }

    public float getRoomXPos() {
        return roomXPos;
    }

    public float getRoomYPos() {
        return roomYPos;
    }

    public int getRoomId() {
        return roomId;
    }

    // em PlayableRoom
    public int getLayerCount() {
        return grid == null ? 0 : grid.length;
    }
}
