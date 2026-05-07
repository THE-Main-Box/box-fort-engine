package official.sketchBook.engine.components_related.vehicle;

import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.InteractableObject;

public class VehicleDoor implements InteractableObject {
    ///Fixture física da porta
    public final Fixture doorFix;

    ///Flags de estado
    private boolean
        open;           //Se está aberta

    public VehicleDoor(
        Fixture doorFix,
        boolean open
    ) {
        this.doorFix = doorFix;
        this.open = open;
    }

    public void interact(){
        if(!canInteract()) return;

        this.open = !open;      //Abre e fecha a porta a cada chamada
    }

    public boolean canInteract(){
        return true;
    }
}
