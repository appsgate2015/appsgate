package appsgate.lig.test.pax;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({TestEmptyApAM.class,TestWebServicesAppsgate.class,TestUpnPAppsgate.class,TestCoreAppsgate.class})
public class AllTests {

}
