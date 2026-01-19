package official.sketchBook.engine.projectile_related;

import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.compression.lzma.Base;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.util_related.custom_utils.CustomPool;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseProjectile
    implements
    MovableObjectII,
    CustomPool.Poolable,
    Disposable {

    protected ProjectilePool<?> ownerPool;

    /// Componente de transformação, me permite iterar por dimensões e por unidades de coordenadas
    protected TransformComponent transformC;

    /// Componente de movimentação, me permite lidar com movimentos e outras coisas
    protected MovementComponent moveC;

    /// Lista de componentes que precisam ser atualizados e disposed
    protected List<Component> componentList;

    /// Flags de estado relacionado a pool
    protected boolean
        reset,
        disposed;

    public BaseProjectile(
        ProjectilePool<?> ownerPool
    ) {
        reset = true;

        componentList = new ArrayList<>();

        this.ownerPool = ownerPool;
    }

    public void update(float delta){
        if(reset || disposed) return;
        updateComponents(delta);
    }

    public void postUpdate(){
        if(reset || disposed) return;
        postUpdateComponents();
    }

    private void updateComponents(float delta){
        for(Component component : componentList){
            component.update(delta);
        }
    }

    private void postUpdateComponents(){
        for(Component component : componentList){
            component.postUpdate();
        }
    }

    /// Sequencia de destruição de um projétil
    @Override
    public void destroy() {
        executeProjectileDestruction();

        this.reset();
        this.dispose();
    }

    /// Sequencia de destruição antes do reset e dispose dos dados
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
        ownerPool = null;
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

    @Override
    public boolean isReset() {
        return reset;
    }
}
