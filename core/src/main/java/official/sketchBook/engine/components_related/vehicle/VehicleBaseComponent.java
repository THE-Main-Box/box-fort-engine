package official.sketchBook.engine.components_related.vehicle;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.serialization.instantiation.EmbeddedSaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;

import java.util.Map;

public abstract class VehicleBaseComponent implements Component, EmbeddedSaveData {

    /// Identificação
    protected String  id;             //Id importante para decifrar quem é

    ///Tipo de componente
    protected VehicleComponentType type;

    /// Seção que iremos anexar o objeto
    protected VehicleSection ownerSection;

    private boolean disposed = false;

    public VehicleBaseComponent() {
    }

    public VehicleBaseComponent(String id, VehicleComponentType type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public void load(SaveData data) {

    }

    @Override
    public SaveData toSaveData() {
        return null;
    }

    public void attachToSection(VehicleSection section) {
        this.ownerSection = section;
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {

    }

    @Override
    public void initObject() {

    }

    public void resolveReferences(Map<String, VehicleBaseComponent> byId){}

    @Override
    public void dispose() {
        if (disposed) return;
        executeDispose();
        nullifyReferences();
        disposed = true;
    }

    protected void executeDispose() {
    }

    protected void nullifyReferences() {
        this.id = null;
        this.type = null;
        this.ownerSection = null;
    }

    public boolean canUse() {
        return true;
    }

    public boolean toUpdate(){
        return false;
    }
    public boolean toPostUpdate(){
        return false;
    }
    public boolean toRender(){
        return false;
    }

    public String getId() {
        return id;
    }

    public VehicleComponentType getType() {
        return type;
    }

}
