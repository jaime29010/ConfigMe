package ch.jalu.configme.beanmapper.leafproperties;

import ch.jalu.configme.beanmapper.BeanPropertyDescription;
import ch.jalu.configme.beanmapper.ConfigMeMapperException;
import ch.jalu.configme.properties.BeanProperty;
import ch.jalu.configme.properties.Property;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Generates {@link Property} objects for all "leaf" values of a bean for the export of beans by
 * the {@link ch.jalu.configme.resource.PropertyResource}.
 */
public class LeafPropertiesGenerator {

    /**
     * Generates a list of regular property objects for the given property's data
     *
     * @param beanProperty the property
     * @param value the value of the bean property
     * @param <B> the bean type
     * @return list of all properties necessary to export the bean
     */
    public <B> List<Property<?>> generate(BeanProperty<B> beanProperty, B value) {
        return new EntryBuilder(beanProperty)
            .collectPropertyEntries(value, beanProperty.getPath());
    }

    protected static final class EntryBuilder {
        private final BeanProperty<?> beanProperty;
        private final List<Property<?>> properties = new ArrayList<>();

        EntryBuilder(BeanProperty beanProperty) {
            this.beanProperty = beanProperty;
        }

        /**
         * Processes a bean class and handles all of its writable properties. Throws an exception
         * for non-beans or classes with no writable properties.
         *
         * @param bean the bean to process
         * @param path the path of the bean in the config structure
         * @return list of all properties necessary to export the bean
         */
        protected List<Property<?>> collectPropertiesFromBean(Object bean, String path) {
            Collection<BeanPropertyDescription> writableProperties =
                beanProperty.getWritableProperties(bean.getClass());
            if (writableProperties.isEmpty()) {
                throw new ConfigMeMapperException("Class '" + bean.getClass() + "' has no writable properties");
            }
            String prefix = path.isEmpty() ? "" : (path + ".");
            for (BeanPropertyDescription property : writableProperties) {
                collectPropertyEntries(property.getValue(bean), prefix + property.getName());
            }
            return properties;
        }

        /**
         * Creates property entries for the provided value, recursively for beans.
         *
         * @param value the value to process
         * @param path the path of the value in the config structure
         * @return list of all properties necessary to export the object
         */
        @SuppressWarnings("unchecked")
        protected List<Property<?>> collectPropertyEntries(Object value, String path) {
            if (value != null) {
                Property<?> constant = createConstantProperty(value, path);
                if (constant != null) {
                    //Handle regular properties
                    properties.add(constant);
                } else if (value instanceof Collection<?>) {
                    //Handle collections
                    properties.add(new ConstantValueProperty<>(path, value));
                } else if (value instanceof Map<?, ?>) {
                    //Handle maps
                    Map<?, ?> map = (Map<?, ?>) value;
                    if (map.isEmpty()) {
                        properties.add(new ConstantValueProperty<>(path, Collections.emptyMap()));
                    } else {
                        map.forEach((k, v) -> collectPropertyEntries(v, path + "." + k));
                    }
                } else if (value instanceof Optional) {
                    //Handle optional values
                    ((Optional) value).ifPresent(o -> collectPropertyEntries(o, path));
                } else {
                    //Handle beans
                    collectPropertiesFromBean(value, path);
                }
            }
            return properties;
        }

        @Nullable
        protected ConstantValueProperty<?> createConstantProperty(Object value, String path) {
            if (value instanceof String || value instanceof Enum<?>
                || value instanceof Number || value instanceof Boolean) {
                return new ConstantValueProperty<>(path, value);
            }
            return null;
        }
    }
}
