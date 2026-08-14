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
        JsonValue root = unwrapToJsonValue(data);
        return root.prettyPrint(JsonWriter.OutputType.json, 120);
    }

    private static JsonValue unwrapToJsonValue(Object value) {
        if (value instanceof SaveData) {
            JsonValue obj = new JsonValue(JsonValue.ValueType.object);
            Map<String, Object> raw = ((SaveData) value).raw();
            JsonValue prev = null;
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                JsonValue child = unwrapToJsonValue(entry.getValue());
                child.setName(entry.getKey());
                if (prev == null) obj.child = child;
                else prev.next = child;
                child.parent = obj;
                prev = child;
            }
            obj.size = raw.size();
            return obj;
        }

        if (value instanceof List) {
            JsonValue arr = new JsonValue(JsonValue.ValueType.array);
            List<?> list = (List<?>) value;
            JsonValue prev = null;
            for (Object item : list) {
                JsonValue child = unwrapToJsonValue(item);
                if (prev == null) arr.child = child;
                else prev.next = child;
                child.parent = arr;
                prev = child;
            }
            arr.size = list.size();
            return arr;
        }

        if (value instanceof String)  return new JsonValue((String) value);
        if (value instanceof Boolean) return new JsonValue((Boolean) value);
        if (value instanceof Float)   return new JsonValue(((Float) value).doubleValue());
        if (value instanceof Integer) return new JsonValue(((Integer) value).longValue());
        if (value instanceof Number)  return new JsonValue(((Number) value).doubleValue());

        return new JsonValue(JsonValue.ValueType.nullValue);
    }

    public static SaveData fromJson(String jsonText) {
        JsonValue root = new JsonReader().parse(jsonText);
        return SaveData.of(wrapObject(root));
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
