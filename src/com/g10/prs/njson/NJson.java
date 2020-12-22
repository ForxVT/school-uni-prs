package com.g10.prs.njson;

import com.g10.prs.option.Runnable;
import com.g10.prs.common.PrsException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.Class;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/** Contains utility functions to deserialize and serialize NJson data format files. */
public class NJson {
    /**
     * Deserialize a NJson file.
     *
     * @param filePath The file path to use.
     * @param classType The Class to create.
     * @param <T> The type to return.
     *
     * @return the class created of type T.
     */
    public static <T> T deserialize(String filePath, Class<T> classType) throws PrsException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, IOException, NJSonCannotParseException, ClassNotFoundException {
        checkSerializable(classType);

        return deserialize(new NJsonReader(filePath).readMap(), classType);
    }

    public static <T> T deserialize(Map<String, Object> njson, Class<T> classType) throws PrsException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
        checkSerializable(classType);

        T object = classType.getConstructor().newInstance();

        ArrayList<Field> fields = new ArrayList<>(Arrays.asList(Runnable.class.getDeclaredFields()));
        fields.addAll(Arrays.asList(classType.getDeclaredFields()));
        fields.removeIf(field -> !field.isAnnotationPresent(NJsonSerializable.class));

        for (Field field : fields) {
            NJsonSerializable serializable = field.getAnnotation(NJsonSerializable.class);
            String key = serializable.path().isEmpty() ? field.getName() : serializable.path();
            String[] path = key.split("\\.");
            Map<String, Object> mapToSearch = njson;

            if (path.length > 1) {
                for (int i = 0; i < path.length - 1; i++) {
                    if (mapToSearch.get(path[i]) instanceof Map) {
                        mapToSearch = (Map<String, Object>)mapToSearch.get(path[i]);
                    } else {
                        throw new PrsException("Wrong path!");
                    }
                }
            }

            field.setAccessible(true);

            if (!mapToSearch.containsKey(path[path.length - 1])) {
                if (serializable.necessary()) {
                    throw new PrsException("Missing key '" + key + "'!");
                } else {
                    if (field.getType().equals(boolean.class)) {
                        field.set(object, false);
                    } else if (field.getType().equals(String.class)) {
                        field.set(object, "");
                    } else if (field.getType().equals(Integer.class)) {
                        field.set(object, 0);
                    } else if (field.getType().equals(Double.class)) {
                        field.set(object, 0.0);
                    } else {
                        field.set(object, null);
                    }
                    continue;
                }
            }

            Object value = mapToSearch.get(path[path.length - 1]);

            if (field.getType().isAnnotationPresent(NJsonSerializable.class)) {
                field.set(object, deserialize((Map<String, Object>)value, field.getType()));

                continue;
            } else if (field.getType().isAssignableFrom(List.class)) {
                ParameterizedType parameterizedType = (ParameterizedType)field.getGenericType();
                Type tType = parameterizedType.getActualTypeArguments()[0];

                if (!tType.getTypeName().contains("<") && Class.forName(tType.getTypeName()).isAnnotationPresent(NJsonSerializable.class)) {
                    List<Object> list = new ArrayList<>();

                    for (Object element : (List<Object>)value) {
                        list.add(deserialize((Map<String, Object>)element, Class.forName(tType.getTypeName())));
                    }

                    field.set(object, list);

                    continue;
                }
            }

            field.set(object, mapToSearch.get(path[path.length - 1]));
        }

        return object;
    }

    /**
     * Serialize a NJson file.
     *
     * @param filePath The file path to use.
     * @param object The object to serialize.
     * @param <T> The type of the object.
     */
    public static <T> void serialize(String filePath, T object) throws PrsException, IllegalAccessException, FileNotFoundException {
        Class<T> classType = (Class<T>) object.getClass();
        checkSerializable(classType);

        Map<String, Object> njson = new LinkedHashMap<>();

        ArrayList<Field> fields = new ArrayList<>(Arrays.asList(Runnable.class.getDeclaredFields()));
        fields.addAll(Arrays.asList(classType.getDeclaredFields()));
        fields.removeIf(field -> !field.isAnnotationPresent(NJsonSerializable.class));

        for (Field field : fields) {
            NJsonSerializable serializable = field.getAnnotation(NJsonSerializable.class);
            String key = serializable.path().isEmpty() ? field.getName() : serializable.path();
            String[] path = key.split("\\.");
            field.setAccessible(true);
            addToMap(njson, path, field.get(object));
        }

        NJsonWriter.write(filePath, njson);
    }

    /**
     * Add an object to a map.
     *
     * @param map The map to insert in.
     * @param path The path to search in.
     * @param value The value to add.
     */
    private static void addToMap(Map<String, Object> map, String[] path, Object value) {
        if (path.length == 1) {
            map.put(path[0], value);
            return;
        } else if (!map.containsKey(path[0])) {
            addToMap(map, new String[] {path[0]}, new LinkedHashMap<String, Object>());
        }

        String[] newPath = new String[path.length - 1];
        System.arraycopy(path, 1, newPath, 0, path.length - 1);
        addToMap((Map<String, Object>) map.get(path[0]), newPath, value);
    }

    /**
     * Check if a class is serializable.
     *
     * @param classType The Java Class to use.
     * @param <T> The class's type.
     */
    public static <T> void checkSerializable(Class<T> classType) throws PrsException {
        if (!classType.isAnnotationPresent(NJsonSerializable.class)) {
            throw new PrsException("Not a serializable class!");
        }
    }
}
