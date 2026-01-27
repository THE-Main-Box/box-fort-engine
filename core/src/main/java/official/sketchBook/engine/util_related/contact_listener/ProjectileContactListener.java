package official.sketchBook.engine.util_related.contact_listener;

import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.components_related.projectile.ProjectileControllerComponent;
import official.sketchBook.engine.components_related.projectile.ProjectileMovementLockComponent;
import official.sketchBook.engine.projectile_related.models.PhysicalProjectile;
import official.sketchBook.engine.util_related.enumerators.Direction;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

import static official.sketchBook.engine.util_related.contact_listener.ContactActions.getCollisionDirection;

public class ProjectileContactListener implements MultiContactListener.SubContactListener {

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
        checkAndDisableRestitutionIfLocked(contact);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {
    }

    /* ===========================
       Restituição / Lock
       =========================== */

    private void checkAndDisableRestitutionIfLocked(Contact contact) {
        Direction collDir = getCollisionDirection(contact);

        PhysicalProjectile projectileA = extractProjectile(contact.getFixtureA());
        if (projectileA != null && shouldLockMovement(projectileA, collDir)) {
            contact.setRestitution(0);
            return; // já basta um lado travar
        }

        PhysicalProjectile projectileB = extractProjectile(contact.getFixtureB());
        if (projectileB != null && shouldLockMovement(projectileB, collDir)) {
            contact.setRestitution(0);
        }
    }

    private boolean shouldLockMovement(PhysicalProjectile projectile, Direction collDir) {
        ProjectileMovementLockComponent lockC =
            projectile.getControllerC().getLockC();
        return lockC.shouldLockMovement(collDir);
    }

    /* ===========================
       Begin / PreSolve
       =========================== */

    private void processContact(
        Contact contact,
        GameObjectTag tagA,
        GameObjectTag tagB,
        boolean isContinuous
    ) {
        Direction collDir = getCollisionDirection(contact);

        // A como projétil
        PhysicalProjectile projectileA = extractProjectile(contact.getFixtureA());
        if (projectileA != null) {
            if (isContinuous == projectileA.getControllerC().isContinuousDetection()) {
                handleBegin(
                    projectileA,
                    collDir,
                    contact.getFixtureB().getBody(),
                    tagB,
                    contact
                );
            }
        }

        // B como projétil
        PhysicalProjectile projectileB = extractProjectile(contact.getFixtureB());
        if (projectileB != null) {
            if (isContinuous == projectileB.getControllerC().isContinuousDetection()) {
                handleBegin(
                    projectileB,
                    collDir,
                    contact.getFixtureA().getBody(),
                    tagA,
                    contact
                );
            }
        }
    }

    /* ===========================
       EndContact
       =========================== */

    private void processContactEnd(
        Contact contact,
        GameObjectTag tagA,
        GameObjectTag tagB
    ) {
        Direction collDir = getCollisionDirection(contact);

        PhysicalProjectile projectileA = extractProjectile(contact.getFixtureA());
        if (projectileA != null) {
            handleEnd(
                projectileA,
                collDir,
                contact.getFixtureB().getBody(),
                tagB,
                contact
            );
        }

        PhysicalProjectile projectileB = extractProjectile(contact.getFixtureB());
        if (projectileB != null) {
            handleEnd(
                projectileB,
                collDir,
                contact.getFixtureA().getBody(),
                tagA,
                contact
            );
        }
    }

    /* ===========================
       Handlers
       =========================== */

    private void handleBegin(
        PhysicalProjectile projectile,
        Direction collDir,
        Body targetBody,
        GameObjectTag targetTag,
        Contact contact
    ) {
        ProjectileControllerComponent controller =
            projectile.getControllerC();

        controller.markStartOfCollision(
            targetTag,
            collDir,
            projectile.getBody().getPosition(),
            targetBody.getPosition(),
            contact.getWorldManifold().getNormal()
        );
    }

    private void handleEnd(
        PhysicalProjectile projectile,
        Direction collDir,
        Body targetBody,
        GameObjectTag targetTag,
        Contact contact
    ) {
        ProjectileControllerComponent controller =
            projectile.getControllerC();

        controller.markEndOfCollision(
            targetTag,
            collDir,
            projectile.getBody().getPosition(),
            targetBody.getPosition(),
            contact.getWorldManifold().getNormal()
        );
    }

    /* ===========================
       Utils
       =========================== */

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
}
