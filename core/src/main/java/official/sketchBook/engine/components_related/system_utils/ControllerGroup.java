package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ControllableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.SerializableWirableConfigurable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WirableConfigurable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.WiringConfig;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;
import official.sketchBook.engine.util_related.serialization.instantiation.EmbeddedSaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ControllerGroup implements EmbeddedSaveData {

    public String name;

    private final ArrayList<ControllableObjectII> targets;
    private final IdentityHashMap<ControllableObjectII, WiringConfig> configMap;

    /// Estado bruto entre load() e resolveReferences() ? nunca consultado
    /// depois que a resolu??o termina (targets/configMap j? cont?m tudo).
    private List<String> pendingTargetIds;
    private SaveData pendingConfigs;

    public ControllerGroup(String name) {
        this.name = name;
        this.targets = new ArrayList<>();
        this.configMap = new IdentityHashMap<>();
    }

    /// Construtor vazio ? exigido pra reflection (getEmbeddedList).
    public ControllerGroup() {
        this.targets = new ArrayList<>();
        this.configMap = new IdentityHashMap<>();
    }

    public void add(ControllableObjectII object) {
        if (object == null) return;
        targets.add(object);

        if (object instanceof WirableConfigurable) {
            WiringConfig defaultConfig = ((WirableConfigurable) object).getCurrentConfiguration();
            if (defaultConfig != null) {
                configMap.put(object, defaultConfig);
            }
        }
    }

    public void setConfig(ControllableObjectII object, WiringConfig config) {
        if (object instanceof WirableConfigurable) {
            configMap.put(object, config);
        }
    }

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

    // ============================================================
    // SERIALIZA??O ? config indexada por id, "existe ou n?o" via has()
    // ============================================================

    @Override
    public SaveData toSaveData() {
        SaveData data = new SaveData().put("name", name);

        List<String> ids = new ArrayList<>(targets.size());
        SaveData configsBlock = new SaveData();

        for (int i = 0; i < targets.size(); i++) {
            ControllableObjectII target = targets.get(i);

            // s? sabemos extrair id de VehicleBaseComponent ? targets fora
            // desse universo simplesmente n?o sobrevivem ao save
            if (!(target instanceof VehicleBaseComponent)) continue;

            String targetId = ((VehicleBaseComponent) target).getId();
            ids.add(targetId);

            if (target instanceof SerializableWirableConfigurable) {
                WiringConfig groupConfig = configMap.get(target); // a config DESTE grupo, não a atual do componente
                if (groupConfig != null) {
                    SaveData configData = ((SerializableWirableConfigurable) target).saveWiringConfig(groupConfig);
                    if (configData != null) {
                        configsBlock.put(targetId, configData);
                    }
                }
            }
        }

        data.putStringList("target_ids", ids);
        data.put("configs", configsBlock);
        return data;
    }

    @Override
    public void load(SaveData data) {
        this.name = data.getStringRequired("name");
        this.pendingTargetIds = data.getStringList("target_ids");
        this.pendingConfigs = data.getSaveData("configs"); // nunca null, pode vir vazio
    }

    /**
     * Resolve os ids pendentes para objetos reais, usando um mapa j?
     * montado com TODOS os componentes do submarino (cross-node
     * permitido). Chamado depois que todo o submarino j? existe.
     * Ids n?o encontrados no mapa s?o ignorados silenciosamente (target
     * pode ter sido removido/renomeado entre saves).
     */
    public void resolveReferences(Map<String, VehicleBaseComponent> byId) {
        if (pendingTargetIds == null) return;

        for (int i = 0; i < pendingTargetIds.size(); i++) {
            String targetId = pendingTargetIds.get(i);

            VehicleBaseComponent resolved = byId.get(targetId);
            if (!(resolved instanceof ControllableObjectII)) continue;

            ControllableObjectII target = (ControllableObjectII) resolved;
            add(target);

            if (pendingConfigs.has(targetId) && target instanceof SerializableWirableConfigurable) {
                SaveData configData = pendingConfigs.getSaveData(targetId);
                ((SerializableWirableConfigurable) target).loadWiringConfig(configData);
                setConfig(target, ((WirableConfigurable) target).getCurrentConfiguration());
            }
        }

        pendingTargetIds = null;
        pendingConfigs = null;
    }
}
