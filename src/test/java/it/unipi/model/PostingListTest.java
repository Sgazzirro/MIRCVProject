package it.unipi.model;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Encoder;
import it.unipi.model.implementation.PostingListCompressedImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class PostingListTest {

    // Test over different compression types
    @Parameterized.Parameters
    public static Collection<Object[]> compressionTypes() {
        return Arrays.asList(new Object[][] {
                {CompressionType.DEBUG},
                {CompressionType.BINARY},
                {CompressionType.COMPRESSED}
        });
    }

    private static final int postingLength = 175182;
    private final PostingList postingList;

    public PostingListTest(CompressionType compression) {
        this.postingList = PostingList.getInstance(compression, new VocabularyEntry());

        for (int i = 0; i < postingLength; i++)
            postingList.addPosting(i);   // Add docId i with tf 1

        if (compression == CompressionType.COMPRESSED) {
            ((PostingListCompressedImpl) postingList).setCompressedDocIds(
                    Encoder.getDocIdsEncoder().encode(postingList.getDocIdsList())
            );

            ((PostingListCompressedImpl) postingList).setCompressedTermFrequencies(
                    Encoder.getTermFrequenciesEncoder().encode(postingList.getTermFrequenciesList())
            );
        }
    }

    /**
     * Check that an exception is thrown when and only when
     * we are at the end of the list
     */
    @Test
    public void testNextEOF() {
        for (int i = 0; i < postingLength; i++)
            postingList.next();

        assertThrows(NoSuchElementException.class, postingList::next);
    }

    @Test
    public void testNextGEQEOF() {
        postingList.nextGEQ(postingLength - 1);     // Go to last posting

        assertThrows(NoSuchElementException.class, postingList::next);
    }

    @Test
    public void testNext() {
        for (int i = 0; i <= postingLength / 2; i++)
            postingList.next();

        assertEquals(postingList.docId(), postingLength / 2);
    }

    @Test
    public void testNextGEQ() {
        postingList.nextGEQ(postingLength / 2);

        assertEquals(postingList.docId(), postingLength / 2);
    }

    @Test
    public void testHasNext() {
        assertTrue(postingList.hasNext());

        postingList.nextGEQ(postingLength / 2);
        assertTrue(postingList.hasNext());

        postingList.nextGEQ(postingLength - 1);
        assertFalse(postingList.hasNext());
    }

    @Test
    public void testAddPosting() {
        int docId = postingLength - 1;      // docId of last posting
        postingList.nextGEQ(docId);

        postingList.addPosting(docId);
        assertEquals(postingList.termFrequency(), 2);

        postingList.addPosting(docId, 200);
        assertEquals(postingList.termFrequency(), 202);
    }

    @Test
    public void testReset() {
        postingList.nextGEQ(postingLength / 2);
        postingList.reset();
        postingList.next();

        assertEquals(postingList.docId(), 0);
    }
}