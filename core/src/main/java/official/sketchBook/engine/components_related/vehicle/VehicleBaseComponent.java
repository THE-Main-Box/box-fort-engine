package official.sketchBook.engine.components_related.vehicle;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.game_object_related.vehicle.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;

public abstract class VehicleBaseComponent implements Component {

    /// Identificação
    protected final String
        name,           //Nome do componente
        id;             //Id importante para decifrar quem é

    /// Referência ao local de instancia
    protected final VehicleSection ownerSection;
    protected final VehicleComponentType type;

    protected boolean isFunctional;

    private boolean disposed = false;

    public VehicleBaseComponent(
        String name,
        String id,
        VehicleSection ownerSection,
        VehicleComponentType type
    ) {
        this.name = name;
        this.id = id;
        this.ownerSection = ownerSection;
        this.type = type;
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

    @Override
    public void dispose() {
        if (disposed) return;
        nullifyReferences();
        disposed = true;
    }

    private void nullifyReferences() {

    }

    /// Para aqueles com lógica especial,
    ///  poderemos usar para determinar se podemos executar ou não sua função primária ou não
    public boolean canUse() {
        return isFunctional;
    }
}
