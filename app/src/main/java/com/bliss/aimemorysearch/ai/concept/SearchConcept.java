package com.bliss.aimemorysearch.ai.concept;

import java.util.ArrayList;
import java.util.List;

public class SearchConcept {

    private final List<Concept> objects = new ArrayList<>();
    private final List<Concept> colors = new ArrayList<>();
    private final List<Concept> places = new ArrayList<>();
    private final List<Concept> actions = new ArrayList<>();

    public List<Concept> getObjects() {
        return objects;
    }

    public List<Concept> getColors() {
        return colors;
    }

    public List<Concept> getPlaces() {
        return places;
    }

    public List<Concept> getActions() {
        return actions;
    }

    public void addObject(Concept concept) {
        objects.add(concept);
    }

    public void addColor(Concept concept) {
        colors.add(concept);
    }

    public void addPlace(Concept concept) {
        places.add(concept);
    }

    public void addAction(Concept concept) {
        actions.add(concept);
    }
}