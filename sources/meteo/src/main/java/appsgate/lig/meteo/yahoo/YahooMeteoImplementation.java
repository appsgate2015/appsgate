package appsgate.lig.meteo.yahoo;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import appsgate.lig.meteo.DayForecast;
import appsgate.lig.meteo.Meteo;
import appsgate.lig.meteo.YahooGeoPlanet;

/**
 * Implementation of Yahoo forecast, allows to change unit (Celsius,Fahrenheit)
 * but gives maximum 2 days forecast, as input its required the WOEID
 * (http://developer.yahoo.com/geo/geoplanet/guide/concepts.html#hierarchy)
 * which indicates the location for the forecast. This class parses the
 * information obtained in from weather yahoo service: e.g.
 * http://weather.yahooapis.com/forecastrss?w=12724717&u=c
 * 
 * @author jnascimento
 * 
 */
public class YahooMeteoImplementation implements Meteo {

	static final String FORECAST = "forecast";
	static final String TITLE = "title";
	static final String ITEM = "item";
	static final String DATEPUBLICATION = "pubDate";

	private Logger logger = Logger.getLogger(YahooMeteoImplementation.class
			.getSimpleName());
	
	public Integer refreshRate;

	URL url;
	String WOEID;
	Integer temperature;

	private String location;
	private Timer refreshTimer = new Timer();
	private List<DayForecast> forecasts = new ArrayList<DayForecast>();
	private Calendar datePublication;
	private Calendar lastFetch;

	/**
	 * Feed URL that returns an XML with the forecast (e.g.
	 * http://weather.yahooapis.com/forecastrss?w=12724717&u=c)
	 */
	private String feedUrlTemplate = "http://weather.yahooapis.com/forecastrss?w=%s&u=c";

	private String feedUrl;

	TimerTask refreshtask = new TimerTask() {
		@Override
		public void run() {

			if (YahooMeteoImplementation.this.refreshRate != -1) {

				logger.fine("Refreshing meteo data");
				YahooMeteoImplementation.this.fetch();

			}
		}
	};

	public YahooMeteoImplementation() {
	    // Setting default location to Grenoble
	    this("593720");

	}

	public YahooMeteoImplementation(String woeid) {
		this.WOEID = woeid;
	}

	/**
	 * Process an element <yweather:forecast/> of the xml
	 * 
	 * @param XMLEvent
	 *            element
	 * @return DayForecast
	 */
	private DayForecast processForecastElement(
			javax.xml.stream.events.StartElement element) {
		Float temperatureLow = Float.parseFloat(element.getAttributeByName(
				new QName("low")).getValue());

		Float temperatureHigh = Float.parseFloat(element.getAttributeByName(
				new QName("high")).getValue());

		Date date = null;

		try {
			DateFormat yahooForecastDateAttributeParser = new SimpleDateFormat(
					"dd MMM yyyy", Locale.ENGLISH);

			String dateString = element.getAttributeByName(new QName("date"))
					.getValue();

			date = yahooForecastDateAttributeParser.parse(dateString);
		} catch (ParseException e) {
			logger.warning("Unable to parse:"
					+ element.getAttributeByName(new QName("date")).getValue());
		}

		return new DayForecast(date, temperatureLow, temperatureHigh);

	}

	/**
	 * Process element "<item>"
	 * 
	 * @param reader
	 * @param event
	 * @throws XMLStreamException
	 */
	private void digElementItem(XMLEventReader reader, XMLEvent event)
			throws XMLStreamException {

		if (event.isStartElement()) {
			javax.xml.stream.events.StartElement se = event.asStartElement();
			String localpart = se.getName().getLocalPart();

			if (localpart.equals(FORECAST)) {

				DayForecast dayforecast = processForecastElement(se);

				forecasts.add(dayforecast);
			}

			if (localpart.equals(TITLE)) {

				this.location = getCharacterData(reader, se);

			}

			if (localpart.equals("condition")) {

				this.temperature = Integer.parseInt(event.asStartElement()
						.getAttributeByName(new QName("temp")).getValue());

			}

			if (localpart.equals(DATEPUBLICATION)) {
				// Wed, 22 May 2013 3:58 pm CEST - convert this format to date
				// EEE, d MMM yyyy h:m zzz
				try {
					DateFormat yahooForecastDateAttributeParser = new SimpleDateFormat(
							"EEE, d MMM yyyy h:m a zzz", Locale.ENGLISH);

					String dateString = getCharacterData(reader, event)
							.toLowerCase();

					Date date = yahooForecastDateAttributeParser
							.parse(dateString);

					this.datePublication = Calendar.getInstance();
					this.datePublication.setTime(date);

				} catch (ParseException e) {
					logger.warning("Unable to parse (pubDate):"
							+ getCharacterData(reader, event));
				}
			}
		}

		if (reader.hasNext())
			digElementItem(reader, reader.nextEvent());

		if (event.isEndElement()
				&& event.asEndElement().getName().getLocalPart().equals(ITEM)) {
			return;
		}
	}

	private void digElement(XMLEventReader reader, XMLEvent event)
			throws XMLStreamException {

		if (event.isStartElement()) {

			String name = event.asStartElement().getName().getLocalPart();

			if (name.equals(ITEM)) {
				digElementItem(reader, reader.nextEvent());
			}
		}

		if (event.isEndElement()) {
			String name = event.asEndElement().getName().getLocalPart();
		}

		if (reader.hasNext())
			digElement(reader, reader.nextEvent());
	}

	/**
	 * Update all meteo information stored here
	 */
	public Meteo fetch() {
		try {
			
			feedUrl = String.format(feedUrlTemplate, WOEID);
			
			this.url = new URL(feedUrl);

			// First create a new XMLInputFactory
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// Setup a new eventReader
			InputStream in = read();
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);

			forecasts = new ArrayList<DayForecast>();

			digElement(eventReader, eventReader.nextEvent());

		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		lastFetch = Calendar.getInstance();

		return this;
	}

	private String getCharacterData(XMLEventReader reader, XMLEvent event)
			throws XMLStreamException {
		String result = "";
		event = reader.nextEvent();
		if (event instanceof javax.xml.stream.events.Characters) {
			result = event.asCharacters().getData();
		}
		return result;
	}

	private InputStream read() {
		try {
			return url.openStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Calendar getDateForecast() {
		return null;
	}

	public Calendar getDatePublication() {
		return this.datePublication;
	}

	public List<DayForecast> getForecast() {
		return forecasts;
	}

	public Calendar getLastFetch() {
		return lastFetch;
	}

	public String getLocation() {
		return location;
	}

	public void start() {

		fetch();

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
		logger.info("Meteo stopped:" + this.getClass().getSimpleName());
		refreshtask.cancel();
	}

	public Integer getCurrentTemperature() {
		return temperature;
	}
	
	@Override
	public String toString(){
		
		if(this.getLastFetch()==null){
			return "No info retrieved, wait for the next fetch";
		}
		
		StringBuffer sb=new StringBuffer();
		sb.append(String.format("location %s \n",this.location));
		sb.append(String.format("current temperature %s \n",this.getCurrentTemperature()));
		if(this.getDatePublication()!=null)
			sb.append(String.format("meteo report from %1$te/%1$tm/%1$tY %1$tH:%1$tM \n",this.getDatePublication()));
		sb.append(String.format("osgi meteo polled report at %1$te/%1$tm/%1$tY %1$tH:%1$tM:%1$tS \n",this.getLastFetch().getTime()));
		sb.append("-- forecasts --\n");
		for(DayForecast forecast:this.getForecast()){
			sb.append(String.format("Date: %1$te/%1$tm/%1$tY (min:%2$s,max:%3$s) \n",forecast.getDate(),forecast.getMin(),forecast.getMax()));
		}
		sb.append("-- /forecasts --\n");
		
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see appsgate.lig.meteo.Meteo#setLocation(java.lang.String)
	 */
	public void setLocation(String placeName) {
	    YahooGeoPlanet geoPlanet = new YahooGeoPlanetImpl();
	    String newWOEID = geoPlanet.getWOEIDFromPlaceName(placeName);
	    if(newWOEID!= null) {
		WOEID = newWOEID;
		location=placeName;
	    }
	}

}
