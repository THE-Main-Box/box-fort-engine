package official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related;

import com.badlogic.gdx.math.Vector2;

public interface ObjectMassContributor {
    float getContributionMass();
    Vector2 getContributionPoint();
    boolean isContributing();   // gate (ex: isOnGround, ou sempre true)
}
