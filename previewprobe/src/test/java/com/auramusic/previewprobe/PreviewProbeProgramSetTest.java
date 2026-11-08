package com.auramusic.previewprobe;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public final class PreviewProbeProgramSetTest {
    @Test public void missingArgumentUsesCompleteDefaultSet() {
        assertEquals(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H"),
                PreviewProbeProgramSet.parse(null));
    }

    @Test public void emptyArgumentClearsPrograms() {
        assertEquals(Arrays.asList(), PreviewProbeProgramSet.parse("  "));
    }

    @Test public void orderAndCaseAreNormalizedWithoutReordering() {
        assertEquals(Arrays.asList("B", "A", "C"), PreviewProbeProgramSet.parse(" b, a, C "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateIdsAreRejected() {
        PreviewProbeProgramSet.parse("A,a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownIdsAreRejected() {
        PreviewProbeProgramSet.parse("A,I");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyEntriesAreRejected() {
        PreviewProbeProgramSet.parse("A,");
    }
}
