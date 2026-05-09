package official.sketchBook.engine.game_object_related.vehicle_related;

import java.util.List;

public interface Vehicle {
    List<? extends VehicleSection> getSections();
}
