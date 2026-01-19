package official.sketchBook.engine.projectile_related;

import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;

public abstract class PhysicalProjectile extends BaseProjectile implements PhysicalObjectII {
    protected World world;
    protected PhysicsComponent physicsC;

    public PhysicalProjectile(World world) {
        super();
        this.world = world;
    }

    @Override
    protected void disposeGeneralData() {

    }

    @Override
    protected void nullifyReferences() {
        super.nullifyReferences();
        physicsC = null;
    }

    @Override
    public PhysicsComponent getPhysicsC() {
        return physicsC;
    }
}
