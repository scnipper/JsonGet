package me.creese.jsonget;

import me.creese.jsonget.annotation.JsonField;
import me.creese.jsonget.annotation.UseMethod;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class JsonG {

    private StringBuilder fabricJson;
    private boolean isOnlyPublic;
    private boolean isPretty;
    private int tabNum;

    public JsonG() {
        fabricJson = new StringBuilder();
        isPretty = false;
        tabNum = 0;
    }

    public String toJson(Object object) {

        Class classObject = object.getClass();
        Field[] fields;
        Method[] methods;


        if (isOnlyPublic) {
            methods = classObject.getMethods();
            fields = classObject.getFields();
        } else {
            fields = classObject.getDeclaredFields();
            methods = classObject.getDeclaredMethods();
        }


        Map<Object, Object> mapValues = new LinkedHashMap<>();

        for (int i = 0; i < fields.length; i++) {


            try {
                if (fields[i].isAnnotationPresent(JsonField.class)) {
                    JsonField name = fields[i].getAnnotation(JsonField.class);
                    mapValues.put(name.name(), fields[i].get(object));
                } else {
                    mapValues.put(fields[i].getName(), fields[i].get(object));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < methods.length; i++) {
            UseMethod nameMethod = methods[i].getAnnotation(UseMethod.class);
            if (nameMethod != null) {

                try {
                    if (nameMethod.name().equals("")) {
                        mapValues.put(methods[i].getName(), methods[i].invoke(object));
                    }
                    else {
                        mapValues.put(nameMethod.name(), methods[i].invoke(object));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        printBody(mapValues);

        return fabricJson.toString();
    }


    private void printBody(Map<Object, Object> mapValues) {
        tabNum += 1;
        fabricJson.append('{');

        if (isPretty) fabricJson.append('\n');

        for (Map.Entry<Object, Object> entry : mapValues.entrySet()) {


            if (isPretty) {
                for (int i = 0; i < tabNum; i++) {
                    fabricJson.append('\t');
                }

            }

            fabricJson.append('\"');
            fabricJson.append(entry.getKey());
            fabricJson.append('\"');
            fabricJson.append(':');

            if (isPretty) fabricJson.append(' ');

            if (entry.getValue() instanceof Object[]) {
                Object[] values = getArray(entry.getValue());
                printArray(values);
            } else {
                Object value = entry.getValue();
                boolean isNum = checkToNumber(value);

                if (value instanceof Map && !isNum) {
                    printBody((Map<Object, Object>) value);
                } else {

                    if (!isNum) fabricJson.append('\"');
                    fabricJson.append(value);
                    if (!isNum) fabricJson.append('\"');
                }
            }
            fabricJson.append(',');
            if (isPretty) fabricJson.append('\n');

        }

        if (isPretty) {
            fabricJson = fabricJson.delete(fabricJson.length() - 2, fabricJson.length());
            fabricJson.append('\n');
        } else {
            fabricJson = fabricJson.deleteCharAt(fabricJson.length() - 1);
        }

        tabNum--;
        if (isPretty) {
            for (int i = 0; i < tabNum; i++) {
                fabricJson.append('\t');
            }

        }
        fabricJson.append('}');

    }

    private boolean checkToNumber(Object value) {
        if (value instanceof Integer) {
            return true;
        } else return false;
    }

    private void printArray(Object[] values) {
        fabricJson.append('[');
        boolean isInnerArray = false;
        if (values.length > 0) {
            if (values[0] instanceof Object[]) {
                isInnerArray = true;
            }
        }
        for (int j = 0; j < values.length; j++) {


            if (isInnerArray) {
                printArray((Object[]) values[j]);
            } else {
                fabricJson.append('\"');
                fabricJson.append(values[j]);
                fabricJson.append('\"');
            }
            if (j < values.length - 1) {
                fabricJson.append(',');
                if (isPretty) fabricJson.append(' ');
            }

        }
        fabricJson.append(']');
    }

    private Object[] getArray(Object array) {
        Object[] array2 = new Object[Array.getLength(array)];
        for (int i = 0; i < array2.length; i++)
            array2[i] = Array.get(array, i);
        return array2;
    }

    public void setOnlyPublic(boolean onlyPublic) {
        isOnlyPublic = onlyPublic;
    }

    public void setPretty(boolean pretty) {
        isPretty = pretty;
    }
}
