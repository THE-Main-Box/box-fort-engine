package official.sketchBook.engine.projectile_related.models;

import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.projectile.ProjectileControllerComponent;
import official.sketchBook.game.projectile_related.pool.ProjectilePool;
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

    /// Componente de controle, precisa ser iniciado de forma estrategica
    protected ProjectileControllerComponent controllerC;

    public BaseProjectile(
        ProjectilePool<?> ownerPool
    ) {
        reset = true;

        transformC = new TransformComponent();

        toUpdate = new ArrayList<>();
        toPostUpdate = new ArrayList<>();

        this.ownerPool = ownerPool;

    }

    /// Inicia os componentes para prepararmos para o uso da pool
    protected abstract void initComponents();

    /// Inicia o controlador do projétil
    protected abstract void initController();

    /// Inicia o projétil e o torna ativo para ser usado
    public final void startProjectile(
        float x,
        float y,
        float rotation
    ){
        if(ownerPool == null || disposed) return;

        //Marca o projétil como não sendo resetado, pois estamos iniciando o projétil aqui
        this.reset = false;

        //Fazemos o projétil entrar na pipeline de objetos ativos
        this.ownerPool.addToActive(this);

        //Executa a inicialização de cada instância
        executeProjectileStart(
            x,
            y,
            rotation
        );
    }

    /// Sequencia de ativação do projétil, de cada instancia
    protected void executeProjectileStart(
        float x,
        float y,
        float rotation
    ){
        this.transformC.x = x;
        this.transformC.y = y;
        this.transformC.rotation = rotation;
    }

    public final void update(float delta) {
        //Se o projétil estiver resetado ou disposed, não atualizamos
        if (reset || disposed) return;
        updateComponents(delta);
        executeUpdate(delta);
    }

    public final void postUpdate() {
        //Se o projétil estiver resetado ou disposed não atualizamos
        if (reset || disposed) return;
        postUpdateComponents();
        executePostUpdate();
    }

    /// Execução de atualização dentro de cada instancia
    protected abstract void executeUpdate(float delta);

    /// Execução de pós atualização dentro de cada instancia
    protected abstract void executePostUpdate();

    /// Atualiza todos os componentes
    private void updateComponents(float delta) {
        for (Component component : toUpdate) {
            component.update(delta);
        }
    }

    /// Realiza a pós atualização de todos os componentes
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
        nullifyReferences();
        disposeCriticalData();

        disposed = true;
    }

    /// realiza o dispose de dados inicial
    protected abstract void disposeGeneralData();

    /// Limpa os componentes
    protected void disposeComponents() {
        //Realiza o dispose de todos os componentes da lista de update
        for (Component component : toUpdate) {
            if (component.isDisposed()) continue;
            component.dispose();
        }

        //Realiza o dispose de todos os componentes da lista de post update
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
    protected abstract void disposeCriticalData();

    /// Limpa as referenciais
    protected void nullifyReferences() {
        moveC = null;
        transformC = null;
        ownerPool = null;
        controllerC = null;
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

    public ProjectilePool<?> getOwnerPool() {
        return ownerPool;
    }

    @Override
    public boolean isReset() {
        return reset;
    }
}
