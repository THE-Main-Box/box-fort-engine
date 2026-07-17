package official.sketchBook.engine.liquid_related.model;

public class LiquidData {

    /// Nome do liquido
    public final String name;

    /// Id de referência
    public final int id;

    /// Valores de propriedade de liquido
    public final float
        density,        //Densidade do liquido, irá determinar o quão fácil ou difícil é para um objeto boiar
        drag;     //Resistencia de movimento, o quão difícil é para acelerar, e o quão fácil é para desacelerar

    public LiquidData(
        String name,
        int id,
        float density,
        float drag
    ) {
        this.name = name;
        this.id = id;

        this.density = density;
        this.drag = drag;

    }

    public LiquidData() {
        this.name = "";
        this.id = -1;

        this.drag
            = this.density
            = 0;
    }

    @Override
    public String toString() {
        return "LiquidData{" +
            "name='" + name + '\'' +
            ", id=" + id +
            ", density=" + density +
            ", resistance=" + drag +
            '}';
    }
}
