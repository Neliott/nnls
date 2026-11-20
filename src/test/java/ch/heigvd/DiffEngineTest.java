package ch.heigvd;

import ch.heigvd.common.DiffEngine;
import ch.heigvd.common.NetworkDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.PatchFailedException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DiffEngineTest {

    static final List<String> SHORT_ORIGINAL = Arrays.asList(new String[] {"LINE1", "LINE2"});
    static final List<String> SHORT_REVISED_FROM_ORIGINAL = Arrays.asList(new String[] {"LINE1_R", "LINE2","LINE3"});

    static final List<String> SHORT_CHANGE_FROM_ORIGINAL = Arrays.asList(new String[] {"CHANGED", "LINE2"});
    static final List<String> SHORT_INSERT_TO_ORIGINAL = Arrays.asList(new String[] {"LINE1", "INSERTED", "LINE2"});
    static final List<String> SHORT_DELETE_FROM_ORIGINAL = Arrays.asList(new String[] {"LINE2"});

    static final List<String> SHORT_ORIGINAL2 = Arrays.asList(new String[] {"2LINE1", "2LINE2"});
    static final List<String> SHORT_EMPTY = List.of();
    static final List<String> SHORT_EMPTYLINES = Arrays.asList(new String[] {"", ""});

    static final List<String> MEDIUM_ORIGINAL = Arrays.asList(new String[] {"LINE1", "LINE2", "LINE3", "LINE4"});
    static final List<String> MEDIUM_MULTI_CHANGE_FROM_ORIGINAL = Arrays.asList(new String[] {"CHANGED", "LINE2", "LINE3", "CHANGED"});

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

    @Test
    void test_computeDeltas_computesOriginalAndChange_then_getChangeDelta(){
        List<NetworkDelta> expected = List.of(new NetworkDelta(DeltaType.CHANGE, 0, 1, List.of("CHANGED")));
        List<NetworkDelta> actual = DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_CHANGE_FROM_ORIGINAL);

        assertEquals(expected, actual);
    }

    @Test
    void test_computeDeltas_computesOriginalAndInsert_then_getInsertDelta(){
        List<NetworkDelta> expected = List.of(new NetworkDelta(DeltaType.INSERT, 1, 0, List.of("INSERTED")));
        List<NetworkDelta> actual = DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_INSERT_TO_ORIGINAL);

        assertEquals(expected, actual);
    }

    @Test
    void test_computeDeltas_computesOriginalAndDelete_then_getDeleteDelta(){
        List<NetworkDelta> expected = List.of(new NetworkDelta(DeltaType.DELETE, 0, 1, List.of()));
        List<NetworkDelta> actual = DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_DELETE_FROM_ORIGINAL);

        assertEquals(expected, actual);
    }

    @Test
    void test_applyDeltas_computesOriginalAndChange_apply_then_getChangedText() throws PatchFailedException {
        List<String> expected = SHORT_CHANGE_FROM_ORIGINAL;
        List<String> actual = DiffEngine.applyDeltas(SHORT_ORIGINAL,
                DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_CHANGE_FROM_ORIGINAL));

        assertEquals(expected, actual);
    }

    @Test
    void test_applyDeltas_computesOriginalAndInsert_apply_then_getInsertedText() throws PatchFailedException {
        List<String> expected = SHORT_INSERT_TO_ORIGINAL;
        List<String> actual = DiffEngine.applyDeltas(SHORT_ORIGINAL,
                DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_INSERT_TO_ORIGINAL));

        assertEquals(expected, actual);
    }

    @Test
    void test_applyDeltas_computesOriginalAndDelete_apply_then_getDeletedText() throws PatchFailedException {
        List<String> expected = SHORT_DELETE_FROM_ORIGINAL;
        List<String> actual = DiffEngine.applyDeltas(SHORT_ORIGINAL,
                DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_DELETE_FROM_ORIGINAL));

        assertEquals(expected, actual);
    }
}
