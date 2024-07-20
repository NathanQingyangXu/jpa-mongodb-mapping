package org.hibernate.omm;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.omm.cfg.MongodbAvailableSettings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.testcontainers.containers.MongoDBContainer;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public abstract class AbstractMongodbContainerTests {
	//private final MongoDBContainer mongodbContainer = new MongoDBContainer("mongo:4.0.10" );
	private MongoDBContainer mongodbContainer;

	private SessionFactory sessionFactory;

	@BeforeEach
	void startContainers() {
		if (mongodbContainer != null) {
			mongodbContainer.start();
		}
	}

	@AfterEach
	void stopContainers() {
		if (mongodbContainer != null) {
			mongodbContainer.stop();
		}
	}

	public abstract List<Class<?>> getAnnotatedClasses();

	protected SessionFactory getSessionFactory() {
		if (sessionFactory == null) {
			Configuration cfg = new Configuration();
			getAnnotatedClasses().forEach( cfg::addAnnotatedClass );
			cfg.setProperty( AvailableSettings.DIALECT, "org.hibernate.omm.dialect.MongodbDialect" );
			cfg.setProperty(
					AvailableSettings.CONNECTION_PROVIDER,
					"org.hibernate.omm.jdbc.MongodbConnectionProvider"
			);
			//cfg.setProperty( MongodbAvailableSettings.MONGODB_CONNECTION_URL, mongodbContainer.getReplicaSetUrl() );
			cfg.setProperty(
					MongodbAvailableSettings.MONGODB_CONNECTION_URL, "mongodb://localhost"
			);
			cfg.setProperty( MongodbAvailableSettings.MONGODB_DATABASE, "sample_training" );
			sessionFactory = cfg.buildSessionFactory();
		}
		return sessionFactory;
	}
}
