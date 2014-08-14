package se.tap2.j2nbus;

import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class J2NBus {

    private static J2NBus bus = new J2NBus();

    /**
     * Get the bus
     * @return
     */
    public static J2NBus getBus() {
        return bus;
    }

    private J2NBus() {
        // this will init the bus on the native side
        init();
    }

    public synchronized  <T> void removeListener(Class<T> type, Listener<T> listener) {
        String typeName = type.getName();

        List<Listener> listenersForType = listeners.get(typeName);
        if (listenersForType != null) {
            listenersForType.remove(listener);
        }

    }

    public synchronized  <T> void removeListener(Listener<T> listener) {

        String typeName = ((Class<T>)((ParameterizedType)listener.getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getName();

        List<Listener> listenersForType = listeners.get(typeName);
        if (listenersForType != null) {
            listenersForType.remove(listener);
        }

    }

    public static interface Listener<T> {
        void onData(T data);
    }


    /**
     * Subscribe to messages on the bus of a certain type.
     * @param type
     * @param listener
     * @param <T> the typ you are interested in
     */
    public synchronized <T> void addListener(Class<T> type, Listener<T> listener) {

        String typeName = type.getName();

        List<Listener> listenersForType = listeners.get(typeName);
        if (listenersForType == null) {
            listenersForType = new ArrayList<Listener>();
            listeners.put(typeName, listenersForType);
        }
        listenersForType.add(listener);


    }

    /**
     * Subscribe to messages on the bus of a certain type.
     * @param listener
     * @param <T> the typ you are interested in
     */
    public synchronized <T> void addListener(Listener<T> listener) {

        String typeName = ((Class<T>)((ParameterizedType)listener.getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getName();

        List<Listener> listenersForType = listeners.get(typeName);
        if (listenersForType == null) {
            listenersForType = new ArrayList<Listener>();
            listeners.put(typeName, listenersForType);
        }
        listenersForType.add(listener);


    }

    Map<String, List<Listener>> listeners = new HashMap<String, List<Listener>>();


    public void post(Object o) {
        post(o.getClass().getName(), o);


    }

    private void post(String topic, Object o) {


        try {
            byte[] objBytes = J2NBusProto.serializeDataObject(o);
            String typeName = o.getClass().getName();
            sendObjectToNative(topic, typeName, objBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    @SuppressWarnings("UnusedDeclaration") // will be called from native code
    private static void onBytesFromNative(String topic, String type, byte[] objBytes) {

        try {

            ByteBuffer byteBuffer = ByteBuffer.wrap(objBytes);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            int stringSize = byteBuffer.getInt();
            byte[] stringBytes = new byte[stringSize];
            byteBuffer.get(stringBytes);
            String typeName = new String(stringBytes);


            Object deserializeDataObject = J2NBusProto.deserializeDataObject(typeName, byteBuffer);

            getBus().notifyListeners(topic, typeName, deserializeDataObject);

        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private synchronized void notifyListeners(String topic, String type, Object deserializeDataObject) {

        List<Listener> listenersForType = listeners.get(type);
        if (listenersForType != null) {
            for (Listener listener : listenersForType) {
                listener.onData(deserializeDataObject);
            }
        }

    }


    private static native void sendObjectToNative(String topic, String type, byte[] objectBytes);

    private static native void init();

}
