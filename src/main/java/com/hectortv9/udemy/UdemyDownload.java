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

public class UdemyDownload {
	
	private static Properties privateProps = new Properties();
	static {
		try {
			privateProps.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("private.properties"));
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
	private static int[] illegalChars = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124};
	
	private static String starterUrl = privateProps.getProperty("starterUrl");	
	
	private static String sectionsXpath = "//div[starts-with(@data-purpose,'section-panel-')]";
	private static String sectionLabelXpath = ".//div[starts-with(@data-purpose,'section-label')]";
	private static String sectionTitlesXpath = sectionsXpath + "/div//span[starts-with(text(),'Section')]";
    private static String sectionTitleSpansXpath = ".//span/span//span";
    private static String curriculumItemDivsXpath = ".//ul/li/div";
	private static String resourceButtonXpath = ".//button[@aria-label='Resource list']";
	private static String resourcesXpath = ".//following-sibling::ul[@role='menu']/li/a/span[2]";
	private static String downloadsDirectory =  profileDirectory + "/Downloads"; //Used by deleteFiles()
//	private static String fileNameFormat = downloadsDirectory + "/original (%d).pdf"; //NOT USED ANYMORE DUE TO NON-INCREMENTAL FILENAME APPROACH
	private static String fileNameFormat = downloadsDirectory + "/original.pdf";
	private static String fileNameRegEx = "original \\(\\d+\\).pdf"; //Used by deleteFiles()
	private static String resourcesDirectory = downloadsDirectory + "/Resources";
	private static boolean isFileDownloadEnabled = true;

	private static AtomicInteger downloadsCounter = new AtomicInteger(0);
	private static Instant executionStartTime = Instant.now();
	private static Path latestFile;

	
	public static void main(String[] args) throws IOException {
		
		deleteFiles(downloadsDirectory, fileNameRegEx);
		restoreResourcesDirectory(resourcesDirectory);

		ChromeOptions options = new ChromeOptions();
        options.addArguments("--user-data-dir=" + profileDirectory + "/AppData/Local/Google/Chrome/User Data");
        options.addArguments("--profile-directory=Profile 1");
        options.addArguments("--start-maximized");
//        options.addArguments("--disable-extensions");
//        options.addArguments("--no-sandbox");
        System.setProperty("webdriver.chrome.driver", driverAbsolutePath);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, 30);
		js = (JavascriptExecutor) driver;
		
		try {
//			driver.manage().window().setPosition(new Point(2000, 10));
			driver.manage().window().maximize();
			driver.get(starterUrl);
			List<WebElement> sectionDivs = driver.findElements(By.xpath(sectionsXpath));
			int sectionCount = sectionDivs.size();
			wait.until(webDriver -> ((JavascriptExecutor) driver).executeScript("return document.readyState").toString().equals("complete"));
			UdemyCourseStructure courseStructure = new UdemyCourseStructure(sectionCount);
			for (int i = 0; i < sectionCount; i++) {
				WebElement sectionDiv = sectionDivs.get(i);
				WebElement sectionLabelDiv = sectionDiv.findElement(By.xpath(sectionLabelXpath));
				List<WebElement> sectionTitleSpans = sectionLabelDiv.findElements(By.xpath(sectionTitleSpansXpath));
				StringBuilder sb = new StringBuilder(sectionTitleSpans.size());
				for (WebElement sectionTitleSpan : sectionTitleSpans) {
					sb
					.append(sectionTitleSpan.getText().trim())
					.append(" ");
				}
				String sectionName = sb.toString().trim();
				
				boolean isExpanded = Boolean.parseBoolean(sectionDiv.getAttribute("aria-expanded"));
				if ( ! isExpanded ) {
					sectionLabelDiv.click();
				}
				
				List<WebElement> curriculumItemDivs = sectionDiv.findElements(By.xpath(curriculumItemDivsXpath));
				int curriculumItemCount = curriculumItemDivs.size();

				courseStructure.addCourseSection(sectionName, cleanFileName(sectionName), curriculumItemCount);
				
				for (int j = 0; j < curriculumItemCount; j++) {
					WebElement curriculumItemDiv = curriculumItemDivs.get(j);
					String curriculumItemName = curriculumItemDiv.getAttribute("aria-label").trim();
					
					List<WebElement> resourceButtons = curriculumItemDiv.findElements(By.xpath(resourceButtonXpath));
					if(resourceButtons.size() != 0) {
						WebElement resourceButton = resourceButtons.get(0);
						wait.until(ExpectedConditionsPlus.javaScriptThrowsNoExceptions("arguments[0].scrollIntoView(true);", resourceButtons.get(0)));

						resourceButton.click(); //display the resources menu
						List<WebElement> resourcesSpans = resourceButton.findElements(By.xpath(resourcesXpath));
						int resourcesSpansCount = resourcesSpans.size();
						
						courseStructure
							.getCourseSection(i)
							.addCourseItem(curriculumItemName, cleanFileName(curriculumItemName), resourcesSpansCount);
						
						for (int k = 0; k < resourcesSpans.size(); k++) {
							WebElement resourcesSpan = resourcesSpans.get(k);
							String resourceName = resourcesSpan.getText().trim();

							if (isFileDownloadEnabled) {
								String downloadedFilename = downloadResource(resourcesSpan);
								courseStructure
									.getCourseSection(i)
									.getCourseItem(j)
									.addCourseResource(
										resourceName,
										cleanFileName(resourceName),
										downloadedFilename);
								String organizedResourceFilename = organizeResource(courseStructure.getCourseResource(i, j, k).get());
								courseStructure
									.getCourseResource(i, j, k)
									.get()
									.setActualFilename(organizedResourceFilename);
								
							} else {
								courseStructure
								.getCourseSection(i)
								.getCourseItem(j)
								.addCourseResource(
									resourceName,
									cleanFileName(resourceName),
									"File Downloads Disabled");
								resourceButton.click(); //hide the resources menu
							}
							resourceButton.click(); //display the resources menu hidden by resourcesSpan.click()
						}
						resourceButton.click(); //hide the resources menu
					} else {
						courseStructure
							.getCourseSection(i)
							.addCourseItem(curriculumItemName, cleanFileName(curriculumItemName));
					}

				}
				
			}
			Files.write(
					Paths.get(resourcesDirectory).resolve("Course Structure.txt"),
					courseStructure.toString().getBytes(),
					StandardOpenOption.CREATE_NEW);
			
			Files.write(
					Paths.get(resourcesDirectory).resolve("Resource files Structure.txt"),
					courseStructure.toStringForFiles().getBytes(),
					StandardOpenOption.CREATE_NEW);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
//			driver.quit();
			System.out.println("Script Execution Finalized!");
		}

	}
	
	private static String organizeResource(CourseResource courseResource) throws IOException {
		CourseItem courseItem = courseResource.getParent();
		CourseSection courseSection = courseItem.getParent();
		StringBuilder sb = new StringBuilder();
		
		sb
			.append(resourcesDirectory)
			.append("/")
			.append(courseSection.getSectionNameForFiles());
		
		if ( ! courseSection.wasDirectoryCreated()) {
			FileUtils.forceMkdir(new File(sb.toString()).getCanonicalFile());
			courseSection.setWasDirectoryCreated(true);
		}
		
		sb
			.append("/")
			.append(courseItem.getItemNameForFiles());
		if ( ! courseItem.wasDirectoryCreated()) {
			FileUtils.forceMkdir(new File(sb.toString()).getCanonicalFile());
			courseItem.setWasDirectoryCreated(true);
		}
		
		sb
			.append("/")
			.append(courseResource.getResourceNameForFiles());
		
		Path toPath = Paths.get(sb.toString()).toAbsolutePath();
		Path fromPath = Paths.get(courseResource.getOriginalFilename()).toRealPath();
		Path resultPath = Files.move(fromPath, toPath);
		
		return resultPath.toRealPath().toString();
	}
	
	public static Path getLatestFile (String downloadsDirectory) {
		Path directory = Paths.get(downloadsDirectory);
		Optional<Path> latestFile = Optional.empty();
		try {
			latestFile = Files
				.list(directory)
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
	
	public static boolean wasFileDownloadedDuringExecution (Path file) {
		
		if(file.toFile().lastModified() > executionStartTime.toEpochMilli()) {
			return true;
		}
		return false;
	}

	public static String downloadResource(WebElement element) throws IOException {
		element.click(); //Starts the download
		
		/*
		downloadsCounter.getAndIncrement();
		String fileName = String.format(fileNameFormat, downloadsCounter.get());
		Path filePath = Paths.get(fileName);
		wait
			.pollingEvery(Duration.ofSeconds(1))
			.withTimeout(Duration.ofSeconds(30))
			.until(x -> {
				return filePath.toFile().exists();});
		return filePath.toRealPath().toString();
		*/
		
		latestFile = null;
		wait
			.pollingEvery(Duration.ofSeconds(1))
			.withTimeout(Duration.ofSeconds(30))
			.until(x -> {
				latestFile = getLatestFile(downloadsDirectory);
				return wasFileDownloadedDuringExecution(latestFile);});
		downloadsCounter.getAndIncrement();
		
		return latestFile.toRealPath().toString();		
	}
	
	public static void deleteFiles(String directory, String fileNameRegEx) throws IOException {
		
		int response = JOptionPane.showConfirmDialog(
				null, 
				String.format(
						"The following data will be used to erase files:%n"
						+ "Directory:%n  %s%n"
						+ "Filename Regex:%n  %s%n%n"
						+ "Do you want to continue?",
						directory, fileNameRegEx),
				"Continue with file deletion?",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		
		if (response != JOptionPane.YES_OPTION) {
			throw new IOException("User cancelled execution due to file deletion warning.");
		}

		Files
			.find(Paths.get(directory),
					1,
					(path, basicFileAttributes) -> {
						return path.toFile().getName().matches(fileNameRegEx);
					})
			.forEach(matchingFile -> {
				try {
					System.out.printf("Deleting file: %s%n", matchingFile.toAbsolutePath());
					Files.deleteIfExists(matchingFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
	}
	
	public static void restoreResourcesDirectory(String resourcesDirectory) throws IOException {
		
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
		for (int i=0; i<len; i++) {
			int c = badFileName.codePointAt(i);
			if (Arrays.binarySearch(illegalChars, c) < 0) {
				cleanName.appendCodePoint(c);
			}
		}
		return cleanName.toString();
	}
}



class UdemyCourseStructure {
	
	@Nonnull private List<CourseSection> courseSections;

			public UdemyCourseStructure() {
		this(0);
	}

	public UdemyCourseStructure(int sectionCount) {
		super();
		courseSections = new ArrayList<CourseSection>(sectionCount);
	}

	public void addCourseSection(String sectionName, String sectionNameForFiles, int expectedItemsInSection) {
		courseSections.add(new CourseSection(sectionName, sectionNameForFiles, expectedItemsInSection, this));
	}
	
	public void addCourseSection(String sectionName, String sectionNameForFiles) {
		addCourseSection(sectionName, sectionNameForFiles, 0);
	}
	
	public CourseSection getCourseSection(int sectionNumber) {
		return courseSections.get(sectionNumber);
	}
	
	public CourseItem getCourseItem(int sectionNumber, int itemNumber) {
		return courseSections
					.get(sectionNumber)
					.getCourseItem(itemNumber);
	}
	
	public Optional<CourseResource> getCourseResource(int sectionNumber, int itemNumber, int resourceNumber) {
		return courseSections
					.get(sectionNumber)
					.getCourseItem(itemNumber)
					.getCourseResource(resourceNumber);
	}	

	@Override
	public String toString() {
		return String.format("%s", courseSections.stream().map(Object::toString).collect(Collectors.joining()));
	}
	
	public String toStringForFiles() {
		return String.format("%s", courseSections.stream().map(x -> x.toStringForFiles()).collect(Collectors.joining()));
	}
}

class CourseSection {
	private UdemyCourseStructure parent;
	
	private boolean wasDirectoryCreated = false;
	
	private String sectionName;
	private String sectionNameForFiles;
	@Nonnull private List<CourseItem> courseItems;
	
	public CourseSection(String sectionName, String sectionNameForFiles, int expectedItemsInSection, UdemyCourseStructure parent) {
		super();
		this.parent = parent;
		this.sectionName = sectionName;
		this.sectionNameForFiles = sectionNameForFiles;
		courseItems = new ArrayList<CourseItem>(expectedItemsInSection);
	}
	
	public String getSectionName() {
		return sectionName;
	}

	public String getSectionNameForFiles() {
		return sectionNameForFiles;
	}

	public void addCourseItem(String itemName, String itemNameForFiles, int expectedResourcesInItem) {
		courseItems.add(new CourseItem(itemName, itemNameForFiles, expectedResourcesInItem, this));
	}
	
	public void addCourseItem(String itemName, String itemNameForFiles) {
		courseItems.add(new CourseItem(itemName, itemNameForFiles, this));
	}
	
	public CourseItem getCourseItem(int itemNumber) {
		return courseItems.get(itemNumber);
	}
		
	public boolean wasDirectoryCreated() {
		return wasDirectoryCreated;
	}
	
	public void setWasDirectoryCreated(boolean wasDirectoryCreated) {
		this.wasDirectoryCreated = wasDirectoryCreated;
	}
	
	public UdemyCourseStructure getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return String.format("%s%n%s", sectionName, 
				courseItems
					.stream()
					.map(Object::toString)
					.collect(Collectors.joining()));
				
	}
	
	public String toStringForFiles() {
		return String.format("%s%n%s", sectionNameForFiles, 
				courseItems
					.stream()
					.map(x -> x.toStringForFiles())
					.collect(Collectors.joining()));
	}
	
}

class CourseItem {
	private CourseSection parent;

	private boolean wasDirectoryCreated = false;
	
	private String itemName;
	private String itemNameForFiles;
	@Nullable private List<CourseResource> courseResources;
	
	public CourseItem(String itemName, String itemNameForFiles, CourseSection parent) {
		super();
		this.parent = parent;
		this.itemName = itemName;
		this.itemNameForFiles = itemNameForFiles;
		courseResources = null;
	}
	
	public CourseItem(String itemName, String itemNameForFiles, int expectedResourcesInItem, CourseSection parent) {
		this(itemName, itemNameForFiles, parent);
		courseResources = new ArrayList<CourseResource>(expectedResourcesInItem);
	}
	
	public void addCourseResource(
			String resourceName, String resourceNameForFiles, String originalFilename) {
		addCourseResource(resourceName, resourceNameForFiles, originalFilename, 0);
	}
	
	public void addCourseResource(
			String resourceName, String resourceNameForFiles,
			String originalFilename, String actualFilename) {
		addCourseResource(resourceName, resourceNameForFiles, originalFilename, actualFilename, 0);
	}
	
	public void addCourseResource(
			String resourceName, String resourceNameForFiles,
			String originalFilename, int expectedResourcesInItem) {
		addCourseResource(resourceName, resourceNameForFiles, originalFilename, null, expectedResourcesInItem);
	}
	
	public void addCourseResource(
			String resourceName, String resourceNameForFiles,
			String originalFilename, String actualFilename,
			int expectedResourcesInItem) {
		if(courseResources == null) {
			courseResources = new ArrayList<CourseResource>(expectedResourcesInItem);
		}
		if(actualFilename == null) {
			courseResources.add(new CourseResource(resourceName, resourceNameForFiles, originalFilename, this));
		} else {
			courseResources.add(new CourseResource(resourceName, resourceNameForFiles, originalFilename, actualFilename, this));
		}			
	}

	public Optional<CourseResource> getCourseResource(int resourceNumber) {
		return courseResources != null ? 
				Optional.ofNullable(courseResources.get(resourceNumber)) :
				Optional.empty();
	}
		
	public String getItemName() {
		return itemName;
	}

	public String getItemNameForFiles() {
		return itemNameForFiles;
	}

	public boolean wasDirectoryCreated() {
		return wasDirectoryCreated;
	}

	public void setWasDirectoryCreated(boolean wasDirectoryCreated) {
		this.wasDirectoryCreated = wasDirectoryCreated;
	}

	public CourseSection getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return String.format("   %s%n%s", itemName, 
				courseResources != null ? 
						courseResources
							.stream()
							.map(Object::toString)
							.collect(Collectors.joining()) :
						""
						);
				
	}
	
	public String toStringForFiles() {
		return String.format("   %s%n%s", itemNameForFiles, 
				courseResources != null ? 
						courseResources
							.stream()
							.map(x -> x.toStringForFiles())
							.collect(Collectors.joining()) :
						"");
	}
	
}

 class CourseResource {
	private CourseItem parent;

	private String resourceName;
	private String resourceNameForFiles;
	private String originalFilename;
	private String actualFilename;
	
	public CourseResource(String resourceName, String resourceNameForFiles, String originalFilename, CourseItem parent) {
		this(resourceName, resourceNameForFiles, originalFilename, null, parent);
	}
	
	public CourseResource(String resourceName, String resourceNameForFiles, String originalFilename, String actualFilename, CourseItem parent) {
		super();
		this.parent = parent;
		this.resourceName = resourceName;
		this.resourceNameForFiles = resourceNameForFiles;
		this.originalFilename = originalFilename;
		this.actualFilename = actualFilename;
	}
	
	public String getResourceName() {
		return resourceName;
	}

	public String getResourceNameForFiles() {
		return resourceNameForFiles;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setActualFilename(String actualFilename) {
		this.actualFilename = actualFilename;
	}
	
	public Optional<String> getActualFilename() {
		return Optional.ofNullable(actualFilename);
	}

	public CourseItem getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return String.format("      %s%n", resourceName);
	}

	public String toStringForFiles() {
		return String.format("      %s%n            %s [original]%n            %s [actual]%n",
				resourceNameForFiles,
				originalFilename,
				actualFilename);
	}

}

class ExpectedConditionsPlus {
	  /**
	   * An expectation to check if js executable.
	   *
	   * Useful whenyou know that there should be a Javascript value or something at the stage.
	   *
	   * @param javaScript used as executable script
	   * @return true once javaScript executed without errors
	   */
	  public static ExpectedCondition<Boolean> javaScriptThrowsNoExceptions(final String javaScript, Object... args) {
	    return new ExpectedCondition<Boolean>() {
	      @Override
	      public Boolean apply(WebDriver driver) {
	        try {
	          ((JavascriptExecutor) driver).executeScript(javaScript, args);
	          return true;
	        } catch (WebDriverException e) {
	          return false;
	        }
	      }

	      @Override
	      public String toString() {
	        return String.format("js %s to be executable", javaScript);
	      }
	    };
	  }
}

