package official.sketchBook.engine.world_gen.util;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.game_object_related.vehicle_related.Submarine;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarineNode;
import official.sketchBook.engine.util_related.path.SerializationPaths;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveDataInstance;
import official.sketchBook.engine.util_related.serialization.persistance.SaveManager;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.SubmarineBlueprint;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.SubmarineNodeBlueprint;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.save_data.SubmarineBlueprintSaveData;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.save_data.SubmarineStateSaveData;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.dataManager_related.GameObjectDataManager;

import java.util.List;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.toPixels;

public class SubmarinePersistence {

    private final SaveManager saveManager;
    private final GameObjectDataManager objectManager;

    public SubmarinePersistence(SaveManager saveManager, GameObjectDataManager objectManager) {
        this.saveManager = saveManager;
        this.objectManager = objectManager;
    }

    // ============================================================
    // CAMADA 1 ? LEITURA DE DADO CRU (sem física, sem Submarine)
    // ============================================================

    /** Carrega o blueprint global cru pelo tag. Não instancia nada físico. */
    public SubmarineBlueprint loadBlueprint(String tag) {
        return saveManager.loadAndInstantiate(
            SerializationPaths.Blueprints.Vehicles.BP_SUB,
            tag + ".json"
        );
    }

    /** Carrega o estado salvo do jogador para este tag, ou null se ainda não existir. */
    public SubmarineStateSaveData.SubmarineState loadStateOrNull(String tag) {
        SaveDataInstance<SubmarineStateSaveData.SubmarineState> dto = saveManager.loadDtoOrNull(
            SerializationPaths.SaveCategories.vehicles(),
            tag + "_state.json"
        );
        return dto != null ? dto.newInstance() : null;
    }

    // ============================================================
    // CAMADA 2 ? CONVERSÃO (dado cru -> Submarine físico)
    // ============================================================

    /** Converte um SubmarineBlueprint já carregado num Submarine, na posição informada. */
    public Submarine convertToSubmarine(SubmarineBlueprint bp, PlayableRoom room, float spawnX, float spawnY) {
        List<SubmarineNode> nodes = bp.toSubmarineNodes(objectManager.getPhysicsWorld(), spawnX, spawnY);
        return new Submarine(bp.tag, objectManager, room, nodes);
    }

    /** Converte um SubmarineState já carregado num Submarine, usando a posição gravada no state. */
    public Submarine convertToSubmarine(SubmarineStateSaveData.SubmarineState state, PlayableRoom room) {
        return convertToSubmarine(state.submarine, room, state.spawnX, state.spawnY);
    }

    // ============================================================
    // CAMADA 3 ? COMPOSIÇÕES PRONTAS (conveniência, opcionais)
    // ============================================================

    /** Vai direto de blueprint global pra Submarine, ignorando qualquer save existente. */
    public Submarine createSubmarineFromBlueprint(String tag, PlayableRoom room, float spawnX, float spawnY) {
        return convertToSubmarine(loadBlueprint(tag), room, spawnX, spawnY);
    }

    /** Vai direto de save do jogador pra Submarine. Lança se o save não existir — sem fallback implícito. */
    public Submarine createSubmarineFromState(String tag, PlayableRoom room) {
        SubmarineStateSaveData.SubmarineState state = loadStateOrNull(tag);
        if (state == null) {
            throw new IllegalStateException("Nenhum save encontrado para o submarino '" + tag + "'");
        }
        return convertToSubmarine(state, room);
    }

    /**
     * Composição de conveniência: tenta save primeiro, cai pro blueprint
     * com fallbackSpawnX/Y se não existir. Não é a única forma de
     * carregar — quem quiser controle total usa loadBlueprint/loadStateOrNull
     * + convertToSubmarine diretamente.
     */
    public Submarine loadSubmarineWithFallback(
        String tag, PlayableRoom room, float fallbackSpawnX, float fallbackSpawnY
    ) {
        SubmarineStateSaveData.SubmarineState state = loadStateOrNull(tag);

        if (state != null) {
            return convertToSubmarine(state, room);
        }

        return createSubmarineFromBlueprint(tag, room, fallbackSpawnX, fallbackSpawnY);
    }

    // ============================================================
    // SALVAMENTO
    // ============================================================

    public void saveBlueprint(SubmarineBlueprint bp) {
        saveManager.save(
            SerializationPaths.Blueprints.Vehicles.BP_SUB,
            bp.tag + ".json",
            SubmarineBlueprintSaveData.TYPE_KEY,
            bp
        );
    }

    public SubmarineBlueprint saveAsBlueprint(Submarine submarine) {
        SubmarineBlueprint bp = captureBlueprintFromLiveSubmarine(submarine);
        saveBlueprint(bp);
        return bp;
    }

    public void saveSubmarineState(Submarine submarine) {
        SubmarineBlueprint currentBlueprint = captureBlueprintFromLiveSubmarine(submarine);
        Vector2 primaryPos = submarine.getSections().get(0).getBody().getPosition();

        SubmarineStateSaveData.SubmarineState state = new SubmarineStateSaveData.SubmarineState(
            currentBlueprint,
            toPixels(primaryPos.x),
            toPixels(primaryPos.y)
        );

        saveManager.save(
            SerializationPaths.SaveCategories.vehicles(),
            submarine.getName() + "_state.json",
            SubmarineStateSaveData.TYPE_KEY,
            state
        );
    }

    private SubmarineBlueprint captureBlueprintFromLiveSubmarine(Submarine submarine) {
        SubmarineBlueprint bp = new SubmarineBlueprint();
        bp.tag = submarine.getName();

        List<SubmarineNode> nodes = submarine.getSections();
        Vector2 originPos = nodes.get(0).getBody().getPosition();

        for (int i = 0; i < nodes.size(); i++) {
            SubmarineNode node = nodes.get(i);
            Vector2 nodePos = node.getBody().getPosition();

            float relOffsetX = toPixels(nodePos.x - originPos.x);
            float relOffsetY = toPixels(nodePos.y - originPos.y);

            bp.nodes.add(new SubmarineNodeBlueprint(node, relOffsetX, relOffsetY));
        }

        return bp;
    }
}
