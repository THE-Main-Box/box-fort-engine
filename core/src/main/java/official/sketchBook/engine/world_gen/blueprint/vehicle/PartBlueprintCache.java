package official.sketchBook.engine.world_gen.blueprint.vehicle;

import com.badlogic.gdx.utils.OrderedMap;

/**
 * Cache de SubmarinePartBlueprint por tag, usado durante a geração de UM
 * node — evita reler/parsear o mesmo JSON de part quando o node
 * referencia a mesma tag várias vezes (ex: 2x "corridor" em posições
 * diferentes).
 * <p>
 * Escopo de vida: criado no início da geração de um node, descartado no
 * fim — não é um cache global persistente. Limite de tamanho existe só
 * como guarda-corpo contra nodes com uma quantidade anormal de tags
 * distintas; não é um cache de longa duração então uma política de
 * despejo simples (remove o mais antigo) é suficiente, sem necessidade
 * de LRU real.
 */
public class PartBlueprintCache {

    private static final int DEFAULT_MAX_SIZE = 32;

    private final OrderedMap<String, SubmarinePartBlueprint> cache = new OrderedMap<>();
    private final int maxSize;

    public PartBlueprintCache() {
        this(DEFAULT_MAX_SIZE);
    }

    public PartBlueprintCache(int maxSize) {
        this.maxSize = maxSize;
    }

    public SubmarinePartBlueprint getOrLoad(String partTag, PartBlueprintLoader loader) {
        SubmarinePartBlueprint cached = cache.get(partTag);
        if (cached != null) return cached;

        SubmarinePartBlueprint loaded = loader.load(partTag);

        if (cache.size >= maxSize) {
            // despeja o mais antigo (primeira chave inserida) — OrderedMap
            // preserva ordem de inserção, então orderedKeys().first() é o
            // candidato correto sem precisar de estrutura extra tipo LinkedHashMap
            String oldestKey = cache.orderedKeys().first();
            cache.remove(oldestKey);
        }

        cache.put(partTag, loaded);
        return loaded;
    }

    public interface PartBlueprintLoader {
        SubmarinePartBlueprint load(String partTag);
    }
}
