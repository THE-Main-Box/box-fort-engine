package official.sketchBook.engine.game_object_related.vehicle_related;

import java.util.List;

public interface Vehicle {
    default String getName(){
        return "";
    }
    List<? extends VehicleSection> getSections();
}
