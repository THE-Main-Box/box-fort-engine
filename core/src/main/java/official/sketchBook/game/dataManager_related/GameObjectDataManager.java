package official.sketchBook.game.dataManager_related;

import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.dataManager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.projectile_related.*;
import official.sketchBook.engine.util_related.contact_listener.ContactUtils;
import official.sketchBook.engine.util_related.contact_listener.MovableObjectContactListener;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.engine.world_gen.PlayableRoomManager;
import official.sketchBook.game.gameObject_related.Player;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;
import static official.sketchBook.game.util_related.constants.RenderingConstants.TILES_VIEW_HEIGHT;
import static official.sketchBook.game.util_related.constants.RenderingConstants.TILES_VIEW_WIDTH;

public class GameObjectDataManager extends PhysicalGameObjectDataManager {

    private GlobalProjectilePool globalProjectilePool;

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

    private void initPoolFactories(){
        globalProjectilePool.registerFactory(
            Bullet.class,
            type -> new PhysicalProjectilePool<>(type, physicsWorld)
        );
    }

    @Override
    protected void setupSystems() {
        super.setupSystems();

        globalProjectilePool = new GlobalProjectilePool();
        this.initPoolFactories();

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

        Emitter bulletEmitter = new Emitter(globalProjectilePool);
        bulletEmitter.configure(Bullet.class);
        Bullet bullet = (Bullet) bulletEmitter.obtain();

//        bullet.startProjectile(
//            300,
//            90,
//            45
//        );

    }

    @Override
    protected void setupContactListeners() {
        ContactUtils.handleContactListener(
            this.contactListeners,
            false,
            ContactUtils.keys.MOB_LISTENER,
            new MovableObjectContactListener()
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

    @Override
    protected void updateGameObjects(float delta) {
        super.updateGameObjects(delta);
        globalProjectilePool.update(delta);
        globalProjectilePool.updatePoolProjectiles(delta);
    }

    @Override
    protected void postUpdateGameObjects() {
        super.postUpdateGameObjects();
        globalProjectilePool.postUpdateProjectiles();
    }

    private int[][] initBaseTileMap() {
        int[][] toReturn = new int[TILES_VIEW_HEIGHT][TILES_VIEW_WIDTH];

        for (int y = 0; y < TILES_VIEW_HEIGHT; y++) {
            for (int x = 0; x < TILES_VIEW_WIDTH; x++) {
                toReturn[y][x] = 0;

                List<Boolean> canCreate = new ArrayList<>();
                canCreate.add(y >= TILES_VIEW_HEIGHT - 2);  //chão
                canCreate.add(y == 0);                      //teto
                canCreate.add(x == 0);                      //parede esquerda
                canCreate.add(x == TILES_VIEW_WIDTH - 1);   //parede direita

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
        globalProjectilePool.dispose();
    }

    public PlayableRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(PlayableRoom newRoom) {
        if (newRoom == null || newRoom == currentRoom) return;

        //Atualizamos as referencias
        PlayableRoom oldRoom = this.currentRoom;
        this.currentRoom = newRoom;

        //passamos todos os objetos ainda ativos que são de sala para uma validação,
        // assim decidindo e agindo,
        // se eles vão para a próxima sala ou se serão marcados para serem destruídos
        roomManager.transitionRoomObjects(
            gameObjectList,
            oldRoom,
            newRoom
        );

        /*
        * Como os objetos em si, que eram da sala que deveriam ser disposed,
        * foram lidados préviamente com a função de usada para a transição,
        * aqui iremos apenas realizar uma limpeza final de dados que são gerenciados únicamente pela sala
        */

        //Realizamos um dispose dos dados da antiga sala
        roomManager.cleanUpRoom(oldRoom);

    }

    /// Define a câmera do jogo (chamado por PlayScreen após criar o manager)
    public void setGameCamera(OrthographicCameraManager camera) {
        this.gameCamera = camera;
    }
}
