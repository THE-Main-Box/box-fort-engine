package official.sketchBook.engine.components_related.input_system.input_classes;

import official.sketchBook.engine.components_related.input_system.infra.InputCommandType;

public class ToggleInputCommand extends BaseInputValue<Boolean> {

    private boolean value;

    public ToggleInputCommand(
        String name,
        String description,
        boolean defaultValue
    ) {
        super(
            name,
            description,
            InputCommandType.TOGGLE
        );
        this.value = defaultValue;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public void setValue(Boolean newValue) {
        this.value = newValue;
    }
}
