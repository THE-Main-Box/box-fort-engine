package official.sketchBook.engine.projectile_related.models;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.movement.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.game.projectile_related.pool.ProjectilePool;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public abstract class PhysicalProjectile extends BaseProjectile implements PhysicalObjectII {
    protected World world;
    protected Body body;

    protected MovableObjectPhysicsComponent physicsC;

    public PhysicalProjectile(
        ProjectilePool<?> ownerPool,
        World world
    ) {
        super(ownerPool);
        this.world = world;
    }

    @Override
    public void launch() {
        super.launch();

        //Se o componente de física já estiver lidando com a aplicação da movimentação retornamos
        if (physicsC.autoApplyMovement) return;

        //Caso aidna precisemos lidar com a movimentação de forma manual, realizamos aqui
        physicsC.applyTrajectoryImpulse(
            controllerC.launchSpeedY / PPM,
            controllerC.launchSpeedX / PPM
        );
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
