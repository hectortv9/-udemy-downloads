package com.hectortv9.udemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.hectortv9.udemy.CourseResource.ResourceType;

public class CourseItem {
    private CourseSection parent;

    private boolean wasDirectoryCreated = false;

    private String itemName;
    private String itemNameForFiles;
    @Nullable
    private List<CourseResource> courseResources;

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

    public void addCourseResource(ResourceType resourceType, String resourceName, String resourceNameForFiles, String originalFilename) {
        addCourseResource(resourceType, resourceName, resourceNameForFiles, originalFilename, 0);
    }

    public void addCourseResource(ResourceType resourceType, String resourceName, String resourceNameForFiles, String originalFilename,
            String actualFilename) {
        addCourseResource(resourceType, resourceName, resourceNameForFiles, originalFilename, actualFilename, 0);
    }

    public void addCourseResource(ResourceType resourceType, String resourceName, String resourceNameForFiles, String originalFilename,
            int expectedResourcesInItem) {
        addCourseResource(resourceType, resourceName, resourceNameForFiles, originalFilename, null, expectedResourcesInItem);
    }

    public void addCourseResource(ResourceType resourceType, String resourceName, String resourceNameForFiles, String originalFilename,
            String actualFilename, int expectedResourcesInItem) {
        if (courseResources == null) {
            courseResources = new ArrayList<CourseResource>(expectedResourcesInItem);
        }
        if (actualFilename == null) {
            courseResources.add(new CourseResource(resourceType, resourceName, resourceNameForFiles, originalFilename, this));
        } else {
            courseResources.add(
                    new CourseResource(resourceType, resourceName, resourceNameForFiles, originalFilename, actualFilename, this));
        }
    }

    public Optional<CourseResource> getCourseResource(int resourceNumber) {
        return courseResources != null ? Optional.ofNullable(courseResources.get(resourceNumber)) : Optional.empty();
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
                courseResources != null ? courseResources.stream().map(Object::toString).collect(Collectors.joining())
                        : "");

    }

    public String toStringForFiles() {
        return String.format("   %s%n%s", itemNameForFiles,
                courseResources != null
                        ? courseResources.stream().map(x -> x.toStringForFiles()).collect(Collectors.joining())
                        : "");
    }

}