package official.sketchBook.game.dataManager_related;

import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.util_related.contact_listener.ContactUtils;
import official.sketchBook.engine.util_related.contact_listener.listeners.*;
import official.sketchBook.engine.util_related.pools.GlobalProjectilePool;
import official.sketchBook.engine.util_related.pools.RayCastPool;
import official.sketchBook.engine.util_related.serialization.SaveDataInstanceRegistry;
import official.sketchBook.engine.world_gen.PlayableRoomManager;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.gameObject_related.player.Player;
import official.sketchBook.game.gameObject_related.player.PlayerSaveData;
import official.sketchBook.game.projectile_related.factories.ProjectilePoolFactory;
import official.sketchBook.game.serialization.SaveFileLoader;

public class GameObjectDataManager extends PhysicalGameObjectDataManager {

    /// Buffers para camera
    private float
        cachedCamX,
        cachedCamY,
        cachedCamWidth,
        cachedCamHeight;

    private GlobalProjectilePool globalProjectilePool;

    /// Gerenciador de salas do mundo
    private PlayableRoom currentRoom;
    private PlayableRoomManager roomManager;

    private SaveFileLoader SFLoader;

    /// Câmera do jogo (referência, não é owned)
    private OrthographicCameraManager gameCamera;

    /// Referência ao jogador principal (pode expandir para múltiplos)
    public Player mainPlayer;

    public GameObjectDataManager(
        World physicsWorld,
        int velIterations,
        int posIterations
    ) {
        super(physicsWorld, velIterations, posIterations);
    }

    private void initPools() {
        globalProjectilePool = new GlobalProjectilePool();
        RayCastPool.getInstance(physicsWorld);
        this.initPoolFactories();
    }

    private void initPoolFactories() {
        ProjectilePoolFactory.initPoolFactories(renderTree, physicsWorld);
        ProjectilePoolFactory.applyFactories(globalProjectilePool);
    }

    @Override
    protected void setupSystems() {
        super.setupSystems();

        initSDIRegistration();

        initPools();

        //Inicializa o manager de salas
        roomManager = new PlayableRoomManager();
        SFLoader = new SaveFileLoader(this);

        SFLoader.loadSaveFile();

    }

    private void initSDIRegistration(){
        SaveDataInstanceRegistry.GLOBAL.register(PlayerSaveData.TYPE_KEY, PlayerSaveData::new);
    }

    @Override
    protected void setupContactListeners() {
        ContactUtils.handleContactListener(
            this.contactListeners,
            false,
            ContactUtils.keys.MOB_LISTENER,
            new MovableObjectContactListener()
        );

        ContactUtils.handleContactListener(
            this.contactListeners,
            false,
            ContactUtils.keys.VEHICLE_LISTENER,
            new VehicleContactListener()
        );

        ContactUtils.handleContactListener(
            this.contactListeners,
            false,
            ContactUtils.keys.SELF_LISTENER,
            new SelfListenedPhysicalObjectContactListener()
        );

        ContactUtils.handleContactListener(
            this.contactListeners,
            false,
            ContactUtils.keys.PROJECTILE_LISTENER,
            new ProjectileContactListener()
        );

        ContactUtils.handleContactListener(
            this.contactListeners,
            false,
            ContactUtils.keys.LIQUID_LISTENER,
            new LiquidContactListener()
        );
    }

    /// Atualiza o tracking da câmera baseado no jogador
    /// Chamado durante o update do mundo
    protected void updateCameraTracking() {
        //Se não temos câmera ou jogador, não fazemos nada
        if (gameCamera == null || mainPlayer == null || mainPlayer.getTransformC() == null) return;

        //Rastreia a câmera para a posição do jogador
        gameCamera.trackObjectByOffset(
            mainPlayer.getTransformC().getCenterX(),
            mainPlayer.getTransformC().getCenterY()
        );
    }

    /// Override do update para adicionar lógica de câmera
    @Override
    public void update(float delta) {
        //Atualiza o buffer antes de entrar na pipeline de update
        updateCameraBoundsCache();

        //Chama o update padrão
        super.update(delta);

        //Depois de tudo atualizado, move a camera
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


    @Override
    protected void updateRenderableObjectVisuals(float delta) {

        renderTree.forEachObject(
            obj -> obj.updateVisuals(delta),
            cachedCamX,
            cachedCamY,
            cachedCamWidth,
            cachedCamHeight
        );

    }

    /**
     * Atualiza cache de bounds da câmera.
     */
    private void updateCameraBoundsCache() {
        cachedCamX = gameCamera.getCamera().position.x;
        cachedCamY = gameCamera.getCamera().position.y;
        cachedCamWidth = gameCamera.getCamera().viewportWidth * gameCamera.getCamera().zoom;
        cachedCamHeight = gameCamera.getCamera().viewportHeight * gameCamera.getCamera().zoom;
    }



    @Override
    protected void onManagerDestruction() {
        System.out.println("destruindo manager");
    }

    @Override
    protected void disposeGeneralData() {
        super.disposeGeneralData();
        currentRoom.dispose();

        if (globalProjectilePool != null) globalProjectilePool.dispose();

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
            updatableObjectList,
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

        if (gameCamera != null) {
            gameCamera.updateRoomLimits(
                currentRoom.roomWidthPx,
                currentRoom.roomHeightPx
            );
        }

    }

    public PlayableRoomManager getRoomManager() {
        return roomManager;
    }

    /// Define a câmera do jogo (chamado por PlayScreen após criar o manager)
    public void setGameCamera(OrthographicCameraManager camera) {
        this.gameCamera = camera;
    }

    public GlobalProjectilePool getGlobalProjectilePool() {
        return globalProjectilePool;
    }
}
