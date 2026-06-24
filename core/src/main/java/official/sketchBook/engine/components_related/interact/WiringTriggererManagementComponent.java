package official.sketchBook.engine.components_related.interact;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WirableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WiringTrigger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

public class WiringTriggererManagementComponent implements Component {

    private IdentityHashMap<
        WiringTrigger,
        ArrayList<WirableObjectII>
        > wiringMap;

    private boolean disposed = false;

    public WiringTriggererManagementComponent() {
        wiringMap = new IdentityHashMap<>();
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {

    }

    @Override
    public void initObject() {

    }

    /// Dá gatilho em todos os objetos ligados ao trigger
    public void trigger(WiringTrigger trigger) {

        ArrayList<WirableObjectII> outputs =
            wiringMap.get(trigger);

        if(outputs == null || outputs.isEmpty())
            return;

        for(int i = 0; i < outputs.size(); i++) {

            WirableObjectII target =
                outputs.get(i);

            if(target != null)
                target.interactByWiring();
        }
    }

    /// Adiciona uma conexão
    public boolean addWiring(
        WiringTrigger trigger,
        WirableObjectII target
    ) {

        if(trigger == null || target == null)
            return false;

        ArrayList<WirableObjectII> outputs =
            wiringMap.computeIfAbsent(
                trigger,
                k -> new ArrayList<>()
            );

        if(containsReference(outputs, target))
            return false;

        outputs.add(target);

        return true;
    }

    /// Remove uma conexão específica
    public boolean removeWiring(
        WiringTrigger trigger,
        WirableObjectII target
    ) {

        ArrayList<WirableObjectII> outputs =
            wiringMap.get(trigger);

        if(outputs == null)
            return false;

        boolean removed =
            removeReference(outputs, target);

        if(outputs.isEmpty())
            wiringMap.remove(trigger);

        return removed;
    }

    /// Remove um trigger inteiro
    public boolean removeTrigger(
        WiringTrigger trigger
    ) {

        return wiringMap.remove(trigger) != null;
    }

    /// Remove um alvo de todos os triggers
    public int removeTarget(
        WirableObjectII target
    ) {

        int removedCount = 0;

        ArrayList<WiringTrigger> emptyTriggers =
            new ArrayList<>();

        for(Map.Entry<
            WiringTrigger,
            ArrayList<WirableObjectII>
            > entry : wiringMap.entrySet()) {

            ArrayList<WirableObjectII> outputs =
                entry.getValue();

            for(
                int i = outputs.size() - 1;
                i >= 0;
                i--
            ) {

                if(outputs.get(i) == target) {

                    outputs.remove(i);

                    removedCount++;
                }
            }

            if(outputs.isEmpty())
                emptyTriggers.add(
                    entry.getKey()
                );
        }

        for(
            int i = 0;
            i < emptyTriggers.size();
            i++
        ) {
            wiringMap.remove(
                emptyTriggers.get(i)
            );
        }

        return removedCount;
    }

    private boolean containsReference(
        ArrayList<WirableObjectII> list,
        WirableObjectII target
    ) {

        for(int i = 0; i < list.size(); i++) {

            if(list.get(i) == target)
                return true;
        }

        return false;
    }

    private boolean removeReference(
        ArrayList<WirableObjectII> list,
        WirableObjectII target
    ) {

        for(int i = 0; i < list.size(); i++) {

            if(list.get(i) == target) {

                list.remove(i);

                return true;
            }
        }

        return false;
    }

    @Override
    public void dispose() {

        if(disposed)
            return;

        for(ArrayList<WirableObjectII> list
            : wiringMap.values()) {

            list.clear();
        }

        wiringMap.clear();

        wiringMap = null;

        disposed = true;
    }
}
