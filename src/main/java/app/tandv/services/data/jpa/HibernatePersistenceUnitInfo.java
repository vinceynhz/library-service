package app.tandv.services.data.jpa;

import org.hibernate.jpa.HibernatePersistenceProvider;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author vic on 2020-07-23
 */
public class HibernatePersistenceUnitInfo implements PersistenceUnitInfo {
    private static final String JPA_VERSION = "2.1";
    private final String persistenceUnitName;
    private PersistenceUnitTransactionType transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
    private final List<String> managedClassNames;
    private final List<String> mappingFileNames = new ArrayList<>();
    private final Properties properties;
    private DataSource jtaDataSource;
    private DataSource nonjtaDataSource;
    private final List<ClassTransformer> transformers = new ArrayList<>();

    HibernatePersistenceUnitInfo(String persistenceUnitName, List<String> managedClassNames, Properties properties) {
        this.persistenceUnitName = persistenceUnitName;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
    }

    @Override
    public String getPersistenceUnitName() {
        return this.persistenceUnitName;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return HibernatePersistenceProvider.class.getName();
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return this.transactionType;
    }

    @Override
    public DataSource getJtaDataSource() {
        return this.jtaDataSource;
    }

    public HibernatePersistenceUnitInfo withNonJtaDataSource(DataSource nonjtaDataSource) {
        this.nonjtaDataSource = nonjtaDataSource;
        this.jtaDataSource = null;
        this.transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
        return this;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return this.nonjtaDataSource;
    }

    @Override
    public List<String> getMappingFileNames() {
        return this.mappingFileNames;
    }

    @Override
    public List<URL> getJarFileUrls() {
        return Collections.emptyList();
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return null;
    }

    @Override
    public List<String> getManagedClassNames() {
        return this.managedClassNames;
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return false;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return SharedCacheMode.UNSPECIFIED;
    }

    @Override
    public ValidationMode getValidationMode() {
        return ValidationMode.AUTO;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return JPA_VERSION;
    }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void addTransformer(ClassTransformer classTransformer) {
        this.transformers.add(classTransformer);
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return null;
    }
}
