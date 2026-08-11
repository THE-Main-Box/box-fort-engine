package official.sketchBook.game.serialization;

import com.badlogic.gdx.physics.box2d.Transform;
import official.sketchBook.engine.components_related.system_utils.ControllerGroup;
import official.sketchBook.engine.game_object_related.vehicle_related.Submarine;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarineNode;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarinePart;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.model.RoomLiquid;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;
import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.util_related.path.SerializationPaths;
import official.sketchBook.engine.util_related.serialization.SaveDataInstanceRegistry;
import official.sketchBook.engine.util_related.serialization.SaveManager;
import official.sketchBook.engine.world_gen.PlayableRoomManager;
import official.sketchBook.engine.world_gen.model.PhysicalPlayableRoom;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.components_related.vehicle.VehicleControllerComponent;
import official.sketchBook.game.components_related.vehicle.VehicleDoor;
import official.sketchBook.game.components_related.vehicle.VehicleEngineComponent;
import official.sketchBook.game.dataManager_related.GameObjectDataManager;
import official.sketchBook.game.gameObject_related.player.Player;
import official.sketchBook.game.gameObject_related.player.PlayerContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.*;
import static official.sketchBook.game.util_related.constants.RenderingConstants.TILES_VIEW_HEIGHT;
import static official.sketchBook.game.util_related.constants.RenderingConstants.TILES_VIEW_WIDTH;
import static official.sketchBook.game.util_related.constants.WorldConstants.PlayerConstants.HEIGHT;
import static official.sketchBook.game.util_related.constants.WorldConstants.PlayerConstants.WIDTH;
import static official.sketchBook.game.util_related.constants.WorldConstants.TILE_SIZE_PX;

/**
 * Orquestrador central de carregamento de um save: sala primeiro,
 * objetos depois ? nessa ordem, sempre, porque objetos de sala
 * (Player, ve�culos) precisam de uma {@link PlayableRoom} pronta.
 * <p>
 * Constru�do com 1 argumento (s� o {@link GameObjectDataManager}) ?
 * usa {@link SaveDataInstanceRegistry#GLOBAL} internamente pra montar
 * seu pr�prio {@link SaveManager}, em vez de exigir que quem cria este
 * loader tamb�m monte um SaveManager na m�o. Isso bate com o registro
 * autom�tico via {@code static{}} de cada classe savable (Player,
 * etc): o registry global j� est� populado antes de qualquer coisa
 * aqui rodar.
 */
public class SaveFileLoader {

    private final GameObjectDataManager objectManager;
    private final SaveManager saveManager;

    public SaveFileLoader(GameObjectDataManager objectManager) {
        this.objectManager = objectManager;
        this.saveManager = new SaveManager(SaveDataInstanceRegistry.GLOBAL);
    }

    /**
     * Ponto de entrada �nico: carrega um save inteiro, na ordem
     * correta.
     */
    public void loadSaveFile() {
        PlayableRoom room = loadCurrentRoom();
        loadRoomLiquid(room);

        loadPlayer(room);
        loadVehicles(room);
    }

    private void loadRoomLiquid(PlayableRoom currentRoom) {
        List<LiquidRegion> regionList = new ArrayList<>();
        LiquidData data;

        regionList.add(
            new LiquidRegion(
                200,
                10,
                5000,
                150
            )
        );

        data = new LiquidData(
            "water",
            1,
            8f,
            3f
        );

        RoomLiquid water = new RoomLiquid(
            objectManager,
            currentRoom,
            data,
            regionList
        );
    }

    private PlayableRoom loadCurrentRoom() {
        PlayableRoomManager manager = objectManager.getRoomManager();

        PhysicalPlayableRoom currentRoom = new PhysicalPlayableRoom(
            1,
            0,
            0,
            objectManager.getPhysicsWorld()
        );

        currentRoom.addNewTileModel(
            1
        );

        currentRoom.initRoomGrid(
            initBaseTileMap(),
            TILE_SIZE_PX
        );

        objectManager.setCurrentRoom(currentRoom);

        return currentRoom;
    }

    private int[][][] initBaseTileMap() {
        int
            layers = 1,
            width = TILES_VIEW_WIDTH * 3,
            height = TILES_VIEW_HEIGHT;

        int[][][] toReturn = new int[layers][height][width];

        int layer = 0; // única camada gerada por este método, por enquanto

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                toReturn[layer][y][x] = 0;

                boolean isBorderTile =
                    y >= height - 2 ||
                        y == 0 ||
                        x == 0 ||
                        x == width - 1;

                if (isBorderTile) {
                    toReturn[layer][y][x] = 1;
                }
            }
        }

        return toReturn;
    }

    private void loadPlayer(PlayableRoom room) {
        PlayerContext context = new PlayerContext(
            objectManager,
            room,
            RoomObjectScope.GLOBAL,
            0, 0, 0, 0,   // x,y,z,rotation: sobrescritos pelo loadFields do DTO a partir do SaveData
            WIDTH,
            HEIGHT,
            1f,
            1f,
            false,
            false,
            0,
            0,
            false
        );

        objectManager.mainPlayer = saveManager.loadAndInstantiate(
            SerializationPaths.getCurrentSaveFilePath(),        //Path da pasta do save atual
            Player.class.getSimpleName().toLowerCase(),         //Nome do arquivo a buscar (nome da classe minúscula)
            context                                             //Contexto de instanciação
        );

    }

    private void loadVehicles(PlayableRoom currentRoom) {
        float
            subX = 400,
            subY = 190;

        List<SubmarinePart> subParts = getBaseSubmarineParts();

        List<SubmarineNode> nodeList = new ArrayList<>();

        SubmarineNode node_1 = new SubmarineNode(
            objectManager.getPhysicsWorld(),
            subParts,
            subX,
            subY,
            0,
            0,
            false,
            false
        );

        SubmarineNode node_2 = new SubmarineNode(
            objectManager.getPhysicsWorld(),
            getBaseSubmarineParts(),
            subX + 120,
            subY,
            0,
            0,
            false,
            false
        );

        nodeList.add(node_2);
        nodeList.add(node_1);

        Submarine baseSubmarine = new Submarine(
            objectManager,
            currentRoom,
            nodeList
        );

        VehicleDoor door = new VehicleDoor(
            node_1,
            new FixtureData(
                0, 0, 55, 0, 0, 9, 40,
                VEHICLE.bit(), VEHICLE_PASSENGER.bit(), false, false
            ),
            new FixtureData(
                0, 0, 55 - 9, 0, 0, 9 * 4, 40,
                INTERACTABLE.bit(), INTERACTABLE_TRIGGERER.bit(), false, true
            ),
            false, false, false
        );

        VehicleDoor door2 = new VehicleDoor(
            node_1,
            new FixtureData(
                0, 0, 0, 0, 0, -55, 0, 0, 9, 40,
                VEHICLE.bit(), VEHICLE_PASSENGER.bit(), false, false
            ),
            new FixtureData(
                0, 0, -55 + 9, 0, 0, 9 * 4, 40,
                INTERACTABLE.bit(), INTERACTABLE_TRIGGERER.bit(), false, true
            ),
            false, false, true
        );

        VehicleControllerComponent controller = new VehicleControllerComponent(
            node_1,
            new FixtureData(
                0, 0, 0, 0, 0, 0, 0,
                VEHICLE.bit(), VEHICLE_PASSENGER.bit(), false, false
            ),
            new FixtureData(
                0, 0, 0, 0, 0, 12 * 4, 30,
                INTERACTABLE.bit(), INTERACTABLE_TRIGGERER.bit(), false, true
            )
        );

        VehicleEngineComponent engine = new VehicleEngineComponent(
            node_1,
            node_1.getBody(),
            1f, 0f, 0f, 0f, 500f, -1f, 1f, 0f, 10f, false, false
        );

        ControllerGroup engineForwardGroup = controller.addGroup("engine_drive");
        ControllerGroup engineReverseGroup = controller.addGroup("engine_reverse");
        ControllerGroup turnOff = controller.addGroup("engine_off");

        engineForwardGroup.add(engine);
        engineReverseGroup.add(engine);
        turnOff.add(engine);

        engineForwardGroup.setConfig(engine, new VehicleEngineComponent.VehicleEngineConfig(1f, true));
        engineReverseGroup.setConfig(engine, new VehicleEngineComponent.VehicleEngineConfig(-1f, true));
        turnOff.setConfig(engine, new VehicleEngineComponent.VehicleEngineConfig(0f, false));

        node_1.addVehicleComponent(engine, false, true, true);
        node_1.addVehicleComponent(door, true, true, true);
        node_1.addVehicleComponent(door2, false, true, true);
        node_1.addVehicleComponent(controller, false, true, true);

        Transform t = node_1.getBody().getTransform();
        node_1.getBody().setTransform(
            t.getPosition(),
            45
        );
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

        corridor.updateBaseMass(1.5f);

        corridor.setMargins(2, 2, 2, 2);

        corridor.addInternalFixture(
            0, 0, 0, 25, 0, width, height, categoryBit, maskBit, true, false
        );

        corridor.addInternalFixture(
            0, 0, 0, -25, 0, width, height, categoryBit, maskBit, true, false
        );

        subParts.add(corridor);
        return subParts;
    }
}
