package official.sketchBook.engine.projectile_related.models;

import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.AutoListenedPhysicalObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.physics.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.components_related.projectile.ProjectileControllerComponent;
import official.sketchBook.engine.util_related.contact_listener.ContactActions;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.game.util_related.pools.ProjectilePool;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public abstract class PhysicalProjectile extends BaseProjectile implements PhysicalObjectII, AutoListenedPhysicalObjectII {
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

    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        onBeginContact(this, contact, tagA, tagB);
    }

    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        onEndContact(this, contact, tagA, tagB);
    }

    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {
        onPreSolve(this, contact, oldManifold, tagA, tagB);
    }

    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {
        onPostSolve(this, contact, impulse, tagA, tagB);
    }

    private static void onBeginContact(PhysicalProjectile self, Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        ProjectileControllerComponent controller = self.getControllerC();
        if (controller.isContinuousDetection()) return;

        controller.markStartOfCollision(
            tagB,
            ContactActions.getCollisionDirection(contact),
            self.body.getPosition(),
            contact.getFixtureB().getBody().getPosition(),
            contact.getWorldManifold().getNormal()
        );
    }

    private static void onEndContact(PhysicalProjectile self, Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        ProjectileControllerComponent controller = self.getControllerC();

        controller.markEndOfCollision(
            tagB,
            ContactActions.getCollisionDirection(contact),
            self.body.getPosition(),
            contact.getFixtureB().getBody().getPosition(),
            contact.getWorldManifold().getNormal()
        );
    }

    private static void onPreSolve(PhysicalProjectile self, Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {
        ProjectileControllerComponent controller = self.getControllerC();
        if (!controller.isContinuousDetection()) return;

        controller.markStartOfCollision(
            tagB,
            ContactActions.getCollisionDirection(contact),
            self.body.getPosition(),
            contact.getFixtureB().getBody().getPosition(),
            contact.getWorldManifold().getNormal()
        );

        if (controller.getLockC().shouldLockMovement(ContactActions.getCollisionDirection(contact))) {
            contact.setRestitution(0);
        }
    }

    private static void onPostSolve(PhysicalProjectile self, Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {
    }
}
