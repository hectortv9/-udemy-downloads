package com.hectortv9.udemy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public class CourseSection {
    private CourseStructure parent;

    private boolean wasDirectoryCreated = false;

    private String sectionName;
    private String sectionNameForFiles;
    @Nonnull
    private List<CourseItem> courseItems;

    public CourseSection(String sectionName, String sectionNameForFiles, int expectedItemsInSection,
            CourseStructure parent) {
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

    public CourseStructure getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return String.format("%s%n%s", sectionName,
                courseItems.stream().map(Object::toString).collect(Collectors.joining()));

    }

    public String toStringForFiles() {
        return String.format("%s%n%s", sectionNameForFiles,
                courseItems.stream().map(x -> x.toStringForFiles()).collect(Collectors.joining()));
    }

}