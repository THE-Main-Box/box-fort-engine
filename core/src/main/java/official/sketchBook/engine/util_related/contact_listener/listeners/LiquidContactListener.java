package official.sketchBook.engine.util_related.contact_listener.listeners;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.MultiLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.liquid_related.model.Liquid;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.util_related.contact_listener.MultiContactListener;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

import java.util.List;

public class LiquidContactListener implements MultiContactListener.SubContactListener {

    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        LiquidData liquid = extractLiquid(tagA, tagB);
        if (liquid == null) return;

        // Tenta interativo simples primeiro
        SimpleLiquidInteractableObjectII simple = extractSimpleInteractable(tagA, tagB);
        if (simple != null) {
            simple.getLiquidInteractionC().addLiquid(liquid);
            return;
        }

        // Tenta interativo composto
        MultiLiquidInteractableObjectII multi = extractMultiInteractable(tagA, tagB);
        if (multi == null) return;

        List<? extends SimpleLiquidInteractableObjectII> list = multi.getLiquidIObj();
        for (int i = 0; i < list.size(); i++) {
            list.get(i).getLiquidInteractionC().addLiquid(liquid);
        }
    }

    @Override
    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        LiquidData liquid = extractLiquid(tagA, tagB);
        if (liquid == null) return;

        SimpleLiquidInteractableObjectII simple = extractSimpleInteractable(tagA, tagB);
        if (simple != null) {
            simple.getLiquidInteractionC().removeLiquid(liquid);
            return;
        }

        MultiLiquidInteractableObjectII multi = extractMultiInteractable(tagA, tagB);
        if (multi == null) return;

        List<? extends SimpleLiquidInteractableObjectII> list = multi.getLiquidIObj();
        for (int i = 0; i < list.size(); i++) {
            list.get(i).getLiquidInteractionC().removeLiquid(liquid);
        }
    }

    private LiquidData extractLiquid(GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA != null && tagA.owner instanceof Liquid)
            return ((Liquid) tagA.owner).getLiquidData();
        if (tagB != null && tagB.owner instanceof Liquid)
            return ((Liquid) tagB.owner).getLiquidData();
        return null;
    }

    private SimpleLiquidInteractableObjectII extractSimpleInteractable(GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA != null && !(tagA.owner instanceof Liquid) && tagA.owner instanceof SimpleLiquidInteractableObjectII)
            return (SimpleLiquidInteractableObjectII) tagA.owner;
        if (tagB != null && !(tagB.owner instanceof Liquid) && tagB.owner instanceof SimpleLiquidInteractableObjectII)
            return (SimpleLiquidInteractableObjectII) tagB.owner;
        return null;
    }

    private MultiLiquidInteractableObjectII extractMultiInteractable(GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA != null && !(tagA.owner instanceof Liquid) && tagA.owner instanceof MultiLiquidInteractableObjectII)
            return (MultiLiquidInteractableObjectII) tagA.owner;
        if (tagB != null && !(tagB.owner instanceof Liquid) && tagB.owner instanceof MultiLiquidInteractableObjectII)
            return (MultiLiquidInteractableObjectII) tagB.owner;
        return null;
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {}

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {}
}
