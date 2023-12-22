package it.unipi.io;

import it.unipi.model.Document;
import it.unipi.utils.Constants;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DocumentStreamTest {

    private DocumentStream stream;

    @Before
    public void setUp() throws IOException {
        stream = new DocumentStream(Constants.COLLECTION_FILE);
    }

    @Test
    public void testFirstDoc() throws IOException {
        Document doc = stream.nextDoc();
        assertEquals(doc.getId(), 0);
    }

    @Test
    public void testSkipDocs() throws IOException {
        Document doc = null;

        int docId = 8;
        String text = "In June 1942, the United States Army Corps of Engineersbegan the Manhattan Project- The secret name for the 2 atomic bombs.";

        // Skip to docId = 8
        for (int i = 0; i <= docId; i++)
            doc = stream.nextDoc();

        assertEquals(doc.getId(), docId);
        assertEquals(doc.getText(), text);
    }

    @Test
    public void testGetDoc() throws IOException {
        int docId = 8;
        String text = "In June 1942, the United States Army Corps of Engineersbegan the Manhattan Project- The secret name for the 2 atomic bombs.";

        Document doc = stream.getDoc(docId);

        assertEquals(doc.getId(), docId);
        assertEquals(doc.getText(), text);
    }
}