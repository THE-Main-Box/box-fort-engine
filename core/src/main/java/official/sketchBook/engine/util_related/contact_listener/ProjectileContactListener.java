package official.sketchBook.engine.util_related.contact_listener;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.projectile.ProjectileControllerComponent;
import official.sketchBook.engine.projectile_related.models.BaseProjectile;
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
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {

    }

    private void processProjectileTag(GameObjectTag projectileTag) {

    }

    private void processContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB, boolean isContinuous) {
        // Processa A como projétil
        PhysicalProjectile projectileA = extractProjectile(contact.getFixtureA());
        Direction collDirA = getCollisionDirection(contact);

        if (projectileA != null) {
            if (isContinuous == projectileA.getControllerC().isContinuousDetection()) {
                handleBegin(
                    projectileA,
                    collDirA,
                    contact.getFixtureB().getBody(),
                    tagB,
                    contact
                );
            }
        }

        // Processa B como projétil (SEMPRE, mesmo se A foi processado)
        PhysicalProjectile projectileB = extractProjectile(contact.getFixtureB());
        if (projectileB != null) {
            // Se o projétil quer contínuo e estamos em beginContact, ignora
            if (isContinuous == projectileB.getControllerC().isContinuousDetection()) {
                handleBegin(
                    projectileB,
                    collDirA,
                    contact.getFixtureA().getBody(),
                    tagA,
                    contact
                );
            }
        }

    }

    private void processContactEnd(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        // Processa A como projétil
        PhysicalProjectile projectileA = extractProjectile(contact.getFixtureA());
        Direction collDirA = getCollisionDirection(contact);

        if (projectileA != null) {
            handleEnd(
                projectileA,
                collDirA,
                contact.getFixtureB().getBody(),
                tagB,
                contact
            );
        }

        // Processa B como projétil
        PhysicalProjectile projectileB = extractProjectile(contact.getFixtureB());
        if (projectileB != null) {
            handleEnd(
                projectileB,
                collDirA,
                contact.getFixtureA().getBody(),
                tagA,
                contact
            );
        }
    }

    private void handleBegin(
        PhysicalProjectile projectile,
        Direction collDir,
        Body targetBody,
        GameObjectTag targetTag,
        Contact contact
    ) {
        ProjectileControllerComponent controller = projectile.getControllerC();

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
        ProjectileControllerComponent controller = projectile.getControllerC();

        controller.markEndOfCollision(
            targetTag,
            collDir,
            projectile.getBody().getPosition(),
            targetBody.getPosition(),
            contact.getWorldManifold().getNormal()
        );
    }

    private PhysicalProjectile extractProjectile(com.badlogic.gdx.physics.box2d.Fixture fixture) {
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
