package ch.jalu.configme.properties;

import ch.jalu.configme.beanmapper.Mapper;
import ch.jalu.configme.beanmapper.command.Command;
import ch.jalu.configme.beanmapper.command.CommandConfig;
import ch.jalu.configme.beanmapper.command.Executor;
import ch.jalu.configme.beanmapper.worldgroup.WorldGroupConfig;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.resource.PropertyResource;
import ch.jalu.configme.resource.YamlFileResource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static ch.jalu.configme.TestUtils.copyFileFromResources;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link BeanProperty} and its integration in {@link YamlFileResource}.
 */
public class BeanPropertyTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldExportPropertyAndReimport() {
        // given
        BeanProperty<CommandConfig> property =
            new BeanProperty<>(CommandConfig.class, "commandconfig", new CommandConfig());
        File configFile = copyFileFromResources("/beanmapper/commands.yml", temporaryFolder);
        PropertyResource resource = new YamlFileResource(configFile);

        // when
        resource.exportProperties(new ConfigurationData(singletonList(property)));
        resource = new YamlFileResource(configFile);

        // then
        CommandConfig config = property.getFromResource(resource);
        assertThat(config.getCommands().keySet(), contains("save", "refresh", "open"));
        Command refreshCommand = config.getCommands().get("refresh");
        assertThat(refreshCommand.getExecution().getPrivileges(), contains("page.view", "action.refresh"));
        assertThat(refreshCommand.getExecution().getExecutor(), equalTo(Executor.CONSOLE));
        assertThat(refreshCommand.getExecution().isOptional(), equalTo(true));
        Command openCommand = config.getCommands().get("open");
        assertThat(openCommand.getArguments(), contains("f", "x", "z"));
        assertThat(openCommand.getExecution().isOptional(), equalTo(false));
        assertThat(config.getDuration(), equalTo(13));
    }

    @Test
    public void shouldExportBeanPropertyAtRootProperly() {
        // given
        BeanProperty<CommandConfig> property =
            new BeanProperty<>(CommandConfig.class, "", new CommandConfig());
        File configFile = copyFileFromResources("/beanmapper/commands_root_path.yml", temporaryFolder);
        PropertyResource resource = new YamlFileResource(configFile);

        // when
        resource.exportProperties(new ConfigurationData(singletonList(property)));
        resource = new YamlFileResource(configFile);

        // then
        CommandConfig config = property.getFromResource(resource);
        assertThat(config.getCommands().keySet(), contains("save"));
        Command saveCommand = config.getCommands().get("save");
        assertThat(saveCommand.getExecution().getPrivileges(), contains("action.open", "action.save"));
        assertThat(saveCommand.getExecution().getExecutor(), equalTo(Executor.CONSOLE));
    }

    @Test
    public void shouldUseCustomMapper() {
        // given
        Mapper mapper = mock(Mapper.class);
        String path = "cnf";
        BeanProperty<WorldGroupConfig> property =
            new BeanProperty<>(WorldGroupConfig.class, path, new WorldGroupConfig(), mapper);
        PropertyResource resource = mock(PropertyResource.class);
        Object value = new Object();
        given(resource.getObject(path)).willReturn(value);
        WorldGroupConfig groupConfig = new WorldGroupConfig();
        given(mapper.convertToBean(path, resource, WorldGroupConfig.class)).willReturn(groupConfig);

        // when
        WorldGroupConfig result = property.getValue(resource);

        // then
        assertThat(result, equalTo(groupConfig));
        verify(mapper).convertToBean(path, resource, WorldGroupConfig.class);
    }
}
