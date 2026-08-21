package official.sketchBook.game.serialization;

import official.sketchBook.engine.components_related.system_utils.ControllerGroup;
import official.sketchBook.engine.game_object_related.vehicle_related.Submarine;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarineNode;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarinePart;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.util_related.path.SerializationPaths;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveDataInstanceRegistry;
import official.sketchBook.engine.util_related.serialization.persistance.SaveManager;
import official.sketchBook.engine.world_gen.blueprint.room.RoomBlueprint;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.SubmarinePersistence;
import official.sketchBook.engine.world_gen.model.PhysicalPlayableRoom;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.engine.world_gen.model.TilePhysicsConfig;
import official.sketchBook.engine.world_gen.util.LayerGenerationRegistry;
import official.sketchBook.engine.world_gen.util.RoomGenerator;
import official.sketchBook.engine.world_gen.util.SubmarineComponentUtils;
import official.sketchBook.game.components_related.vehicle.VehicleControllerComponent;
import official.sketchBook.game.components_related.vehicle.VehicleDoor;
import official.sketchBook.game.components_related.vehicle.VehicleEngineComponent;
import official.sketchBook.game.dataManager_related.GameObjectDataManager;
import official.sketchBook.game.gameObject_related.player.Player;
import official.sketchBook.game.gameObject_related.player.PlayerSaveData;
import official.sketchBook.game.world_gen.generation.LiquidGenerationResolver;
import official.sketchBook.game.world_gen.generation.StandardTileGenerationResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.*;
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
    private final SubmarinePersistence submarinePersistence;

    public SaveFileLoader(GameObjectDataManager objectManager) {
        this.objectManager = objectManager;
        this.saveManager = new SaveManager(SaveDataInstanceRegistry.GLOBAL);
        this.submarinePersistence = new SubmarinePersistence(saveManager, objectManager);
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

        testSubmarinePersistence(room);
        loadVehicles(room);

    }


//    private void saveTestRoomPersistence() {
//        int
//            width = 234,
//            height = 42;
//
//        RoomBlueprint bp = new RoomBlueprint(
//            "flooded_test_room",
//            width,
//            height,
//            new int[]{0, 1},
//            initBaseTileMap(
//                2,
//                height,
//                width
//            )
//        );
//
//        saveManager.save(
//            SerializationPaths.Blueprints.BP_ROOMS,
//            bp.name + ".json",
//            RoomBlueprintSaveData.TYPE_KEY,
//            bp
//        );
//    }

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
                        ObjectType.ENVIRONMENT,
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
        RoomBlueprint bp = saveManager.loadAndInstantiate(
            SerializationPaths.Blueprints.BP_ROOMS,
            "flooded_test_room.json"
        );

        PhysicalPlayableRoom currentRoom = new PhysicalPlayableRoom(
            bp.id,
            0,
            0,
            objectManager.getPhysicsWorld()
        );

        currentRoom.initRoomGrid(
            bp.grid,
            TILE_SIZE_PX
        );

        for (int layer = 0; layer < bp.layerGenerationStyles.length; layer++) {
            currentRoom.setLayerGenerationStyle(
                layer,
                bp.layerGenerationStyles[layer]
            );
        }

        objectManager.setCurrentRoom(currentRoom);

        RoomGenerator.generate(currentRoom);

        return currentRoom;
    }

//    private static final int WATER_HEIGHT_TILES = 5;

//    private int[][][] initBaseTileMap(int layers, int height, int width) {
//
//        int[][][] toReturn = new int[layers][height][width];
//
//        int structureLayer = 0;
//        int liquidLayer = 1;
//
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                // borda: topo, base, esquerda, direita da tela inteira
//                boolean isBorderTile =
//                    y == 0 ||
//                        y == height - 1 ||
//                        x == 0 ||
//                        x == width - 1;
//
//                toReturn[structureLayer][y][x] = isBorderTile ? 1 : 0;
//
//                // água: as WATER_HEIGHT_TILES linhas imediatamente acima do chão
//                boolean isWaterTile =
//                    y > 0 &&
//                        y <= WATER_HEIGHT_TILES;
//                toReturn[liquidLayer][y][x] = isWaterTile ? 2 : 0;
//            }
//        }
//
//        return toReturn;
//    }

    private void loadPlayer(PlayableRoom room) {

        PlayerSaveData.setWorldDataManager(objectManager);
        PlayerSaveData.setOwnerRoom(room);

        objectManager.mainPlayer = saveManager.loadAndInstantiate(
            SerializationPaths.SaveCategories.entities(),        //Path da pasta do save atual
            Player.class.getSimpleName().toLowerCase() + ".json"         //Nome do arquivo a buscar (nome da classe minúscula)
        );

    }

    private void loadVehicles(PlayableRoom currentRoom) {

        System.out.println(
            submarinePersistence.createSubmarineFromState(
                    "walrus",
                    currentRoom
                )
                .getSections().get(0)
                .getVehicleComponentList().get(0)
                .getId()
        );
    }

    private void testSubmarinePersistence(PlayableRoom currentRoom) {
        List<SubmarinePart> subParts = getBaseSubmarineParts();
        List<SubmarineNode> nodeList = new ArrayList<>();

        nodeList.add(
            new SubmarineNode(
                objectManager.getPhysicsWorld(),
                subParts,
                400, 190, 0, 0, false, false
            )
        );

        Submarine baseSubmarine = new Submarine(
            "walrus",
            objectManager,
            currentRoom,
            nodeList
        );

        SubmarineNode node = nodeList.get(0);

        // --- Portas ---
        List<VehicleDoor> doorList = new ArrayList<>();

        doorList.add(new VehicleDoor(
            SubmarineComponentUtils.generateComponentId(VehicleDoor.TYPE_KEY, doorList),
            new FixtureData(0, 0, 55, 0, 0, 9, 40, VEHICLE.bit(), VEHICLE_PASSENGER.bit(), false, false),
            new FixtureData(0, 0, 55, 0, 0, 9 * 4, 40, INTERACTABLE.bit(), INTERACTABLE_TRIGGERER.bit(), false, true),
            false, false, false
        ));

        doorList.add(new VehicleDoor(
            SubmarineComponentUtils.generateComponentId(VehicleDoor.TYPE_KEY, doorList),
            new FixtureData(0, 0, -55, 0, 0, 9, 40, VEHICLE.bit(), VEHICLE_PASSENGER.bit(), false, false),
            new FixtureData(0, 0, -55, 0, 0, 9 * 4, 40, INTERACTABLE.bit(), INTERACTABLE_TRIGGERER.bit(), false, true),
            false, false, false
        ));

        for (VehicleDoor door : doorList) {
            door.attachToSection(node);
            door.initObject();
            node.addVehicleComponent(door);
        }

        // --- Motor ---
        List<VehicleEngineComponent> engineList = new ArrayList<>();

        VehicleEngineComponent engine = new VehicleEngineComponent(
            SubmarineComponentUtils.generateComponentId(VehicleEngineComponent.TYPE_KEY, engineList),
            1f, 0f,      // localDirX, localDirY
            0f, 0f,      // offsetX, offsetY
            500f,        // maxForce
            -1f, 1f,     // minPower, maxPower
            0f,          // defaultPower
            10f,         // accelerationRate
            true,       // startActive
            false        // isBroken
        );

        engineList.add(engine);

        engine.attachToSection(node);
        engine.initObject();
        node.addVehicleComponent(engine);

        // --- Controller ---
        List<VehicleControllerComponent> controllerList = new ArrayList<>();

        VehicleControllerComponent controller = new VehicleControllerComponent(
            SubmarineComponentUtils.generateComponentId(VehicleControllerComponent.TYPE_KEY, controllerList),
            null, // fixData nullable ? controller não tem corpo físico
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

        controllerList.add(controller);

        controller.attachToSection(node);
        controller.initObject();
        node.addVehicleComponent(controller);

        ControllerGroup engineForwardGroup = controller.addGroup("engine_drive");
        ControllerGroup engineReverseGroup = controller.addGroup("engine_reverse");
        ControllerGroup turnOff = controller.addGroup("engine_off");

        engineForwardGroup.add(engine);
        engineReverseGroup.add(engine);
        turnOff.add(engine);

        engineForwardGroup.setConfig(engine, new VehicleEngineComponent.VehicleEngineConfig(1f, true));
        engineReverseGroup.setConfig(engine, new VehicleEngineComponent.VehicleEngineConfig(-1f, true));
        turnOff.setConfig(engine, new VehicleEngineComponent.VehicleEngineConfig(0f, false));

        submarinePersistence.saveAsBlueprint(baseSubmarine);
        submarinePersistence.saveSubmarineState(baseSubmarine);

        baseSubmarine.markToDestroy();
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
