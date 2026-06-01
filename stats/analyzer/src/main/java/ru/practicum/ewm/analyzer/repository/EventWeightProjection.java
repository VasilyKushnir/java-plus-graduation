package ru.practicum.ewm.analyzer.repository;

public interface EventWeightProjection {
    Long getEventId();
    Double getWeight();
}