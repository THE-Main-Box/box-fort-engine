package official.sketchBook.game.dataManager_related;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.base_game_object.BaseRoomGameObject;
import official.sketchBook.engine.game_object_related.vehicle.Submarine;
import official.sketchBook.engine.game_object_related.vehicle.SubmarineNode;
import official.sketchBook.engine.game_object_related.vehicle.SubmarinePart;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.model.RoomLiquid;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;
import official.sketchBook.engine.util_related.contact_listener.ContactUtils;
import official.sketchBook.engine.util_related.contact_listener.listeners.MovableObjectContactListener;
import official.sketchBook.engine.util_related.contact_listener.listeners.ProjectileContactListener;
import official.sketchBook.engine.util_related.contact_listener.listeners.VehicleContactListener;
import official.sketchBook.engine.util_related.pools.GlobalProjectilePool;
import official.sketchBook.engine.world_gen.PlayableRoomManager;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.gameObject_related.Player;
import official.sketchBook.game.projectile_related.factories.ProjectilePoolFactory;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.VEHICLE;
import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.VEHICLE_PASSENGER;
import static official.sketchBook.game.util_related.constants.RenderingConstants.TILES_VIEW_HEIGHT;
import static official.sketchBook.game.util_related.constants.RenderingConstants.TILES_VIEW_WIDTH;

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
        this.initPoolFactories();
    }

    private void initPoolFactories() {
        ProjectilePoolFactory.initPoolFactories(renderTree, physicsWorld);
        ProjectilePoolFactory.applyFactories(globalProjectilePool);
    }

    @Override
    protected void setupSystems() {
        super.setupSystems();

        initPools();

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

        List<LiquidRegion> regionList = new ArrayList<>();
        LiquidData data;

        regionList.add(
            new LiquidRegion(
                200,
                10,
                150,
                50
            )
        );

        data = new LiquidData(
            "water",
            1,
            1f,
            20f,
            999f,
            999f,
            9999f
        );

        RoomLiquid water = new RoomLiquid(
            this,
            currentRoom,
            data,
            regionList
        );

        float
            subX = 150,
            subY = 60;

        List<SubmarinePart> subParts = getBaseSubmarineParts();

        List<SubmarineNode> nodeList = new ArrayList<>();

        nodeList.add(
            new SubmarineNode(
                physicsWorld,
                subParts,
                subX,
                subY,
                0,
                0,
                false,
                false
            )
        );

        Submarine baseSubmarine = new Submarine(
            this,
            currentRoom,
            nodeList
        );

    }

    private static List<SubmarinePart> getBaseSubmarineParts() {
        List<SubmarinePart> subParts = new ArrayList<>();

        int
            categoryBit = VEHICLE.bit(),
            maskBit = VEHICLE_PASSENGER.bit();

        SubmarinePart corridor = new SubmarinePart(1, "corridor_test");

        corridor.addInternalFixture(
            0,
            0,
            0,
            25,
            0,
            120,
            10,
            categoryBit,
            maskBit,
            true,
            false
        );

        corridor.addInternalFixture(
            0,
            0,
            0,
            -25,
            0,
            120,
            10,
            categoryBit,
            maskBit,
            true,
            false
        );

        subParts.add(corridor);
        return subParts;
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
            ContactUtils.keys.PROJECTILE_LISTENER,
            new ProjectileContactListener()
        );

        ContactUtils.handleContactListener(
            this.contactListeners,
            false,
            ContactUtils.keys.VEHICLE_LISTENER,
            new VehicleContactListener()
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
            (gameCamera.getCamera().position.x),
            (gameCamera.getCamera().position.y),
            (gameCamera.getCamera().viewportWidth * gameCamera.getCamera().zoom),
            (gameCamera.getCamera().viewportHeight * gameCamera.getCamera().zoom)
        );
    }

//    @Override
//    protected void drawRenderableObjects(SpriteBatch batch) {
//        renderTree.forEachObject(
//            obj -> obj.render(batch),
//            (gameCamera.getCamera().position.x),
//            (gameCamera.getCamera().position.y),
//            (gameCamera.getCamera().viewportWidth * gameCamera.getCamera().zoom),
//            (gameCamera.getCamera().viewportHeight * gameCamera.getCamera().zoom)
//        );
//    }

    /**
     * Atualiza cache de bounds da câmera.
     */
    private void updateCameraBoundsCache() {
        cachedCamX = gameCamera.getCamera().position.x;
        cachedCamY = gameCamera.getCamera().position.y;
        cachedCamWidth = gameCamera.getCamera().viewportWidth * gameCamera.getCamera().zoom;
        cachedCamHeight = gameCamera.getCamera().viewportHeight * gameCamera.getCamera().zoom;
    }

    public void updateVisuals(float delta) {
        renderTree.forEachObject(
            obj -> obj.updateVisuals(delta),
            cachedCamX,
            cachedCamY,
            cachedCamWidth,
            cachedCamHeight
        );
    }

    public void render(SpriteBatch batch) {
        renderTree.forEachObject(
            obj -> obj.render(batch),
            cachedCamX,
            cachedCamY,
            cachedCamWidth,
            cachedCamHeight
        );
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

    public GlobalProjectilePool getGlobalProjectilePool() {
        return globalProjectilePool;
    }
}
