package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.world_gen.model.PlayableRoom;

public interface RoomGroundInteractableObject extends GroundInteractableObjectII{

    PlayableRoom getOwnerRoom();

}
