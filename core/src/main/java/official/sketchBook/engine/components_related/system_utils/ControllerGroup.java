package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ControllableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WirableConfigurable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WiringConfig;

import java.util.ArrayList;
import java.util.IdentityHashMap;

public class ControllerGroup {

    /// Nome do grupo para identifica��o no HUD
    public final String name;

    /// Lista de control�veis do grupo
    private final ArrayList<ControllableObjectII> targets;

    /// Map de configs por objeto — um objeto s� pode ter uma config por grupo
    /// IdentityHashMap usa refer�ncia direta — sem equals(), sem autoboxing
    private final IdentityHashMap<ControllableObjectII, WiringConfig> configMap;

    public ControllerGroup(String name) {
        this.name = name;
        this.targets = new ArrayList<>();
        this.configMap = new IdentityHashMap<>();
    }

    /// Adiciona um objeto ao grupo.
    /// Se for WirableConfigurable, registra sua config padr�o automaticamente.
    public void add(ControllableObjectII object) {
        if (object == null) return;
        targets.add(object);

        /// Registra a config padr�o caso o objeto seja configur�vel
        if (object instanceof WirableConfigurable) {
            WiringConfig defaultConfig = ((WirableConfigurable) object).getCurrentConfiguration();
            if (defaultConfig != null) {
                configMap.put(object, defaultConfig);
            }
        }
    }

    /// Atualiza a config de um objeto neste grupo.
    /// Usa o map diretamente — O(1) via refer�ncia, sem iterar a lista.
    public void setConfig(ControllableObjectII object, WiringConfig config) {
        if (!configMap.containsKey(object)) return;
        configMap.put(object, config);
    }

    /// Aciona todos os objetos do grupo.
    /// Se configur�vel e houver config no map, aplica antes de acionar.
    public void trigger() {
        for (int i = 0; i < targets.size(); i++) {
            ControllableObjectII target = targets.get(i);

            if (target instanceof WirableConfigurable) {
                WiringConfig config = configMap.get(target);
                if (config != null) {
                    ((WirableConfigurable) target).setCurrentConfiguration(config);
                }
            }

            target.interactByWiring();
        }
    }

    public void remove(ControllableObjectII object) {
        targets.remove(object);
        configMap.remove(object);
    }

    public void clear() {
        targets.clear();
        configMap.clear();
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    public WiringConfig getConfig(ControllableObjectII object) {
        return configMap.get(object);
    }

    public ArrayList<ControllableObjectII> getTargets() {
        return targets;
    }
}
