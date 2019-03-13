/*
 * Copyright 2018 creese
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.creese.jsonget;

import me.creese.jsonget.annotation.JsonField;
import me.creese.jsonget.annotation.UseConstructor;
import me.creese.jsonget.annotation.UseMethod;

import java.lang.reflect.*;
import java.util.*;

public class JsonG {

    private StringBuilder fabricJson;
    private boolean isOnlyPublic;
    private boolean isPretty;
    private int tabNum;
    private StringBuilder builder;
    private int index;

    public JsonG() {
        fabricJson = new StringBuilder();
        isPretty = false;
        tabNum = 0;
    }

    public <T> T fromJson(String jsonString, Class<T> castClass) {
        Object instance = null;
        Constructor[] constructors = castClass.getConstructors();
        if (constructors.length > 1) {
            for (Constructor constructor : constructors) {
                if (constructor.isAnnotationPresent(UseConstructor.class)) {
                    instance = createInstance(constructor, castClass);
                    break;
                }
            }
            if (instance == null) {
                instance = createInstance(castClass.getConstructors()[0], castClass);
            }

        } else {
            instance = createInstance(castClass.getConstructors()[0], castClass);
        }


        parseJson(instance, jsonString, castClass);


        return castClass.cast(instance);
    }

    private Object createInstance(Constructor constructor, Class castClass) {
        Parameter[] parameters = constructor.getParameters();
        if (parameters.length > 0) {
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getType() == int.class ||
                        parameters[i].getType() == float.class ||
                        parameters[i].getType() == long.class ||
                        parameters[i].getType() == short.class ||
                        parameters[i].getType() == double.class ||
                        parameters[i].getType() == byte.class ||
                        parameters[i].getType() == char.class) {
                    args[i] = 0;
                } else if (parameters[i].getType() == boolean.class) {
                    args[i] = false;
                } else args[i] = null;
            }

            try {
                return constructor.newInstance(args);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            try {
                return castClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
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
                    } else {
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

    private void parseJson(Object instance, String json, Class classInstance) {
        char[] chars = new char[json.length()];
        json.getChars(0, json.length(), chars, 0);


        boolean isFirstBrace = false;
        boolean startRead = false;
        builder = new StringBuilder();
        /*Map<Object, Object> rootMap = generateRoot(chars);

        for (Map.Entry<Object, Object> entry : rootMap.entrySet()) {
            Field field = null;
            try {
                field = classInstance.getDeclaredField(String.valueOf(entry.getKey()));
            } catch (NoSuchFieldException e) {
                for (Field field1 : classInstance.getDeclaredFields()) {
                    if (field1.isAnnotationPresent(JsonField.class)) {
                        if(field1.getAnnotation(JsonField.class).name().equals(entry.getKey())) {
                            field = field1;
                            break;
                        }
                    }
                }

            }

            try {
                field.set(instance, entry.getValue());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }*/

    }

    private JsonEntity generateRoot(char[] chars) {


        while (index < chars.length) {
            char currChar = chars[index];

            if (currChar == '[') {

                index++;
                return readArray(chars);
            }
            if (currChar == '{') {

                index++;
                return readObject(chars);
            }

        }

        return null;
    }

    private JsonEntity readArray(char[] chars) {
        StringBuilder sb = new StringBuilder();
        JsonArray jsonArray = new JsonArray();

        while (index < chars.length) {
            char currChar = chars[index];


            switch (currChar) {
                case '[':
                    index++;
                    jsonArray.add(readArray(chars));
                    break;

                case '{':
                    index++;
                    jsonArray.add(readObject(chars));
                    break;
                case ']':
                    String s = sb.toString();
                    String[] split = s.split(",");

                    for (int i = 0; i < split.length; i++) {
                        if(!split[i].equals(""))
                            jsonArray.add(new JsonEntity(split[i]));
                    }
                    return jsonArray;
                default:
                    if (currChar != '\n' && currChar != ' ' && currChar != '"') {
                        sb.append(currChar);
                    }

            }
            index++;

        }
        return jsonArray;
    }

    private JsonEntity readObject(char[] chars) {
        StringBuilder sb = new StringBuilder();
        JsonObject jsonObject = new JsonObject();

        while (index < chars.length) {
            char currChar = chars[index];

            switch (currChar) {
                case '[':


                    if (index > 0) {
                        StringBuilder str = new StringBuilder();
                        for (int i = index; i >= 0; i--) {
                            char ch = chars[i];

                            if (ch == ',' || ch == '{'){
                                sb = sb.deleteCharAt(sb.length()-1);
                                break;
                            }
                            if (ch != ':' && ch != '"' && ch != '\n' && ch != ' ' && ch != '[') {
                                str.append(ch);

                                sb = sb.deleteCharAt(sb.length()-1);
                            }

                        }

                        if(sb.length() > 0) {
                            if (sb.charAt(sb.length() - 1) == ',') {
                                sb = sb.deleteCharAt(sb.length() - 1);
                            }
                        }
                        index++;
                        JsonEntity value = readArray(chars);
                        jsonObject.addField(str.reverse().toString(), value);


                    }
                    break;

                case '{':
                    if (index > 0) {
                        StringBuilder str = new StringBuilder();
                        for (int i = index; i >= 0; i--) {
                            char ch = chars[i];

                            if (ch == ','){
                                sb = sb.deleteCharAt(sb.length()-1);
                                break;
                            }
                            if (ch != ':' && ch != '"' && ch != '\n' && ch != ' ' && ch != '['&& ch != '{' ) {
                                str.append(ch);

                                sb = sb.deleteCharAt(sb.length()-1);
                            }

                        }

                        if(sb.length() > 0) {
                            if (sb.charAt(sb.length() - 1) == ',') {
                                sb = sb.deleteCharAt(sb.length() - 1);
                            }
                        }
                        index++;
                        JsonEntity value = readObject(chars);
                        jsonObject.addField(str.reverse().toString(), value);


                    }
                    break;
                case '}':
                    String s = sb.toString();
                    String[] split = s.split(",");

                    for (int i = 0; i < split.length; i++) {

                        if(!split[i].equals("")) {
                            String[] pair = split[i].split(":");

                            jsonObject.addField(pair[0], new JsonEntity(pair[1]));
                        }
                    }
                    return jsonObject;
                default:
                    if (currChar != '\n' && currChar != ' ' && currChar != '"') {
                        sb.append(currChar);
                    }

            }

            index++;

        }
        return jsonObject;
    }

    private void appendChar(char aChar) {
        if (aChar != 10 && aChar != 9 && aChar != 13 && aChar != 32) {
            builder.append(aChar);
        }
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
