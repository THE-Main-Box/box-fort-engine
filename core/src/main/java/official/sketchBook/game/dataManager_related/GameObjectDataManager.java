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
import official.sketchBook.engine.world_gen.RoomRetentionPool;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.gameObject_related.player.Player;
import official.sketchBook.game.gameObject_related.player.PlayerSaveData;
import official.sketchBook.game.projectile_related.factories.ProjectilePoolFactory;
import official.sketchBook.game.serialization.SaveFileLoader;

import static official.sketchBook.game.util_related.constants.WorldConstants.DEFAULT_ROOM_CLEANUP_TIME;

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
    private RoomRetentionPool roomRetentionPool;

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
        roomRetentionPool = new RoomRetentionPool(DEFAULT_ROOM_CLEANUP_TIME);
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

        roomRetentionPool.update(delta);

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

        roomRetentionPool.disposeAllRetained();
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

        //Se a sala nova estava retida (jogador voltou antes do timer expirar),
        //resgatamos a mesma inst?ncia em vez de tratar como sala nova
        PlayableRoom reclaimedRoom = roomRetentionPool.reclaim(newRoom.getRoomId());
        if (reclaimedRoom != null) {
            newRoom = reclaimedRoom;
        }

        this.currentRoom = newRoom;

        //passamos todos os objetos ainda ativos que s�o de sala para uma valida��o,
        // assim decidindo e agindo,
        // se eles v�o para a pr�xima sala ou se ser�o marcados para serem destru�dos
        roomManager.transitionRoomObjects(
            updatableObjectList,
            oldRoom,
            newRoom
        );

        /*
         * A sala antiga N?O sofre dispose nem cleanUpRoom imediato ? ela entra
         * em reten??o com um timer. Se o jogador voltar antes do timer expirar,
         * ela ? resgatada intacta acima. Se expirar sem retorno, o
         * RoomRetentionPool chama dispose() de verdade nela sozinho.
         */
        if (oldRoom != null) {
            roomRetentionPool.retain(oldRoom);
        }

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

    public RoomRetentionPool getRoomRetentionPool() {
        return roomRetentionPool;
    }

    public GlobalProjectilePool getGlobalProjectilePool() {
        return globalProjectilePool;
    }
}
