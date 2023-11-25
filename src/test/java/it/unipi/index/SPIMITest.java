package it.unipi.index;

import it.unipi.model.DocumentStream;
import it.unipi.model.VocabularyEntry;
import it.unipi.model.implementation.*;
import it.unipi.utils.Fetcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SPIMITest {

    @InjectMocks
    newSPIMI spimi;

    @Mock
    List<VocabularyEntry> entry;
    @Mock
    InMemoryIndexing i;
    @Mock
    DocumentStream stream;

    @Mock
    Fetcher mockFetcher1;

    @Mock
    Fetcher mockFetcher2;
    @Mock
    newSPIMI mockSpimi;

    @Test
    public void testAvailableMemory_WHEN_full(){
        long usedMemory = Runtime.getRuntime().totalMemory();
        long startmemory = Runtime.getRuntime().totalMemory() - 1000;
        spimi.setLimit(100);
        assertFalse(spimi.availableMemory(usedMemory, startmemory));
    }

    @Test
    public void testAvailableMemory_WHEN_Ok(){
        long usedMemory = Runtime.getRuntime().totalMemory();
        long startmemory = Runtime.getRuntime().totalMemory() - 100;
        spimi.setLimit(1000);
        assertTrue(spimi.availableMemory(usedMemory, startmemory));
    }

    @Test
    public void test_WHEN_finishedStream_THEN_finish(){
        Document emptyDocument = null;
        when(stream.nextDoc()).thenReturn(emptyDocument);
        spimi.invertBlock("test");
        verify(stream, times(1)).nextDoc();
        assertTrue(spimi.finished());
    }

    @Test
    public void testNumberOfBlocks(){
        Document dummyDocument = new Document();
        dummyDocument.setText("dummy doc");
        spimi.setLimit(1);
        when(stream.nextDoc()).thenReturn(dummyDocument);

        for(int i = 0; i < 10; i++){
            spimi.invertBlock("test");
        }

        assertEquals(10, spimi.getNext_block());
    }


    @Test
    public void testLowestTerm() throws IOException {
        newSPIMI mockSpimi = spy(spimi);
        List<Fetcher> mockReadVocBuffers = new ArrayList<>();
        mockReadVocBuffers.add(mockFetcher1);
        mockReadVocBuffers.add(mockFetcher2);

        VocabularyEntry entry1 = new VocabularyEntry();

        when(mockFetcher1.loadVocEntry()).thenReturn(
                new AbstractMap.SimpleEntry<>("aaa", entry1),
                null
        );
        when(mockFetcher2.loadVocEntry()).thenReturn(
                new AbstractMap.SimpleEntry<>("bab", entry1),
                null
        );

        when(mockSpimi.getNext_block()).thenReturn(2);
        when(mockSpimi.mergeEntries(anyList())).thenReturn(new VocabularyEntry());

        mockSpimi.mergeAllBlocks(mockReadVocBuffers);
        ArrayList<String> lowests = new ArrayList<>();
        verify(i, atLeastOnce()).dumpVocabularyLine(
                argThat(entry -> {
                    String lowestTerm = entry.getKey();
                    lowests.add(lowestTerm);
                    return true;
                })
        );
        assertEquals(lowests.get(0), "aaa");
        assertEquals(lowests.get(1), "bab");
    }

}