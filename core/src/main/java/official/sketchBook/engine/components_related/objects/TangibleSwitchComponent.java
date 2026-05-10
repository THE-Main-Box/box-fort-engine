package official.sketchBook.engine.components_related.objects;

import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.util_related.enumerators.CollisionLayers;

import java.util.List;

public class TangibleSwitchComponent {

    public List<Fixture> fixList;

    private final short originalMaskBit;

    private boolean tangible;

    public TangibleSwitchComponent(
        short originalMaskBit,
        boolean tangible,
        List<Fixture> fixList
    ) {
        this.originalMaskBit = originalMaskBit;
        this.tangible = tangible;
        this.fixList = fixList;
    }

    public void setTangible(boolean tangible) {
        if(tangible == this.tangible) return;
        this.tangible = tangible;
    }

    public void updateTangibleState(){
        for (int i = 0; i < fixList.size(); i++) {
            Fixture fix = fixList.get(i);

            if (tangible) {
                fix.getFilterData().maskBits = originalMaskBit;
            } else {
                fix.getFilterData().maskBits = CollisionLayers.NONE.bit();
            }

            fix.refilter();
        }
    }

}
