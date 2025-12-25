package official.sketchBook.engine.dataManager_related;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.StaticResourceDisposable;
import official.sketchBook.engine.gameObject_related.BaseGameObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseWorldDataManager implements Disposable {

    protected float timeStep;
    protected int velIterations;
    protected int posIterations;

    /// Se existe um mundo foi gerado
    protected boolean physicsWorldExists;
    protected boolean disposed = false;

    /// Mundo físico para usar o box2d. Não é obrigatório
    protected World physicsWorld;

    /// Lista de gameObjects base ativos
    protected final List<BaseGameObject> gameObjectList = new ArrayList<>();
    /// Lista de gameObjects a serem adicionados
    protected final List<BaseGameObject> gameObjectToAddList = new ArrayList<>();
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

        //Tenta adicionar os objetos novos
        if (!gameObjectToAddList.isEmpty()) {
            gameObjectList.addAll(gameObjectToAddList);
            gameObjectToAddList.clear();
        }

        //Itera de cima pra baixo
        for (int i = gameObjectList.size() - 1; i >= 0; i--) {
            //Obtém uma referencia
            BaseGameObject object = gameObjectList.get(i);

            if (object.isPendingRemoval()) {                    //Se estiver pendente para remoção
                gameObjectList.remove(i);                       //Remove da lista de objetos ativos
                object.destroy();                               //Executa a pipeline contendo a sequencia de destruição
                continue;                                       //Passa pro próximo objeto
            }

            object.update(delta);                               //Atualização padrão
        }

        this.worldStep();                                       //Tenta realizar um step
        this.postUpdateGameObjects();                           //Pós-atualização manual

    }

    /// Tenta realizar um step do world caso ele exista
    public void worldStep() {
        if (!physicsWorldExists) return;

        physicsWorld.step(
            timeStep,
            velIterations,
            posIterations
        );

    }

    public void postUpdateGameObjects() {
        for (BaseGameObject gameObject : gameObjectList) {
            if (!gameObject.isPendingRemoval()) {
                gameObject.postUpdate();
            }
        }
    }

    /// Destrói o manager e todos os seus dados, não executa sequencia de destruição para os objetos presentes
    public final void destroyManager() {
        if(disposed) return;
        this.onManagerDestruction();
        this.dispose();
    }

    protected abstract void onManagerDestruction();

    /// Dispose completo do manager
    public final void dispose() {
        if(disposed) return;

        disposeGameObjectInstances();
        disposeGameObjectsStaticResourcesOnce();
        disposeLists();
        disposePhysicsWorld();

        disposed = true;
    }

    protected void disposeGameObjectInstances(){
        for (BaseGameObject gameObject : gameObjectList) {
            gameObject.dispose();
        }
    }

    /// Limpa as listas existentes
    protected void disposeLists(){
        gameObjectList.clear();
        gameObjectToAddList.clear();
        registeredClasses.clear();

    }

    /// Limpa o mundo físico
    protected void disposePhysicsWorld(){
        // Limpamos a física se ela existir
        if (physicsWorldExists) {
            physicsWorld.dispose();
            physicsWorld = null;
            physicsWorldExists = false;
        }

    }

    /**
     * Limpa recursos estáticos de forma SEGURA.
     *
     * Itera por TODAS as classes registradas (em registeredClasses),
     * não apenas as que ainda estão ativas.
     * Isso garante que mesmo classes cujos objetos foram removidos
     * tenham seus recursos estáticos limpos.
     */
    public void disposeGameObjectsStaticResourcesOnce() {
        Set<Class<? extends BaseGameObject>> cleanedClasses = new HashSet<>();

        // Usa registeredClasses (todas as classes que PASSARAM pelo manager)
        // Em vez de apenas as ativas
        for (Class<? extends BaseGameObject> clazz : registeredClasses) {

            // Pula se já foi disposado
            if (cleanedClasses.contains(clazz)) {
                continue;
            }

            // ✅ Só tenta reflection se implementar interface
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
//                e.printStackTrace();
            }
        }
    }

    public void addGameObject(BaseGameObject go) {
        gameObjectToAddList.add(go);
        registeredClasses.add(go.getClass());
    }

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

    public void setTimeStep(float timeStep) {
        this.timeStep = timeStep;
    }

    public int getVelIterations() {
        return velIterations;
    }

    public void setVelIterations(int velIterations) {
        this.velIterations = velIterations;
    }

    public int getPosIterations() {
        return posIterations;
    }

    public void setPosIterations(int posIterations) {
        this.posIterations = posIterations;
    }

    public boolean isDisposed() {
        return disposed;
    }
}
