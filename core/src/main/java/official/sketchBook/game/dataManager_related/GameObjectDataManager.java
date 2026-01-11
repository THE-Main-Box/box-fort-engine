package official.sketchBook.game.dataManager_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.dataManager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.world_gen.PlayableRoom;
import official.sketchBook.engine.world_gen.PlayableRoomManager;
import official.sketchBook.game.gameObject_related.Player;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.game.util_related.constants.RenderingC.TILES_VIEW_HEIGHT;
import static official.sketchBook.game.util_related.constants.RenderingC.TILES_VIEW_WIDTH;

public class GameObjectDataManager extends PhysicalGameObjectDataManager {

    /// Gerenciador de salas do mundo
    private PlayableRoom currentRoom;
    private PlayableRoomManager roomManager;

    /// Câmera do jogo (referência, não é owned)
    private OrthographicCameraManager gameCamera;

    /// Referência ao jogador principal (pode expandir para múltiplos)
    public Player mainPlayer;

    public GameObjectDataManager(
        World physicsWorld,
        float timeStep,
        int velIterations,
        int posIterations
    ) {
        super(physicsWorld, timeStep, velIterations, posIterations);
    }

    @Override
    protected void setupSystems() {
        //Inicializa o manager de salas
        roomManager = new PlayableRoomManager();

        //Cria a sala inicial
        currentRoom = new PlayableRoom(
            1,
            0,
            0,
            physicsWorld
        );

        //Adiciona os modelos de tile
        roomManager.addNewTileModel(
            currentRoom,
            1,
            1
        );

        //Inicializa a grid da sala
        roomManager.initRoomGrid(
            currentRoom,
            initBaseTileMap()
        );
    }

    /// Atualiza o tracking da câmera baseado no jogador
    /// Chamado durante o update do mundo
    protected void updateCameraTracking() {
        //Se não temos câmera ou jogador, não fazemos nada
        if (gameCamera == null || mainPlayer == null) return;

        //Rastreia a câmera para a posição do jogador
        gameCamera.trackObjectByOffset(
            mainPlayer.getTransformC().getCenterX(),
            mainPlayer.getTransformC().getCenterY()
        );
    }

    /// Override do update para adicionar lógica de câmera
    @Override
    public void update(float delta) {
        //Chama o update padrão
        super.update(delta);

        //Depois de tudo atualizado, move a câmera
        updateCameraTracking();
    }

    private int[][] initBaseTileMap() {
        int[][] toReturn = new int[TILES_VIEW_HEIGHT][TILES_VIEW_WIDTH];

        for (int y = 0; y < TILES_VIEW_HEIGHT; y++) {
            for (int x = 0; x < TILES_VIEW_WIDTH; x++) {
                toReturn[y][x] = 0;

                List<Boolean> canCreate = new ArrayList<>();
                canCreate.add(y >= TILES_VIEW_HEIGHT - 1);

                for (boolean value : canCreate) {
                    if (value) {
                        toReturn[y][x] = 1;
                        break;
                    }
                }
            }
        }

        return toReturn;
    }

    @Override
    protected void onManagerDestruction() {
        System.out.println("destruindo manager");
    }

    @Override
    protected void disposeGeneralData() {
        super.disposeGeneralData();
        currentRoom.dispose();
    }

    public PlayableRoom getCurrentRoom() {
        return currentRoom;
    }

    /// Define a câmera do jogo (chamado por PlayScreen após criar o manager)
    public void setGameCamera(OrthographicCameraManager camera) {
        this.gameCamera = camera;
    }
}
