package com.hectortv9.udemy;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebElement;

public class CourseResource {
    //this pattern will extract the type of resource from an html class atribute like the following:
    //resource--link-icon--1j-Ru udi udi-file
    static final Pattern RESOURCE_TYPE_PATTERN = Pattern.compile(
            "^.*?\\budi\\p{Punct}(\\p{Alpha}+).*$", Pattern.UNICODE_CHARACTER_CLASS);

    private CourseItem parent;
    private ResourceType resourceType;

    private String resourceName;
    private String resourceNameForFiles;
    private String originalFilename;
    private String actualFilename;

    public CourseResource(ResourceType resourceType, String resourceName, String resourceNameForFiles, String originalFilename,
            CourseItem parent) {
        this(resourceType, resourceName, resourceNameForFiles, originalFilename, null, parent);
    }

    public CourseResource(ResourceType resourceType, String resourceName, String resourceNameForFiles, String originalFilename,
            String actualFilename, CourseItem parent) {
        super();
        this.parent = parent;
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.resourceNameForFiles = resourceNameForFiles;
        this.originalFilename = originalFilename;
        this.actualFilename = actualFilename;
    }

    public ResourceType getResourceType() {
        return resourceType;
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

    public static ResourceType getResourceType(WebElement htmlElement) {
        ResourceType resourceType;    
        String htmlClassAttributeName = htmlElement.getAttribute("class");
        Matcher resourceTypeMatcher = RESOURCE_TYPE_PATTERN.matcher(htmlClassAttributeName);
        if ( resourceTypeMatcher.matches() ) {
            resourceType = ResourceType.fromText(resourceTypeMatcher.group(1));
        } else {
            System.err.printf("htmlClassAttributeName: %s", htmlClassAttributeName);
            resourceType = ResourceType.UNKNOWN;
        }
        return resourceType;
    }

    @Override
    public String toString() {
        return String.format("      [%s] %s%n", resourceType, resourceName);
    }

    public String toStringForFiles() {
        return String.format("      [%s] %s%n            %s [original]%n            %s [actual]%n",
                resourceType, resourceNameForFiles, originalFilename, actualFilename);
    }

    static enum ResourceType {
        FILE("file", true),
        LINK("link", false),
        UNKNOWN("unknown", false);
        
        private final String name;
        private final boolean isDownloadable;
        
        private ResourceType(String name, boolean isDownloadable) {
            this.name = name;
            this.isDownloadable = isDownloadable;
        }
        
        public boolean isDownloadable() {
            return isDownloadable;
        }
        
        public static ResourceType fromText(String text) {
            return Arrays.stream(values())
              .filter(resourceType -> resourceType.name.equalsIgnoreCase(text))
              .findFirst()
              .orElseGet(() -> UNKNOWN);
        }

        @Override
        public String toString() {
            return name;
        }
    }
    public static void main(String[] args) {
        String clazz= "resource--link-icon--1j-Ru udi udi-file";
        
        final Pattern RESOURCE_TYPE_PATTERNX = Pattern.compile(
                "^.*?\\budi\\p{Punct}(\\p{Alpha}+).*$"
                //"\\budi\\p{Punct}(\\p{Alpha}+)"
                , Pattern.UNICODE_CHARACTER_CLASS
                );
        Matcher trimMatcherx = RESOURCE_TYPE_PATTERNX.matcher(clazz);
        boolean doMatch = trimMatcherx.matches(); // always true but must be called since it does the actual matching/grouping
        System.out.println(doMatch);
        System.out.println(trimMatcherx.group(0));
        System.out.println(trimMatcherx.group(1));

    }
}