package official.sketchBook.engine.util_related.serialization;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * �nica borda entre {@link SaveData} (formato de trabalho em mem�ria) e
 * texto JSON (formato de arquivo). Nenhuma outra classe do sistema
 * deveria saber que JSON existe.
 * <p>
 * Usa {@code com.badlogic.gdx.utils.Json}, j� dispon�vel no classpath
 * do libGDX ? nenhuma depend�ncia externa nova.
 */
public final class SaveDataJson {

    private SaveDataJson() {
    }

    public static String toJson(SaveData data) {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        Object plain = unwrap(data);
        return json.toJson(plain, plain.getClass());
    }

    public static SaveData fromJson(String jsonText) {
        JsonValue root = new JsonReader().parse(jsonText);
        return SaveData.of(wrapObject(root));
    }

    @SuppressWarnings("unchecked")
    private static Object unwrap(Object value) {
        if (value instanceof SaveData) {
            Map<String, Object> raw = ((SaveData) value).raw();
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                out.put(entry.getKey(), unwrap(entry.getValue()));
            }
            return out;
        }

        if (value instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                out.add(unwrap(item));
            }
            return out;
        }

        return value;
    }

    private static Map<String, Object> wrapObject(JsonValue node) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (JsonValue child = node.child; child != null; child = child.next) {
            out.put(child.name, wrapValue(child));
        }
        return out;
    }

    private static Object wrapValue(JsonValue node) {
        if (node.isObject()) {
            return SaveData.of(wrapObject(node));
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonValue child = node.child; child != null; child = child.next) {
                list.add(wrapValue(child));
            }
            return list;
        }
        if (node.isNumber()) {
            return node.asFloat();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isString()) {
            return node.asString();
        }
        return null;
    }
}
