package official.sketchBook.engine.world_gen.util;

import com.badlogic.gdx.math.MathUtils;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;

import java.util.List;

public class SubmarineComponentUtils {
    public static String generateComponentId(String typeKey, List<? extends VehicleBaseComponent> vehicleComponentList) {
        String candidate;
        do {
            candidate = typeKey + "_" + MathUtils.random(0, 999999);
        } while (hasComponentWithId(candidate, vehicleComponentList));
        return candidate;
    }

    private static boolean hasComponentWithId(String id, List<? extends VehicleBaseComponent> vehicleComponentList) {
        for (int i = 0; i < vehicleComponentList.size(); i++) {
            if (vehicleComponentList.get(i).getId().equals(id)) return true;
        }
        return false;
    }
}
