package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;

import java.util.ArrayList;
import java.util.List;

public class ComponentManagerComponent implements Component {

    private final List<Component>
        toUpdate,
        toPostUpdate,
        allComponents;     // refer�ncia de todos os componentes, para garantir dispose correto

    private boolean
        disposed = false;

    public ComponentManagerComponent() {
        toUpdate = new ArrayList<>();
        toPostUpdate = new ArrayList<>();
        allComponents = new ArrayList<>();
    }

    public void update(float delta) {
        for (int i = 0; i < toUpdate.size(); i++) {
            toUpdate.get(i).update(delta);
        }
    }

    public void postUpdate() {
        for (int i = 0; i < toPostUpdate.size(); i++) {
            toPostUpdate.get(i).postUpdate();
        }
    }

    @Override
    public void initObject() {
    }

    public <T extends Component> void remove(
        Class<T> type,
        boolean removeFromUpdateList,
        boolean removeFromPostUpdateList,
        boolean autoDispose
    ) {
        if (removeFromUpdateList) {
            for (int i = toUpdate.size() - 1; i >= 0; i--) {
                Component c = toUpdate.get(i);
                if (type.isInstance(c)) {
                    toUpdate.remove(i);
                }
            }
        }

        if (removeFromPostUpdateList) {
            for (int i = toPostUpdate.size() - 1; i >= 0; i--) {
                Component c = toPostUpdate.get(i);
                if (type.isInstance(c)) {
                    toPostUpdate.remove(i);
                }
            }
        }

        for (int i = allComponents.size() - 1; i >= 0; i--) {
            Component c = allComponents.get(i);
            if (type.isInstance(c)) {
                if (autoDispose) c.dispose();
                allComponents.remove(i);
            }
        }
    }

    public void add(
        Component component,
        boolean toUpdate,
        boolean toPostUpdate
    ) {
        allComponents.add(component);

        if (toUpdate) {
            this.toUpdate.add(component);
        }

        if (toPostUpdate) {
            this.toPostUpdate.add(component);
        }
    }

    public void dispose() {
        if (disposed) return;

        for (int i = 0; i < allComponents.size(); i++) {
            allComponents.get(i).dispose();
        }

        toUpdate.clear();
        toPostUpdate.clear();
        allComponents.clear();

        disposed = true;
    }
}
