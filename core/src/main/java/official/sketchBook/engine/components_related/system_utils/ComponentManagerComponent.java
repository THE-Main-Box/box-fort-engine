package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;

import java.util.ArrayList;
import java.util.List;

public class ComponentManagerComponent implements Component{

    private final List<Component>
        toUpdate,
        toPostUpdate;

    private boolean disposed = false;

    public ComponentManagerComponent() {
        toUpdate = new ArrayList<>();
        toPostUpdate = new ArrayList<>();
    }

    public void update(float delta) {
        // Loop simples sem iterador
        for (int i = 0; i < toUpdate.size(); i++) {
            toUpdate.get(i).update(delta);
        }
    }
    public void postUpdate(){
        for (int i = 0; i < toUpdate.size(); i++) {
            toUpdate.get(i).postUpdate();
        }
    }

    public <T extends Component> void remove(
        Class<T> type,
        boolean removeFromUpdateList,
        boolean removeFromPostUpdateList,
        boolean autoDispose
    ){
        if (removeFromUpdateList) {
            for (int i = toUpdate.size() - 1; i >= 0; i--) {
                Component c = toUpdate.get(i);
                if (type.isInstance(c)) {
                    if (!c.isDisposed() && autoDispose) c.dispose();
                    toUpdate.remove(i);
                }
            }
        }

        if (removeFromPostUpdateList) {
            for (int i = toPostUpdate.size() - 1; i >= 0; i--) {
                Component c = toPostUpdate.get(i);
                if (type.isInstance(c)) {
                    if (!c.isDisposed() && autoDispose) c.dispose();
                    toPostUpdate.remove(i);
                }
            }
        }
    }

    public void add(
        Component component,
        boolean toUpdate,
        boolean toPostUpdate
    ) {
        if (toUpdate) {
            this.toUpdate.add(component);
        }
        if (toPostUpdate) {
            this.toPostUpdate.add(component);
        }
    }

    public void dispose(){
        if(disposed) return;

        for (Component component : toUpdate) {
            if (component.isDisposed()) continue;
            component.dispose();
        }

        for (Component component : toPostUpdate) {
            if (component.isDisposed()) continue;
            component.dispose();
        }

        toUpdate.clear();
        toPostUpdate.clear();

        disposed = true;
    }

    @Override
    public void nullifyReferences() {

    }


    public boolean isDisposed() {
        return disposed;
    }

}
