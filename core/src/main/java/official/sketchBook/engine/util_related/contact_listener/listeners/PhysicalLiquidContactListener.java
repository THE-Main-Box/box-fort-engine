package official.sketchBook.engine.util_related.contact_listener.listeners;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.LiquidInteractableObjectII;
import official.sketchBook.engine.liquid_related.model.Liquid;
import official.sketchBook.engine.util_related.contact_listener.MultiContactListener;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

public class PhysicalLiquidContactListener implements MultiContactListener.SubContactListener {

    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        handle(tagA, tagB, true);
    }

    @Override
    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        // Sensores podem disparar endContact de modo inconsistente
        // só removemos se realmente não estiver mais tocando
        if (contact.isTouching()) return;

        handle(tagA, tagB, false);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {
    }

    private void handle(
        GameObjectTag tagA,
        GameObjectTag tagB,
        boolean entering
    ) {
        // Tenta A como objeto e B como líquido
        if (!tryHandle(tagA, tagB, entering)) {
            // Tenta B como objeto e A como líquido
            tryHandle(tagB, tagA, entering);
        }
    }

    private boolean tryHandle(
        GameObjectTag objectTag,
        GameObjectTag liquidTag,
        boolean entering
    ) {
        if (objectTag == null || liquidTag == null) return false;

        if (!(objectTag.owner instanceof LiquidInteractableObjectII)) {
            return false;
        }
        if (!(liquidTag.owner instanceof Liquid)) {
            return false;
        }

        LiquidInteractableObjectII obj = (LiquidInteractableObjectII) objectTag.owner;
        Liquid liquid = (Liquid) liquidTag.owner;

        if (entering) {
            obj.getLiquidInteractionC().addLiquid(liquid.getLiquidData());
        } else {
            obj.getLiquidInteractionC().removeLiquid(liquid.getLiquidData());
        }

        return true;
    }
}
