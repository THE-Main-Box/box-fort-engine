package official.sketchBook.engine.world_gen.blueprint.vehicle;

import official.sketchBook.engine.game_object_related.vehicle_related.SubmarinePart;
import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.world_gen.blueprint.embedded.FixtureDataBlueprint;

import java.util.ArrayList;
import java.util.List;

public class SubmarinePartBlueprint {
    public final int id;
    public final String tag;
    public final float baseMass;
    public final float
        marginLeft,
        marginRight,
        marginUp,
        marginDown;

    /**
     * Offset desta part em relação ao centro do node — dado de
     * composição, não de fixture individual. Cada FixtureDataBlueprint
     * desta part recebe este valor como globalOffsetX/Y na hora de gerar
     * o FixtureData final.
     */
    public float partOffsetX, partOffsetY;

    public List<FixtureDataBlueprint> fixtures = new ArrayList<>();

    public SubmarinePartBlueprint(
        int id, String tag,
        float baseMass,
        float marginLeft,
        float marginRight,
        float marginUp,
        float marginDown
    ) {
        this.id = id;
        this.tag = tag;
        this.baseMass = baseMass;
        this.marginLeft = marginLeft;
        this.marginRight = marginRight;
        this.marginUp = marginUp;
        this.marginDown = marginDown;
    }

    public SubmarinePart toSubmarinePart() {
        List<FixtureData> fixtureDataList = new ArrayList<>(fixtures.size());

        for (int i = 0; i < fixtures.size(); i++) {
            fixtureDataList.add(fixtures.get(i).toFixtureData(partOffsetX, partOffsetY));
        }

        return new SubmarinePart(
            id,
            tag,
            baseMass,
            marginLeft,
            marginRight,
            marginUp,
            marginDown,
            fixtureDataList
        );
    }
}
