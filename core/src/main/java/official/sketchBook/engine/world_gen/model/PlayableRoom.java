package official.sketchBook.engine.world_gen.model;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.game_object_related.base_game_object.BaseRoomGameObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    ///Grade em camadas
    protected int[][][] grid;

    /// Lista de referência a todas as tiles que estamos usando
    public Map<Integer, TileModel> tileModelIdMap;

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


        this.tileModelIdMap = new HashMap<>();               //Inicializamos o hashMap
        roomGameObjectList = new ArrayList<>();
    }

    /**
     * Adiciona dentro da biblioteca da sala um novo tipo de tile
     * cujo o qual será usado de referencia para sistemas posteriores
     *
     * @param id   id de referência da tile
     */
    public void addNewTileModel(
        int id
    ) {
        //se já temos o id inserido não podemos sobrescrever
        if (this.tileModelIdMap.containsKey(id)) {
            throw new IllegalArgumentException("já possuimos uma tile marcada com esse id");
        }

        //Iniciamos uma nova tile com um id que será referenciado internamente
        TileModel modelToAdd = new TileModel(id);

        //Adicionamos no mapa interno pra ser referenciado internamente, usando o id passado na geração como chave
        this.tileModelIdMap.put(
            id,
            modelToAdd
        );

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
     *         via addNewTileModel (evita tile "fantasma" sem TileModel associado)
     */
    public void setGridDataAt(int z, int y, int x, int tileId) {
        if (tileId != 0 && !tileModelIdMap.containsKey(tileId)) {
            throw new IllegalArgumentException(
                "tentativa de setar tile id " + tileId + " sem TileModel registrado"
            );
        }

        grid[z][y][x] = tileId;
    }

    public int getGridDataAt(int z, int y, int x) {
        return grid[z][y][x];
    }

    public void dispose() {
        if (disposed) return;

        executeDispose();

        disposed = true;
    }

    protected void executeDispose(){
        disposeLists();

        tileModelIdMap = null;
        roomGameObjectList = null;
    }

    /// Limpa todas as listas
    protected void disposeLists() {
        tileModelIdMap.clear();
        roomGameObjectList.clear();
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
}
