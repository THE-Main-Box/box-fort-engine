package official.sketchBook.game.components_related.vehicle;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractionTriggerer;
import official.sketchBook.engine.components_related.system_utils.ControllerGroup;
import official.sketchBook.engine.components_related.vehicle.VehicleInteractableComponent;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

import java.util.ArrayList;

public class VehicleControllerComponent extends VehicleInteractableComponent {

    public static int quantity;

    /// Lista de grupos de controle
    private final ArrayList<ControllerGroup> groups;

    public VehicleControllerComponent(
        VehicleSection ownerSection,
        FixtureData fixData,
        FixtureData triggerFixData
    ) {
        super(
            "Controller_id: " + quantity,
            String.valueOf(quantity),
            ownerSection,
            VehicleComponentType.LOGICAL_INTERNAL,
            fixData,
            triggerFixData
        );
        quantity++;
        this.groups = new ArrayList<>();

        this.initObject();
    }

    @Override
    public void initObject() {
        super.initObject();
    }

    @Override
    public void executeInteraction(InteractionTriggerer triggerer) {
        //TO-DO: Remover esse sistema temporário,
        // e usar um sistema mais funcional e flexivel para lidar com o input
        PendingGroupInput.set(this);
    }

    @Override
    public boolean canInteract() {
        return true;
    }

    // --- Grupos ---

    /// Cria e adiciona um novo grupo
    public ControllerGroup addGroup(String name) {
        ControllerGroup group = new ControllerGroup(name);
        groups.add(group);
        return group;
    }

    /// Remove um grupo
    public void removeGroup(ControllerGroup group) {
        group.clear();
        groups.remove(group);
    }

    /// Aciona um grupo específico
    public void triggerGroup(ControllerGroup group) {
        group.trigger();
    }

    /// Expõe a lista de grupos para o HUD iterar
    public ArrayList<ControllerGroup> getGroups() {
        return groups;
    }

    // --- Dispose ---

    @Override
    protected void executeDispose() {
        super.executeDispose();
        for (int i = 0; i < groups.size(); i++) {
            groups.get(i).clear();
        }
        groups.clear();
    }

    public static class PendingGroupInput {

        private static VehicleControllerComponent pendingController = null;

        public static void set(VehicleControllerComponent controller) {
            pendingController = controller;
            System.out.println("Controller ativo: " + controller.name);
            System.out.println("Grupos disponíveis:");
            for (int i = 0; i < controller.groups.size(); i++) {
                System.out.println("  [" + i + "] " + controller.groups.get(i).name);
            }
            System.out.println("Chame PendingGroupInput.trigger(index) para acionar um grupo.");
        }

        /// Aciona o grupo pelo índice — índice -1 é ignorado
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
