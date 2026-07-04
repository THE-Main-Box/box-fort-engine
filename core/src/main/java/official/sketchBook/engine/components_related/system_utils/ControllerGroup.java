package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ControllableObjectII;

import java.util.ArrayList;

public class ControllerGroup {

    /// Nome do grupo para identificação no HUD
    public final String name;

    /// Lista de wiráveis do grupo
    private final ArrayList<ControllableObjectII> targets = new ArrayList<>();

    public ControllerGroup(String name) {
        this.name = name;
    }

    public boolean add(ControllableObjectII target) {
        if (target == null) return false;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i) == target) return false;
        }
        targets.add(target);
        return true;
    }

    public boolean remove(ControllableObjectII target) {
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i) == target) {
                targets.remove(i);
                return true;
            }
        }
        return false;
    }

    public void trigger() {
        for (int i = 0; i < targets.size(); i++) {
            ControllableObjectII target = targets.get(i);
            if (target != null) target.interactByWiring();
        }
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    public void clear() {
        targets.clear();
    }

    public ArrayList<ControllableObjectII> getTargets() {
        return targets;
    }
}
