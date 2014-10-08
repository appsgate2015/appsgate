/**
 * Copyright 2011-2013 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * YahooGeoPlanetImplTest.java - 15 juil. 2013
 */
package appsgate.lig.meteo.yahoo.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import appsgate.lig.yahoo.geoplanet.YahooGeoPlanet;
import appsgate.lig.yahoo.impl.YahooGeoPlanetImpl;

/**
 * @author thibaud
 *
 */
public class YahooGeoPlanetImplTest {
	
	private static Logger logger = LoggerFactory
			.getLogger(YahooGeoPlanetImplTest.class);	


    @Test
    public void testgetWOEIDFromPlaceName() {
        logger.debug("testgetWOEIDFromPlaceName");    	

	YahooGeoPlanet planet = new YahooGeoPlanetImpl();
	assertEquals("593720", planet.getWOEIDFromPlaceName("Grenoble"));
    }
    
    @Test
    public void testgetDescriptionFromWOEID() {
        logger.debug("getDescriptionFromWOEID");    	
	YahooGeoPlanet planet = new YahooGeoPlanetImpl();
	
	planet.getDescriptionFromWOEID("593720");

    }
    
    @Test
    public void testgetDescriptionFromPlaceName() {
        logger.debug("testgetDescriptionFromPlaceName");    	
	YahooGeoPlanet planet = new YahooGeoPlanetImpl();
	
	planet.getDescriptionFromPlaceName("Grenoble");
	planet.getDescriptionFromPlaceName("France");
	planet.getDescriptionFromPlaceName("Par");

    }    
    
    @Test
    public void testcheckLocationStartingWith() {
    logger.debug("checkLocationStartingWith");
	YahooGeoPlanet planet = new YahooGeoPlanetImpl();
	
	planet.getLocationsStartingWith("Gre");
    }    

    
}
