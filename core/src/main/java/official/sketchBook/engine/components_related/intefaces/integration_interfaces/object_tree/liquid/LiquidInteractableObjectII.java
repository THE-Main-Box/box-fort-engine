package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid;

import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.MobLiquidInteractionComponent;
import official.sketchBook.engine.components_related.system_utils.SubmersibleVolume;

import java.util.List;

public interface LiquidInteractableObjectII extends LIOBase{

    List<SubmersibleVolume> getSubmersibleVolumeList();

    TransformComponent getTransformC();
    MovementComponent getMoveC();
    MobLiquidInteractionComponent getLiquidInteractionC();
}
