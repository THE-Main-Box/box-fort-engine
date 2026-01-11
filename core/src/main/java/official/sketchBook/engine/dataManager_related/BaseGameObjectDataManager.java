package official.sketchBook.engine.dataManager_related;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderAbleObject;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.StaticResourceDisposable;
import official.sketchBook.engine.gameObject_related.BaseGameObject;
import official.sketchBook.engine.util_related.helper.RenderableTreeManager;

import java.lang.reflect.Method;
import java.util.*;

import static official.sketchBook.game.util_related.constants.PhysicsC.PPM;

public abstract class BaseGameObjectDataManager implements Disposable {

    /// Constante de atualização do box2d
    protected final float timeStep;
    /// Constante de iterações por velocidade do box2d
    protected final int velIterations;
    /// Constante de iterações por posição do box2d
    protected final int posIterations;

    /// Mundo físico para usar o box2d. Não é obrigatório
    protected World physicsWorld;
    /// Renderizador de debug
    protected Box2DDebugRenderer debugRenderer;
    /// Matriz de renderização para depuração
    protected Matrix4 renderDebugMatrix;
    /// Se existe um mundo foi gerado
    protected boolean physicsWorldExists;

    /// Lista de objects que precisam de rendering - DEPOIS
    protected final RenderableTreeManager renderTreeManager = new RenderableTreeManager();

    /// Lista de gameObjects base ativos
    protected final List<BaseGameObject> gameObjectList = new ArrayList<>();
    /// Lista de gameObjects a serem adicionados
    protected final List<BaseGameObject> gameObjectToAddList = new ArrayList<>();
    /// Rastreamento de todas as classes que passaram pelo manager
    protected final Set<Class<? extends BaseGameObject>> registeredClasses = new HashSet<>();

    protected boolean disposed = false;

    public BaseGameObjectDataManager(
        World physicsWorld,
        float timeStep,
        int velIterations,
        int posIterations
    ) {
        this.physicsWorld = physicsWorld;                   //Inicia um world
        this.physicsWorldExists = physicsWorld != null;     //Se temos um mundo físico podemos usar a física

        //Se o mundo físico existir
        if (physicsWorldExists) {
            //Iniciamos os objetos que irão nos auxiliar na depuração
            this.debugRenderer = new Box2DDebugRenderer();
            this.renderDebugMatrix = new Matrix4();
        }

        this.timeStep = timeStep;
        this.velIterations = velIterations;
        this.posIterations = posIterations;

        //Inicia os sistemas nativos de cada instancia
        this.setupSystems();
    }

    /// Inicia todos os sistemas nativos dos managers filho
    protected abstract void setupSystems();

    /// Atualização dos game objects
    public void update(float delta) {
        this.insertGameObjectsInSys();                          //Tenta adicionar os novos objetos
        this.updateGameObjects(delta);                          //Realiza a atualização interna dos objetos
        this.worldStep();                                       //Tenta realizar um step do mundo caso exista
        this.postUpdateGameObjects();                           //pós atualização dos objetos

    }

    /// Executa a sequencia de atualização
    protected void updateGameObjects(float delta) {
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
     * @param i      Index presente na lista
     * @param object referência do objeto, para impedir ter que obter a referencia diretamente
     */
    protected void removePendingObject(int i, BaseGameObject object) {
        gameObjectList.remove(i);                       //Remove da lista de objetos ativos

        //remove da pipeline de render caso seja renderizável e esteja marcado para remoção
        if (object instanceof RenderAbleObject) {
            renderTreeManager.remove(
                (RenderAbleObject) object
            );
        }

        object.destroy();                               //Executa a pipeline contendo a sequencia de destruição
    }

    /// Tenta inserir os objetos pendentes na lista para atualização antes de começar a atualização geral
    protected void insertGameObjectsInSys() {
        //Tenta adicionar os objetos novos
        if (!gameObjectToAddList.isEmpty()) {
            gameObjectList.addAll(gameObjectToAddList);

            //AGORA adiciona à árvore de renderização (depois de estar na gameObjectList)
            for (BaseGameObject go : gameObjectToAddList) {
                if (go instanceof RenderAbleObject) {
                    renderTreeManager.add((RenderAbleObject) go);
                }
            }

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
        //Percorremos a lista e atualizamos os objetos na nova pipeline
        for (BaseGameObject gameObject : gameObjectList) {
            //Se o objeto ficou pendente para remoção desde o ultimo loop ignoramos ele
            if (gameObject.isPendingRemoval()) continue;
            //chamamos a pós-atualização
            gameObject.postUpdate();
        }
    }

    /// Atualiza os visuais dos objetos renderizáveis
    public void updateVisuals(float delta){
        updateRenderableObjectVisuals(delta);
    }

    /// Percorre o renderManager para atualizar os visuais de cada objeto renderizável
    private void updateRenderableObjectVisuals(float delta){
        renderTreeManager.forEachForUpdate(
            obj -> obj.updateVisuals(delta)
        );
    }

    /// Executa a renderização dos objetos
    public void render(SpriteBatch batch){
        drawRenderableObjects(batch);
    }

    ///Percorre o renderManager e renderiza todos os objetos que podem ser renderizados
    private void drawRenderableObjects(SpriteBatch batch){
        renderTreeManager.forEachForRender(
            obj -> obj.render(batch)
        );
    }

    ///Executa a sequencia de destruição do manager
    public final void destroyManager() {
        if (disposed) return;
        this.onManagerDestruction();
        this.dispose();
    }

    /// Sequencia de destruição customizável por instancia
    protected abstract void onManagerDestruction();

    /// Dispose completo do manager
    public final void dispose() {
        if (disposed) return;

        //Dispose dos dados de cada instancia do manager, para evitar ter que manipular o dispose o tempo
        disposeGeneralData();
        //Dispose dos gameObjects
        disposeGameObjectInstances();
        //Dispose dos dados estáticos de todos os gameObjects que percorreram o manager
        disposeGameObjectsStaticResourcesOnce();
        //Dispose das listas usadas
        disposeLists();
        //Dispose do mundo físico caso estejamos usando ele
        disposePhysicsWorld();

        disposed = true;
    }

    protected abstract void disposeGeneralData();

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
        renderTreeManager.clear();
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
    }

    /// Usa o debugRenderer para visualizar as hitboxes
    public void renderWorldHitboxes(Camera gameCamera) {
        renderDebugMatrix.set(
            gameCamera.combined
        ).scl(PPM);

        debugRenderer.render(
            physicsWorld,
            renderDebugMatrix
        );
    }

    /// Usa a pipeline interna para marcar um objeto para ser destruido internamente
    public void removeGameObject(BaseGameObject go) {
        if (gameObjectList.contains(go)) {
            go.markToDestroy();
        }
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

    public RenderableTreeManager getRenderTreeManager() {
        return renderTreeManager;
    }

    public boolean isDisposed() {
        return disposed;
    }
}
