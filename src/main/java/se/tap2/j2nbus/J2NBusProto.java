package se.tap2.j2nbus;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * This is a helper class used by J2NBus internally to serialize and deserialize objects
 */
class J2NBusProto {

    public static byte[] serializeDataObject(Object o) throws IllegalArgumentException, IllegalAccessException {

        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4096]);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        String string = o.getClass().getName();

        byteBuffer.putInt(string.getBytes().length);
        byteBuffer.put(string.getBytes(), 0, string.getBytes().length);

        writeObject(o, byteBuffer);

        return byteBuffer.array();
    }

    public static Object deserializeDataObject(String typeName, ByteBuffer byteBuffer) throws ClassNotFoundException, InstantiationException, IllegalAccessException {


        Class<?> loadClass = J2NBus.class.getClassLoader().loadClass(typeName);

        Object object = loadClass.newInstance();
        readObject(object, byteBuffer);
        return object;
    }

    private static void checkObjectIsData(Object o) {
        Data dataAnnotation = o.getClass().getAnnotation(Data.class);
        if(dataAnnotation == null) {
            throw new IllegalArgumentException("Objects posted must be annotated with " + Data.class.getName());
        }

    }

    private static void writeObject(Object o, ByteBuffer byteBuffer) throws IllegalArgumentException, IllegalAccessException {
        checkObjectIsData(o);

        Field[] fields = o.getClass().getDeclaredFields();

        Arrays.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field field, Field field2) {
                return field.getName().compareTo(field2.getName());
            }
        });


        for (Field field : fields) {
            if(Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            if (field.getType().isPrimitive()) {
                writePrimitive(o, field, byteBuffer);
            } else if(field.getType().isArray()) {
                writePrimitiveArray(o, field, byteBuffer);
            }else if (field.getType() == String.class) {
                writeString(o, field, byteBuffer);
            } else {
                writeObject(field.get(o), byteBuffer);
            }
        }

    }

    private static void writePrimitiveArray(Object o, Field field, ByteBuffer byteBuffer) throws IllegalAccessException {
        if(field.getType().getName().equals("[I")) {
            int[] a = (int[])field.get(o);
            byteBuffer.putInt(a.length);
            byteBuffer.asIntBuffer().put(a);

        } else if(field.getType().getName().equals("[B")) {
            byte[] a = (byte[])field.get(o);
            byteBuffer.putInt(a.length);
            byteBuffer.put(a);

        } else if(field.getType().getName().equals("[S")) {
            short[] a = (short[])field.get(o);
            byteBuffer.putInt(a.length);
            byteBuffer.asShortBuffer().put(a);

        } else if(field.getType().getName().equals("[J")) {
            long[] a = (long[])field.get(o);
            byteBuffer.putInt(a.length);
            byteBuffer.asLongBuffer().put(a);

        } else if(field.getType().getName().equals("[F")) {
            float[] a = (float[])field.get(o);
            byteBuffer.putInt(a.length);
            byteBuffer.asFloatBuffer().put(a);

        } else if(field.getType().getName().equals("[D")) {
            double[] a = (double[])field.get(o);
            byteBuffer.putInt(a.length);
            byteBuffer.asDoubleBuffer().put(a);

        } else if(field.getType().getName().equals("[Z")) {
            boolean[] a = (boolean[])field.get(o);
            byteBuffer.putInt(a.length);
            for (int i = 0 ; i < a.length; i++) {
                byteBuffer.put(a[i] ? (byte) 1 : (byte) 0);
            }

        } else {
            throw new RuntimeException("Primitive array type not supported: " + field.getType());
        }
    }

    private static void writeString(Object o, Field field, ByteBuffer byteBuffer)
            throws IllegalAccessException {
        String string = (String) field.get(o);
        byteBuffer.putInt(string.getBytes().length);
        byteBuffer.put(string.getBytes(), 0, string.getBytes().length);
    }

    private static void writePrimitive(Object o, Field field, ByteBuffer byteBuffer) throws IllegalArgumentException, IllegalAccessException {
        if (field.getType() == Byte.TYPE) {
            byteBuffer.put(field.getByte(o));
        } else if (field.getType() == Short.TYPE) {
            byteBuffer.putShort(field.getShort(o));
        } else if (field.getType() == Integer.TYPE) {
            byteBuffer.putInt(field.getInt(o));
        } else if (field.getType() == Long.TYPE) {
            byteBuffer.putLong(field.getLong(o));
        } else if (field.getType() == Float.TYPE) {
            byteBuffer.putFloat(field.getFloat(o));
        } else if (field.getType() == Double.TYPE) {
            byteBuffer.putDouble(field.getDouble(o));
        } else if (field.getType() == Boolean.TYPE) {
            byteBuffer.put(field.getBoolean(o) ? (byte) 1 : (byte) 0);
        } else {
            throw new RuntimeException("Primitive type not supported: " + field.getType());
        }

    }

    private static void readPrimitiveArray(Object o, Field field, ByteBuffer byteBuffer) throws IllegalAccessException {

        int size = byteBuffer.getInt();
        if(field.getType().getName().equals("[I")) {
            int[] a = new int[size];
            byteBuffer.asIntBuffer().get(a);
            field.set(o, a);

        } else if(field.getType().getName().equals("[B")) {
            byte[] a = new byte[size];
            byteBuffer.get(a);
            field.set(o, a);

        } else if(field.getType().getName().equals("[S")) {
            short[] a = new short[size];
            byteBuffer.asShortBuffer().get(a);
            field.set(o, a);

        } else if(field.getType().getName().equals("[J")) {
            long[] a = new long[size];
            byteBuffer.asLongBuffer().get(a);
            field.set(o, a);

        } else if(field.getType().getName().equals("[F")) {
            float[] a = new float[size];
            byteBuffer.asFloatBuffer().get(a);
            field.set(o, a);

        } else if(field.getType().getName().equals("[D")) {
            double[] a = new double[size];
            byteBuffer.asDoubleBuffer().get(a);
            field.set(o, a);

        } else if(field.getType().getName().equals("[Z")) {
            boolean[] a = new boolean[size];
            for (int i = 0 ; i < a.length; i++) {
                a[i] = byteBuffer.get() > 0;
            }
            field.set(o, a);

        } else {
            throw new RuntimeException("Primitive array type not supported: " + field.getType());
        }
    }


    private static void readObject(Object o, ByteBuffer byteBuffer) throws InstantiationException, IllegalAccessException {
        Field[] fields = o.getClass().getDeclaredFields();

        Arrays.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field field, Field field2) {
                return field.getName().compareTo(field2.getName());
            }
        });

        for (Field field : fields) {
            if(Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            if (field.getType().isPrimitive()) {
                readPrimitive(o, field, byteBuffer);
            } else if (field.getType() == String.class) {
                readString(o, field, byteBuffer);
            } else if (field.getType().isArray()) {
                readPrimitiveArray(o, field, byteBuffer);
            } else if (field.getType() == List.class) {
                readObjectList(o, field, byteBuffer);
            }  else {
                Object fieldObject = field.getType().newInstance();
                readObject(fieldObject, byteBuffer);
                field.set(o, fieldObject);
            }
        }

    }

    private static void readObjectList(Object o, Field field, ByteBuffer byteBuffer) throws IllegalAccessException, InstantiationException {
        int size = byteBuffer.getInt();
        List list = new ArrayList(size);
        ParameterizedType listType = (ParameterizedType) field.getGenericType();
        Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
        for (int i = 0; i < size; i++) {
            Object elem = listClass.newInstance();
            readObject(elem, byteBuffer);
            list.add(elem);
        }
        field.set(o, list);
    }

    private static void readString(Object o, Field field,
                                  ByteBuffer byteBuffer) throws IllegalArgumentException, IllegalAccessException {
        int stringSize = byteBuffer.getInt();
        byte[] stringBytes = new byte[stringSize];
        byteBuffer.get(stringBytes);
        field.set(o, new String(stringBytes));

    }

    private static void readPrimitive(Object o, Field field,
                                     ByteBuffer byteBuffer) throws IllegalArgumentException, IllegalAccessException {

        if (field.getType() == Byte.TYPE) {
            field.setByte(o, byteBuffer.get());
        } else if (field.getType() == Short.TYPE) {
            field.setShort(o, byteBuffer.getShort());
        } else if (field.getType() == Integer.TYPE) {
            field.setInt(o, byteBuffer.getInt());
        } else if (field.getType() == Long.TYPE) {
            field.setLong(o, byteBuffer.getLong());
        } else if (field.getType() == Float.TYPE) {
            field.setFloat(o, byteBuffer.getFloat());
        } else if (field.getType() == Double.TYPE) {
            field.setDouble(o, byteBuffer.getDouble());
        } else if (field.getType() == Boolean.TYPE) {
            field.setBoolean(o, byteBuffer.get() != 0);
        } else {
            throw new RuntimeException("Primitive type not supported");
        }

    }
}
