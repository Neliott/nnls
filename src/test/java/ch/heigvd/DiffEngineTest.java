package ch.heigvd;

import ch.heigvd.common.DiffEngine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DiffEngineTest {

    static final List<String> SHORT_ORIGINAL = Arrays.asList(new String[] {"LINE1", "LINE2"});
    static final List<String> SHORT_REVISED_FROM_ORIGINAL = Arrays.asList(new String[] {"LINE1_R", "LINE2","LINE3"});

    static final List<String> SHORT_INSERT_FROM_ORIGINAL = Arrays.asList(new String[] {"LINE1", "LINEI", "LINE2"});
    static final List<String> SHORT_CHANGE_FROM_ORIGINAL = Arrays.asList(new String[] {"LINEC", "LINE2"});
    static final List<String> SHORT_DELETE_FROM_ORIGINAL = Arrays.asList(new String[] {"LINE2"});

    static final List<String> SHORT_ORIGINAL2 = Arrays.asList(new String[] {"2LINE1", "2LINE2"});
    static final List<String> SHORT_EMPTY = List.of();
    static final List<String> SHORT_EMPTYLINES = Arrays.asList(new String[] {"", ""});

    @Test
    void test_computeSha256_computesTwoSameLineLists_then_areSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_ORIGINAL);
        String sha2 = DiffEngine.computeSha256(SHORT_ORIGINAL);
        assertEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_computesTwoDifferentLineLists_then_areNotSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_ORIGINAL);
        String sha2 = DiffEngine.computeSha256(SHORT_ORIGINAL2);
        assertNotEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_computesTwoEmptyLists_then_areSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_EMPTY);
        String sha2 = DiffEngine.computeSha256(SHORT_EMPTY);
        assertEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_computesEmptyListAndOriginalLineLists_then_areNotSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_ORIGINAL);
        String sha2 = DiffEngine.computeSha256(SHORT_EMPTY);
        assertNotEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_computesTwoEmptyLinesLists_then_areSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_EMPTYLINES);
        String sha2 = DiffEngine.computeSha256(SHORT_EMPTYLINES);
        assertEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_computesEmptyListAndEmptyLinesLists_then_areNotSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_EMPTY);
        String sha2 = DiffEngine.computeSha256(SHORT_EMPTYLINES);
        assertNotEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_computesOriginalAndEmptyLinesLists_then_areNotSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_ORIGINAL);
        String sha2 = DiffEngine.computeSha256(SHORT_EMPTYLINES);
        assertNotEquals(sha1,sha2);
    }
}
