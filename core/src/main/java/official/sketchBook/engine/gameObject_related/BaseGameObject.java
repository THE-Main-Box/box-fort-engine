package official.sketchBook.engine.gameObject_related;

import com.badlogic.gdx.utils.Disposable;
import jdk.jfr.internal.Utils;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseGameObject implements Disposable {

    /// Se deve eliminar por completo
    protected boolean pendingRemoval = false;
    protected boolean disposed = false;

    /// Manager dono do objeto, onde ele será gerenciado
    protected final BaseGameObjectDataManager worldDataManager;

    /// Lista de componentes que precisam ser atualizados normalmente
    protected List<Component> toUpdateComponentList;
    /// Lista de componentes que precisam ser atualizados após o update
    protected List<Component> toPostUpdateComponentList;

    public BaseGameObject(BaseGameObjectDataManager worldDataManager) {
        this.worldDataManager = worldDataManager;
        this.worldDataManager.addGameObject(this);

        this.toUpdateComponentList = new ArrayList<>();
        this.toPostUpdateComponentList = new ArrayList<>();
    }

    /// Inicia os dados importantes antes de alocar ele no mundo
    protected abstract void initObject();

    /// Atualização manual
    public abstract void update(float delta);

    /// Pós-atualização manual
    public abstract void postUpdate();

    protected void updateComponents(float delta) {
        for (Component component : toUpdateComponentList) {
            component.update(delta);
        }
    }

    protected void postUpdateComponents() {
        for (Component component : toPostUpdateComponentList) {
            component.postUpdate();
        }
    }

    /// Sequência de destruição de objeto
    public final void destroy() {
        if (disposed) return;           //se já limpamos não podemos prosseguir nessa sequencia de eventos
        this.onObjectDestruction();     //Código personalizado antes de eliminarmos o objeto
        this.dispose();                 //Limpeza de recursos
    }

    /// Callback para lógica customizada de destruição
    protected abstract void onObjectDestruction();

    /// Dispose dos dados
    public final void dispose() {
        if (disposed) return;
        disposeAllComponents();
        disposeData();
        disposed = true;
    }

    /// Realiza um dispose de todos os componentes
    protected void disposeAllComponents() {
        for (Component component : toUpdateComponentList) {
            if (component.isDisposed()) continue;
            component.dispose();
        }
        for (Component component : toPostUpdateComponentList) {
            if (component.isDisposed()) continue;
            component.dispose();
        }

        toPostUpdateComponentList.clear();
        toUpdateComponentList.clear();
    }

    /// Pipeline interna para o dispose
    protected abstract void disposeData();

    public void markToDestroy() {
        this.pendingRemoval = true;
    }

    public boolean isPendingRemoval() {
        return pendingRemoval;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public static <T extends Component> void removeComponentByType(
        BaseGameObject object,
        Class<T> type,
        boolean removeFromUpdateList,
        boolean removeFromPostUpdateList,
        boolean autoDispose
    ) {
        if (object == null || type == null) return;

        if (removeFromUpdateList) {
            List<Component> list = object.toUpdateComponentList;
            for (int i = list.size() - 1; i >= 0; i--) {
                Component c = list.get(i);
                if (type.isInstance(c)) {
                    if (!c.isDisposed() && autoDispose) c.dispose();
                    list.remove(i);
                }
            }
        }

        if (removeFromPostUpdateList) {
            List<Component> list = object.toPostUpdateComponentList;
            for (int i = list.size() - 1; i >= 0; i--) {
                Component c = list.get(i);
                if (type.isInstance(c)) {
                    if (!c.isDisposed() && autoDispose) c.dispose();
                    list.remove(i);
                }
            }
        }
    }

}
