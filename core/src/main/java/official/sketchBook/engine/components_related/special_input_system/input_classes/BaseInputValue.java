package official.sketchBook.engine.components_related.special_input_system.input_classes;

import official.sketchBook.engine.components_related.special_input_system.infra.InputCommand;
import official.sketchBook.engine.components_related.special_input_system.infra.InputCommandType;

public abstract class BaseInputValue<T> implements InputCommand<T> {
    private final String
        name,
        description;

    protected final InputCommandType type;

    public BaseInputValue(
        String name,
        String description,
        InputCommandType type
    ) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public InputCommandType getType() {
        return type;
    }
}
