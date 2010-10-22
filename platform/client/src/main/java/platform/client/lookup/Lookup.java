package platform.client.lookup;

import platform.base.WeakIdentityHashSet;

import java.lang.ref.WeakReference;
import java.util.*;

public class Lookup {
    public static final String NEW_EDITABLE_OBJECT_PROPERTY = "newEditableObjectProperty";
    public static final String DELETED_OBJECT_PROPERTY = "deletedObjectProperty";

    private static Lookup instance = new Lookup();

    private Lookup(){}

    public static Lookup getDefault() {
        return instance;
    }

    private Map<String, WeakReference<Object>> properties = Collections.synchronizedMap(new HashMap<String, WeakReference<Object>>());

    private final Map<String, WeakIdentityHashSet<LookupResultChangeListener>> listeners = Collections.synchronizedMap(new HashMap<String, WeakIdentityHashSet<LookupResultChangeListener>>());

    public void setProperty(String name, Object object) {
        expungeStaleEntries();

        Object oldValue = nullRefGet(name);
        properties.put(name, new WeakReference(object));

        firePropertyChange(name, oldValue, object);
    }

    private void expungeStaleEntries() {
        for (Iterator<Map.Entry<String, WeakReference<Object>>> it = properties.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, WeakReference<Object>> entry = it.next();
            if (entry.getValue().get() == null) {
                it.remove();
            }
        }
    }

    private Object nullRefGet(String name) {
        WeakReference<Object> oldRef = properties.get(name);
        return oldRef == null ? null : oldRef.get();
    }

    public Object getProperty(String name) {
        expungeStaleEntries();
        return nullRefGet(name);
    }

    private void firePropertyChange(String name, Object oldValue, Object newValue) {
        for (LookupResultChangeListener listener : getPropertyListeners(name)) {
            listener.resultChanged(name, oldValue, newValue);
        }
    }

    private WeakIdentityHashSet<LookupResultChangeListener> getPropertyListeners(String name) {
        if (name == null) {
            return new WeakIdentityHashSet<LookupResultChangeListener>();
        }

        WeakIdentityHashSet<LookupResultChangeListener> mp = listeners.get(name);
        if (mp == null) {
            mp = new WeakIdentityHashSet<LookupResultChangeListener>();
            listeners.put(name, mp);
        }
        
        return mp;
    }

    public void addLookupResultChangeListener(String name, LookupResultChangeListener listener) {
        getPropertyListeners(name).add(listener);
    }

    public static interface LookupResultChangeListener {
        public void resultChanged(String name, Object oldValue, Object newValue);
    }
}
