package org.hibernate.omm.extension;

import com.mongodb.client.MongoDatabase;
import jakarta.persistence.Entity;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.omm.jdbc.MongoDriver;
import org.hibernate.omm.service.CommandRecorder;
import org.hibernate.omm.service.DefaultCommandRecorderImpl;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;
import org.testcontainers.containers.MongoDBContainer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Predicate;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedFields;

/**
 * A JUnit 5 extension mechanism injecting initialized {@link SessionFactory} and/or {@link MongoDatabase}
 * into testing class's static or instance fields annotated with the following annotations:
 * <ul>
 *     <li>{@link SessionFactoryInjected}</li>
 *     <li>{@link MongoDatabaseInjected}</li>
 *     <li>{@link CommandRecorderInjected}</li>
 * </ul>
 * It also scans the inner classes annotated with {@link Entity} and adding them automatically into Hibernate's meta model.
 * <p>
 * For external {@link Entity} classes, {@link MongoIntegrationTest} annotation provides the {@link MongoIntegrationTest#externalEntities()}
 * properties to configure explicitly.
 * <p>
 * Testing case specific Hibernate property could be specified as another property of {@link MongoIntegrationTest}:
 * {@link MongoIntegrationTest#hibernateProperties()}.
 * <p>
 * Another benefit is the testing class could extend from other parent class, if needed.
 *
 * @author Nathn Xu
 * @since 1.0.0
 */
class ChameleonExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final String MONGODB_DOCKER_IMAGE_NAME = "mongo:7.0.14";
    private static final String DATABASE_NAME = "test";

    private MongoDBContainer mongoDBContainer;
    private SessionFactoryImplementor sessionFactory;
    private MongoDatabase mongoDatabase;

    @Override
    public void beforeAll(final ExtensionContext context) {
        final var testClass = context.getRequiredTestClass();

        final var annotatedClasses = new HashSet<Class<?>>();
        Arrays.stream(testClass.getDeclaredClasses())
                .filter(clazz -> clazz.getAnnotation(Entity.class) != null)
                .forEach(annotatedClasses::add);

        final var mongoIntegrationTestAnnotation = testClass.getAnnotation(MongoIntegrationTest.class);
        annotatedClasses.addAll(Arrays.asList(mongoIntegrationTestAnnotation.externalEntities()));

        if (annotatedClasses.isEmpty()) {
            throw new IllegalStateException("No annotated Entity class found for the testing class: " + testClass.getName());
        }

        mongoDBContainer = new MongoDBContainer(MONGODB_DOCKER_IMAGE_NAME);
        mongoDBContainer.start();

        final var cfg = new Configuration();

        final var url = mongoDBContainer.getConnectionString() + "/" + DATABASE_NAME;
        cfg.setProperty(AvailableSettings.JAKARTA_JDBC_URL, url);
        annotatedClasses.forEach(cfg::addAnnotatedClass);

        var standardServiceRegistryBuilder = cfg.getStandardServiceRegistryBuilder();
        standardServiceRegistryBuilder.addService(CommandRecorder.class, DefaultCommandRecorderImpl.INSTANCE);

        Arrays.stream(mongoIntegrationTestAnnotation.hibernateProperties()).forEach(prop -> cfg.setProperty(prop.key(), prop.value()));

        mongoDatabase = MongoDriver.getMongoDatabase(url);
        MongoDriver.commandRecorder = DefaultCommandRecorderImpl.INSTANCE;

        sessionFactory = (SessionFactoryImplementor) cfg.buildSessionFactory();

        injectStaticFields(testClass, SessionFactoryInjected.class, SessionFactory.class, sessionFactory);
        injectStaticFields(testClass, MongoDatabaseInjected.class, MongoDatabase.class, mongoDatabase);
        injectStaticFields(testClass, CommandRecorderInjected.class, CommandRecorder.class, DefaultCommandRecorderImpl.INSTANCE);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        if (sessionFactory != null) {
            try {
                sessionFactory.close();
            } finally {
                sessionFactory = null;
            }
        }
        if (mongoDBContainer != null) {
            try {
                mongoDBContainer.stop();
            } finally {
                mongoDBContainer = null;
            }
        }
    }


    @Override
    public void beforeEach(final ExtensionContext context) {
        try (var collectionNameIterator = mongoDatabase.listCollectionNames().iterator()) {
            while (collectionNameIterator.hasNext()) {
                mongoDatabase.getCollection(collectionNameIterator.next()).drop();
            }
        }

        var commandRecorder = DefaultCommandRecorderImpl.INSTANCE;

        injectInstanceFields(context.getRequiredTestClass(), context.getRequiredTestInstance(), SessionFactoryInjected.class,
                SessionFactory.class,
                sessionFactory);
        injectInstanceFields(context.getRequiredTestClass(), context.getRequiredTestInstance(), MongoDatabaseInjected.class,
                MongoDatabase.class, mongoDatabase);
        injectInstanceFields(context.getRequiredTestClass(), context.getRequiredTestInstance(), SessionFactoryInjected.class,
                SessionFactory.class,
                sessionFactory);
        injectInstanceFields(context.getRequiredTestClass(), context.getRequiredTestInstance(), CommandRecorderInjected.class,
                CommandRecorder.class, commandRecorder);
    }



    private void injectStaticFields(Class<?> testClass, Class<? extends Annotation> annotationClass, Class<?> fieldType,
            Object injectedValue) {
        doInjectFields(true, testClass, testClass, annotationClass, fieldType, injectedValue);
    }

    private void injectInstanceFields(Class<?> testClass, Object testInstance, Class<? extends Annotation> annotationClass,
            Class<?> fieldType,
            Object injectedValue) {
        doInjectFields(false, testClass, testInstance, annotationClass, fieldType, injectedValue);
    }

    private void doInjectFields(boolean injectStaticField, Class<?> testClass, Object fieldOwner,
            Class<? extends Annotation> annotationClass, Class<?> fieldType,
            Object injectedValue) {
        Predicate<Field> predicate = field -> (injectStaticField ?
                ReflectionUtils.isStatic(field) : ReflectionUtils.isNotStatic(field) ) && fieldType.isAssignableFrom(field.getType());
        findAnnotatedFields(testClass, annotationClass, predicate)
                .forEach(field -> {
                    try {
                        field.setAccessible(true);
                        field.set(fieldOwner, injectedValue);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) {
        DefaultCommandRecorderImpl.INSTANCE.clearCommandsRecorded();
    }
}
