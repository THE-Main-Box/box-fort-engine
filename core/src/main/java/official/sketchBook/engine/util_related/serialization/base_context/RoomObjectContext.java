package official.sketchBook.engine.util_related.serialization.base_context;

import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;


public interface RoomObjectContext {

    ROContextData getRoomObjectContextData();

    class ROContextData {
        public final PlayableRoom ownerRoom;
        public final RoomObjectScope roomScope;

        public ROContextData(PlayableRoom ownerRoom, RoomObjectScope roomScope) {
            this.ownerRoom = ownerRoom;
            this.roomScope = roomScope;
        }
    }
}
