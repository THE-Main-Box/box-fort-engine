package official.sketchBook.game.components_related.vehicle;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractionTriggerer;
import official.sketchBook.engine.components_related.system_utils.ControllerGroup;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;
import official.sketchBook.engine.components_related.vehicle.VehicleInteractableComponent;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;
import official.sketchBook.engine.world_gen.blueprint.embedded.FixtureDataBlueprint;

import java.util.ArrayList;
import java.util.Map;

public class VehicleControllerComponent extends VehicleInteractableComponent {

    public static final String TYPE_KEY = "vehicle_controller";

    private final ArrayList<ControllerGroup> groups;

    public VehicleControllerComponent(
        String id,
        FixtureData fixData,
        FixtureData triggerFixData
    ) {
        super(id, VehicleComponentType.LOGICAL_INTERNAL, fixData, triggerFixData);
        this.groups = new ArrayList<>();
    }

    /// Construtor vazio ? exigido pra reflection (registry).
    public VehicleControllerComponent() {
        super();
        this.groups = new ArrayList<>();
    }

    @Override
    public void initObject() {
        super.initObject();
    }

    @Override
    public void executeInteraction(InteractionTriggerer triggerer) {
        PendingGroupInput.set(this);
    }

    @Override
    public boolean canInteract() {
        return true;
    }

    public ControllerGroup addGroup(String name) {
        ControllerGroup group = new ControllerGroup(name);
        groups.add(group);
        return group;
    }

    public void removeGroup(ControllerGroup group) {
        group.clear();
        groups.remove(group);
    }

    public void triggerGroup(ControllerGroup group) {
        group.trigger();
    }

    public ArrayList<ControllerGroup> getGroups() {
        return groups;
    }

    @Override
    protected void executeDispose() {
        super.executeDispose();
        for (int i = 0; i < groups.size(); i++) {
            groups.get(i).clear();
        }
        groups.clear();
    }

    // ============================================================
    // SERIALIZA??O
    // ============================================================

    @Override
    public SaveData toSaveData() {
        SaveData data = new SaveData()
            .put("id", id)
            .putTypeKey(TYPE_KEY)
            .putEmbedded("trigger_fix_data", FixtureDataBlueprint.toBlueprint(triggerFixData))
            .putEmbeddedList("groups", groups);

        if (fixData != null) {
            data.putEmbedded("fix_data", FixtureDataBlueprint.toBlueprint(fixData));
        }

        return data;
    }

    @Override
    public void load(SaveData data) {
        this.id = data.getStringRequired("id");

        FixtureDataBlueprint fixBp = data.getEmbedded("fix_data", FixtureDataBlueprint.class);
        this.fixData = fixBp != null ? fixBp.toFixtureData(0, 0) : null;

        FixtureDataBlueprint triggerBp = data.getEmbedded("trigger_fix_data", FixtureDataBlueprint.class);
        this.triggerFixData = triggerBp.toFixtureData(0, 0);

        this.groups.clear();
        this.groups.addAll(data.getEmbeddedList("groups", ControllerGroup.class));

        this.type = VehicleComponentType.LOGICAL_INTERNAL;
    }

    @Override
    public void resolveReferences(Map<String, VehicleBaseComponent> byId) {
        for (int i = 0; i < groups.size(); i++) {
            groups.get(i).resolveReferences(byId);
        }
    }

    public static class PendingGroupInput {
        private static VehicleControllerComponent pendingController = null;

        public static void set(VehicleControllerComponent controller) {
            pendingController = controller;
            System.out.println("Controller ativo: " + controller.id);
            System.out.println("Grupos disponíveis:");
            for (int i = 0; i < controller.groups.size(); i++) {
                System.out.println("  [" + i + "] " + controller.groups.get(i).name);
            }
            System.out.println("Chame PendingGroupInput.trigger(index) para acionar um grupo.");
        }

        public static void trigger(int index) {
            if (pendingController == null) return;
            if (index < 0 || index >= pendingController.groups.size()) return;
            pendingController.triggerGroup(pendingController.groups.get(index));
        }

        public static void clear() {
            pendingController = null;
        }

        public static boolean hasPending() {
            return pendingController != null;
        }
    }
}
