package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid;

import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.components_related.system_utils.SubmersibleVolume;

import java.util.List;

public interface LiquidInteractableObjectII {

    void onLiquidExit();
    void onLiquidEnter();

    void inLiquidUpdate();

    List<SubmersibleVolume> getSubmersibleVolume();

    TransformComponent getTransformC();
    MovementComponent getMoveC();
    PhysicalMobLiquidInteractionComponent getLiquidInteractionC();
}
