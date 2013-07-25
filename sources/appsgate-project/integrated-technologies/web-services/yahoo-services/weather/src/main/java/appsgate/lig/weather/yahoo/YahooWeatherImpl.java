package appsgate.lig.weather.yahoo;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;

import org.w3c.dom.Document;

import appsgate.lig.weather.CurrentWeather;
import appsgate.lig.weather.DayForecast;
import appsgate.lig.weather.WeatherForecast;
import appsgate.lig.weather.WeatherForecastException;

/**
 * Implementation of Yahoo forecast, allows to change unit (Celsius,Fahrenheit)
 * as input its required the WOEID
 * (http://developer.yahoo.com/geo/geoplanet/guide/concepts.html#hierarchy)
 * which indicates the location for the forecast. This class parses the
 * information obtained in from weather yahoo service: e.g.
 * http://weather.yahooapis.com/forecastrss?w=12724717&u=c
 * 
 * @author thibaud
 * 
 */
public class YahooWeatherImpl implements WeatherForecast {

    static final String DATEPUBLICATION = "pubDate";

    private Logger logger = Logger.getLogger(YahooWeatherImpl.class
	    .getSimpleName());

    public Integer refreshRate;

    URL url;

    boolean noFetch;
    private char currentUnit;
    private Timer refreshTimer = new Timer();
    private Calendar lastFetchDate;
    private YahooGeoPlanet geoPlanet;

    /*
     * For each human place Name, as the user entered it (the key), associate a
     * WOEID (the value)
     */
    private Map<String, String> woeidFromePlaceName;

    // Set of map defining combination between a WOEID (Where on Earth
    // IDentifier)
    // and some information about this location WOEID always comes as key and
    // the information as value
    private Map<String, Calendar> lastPublicationDates;
    private Map<String, CurrentWeather> currentWeathers;
    private Map<String, List<DayForecast>> forecasts;

    /**
     * Feed URL that returns an XML with the forecast (e.g.
     * http://weather.yahooapis.com/forecastrss?w=12724717&u=c)
     */
    private String feedUrlTemplate = "http://weather.yahooapis.com/forecastrss?w=%s&u=%c";

    private String feedUrl;

    TimerTask refreshtask = new TimerTask() {
	@Override
	public void run() {

	    if (YahooWeatherImpl.this.refreshRate != -1) {

		logger.fine("Refreshing meteo data");
		try {
		    YahooWeatherImpl.this.fetch();
		} catch (WeatherForecastException exc) {
		    exc.printStackTrace();
		}

	    }
	}
    };

    public YahooWeatherImpl() {
	woeidFromePlaceName = new HashMap<String, String>();
	lastPublicationDates = new HashMap<String, Calendar>();
	currentWeathers = new HashMap<String, CurrentWeather>();
	forecasts = new HashMap<String, List<DayForecast>>();

	lastFetchDate = null;
	currentUnit = 'c';
	noFetch = true;

	geoPlanet = new YahooGeoPlanetImpl();
    }

    public Calendar getLastFetchDate() {
	return lastFetchDate;
    }

    private String getWOEID(String placeName) throws WeatherForecastException {
	if (!woeidFromePlaceName.containsKey(placeName))
	    throw new WeatherForecastException(
		    "Place Name is not monitored (Not spelled as previously added ?)");
	return woeidFromePlaceName.get(placeName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.weather.WeatherForecast#getForecast(java.lang.String)
     */
    @Override
    public List<DayForecast> getForecast(String placeName)
	    throws WeatherForecastException {
	return forecasts.get(getWOEID(placeName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * appsgate.lig.weather.WeatherForecast#getPublicationDate(java.lang.String)
     */
    @Override
    public Calendar getPublicationDate(String placeName)
	    throws WeatherForecastException {
	return lastPublicationDates.get(getWOEID(placeName));

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * appsgate.lig.weather.WeatherForecast#getCurrentWeather(java.lang.String)
     */
    @Override
    public CurrentWeather getCurrentWeather(String placeName)
	    throws WeatherForecastException {
	return currentWeathers.get(getWOEID(placeName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.weather.WeatherForecast#getLocations()
     */
    @Override
    public String[] getLocations() {
	return woeidFromePlaceName.keySet().toArray(new String[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.weather.WeatherForecast#addLocation(java.lang.String)
     */
    @Override
    public void addLocation(String placeName) throws WeatherForecastException {
	if (woeidFromePlaceName.containsKey(placeName))
	    throw new WeatherForecastException(
		    "Already monitoring this location");

	String okPlaceName = placeName.replace(" ", "%20");

	String newWOEID = geoPlanet.getWOEIDFromPlaceName(okPlaceName);
	if (newWOEID != null) {
	    woeidFromePlaceName.put(placeName, newWOEID);
	    lastPublicationDates.put(newWOEID, null);
	    currentWeathers.put(newWOEID, null);
	    forecasts.put(newWOEID, null);
	} else
	    throw new WeatherForecastException("Place was not found");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * appsgate.lig.weather.WeatherForecast#removeLocation(java.lang.String)
     */
    @Override
    public boolean removeLocation(String placeName)
	    throws WeatherForecastException {
	if (!woeidFromePlaceName.containsKey(placeName))
	    throw new WeatherForecastException(
		    "Already monitoring this location");
	else {
	    String woeid = woeidFromePlaceName.remove(placeName);
	    if (woeid == null)
		return false;
	    lastPublicationDates.remove(woeid);
	    currentWeathers.remove(woeid);
	    forecasts.remove(woeid);
	    return true;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * appsgate.lig.weather.WeatherForecast#containLocation(java.lang.String)
     */
    @Override
    public boolean containLocation(String placeName)
	    throws WeatherForecastException {
	return woeidFromePlaceName.containsKey(placeName);
    }

    public void start() {

	try {
	    fetch();
	} catch (WeatherForecastException exc) {
	    exc.printStackTrace();
	}

	/**
	 * Configure auto-refresh meteo data
	 */
	if (refreshRate != null && refreshRate != -1) {
	    logger.fine("Configuring auto-refresh for :" + refreshRate);
	    refreshTimer.scheduleAtFixedRate(refreshtask, 0,
		    refreshRate.longValue());
	}

    }

    public void stop() {
	logger.info("WeatherForecast stopped:"
		+ this.getClass().getSimpleName());
	refreshtask.cancel();
    }

    public Unit getUnit() {
	switch (currentUnit) {
	case 'c':
	    return Unit.EU;
	case 'f':
	    return Unit.US;
	default:
	    currentUnit = 'c';
	    return WeatherForecast.Unit.EU;
	}
    }

    @Override
    public String toString() {

	if (this.getLastFetchDate() == null) {
	    return "No info retrieved, wait for the next fetch";
	}
	StringBuffer sb = new StringBuffer();
	for (String placeName : woeidFromePlaceName.keySet()) {
	    sb.append(String.format("location %s \n", placeName));

	    try {
		if (getCurrentWeather(placeName) != null)
		    sb.append(this.getCurrentWeather(placeName));

		if (getPublicationDate(placeName) != null)
		    sb.append(String
			    .format("meteo report from %1$te/%1$tm/%1$tY %1$tH:%1$tM \n",
				    this.getPublicationDate(placeName)));
		sb.append("-- forecasts --\n");
		for (DayForecast forecast : getForecast(placeName)) {
		    sb.append(String
			    .format("Date: %1$te/%1$tm/%1$tY (min:%2$s,max:%3$s, code:%4$s) \n",
				    forecast.getDate(), forecast.getMin(),
				    forecast.getMax(), forecast.getCode()));
		}
	    } catch (WeatherForecastException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    sb.append("--- /forecasts ---\n");
	}
	sb.append(String
		.format("osgi meteo polled report at %1$te/%1$tm/%1$tY %1$tH:%1$tM:%1$tS \n",
			this.getLastFetchDate().getTime()));

	return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.weather.WeatherForecast#fetch()
     */
    public void fetch() throws WeatherForecastException {
	try {

	    for (String woeid : woeidFromePlaceName.values()) {
		logger.fine("Fetching Weather for " + woeid);

		feedUrl = String.format(feedUrlTemplate, woeid, currentUnit);

		this.url = new URL(feedUrl);

		// First create a new XMLInputFactory
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();

		DocumentBuilder db = null;
		db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(url.openStream());

		currentWeathers.put(woeid,
			YahooWeatherParser.parseCurrentConditions(doc));
		forecasts.put(woeid, YahooWeatherParser.parseForecast(doc));
		lastPublicationDates.put(woeid,
			YahooWeatherParser.parsePublicationDate(doc));
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	    throw new WeatherForecastException(
		    "Impossible to fetch to weather forecast service, "
			    + e.getMessage());
	}

	lastFetchDate = Calendar.getInstance();
    }

    /*
     * (non-Javadoc)
     * 
     * @see appsgate.lig.weather.WeatherForecast#setUnit(appsgate.lig.weather.
     * WeatherForecast.Unit)
     */
    @Override
    public void setUnit(Unit unit) {
	switch (unit) {
	case EU:
	    currentUnit = 'c';
	case US:
	    currentUnit = 'f';
	    break;
	}
    }

}
