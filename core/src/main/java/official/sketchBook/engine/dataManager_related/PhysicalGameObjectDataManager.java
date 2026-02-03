package official.sketchBook.engine.dataManager_related;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.util_related.contact_listener.MultiContactListener;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public abstract class PhysicalGameObjectDataManager extends BaseGameObjectDataManager{

    /// Constante de iterações por velocidade do box2d
    protected final int velIterations;
    /// Constante de iterações por posição do box2d
    protected final int posIterations;

    /// Mundo físico para usar o box2d. Não é obrigatório
    protected World physicsWorld;

    /// Listeners existentes no manager
    protected MultiContactListener contactListeners;

    /// Renderizador de debug
    protected Box2DDebugRenderer debugRenderer;
    /// Matriz de renderização para depuração
    protected Matrix4 renderDebugMatrix;
    /// Se existe um mundo foi gerado
    protected boolean physicsWorldExists;

    public PhysicalGameObjectDataManager(World physicsWorld, int velIterations, int posIterations) {
        this.physicsWorld = physicsWorld;                   //Inicia um world
        this.physicsWorldExists = physicsWorld != null;     //Se temos um mundo físico podemos usar a física

        //Se o mundo físico existir
        if (physicsWorldExists) {

            //Iniciamos o contactListener
            this.contactListeners = new MultiContactListener();
            this.physicsWorld.setContactListener(contactListeners);

            //Iniciamos os objetos que irão nos auxiliar na depuração
            this.debugRenderer = new Box2DDebugRenderer();
            this.renderDebugMatrix = new Matrix4();
        }

        this.velIterations = velIterations;
        this.posIterations = posIterations;

        this.setupSystems();
    }

    @Override
    protected void setupSystems() {
        setupContactListeners();
    }

    protected abstract void setupContactListeners();

    @Override
    public void update(float delta) {
        super.update(delta);
        worldStep(delta);
    }

    /// Tenta realizar um step do world caso ele exista
    protected void worldStep(float delta) {
        if (!physicsWorldExists) return;

        physicsWorld.step(
            delta,
            velIterations,
            posIterations
        );

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

    @Override
    protected void onManagerDestruction() {

    }

    @Override
    protected void disposeGeneralData() {
        if(physicsWorldExists){
            debugRenderer.dispose();
            debugRenderer = null;
        }
    }

    @Override
    protected void disposeCriticalData() {
        disposePhysicsWorld();
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


    public boolean isPhysicsWorldExists() {
        return physicsWorldExists;
    }

    public World getPhysicsWorld() {
        return physicsWorld;
    }

    public MultiContactListener getContactListeners() {
        return contactListeners;
    }

    public int getVelIterations() {
        return velIterations;
    }

    public int getPosIterations() {
        return posIterations;
    }
}
