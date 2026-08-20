package official.sketchBook.engine.components_related.vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registro central de f?bricas de VehicleBaseComponent, por type key.
 * Mesmo padr?o de SaveDataInstanceRegistry, mas dedicado a componentes
 * de ve?culo ? o contrato de reconstru??o ? diferente (attach + init
 * em vez de newInstance com contexto), ent?o n?o faz sentido reusar o
 * registry de save top-level pra isso.
 */
public class VehicleComponentTypeRegistry {

    public static final VehicleComponentTypeRegistry GLOBAL = new VehicleComponentTypeRegistry();

    private final Map<String, Supplier<VehicleBaseComponent>> factories = new HashMap<>();

    public void register(String key, Supplier<VehicleBaseComponent> factory) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Type key não pode ser vazia");
        }
        if (factories.containsKey(key)) {
            throw new IllegalStateException(
                "VehicleBaseComponent já registrado pra key: " + key + " — use uma key diferente."
            );
        }
        factories.put(key, factory);
    }

    public VehicleBaseComponent create(String key) {
        Supplier<VehicleBaseComponent> factory = factories.get(key);
        if (factory == null) {
            throw new IllegalStateException("Nenhum VehicleBaseComponent registrado pra key: " + key);
        }
        return factory.get();
    }

    public boolean isRegistered(String key) {
        return factories.containsKey(key);
    }
}
