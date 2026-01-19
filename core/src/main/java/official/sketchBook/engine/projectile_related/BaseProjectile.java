package official.sketchBook.engine.projectile_related;

import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.util_related.custom_utils.CustomPool;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseProjectile implements MovableObjectII, CustomPool.Poolable, Disposable {

    /// Componente de transformação, me permite iterar por dimensões e por unidades de coordenadas
    protected TransformComponent transformC;

    /// Componente de movimentação, me permite lidar com movimentos e outras coisas
    protected MovementComponent moveC;

    protected List<Component> componentList;

    protected boolean
        active,
        reset,
        disposed;

    public BaseProjectile() {
        active = false;
        reset = true;

        componentList = new ArrayList<>();
    }

    @Override
    public void destroy() {
        //Executamos a sequencia de destruição antes da destruição em si
        executeProjectileDestruction();

        if (!reset) {
            this.reset();
        }
        this.dispose();
    }

    protected abstract void executeProjectileDestruction();

    @Override
    public void reset() {
        //Se já tivermos realizado um reset não resetamos novamente
        //Também evitamos de prosseguir caso já tenhamos realizado um dispose dos dados
        if (reset || disposed) return;
        executeReset();
        reset = true;
    }

    protected abstract void executeReset();

    public void dispose() {
        //Se já tivermos realizado um disposed ou ainda não tivermos resetado os dados não podemos realizar o dispose
        if (disposed || !reset) return;

        disposeGeneralData();
        disposeComponents();
        cleanLists();
        disposeCriticalData();

        disposed = true;
    }

    /// realiza o dispose de dados inicial
    protected abstract void disposeGeneralData();

    /// Limpa os componentes
    protected void disposeComponents() {
        for (Component component : componentList) {
            if (component.isDisposed()) continue;
            component.dispose();
        }
    }

    /// Limpa as listas
    protected void cleanLists() {
        componentList.clear();
    }

    /// Realiza o dispose final de dados, geralmente aqueles que precisam ser limpos por último
    protected void disposeCriticalData() {
        nullifyReferences();
    }

    /// Limpa as referenciais
    protected void nullifyReferences() {
        moveC = null;
        transformC = null;
    }

    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public MovementComponent getMoveC() {
        return moveC;
    }

    @Override
    public TransformComponent getTransformC() {
        return transformC;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isReset() {
        return reset;
    }
}
