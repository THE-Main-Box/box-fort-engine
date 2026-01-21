package official.sketchBook.engine.projectile_related;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.dataManager_related.PhysicalGameObjectDataManager;

public abstract class PhysicalProjectile extends BaseProjectile implements PhysicalObjectII {
    protected World world;
    protected Body body;

    protected PhysicsComponent physicsC;

    public PhysicalProjectile(
        ProjectilePool<?> ownerPool,
        World world
    ) {
        super(ownerPool);
        this.world = world;
    }

    /// Inicia os dados importantes da body e a body
    protected abstract void initPhysicsComponent();

    @Override
    protected void nullifyReferences() {
        super.nullifyReferences();
        physicsC = null;
        body = null;
    }

    public PhysicsComponent getPhysicsC() {
        return physicsC;
    }

    public Body getBody() {
        return body;
    }

}
