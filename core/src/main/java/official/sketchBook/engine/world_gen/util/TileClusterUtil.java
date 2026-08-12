package official.sketchBook.engine.world_gen.util;

import com.badlogic.gdx.utils.Array;

/**
 * Agrupador gen?rico de tiles adjacentes numa mesma camada em ret?ngulos
 * maiores ? o mesmo algoritmo de "merge" que antes vivia preso dentro de
 * RoomBodyFactoryHelper (acoplado a TileBodyType), agora desacoplado de
 * qualquer sistema espec?fico (f?sica, l?quido, o que for).
 * <p>
 * Quem usa decide, via {@link ClusterMatcher}, quais ids pertencem ao mesmo
 * cluster ? o algoritmo em si s? sabe percorrer e agrupar. O resultado ?
 * uma lista de ret?ngulos (posi??o + tamanho em c?lulas de grid), nunca
 * bodies ou regions prontos: a constru??o do que aquele ret?ngulo vira
 * (Body via FixtureData, LiquidRegion, etc) fica com o chamador.
 */
public class TileClusterUtil {

    /**
     * Decide se duas c?lulas da grid pertencem ao mesmo cluster.
     * Ex: "mesmo id exato", "ambos s?lidos", "ambos l?quidos do mesmo tipo".
     */
    public interface ClusterMatcher {
        boolean matches(int originId, int candidateId);
    }

    public static final class ClusterRect {
        public final int x, y, width, height;

        public ClusterRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Percorre uma camada (matriz de ids) e agrupa c?lulas adjacentes que
     * o matcher considera do mesmo cluster em ret?ngulos maiores (merge
     * guloso: expande largura primeiro, depois altura, igual ao algoritmo
     * original de RoomBodyFactoryHelper).
     *
     * @param layerData matriz [y][x] de ids de tile de UMA camada j? extra?da
     * @param matcher   decide quais ids formam cluster entre si
     * @param mergeable se false, cada c?lula vira seu pr?prio ret?ngulo 1x1
     *                  (sem merge) ? ?til pra tiles que n?o devem ser fundidas
     * @param skipId    id que deve ser ignorado por completo (ex: 0 = vazio)
     */
    public static Array<ClusterRect> findClusters(
        int[][] layerData,
        ClusterMatcher matcher,
        boolean mergeable,
        int skipId
    ) {
        Array<ClusterRect> result = new Array<>();

        int rows = layerData.length;
        if (rows == 0) return result;
        int cols = layerData[0].length;

        boolean[][] visited = new boolean[rows][cols];

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int originId = layerData[y][x];

                if (visited[y][x] || originId == skipId) continue;

                int width = 1;
                int height = 1;

                if (mergeable) {
                    // expande a largura enquanto o vizinho da direita casar
                    while (x + width < cols
                        && !visited[y][x + width]
                        && matcher.matches(originId, layerData[y][x + width])) {
                        width++;
                    }

                    // expande a altura enquanto TODA a linha de baixo casar
                    boolean canExpandDown = true;
                    while (canExpandDown && y + height < rows) {
                        for (int dx = 0; dx < width; dx++) {
                            int candidateId = layerData[y + height][x + dx];
                            if (visited[y + height][x + dx] || !matcher.matches(originId, candidateId)) {
                                canExpandDown = false;
                                break;
                            }
                        }
                        if (canExpandDown) height++;
                    }
                }

                for (int dy = 0; dy < height; dy++) {
                    for (int dx = 0; dx < width; dx++) {
                        visited[y + dy][x + dx] = true;
                    }
                }

                result.add(new ClusterRect(x, y, width, height));
            }
        }

        return result;
    }
}
