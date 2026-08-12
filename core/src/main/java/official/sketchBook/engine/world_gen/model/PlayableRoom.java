package official.sketchBook.engine.world_gen.model;

import official.sketchBook.engine.game_object_related.base_game_object.BaseRoomGameObject;

import java.util.*;

public class PlayableRoom {

    /// Id da sala
    protected final int roomId;

    /// Refer�ncia a posi��o da grid de world
    protected float
        roomXPos,
        roomYPos;

    /// Dimens�es da grid
    public int
        gridWidth,
        gridHeight;

    /// Dimens�es da sala vindas da grid
    public int
        roomWidthPx,
        roomHeightPx;

    /// Grade em camadas
    protected int[][][] grid;

    /// Lista de refer�ncia a objetos presentes na sala, s�o apenas refer�ncias,
    /// e n�o s�o gerenciados aqui dentro
    public List<BaseRoomGameObject> roomGameObjectList;

    public boolean disposed = false;

    public PlayableRoom(
        int id,
        float roomX,
        float roomY
    ) {
        this.roomId = id;                                   //Id da sala
        this.roomXPos = roomX;                              //Posi��o que iremos come�ar a gerar a sala no eixo x
        this.roomYPos = roomY;                              //Posi��o que iremos come�ar a gerar a sala no eixo y

        this.roomGameObjectList = new ArrayList<>();            //Iniciamos a lista
    }

    public void clearObjectList(){
        roomGameObjectList.clear();
    }

    /**
     * Inicializa a grid de uma sala jog�vel
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
        A primeira lista da grid s�o as camadas, � uma lista em 3 dimens�es,
         portanto isso me permite muita coisa futuramente,
         mas agora estaremos lidando com 2d apenas...

        Como a grid � uma lista de lista,
         a length � a altura, pois se refere � primeira lista,
          ou seja � quantidade de linhas.

          J� [0].length se refere � quantidade de colunas,
           pois estamos acessando a primeira linha
          */

        //Determina a altura da grid obtendo a altura da primeira camada
        this.gridHeight = roomTileGrid[0]
            .length;

        //Determina a largura da grid obtendo a largura da primeira camada
        this.gridWidth = roomTileGrid[0][0].length;

        //Determina a largura da sala em pixels com base na constante de dimens�es do sistema de grid
        this.roomWidthPx = this.gridWidth * pixelsPerTile;

        //Determina a altura da sala em pixels com base na constante de dimens�es do sistema de grid
        this.roomHeightPx = this.gridHeight * pixelsPerTile;
    }

    public void addNewRoomGameObject(BaseRoomGameObject roomObject) {
        this.roomGameObjectList.add(roomObject);
    }

    public void removeRoomObject(BaseRoomGameObject roomObjectToRemove) {
        this.roomGameObjectList.remove(roomObjectToRemove);
    }

    /**
     * Altera o id de uma tile em uma posi??o espec?fica da grid.
     * ?nico ponto de escrita individual da grid ? qualquer sistema que
     * precise modificar uma tile em runtime (escava??o, portas, etc)
     * passa por aqui, nunca acessando o array diretamente.
     */
    public void setGridDataAt(int z, int y, int x, int tileId) {
        grid[z][y][x] = tileId;
    }

    public int getGridDataAt(int z, int y, int x) {
        return grid[z][y][x];
    }

    /**
     * Imprime no console o conte?do de uma camada espec?fica da grid, formatado
     * como matriz. ?til pra debug visual r?pido sem precisar acessar o array
     * diretamente de fora.
     *
     * @param z ?ndice da camada a ser impressa
     * @throws IndexOutOfBoundsException se z estiver fora do intervalo de camadas existentes
     */
    public void printMatrixContentAtLayer(int z) {
        int layerCount = getLayerCount();

        if (layerCount == 0) {
            System.out.println("A grid ainda n?o foi inicializada.");
            return;
        }

        if (z < 0 || z >= layerCount) {
            throw new IndexOutOfBoundsException(
                "camada " + z + " fora do intervalo (0 a " + (layerCount - 1) + ")"
            );
        }

        System.out.println("--- In?cio do Debug da Matrix [camada " + z + "] (" + gridHeight + "x" + gridWidth + ") ---");

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
     * Imprime todas as camadas da grid, uma ap?s a outra.
     */
    public void printMatrixContentOfAllLayers() {
        int layerCount = getLayerCount();

        for (int z = 0; z < layerCount; z++) {
            printMatrixContentAtLayer(z);
        }
    }

    public void cleanUpRoom() {
        clearObjectList();
    }

    public void dispose() {
        if (disposed) return;

        executeDispose();

        disposed = true;
    }

    protected void executeDispose() {
        disposeLists();

        roomGameObjectList = null;
    }

    /// Limpa todas as listas
    protected void disposeLists() {
        clearObjectList();
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

    public int getLayerCount() {
        return grid == null ? 0 : grid.length;
    }
}
