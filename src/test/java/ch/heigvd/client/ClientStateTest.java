package ch.heigvd.client;

import ch.heigvd.DiffEngineTest;
import ch.heigvd.common.DiffEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientStateTest {

    @TempDir
    Path tempDir;

    private Path testFile;
    private ClientState state;

    @BeforeEach
    void setUp() throws IOException {
        testFile = tempDir.resolve("sample.txt");
        Files.write(testFile, DiffEngineTest.SHORT_ORIGINAL, StandardCharsets.UTF_8);
        state = new ClientState(testFile);
    }

    @Test
    void test_readLocalLines_withExistingFile_then_returnsExpectedLines() {
        List<String> lines = state.readLocalLines();
        assertEquals(DiffEngineTest.SHORT_ORIGINAL, lines);
    }

    @Test
    void test_writeLocalLines_withMediumLines_then_overwritesFileContent() throws IOException {
        state.writeLocalLines(DiffEngineTest.MEDIUM_ORIGINAL);

        List<String> diskLines = Files.readAllLines(testFile, StandardCharsets.UTF_8);
        assertEquals(DiffEngineTest.MEDIUM_ORIGINAL, diskLines);
    }

    @Test
    void test_writeLocalLines_withEmptyLinesList_then_readsBackEmptyLines() {
        state.writeLocalLines(DiffEngineTest.SHORT_EMPTYLINES);

        assertEquals(DiffEngineTest.SHORT_EMPTYLINES, state.readLocalLines());
    }

    @Test
    void test_getCurrentChecksum_withModifiedFile_then_matchesComputedSha256() {
        String expected = DiffEngine.computeSha256(DiffEngineTest.SHORT_ORIGINAL);
        assertEquals(expected, state.getCurrentChecksum());

        state.writeLocalLines(DiffEngineTest.SHORT_CHANGE_FROM_ORIGINAL);
        String updatedExpected = DiffEngine.computeSha256(DiffEngineTest.SHORT_CHANGE_FROM_ORIGINAL);
        assertEquals(updatedExpected, state.getCurrentChecksum());
    }

    @Test
    void test_computeLocalDiff_withChangedLocalFile_then_returnsNonEmptyDeltas() {
        String baseChecksum = DiffEngine.computeSha256(DiffEngineTest.LARGE_ORIGINAL);
        state.updateSyncedState(DiffEngineTest.LARGE_ORIGINAL, baseChecksum);
        state.writeLocalLines(DiffEngineTest.LARGE_REVISED);

        assertEquals(baseChecksum, state.getSyncedChecksum());

        var deltas = state.computeLocalDiff();
        assertNotNull(deltas);
        assertFalse(deltas.isEmpty());
    }

    @Test
    void test_acceptRemote_withRemoteLinesAndChecksum_then_updatesFileAndChecksums() {
        List<String> remote = DiffEngineTest.SHORT_ORIGINAL2;
        String remoteChecksum = DiffEngine.computeSha256(remote);

        state.acceptRemote(remote, remoteChecksum);

        assertEquals(remote, state.readLocalLines());
        assertEquals(remoteChecksum, state.getSyncedChecksum());
        assertEquals(remoteChecksum, state.getCurrentChecksum());
    }

    @Test
    void test_keepLocalAndRebase_withRemoteChecksum_then_updatesSyncedChecksumWithoutTouchingFile() {
        List<String> remote = DiffEngineTest.SHORT_CHANGE_FROM_ORIGINAL;
        String remoteChecksum = DiffEngine.computeSha256(remote);
        String localChecksumBefore = state.getCurrentChecksum();

        state.keepLocalAndRebase(remote, remoteChecksum);

        assertEquals(DiffEngineTest.SHORT_ORIGINAL, state.readLocalLines());
        assertEquals(localChecksumBefore, state.getCurrentChecksum());
        assertEquals(remoteChecksum, state.getSyncedChecksum());
    }

    @Test
    void test_readLocalLines_withMissingFile_then_throwsRuntimeException() {
        Path missingFile = tempDir.resolve("does-not-exist.txt");
        ClientState missingState = new ClientState(missingFile);

        assertThrows(RuntimeException.class, missingState::readLocalLines);
    }
}