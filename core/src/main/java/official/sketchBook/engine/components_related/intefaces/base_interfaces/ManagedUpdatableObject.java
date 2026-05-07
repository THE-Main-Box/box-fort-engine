package official.sketchBook.engine.components_related.intefaces.base_interfaces;

public interface ManagedUpdatableObject extends UpdatableObject{
    void destroy();
    boolean isPendingRemoval();
}
