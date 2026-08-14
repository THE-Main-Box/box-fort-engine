package official.sketchBook.engine.util_related.serialization.persistance;

import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;

import java.util.ArrayList;
import java.util.List;

/**
 * Convers?o entre arrays de int (1D/2D/3D) e a estrutura de SaveData ?
 * usa getIntList/putIntList como base (sem wrapper por valor escalar),
 * s? aninhando listas pra dimens?es maiores.
 */
public class SaveDataArrayUtil {

    private static final String ITEMS_KEY = "items";

    public static SaveData toSaveData(int[] array) {
        List<Integer> list = new ArrayList<>(array.length);
        for (int value : array) list.add(value);

        return new SaveData().putIntList(ITEMS_KEY, list);
    }

    public static int[] fromSaveData1D(SaveData data) {
        List<Integer> list = data.getIntList(ITEMS_KEY);
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) result[i] = list.get(i);
        return result;
    }

    public static SaveData toSaveData(int[][] array) {
        List<SaveData> list = new ArrayList<>(array.length);
        for (int[] row : array) list.add(toSaveData(row));

        return new SaveData().putList(ITEMS_KEY, list);
    }

    public static int[][] fromSaveData2D(SaveData data) {
        List<SaveData> list = data.getList(ITEMS_KEY);
        int[][] result = new int[list.size()][];
        for (int i = 0; i < list.size(); i++) result[i] = fromSaveData1D(list.get(i));
        return result;
    }

    public static SaveData toSaveData(int[][][] array) {
        List<SaveData> list = new ArrayList<>(array.length);
        for (int[][] layer : array) list.add(toSaveData(layer));

        return new SaveData().putList(ITEMS_KEY, list);
    }

    public static int[][][] fromSaveData3D(SaveData data) {
        List<SaveData> list = data.getList(ITEMS_KEY);
        int[][][] result = new int[list.size()][][];
        for (int i = 0; i < list.size(); i++) result[i] = fromSaveData2D(list.get(i));
        return result;
    }
}
