package official.sketchBook.engine.util_related.serialization.base_context;

/**
 * Garante a exist�ncia dos dados de transform (posi��o, tamanho,
 * escala, espelhamento) pra quem implementar. Independente das outras
 * ? substitui o antigo TransformedRoomObjectContext, que estendia
 * RoomObjectContext e for�ava toda classe "transform�vel" a
 * obrigatoriamente ser tamb�m "de sala", mesmo quando isso n�o fazia
 * sentido (ex: um objeto transform�vel fora de uma room).
 * <p>
 * Mesma garantia das outras: sem setter, tudo pelo construtor.
 */
public interface TransformContext {

    TContextData getTransformContextData();

    class TContextData {
        public final float x, y, z, rotation, width, height, scaleX, scaleY;
        public final boolean mirrorX, mirrorY;

        public TContextData(
            float x,
            float y,
            float z,
            float rotation,
            float width,
            float height,
            float scaleX,
            float scaleY,
            boolean mirrorX,
            boolean mirrorY
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rotation = rotation;
            this.width = width;
            this.height = height;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.mirrorX = mirrorX;
            this.mirrorY = mirrorY;
        }
    }
}
