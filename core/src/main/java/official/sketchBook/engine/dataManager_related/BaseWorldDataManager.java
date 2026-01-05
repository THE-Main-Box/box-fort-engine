package official.sketchBook.engine.dataManager_related;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderAbleObject;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.StaticResourceDisposable;
import official.sketchBook.engine.gameObject_related.BaseGameObject;

import java.lang.reflect.Method;
import java.util.*;

public abstract class BaseWorldDataManager implements Disposable {

    protected final float timeStep;
    protected final int velIterations;
    protected final int posIterations;

    /// Se existe um mundo foi gerado
    protected boolean physicsWorldExists;
    /// Se o manager foi limpo
    protected boolean disposed = false;
    /// Se as camadas de rendering precisam ter suas ordens atualizadas
    protected boolean renderingNeedsSorting = false;

    /// Mundo físico para usar o box2d. Não é obrigatório
    protected World physicsWorld;

    /// Lista de gameObjects base ativos
    protected final List<BaseGameObject> gameObjectList = new ArrayList<>();
    /// Lista de gameObjects a serem adicionados
    protected final List<BaseGameObject> gameObjectToAddList = new ArrayList<>();

    /// Lista de objects que precisam de rendering
    protected final List<RenderAbleObject> renderAbleObjectList = new ArrayList<>();

    /// Rastreamento de todas as classes que passaram pelo manager
    protected final Set<Class<? extends BaseGameObject>> registeredClasses = new HashSet<>();

    public BaseWorldDataManager(
        World physicsWorld,
        float timeStep,
        int velIterations,
        int posIterations
    ) {
        this.physicsWorld = physicsWorld;                   //Inicia um world
        this.physicsWorldExists = physicsWorld != null;     //Se temos um mundo físico podemos usar a física

        this.timeStep = timeStep;
        this.velIterations = velIterations;
        this.posIterations = posIterations;

        this.setupSystems();                                //Inicia os sistemas nativos
    }

    /// Inicia todos os sistemas nativos dos managers filho
    protected abstract void setupSystems();

    /// Atualização do manager
    public void update(float delta) {
        this.addNewObjectsToPipeLine();                         //Tenta adicionar os novos objetos
        this.updateGameObjectsOnOriginalPipeLine(delta);        //Realiza a atualização interna dos objetos
        this.worldStep();                                       //Tenta realizar um step
        this.postUpdateGameObjects();                           //Pós-atualização manual

    }

    /// Executa a sequencia de atualização
    protected void updateGameObjectsOnOriginalPipeLine(float delta){
        //Itera de cima pra baixo
        for (int i = gameObjectList.size() - 1; i >= 0; i--) {
            //Obtém uma referencia
            BaseGameObject object = gameObjectList.get(i);

            //Se estiver pendente para remoção
            if (object.isPendingRemoval()) {
                removePendingObject(i, object);                 //Executa a remoção da pipeline
                continue;                                       //Passa pro próximo objeto
            }

            object.update(delta);                               //Atualização padrão
        }
    }

    /**
     * Executa a remoção do objeto pendente para remoção
     *
     * @param i Index presente na lista
     * @param object referência do objeto, para impedir ter que obter a referencia diretamente
     */
    protected void removePendingObject(int i, BaseGameObject object) {
        gameObjectList.remove(i);                       //Remove da lista de objetos ativos

        //remove da pipeline de render caso seja renderizável e esteja marcado para remoção
        if (object instanceof RenderAbleObject) {
            renderAbleObjectList.remove(
                (RenderAbleObject) object
            );
        }

        object.destroy();                               //Executa a pipeline contendo a sequencia de destruição
    }

    /// Tenta inserir os objetos pendentes na lista para atualização antes de começar a atualização geral
    protected void addNewObjectsToPipeLine() {
        //Tenta adicionar os objetos novos
        if (!gameObjectToAddList.isEmpty()) {
            gameObjectList.addAll(gameObjectToAddList);
            gameObjectToAddList.clear();
        }
    }

    /// Tenta realizar um step do world caso ele exista
    protected void worldStep() {
        if (!physicsWorldExists) return;

        physicsWorld.step(
            timeStep,
            velIterations,
            posIterations
        );

    }

    /// Atualização tardia dos objetos, geralmente aqueles que precisam ter dados atualizados após o step do mundo
    protected void postUpdateGameObjects() {
        for (BaseGameObject gameObject : gameObjectList) {
            if (gameObject.isPendingRemoval()) continue;
            gameObject.postUpdate();
        }
    }

    /// Destrói o manager e todos os seus dados, não executa sequencia de destruição para os objetos presentes
    public final void destroyManager() {
        if (disposed) return;
        this.onManagerDestruction();
        this.dispose();
    }

    /// Sequencia por instancia de destruição de manager
    protected abstract void onManagerDestruction();

    /// Dispose completo do manager
    public final void dispose() {
        if (disposed) return;

        disposeGameObjectInstances();
        disposeGameObjectsStaticResourcesOnce();
        disposeLists();
        disposePhysicsWorld();

        disposed = true;
    }

    /// Realiza um dispose dos dados pro instancia dos GameObjects existentes dentro do manager
    protected void disposeGameObjectInstances() {
        for (BaseGameObject gameObject : gameObjectList) {
            gameObject.dispose();
        }
    }

    /// Limpa as listas existentes
    protected void disposeLists() {
        gameObjectList.clear();
        gameObjectToAddList.clear();
        registeredClasses.clear();
        renderAbleObjectList.clear();
    }

    /// Limpa o mundo físico
    protected void disposePhysicsWorld() {
        // Limpamos a física se ela existir
        if (physicsWorldExists) {
            physicsWorld.dispose();
            physicsWorld = null;
            physicsWorldExists = false;
        }

    }

    /**
     * Limpa recursos estáticos de forma SEGURA.
     * <p>
     * Itera por TODAS as classes registradas (em registeredClasses),
     * não apenas as que ainda estão ativas.
     * Isso garante que mesmo classes cujos objetos foram removidos
     * tenham seus recursos estáticos limpos.
     */
    protected final void disposeGameObjectsStaticResourcesOnce() {
        Set<Class<? extends BaseGameObject>> cleanedClasses = new HashSet<>();

        // Usa registeredClasses (todas as classes que PASSARAM pelo manager)
        // Em vez de apenas as ativas
        for (Class<? extends BaseGameObject> clazz : registeredClasses) {

            // Pula se já foi disposado
            if (cleanedClasses.contains(clazz)) {
                continue;
            }

            // Só tenta reflection se implementar interface
            if (!StaticResourceDisposable.class.isAssignableFrom(clazz)) {
                continue;
            }

            try {
                Method method = clazz.getMethod("disposeStaticResources");
                method.invoke(null);
                cleanedClasses.add(clazz);
            } catch (NoSuchMethodException e) {
                System.err.println("ERRO: Classe " + clazz.getSimpleName() +
                    " implementa StaticResourceDisposable mas não tem disposeStaticResources()");
            } catch (Exception e) {
                System.err.println("Erro ao disposar recursos estáticos de " + clazz.getSimpleName());
            }
        }
    }

    /// Adiciona um gameObject para ser gerenciado
    public void addGameObject(BaseGameObject go) {
        //Prepara para inserir na pipeline
        gameObjectToAddList.add(go);
        //Registra a classe para permitir a limpeza de dados estaticos futuramente
        registeredClasses.add(go.getClass());

        //Verificamos se o objeto pode ser renderizado e inserimos ele na pipeline de render
        if (go instanceof RenderAbleObject) {
            renderAbleObjectList.add((RenderAbleObject) go);    //Adicionamos à pipeline
            notifyRenderIndexUpdate();                          //Notificamos a necessidade de ordenar a lista
        }
    }

    /// Realiza a ordenação dos objetos que serão mostrados na tela
    public void sortRenderables() {
        if (renderingNeedsSorting) {
            // Ordenação estável para não tremer objetos no mesmo Z
            renderAbleObjectList.sort(
                Comparator.comparingInt(RenderAbleObject::getRenderIndex)
            );
            renderingNeedsSorting = false;
        }
    }

    /// Usa a pipeline interna para marcar um objeto para ser destruido internamente
    public void removeGameObject(BaseGameObject go) {
        if (gameObjectList.contains(go)) {
            go.markToDestroy();
        }
    }

    public void notifyRenderIndexUpdate() {
        this.renderingNeedsSorting = true;
    }

    public List<RenderAbleObject> getRenderAbleObjectList() {
        return renderAbleObjectList;
    }

    public boolean isPhysicsWorldExists() {
        return physicsWorldExists;
    }

    public World getPhysicsWorld() {
        return physicsWorld;
    }

    public List<BaseGameObject> getGameObjectList() {
        return gameObjectList;
    }

    public float getTimeStep() {
        return timeStep;
    }

    public int getVelIterations() {
        return velIterations;
    }

    public int getPosIterations() {
        return posIterations;
    }

    public boolean isDisposed() {
        return disposed;
    }
}
