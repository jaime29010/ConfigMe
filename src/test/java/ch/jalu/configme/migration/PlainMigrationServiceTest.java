package ch.jalu.configme.migration;

import ch.jalu.configme.TestUtils;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.properties.IntegerProperty;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.resource.PropertyResource;
import ch.jalu.configme.resource.YamlFileResource;
import ch.jalu.configme.samples.TestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PlainMigrationService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlainMigrationServiceTest {

    private static final String COMPLETE_CONFIG = "/config-sample.yml";
    private static final String INCOMPLETE_CONFIG = "/config-incomplete-sample.yml";

    private static final List<Property<?>> KNOWN_PROPERTIES =
        ConfigurationDataBuilder.collectData(TestConfiguration.class).getProperties();

    @Spy
    private PlainMigrationService service;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldReturnNoSaveNecessaryForAllPropertiesPresent() {
        // given
        PropertyResource resource = createResourceSpy(COMPLETE_CONFIG);

        // when
        boolean result = service.checkAndMigrate(resource, KNOWN_PROPERTIES);

        // then
        assertThat(result, equalTo(false));
        verify(service).performMigrations(resource, KNOWN_PROPERTIES);
    }

    @Test
    public void shouldReturnTrueForMissingProperty() {
        // given
        PropertyResource resource = createResourceSpy(INCOMPLETE_CONFIG);

        // when
        boolean result = service.checkAndMigrate(resource, KNOWN_PROPERTIES);

        // then
        assertThat(result, equalTo(true));
        // Verify that performMigrations was called; it should be called before our generic property check
        verify(service).performMigrations(resource, KNOWN_PROPERTIES);
    }

    @Test
    public void shouldPassResourceToExtendedMethod() {
        // given
        PropertyResource resource = createResourceSpy(COMPLETE_CONFIG);
        given(resource.contains("old.property")).willReturn(true);
        PlainMigrationServiceTestExtension service = Mockito.spy(new PlainMigrationServiceTestExtension());

        // when
        boolean result = service.checkAndMigrate(resource, KNOWN_PROPERTIES);

        // then
        assertThat(result, equalTo(true));
        verify(service).performMigrations(resource, KNOWN_PROPERTIES);
        verify(resource).contains("old.property");
    }

    @Test
    public void shouldResetNegativeIntegerProperties() {
        // given
        PropertyResource resource = createResourceSpy(COMPLETE_CONFIG);
        PlainMigrationServiceTestExtension service = new PlainMigrationServiceTestExtension();

        // when
        boolean result = service.checkAndMigrate(resource, KNOWN_PROPERTIES);

        // then
        assertThat(result, equalTo(true));
        assertThat(resource.getInt(TestConfiguration.DURATION_IN_SECONDS.getPath()), equalTo(0));
        verify(resource).setValue(TestConfiguration.DURATION_IN_SECONDS.getPath(), 0);
    }

    private PropertyResource createResourceSpy(String file) {
        // It's a little difficult to set up mock behavior for all cases, so use a YML file from test/resources
        File copy = TestUtils.copyFileFromResources(file, temporaryFolder);
        return Mockito.spy(new YamlFileResource(copy));
    }

    private static class PlainMigrationServiceTestExtension extends PlainMigrationService {

        @Override
        protected boolean performMigrations(PropertyResource resource, List<Property<?>> properties) {
            // If contains -> return true = migration is necessary
            if (resource.contains("old.property")) {
                return true;
            }

            // Set any int property to 0 if its value is above 20
            boolean hasChange = false;
            for (Property<?> property : properties) {
                if (property instanceof IntegerProperty) {
                    IntegerProperty intProperty = (IntegerProperty) property;
                    if (intProperty.getValue(resource) > 20) {
                        resource.setValue(intProperty.getPath(), 0);
                        hasChange = true;
                    }
                }
            }
            return hasChange;
        }
    }

}
