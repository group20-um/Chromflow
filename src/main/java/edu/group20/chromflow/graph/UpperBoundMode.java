package edu.group20.chromflow.graph;

/**
 * We experimented with several topological sortings for our upper-bounds. For ease of testing, we can quickly switch
 * inbetween them.
 */
public enum UpperBoundMode {
    DEGREE_DESC,
    SHUFFLE,
    UNORDERED,
    SUPERMAN
}
