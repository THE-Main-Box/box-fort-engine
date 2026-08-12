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
import official.sketchBook.engine.world_gen.model.TilePhysicsConfig;
import official.sketchBook.engine.world_gen.util.LayerGenerationRegistry;
import official.sketchBook.engine.world_gen.util.PlayableRoomManager;
import official.sketchBook.engine.world_gen.model.PhysicalPlayableRoom;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.engine.world_gen.util.RoomGenerator;
import official.sketchBook.game.components_related.vehicle.VehicleControllerComponent;
import official.sketchBook.game.components_related.vehicle.VehicleDoor;
import official.sketchBook.game.components_related.vehicle.VehicleEngineComponent;
import official.sketchBook.game.dataManager_related.GameObjectDataManager;
import official.sketchBook.game.gameObject_related.player.Player;
import official.sketchBook.game.gameObject_related.player.PlayerContext;
import official.sketchBook.game.world_gen.generation.LiquidGenerationResolver;
import official.sketchBook.game.world_gen.generation.StandardTileGenerationResolver;

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
        loadRoomLiquid();
        loadRoomStructure();

        PlayableRoom room = loadCurrentRoom(); // cria a sala, define os estilos por camada

        loadPlayer(room);
        loadVehicles(room);
    }

    private void loadRoomStructure() {
        LayerGenerationRegistry.GLOBAL.register(
            0, // estilo "estrutura padrão"
            new StandardTileGenerationResolver(
                TILE_SIZE_PX,
                Collections.singletonList((
                    new TilePhysicsConfig(
                        1,      // tileId da borda
                        true,   // mergeable: agrupa bordas contíguas em retângulos maiores
                        false,  // ownBodyPerCluster: false -> vai pra body compartilhada da sala
                        (clusterWidthTiles, clusterHeightTiles) -> new FixtureData(
                            0f,
                            0f,
                            0f,                                  // density, restitution, friction
                            0,
                            0,                                        // globalOffsetX, globalOffsetY
                            0,
                            0,                                        // offsetX, offsetY (attachFixture soma o offset do cluster depois)
                            0,                                           // radius (não é círculo)
                            clusterWidthTiles * TILE_SIZE_PX,
                            clusterHeightTiles * TILE_SIZE_PX,
                            ENVIRONMENT.bit(),
                            (ENTITIES.bit() | PROJECTILES.bit() | SENSOR.bit() | VEHICLE.bit()),
                            false,  // isCircle
                            false   // isSensor
                        )
                    )
                ))
            )
        );
    }

    private void loadRoomLiquid() {
        // registro (uma vez, no boot do jogo — ex: static{} ou setup)
        LayerGenerationRegistry.GLOBAL.register(
            1, // estilo "líquido", por exemplo
            new LiquidGenerationResolver(
                objectManager,
                new LiquidData("water", 2, 8f, 3f),
                2,          // tileId que representa água na grid
                TILE_SIZE_PX
            )
        );
    }

    private PlayableRoom loadCurrentRoom() {
        PhysicalPlayableRoom currentRoom = new PhysicalPlayableRoom(
            1,
            0,
            0,
            objectManager.getPhysicsWorld()
        );

        currentRoom.initRoomGrid(
            initBaseTileMap(),
            TILE_SIZE_PX
        );

        // camada 0 (estrutura): ainda sem resolver de física de tile, então
        // fica marcada como -1 (skip) até esse sistema existir
        currentRoom.setLayerGenerationStyle(0, 0);

        //TO-DO:FAZER COM QUE O SISTEMA IDENTIFIQUE AS CAMADAS E A GERAÇÃO DELAS AUTOMATICAMENTE
        // COM OS DADOS SERIALIZÁVEIS

        // camada 1 (água): estilo 1, resolvido pelo LiquidGenerationResolver
        // já registrado em loadRoomLiquid()
        currentRoom.setLayerGenerationStyle(1, 1);

        objectManager.setCurrentRoom(currentRoom);

        RoomGenerator.generate(currentRoom); // dispara a geração de fato

        return currentRoom;
    }

    private static final int WATER_HEIGHT_TILES = 5;

    private int[][][] initBaseTileMap() {
        int
            layers = 2, // camada 0 = estrutura (borda), camada 1 = água
            width = TILES_VIEW_WIDTH * 3,
            height = TILES_VIEW_HEIGHT;

        int[][][] toReturn = new int[layers][height][width];

        int structureLayer = 0;
        int liquidLayer = 1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // borda: topo, base, esquerda, direita da tela inteira
                boolean isBorderTile =
                    y == 0 ||
                        y == height - 1 ||
                        x == 0 ||
                        x == width - 1;

                toReturn[structureLayer][y][x] = isBorderTile ? 1 : 0;

                // água: as WATER_HEIGHT_TILES linhas imediatamente acima do chão
                boolean isWaterTile =
                    y > 0 &&
                        y <= WATER_HEIGHT_TILES;
                toReturn[liquidLayer][y][x] = isWaterTile ? 2 : 0;
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
