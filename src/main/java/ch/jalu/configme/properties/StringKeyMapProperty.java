package ch.jalu.configme.properties;

import ch.jalu.configme.resource.PropertyResource;

import java.util.Map;

/**
 * Map property with string keys
 * @param <V> the type of the values
 */
public class StringKeyMapProperty<V> extends Property<Map<String, V>> {

    private final Class<V> valueType;

    public StringKeyMapProperty(Class<V> valueType, String path, Map<String, V> defaultMap) {
        super(path, defaultMap);
        this.valueType = valueType;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, V> getFromResource(PropertyResource resource) {
        Map<?, ?> rawMap = resource.getMap(getPath());

        if (rawMap != null) {
            for (Map.Entry<?, ?> o : rawMap.entrySet()) {
                if (!(o.getKey() instanceof String)) {
                    return null;
                }

                if (!valueType.isInstance(o.getValue())) {
                    return null;
                }
            }

            //We checked that every entry is valid
            return (Map<String, V>) rawMap;
        }
        return null;
    }

    @Override
    public boolean isPresent(PropertyResource resource) {
        return resource.getMap(getPath()) != null;
    }

    public Class<V> getValueType() {
        return valueType;
    }
}
