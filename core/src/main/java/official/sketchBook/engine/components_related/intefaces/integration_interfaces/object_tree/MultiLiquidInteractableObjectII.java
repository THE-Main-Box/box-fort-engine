package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import java.util.List;

public interface MultiLiquidInteractableObjectII {
    void onLiquidExit();
    void onLiquidEnter();

    void inLiquidUpdate();

    List<? extends SimpleLiquidInteractableObjectII> getLiquidIObj();
}
