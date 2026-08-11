package official.sketchBook.engine.world_gen.model;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.game.util_related.body.world_gen.RoomBodyFactory;

import java.util.Arrays;
import java.util.List;

public class PhysicalPlayableRoom extends PlayableRoom {

    protected final World physicsWorld;
    public List<Body> nativeBodies;
    protected final boolean physicsWorldAccessible;

    /**
     * Estilo de geração física por camada, índice = índice da camada na grid.
     * -1 = não gerar/ignorar essa camada, 0 = geração padrão (grid comum),
     * 1+ = estilos especiais (resolvidos por um registry externo, ainda a definir).
     * Alocado automaticamente em initRoomGrid, com o tamanho certo já garantido ?
     * preenchido depois, camada por camada, via setLayerGenerationStyle.
     */
    protected int[] layerGenerationStyles;

    public PhysicalPlayableRoom(int id, float roomX, float roomY, World physicsWorld) {
        super(id, roomX, roomY);
        this.physicsWorld = physicsWorld;
        this.physicsWorldAccessible = physicsWorld != null; //Validamos se podemos usar o world
    }

    @Override
    public void initRoomGrid(int[][][] roomTileGrid, int pixelsPerTile) {
        super.initRoomGrid(roomTileGrid, pixelsPerTile);

        // a grid acabou de ser definida (ou redefinida) ? realocamos o array
        // de estilos do tamanho certo, com -1 (n�o gerar) como padr�o at�
        // que algu�m de fora preencha camada por camada
        this.layerGenerationStyles = new int[getLayerCount()];
        Arrays.fill(this.layerGenerationStyles, -1);
    }

    /**
     * Define o estilo de geração de uma camada específica.
     * S� pode ser chamado depois de initRoomGrid (array j� alocado).
     *
     * @throws IllegalStateException se a grid ainda n�o foi inicializada
     */
    public void setLayerGenerationStyle(int layerIndex, int style) {
        if (layerGenerationStyles == null) {
            throw new IllegalStateException("grid ainda não foi inicializada (initRoomGrid)");
        }

        layerGenerationStyles[layerIndex] = style;
    }

    /**
     * @return o estilo de geração da camada informada (-1 se ainda não definido).
     * @throws IllegalStateException se a grid ainda não foi inicializada
     */
    public int getLayerGenerationStyle(int layerIndex) {
        if (layerGenerationStyles == null) {
            throw new IllegalStateException("grid ainda não foi inicializada (initRoomGrid)");
        }

        return layerGenerationStyles[layerIndex];
    }

//    /// Cria as bodies das tiles
//    public void createTileBodies(PlayableRoom room) {
//        //Criamos as bodies das tiles da sala e armazenamos como bodies nativas da sala
//        room.nativeBodies = RoomBodyFactory.createRoomBodies(
//            prepareBodyIdGrid(room),
//            room.getPhysicsWorld()
//        );
//    }
//
//    /// Percorre a grid e insere dentro da grid de body a id correspondente
//    private int[][] prepareBodyIdGrid(
//        PlayableRoom room
//    ) {
//        //Inicializa a grid das body
//        int[][] bodyIdGrid = new int[room.gridHeight][room.gridWidth];
//
//        for (int h = 0; h < room.grid.length; h++) {
//            for (int w = 0; w < room.grid[0].length; w++) {
//
//                //Obtemos a tile da coordenada passada
//                TileModel currentTile = room.tileModelIdMap.get(
//                    room.grid[h][w]
//                );
//
//                //Se houver uma tile e sua id de body tiver sido passada
//                if (currentTile != null && currentTile.getBodyId() != null) {
//                    bodyIdGrid[h][w] = currentTile.getBodyId();
//                }
//
//            }
//        }
//
//        return bodyIdGrid;
//    }

    @Override
    protected void executeDispose() {
        disposeNativeBodies();
        layerGenerationStyles = null;
        super.executeDispose();
    }

    private void disposeNativeBodies() {
        if (!physicsWorldAccessible || nativeBodies == null) return;
        for (int i = nativeBodies.size() - 1; i >= 0; i--) {
            physicsWorld.destroyBody(nativeBodies.get(i));
        }
        nativeBodies.clear();
        nativeBodies = null;
    }

    public boolean isPhysicsWorldAccessible() {
        return physicsWorldAccessible;
    }

    public World getPhysicsWorld() {
        return physicsWorld;
    }

    // em PlayableRoom
    public int getLayerCount() {
        return grid == null ? 0 : grid.length;
    }
}
