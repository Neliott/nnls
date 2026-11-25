package ch.heigvd;

import ch.heigvd.common.DiffEngine;
import ch.heigvd.common.NetworkDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.PatchFailedException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DiffEngineTest {

    static final List<String> SHORT_ORIGINAL = List.of("LINE1", "LINE2");

    static final List<String> SHORT_CHANGE_FROM_ORIGINAL = List.of("CHANGED", "LINE2");
    static final List<String> SHORT_INSERT_INTO_ORIGINAL = List.of("LINE1", "INSERTED", "LINE2");
    static final List<String> SHORT_DELETE_FROM_ORIGINAL = List.of("LINE2");

    static final List<String> SHORT_ORIGINAL2 = List.of("2LINE1", "2LINE2");
    static final List<String> SHORT_EMPTY = List.of();
    static final List<String> SHORT_EMPTYLINES = List.of("", "");

    static final List<String> MEDIUM_ORIGINAL = List.of("LINE1", "LINE2", "LINE3", "LINE4");
    static final List<String> MEDIUM_MULTI_CHANGE_FROM_ORIGINAL = List.of("CHANGED", "LINE2", "LINE3", "CHANGED");
    static final List<String> MEDIUM_MULTI_INSERT_INTO_ORIGINAL = List.of("LINE1", "INSERTED","LINE2", "LINE3", "INSERTED", "LINE4");
    static final List<String> MEDIUM_MULTI_DELETE_FROM_ORIGINAL = List.of("LINE1", "LINE3");

    static final List<String> LARGE_ORIGINAL = List.of(
            "LINE1",
            "LINE2",
            "LINE3",
            "LINE4",
            "LINE5",
            "LINE6",
            "LINE7",
            "LINE8",
            "LINE9",
            "LINE10",
            "LINE11",
            "LINE12"
    );

    static final List<String> LARGE_REVISED = List.of(
            "INSERTED",
            "LINE1",
            "INSERTED",
            "INSERTED",
            "LINE2",
            "CHANGED",
            "LINE4",
            "LINE5",
            "LINE6",
            "LINE10",
            "CHANGED",
            "CHANGED",
            "LINE12",
            "INSERTED"
    );

    @Test
    void test_computeSha256_withTwoSameLineLists_then_areSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_ORIGINAL);
        String sha2 = DiffEngine.computeSha256(SHORT_ORIGINAL);
        assertEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_withTwoDifferentLineLists_then_areNotSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_ORIGINAL);
        String sha2 = DiffEngine.computeSha256(SHORT_ORIGINAL2);
        assertNotEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_withTwoEmptyLists_then_areSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_EMPTY);
        String sha2 = DiffEngine.computeSha256(SHORT_EMPTY);
        assertEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_withEmptyListAndOriginalLineLists_then_areNotSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_ORIGINAL);
        String sha2 = DiffEngine.computeSha256(SHORT_EMPTY);
        assertNotEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_withTwoEmptyLinesLists_then_areSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_EMPTYLINES);
        String sha2 = DiffEngine.computeSha256(SHORT_EMPTYLINES);
        assertEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_withEmptyListAndEmptyLinesLists_then_areNotSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_EMPTY);
        String sha2 = DiffEngine.computeSha256(SHORT_EMPTYLINES);
        assertNotEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_withOriginalAndEmptyLinesLists_then_areNotSameSHA(){
        String sha1 = DiffEngine.computeSha256(SHORT_ORIGINAL);
        String sha2 = DiffEngine.computeSha256(SHORT_EMPTYLINES);
        assertNotEquals(sha1,sha2);
    }

    @Test
    void test_computeSha256_withEndLineAndCommaSpace_then_areNotSameSHA(){
        String sha1 = DiffEngine.computeSha256(List.of("L1","L2"));
        String sha2 = DiffEngine.computeSha256(List.of("L1, L2"));
        assertNotEquals(sha1,sha2);
    }

    @Test
    void test_computeDeltas_withOriginalAndChange_then_getChangeDelta(){
        List<NetworkDelta> expected = List.of(new NetworkDelta(DeltaType.CHANGE, 0, 1, List.of("CHANGED")));
        List<NetworkDelta> actual = DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_CHANGE_FROM_ORIGINAL);

        assertEquals(expected, actual);
    }

    @Test
    void test_computeDeltas_withOriginalAndInsert_then_getInsertDelta(){
        List<NetworkDelta> expected = List.of(new NetworkDelta(DeltaType.INSERT, 1, 0, List.of("INSERTED")));
        List<NetworkDelta> actual = DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_INSERT_INTO_ORIGINAL);

        assertEquals(expected, actual);
    }

    @Test
    void test_computeDeltas_withOriginalAndDelete_then_getDeleteDelta(){
        List<NetworkDelta> expected = List.of(new NetworkDelta(DeltaType.DELETE, 0, 1, List.of()));
        List<NetworkDelta> actual = DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_DELETE_FROM_ORIGINAL);

        assertEquals(expected, actual);
    }

    @Test
    void test_applyDeltas_withOriginalAndChange_apply_then_getChangedText() throws PatchFailedException {
        List<String> actual = DiffEngine.applyDeltas(SHORT_ORIGINAL,
                DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_CHANGE_FROM_ORIGINAL));

        assertEquals(SHORT_CHANGE_FROM_ORIGINAL, actual);
    }

    @Test
    void test_applyDeltas_withOriginalAndInsert_apply_then_getInsertedText() throws PatchFailedException {
        List<String> actual = DiffEngine.applyDeltas(SHORT_ORIGINAL,
                DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_INSERT_INTO_ORIGINAL));

        assertEquals(SHORT_INSERT_INTO_ORIGINAL, actual);
    }

    @Test
    void test_applyDeltas_withOriginalAndDelete_apply_then_getDeletedText() throws PatchFailedException {
        List<String> actual = DiffEngine.applyDeltas(SHORT_ORIGINAL,
                DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_DELETE_FROM_ORIGINAL));

        assertEquals(SHORT_DELETE_FROM_ORIGINAL, actual);
    }

    @Test
    void test_applyDeltas_withEmptyAndOriginal_apply_then_getOriginalText() throws PatchFailedException {
        List<String> actual = DiffEngine.applyDeltas(SHORT_EMPTY,
                DiffEngine.computeDeltas(SHORT_EMPTY, SHORT_ORIGINAL));

        assertEquals(SHORT_ORIGINAL, actual);
    }

    @Test
    void test_applyDeltas_withSameDocs_apply_then_getSame() throws PatchFailedException {
        List<String> actual = DiffEngine.applyDeltas(SHORT_ORIGINAL,
                DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_ORIGINAL));

        assertEquals(SHORT_ORIGINAL, actual);
    }

    @Test
    void test_applyDeltas_multi_withOriginalAndChange_apply_then_getChangedText() throws PatchFailedException {
        List<String> actual = DiffEngine.applyDeltas(MEDIUM_ORIGINAL,
                DiffEngine.computeDeltas(MEDIUM_ORIGINAL, MEDIUM_MULTI_CHANGE_FROM_ORIGINAL));

        assertEquals(MEDIUM_MULTI_CHANGE_FROM_ORIGINAL, actual);
    }

    @Test
    void test_applyDeltas_multi_withOriginalAndInsert_apply_then_getInsertedText() throws PatchFailedException {
        List<String> actual = DiffEngine.applyDeltas(MEDIUM_ORIGINAL,
                DiffEngine.computeDeltas(MEDIUM_ORIGINAL, MEDIUM_MULTI_INSERT_INTO_ORIGINAL));

        assertEquals(MEDIUM_MULTI_INSERT_INTO_ORIGINAL, actual);
    }

    @Test
    void test_applyDeltas_multi_withOriginalAndDelete_apply_then_getDeletedText() throws PatchFailedException {
        List<String> actual = DiffEngine.applyDeltas(MEDIUM_ORIGINAL,
                DiffEngine.computeDeltas(MEDIUM_ORIGINAL, MEDIUM_MULTI_DELETE_FROM_ORIGINAL));

        assertEquals(MEDIUM_MULTI_DELETE_FROM_ORIGINAL, actual);
    }

    @Test
    void test_computeDeltas_and_applyDeltas_with_largeRevised_getRevised() throws PatchFailedException {
        List<String> actual = DiffEngine.applyDeltas(LARGE_ORIGINAL,
                DiffEngine.computeDeltas(LARGE_ORIGINAL, LARGE_REVISED));

        assertEquals(LARGE_REVISED, actual);
    }

    @Test
    void test_serializeDeltas_with_smallRevisedDeltas_getCorrectJSONAccordingToProtocol(){
        List<NetworkDelta> deltas = DiffEngine.computeDeltas(SHORT_ORIGINAL, SHORT_CHANGE_FROM_ORIGINAL);
        List<NetworkDelta> actual = DiffEngine.deserializeDeltas(DiffEngine.serializeDeltas(deltas));

        assertEquals(deltas, actual);
    }

    @Test
    void test_serializeDeltas__with_smallChange_getCorrectJSONAccordingToProtocol(){
        String expected = "[{\"type\":\"CHANGE\",\"pos\":0,\"size\":1,\"lines\":[\"CHANGED\"]}]";
        String actual = DiffEngine.serializeDeltas(List.of(
                        new NetworkDelta(DeltaType.CHANGE, 0, 1, List.of("CHANGED"))
        ));

        assertEquals(expected, actual);
    }
}
