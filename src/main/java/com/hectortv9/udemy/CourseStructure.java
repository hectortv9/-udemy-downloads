package com.hectortv9.udemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public class CourseStructure {

    @Nonnull
    private List<CourseSection> courseSections;

    public CourseStructure() {
        this(0);
    }

    public CourseStructure(int sectionCount) {
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
        return courseSections.get(sectionNumber).getCourseItem(itemNumber);
    }

    public Optional<CourseResource> getCourseResource(int sectionNumber, int itemNumber, int resourceNumber) {
        return courseSections.get(sectionNumber).getCourseItem(itemNumber).getCourseResource(resourceNumber);
    }

    @Override
    public String toString() {
        return String.format("%s", courseSections.stream().map(Object::toString).collect(Collectors.joining()));
    }

    public String toStringForFiles() {
        return String.format("%s",
                courseSections.stream().map(x -> x.toStringForFiles()).collect(Collectors.joining()));
    }
}