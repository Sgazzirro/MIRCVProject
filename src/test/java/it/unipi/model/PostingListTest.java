package it.unipi.model;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.implementation.EliasFano;
import it.unipi.encoding.implementation.Simple9;
import it.unipi.model.implementation.PostingListCompressedImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.EOFException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class PostingListTest {

    // Test over different compression types
    @Parameterized.Parameters
    public static Collection<Object[]> compressionTypes() {
        return Arrays.asList(new Object[][] {
                {CompressionType.DEBUG},
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
                    new EliasFano().encode(postingList.getDocIdsDecompressedList())
            );

            ((PostingListCompressedImpl) postingList).setCompressedTermFrequencies(
                    new Simple9(true).encode(postingList.getTermFrequenciesDecompressedList())
            );
        }
    }

    /**
     * Check that an exception is thrown when and only when
     * we are at the end of the list
     */
    @Test
    public void testNextEOF() throws EOFException {
        for (int i = 0; i < postingLength; i++)
            postingList.next();

        assertThrows(EOFException.class, postingList::next);
    }

    @Test
    public void testNextGEQEOF() throws EOFException {
        postingList.nextGEQ(postingLength - 1);     // Go to last posting

        assertThrows(EOFException.class, postingList::next);
    }

    @Test
    public void testNext() throws EOFException {
        for (int i = 0; i <= postingLength / 2; i++)
            postingList.next();

        assertEquals(postingList.docId(), postingLength / 2);
    }

    @Test
    public void testNextGEQ() throws EOFException {
        postingList.nextGEQ(postingLength / 2);

        assertEquals(postingList.docId(), postingLength / 2);
    }

    @Test
    public void testHasNext() throws EOFException {
        assertTrue(postingList.hasNext());

        postingList.nextGEQ(postingLength / 2);
        assertTrue(postingList.hasNext());

        postingList.nextGEQ(postingLength - 1);
        assertFalse(postingList.hasNext());
    }

    @Test
    public void testAddPosting() throws EOFException {
        int docId = postingLength - 1;      // docId of last posting
        postingList.nextGEQ(docId);

        postingList.addPosting(docId);
        assertEquals(postingList.termFrequency(), 2);

        postingList.addPosting(docId, 200);
        assertEquals(postingList.termFrequency(), 202);
    }

    @Test
    public void testReset() throws EOFException {
        postingList.nextGEQ(postingLength / 2);
        postingList.reset();
        postingList.next();

        assertEquals(postingList.docId(), 0);
    }

}