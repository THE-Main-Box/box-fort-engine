package official.sketchBook.engine.liquid_related.model;

public class LiquidData {

    /// Nome do liquido
    public final String name;

    /// Id de referência
    public final int id;

    /// Valores de propriedade de liquido
    public final float
        density,        //Densidade do liquido, irá determinar o quão fácil ou difícil é para um objeto boiar
        resistance,     //Resistencia de movimento, o quão difícil é para acelerar, e o quão fácil é para desacelerar
        maxMoveSpeed,   //Velocidade máxima para se mover
        maxSinkSpeed,   //Velocidade máxima para afundar
        maxRiseSpeed;   //Velocidade máxima para flutuar

    public LiquidData(
        String name,
        int id,
        float density,
        float resistance,
        float maxSinkSpeed,
        float maxRiseSpeed,
        float maxMoveSpeed
    ) {
        this.name = name;
        this.id = id;

        this.density = density;
        this.resistance = resistance;

        this.maxRiseSpeed = maxRiseSpeed;
        this.maxSinkSpeed = maxSinkSpeed;
        this.maxMoveSpeed = maxMoveSpeed;
    }

    public LiquidData() {
        this.name = "";
        this.id = -1;

        this.resistance
            = this.maxMoveSpeed
            = this.density
            = this.maxSinkSpeed
            = this.maxRiseSpeed
            = 0;
    }

    @Override
    public String toString() {
        return "LiquidData{" +
            "name='" + name + '\'' +
            ", id=" + id +
            ", density=" + density +
            ", resistance=" + resistance +
            ", maxSinkSpeed=" + maxSinkSpeed +
            ", maxRiseSpeed=" + maxRiseSpeed +
            ", maxMoveSpeed=" + maxMoveSpeed +
            '}';
    }
}
