package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid;

import official.sketchBook.engine.components_related.physics.LiquidInteractionComponent;

public interface LIOBase {
    void onLiquidExit();
    void onLiquidEnter();

    void inLiquidUpdate();

    LiquidInteractionComponent getLiquidInteractionC();
}
