package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.util_related.pools.RayCastPool;

public interface RayCaster extends PhysicalObjectII{
    RayCastPool getRayCastPoolInstance();
}
