package official.sketchBook.engine.world_gen.blueprint.vehicle.submarine;

import official.sketchBook.engine.game_object_related.vehicle_related.SubmarinePart;
import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.util_related.serialization.instantiation.EmbeddedSaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;
import official.sketchBook.engine.world_gen.blueprint.embedded.FixtureDataBlueprint;

import java.util.ArrayList;
import java.util.List;

public class SubmarinePartBlueprint implements EmbeddedSaveData {
    public int id;
    public String tag;
    public float baseMass;
    public float
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
    public float offsetX, offsetY;

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

    public SubmarinePartBlueprint(
        SubmarinePart part,
        float offsetX,
        float offsetY
    ){
        //Geramos a bp normalmente
        this(
            part.id,
            part.tag,
            part.getBaseMass(),
            part.getInternalMarginLeft(),
            part.getInternalMarginRight(),
            part.getInternalMarginUp(),
            part.getInternalMarginDown()
        );

        //Atualizamos o offset
        updateOffsets(
            offsetX,
            offsetY
        );

        //Percorremos a lista de fixdata
        for (int i = 0; i < part.fixtureDataList.size(); i++) {
            //Adicionamos na lista
            fixtures.add(
                //Convertemos na bp
                FixtureDataBlueprint.toBlueprint(
                    //Passamos a fixtureData atual
                    part.fixtureDataList.get(i)
                )
            );
        }
    }

    public SubmarinePartBlueprint() {
    }

    public void updateOffsets(float x, float y){
        this.offsetX = x;
        this.offsetY = y;
    }

    public SubmarinePart toSubmarinePart() {
        List<FixtureData> fixtureDataList = new ArrayList<>(fixtures.size());

        for (int i = 0; i < fixtures.size(); i++) {
            fixtureDataList.add(fixtures.get(i).toFixtureData(offsetX, offsetY));
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

    @Override
    public SaveData toSaveData() {
        SaveData data = new SaveData();

        data.put("id", id);
        data.put("tag", tag);

        data.put("base_mass",baseMass);

        data.put("margin_left",marginLeft);
        data.put("margin_right",marginRight);
        data.put("margin_up", marginUp);
        data.put("margin_down",marginDown);

        data.putEmbeddedList("fixtures",fixtures);

        return data;
    }

    @Override
    public void load(SaveData data) {
        this.id = data.getIntRequired("id");
        this.tag = data.getStringRequired("tag");

        this.baseMass = data.getFloatRequired("base_mass");

        this.marginLeft = data.getFloat("margin_left", 0);
        this.marginRight= data.getFloat("margin_right", 0);
        this.marginUp = data.getFloat("margin_up", 0);
        this.marginDown = data.getFloat("margin_down", 0);

        this.fixtures = data.getEmbeddedList("fixtures", FixtureDataBlueprint.class);
    }
}
