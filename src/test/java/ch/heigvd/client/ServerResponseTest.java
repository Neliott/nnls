package ch.heigvd.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link ServerResponse}. */
class ServerResponseTest {

    @Test
    void parsesValidResponses() {
        assertEquals(ServerResponse.Kind.OK, ServerResponse.parse("OK").getKind());
        assertEquals("A7B9X", ServerResponse.parse("OK A7B9X").getPayload());
        assertEquals(ServerResponse.Kind.UP_TO_DATE, ServerResponse.parse("UP_TO_DATE").getKind());
        assertEquals(ServerResponse.Kind.BYE, ServerResponse.parse("BYE").getKind());

        String json = "[{\"type\":\"INSERT\",\"pos\":0,\"size\":0,\"lines\":[\"Hi\"]}]";
        ServerResponse sync = ServerResponse.parse("SYNC chk_2 " + json);
        assertEquals(ServerResponse.Kind.SYNC, sync.getKind());
        assertEquals("chk_2 " + json, sync.getPayload());
    }

    @Test
    void parsesErrorCodesOneThroughFive() {
        assertEquals(1, ServerResponse.parse("ERROR 1 UNKNOWN_COMMAND").getErrorCode());
        assertEquals(2, ServerResponse.parse("ERROR 2 INVALID_STATE").getErrorCode());
        assertEquals(3, ServerResponse.parse("ERROR 3 ROOM_NOT_FOUND").getErrorCode());
        assertEquals(5, ServerResponse.parse("ERROR 5 BAD_ARGUMENTS").getErrorCode());

        ServerResponse err4 = ServerResponse.parse("ERROR 4 OUTDATED_CHECKSUM chk_2 []");
        assertEquals(4, err4.getErrorCode());
        assertEquals("OUTDATED_CHECKSUM", err4.getErrorReason());
        assertEquals("chk_2 []", err4.getErrorData());
        assertTrue(err4.formatErrorMessage().contains("Conflict"));
    }

    @Test
    void rejectsMalformedResponses() {
        assertThrows(IllegalArgumentException.class, () -> ServerResponse.parse(null));
        assertThrows(IllegalArgumentException.class, () -> ServerResponse.parse("  "));
        assertThrows(IllegalArgumentException.class, () -> ServerResponse.parse("NOPE"));
        assertThrows(IllegalArgumentException.class, () -> ServerResponse.parse("BYE extra"));
        assertThrows(IllegalArgumentException.class, () -> ServerResponse.parse("SYNC"));
        assertThrows(IllegalArgumentException.class, () -> ServerResponse.parse("ERROR x BAD"));
    }
}
