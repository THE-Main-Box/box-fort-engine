package official.sketchBook.engine.components_related.special_input_system.input_classes;

import official.sketchBook.engine.components_related.special_input_system.infra.InputCommandType;

public class NumValueInputCommand extends BaseInputValue<Float> {

    ///Valores internos auxíliares
    private final float
        min,        //Valor mínimo aceitável
        max,        //Valor máximo aceitável
        gap;        //Gap entre um valor e outro "<1" e ">0"

    ///Valor a ser percebido
    private float value;

    public NumValueInputCommand(
        String name,
        String description,
        float min,
        float max,
        float gap,
        float defaultValue
    ) {
        super(
            name,
            description,
            InputCommandType.NUM_VALUE
        );

        if (gap <= 0f)
            throw new IllegalArgumentException("gap deve ser maior que 0");

        if (min > max)
            throw new IllegalArgumentException("min n�o pode ser maior que max");

        this.min = min;
        this.max = max;
        this.gap = gap;
        this.setValue(defaultValue);
    }

    ///Faz a limitação e o calculo do gap entre valores
    private float clamp(float v) {
        float stepped = Math.round(v / gap) * gap;
        return Math.max(min, Math.min(max, stepped));
    }

    @Override
    public Float getValue() {
        return value;
    }

    @Override
    public void setValue(Float newValue) {
        this.value = clamp(newValue);
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getGap() {
        return gap;
    }
}
