package official.sketchBook.game.projectile_related.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.components_related.movement.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.projectile.ProjectileControllerComponent;
import official.sketchBook.engine.projectile_related.models.PhysicalProjectile;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.game.projectile_related.pool.ProjectilePool;
import official.sketchBook.game.util_related.constants.WorldConstants;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.ALL;
import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.PROJECTILES;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public class Bullet extends PhysicalProjectile {

    public Bullet(ProjectilePool<?> ownerPool, World world) {
        super(ownerPool, world);

        initComponents();
    }

    @Override
    protected void initComponents() {
        this.reset = true;
        /*Iniciamos o componente de movimento, ele lida com a movimentação do projétil,
         * então precisa existir antes do sistema de física*/
        initMovementComponent();
        /*Iniciamos o controller, ele que irá determinar algumas informações extremamente importantes,
         *então ele precisa ter prioridade na atualização*/
        initController();
        /*Iniciamos o pseudoComponente de transform, ele não é um compoentne de fato,
         *mas armazena dados de coordenadas e dimensão,
         *importante e precisa ser um dos primeiros a ser criado*/
        initTransformC();

        /*Iniciamos o componetne de física, ele que aplica tudo na body*/
        initPhysicsComponent();
    }

    @Override
    protected void initController() {
        this.controllerC = new ProjectileControllerComponent(this);

        managerC.add(
            controllerC,
            true,
            true
        );
    }

    private void initMovementComponent() {
        this.moveC = new MovementComponent(
            this,
            WorldConstants.ProjectileConstants.PROJECTILE_MAX_SPEED_X,
            WorldConstants.ProjectileConstants.PROJECTILE_MAX_SPEED_Y,
            WorldConstants.ProjectileConstants.PROJECTILE_MAX_SPEED_R,
            WorldConstants.ProjectileConstants.PROJECTILE_DECELERATION_X,
            WorldConstants.ProjectileConstants.PROJECTILE_DECELERATION_Y,
            WorldConstants.ProjectileConstants.PROJECTILE_DECELERATION_R,
            true,
            true,
            true,
            true,
            true,
            true,
            false,
            false,
            false,
            false,
            true
        );

        managerC.add(
            moveC,
            true,
            false
        );
    }

    private void initTransformC() {
        this.transformC = new TransformComponent();
        this.transformC.width = 5;
        this.transformC.height = 5;
    }

    @Override
    protected void initPhysicsComponent() {
        this.physicsC = new MovableObjectPhysicsComponent(
            this,
            PROJECTILES.bit(),
            ALL.bit(),
            0.5f,
            1f,
            1f
        );

//        physicsC.autoApplyMovement = false;

        managerC.add(
            physicsC,
            true,
            true
        );
    }

    /// Criamos a body do projétil
    private void createBody() {
        if (body != null) return;

        this.body = BodyCreatorHelper.createCircle(
            this.world,
            new Vector2(
                this.transformC.x,
                this.transformC.y
            ),
            this.transformC.width,
            BodyDef.BodyType.DynamicBody,
            physicsC.getDensity(),
            physicsC.getFrict(),
            physicsC.getRest(),
            physicsC.getCategoryBit(),
            physicsC.getMaskBit()
        );

        body.setActive(false);
        body.setBullet(true);
        body.setUserData(
            new GameObjectTag(ObjectType.PROJECTILE, this)
        );
    }

    @Override
    protected void executeProjectileActivation(float x, float y, float rotation) {
        super.executeProjectileActivation(x, y, rotation);

        this.createBody();

        this.body.setTransform(
            (
                (x + transformC.getHalfWidth()) / PPM
            ),
            (
                (y + transformC.getHalfHeight()) / PPM
            ),
            rotation
        );

        this.body.setActive(true);
    }

    @Override
    protected void executeUpdate(float delta) {
    }

    @Override
    protected void executePostUpdate() {
    }

    @Override
    public void onCollisionDetection() {

    }

    @Override
    public void onEndCollisionDetection() {
//        System.out.println("saindo de colisao");
    }


    public void onObjectAndBodyPosSync() {

    }

    @Override
    protected void executeProjectileDestruction() {

    }

    @Override
    protected void executeReset() {
        this.body.setActive(false);
    }

    @Override
    protected void disposeGeneralData() {

    }

    @Override
    protected void disposeCriticalData() {

    }
}
