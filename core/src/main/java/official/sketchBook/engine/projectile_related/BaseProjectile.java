package official.sketchBook.engine.projectile_related;

import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.util_related.custom_utils.CustomPool;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseProjectile
    implements
    CustomPool.Poolable,
    MovableObjectII,
    Disposable {

    protected ProjectilePool<?> ownerPool;

    /// Componente de transformação, me permite iterar por dimensões e por unidades de coordenadas
    protected TransformComponent transformC;

    /// Componente de movimentação, me permite lidar com movimentos e outras coisas
    protected MovementComponent moveC;

    /// Lista de componentes que precisam ser atualizados e disposed
    protected List<Component> toUpdate;
    protected List<Component> toPostUpdate;

    /// Flags de estado relacionado a pool
    protected boolean
        reset,
        disposed;

    public BaseProjectile(
        ProjectilePool<?> ownerPool
    ) {
        reset = true;

        transformC = new TransformComponent();

        toUpdate = new ArrayList<>();
        toPostUpdate = new ArrayList<>();

        this.ownerPool = ownerPool;

    }

    /// Inicia todos os componentes, preparando para usar dentro da pool
    protected abstract void initComponents();

    public final void update(float delta) {
        if (reset || disposed) return;
        updateComponents(delta);
        executeUpdate(delta);
    }

    public final void postUpdate() {
        if (reset || disposed) return;
        postUpdateComponents();
        executePostUpdate();
    }

    /// Execução de atualização dentro de cada instancia
    protected abstract void executeUpdate(float delta);

    /// Execução de pós atualização dentro de cada instancia
    protected abstract void executePostUpdate();

    private void updateComponents(float delta) {
        for (Component component : toUpdate) {
            component.update(delta);
        }
    }

    private void postUpdateComponents() {
        for (Component component : toPostUpdate) {
            component.postUpdate();
        }
    }

    /// Sequencia de destruição de um projétil
    @Override
    public final void destroy() {
        executeProjectileDestruction();

        this.reset();
        this.dispose();
    }

    /// Sequencia de destruição antes do reset e dispose dos dados
    protected abstract void executeProjectileDestruction();

    @Override
    public final void reset() {
        //Se já tivermos realizado um reset não resetamos novamente
        //Também evitamos de prosseguir caso já tenhamos realizado um dispose dos dados
        if (reset || disposed) return;
        executeReset();
        reset = true;
    }

    /// Execução de reset dentro de cada instancia
    protected abstract void executeReset();

    public final void dispose() {
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
        for (Component component : toUpdate) {
            if (component.isDisposed()) continue;
            component.dispose();
        }

        for (Component component : toPostUpdate) {
            if (component.isDisposed()) continue;
            component.dispose();
        }
    }

    /// Limpa as listas
    protected void cleanLists() {
        toPostUpdate.clear();
        toUpdate.clear();
    }

    /// Realiza o dispose final de dados, geralmente aqueles que precisam ser limpos por último
    protected void disposeCriticalData() {
        nullifyReferences();
    }

    /// Limpa as referenciais
    protected void nullifyReferences() {
        moveC = null;
        transformC = null;
        ownerPool = null;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public MovementComponent getMoveC() {
        return moveC;
    }

    public TransformComponent getTransformC() {
        return transformC;
    }

    @Override
    public boolean isReset() {
        return reset;
    }
}
