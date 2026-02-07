package official.sketchBook.engine.dataManager_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.Disposable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderAbleObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.StaticResourceDisposable;
import official.sketchBook.engine.dataManager_related.util.RenderableObjectManager;
import official.sketchBook.engine.gameObject_related.BaseGameObject;

import java.lang.reflect.Method;
import java.util.*;

import static official.sketchBook.engine.dataManager_related.util.RenderableObjectManager.tryAddToRender;
import static official.sketchBook.engine.dataManager_related.util.RenderableObjectManager.tryRemoveFromRender;

public abstract class BaseGameObjectDataManager implements Disposable {

    /// Lista de objects que precisam de rendering
    protected final RenderableObjectManager renderTree = new RenderableObjectManager();

    /// Lista de gameObjects base ativos
    protected final List<BaseGameObject> gameObjectList = new ArrayList<>();
    /// Lista de gameObjects a serem adicionados
    protected final List<BaseGameObject> gameObjectToAddList = new ArrayList<>();
    /// Rastreamento de todas as classes que passaram pelo manager
    protected final Set<Class<? extends BaseGameObject>> registeredClasses = new HashSet<>();

    /// Flags de identificação de limpeza
    protected boolean
        disposed = false,
        graphicsDisposed = false;

    /// Inicia todos os sistemas nativos dos managers filho
    protected abstract void setupSystems();

    /// Atualização dos game objects
    public void update(float delta) {
        this.insertGameObjectsInSys();                          //Tenta adicionar os novos objetos
        this.updateGameObjects(delta);                          //Realiza a atualização interna dos objetos
    }

    public void postUpdate() {
        this.postUpdateGameObjects();                           //pós atualização dos objetos
    }

    /// Executa a sequencia de atualização
    protected void updateGameObjects(float delta) {
        BaseGameObject currentObject;

        //Itera de cima pra baixo
        for (int i = gameObjectList.size() - 1; i >= 0; i--) {
            //Obtém uma referencia
            currentObject = gameObjectList.get(i);

            //Se estiver pendente para remoção
            if (currentObject.isPendingRemoval()) {
                removePendingObject(i, currentObject);          //Executa a remoção da pipeline
                continue;                                       //Passa pro próximo objeto
            }

            currentObject.update(delta);                        //Atualização padrão
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
        if (object instanceof RenderAbleObjectII) {
            renderTree.remove(
                (RenderAbleObjectII) object
            );
        }

        tryRemoveFromRender(
            renderTree,
            object
        );

        object.destroy();       //Executa a pipeline contendo a sequencia de destruição
    }

    /// Tenta inserir os objetos pendentes na lista para atualização antes de começar a atualização geral
    protected void insertGameObjectsInSys() {
        if (gameObjectToAddList.isEmpty()) return;

        BaseGameObject currentObject;
        for (int i = 0; i < gameObjectToAddList.size(); i++) {
            currentObject = gameObjectToAddList.get(i);

            gameObjectList.add(currentObject);

            tryAddToRender(
                renderTree,
                currentObject
            );
        }

        gameObjectToAddList.clear();

    }

    /// Atualização tardia dos objetos, geralmente aqueles que precisam ter dados atualizados após o step do mundo
    protected void postUpdateGameObjects() {
        BaseGameObject currentObject;
        for (int i = 0; i < gameObjectList.size(); i++) {
            currentObject = gameObjectList.get(i);

            if (currentObject.isPendingRemoval()) continue;
            currentObject.postUpdate();
        }
    }

    /// Atualiza os visuais dos objetos renderizáveis
    public void updateVisuals(float delta) {
        updateRenderableObjectVisuals(delta);
    }

    /// Percorre o renderManager para atualizar os visuais de cada objeto renderizável
    protected void updateRenderableObjectVisuals(float delta) {
        renderTree.forEachObject(
            obj -> obj.updateVisuals(delta)
        );
    }

    /// Executa a renderização dos objetos
    public void render(SpriteBatch batch) {
        drawRenderableObjects(batch);
    }

    /// Percorre o renderManager e renderiza todos os objetos que podem ser renderizados
    protected void drawRenderableObjects(SpriteBatch batch) {
        renderTree.forEachObject(
            obj -> obj.render(batch)
        );
    }

    /// Executa a sequencia de destruição do manager
    public final void destroyManager() {
        if (disposed) return;
        this.onManagerDestruction();

        this.dispose();
        this.disposeGraphics();
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
        //Dispose de dados finais
        disposeCriticalData();

        disposed = true;
    }

    /// Limpa os dados gerais que podem ser limpos no começo da limpeza
    protected abstract void disposeGeneralData();

    /// Limpa os dados críticos que podem ser limpos apenas no final de tudo
    protected abstract void disposeCriticalData();

    /// Realiza um dispose dos dados pro instancia dos GameObjects existentes dentro do manager
    protected void disposeGameObjectInstances() {
        for (int i = 0; i < gameObjectList.size(); i++) {
            gameObjectList.get(i).dispose();
        }
    }

    /// Limpa as listas existentes
    protected void disposeLists() {
        gameObjectList.clear();
        gameObjectToAddList.clear();
        registeredClasses.clear();
    }

    /// Dispose dos gráficos
    public final void disposeGraphics() {
        if (graphicsDisposed) return;
        //Realiza uma limpeza de dados gráficos gerais,
        // como as textures dos objetos renderizaveis e outros que precisam ser feitos em thread-safe
        disposeGeneralGraphics();
        //Dispose crítico que precisa ser feito por último
        disposeCriticalGraphics();

        graphicsDisposed = true;
    }

    /// Dispose geral dos gráficos
    protected void disposeGeneralGraphics() {
        renderTree.dispose();
    }

    protected void disposeCriticalGraphics() {
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

    /// Usa a pipeline interna para marcar um objeto para ser destruido internamente
    public void removeGameObject(BaseGameObject go) {
        if (gameObjectList.contains(go)) {
            go.markToDestroy();
        }
    }

    public List<BaseGameObject> getGameObjectList() {
        return gameObjectList;
    }

    public RenderableObjectManager getRenderTree() {
        return renderTree;
    }

    public boolean isDisposed() {
        return disposed;
    }
}
