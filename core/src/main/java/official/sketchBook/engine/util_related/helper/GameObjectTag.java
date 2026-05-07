package official.sketchBook.engine.util_related.helper;

import official.sketchBook.engine.util_related.enumerators.ObjectType;

public class GameObjectTag {
    public final ObjectType type;
    public final Object owner;

    public GameObjectTag(ObjectType type, Object owner) {
        this.type = type;
        this.owner = owner;
    }
}
