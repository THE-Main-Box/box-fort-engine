package official.sketchBook.engine.projectile_related.models;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.Disposable;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.projectile.ProjectileControllerComponent;
import official.sketchBook.engine.components_related.system_utils.ComponentManagerComponent;
import official.sketchBook.game.projectile_related.pool.ProjectilePool;
import official.sketchBook.engine.util_related.custom_utils.CustomPool;

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
    /// Componente de controle do projétil
    protected ProjectileControllerComponent controllerC;
    /// Lista de componentes que precisam ser atualizados e disposed
    protected ComponentManagerComponent managerC;

    /// Flags de estado relacionado a pool
    protected boolean
        reset,
        disposed;

    public BaseProjectile(
        ProjectilePool<?> ownerPool
    ) {
        //Colocamos como reset pois ainda não iniciamos nada
        this.reset = true;

        this.transformC = new TransformComponent();
        this.managerC = new ComponentManagerComponent();

        this.ownerPool = ownerPool;

    }

    /// Inicia os componentes para prepararmos para o uso da pool
    protected abstract void initComponents();

    /// Inicia o controlador do projétil
    protected abstract void initController();

    /// Inicia o projétil e o torna ativo para ser usado
    public final void activate(
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
        executeProjectileActivation(
            x,
            y,
            rotation
        );

        controllerC.start();
    }

    /// Sequencia de ativação do projétil, de cada instancia
    protected void executeProjectileActivation(
        float x,
        float y,
        float rotation
    ){
        this.transformC.x = x;
        this.transformC.y = y;
        this.transformC.rotation = rotation;
    }

    /// Chama os métodos de disparo do controlador
    public void launch(){
        controllerC.launch();
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
        managerC.update(delta);
    }

    /// Realiza a pós atualização de todos os componentes
    private void postUpdateComponents() {
        managerC.postUpdate();
    }

    public abstract void onCollisionDetection();
    public abstract void onEndCollisionDetection();

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
        controllerC.reset();
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
        nullifyReferences();
        disposeCriticalData();

        disposed = true;
    }

    /// realiza o dispose de dados inicial
    protected abstract void disposeGeneralData();

    /// Limpa os componentes
    protected void disposeComponents() {
        managerC.dispose();
    }

    /// Limpa as referenciais
    protected void nullifyReferences() {
        moveC = null;
        transformC = null;
        managerC = null;
        ownerPool = null;
        controllerC = null;
    }

    /// Realiza o dispose final de dados, geralmente aqueles que precisam ser limpos por último
    protected abstract void disposeCriticalData();

    public boolean isDisposed() {
        return disposed;
    }

    public MovementComponent getMoveC() {
        return moveC;
    }

    public TransformComponent getTransformC() {
        return transformC;
    }

    public ComponentManagerComponent getManagerC() {
        return managerC;
    }

    public ProjectileControllerComponent getControllerC() {
        return controllerC;
    }

    public ProjectilePool<?> getOwnerPool() {
        return ownerPool;
    }

    @Override
    public boolean isReset() {
        return reset;
    }
}
