package at.esque.kafka;

import at.esque.kafka.topics.model.KafkaMessageBookWrapper;
import at.esque.kafka.topics.model.KafkaMessageForMessageBook;
import org.junit.Test;
import static org.junit.Assert.*;


import java.io.File;
import java.util.*;

public class ControllerTest {

    @Test
    public void testAddMessagesToSend() {
        // Given
        List<KafkaMessageBookWrapper> messagesToSend = new ArrayList<>();
        File playFile = new File("src/test/resources/csv_testfiles/testAddMessagesToSend.csv");

        KafkaMessageForMessageBook message1Expected = new KafkaMessageForMessageBook();
        message1Expected.setKey("${my_uuid1:UUID}");
        message1Expected.setPartition(-1);
        message1Expected.setTimestamp("2022-11-06T17:11:52.919Z");
        message1Expected.setValue("{\"version\":0,\"newId\":\"${my_uuid1:UUID}\"}");

        KafkaMessageForMessageBook message2Expected = new KafkaMessageForMessageBook();
        message2Expected.setKey("${my_uuid2:UUID}");
        message2Expected.setPartition(-1);
        message2Expected.setTimestamp("2022-11-06T17:13:52.919Z");
        message2Expected.setValue("{\"version\":0,\"newId\":\"${my_uuid2:UUID}\"}");

        KafkaMessageForMessageBook message3Expected = new KafkaMessageForMessageBook();
        message3Expected.setKey("${my_int1:RANDOM_INT_OF_LENGTH_4}");
        message3Expected.setPartition(-1);
        message3Expected.setTimestamp("2022-11-06T17:14:52.919Z");
        message3Expected.setValue("{\"version\":0,\"newId\":\"${my_int1:RANDOM_INT_OF_LENGTH_4}\"}");

        KafkaMessageForMessageBook message4Expected = new KafkaMessageForMessageBook();
        message4Expected.setKey("${my_int2:RANDOM_INT_OF_LENGTH_12}");
        message4Expected.setPartition(-1);
        message4Expected.setTimestamp("2022-11-06T17:17:52.919Z");
        message4Expected.setValue("{\"version\":0,\"newId\":\"${my_int2:RANDOM_INT_OF_LENGTH_12}\"}");


        // When
        Controller controller = new Controller();
        controller.addMessagesToSend(messagesToSend, playFile);

        // Then
        assertEquals(4, messagesToSend.size());

        assertEquals(message1Expected.getPartition(), messagesToSend.get(0).getWrappedMessage().getPartition());
        assertEquals(message1Expected.getKey(), messagesToSend.get(0).getWrappedMessage().getKey());
        assertEquals(message1Expected.getTimestamp(), messagesToSend.get(0).getWrappedMessage().getTimestamp());
        assertEquals(message1Expected.getValue(), messagesToSend.get(0).getWrappedMessage().getValue());

        assertEquals(message2Expected.getPartition(), messagesToSend.get(1).getWrappedMessage().getPartition());
        assertEquals(message2Expected.getKey(), messagesToSend.get(1).getWrappedMessage().getKey());
        assertEquals(message2Expected.getTimestamp(), messagesToSend.get(1).getWrappedMessage().getTimestamp());
        assertEquals(message2Expected.getValue(), messagesToSend.get(1).getWrappedMessage().getValue());

        assertEquals(message3Expected.getPartition(), messagesToSend.get(2).getWrappedMessage().getPartition());
        assertEquals(message3Expected.getKey(), messagesToSend.get(2).getWrappedMessage().getKey());
        assertEquals(message3Expected.getTimestamp(), messagesToSend.get(2).getWrappedMessage().getTimestamp());
        assertEquals(message3Expected.getValue(), messagesToSend.get(2).getWrappedMessage().getValue());

        assertEquals(message4Expected.getPartition(), messagesToSend.get(3).getWrappedMessage().getPartition());
        assertEquals(message4Expected.getKey(), messagesToSend.get(3).getWrappedMessage().getKey());
        assertEquals(message4Expected.getTimestamp(), messagesToSend.get(3).getWrappedMessage().getTimestamp());
        assertEquals(message4Expected.getValue(), messagesToSend.get(3).getWrappedMessage().getValue());
    }

    @Test
    public void testAddReplacementEntries_UUID() {
        // Given
        Map<String, String> replacementMap = new HashMap<>();
        String matchingString = "${value1:UUID}";
        Controller controller = new Controller();

        // When
        controller.addReplacementEntries(replacementMap, matchingString);

        // Then
        assertEquals(1, replacementMap.size());
        assertTrue(replacementMap.containsKey("${value1:UUID}"));
        // Verify it's a valid UUID format
        String uuidValue = replacementMap.get("${value1:UUID}");
        assertNotNull(uuidValue);
        UUID.fromString(uuidValue); // Should not throw exception
    }

    @Test
    public void testAddReplacementEntries_MultipleUUIDs() {
        // Given
        Map<String, String> replacementMap = new HashMap<>();
        String matchingString = "${value1:UUID} and ${value2:UUID}";
        Controller controller = new Controller();

        // When
        controller.addReplacementEntries(replacementMap, matchingString);

        // Then
        assertEquals(2, replacementMap.size());
        assertTrue(replacementMap.containsKey("${value1:UUID}"));
        assertTrue(replacementMap.containsKey("${value2:UUID}"));
    }

    @Test
    public void testAddReplacementEntries_RandomIntOfLength() {
        // Given
        Map<String, String> replacementMap = new HashMap<>();
        String matchingString = "${id:RANDOM_INT_OF_LENGTH_5}";
        Controller controller = new Controller();

        // When
        controller.addReplacementEntries(replacementMap, matchingString);

        // Then
        assertEquals(1, replacementMap.size());
        assertTrue(replacementMap.containsKey("${id:RANDOM_INT_OF_LENGTH_5}"));
        String randomInt = replacementMap.get("${id:RANDOM_INT_OF_LENGTH_5}");
        assertEquals(5, randomInt.length());
        assertTrue(randomInt.matches("\\d{5}"));
    }

    @Test
    public void testAddReplacementEntries_RandomIntDifferentLengths() {
        // Given
        Map<String, String> replacementMap = new HashMap<>();
        String matchingString = "${small:RANDOM_INT_OF_LENGTH_1} ${large:RANDOM_INT_OF_LENGTH_19}";
        Controller controller = new Controller();

        // When
        controller.addReplacementEntries(replacementMap, matchingString);

        // Then
        assertEquals(2, replacementMap.size());
        assertEquals(1, replacementMap.get("${small:RANDOM_INT_OF_LENGTH_1}").length());
        assertEquals(19, replacementMap.get("${large:RANDOM_INT_OF_LENGTH_19}").length());
    }

    @Test(expected = RuntimeException.class)
    public void testAddReplacementEntries_InvalidLength_TooLarge() {
        // Given
        Map<String, String> replacementMap = new HashMap<>();
        String matchingString = "${id:RANDOM_INT_OF_LENGTH_20}";
        Controller controller = new Controller();

        // When
        controller.addReplacementEntries(replacementMap, matchingString);
    }

    @Test(expected = RuntimeException.class)
    public void testAddReplacementEntries_InvalidLength_Zero() {
        // Given
        Map<String, String> replacementMap = new HashMap<>();
        String matchingString = "${id:RANDOM_INT_OF_LENGTH_0}";
        Controller controller = new Controller();

        // When
        controller.addReplacementEntries(replacementMap, matchingString);
    }

    @Test(expected = RuntimeException.class)
    public void testAddReplacementEntries_InvalidLength_MissingNumber() {
        // Given
        Map<String, String> replacementMap = new HashMap<>();
        String matchingString = "${id:RANDOM_INT_OF_LENGTH}";
        Controller controller = new Controller();

        // When
        controller.addReplacementEntries(replacementMap, matchingString);
    }

    @Test(expected = RuntimeException.class)
    public void testAddReplacementEntries_UnsupportedType() {
        // Given
        Map<String, String> replacementMap = new HashMap<>();
        String matchingString = "${id:UNSUPPORTED_TYPE}";
        Controller controller = new Controller();

        // When
        controller.addReplacementEntries(replacementMap, matchingString);
    }

    @Test
    public void testAddReplacementEntries_NoMatches() {
        // Given
        Map<String, String> replacementMap = new HashMap<>();
        String matchingString = "This has no placeholders";
        Controller controller = new Controller();

        // When
        controller.addReplacementEntries(replacementMap, matchingString);

        // Then
        assertEquals(0, replacementMap.size());
    }

    @Test
    public void testAddReplacementEntries_MixedTypes() {
        // Given
        Map<String, String> replacementMap = new HashMap<>();
        String matchingString = "{\"id\":\"${id:UUID}\",\"number\":${num:RANDOM_INT_OF_LENGTH_10}}";
        Controller controller = new Controller();

        // When
        controller.addReplacementEntries(replacementMap, matchingString);

        // Then
        assertEquals(2, replacementMap.size());
        assertTrue(replacementMap.containsKey("${id:UUID}"));
        assertTrue(replacementMap.containsKey("${num:RANDOM_INT_OF_LENGTH_10}"));
        assertEquals(10, replacementMap.get("${num:RANDOM_INT_OF_LENGTH_10}").length());
    }

}
