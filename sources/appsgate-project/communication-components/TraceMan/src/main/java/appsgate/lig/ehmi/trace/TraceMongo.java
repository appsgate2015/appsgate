package appsgate.lig.ehmi.trace;

import appsgate.lig.persistence.MongoDBConfiguration;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import org.json.JSONArray;
import org.json.JSONException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jr
 */
public class TraceMongo implements TraceHistory {

    /**
     * The logger
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(TraceMongo.class);

    /**
     * The db name
     */
    private static final String DBNAME = "TraceHistory";
    /**
     * The collection containing symbol table
     */
    private static final String TRACES = "traces";

    /**
     * The connection to the mongo db
     */
    private final MongoDBConfiguration conf;

    /**
     * Constructor
     *
     * @param c
     */
    public TraceMongo(MongoDBConfiguration c) {
        this.conf = c;
    }

    /**
     *
     * @param timestamp
     * @param o
     * @return
     */
    private boolean add(Long timestamp, JSONObject o) {
        if (conf != null && conf.isValid()) {
            try {
                DBCollection context = conf.getDB(DBNAME).getCollection(TRACES);

                BasicDBObject newVal = new BasicDBObject("name", "")
                        .append("time", timestamp)
                        .append("trace", o.toString());

                context.insert(newVal);
                return true;

            } catch (MongoException e) {
                LOGGER.error("A Database Excepion has been raised: " + e);
            }
        }
        return false;
    }

    @Override
    public JSONArray get(Long timestamp, Integer count) {
        if (conf != null && conf.isValid()) {

            DBCollection context = conf.getDB(DBNAME).getCollection(TRACES);
            DBCursor cursor = context
                    .find(new BasicDBObject("time", new BasicDBObject("$lte", timestamp)))
                    //.find()
                    .limit(count)
                    .sort(new BasicDBObject("time", 1));

            return formatData(cursor);
        }

        return null;

    }

    @Override
    public JSONArray getInterval(Long start, Long end) {
        if (conf != null && conf.isValid()) {

            DBCollection context = conf.getDB(DBNAME).getCollection(TRACES);
            DBCursor cursor = context
                    .find(new BasicDBObject("time", BasicDBObjectBuilder.start("$gte", start).add("$lte", end).get()))
                    .sort(new BasicDBObject("time", 1));

            return formatData(cursor);
        }
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public void trace(JSONObject o) {
        try {
            add(o.getLong("timestamp"), o);
        } catch (JSONException ex) {
            // if there is no timestamp, just don't log the trace
        }
    }

    @Override
    public Boolean init() {
        if (conf == null) {
            LOGGER.error("Unable to init TraceMongo, no MongoDB configuration");
            return false;
        }
        if (!conf.isValid()) {
            LOGGER.error("Unable to init TraceMongo, configuration not valid");
            return false;
        }
        return true;
    }

    /**
     * Method to format data
     *
     * @param cursor the cursor on the database
     * @return a JSON array containing the trace
     */
    private JSONArray formatData(DBCursor cursor) {
        JSONArray a = new JSONArray();
        for (DBObject cur : cursor) {
            String o = cur.get("trace").toString();
            try {
                a.put(new JSONObject(o));
            } catch (JSONException ex) {

            }
        }
        return a;

    }

}
