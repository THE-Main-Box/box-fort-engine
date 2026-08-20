package official.sketchBook.engine.world_gen.blueprint.vehicle.submarine;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;
import official.sketchBook.engine.components_related.vehicle.VehicleComponentTypeRegistry;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarineNode;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarinePart;
import official.sketchBook.engine.util_related.serialization.instantiation.EmbeddedSaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;

import java.util.ArrayList;
import java.util.List;

public class SubmarineNodeBlueprint implements EmbeddedSaveData {

    public float offsetX, offsetY;
    public float rotation;

    public List<SubmarinePartBlueprint> partList = new ArrayList<>();

    /**
     * SaveData cru de cada componente ? n?o dá pra usar getEmbeddedList
     * com uma Class<T> fixa aqui porque a lista é heterog?nea (porta,
     * motor, controller...). Cada bloco já carrega sua pr?pria type key
     * (via putTypeKey/getTypeKey), e é essa key que resolve, no momento
     * de reconstruir, qual VehicleBaseComponent concreto instanciar via
     * VehicleComponentTypeRegistry. O node nunca v? SaveData ? só recebe
     * VehicleBaseComponent já prontos.
     */
    public List<SaveData> componentDataList = new ArrayList<>();

    public SubmarineNodeBlueprint() {
    }

    public SubmarineNodeBlueprint(SubmarineNode node, float relativeOffsetX, float relativeOffsetY) {
        this.offsetX = relativeOffsetX;
        this.offsetY = relativeOffsetY;
        this.rotation = node.getBody().getAngle() * MathUtils.radiansToDegrees;

        List<SubmarinePart> parts = node.getPhysicalParts();
        for (int i = 0; i < parts.size(); i++) {
            SubmarinePart part = parts.get(i);
            this.partList.add(new SubmarinePartBlueprint(part, part.getCenterX(), part.getCenterY()));
        }

        List<VehicleBaseComponent> components = node.getVehicleComponentList();
        for (int i = 0; i < components.size(); i++) {
            this.componentDataList.add(components.get(i).toSaveData());
        }
    }

    @Override
    public SaveData toSaveData() {
        return new SaveData()
            .put("offset_x", offsetX)
            .put("offset_y", offsetY)
            .putEmbeddedList("part_list", partList)
            .putList("component_list", componentDataList); // já são SaveData prontos ? sem conversão
    }

    @Override
    public void load(SaveData data) {
        this.offsetX = data.getFloat("offset_x", 0);
        this.offsetY = data.getFloat("offset_y", 0);
        this.partList = data.getEmbeddedList("part_list", SubmarinePartBlueprint.class);
        this.componentDataList = data.getList("component_list");
    }

    public SubmarineNode toSubmarineNode(
        World physicsWorld,
        float originX,
        float originY
    ) {
        List<SubmarinePart> parts = new ArrayList<>(partList.size());
        for (int i = 0; i < partList.size(); i++) {
            parts.add(partList.get(i).toSubmarinePart());
        }

        SubmarineNode node = new SubmarineNode(
            physicsWorld,
            parts,
            originX + offsetX,
            originY + offsetY,
            0,
            rotation,
            false,
            false
        );


        return node;
    }

    // SubmarineNodeBlueprint
    public void attachComponentsToNode(SubmarineNode node) {
        for (int i = 0; i < componentDataList.size(); i++) {
            SaveData data = componentDataList.get(i);
            String typeKey = data.getTypeKey();

            VehicleBaseComponent component = VehicleComponentTypeRegistry.GLOBAL.create(typeKey);
            component.load(data);
            component.attachToSection(node);
            component.initObject();

            node.addVehicleComponent(component);
        }
    }
}
