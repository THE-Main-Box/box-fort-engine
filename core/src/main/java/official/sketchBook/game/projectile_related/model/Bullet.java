package official.sketchBook.game.projectile_related.model;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.components_related.movement.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.projectile.ProjectileControllerComponent;
import official.sketchBook.engine.projectile_related.models.PhysicalProjectile;
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

        initTransformC();
        initPhysicsComponent();
        initMovementComponent();
    }

    @Override
    protected void initController() {
        this.controllerC = new ProjectileControllerComponent(this);

        this.toUpdate.add(controllerC);
        this.toPostUpdate.add(controllerC);
    }

    private void initMovementComponent() {
        this.moveC = new MovementComponent(
            this,
            WorldConstants.ProjectileConstants.PROJECTILE_MAX_SPEED_X,
            WorldConstants.ProjectileConstants.PROJECTILE_MAX_SPEED_Y,
            999,
            WorldConstants.ProjectileConstants.PROJECTILE_DECELERATION_X,
            WorldConstants.ProjectileConstants.PROJECTILE_DECELERATION_Y,
            0,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true
        );

        this.toUpdate.add(moveC);
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
            0f,
            0f
        );

        this.toUpdate.add(physicsC);
        this.toPostUpdate.add(physicsC);
    }

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
    }

    @Override
    protected void executeProjectileStart(float x, float y, float rotation) {
        super.executeProjectileStart(x, y, rotation);

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
}
