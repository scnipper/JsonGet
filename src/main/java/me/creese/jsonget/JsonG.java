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
import me.creese.jsonget.exceptions.JsonFormatError;

import java.lang.reflect.*;
import java.util.*;

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

    public <T> T fromJson(String jsonString, Class castClass) {
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


        return (T) castClass.cast(instance);
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

        StringBuilder builder = new StringBuilder();

        ArrayList<String> strings = new ArrayList<>();

        int deep = 0;
        int deepArray = 0;


        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '{') {
                deep++;
                continue;
            }
           /* if (chars[i] == '}') {
                deep--;
            }*/

            if (chars[i] == '[') {
                deepArray++;
                continue;
            }

            if (chars[i] == ']') {
                deepArray--;
                if(deepArray == 0)
                continue;
            }

            if (deep > 0) {

                if (chars[i] == ',' || chars[i] == '}') {
                    if (deepArray == 0) {
                        String str = builder.toString();

                        //strings.add(builder.toString());

                        String[] spl = str.split(":");


                        spl[0] = spl[0].replace("\"", "");
                        Field field = null;
                        try {
                            field = classInstance.getDeclaredField(spl[0]);
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        }
                        if (spl[1].contains("\"")) {
                            spl[1] = spl[1].replace("\"", "");
                            if (spl[1].contains(",")) {
                                if(spl[1].contains("]")) {
                                    String[][] arr = null;
                                    String[] tmp = spl[1].split("]");

                                    for (int i1 = 0; i1 < tmp.length; i1++) {

                                        String[] ts = tmp[i1].split(",");
                                        if(arr == null) {
                                            arr = new String[tmp.length][ts.length];
                                        }
                                        arr[i1] = ts;

                                    }
                                    try {
                                        field.set(instance, arr);
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                                else {
                                    String[] arr = spl[1].split(",");
                                    try {
                                        field.set(instance, arr);
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                            else {
                                try {
                                    field.set(instance, spl[1]);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            if (spl[1].contains(",")) {
                                String[] arr = spl[1].split(",");
                                Integer[] iarr = new Integer[arr.length];
                                for (int i1 = 0; i1 < iarr.length; i1++) {
                                    iarr[i1] = Integer.valueOf(arr[i1]);
                                }
                                try {
                                    field.set(instance, iarr);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                try {
                                    field.set(instance, Integer.valueOf(spl[1]));
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                        }


                        builder = new StringBuilder();
                        continue;
                    }

                }


                if (chars[i] != 10 && chars[i] != 9 && chars[i] != 13 && chars[i] != 32) {
                    builder.append(chars[i]);
                }


            }


        }



        /*for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '{') isFirstBrace = true;
            if (chars[i] == '"' || chars[i] == ':') {
                if (!isFirstBrace) throw new JsonFormatError("Don't find \"{\"");

                if(i > 0 && ch)

                startRead = !startRead;
                if (startRead)
                    continue;
            }

            if (startRead) {
                builder.append(chars[i]);

            } else {
                if (builder.length() > 0) {
                    if (field == null) {
                        try {
                            field = classInstance.getDeclaredField(builder.toString());
                            builder = new StringBuilder();
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            if (chars[i-(builder.length()+1)] == '\"') {
                                field.set(instance, builder.toString());
                            } else {
                                System.out.println(builder);
                                if (builder.toString().equals("true")) {
                                    field.set(instance, true);
                                } else if (builder.toString().equals("false")) {
                                    field.set(instance, false);
                                } else if (builder.toString().equals("null")) {
                                    field.set(instance, null);
                                } else if (builder.indexOf(".") != -1) {
                                    field.set(instance, Float.valueOf(builder.toString()));
                                } else {
                                    field.set(instance, Integer.valueOf(builder.toString()));
                                }

                            }

                            field = null;
                            builder = new StringBuilder();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }*/


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
