package com.hectortv9.udemy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.hectortv9.udemy.CourseResource.ResourceType;

public class UdemyDownload {

    private static Properties privateProps = new Properties();
    static {
        try {
            privateProps.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("private.properties/private.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(privateProps.toString());
    }

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;
    private static String driverAbsolutePath = privateProps.getProperty("webdriver.chrome.driver");

    private static String profileDirectory = privateProps.getProperty("profileDirectory");
    private static int[] illegalChars = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
            22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124 };

    private static String starterUrl = privateProps.getProperty("starterUrl");

    private static final String SECTIONS_XPATH = "//div[starts-with(@data-purpose,'section-panel-')]";
    private static final String SECTION_LABEL_XPATH = ".//div[starts-with(@data-purpose,'section-label')]";
    private static final String SECTION_TITLES_XPATH = SECTIONS_XPATH + "/div//span[starts-with(text(),'Section')]";
    private static final String SECTION_TITLE_SPANS_XPATH = ".//span/span//span";
    private static final String CURRICULUM_ITEM_DIVS_XPATH = ".//ul/li/div";
    private static final String RESOURCE_BUTTON_XPATH = ".//button[@aria-label='Resource list']";
    private static final String RESOURCE_ICONS_XPATH = ".//following-sibling::ul[@role='menu']/li/a/span[1]";
    private static final String RESOURCE_NAMES_XPATH = ".//following-sibling::ul[@role='menu']/li/a/span[2]";
    private static final String DOWNLOADS_DIRECTORY = profileDirectory + "/Downloads"; // Used by deleteFiles()
//	private static final String FILE_NAME_FORMAT = DOWNLOADS_DIRECTORY + "/original (%d).pdf"; //NOT USED ANYMORE DUE TO NON-INCREMENTAL FILENAME APPROACH
    private static final String FILE_NAME_FORMAT = DOWNLOADS_DIRECTORY + "/original.pdf";
    private static final String FILE_NAME_REG_EX = "original \\(\\d+\\).pdf"; // Used by deleteFiles()
    private static final String RESOURCES_DIRECTORY = DOWNLOADS_DIRECTORY + "/Resources";
    private static final int RESOURCE_DOWNLOAD_TIMEOUT_IN_SECONDS = 300;
    private static boolean isFileDownloadEnabled = true;

    private static final String DOCUMENT_READY_JS_CODE = "return document.readyState;";
    private static final String LINK_RESOURCE_URL_VAR_NAME = "window.redirectionUrl";
    private static final String PREPARE_BROWSER_FOR_URL_EXTRACTION_JS_CODE =
            "window.windowOpen = window.open;" +  //backup original function
            "window.isRedirectionEnabled = false;" + 
            LINK_RESOURCE_URL_VAR_NAME + " = null;" + 
            "window.open = function() {" + 
            "    " + LINK_RESOURCE_URL_VAR_NAME + " = arguments[0];" + 
            "    if(window.isRedirectionEnabled) {" + 
            "        windowOpen.apply(this, arguments);" + 
            "    }" + 
            "};"  +
            "return 'complete';";//hack just to make DOCUMENT_READY_JS_CODE easy and compliant with runJsTillCompletion()
    private static final String GET_LINK_RESOURCE_URL_JS_CODE =
            "(function(seleniumCallback){" + 
            "    let intervalId = setInterval(function(){" + 
            "        if(" + LINK_RESOURCE_URL_VAR_NAME + ") {" + 
            "            clearInterval(intervalId);" + 
            "            let temp = " + LINK_RESOURCE_URL_VAR_NAME + ";" + 
            "            " + LINK_RESOURCE_URL_VAR_NAME + " = null;" + 
            "            seleniumCallback(temp);" + 
            "        }" + 
            "    },0);" + 
            "}(arguments[0]));";

    private static AtomicInteger downloadsCounter = new AtomicInteger(0);
    private static Instant executionStartTime = Instant.now();
    private static Path latestFile;
    
    public static ChromeDriver createChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--user-data-dir=" + profileDirectory + "/AppData/Local/Google/Chrome/User Data/Profile 2");
//        options.addArguments("--profile-directory=Profile 2");
        options.addArguments("--start-maximized");
//        options.addArguments("--disable-extensions");
//        options.addArguments("--no-sandbox");
        System.setProperty("webdriver.chrome.driver", driverAbsolutePath);

        return new ChromeDriver(options);
    }

    public static void main(String[] args) throws IOException {

        deleteFiles(DOWNLOADS_DIRECTORY, FILE_NAME_REG_EX);
        emptyResourcesDirectory(RESOURCES_DIRECTORY);

        driver = createChromeDriver();
        wait = new WebDriverWait(driver, 30);
        js = (JavascriptExecutor) driver;

        try {
//             driver.manage().window().setPosition(new Point(2000, 10));
            driver.manage().window().maximize();
            driver.get(starterUrl);
            runJsTillCompletion(DOCUMENT_READY_JS_CODE);
            runJsTillCompletion(PREPARE_BROWSER_FOR_URL_EXTRACTION_JS_CODE);

            List<WebElement> sectionDivs = driver.findElements(By.xpath(SECTIONS_XPATH));
            int sectionCount = sectionDivs.size();
            //Course structure: Section -> Items -> Resources
            CourseStructure courseStructure = new CourseStructure(sectionCount);
            for (int i = 0; i < sectionCount; i++) {
                WebElement sectionDiv = sectionDivs.get(i);
                WebElement sectionLabelDiv = sectionDiv.findElement(By.xpath(SECTION_LABEL_XPATH));
                List<WebElement> sectionTitleSpans = sectionLabelDiv.findElements(By.xpath(SECTION_TITLE_SPANS_XPATH));
                StringBuilder sb = new StringBuilder(sectionTitleSpans.size());
                //Section Title with multiple lines splits in separate spans. Join them to get full Title
                for (WebElement sectionTitleSpan : sectionTitleSpans) {
                    sb.append(sectionTitleSpan.getText().trim()).append(" ");
                }
                String sectionName = sb.toString().trim();

                //Expand section to display list of course items
                boolean isExpanded = Boolean.parseBoolean(sectionDiv.getAttribute("aria-expanded"));
                if (!isExpanded) {
                    sectionLabelDiv.click();
                }

                List<WebElement> curriculumItemDivs = sectionDiv.findElements(By.xpath(CURRICULUM_ITEM_DIVS_XPATH));
                int curriculumItemCount = curriculumItemDivs.size();

                courseStructure.addCourseSection(sectionName, cleanFileName(sectionName), curriculumItemCount);

                for (int j = 0; j < curriculumItemCount; j++) {
                    WebElement curriculumItemDiv = curriculumItemDivs.get(j);
                    String curriculumItemName = curriculumItemDiv.getAttribute("aria-label").trim();

                    List<WebElement> resourceButtons = curriculumItemDiv.findElements(By.xpath(RESOURCE_BUTTON_XPATH));
                    if (resourceButtons.size() != 0) {
                        WebElement resourceButton = resourceButtons.get(0);
                        wait.until(ExpectedConditionsPlus.javaScriptThrowsNoExceptions(
                                "arguments[0].scrollIntoView(true);", resourceButtons.get(0)));

                        resourceButton.click(); // display the resources menu
                        //Assumption is there's always an icon associated to one span
                        List<WebElement> resourcesIcons = resourceButton.findElements(By.xpath(RESOURCE_ICONS_XPATH));
                        List<WebElement> resourcesSpans = resourceButton.findElements(By.xpath(RESOURCE_NAMES_XPATH));
                        int resourcesSpansCount = resourcesSpans.size();

                        courseStructure.getCourseSection(i).addCourseItem(curriculumItemName,
                                cleanFileName(curriculumItemName), resourcesSpansCount);

                        for (int k = 0; k < resourcesSpans.size(); k++) {
                            WebElement resourceIcon = resourcesIcons.get(k);
                            ResourceType resourceType = CourseResource.getResourceType(resourceIcon);
                            WebElement resourceSpan = resourcesSpans.get(k);
                            String resourceName = resourceSpan.getText().trim();

                            if (isFileDownloadEnabled && resourceType.isDownloadable()) {
                                String downloadedFilename = downloadResource(resourceSpan);
                                courseStructure.getCourseSection(i).getCourseItem(j).addCourseResource(
                                        resourceType, resourceName, cleanFileName(resourceName), downloadedFilename);
                                String organizedResourceFilename = organizeResource(
                                        courseStructure.getCourseResource(i, j, k).get());
                                courseStructure.getCourseResource(i, j, k).get()
                                        .setActualFilename(organizedResourceFilename);

                            } else {
                                if (resourceType.isDownloadable()) {
                                    resourceButton.click(); // hide the resources menu
                                    courseStructure.getCourseSection(i).getCourseItem(j).addCourseResource(
                                            resourceType, resourceName, cleanFileName(resourceName), "File Downloads Disabled");
                                } else {
                                    resourceSpan.click(); //puts the link url into a global variable. See PREPARE_BROWSER_FOR_URL_EXTRACTION_JS_CODE
                                    String linkUrl = (String) js.executeAsyncScript(GET_LINK_RESOURCE_URL_JS_CODE);
                                    courseStructure.getCourseSection(i).getCourseItem(j).addCourseResource(
                                            resourceType, resourceName, resourceName, linkUrl, linkUrl);
                                }
                            }
                            resourceButton.click(); // display the resources menu hidden by resourcesSpan.click()
                        }
                        resourceButton.click(); // hide the resources menu
                    } else {
                        courseStructure.getCourseSection(i).addCourseItem(curriculumItemName,
                                cleanFileName(curriculumItemName));
                    }
                }
            }
            Files.write(Paths.get(RESOURCES_DIRECTORY).resolve("Course Structure.txt"),
                    courseStructure.toString().getBytes(), StandardOpenOption.CREATE_NEW);

            Files.write(Paths.get(RESOURCES_DIRECTORY).resolve("Resource files Structure.txt"),
                    courseStructure.toStringForFiles().getBytes(), StandardOpenOption.CREATE_NEW);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//			driver.quit();
            System.out.println("Script Execution Finalized!");
        }
    }
    
    /**
     * The script inside this method avoids redirection and gets the
     * intended destination url. The intention is to get the url of
     * link resources without navigating to the link's destination url.
     */
    private static void runJsTillCompletion(String jsCode) {
        wait.until(webDriver -> js.executeScript(jsCode).toString().equals("complete"));
    }

    private static String organizeResource(CourseResource courseResource) throws IOException {
        CourseItem courseItem = courseResource.getParent();
        CourseSection courseSection = courseItem.getParent();
        StringBuilder sb = new StringBuilder();

        sb.append(RESOURCES_DIRECTORY).append("/").append(courseSection.getSectionNameForFiles());

        if (!courseSection.wasDirectoryCreated()) {
            FileUtils.forceMkdir(new File(sb.toString()).getCanonicalFile());
            courseSection.setWasDirectoryCreated(true);
        }

        sb.append("/").append(courseItem.getItemNameForFiles());
        if (!courseItem.wasDirectoryCreated()) {
            FileUtils.forceMkdir(new File(sb.toString()).getCanonicalFile());
            courseItem.setWasDirectoryCreated(true);
        }

        sb.append("/").append(courseResource.getResourceNameForFiles());

        Path toPath = Paths.get(sb.toString()).toAbsolutePath();
        Path fromPath = Paths.get(courseResource.getOriginalFilename()).toRealPath();
        Path resultPath = Files.move(fromPath, toPath);

        return resultPath.toRealPath().toString();
    }

    public static Path getLatestFile(String DOWNLOADS_DIRECTORY) {
        Path directory = Paths.get(DOWNLOADS_DIRECTORY);
        Optional<Path> latestFile = Optional.empty();
        try {
            latestFile = Files.list(directory)
                    .filter(f -> !Files.isDirectory(f) && !f.toString().endsWith("crdownload"))
                    .max(Comparator.comparingLong(f -> f.toFile().lastModified()));

            if (!latestFile.isPresent()) {
                throw new IOException("No file detected in downloads directory");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return latestFile.get();
    }

    public static boolean wasFileDownloadedDuringExecution(Path file) {

        if (file.toFile().lastModified() > executionStartTime.toEpochMilli()) {
            return true;
        }
        return false;
    }

    public static String downloadResource(WebElement element) throws IOException {
        element.click(); // Starts the download

        /*
         * downloadsCounter.getAndIncrement(); String fileName =
         * String.format(FILE_NAME_FORMAT, downloadsCounter.get()); Path filePath =
         * Paths.get(fileName); wait .pollingEvery(Duration.ofSeconds(1))
         * .withTimeout(Duration.ofSeconds(30)) .until(x -> { return
         * filePath.toFile().exists();}); return filePath.toRealPath().toString();
         */

        latestFile = null;
        wait.pollingEvery(Duration.ofSeconds(1)).withTimeout(Duration.ofSeconds(RESOURCE_DOWNLOAD_TIMEOUT_IN_SECONDS)).until(x -> {
            latestFile = getLatestFile(DOWNLOADS_DIRECTORY);
            return wasFileDownloadedDuringExecution(latestFile);
        });
        downloadsCounter.getAndIncrement();

        return latestFile.toRealPath().toString();
    }

    public static void deleteFiles(String directory, String FILE_NAME_REG_EX) throws IOException {

        int response = JOptionPane.showConfirmDialog(null,
                String.format("The following data will be used to erase files:%n" + "Directory:%n  %s%n"
                        + "Filename Regex:%n  %s%n%n" + "Do you want to continue?", directory, FILE_NAME_REG_EX),
                "Continue with file deletion?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (response != JOptionPane.YES_OPTION) {
            throw new IOException("User cancelled execution due to file deletion warning.");
        }

        Files.find(Paths.get(directory), 1, (path, basicFileAttributes) -> {
            return path.toFile().getName().matches(FILE_NAME_REG_EX);
        }).forEach(matchingFile -> {
            try {
                System.out.printf("Deleting file: %s%n", matchingFile.toAbsolutePath());
                Files.deleteIfExists(matchingFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void emptyResourcesDirectory(String resourcesDirectory) throws IOException {

        Path resourcesPath = Paths.get(resourcesDirectory);
        boolean isDir = Files.isDirectory(resourcesPath);

        if (isDir) {
            System.out.printf("Deleting directory: %s%n", resourcesPath.toAbsolutePath());
            FileUtils.deleteDirectory(resourcesPath.toFile());
        }

        Files.createDirectory(resourcesPath);
    }

    public static String cleanFileName(String badFileName) {
        StringBuilder cleanName = new StringBuilder();
        int len = badFileName.codePointCount(0, badFileName.length());
        for (int i = 0; i < len; i++) {
            int c = badFileName.codePointAt(i);
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.appendCodePoint(c);
            }
        }
        return cleanName.toString();
    }
}
