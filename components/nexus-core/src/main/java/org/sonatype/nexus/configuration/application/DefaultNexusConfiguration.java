/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.configuration.application;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationRequest;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.NexusStreamResponse;
import org.sonatype.nexus.configuration.Configurable;
import org.sonatype.nexus.configuration.ConfigurationChangeEvent;
import org.sonatype.nexus.configuration.ConfigurationCommitEvent;
import org.sonatype.nexus.configuration.ConfigurationLoadEvent;
import org.sonatype.nexus.configuration.ConfigurationPrepareForLoadEvent;
import org.sonatype.nexus.configuration.ConfigurationPrepareForSaveEvent;
import org.sonatype.nexus.configuration.ConfigurationRollbackEvent;
import org.sonatype.nexus.configuration.ConfigurationSaveEvent;
import org.sonatype.nexus.configuration.application.runtime.ApplicationRuntimeConfigurationBuilder;
import org.sonatype.nexus.configuration.model.CPathMappingItem;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.configuration.source.ApplicationConfigurationSource;
import org.sonatype.nexus.configuration.validator.ApplicationConfigurationValidator;
import org.sonatype.nexus.configuration.validator.ApplicationValidationContext;
import org.sonatype.nexus.plugins.RepositoryType;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.cache.CacheManager;
import org.sonatype.nexus.proxy.events.VetoFormatter;
import org.sonatype.nexus.proxy.events.VetoFormatterRequest;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.proxy.storage.local.DefaultLocalStorageContext;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.tasks.descriptors.ScheduledTaskDescriptor;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;

/**
 * The class DefaultNexusConfiguration is responsible for config management. It actually keeps in sync Nexus internal
 * state with persisted user configuration. All changes incoming through its iface is reflect/maintained in Nexus
 * current
 * state and Nexus user config.
 *
 * @author cstamas
 */
@Singleton
@Named
public class DefaultNexusConfiguration
    extends ComponentSupport
    implements NexusConfiguration
{
  /**
   * Only to have UTs work
   */
  private final CacheManager cacheManager;

  private final EventBus eventBus;

  private final ApplicationConfigurationSource configurationSource;

  private final Provider<GlobalRemoteConnectionSettings> globalRemoteConnectionSettingsProvider;

  private final Provider<GlobalRemoteProxySettings> globalRemoteProxySettingsProvider;

  private final ApplicationConfigurationValidator configurationValidator;

  private final ApplicationRuntimeConfigurationBuilder runtimeConfigurationBuilder;

  private final RepositoryTypeRegistry repositoryTypeRegistry;

  private final RepositoryRegistry repositoryRegistry;

  private final List<ScheduledTaskDescriptor> scheduledTaskDescriptors;

  private final VetoFormatter vetoFormatter;

  private final List<ConfigurationModifier> configurationModifiers;

  private final ClassLoader uberClassLoader;

  private final ApplicationDirectories applicationDirectories;

  // ===

  /**
   * The global local storage context.
   */
  private DefaultLocalStorageContext globalLocalStorageContext;

  /**
   * The global remote storage context.
   */
  private DefaultRemoteStorageContext globalRemoteStorageContext;

  /**
   * The config dir
   */
  private File configurationDirectory;

  /**
   * Names of the conf files
   */
  private Map<String, String> configurationFiles;

  /**
   * The default maxInstance count
   */
  private int defaultRepositoryMaxInstanceCountLimit = Integer.MAX_VALUE;

  /**
   * The map with per-repotype limitations
   */
  private Map<RepositoryTypeDescriptor, Integer> repositoryMaxInstanceCountLimits;

  // ==

  @Inject
  public DefaultNexusConfiguration(final CacheManager cacheManager,
                                   final EventBus eventBus,
                                   final @Named("file") ApplicationConfigurationSource configurationSource,
                                   final Provider<GlobalRemoteConnectionSettings> globalRemoteConnectionSettingsProvider,
                                   final Provider<GlobalRemoteProxySettings> globalRemoteProxySettingsProvider,
                                   final ApplicationConfigurationValidator configurationValidator,
                                   final ApplicationRuntimeConfigurationBuilder runtimeConfigurationBuilder,
                                   final RepositoryTypeRegistry repositoryTypeRegistry,
                                   final RepositoryRegistry repositoryRegistry,
                                   final List<ScheduledTaskDescriptor> scheduledTaskDescriptors,
                                   final VetoFormatter vetoFormatter,
                                   final List<ConfigurationModifier> configurationModifiers,
                                   final @Named("nexus-uber") ClassLoader uberClassLoader,
                                   final ApplicationDirectories applicationDirectories)
  {
    this.cacheManager = checkNotNull(cacheManager);
    this.eventBus = checkNotNull(eventBus);
    this.configurationSource = checkNotNull(configurationSource);
    this.globalRemoteConnectionSettingsProvider = checkNotNull(globalRemoteConnectionSettingsProvider);
    this.globalRemoteProxySettingsProvider = checkNotNull(globalRemoteProxySettingsProvider);
    this.configurationValidator = checkNotNull(configurationValidator);
    this.runtimeConfigurationBuilder = checkNotNull(runtimeConfigurationBuilder);
    this.repositoryTypeRegistry = checkNotNull(repositoryTypeRegistry);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.scheduledTaskDescriptors = checkNotNull(scheduledTaskDescriptors);
    this.vetoFormatter = checkNotNull(vetoFormatter);
    this.configurationModifiers = checkNotNull(configurationModifiers);
    this.uberClassLoader = checkNotNull(uberClassLoader);
    this.applicationDirectories = checkNotNull(applicationDirectories);

    this.configurationDirectory = applicationDirectories.getWorkDirectory("conf");
  }

  @Override
  public void loadConfiguration()
      throws ConfigurationException, IOException
  {
    loadConfiguration(false);
  }

  @Override
  public synchronized void loadConfiguration(boolean force)
      throws ConfigurationException, IOException
  {
    if (force || configurationSource.getConfiguration() == null) {
      log.info("Loading Nexus Configuration...");

      configurationSource.loadConfiguration();

      boolean modified = false;
      for (ConfigurationModifier modifier : configurationModifiers) {
        modified |= modifier.apply(configurationSource.getConfiguration());
      }

      if (modified) {
        configurationSource.backupConfiguration();
        configurationSource.storeConfiguration();
      }

      globalLocalStorageContext = new DefaultLocalStorageContext(null);

      // create global remote ctx
      // this one has no parent
      globalRemoteStorageContext = new DefaultRemoteStorageContext(null);

      final GlobalRemoteConnectionSettings globalRemoteConnectionSettings = globalRemoteConnectionSettingsProvider
          .get();
      // TODO: hack
      ((DefaultGlobalRemoteConnectionSettings) globalRemoteConnectionSettings).configure(this);
      globalRemoteStorageContext.setRemoteConnectionSettings(globalRemoteConnectionSettings);

      final GlobalRemoteProxySettings globalRemoteProxySettings = globalRemoteProxySettingsProvider.get();
      // TODO: hack
      ((DefaultGlobalRemoteProxySettings) globalRemoteProxySettings).configure(this);
      globalRemoteStorageContext.setRemoteProxySettings(globalRemoteProxySettings);

      ConfigurationPrepareForLoadEvent loadEvent = new ConfigurationPrepareForLoadEvent(this);

      eventBus.post(loadEvent);

      if (loadEvent.isVetoed()) {
        log.info(
            vetoFormatter.format(new VetoFormatterRequest(loadEvent, log.isDebugEnabled())));

        throw new ConfigurationException("The Nexus configuration is invalid!");
      }

      applyConfiguration();

      // we successfully loaded config
      eventBus.post(new ConfigurationLoadEvent(this));
    }
  }

  protected String changesToString(final Collection<Configurable> changes) {
    final StringBuilder sb = new StringBuilder();

    if (changes != null) {
      sb.append(Collections2.transform(changes, new Function<Configurable, String>()
      {
        @Override
        public String apply(final Configurable input) {
          return input.getName();
        }
      }));
    }

    return sb.toString();
  }

  protected void logApplyConfiguration(final Collection<Configurable> changes) {
    if (changes != null && changes.size() > 0) {
        log.info("Applying Nexus Configuration due to changes in {}...", changesToString(changes));
    }
    else {
        log.info("Applying Nexus Configuration...");
    }
  }


  public synchronized boolean applyConfiguration() {
    log.debug("Applying Nexus Configuration...");

    ConfigurationPrepareForSaveEvent prepare = new ConfigurationPrepareForSaveEvent(this);

    eventBus.post(prepare);

    if (!prepare.isVetoed()) {
      logApplyConfiguration(prepare.getChanges());

      eventBus.post(new ConfigurationCommitEvent(this));

      eventBus.post(new ConfigurationChangeEvent(this, prepare.getChanges()));

      return true;
    }
    else {
      log.info(vetoFormatter.format(new VetoFormatterRequest(prepare, log.isDebugEnabled())));

      eventBus.post(new ConfigurationRollbackEvent(this));

      return false;
    }
  }

  @Override
  public synchronized void saveConfiguration()
      throws IOException
  {
    if (applyConfiguration()) {
      // TODO: when NEXUS-2215 is fixed, this should be remove/moved/cleaned
      // START <<<
      // validate before we do anything
      ValidationRequest request = new ValidationRequest(configurationSource.getConfiguration());
      ValidationResponse response = configurationValidator.validateModel(request);
      if (!response.isValid()) {
        this.log.error("Saving nexus configuration caused unexpected error:\n" + response.toString());
        throw new IOException("Saving nexus configuration caused unexpected error:\n" + response.toString());
      }
      // END <<<

      configurationSource.storeConfiguration();

      // we successfully saved config
      eventBus.post(new ConfigurationSaveEvent(this));
    }
  }

  @Override
  @Deprecated
  public Configuration getConfigurationModel() {
    return configurationSource.getConfiguration();
  }

  @Override
  public ApplicationConfigurationSource getConfigurationSource() {
    return configurationSource;
  }

  @Override
  public boolean isInstanceUpgraded() {
    return configurationSource.isInstanceUpgraded();
  }

  @Override
  public boolean isConfigurationUpgraded() {
    return configurationSource.isConfigurationUpgraded();
  }

  @Override
  public boolean isConfigurationDefaulted() {
    return configurationSource.isConfigurationDefaulted();
  }

  @Override
  public LocalStorageContext getGlobalLocalStorageContext() {
    return globalLocalStorageContext;
  }

  @Override
  public RemoteStorageContext getGlobalRemoteStorageContext() {
    return globalRemoteStorageContext;
  }

  @Nullable
  @Override
  @Deprecated
  public File getInstallDirectory() {
    return applicationDirectories.getInstallDirectory();
  }

  @Override
  @Deprecated
  public File getWorkingDirectory() {
    return applicationDirectories.getWorkDirectory();
  }

  @Override
  @Deprecated
  public File getWorkingDirectory(String key) {
    return applicationDirectories.getWorkDirectory(key);
  }

  @Override
  @Deprecated
  public File getWorkingDirectory(final String key, final boolean createIfNeeded) {
    return applicationDirectories.getWorkDirectory(key, createIfNeeded);
  }

  @Override
  @Deprecated
  public File getTemporaryDirectory() {
    return applicationDirectories.getTemporaryDirectory();
  }

  @Override
  public File getConfigurationDirectory() {
    return configurationDirectory;
  }

  @Override
  public List<ScheduledTaskDescriptor> listScheduledTaskDescriptors() {
    return Collections.unmodifiableList(scheduledTaskDescriptors);
  }

  @Override
  public ScheduledTaskDescriptor getScheduledTaskDescriptor(String id) {
    for (ScheduledTaskDescriptor descriptor : scheduledTaskDescriptors) {
      if (descriptor.getId().equals(id)) {
        return descriptor;
      }
    }

    return null;
  }

  // ------------------------------------------------------------------
  // Security

  @Override
  public void setRealms(List<String> realms)
      throws org.sonatype.configuration.validation.InvalidConfigurationException
  {
  }

  @Override
  public boolean isAnonymousAccessEnabled() {
    return true;
  }

  @Override
  public void setAnonymousAccess(final boolean enabled, final String username, final String password)
      throws InvalidConfigurationException
  {

  }

  protected boolean setAnonymousUserEnabled(final String anonymousUsername, final boolean enabled)
      throws InvalidConfigurationException
  {
     return true;
  }

  @Override
  public String getAnonymousUsername() {
    return null;
  }

  @Override
  public String getAnonymousPassword() {
    return null;
  }

  @Override
  public List<String> getRealms() {
    return null;
  }

  // ------------------------------------------------------------------
  // Booting

  @Override
  public void createInternals()
      throws ConfigurationException
  {
    createRepositories();
  }

  @Override
  public void dropInternals() {
    dropRepositories();
  }

  protected void createRepositories()
      throws ConfigurationException
  {
    List<CRepository> reposes = getConfigurationModel().getRepositories();

    for (CRepository repo : reposes) {

      if (!repo.getProviderRole().equals(GroupRepository.class.getName())) {
        instantiateRepository(getConfigurationModel(), repo);
      }
    }

    for (CRepository repo : reposes) {
      if (repo.getProviderRole().equals(GroupRepository.class.getName())) {
        instantiateRepository(getConfigurationModel(), repo);
      }
    }
  }

  protected void dropRepositories() {
    for (Repository repository : repositoryRegistry.getRepositories()) {
      try {
        repositoryRegistry.removeRepositorySilently(repository.getId());
      }
      catch (NoSuchRepositoryException e) {
        // will not happen
      }
    }
  }

  protected Repository instantiateRepository(final Configuration configuration, final CRepository repositoryModel)
      throws ConfigurationException
  {
    try {
      // core realm will search child/plugin realms too
      final Class<Repository> klazz = (Class<Repository>) uberClassLoader.loadClass(repositoryModel.getProviderRole());
      return instantiateRepository(configuration, klazz, repositoryModel.getProviderHint(), repositoryModel);
    }
    catch (Exception e) {
      Throwables.propagateIfInstanceOf(e, ConfigurationException.class);
      throw new ConfigurationException("Cannot instantiate repository " + repositoryModel.getProviderRole() + ":"
          + repositoryModel.getProviderHint(), e);
    }
  }

  protected Repository instantiateRepository(final Configuration configuration, final Class<? extends Repository> klazz,
                                             final String name, final CRepository repositoryModel)
      throws ConfigurationException
  {
    checkRepositoryMaxInstanceCountForCreation(klazz, name, repositoryModel);

    // create it, will do runtime validation
    Repository repository = runtimeConfigurationBuilder.createRepository(klazz, name);
    if (repository instanceof Configurable) {
      ((Configurable) repository).configure(repositoryModel);
    }

    // register with repoRegistry
    repositoryRegistry.addRepository(repository);

    // give it back
    return repository;
  }

  protected void releaseRepository(final Repository repository, final Configuration configuration,
                                   final CRepository repositoryModel)
      throws ConfigurationException
  {
    // release it
    runtimeConfigurationBuilder.releaseRepository(repository);
  }

  // ------------------------------------------------------------------
  // CRUD-like ops on config sections
  // Globals are mandatory: RU

  // CRepository and CreposioryShadow helper

  private ApplicationValidationContext getRepositoryValidationContext() {
    ApplicationValidationContext result = new ApplicationValidationContext();

    fillValidationContextRepositoryIds(result);

    return result;
  }

  private void fillValidationContextRepositoryIds(ApplicationValidationContext context) {
    context.addExistingRepositoryIds();

    List<CRepository> repositories = getConfigurationModel().getRepositories();

    if (repositories != null) {
      for (CRepository repo : repositories) {
        context.getExistingRepositoryIds().add(repo.getId());
      }
    }
  }

  // ----------------------------------------------------------------------------------------------------------
  // Repositories
  // ----------------------------------------------------------------------------------------------------------

  protected Map<RepositoryTypeDescriptor, Integer> getRepositoryMaxInstanceCountLimits() {
    if (repositoryMaxInstanceCountLimits == null) {
      repositoryMaxInstanceCountLimits = new ConcurrentHashMap<RepositoryTypeDescriptor, Integer>();
    }

    return repositoryMaxInstanceCountLimits;
  }

  @Override
  public void setDefaultRepositoryMaxInstanceCount(int count) {
    if (count < 0) {
      log.info("Default repository maximal instance limit set to UNLIMITED.");

      this.defaultRepositoryMaxInstanceCountLimit = Integer.MAX_VALUE;
    }
    else {
      log.info("Default repository maximal instance limit set to " + count + ".");

      this.defaultRepositoryMaxInstanceCountLimit = count;
    }
  }

  @Override
  public void setRepositoryMaxInstanceCount(RepositoryTypeDescriptor rtd, int count) {
    if (count < 0) {
      log.info("Repository type " + rtd.toString() + " maximal instance limit set to UNLIMITED.");

      getRepositoryMaxInstanceCountLimits().remove(rtd);
    }
    else {
      log.info("Repository type " + rtd.toString() + " maximal instance limit set to " + count + ".");

      getRepositoryMaxInstanceCountLimits().put(rtd, count);
    }
  }

  @Override
  public int getRepositoryMaxInstanceCount(RepositoryTypeDescriptor rtd) {
    Integer limit = getRepositoryMaxInstanceCountLimits().get(rtd);

    if (null != limit) {
      return limit;
    }
    else {
      return defaultRepositoryMaxInstanceCountLimit;
    }
  }

  protected void checkRepositoryMaxInstanceCountForCreation(Class<? extends Repository> klazz, String name,
                                                            CRepository repositoryModel)
      throws ConfigurationException
  {
    RepositoryTypeDescriptor rtd =
        repositoryTypeRegistry.getRepositoryTypeDescriptor(klazz, name);

    int maxCount;

    if (null == rtd) {
      // no check done
      String msg =
          String.format(
              "Repository \"%s\" (repoId=%s) corresponding type is not registered in Core, hence it's maxInstace check cannot be performed: Repository type %s:%s is unknown to Nexus Core. It is probably contributed by an old Nexus plugin. Please contact plugin developers to upgrade the plugin, and register the new repository type(s) properly!",
              repositoryModel.getName(), repositoryModel.getId(), repositoryModel.getProviderRole(),
              repositoryModel.getProviderHint());

      log.warn(msg);

      return;
    }

    if (rtd.getRepositoryMaxInstanceCount() != RepositoryType.UNLIMITED_INSTANCES) {
      maxCount = rtd.getRepositoryMaxInstanceCount();
    }
    else {
      maxCount = getRepositoryMaxInstanceCount(rtd);
    }

    if (rtd.getInstanceCount() >= maxCount) {
      String msg =
          "Repository \"" + repositoryModel.getName() + "\" (id=" + repositoryModel.getId()
              + ") cannot be created. It's repository type " + rtd.toString() + " is limited to " + maxCount
              + " instances, and it already has " + String.valueOf(rtd.getInstanceCount()) + " of them.";

      log.warn(msg);

      throw new ConfigurationException(msg);
    }
  }

  // CRepository: CRUD

  protected void validateRepository(CRepository settings, boolean create)
      throws ConfigurationException
  {
    ApplicationValidationContext ctx = getRepositoryValidationContext();

    if (!create && !Strings.isNullOrEmpty(settings.getId())) {
      // remove "itself" from the list to avoid hitting "duplicate repo" problem
      ctx.getExistingRepositoryIds().remove(settings.getId());
    }

    ValidationResponse vr = configurationValidator.validateRepository(ctx, settings);

    if (!vr.isValid()) {
      throw new InvalidConfigurationException(vr);
    }
  }

  @Override
  public synchronized Repository createRepository(CRepository settings)
      throws ConfigurationException, IOException
  {
    validateRepository(settings, true);

    // create it, will do runtime validation
    Repository repository = instantiateRepository(getConfigurationModel(), settings);

    // now add it to config, since it is validated and successfully created
    getConfigurationModel().addRepository(settings);

    // save
    saveConfiguration();

    return repository;
  }

  @Override
  public void deleteRepository(String id)
      throws NoSuchRepositoryException, IOException, ConfigurationException, AccessDeniedException
  {
    deleteRepository(id, false);
  }

  @Override
  public synchronized void deleteRepository(String id, boolean force)
      throws NoSuchRepositoryException, IOException, ConfigurationException, AccessDeniedException
  {
    Repository repository = repositoryRegistry.getRepository(id);

    if (!force && !repository.isUserManaged()) {
      throw new AccessDeniedException("Not allowed to delete non-user-managed repository '" + id + "'.");
    }

    // put out of service so wont be accessed any longer
    repository.setLocalStatus(LocalStatus.OUT_OF_SERVICE);
    // disable indexing for same purpose
    repository.setIndexable(false);
    repository.setSearchable(false);

    // remove dependants too

    // =======
    // shadows
    // (fail if any repo references the currently processing one)
    List<ShadowRepository> shadows = repositoryRegistry.getRepositoriesWithFacet(ShadowRepository.class);

    for (Iterator<ShadowRepository> i = shadows.iterator(); i.hasNext(); ) {
      ShadowRepository shadow = i.next();

      if (repository.getId().equals(shadow.getMasterRepository().getId())) {
        throw new RepositoryDependentException(repository, shadow);
      }
    }

    // ======
    // groups
    // (correction in config only, registry DOES handle it)
    // since NEXUS-1770, groups are "self maintaining"

    // ===========
    // pahMappings
    // (correction, since registry is completely unaware of this component)

    List<CPathMappingItem> pathMappings = getConfigurationModel().getRepositoryGrouping().getPathMappings();

    for (Iterator<CPathMappingItem> i = pathMappings.iterator(); i.hasNext(); ) {
      CPathMappingItem item = i.next();

      item.removeRepository(id);
    }

    // ===========
    // and finally
    // this cleans it properly from the registry (from reposes and repo groups)
    repositoryRegistry.removeRepository(id);

    List<CRepository> reposes = getConfigurationModel().getRepositories();

    for (Iterator<CRepository> i = reposes.iterator(); i.hasNext(); ) {
      CRepository repo = i.next();

      if (repo.getId().equals(id)) {
        i.remove();

        saveConfiguration();

        releaseRepository(repository, getConfigurationModel(), repo);

        return;
      }
    }

    throw new NoSuchRepositoryException(id);
  }

  // ===

  @Override
  public Map<String, String> getConfigurationFiles() {
    if (configurationFiles == null) {
      configurationFiles = new HashMap<String, String>();

      File configDirectory = getConfigurationDirectory();

      int key = 1;

      // Tamas:
      // configDirectory.listFiles() may be returning null... in this case, it is 99.9% not true (otherwise nexus
      // would not start at all), but in general, be more explicit about checks.

      if (configDirectory.isDirectory() && configDirectory.listFiles() != null) {
        for (File file : configDirectory.listFiles()) {
          if (file.exists() && file.isFile()) {
            configurationFiles.put(Integer.toString(key), file.getName());

            key++;
          }
        }
      }
    }
    return configurationFiles;
  }

  @Override
  public NexusStreamResponse getConfigurationAsStreamByKey(String key)
      throws IOException
  {
    String fileName = getConfigurationFiles().get(key);

    if (fileName != null) {
      File configFile = new File(getConfigurationDirectory(), fileName);

      if (configFile.canRead() && configFile.isFile()) {
        NexusStreamResponse response = new NexusStreamResponse();

        response.setName(fileName);

        if (fileName.endsWith(".xml")) {
          response.setMimeType("text/xml");
        }
        else {
          response.setMimeType("text/plain");
        }

        response.setSize(configFile.length());
        response.setFromByte(0);
        response.setBytesCount(configFile.length());
        response.setInputStream(new FileInputStream(configFile));

        return response;
      }
      else {
        return null;
      }
    }
    else {
      return null;
    }
  }

}
