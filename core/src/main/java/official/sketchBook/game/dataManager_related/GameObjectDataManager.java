package official.sketchBook.game.dataManager_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.components_related.system_utils.ControllerGroup;
import official.sketchBook.game.components_related.vehicle.VehicleControllerComponent;
import official.sketchBook.game.components_related.vehicle.VehicleDoor;
import official.sketchBook.game.components_related.vehicle.VehicleEngineComponent;
import official.sketchBook.game.components_related.vehicle.WirableVehicleDoor;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.vehicle_related.Submarine;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarineNode;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarinePart;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.model.RoomLiquid;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;
import official.sketchBook.engine.util_related.contact_listener.ContactUtils;
import official.sketchBook.engine.util_related.contact_listener.listeners.*;
import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.util_related.pools.GlobalProjectilePool;
import official.sketchBook.engine.util_related.pools.RayCastPool;
import official.sketchBook.engine.world_gen.PlayableRoomManager;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.gameObject_related.Player;
import official.sketchBook.game.projectile_related.factories.ProjectilePoolFactory;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.*;
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
                150
            )
        );

        data = new LiquidData(
            "water",
            1,
            9.5f,
            5f
        );

        RoomLiquid water = new RoomLiquid(
            this,
            currentRoom,
            data,
            regionList
        );

        float
            subX = 500,
            subY = 60;

        List<SubmarinePart> subParts = getBaseSubmarineParts();

        List<SubmarineNode> nodeList = new ArrayList<>();

        SubmarineNode node_1 = new SubmarineNode(
            physicsWorld,
            subParts,
            subX,
            subY,
            0,
            0,
            false,
            false
        );


        nodeList.add(
            node_1
        );

        Submarine baseSubmarine = new Submarine(
            this,
            currentRoom,
            nodeList
        );

        /*
         * To-do: para as portas, um sistema de criação será interessante, a base de orientações
         *  Em sumo a idéia é simples, temos as dimensões, altura e largura,
         *  com base nisso usaremos as dimensões,
         *  para podermos determinar o offset vertical e horizontal com base na posição já determinada, caso necessário,
         *   mas iremos usar claramente para a orientação da fixture sensor,
         *  caso haja, e não só caso haja,
         *  mas irá nos ajudar em muitas outras circunstancias,
         *  como determinar o quanto devemos puxar, empurrar, ou levantar ou abaixar,
         *   a fixture sensor
         * */


        VehicleDoor door = new VehicleDoor(
            node_1,
            new FixtureData(
                0,
                0,
                55,
                0,
                0,
                9,
                40,
                VEHICLE.bit(),
                VEHICLE_PASSENGER.bit(),
                false,
                false
            ),
            new FixtureData(
                0,
                0,
                55 - 9,
                0,
                0,
                9 * 4,
                40,
                INTERACTABLE.bit(),
                INTERACTABLE_TRIGGERER.bit(),
                false,
                true
            ),
            false,
            false,
            false
        );

        VehicleDoor door2 = new VehicleDoor(
            node_1,
            new FixtureData(
                0,
                0,
                0,
                0,
                0,
                -55,
                0,
                0,
                9,
                40,
                VEHICLE.bit(),
                VEHICLE_PASSENGER.bit(),
                false,
                false
            ),
            new FixtureData(
                0,
                0,
                -55 + 9,
                0,
                0,
                9 * 4,
                40,
                INTERACTABLE.bit(),
                INTERACTABLE_TRIGGERER.bit(),
                false,
                true
            ),
            false,
            false,
            true
        );

        VehicleControllerComponent controller = new VehicleControllerComponent(
            node_1,
            new FixtureData(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                VEHICLE.bit(),
                VEHICLE_PASSENGER.bit(),
                false,
                false
            ),
            new FixtureData(
                0,
                0,
                0,
                0,
                0,
                12 * 4,
                30,
                INTERACTABLE.bit(),
                INTERACTABLE_TRIGGERER.bit(),
                false,
                true
            )
        );


        VehicleEngineComponent engine = new VehicleEngineComponent(
            node_1,
            node_1.getBody(),
            1f,
            0f,
            0f,
            20f,
            20f,
            -1f,
            1f,
            0f,
            1f,
            false,
            false
        );

        // --- Grupo frente: power 1.0 ---
        ControllerGroup engineForwardGroup = controller.addGroup("engine_drive");
        // --- Grupo ré: power -1.0 ---
        ControllerGroup engineReverseGroup = controller.addGroup("engine_reverse");

        ControllerGroup turnOff = controller.addGroup("engine_off");

        //Adiciona o motor no grupo da frente
        engineForwardGroup.add(engine);

        //Adiciona o motor no grupo de ré
        engineReverseGroup.add(engine);

        //Adiciona o motor para desligar
        turnOff.add(engine);

        //Seta a config do grupo da frente
        engineForwardGroup.setConfig(engine, new VehicleEngineComponent.VehicleEngineConfig(1f, true));
        //Seta a config do grupo de ré
        engineReverseGroup.setConfig(engine, new VehicleEngineComponent.VehicleEngineConfig(-1f, true));

        turnOff.setConfig(engine, new VehicleEngineComponent.VehicleEngineConfig(0f, false));


        // --- Adiciona ao node ---
        node_1.addVehicleComponent(engine, false, true, true);

        node_1.addVehicleComponent(door, true, true, true);
        node_1.addVehicleComponent(door2, false, true, true);

        node_1.addVehicleComponent(controller, false, true, true);

        Transform transform = node_1.getBody().getTransform();
        node_1.getBody().setTransform(
            transform.getPosition(),
            45f
        );

        node_1.getBody().setFixedRotation(false);

        //TO-do: lidar com o sistema de controle,
        //  para que não possamos ativar quando o jogador estiver fora do sub ou fora do alcance correto

    }

    private static List<SubmarinePart> getBaseSubmarineParts() {
        List<SubmarinePart> subParts = new ArrayList<>();

        float
            width = 120,
            height = 10;

        int
            categoryBit = VEHICLE.bit(),
            maskBit = VEHICLE_PASSENGER.bit();

        SubmarinePart corridor = new SubmarinePart(1, "corridor");

        corridor.baseMass = 0.01f;
        corridor.internalMarginDown
            = corridor.internalMarginUp
            = corridor.internalMarginLeft
            = corridor.internalMarginRight
            = 2;

        corridor.addInternalFixture(
            0,
            0,
            0,
            25,
            0,
            width,
            height,
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
            width,
            height,
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
            ContactUtils.keys.VEHICLE_LISTENER,
            new SubmarineContactListener()
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

    private int[][] initBaseTileMap() {
        int
            width = TILES_VIEW_WIDTH * 3,
            height = TILES_VIEW_HEIGHT;

        int[][] toReturn = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                toReturn[y][x] = 0;

                List<Boolean> canCreate = new ArrayList<>();
                canCreate.add(y >= height - 2);  //chão
                canCreate.add(y == 0);                      //teto
                canCreate.add(x == 0);                      //parede esquerda
                canCreate.add(x == width - 1);   //parede direita

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

    /// Define a câmera do jogo (chamado por PlayScreen após criar o manager)
    public void setGameCamera(OrthographicCameraManager camera) {
        this.gameCamera = camera;
    }

    public GlobalProjectilePool getGlobalProjectilePool() {
        return globalProjectilePool;
    }
}
