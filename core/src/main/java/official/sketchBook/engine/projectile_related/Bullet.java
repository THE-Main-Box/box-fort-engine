package official.sketchBook.engine.projectile_related;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.components_related.movement.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.ALL;
import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.PROJECTILES;

public class Bullet extends PhysicalProjectile {

    public Bullet(ProjectilePool<?> ownerPool, World world) {
        super(ownerPool, world);

        initComponents();
    }

    @Override
    protected void initComponents() {
        this.reset = false;

        initTransformC();
        initPhysicsComponent();
        initMovementComponent();
    }

    private void initMovementComponent(){
        this.moveC = new MovementComponent(
            this,
            9999,
            9999,
            0,
            0,
            true,
            true,
            true,
            true,
            true,
            true,
            false
        );

        this.toUpdate.add(moveC);
    }

    private void initTransformC() {
        this.transformC = TransformComponent.initNewTransformComponent(
            0,
            0,
            0,
            0,
            16,
            0,
            1,
            1,
            false,
            false
        );
    }

    @Override
    protected void initPhysicsComponent() {
        this.physicsC = new MovableObjectPhysicsComponent(
            this,
            PROJECTILES.bit(),
            ALL.bit(),
            0.5f,
            1f,
            0f
        );

        this.createBody();

        this.toUpdate.add(physicsC);
        this.toPostUpdate.add(physicsC);
    }

    public void createBody() {
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
    }

    public void onObjectAndBodyPosSync() {

    }

    @Override
    protected void executeProjectileDestruction() {

    }

    @Override
    protected void executeReset() {

    }
}
