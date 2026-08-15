package official.sketchBook.game.serialization;

import com.badlogic.gdx.math.Vector2;
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
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.SubmarineBlueprint;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.SubmarineNodeBlueprint;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.save_data.SubmarineBlueprintSaveData;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.save_data.SubmarineStateSaveData;
import official.sketchBook.engine.world_gen.model.PhysicalPlayableRoom;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.engine.world_gen.model.TilePhysicsConfig;
import official.sketchBook.engine.world_gen.util.LayerGenerationRegistry;
import official.sketchBook.engine.world_gen.util.RoomGenerator;
import official.sketchBook.engine.world_gen.util.SubmarinePersistence;
import official.sketchBook.game.dataManager_related.GameObjectDataManager;
import official.sketchBook.game.gameObject_related.player.Player;
import official.sketchBook.game.gameObject_related.player.PlayerSaveData;
import official.sketchBook.game.world_gen.generation.LiquidGenerationResolver;
import official.sketchBook.game.world_gen.generation.StandardTileGenerationResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.*;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.toPixels;
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
        submarinePersistence.convertToSubmarine(
            submarinePersistence.loadStateOrNull("walrus"),
            currentRoom
        );
    }

    private void testSubmarinePersistence(PlayableRoom currentRoom) {
        List<SubmarinePart> subParts = getBaseSubmarineParts();
        List<SubmarineNode> nodeList = new ArrayList<>();

        SubmarineNode node_1 = new SubmarineNode(
            objectManager.getPhysicsWorld(),
            subParts,
            400,
            190,
            0,
            0,
            false,
            false
        );

        nodeList.add(node_1);

        Submarine baseSubmarine = new Submarine(
            "walrus",
            objectManager,
            currentRoom,
            nodeList
        );

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
