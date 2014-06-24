package appsgate.lig.persistence;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;

public class MongoDBConfigFactory {

	static private final Logger logger = LoggerFactory
			.getLogger(MongoDBConfigFactory.class);

	public static final String DBHOST_KEY = "DBHost";

	public static final String DBHOST_DEFAULT = "localhost";

	public static final String DBPORT_KEY = "DBPort";

	public static final int DBPORT_DEFAULT = 27017;

	public static final String DBNAME_KEY = "DBName";

	public static final String DBTIMEOUT_KEY = "DBTimeout";

	public static final int DBTIMEOUT_DEFAULT = 3000;

	private String dbHost;
	private Integer dbPort;
	private Integer dbTimeOut;

	private MongoClient mongoClient;

	/**
	 * Default constructor
	 */
	public MongoDBConfigFactory() {
		this(DBHOST_DEFAULT, DBPORT_DEFAULT, DBTIMEOUT_DEFAULT);
	}

	/**
	 * Four ways to configure the DB (each one get priority on its
	 * predecessors), (each one can omit parameters, previously setted values
	 * will be used) 1 - Use default values (the class constants) 2 - Use this
	 * parameterized constructor 3 - Use System Properties 4 - Use values
	 * provided as parameters of the apam-instance
	 * 
	 * (in all case we wait for a client to create a configuration before trying
	 * to connect)
	 */
	public MongoDBConfigFactory(String dbHost, int dbPort, int dbTimeOut) {

        // 2- Using parameterized values
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbTimeOut = dbTimeOut;
    }

    public void setFactoryParameters(Properties props) {

		// 3- Getting System properties
		String tmp_host = props.getProperty(DBHOST_KEY);
		if (tmp_host != null && !tmp_host.equals(this.dbHost)) {
            this.dbHost = tmp_host;
            configValueChanged(this.dbHost);
        }

		Integer tmp_port = Integer.parseInt(props.getProperty(DBPORT_KEY));
		if (tmp_port != null && !tmp_port.equals(this.dbPort)) {
            this.dbPort = tmp_port;
            configValueChanged(this.dbPort);
        }

		Integer tmp_timeout = Integer.parseInt(props.getProperty(DBTIMEOUT_KEY));
		if (tmp_timeout != null && !tmp_timeout.equals(this.dbTimeOut)) {
            this.dbTimeOut = tmp_timeout;
            configValueChanged(this.dbTimeOut);

        }

	}

	public MongoDBConfiguration newConfiguration(String dbName) {

		if (dbName != null) {
			return new MongoDBConfiguration(dbName, this);
		} else {
			logger.error("Not creating the configuration," + " dbName = ");
		}
		return null;
	}

	public void configValueChanged(Object newValue) {
		// Warning the new value should be injected automatically, do not make
		// affectation
		logger.debug("A value has changed, new value : " + newValue);

		// we only reset the mongo client (to be sure to recreate when
		// necessary)
		if (mongoClient != null)
			mongoClient.close();

		mongoClient = null;
	}

	public MongoClient getMongoClient() {
		logger.trace("Checking mongo client");
		if (mongoClient != null) {
			logger.trace("Mongo Client is not null");
			// Forces the connection to check valid
			if (!checkMongoClient())
				return createMongoClient();
			else
				return mongoClient;
		} else {
			logger.debug("Mongo client is null, trying to create one");
			return createMongoClient();
		}
	}

	private boolean checkMongoClient() {
		try {
			// Forces the connection to check valid
			mongoClient.getDatabaseNames();
			logger.trace("Checking Mongo OK");
			return true;
		} catch (MongoException exception) {
			logger.warn("Retrieving databases names throws an error : "
					+ exception.getStackTrace());
			return false;
		}
	}

	private MongoClient createMongoClient() {
		try {
			logger.debug("Testing Mongo socket address...");
			Socket testingSocket = new Socket(dbHost, dbPort);
			testingSocket.close();
		} catch (Exception e) {
			logger.warn("Cannot connect to "+dbHost+" / "+dbPort
					+", No MongoDB running or wrong configuration : "+e.getMessage());
			return null;
		}

		try {
			logger.debug("Creating new mongo client");

			Builder options = new MongoClientOptions.Builder();
			options.connectTimeout(dbTimeOut);
			mongoClient = new MongoClient(new ServerAddress(dbHost, dbPort),
					options.build());
			if (!checkMongoClient()) {
				logger.error("Cannot retrieves databases, mongo client is considered invalid");
				return null;
			}
			return mongoClient;

		} catch (UnknownHostException exception) {
			logger.error("Cannot create MongoDB Client "
					+ exception.getStackTrace());
			return null;
		}
	}

}