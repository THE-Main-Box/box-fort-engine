package official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related;

import java.util.List;

public interface MultiRenderableObjectII extends RenderableObjectII{

    List<? extends RenderableObjectII> getRenderableObjList();

}
