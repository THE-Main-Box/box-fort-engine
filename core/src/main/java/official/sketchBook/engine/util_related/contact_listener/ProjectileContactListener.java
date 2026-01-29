package official.sketchBook.engine.util_related.contact_listener;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.components_related.projectile.ProjectileControllerComponent;
import official.sketchBook.engine.projectile_related.models.PhysicalProjectile;
import official.sketchBook.engine.util_related.enumerators.Direction;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

public class ProjectileContactListener implements MultiContactListener.SubContactListener {

    // Cache de buffers para evitar alocação
    private final Vector2 tmpNormal = new Vector2();

    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        processContact(contact, tagA, tagB, false);
    }

    @Override
    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        processContactEnd(contact, tagA, tagB);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {
        processContact(contact, tagA, tagB, true);

        // Desativa restituição se necessário (feito aqui uma única vez)
        disableRestitutionIfLocked(contact);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {
    }

    private void disableRestitutionIfLocked(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        PhysicalProjectile projectileA = extractProjectile(fixtureA);

        if (projectileA != null) {
            Direction collDir = getCollisionDirection(contact);
            if (projectileA.getControllerC().getLockC().shouldLockMovement(collDir)) {
                contact.setRestitution(0);
                return; // Basta um lado
            }
        }

        Fixture fixtureB = contact.getFixtureB();
        PhysicalProjectile projectileB = extractProjectile(fixtureB);

        if (projectileB != null) {
            Direction collDir = getCollisionDirection(contact);
            if (projectileB.getControllerC().getLockC().shouldLockMovement(collDir)) {
                contact.setRestitution(0);
            }
        }
    }

    private void processContact(
        Contact contact,
        GameObjectTag tagA,
        GameObjectTag tagB,
        boolean isContinuous
    ) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        Direction collDir = getCollisionDirection(contact);
        Body bodyA = fixtureA.getBody();
        Body bodyB = fixtureB.getBody();

        // Cache de projéteis
        PhysicalProjectile projectileA = extractProjectile(fixtureA);
        if (projectileA != null) {
            ProjectileControllerComponent controllerA = projectileA.getControllerC();
            if (isContinuous == controllerA.isContinuousDetection()) {
                handleBegin(
                    projectileA,
                    controllerA,
                    collDir,
                    bodyB,
                    tagB,
                    contact
                );
            }
        }

        PhysicalProjectile projectileB = extractProjectile(fixtureB);
        if (projectileB != null) {
            ProjectileControllerComponent controllerB = projectileB.getControllerC();
            if (isContinuous == controllerB.isContinuousDetection()) {
                handleBegin(
                    projectileB,
                    controllerB,
                    collDir,
                    bodyA,
                    tagA,
                    contact
                );
            }
        }
    }

    private void processContactEnd(
        Contact contact,
        GameObjectTag tagA,
        GameObjectTag tagB
    ) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        Direction collDir = getCollisionDirection(contact);
        Body bodyA = fixtureA.getBody();
        Body bodyB = fixtureB.getBody();

        PhysicalProjectile projectileA = extractProjectile(fixtureA);
        if (projectileA != null) {
            handleEnd(
                projectileA,
                projectileA.getControllerC(),
                collDir,
                bodyB,
                tagB,
                contact
            );
        }

        PhysicalProjectile projectileB = extractProjectile(fixtureB);
        if (projectileB != null) {
            handleEnd(
                projectileB,
                projectileB.getControllerC(),
                collDir,
                bodyA,
                tagA,
                contact
            );
        }
    }

    private void handleBegin(
        PhysicalProjectile projectile,
        ProjectileControllerComponent controller,
        Direction collDir,
        Body targetBody,
        GameObjectTag targetTag,
        Contact contact
    ) {
        // Cache de normal
        tmpNormal.set(contact.getWorldManifold().getNormal());

        controller.markStartOfCollision(
            targetTag,
            collDir,
            projectile.getBody().getPosition(),
            targetBody.getPosition(),
            tmpNormal
        );
    }

    private void handleEnd(
        PhysicalProjectile projectile,
        ProjectileControllerComponent controller,
        Direction collDir,
        Body targetBody,
        GameObjectTag targetTag,
        Contact contact
    ) {
        // Cache de normal
        tmpNormal.set(contact.getWorldManifold().getNormal());

        controller.markEndOfCollision(
            targetTag,
            collDir,
            projectile.getBody().getPosition(),
            targetBody.getPosition(),
            tmpNormal
        );
    }

    private PhysicalProjectile extractProjectile(Fixture fixture) {
        Object userData = fixture.getBody().getUserData();
        if (userData instanceof GameObjectTag) {
            GameObjectTag tag = (GameObjectTag) userData;
            if (tag.owner instanceof PhysicalProjectile) {
                return (PhysicalProjectile) tag.owner;
            }
        }
        return null;
    }

    private Direction getCollisionDirection(Contact contact) {
        return ContactActions.getCollisionDirection(contact);
    }
}
