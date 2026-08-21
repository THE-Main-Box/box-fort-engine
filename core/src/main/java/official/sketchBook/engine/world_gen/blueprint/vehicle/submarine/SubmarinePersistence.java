package official.sketchBook.engine.world_gen.blueprint.vehicle.submarine;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;
import official.sketchBook.engine.game_object_related.vehicle_related.Submarine;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarineNode;
import official.sketchBook.engine.util_related.path.SerializationPaths;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveDataInstance;
import official.sketchBook.engine.util_related.serialization.persistance.SaveManager;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.save_data.SubmarineBlueprintSaveData;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.save_data.SubmarineStateSaveData;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.dataManager_related.GameObjectDataManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Carrega o blueprint global cru pelo tag. Não instancia nada físico.
     */
    public SubmarineBlueprint loadBlueprint(String tag) {
        return saveManager.loadAndInstantiate(
            SerializationPaths.Blueprints.Vehicles.BP_SUB,
            tag + ".json"
        );
    }

    /**
     * Carrega o estado salvo do jogador para este tag, ou null se ainda não existir.
     */
    public SubmarineState loadStateOrNull(String tag) {
        SaveDataInstance<SubmarineState> dto = saveManager.loadDtoOrNull(
            SerializationPaths.SaveCategories.vehicles(),
            tag + "_state.json"
        );
        return dto != null ? dto.newInstance() : null;
    }

    // ============================================================
    // CAMADA 2 ? CONVERSÃO (dado cru -> Submarine físico)
    // ============================================================

    /**
     * Converte um SubmarineState já carregado num Submarine, usando a posição gravada no state.
     */
    public Submarine convertToSubmarine(SubmarineState state, PlayableRoom room) {
        return convertToSubmarine(state.submarine, room, state.spawnX, state.spawnY);
    }




    public Submarine convertToSubmarine(SubmarineBlueprint bp, PlayableRoom room, float spawnX, float spawnY) {
        List<SubmarineNode> nodes = bp.toSubmarineNodes(objectManager.getPhysicsWorld(), spawnX, spawnY);

        Submarine submarine = new Submarine(bp.tag, objectManager, room, nodes);

        attachAllNodeComponents(bp, nodes);
        resolveComponentReferences(nodes);

        return submarine;
    }

    private void attachAllNodeComponents(SubmarineBlueprint bp, List<SubmarineNode> nodes) {
        List<SubmarineNodeBlueprint> nodeBlueprints = bp.nodes;
        for (int i = 0; i < nodeBlueprints.size(); i++) {
            nodeBlueprints.get(i).attachComponentsToNode(nodes.get(i));
        }
    }

    private void resolveComponentReferences(List<SubmarineNode> nodes) {
        Map<String, VehicleBaseComponent> byId = new HashMap<>();

        for (int i = 0; i < nodes.size(); i++) {
            List<VehicleBaseComponent> components = nodes.get(i).getVehicleComponentList();
            for (int j = 0; j < components.size(); j++) {
                VehicleBaseComponent component = components.get(j);
                byId.put(component.getId(), component);
            }
        }

        for (int i = 0; i < nodes.size(); i++) {
            List<VehicleBaseComponent> components = nodes.get(i).getVehicleComponentList();
            for (int j = 0; j < components.size(); j++) {
                components.get(j).resolveReferences(byId);
            }
        }
    }

    // ============================================================
    // CAMADA 3 ? COMPOSIÇÕES PRONTAS (conveniência, opcionais)
    // ============================================================

    /**
     * Vai direto de blueprint global pra Submarine, ignorando qualquer save existente.
     */
    public Submarine createSubmarineFromBlueprint(String tag, PlayableRoom room, float spawnX, float spawnY) {
        return convertToSubmarine(loadBlueprint(tag), room, spawnX, spawnY);
    }

    /**
     * Vai direto de save do jogador pra Submarine. Lança se o save não existir — sem fallback implícito.
     */
    public Submarine createSubmarineFromState(String tag, PlayableRoom room) {
        SubmarineState state = loadStateOrNull(tag);
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
        SubmarineState state = loadStateOrNull(tag);

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

    /// Converte o submarino em uma blueprint e salva nos arquivos de bp
    public SubmarineBlueprint saveAsBlueprint(Submarine submarine) {

        SubmarineBlueprint bp = captureBlueprintFromLiveSubmarine(submarine);
        saveBlueprint(bp);
        return bp;
    }

    /// Converte o submarino em uma blueprint e salva o estado atual nos arquivos de save
    public void saveSubmarineState(Submarine submarine) {
        //Obtemos a posição do primeiro node e usamos esta
        Vector2 primaryPos = submarine.getSections().get(0).getBody().getPosition();

        //Geramos um novo estado
        SubmarineState state = new SubmarineState(
            captureBlueprintFromLiveSubmarine(submarine),
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

    /// Gera uma blueprint com base num submarino
    private SubmarineBlueprint captureBlueprintFromLiveSubmarine(Submarine submarine) {

        //Obtemos os nodes do submarino
        List<SubmarineNode> nodes = submarine.getSections();
        //Obtemos a posição de origem do primeiro node
        Vector2 originPos = nodes.get(0).getBody().getPosition();
        //Geramos uma lista de blueprints para anexarmos na bp que iremos retornar
        List<SubmarineNodeBlueprint> nodeBp = new ArrayList<>();

        //Percorremos a lista de nodes
        for (int i = 0; i < nodes.size(); i++) {
            //Pegamos o atual
            SubmarineNode node = nodes.get(i);
            //Pegamos a posição atual do node atual
            Vector2 nodePos = node.getBody().getPosition();

            //Descobrimos o offset da posição de origem do submarino
            float relOffsetX = toPixels(nodePos.x - originPos.x);
            float relOffsetY = toPixels(nodePos.y - originPos.y);

            //Adicionamos mais uma nova bp com os dados necessários
            nodeBp.add(
                new SubmarineNodeBlueprint(
                    node,
                    relOffsetX,
                    relOffsetY
                )
            );
        }

        //Retornamos uma nova blueprint contendo os dados do submarino e seus nodes
        return new SubmarineBlueprint(
            submarine.getName(),
            nodeBp
        );
    }
}
