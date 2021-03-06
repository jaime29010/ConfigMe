package ch.jalu.configme.properties;

import ch.jalu.configme.resource.PropertyResource;

import java.util.Arrays;
import java.util.List;

/**
 * String list property.
 */
public class StringListProperty extends Property<List<String>> {

    public StringListProperty(String path, String... defaultValues) {
        super(path, Arrays.asList(defaultValues));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<String> getFromResource(PropertyResource resource) {
        List<?> rawList = resource.getList(getPath());
        if (rawList != null) {
            for (Object o : rawList) {
                if (!(o instanceof String)) {
                    return null;
                }
            }
            // We checked that every entry is a String
            return (List<String>) rawList;
        }
        return null;
    }

    @Override
    public boolean isPresent(PropertyResource resource) {
        return resource.getList(getPath()) != null;
    }
}
