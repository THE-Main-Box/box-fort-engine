package official.sketchBook.engine.world_gen.blueprint.vehicle;

import official.sketchBook.engine.game_object_related.vehicle_related.SubmarinePart;
import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.world_gen.blueprint.embedded.FixtureDataBlueprint;

import java.util.ArrayList;
import java.util.List;

public class SubmarinePartBlueprint {
    public int id;
    public String tag;
    public float baseMass;
    public float marginLeft, marginRight, marginUp, marginDown;

    /**
     * Offset desta part em relação ao centro do node — dado de
     * composição, não de fixture individual. Cada FixtureDataBlueprint
     * desta part recebe este valor como globalOffsetX/Y na hora de gerar
     * o FixtureData final.
     */
    public float partOffsetX, partOffsetY;

    public List<FixtureDataBlueprint> fixtures = new ArrayList<>();

    public SubmarinePartBlueprint() {
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
