package org.example;

import org.example.enums.MdcVariableEnum;
import org.example.exceptions.FormValidationFailed;
import org.example.formhandlers.*;
import org.example.model.PersonalInfoFormTO;
import org.example.model.VisaFormTO;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.example.utils.DriverUtils.initDriver;
import static org.example.utils.IoUtils.savePage;

public class TerminFinder {

    public static int searchCount = 0;
    private final Logger logger = LoggerFactory.getLogger(TerminFinder.class);
    private final VisaFormTO visaFormTO;
    private final PersonalInfoFormTO personalInfoFormTO;
    private final long FORM_REFRESH_PERIOD_IN_SECONDS = 1;
    private RemoteWebDriver driver;
    private String currentWindowHandle;
    private final Timer timer = new Timer(true);


    public TerminFinder(PersonalInfoFormTO personalInfoFormTO, VisaFormTO visaFormTO) {
        this.personalInfoFormTO = personalInfoFormTO;
        this.visaFormTO = visaFormTO;
        setMDCVariables();
    }

    public TerminFinder(VisaFormTO visaFormTO, PersonalInfoFormTO personalInfoFormTO, RemoteWebDriver driver) {
        this.visaFormTO = visaFormTO;
        this.personalInfoFormTO = personalInfoFormTO;
        this.driver = driver;
    }

    public void startScanning() throws FormValidationFailed {
        // Section 0
        if (isResidenceTitleInfoVerified(visaFormTO)) {
            logger.info("Successfully validated form: {}", visaFormTO);
        } else {
            logger.error("Failed validate form: {}", visaFormTO);
            throw new FormValidationFailed("");
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::run, 0, FORM_REFRESH_PERIOD_IN_SECONDS, TimeUnit.SECONDS);
        logger.info(String.format("Scheduled the task at rate: %s", FORM_REFRESH_PERIOD_IN_SECONDS));
    }


    private void run() {
        setDriver();
        try {
            getFormPage();
        } catch (Exception e) {
            logger.error("Error in initializing a new session. Exception: ", e);
            handleException();
            return;
        }

        Section1MainPageHandler section1MainPageHandler = new Section1MainPageHandler(driver);
        Section2ServiceSelectionHandler section2ServiceSelectionHandler = new Section2ServiceSelectionHandler(visaFormTO, personalInfoFormTO, driver);
        Section3DateSelectionHandler section3DateSelectionHandler = new Section3DateSelectionHandler(driver);
        Section4VisaFormHandler section4VisaFormHandler = new Section4VisaFormHandler(personalInfoFormTO, visaFormTO, driver);
        Section5ReservationHandler section5ReservationHandler = new Section5ReservationHandler(driver);

        if (!fillAndSendFormWithExceptionHandling(section1MainPageHandler)) return;
        if (!fillAndSendFormWithExceptionHandling(section2ServiceSelectionHandler)) return;
        if (!fillAndSendFormWithExceptionHandling(section3DateSelectionHandler)) return;
        if (!fillAndSendFormWithExceptionHandling(section4VisaFormHandler)) return;
        if (!fillAndSendFormWithExceptionHandling(section5ReservationHandler)) return;

        logger.info("End of process");
        driver.quit();
        timer.cancel();

    }

    boolean fillAndSendFormWithExceptionHandling(IFormHandler formHandler) {
        try {
            boolean isSuccessful = formHandler.fillAndSendForm();
            driver = formHandler.getDriver();
            return isSuccessful;
        } catch (Exception e) {
            logger.error("Exception occurred during handling {}, quitting.", formHandler.getClass().getSimpleName(), e);
            String fileName = formHandler.getClass().getSimpleName();
            savePage(driver, fileName, "exception");
            logger.info("page is saved");
            return false;
        } finally {
            MDC.remove(MdcVariableEnum.elementDescription.name());
        }
    }

    private void handleException() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void setDriver() {
        setMDCVariables();
        if (driver == null) {
            logger.info("Driver is null, initializing the driver");
            driver = initDriver();
        }

        SessionId sessionId = driver.getSessionId();
        if (sessionId == null) {
            logger.info("Session is null, initializing the driver");
            driver = initDriver();
        }
    }

    private void getFormPage() throws InterruptedException {
        currentWindowHandle = driver.getWindowHandle();
        logger.info("Switching to a new tab");
        driver.switchTo().newWindow(WindowType.TAB);
        Thread.sleep(2000);

        currentWindowHandle = driver.getWindowHandle();

        String url = "https://otv.verwalt-berlin.de/ams/TerminBuchen?lang=en";
        logger.info(String.format("Getting the URL: %s", url));
        Set<String> handle = driver.getWindowHandles();
        handle.forEach((asd) -> logger.info(String.format("Window handle: " + asd)));
        logger.info(String.format("Closing the  window handle: %s", handle.stream().collect(Collectors.toList()).get(0)));
        driver.switchTo().window(handle.stream().collect(Collectors.toList()).get(0)).close();
        logger.info(String.format("Switching to window handle: %s", currentWindowHandle));
        driver.switchTo().window(currentWindowHandle);


        currentWindowHandle = driver.getWindowHandle();
        driver.get(url);

    }

    private void setMDCVariables() {
        MDC.put("visaForm", visaFormTO.toString());
        MDC.put(MdcVariableEnum.elementDescription.name(), null);

    }

    private boolean isResidenceTitleInfoVerified(VisaFormTO visaFormTO) {
        logger.info("Verifying form: {}", visaFormTO);
        String serviceType = visaFormTO.getServiceType();
        Boolean isResidencePermitPresent = visaFormTO.getResidencePermitPresent();
        String residencePermitId = visaFormTO.getResidencePermitId();

        if (serviceType.equals("Apply for a residence title")) {
            if (isResidencePermitPresent == null) {
                return false;
            }

            if (isResidencePermitPresent && residencePermitId == null) {
                return false;
            }

            if (!isResidencePermitPresent && residencePermitId != null) {
                return false;
            }
        }

        if (serviceType.equals("Extend a residence title")) {

            if (residencePermitId == null) {
                return false;
            }
        }

        return true;
    }

}
