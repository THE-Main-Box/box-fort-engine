package official.sketchBook.engine.world_gen;

import official.sketchBook.engine.gameObject_related.BaseGameObject;
import official.sketchBook.engine.gameObject_related.BaseRoomGameObject;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.engine.world_gen.model.TileModel;
import official.sketchBook.game.util_related.body.world_gen.RoomBodyFactory;

import java.util.List;

import static official.sketchBook.game.util_related.constants.WorldConstants.TILE_SIZE_PX;

public class PlayableRoomManager {

    /**
     * Adiciona dentro da biblioteca da sala um novo tipo de tile
     * cujo o qual será usado de referencia para sistemas posteriores
     *
     * @param room   sala que iremos realizar a adição
     * @param id     id de referência da tile
     * @param bodyId id de referência ao tipo de corpo que a tile possui
     */
    public void addNewTileModel(
        PlayableRoom room,
        int id,
        int bodyId
    ) {
        //se já temos o id inserido não podemos sobrescrever
        if (room.tileModelIdMap.containsKey(id)) {
            throw new IllegalArgumentException("já possuimos uma tile marcada com esse id");
        }

        //Iniciamos um novo modelo de tile contendo os dados a serem referênciados internamente pela grid
        TileModel modelToAdd = new TileModel(
            id,
            bodyId
        );

        //Adicionamos no map
        room.tileModelIdMap.put(
            id,
            modelToAdd
        );

    }

    /**
     * Inicializa a grid de uma sala jogável
     *
     * @param room         referencia à sala
     * @param roomTileGrid grid contendo os ids das tiles registradas na sala previamente
     */
    public void initRoomGrid(
        PlayableRoom room,
        int[][] roomTileGrid
    ) {
        room.grid = roomTileGrid;                               //Atualiza a grid e seus valores
        room.gridHeight = roomTileGrid.length;                  //Seta a altura da grid
        room.gridWidth = roomTileGrid[0].length;                //Seta a largura da grid

        //Determina a largura da sala em pixels com base na constante de dimensões do sistema de grid
        room.roomWidthPx = room.gridWidth * TILE_SIZE_PX;

        //Determina a altura da sala em pixels com base na constante de dimensões do sistema de grid
        room.roomHeightPx = room.gridHeight * TILE_SIZE_PX;

        //Se não temos um mundo físico não podemos prosseguir nesse ponto
        if (!room.isPhysicsWorldAccessible()) return;

        createTileBodies(room);
    }

    /// Cria as bodies das tiles
    public void createTileBodies(PlayableRoom room) {
        //Criamos as bodies das tiles da sala e armazenamos como bodies nativas da sala
        room.nativeBodies = RoomBodyFactory.createRoomBodies(
            prepareBodyIdGrid(room),
            room.getPhysicsWorld()
        );
    }

    /// Percorre a grid e insere dentro da grid de body a id correspondente
    private int[][] prepareBodyIdGrid(
        PlayableRoom room
    ) {
        //Inicializa a grid das body
        int[][] bodyIdGrid = new int[room.gridHeight][room.gridWidth];

        for (int h = 0; h < room.grid.length; h++) {
            for (int w = 0; w < room.grid[0].length; w++) {

                //Obtemos a tile da coordenada passada
                TileModel currentTile = room.tileModelIdMap.get(
                    room.grid[h][w]
                );

                //Se houver uma tile e sua id de body tiver sido passada
                if (currentTile != null && currentTile.getBodyId() != null) {
                    bodyIdGrid[h][w] = currentTile.getBodyId();
                }

            }
        }

        return bodyIdGrid;
    }

    /**
     * Realiza a transição lógica dos objetos de uma sala para outra.
     * Filtra quem deve ser destruído e quem deve ser notificado da mudança.
     */
    public void transitionRoomObjects(
        List<BaseGameObject> currentActiveObjects,
        PlayableRoom oldRoom,
        PlayableRoom nextRoom
    ) {
        for (int i = currentActiveObjects.size() - 1; i >= 0; i--) {
            BaseGameObject obj = currentActiveObjects.get(i);

            if (obj instanceof BaseRoomGameObject) {
                handleObjectScope(
                    (BaseRoomGameObject) obj,
                    oldRoom,
                    nextRoom
                );
            }
        }
    }

    /**
     * Define o destino do objeto com base no seu escopo
     */
    private void handleObjectScope(BaseRoomGameObject obj, PlayableRoom oldRoom, PlayableRoom nextRoom) {
        if (obj.roomScope == RoomObjectScope.LOCAL) {
            // Se é local, ele deve sair da pipeline do manager de objetos
            obj.markToDestroy();
        } else if (obj.roomScope == RoomObjectScope.GLOBAL) {
            // Se é global, notificamos a troca e atualizamos a referência
            obj.onRoomSwitch(oldRoom, nextRoom);
        }
    }

    /**
     * Limpa completamente os dados de uma sala que está saindo de cena
     */
    public void cleanUpRoom(PlayableRoom roomToClean) {
        if (roomToClean != null) {
            roomToClean.dispose();
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

        for (int[] ints : grid) {
            // Início da linha: um colchete para organizar visualmente
            System.out.print("[");

            for (int anInt : ints) {
                // Imprime o valor seguido de um espaço ou tabulação para manter alinhado
                // Use \t (tab) se os IDs tiverem muitos dígitos diferentes
                System.out.print(anInt + ",");
            }

            // Fim da linha: fecha o colchete e pula para a próxima linha do console
            System.out.println("]");
        }

        System.out.println("--- Fim do Debug da Matrix ---");
    }

}
