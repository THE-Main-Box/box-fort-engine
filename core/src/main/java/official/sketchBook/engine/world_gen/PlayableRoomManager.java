package official.sketchBook.engine.world_gen;

import official.sketchBook.game.util_related.body.world_gen.RoomBodyDataFactory;

import static official.sketchBook.game.util_related.constants.WorldC.TILE_SIZE_PX;

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

        //TODO: ADICIONAR UMA CRIAÇÃO MAIS COMPLEXA PARA O MODELO
        TileModel modelToAdd = new TileModel(
            id,
            bodyId
        );

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
        //Prepara a grid contendo os ids dos corpos
        prepareBodyIdGrid(room);

        //Criamos as bodies das tiles da sala e armazenamos como bodies nativas da sala
        room.nativeBodies = RoomBodyDataFactory.createRoomBodies(
            room.bodyIdGrid,
            room.getPhysicsWorld()
        );

    }

    /// Percorre a grid e insere dentro da grid de body a id correspondente
    private void prepareBodyIdGrid(
        PlayableRoom room
    ) {
        //Inicializa a grid das body
        room.bodyIdGrid = new int[room.gridHeight][room.gridWidth];

        for (int h = 0; h < room.grid.length; h++) {
            for (int w = 0; w < room.grid[0].length; w++) {

                //Obtemos a tile da coordenada passada
                TileModel currentTile = room.tileModelIdMap.get(
                    room.grid[h][w]
                );

                //Se houver uma tile e sua id de body tiver sido passada
                if (currentTile != null && currentTile.getBodyId() != null) {
                    room.bodyIdGrid[h][w] = currentTile.getBodyId();
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
