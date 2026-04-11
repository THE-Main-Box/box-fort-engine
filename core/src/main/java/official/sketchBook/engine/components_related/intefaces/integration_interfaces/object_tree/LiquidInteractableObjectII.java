package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.liquid_related.model.LiquidData;

public interface LiquidInteractableObjectII extends MovableObjectII{

    void onLiquidExit();
    void onLiquidEnter();

    void inLiquidUpdate();
    PhysicalMobLiquidInteractionComponent getLiquidInteractionC();
}
