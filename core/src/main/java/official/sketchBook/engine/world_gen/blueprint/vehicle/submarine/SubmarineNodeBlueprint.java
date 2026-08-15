package official.sketchBook.engine.world_gen.blueprint.vehicle.submarine;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarineNode;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarinePart;
import official.sketchBook.engine.util_related.serialization.instantiation.EmbeddedSaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;
import official.sketchBook.engine.world_gen.blueprint.embedded.PartPlacement;

import java.util.ArrayList;
import java.util.List;

public class SubmarineNodeBlueprint implements EmbeddedSaveData {
    public float offsetX, offsetY;
    public float rotation;

    public List<SubmarinePartBlueprint> partList = new ArrayList<>();

    public SubmarineNodeBlueprint() {
    }

    public SubmarineNodeBlueprint(SubmarineNode node, float relativeOffsetX, float relativeOffsetY) {
        this.offsetX = relativeOffsetX;
        this.offsetY = relativeOffsetY;
        this.rotation = node.getBody().getAngle() * MathUtils.radiansToDegrees;

        List<SubmarinePart> parts = node.getPhysicalParts(); // precisa existir esse getter

        for (int i = 0; i < parts.size(); i++) {
            SubmarinePart part = parts.get(i);
            this.partList.add(new SubmarinePartBlueprint(part, part.getCenterX(), part.getCenterY()));
        }
    }

    @Override
    public SaveData toSaveData() {
        return new SaveData()
            .put("offset_x", offsetX)
            .put("offset_y", offsetY)
            .putEmbeddedList("part_list", partList);
    }

    @Override
    public void load(SaveData data) {
        this.offsetX = data.getFloat("offset_x", 0);
        this.offsetY = data.getFloat("offset_y", 0);

        this.partList = data.getEmbeddedList("part_list", SubmarinePartBlueprint.class);
    }

    public SubmarineNode toSubmarineNode(
        World physicsWorld,
        float originX,
        float originY
    ) {
        //geramos uma lista de partes
        List<SubmarinePart> parts = new ArrayList<>(partList.size());

        //Percorremos a lista de bp
        for (int i = 0; i < partList.size(); i++) {
            //Adicionamos na lista de partes
            parts.add(
                //Pegamos a parte atual
                partList.get(i)
                    //Transformamos em uma parte
                    .toSubmarinePart()
            );
        }

        return new SubmarineNode(
            physicsWorld,
            parts,
            originX + offsetX,
            originY + offsetY,
            0,
            rotation,
            false,
            false
        );

    }
}
