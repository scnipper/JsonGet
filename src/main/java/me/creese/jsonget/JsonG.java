package me.creese.jsonget;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

public class JsonG {

    private StringBuilder fabricJson;

    public JsonG() {
        fabricJson = new StringBuilder();
    }

    public String toJson(Object object) {

        Class classObject = object.getClass();

        fabricJson.append('{');

        Field[] fields = classObject.getFields();
        for (int i = 0;i<fields.length;i++) {


            try {

                fabricJson.append('\"');
                fabricJson.append(fields[i].getName());
                fabricJson.append('\"');
                fabricJson.append(':');



                if(fields[i].getType().isArray()) {
                    Object[] values = getArray(fields[i].get(object));
                    fabricJson.append('[');
                    for (int j = 0; j < values.length; j++) {

                        fabricJson.append('\"');
                        fabricJson.append(values[j]);
                        fabricJson.append('\"');
                        if(j < values.length-1) fabricJson.append(',');

                    }
                    fabricJson.append(']');
                }
                else {
                    fabricJson.append('\"');
                    fabricJson.append(fields[i].get(object));
                    fabricJson.append('\"');
                }
                if(i < fields.length-1) fabricJson.append(',');

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
        fabricJson.append('}');

        return fabricJson.toString();
    }

    private Object[] getArray(Object array) {
        Object[] array2 = new Object[Array.getLength(array)];
        for(int i=0;i<array2.length;i++)
            array2[i] = Array.get(array, i);
        return array2;
    }


}
