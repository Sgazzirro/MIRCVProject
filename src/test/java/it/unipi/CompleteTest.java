package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.index.InMemoryIndexing;
import it.unipi.index.SPIMIIndex;
import it.unipi.io.Dumper;
import it.unipi.io.DocumentStream;
import it.unipi.model.Document;
import it.unipi.model.implementation.DocumentIndexImpl;
import it.unipi.encoding.implementation.TokenizerImpl;
import it.unipi.model.implementation.VocabularyImpl;
import it.unipi.scoring.DocumentScore;
import it.unipi.scoring.MaxScore;
import it.unipi.utils.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.PriorityQueue;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompleteTest {
    @Mock
    static DocumentStream ds;

    @InjectMocks
    InMemoryIndexing indexerSingleBlock;

    VocabularyImpl vocabulary;

    @Before
    public void setup() {
        Constants.setPath(Constants.testPath);

        IOUtils.deleteDirectory(Constants.testPath);
        IOUtils.createDirectory(Constants.testPath);

        when(ds.nextDoc()).thenReturn(
                new Document("0\tduck duck duck"),
                new Document("1\tbeij dish duck duck"),
                new Document("2\tduck duck rabbit recip"),
                new Document("3\trabbit recip recip duck"),
                null
        );
        CompressionType compression = CompressionType.COMPRESSED;
        Constants.setCompression(compression);
        // Dumping
        indexerSingleBlock = new InMemoryIndexing(new VocabularyImpl(), Dumper.getInstance(compression), new DocumentIndexImpl());
        new SPIMIIndex(compression, ds, indexerSingleBlock).buildIndexSPIMI(Constants.testPath);
        vocabulary = new VocabularyImpl();
    }

    @Test
    public void testQueryDuck(){
        String query = "duck";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");
        Assert.assertEquals(results.size(), 4);

        for(int i=0; i<4; i++){
            DocumentScore documentScore = results.poll();
            Assert.assertEquals(0.0, documentScore.score, 0.01);
        }
        Assert.assertNull(results.poll());
    }

    @Test
    public void testQueryRabbit(){
        String query = "rabbit";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");
        Assert.assertEquals(results.size(), 2);

        DocumentScore documentScore2 = results.poll();
        DocumentScore documentScore1 = results.poll();

        Assert.assertEquals(documentScore1.score, Math.log10(2.0), 0.01);
        Assert.assertEquals(documentScore2.score, Math.log10(2.0), 0.01);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testQueryRecip(){
        String query = "recip";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");
        Assert.assertEquals(results.size(), 2);

        DocumentScore documentScore1 = results.poll();
        DocumentScore documentScore2 = results.poll();

        Assert.assertEquals(documentScore2.score, (1+Math.log10(2.0))*Math.log10(2.0), 0.01);
        Assert.assertEquals(documentScore1.score, Math.log10(2.0), 0.01);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testQueryEmpty(){
        // fetching
        String query = "cat";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");
        Assert.assertNull(results);
    }

    @Test
    public void testQueryRabbitRecip(){
        String query = "rabbit recip";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");

        DocumentScore documentScore2 = results.poll();
        DocumentScore documentScore1 = results.poll();

        Assert.assertEquals(documentScore1.score, (1+Math.log10(2.0))*Math.log10(2.0)+Math.log10(2.0), 0.01);
        Assert.assertEquals(documentScore2.score, 2*Math.log10(2.0), 0.01);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testResetPostingList(){
        String query = "rabbit recip";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        for(int i=0; i<2; i++) {
            double start = System.currentTimeMillis();

            PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");

            DocumentScore documentScore2 = results.poll();
            DocumentScore documentScore1 = results.poll();

            Assert.assertEquals(documentScore1.score,(1+Math.log10(2.0))*Math.log10(2.0)+Math.log10(2.0) , 0.01);
            Assert.assertEquals(documentScore2.score, 2 * Math.log10(2.0), 0.01);

            Assert.assertNull(results.poll());

            System.out.println("Iterazione: "+i+" Tempo impiegato: "+(System.currentTimeMillis()-start));
        }
    }
    @Test
    public void testConjunctiveRabbitRecip(){
        String query = "rabbit recip";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "conjunctive");

        DocumentScore documentScore2 = results.poll();
        DocumentScore documentScore1 = results.poll();

        Assert.assertEquals(documentScore1.score, ((1+Math.log10(2.0))*Math.log10(2.0) + Math.log10(2.0)), 0.01);
        Assert.assertEquals(documentScore2.score, 2*Math.log10(2.0), 0.01);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testConjunctiveEmpty(){
        String query = "rabbit cat";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "conjunctive");

        Assert.assertNull(results.poll());
    }

    @Test
    public void testResetConjunctive(){
        String query = "duck rabbit";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        for(int i=0; i<2; i++) {
            double start = System.currentTimeMillis();

            PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "conjunctive");

            DocumentScore documentScore2 = results.poll();
            DocumentScore documentScore1 = results.poll();

            Assert.assertEquals(documentScore1.score, Math.log10(2.0), 0.01);
            Assert.assertEquals(documentScore2.score, Math.log10(2.0), 0.01);

            Assert.assertNull(results.poll());

            System.out.println("Iterazione: "+i+" Tempo impiegato: "+(System.currentTimeMillis()-start));
        }
    }

    @Test
    public void testConjunctiveBeij(){
        String query = "beij";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "conjunctive");

        Assert.assertEquals(results.poll().score, Math.log10(4.0), 0.01);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testConjunctiveRabbitDishCatBeij(){
        String query = "rabbit dish duck beij";
        MaxScore maxScore = new MaxScore(vocabulary, new TokenizerImpl());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "conjunctive");

        Assert.assertNull(results.poll());
    }


    @After
    public void flush() {
        IOUtils.deleteDirectory(Constants.testPath);
    }
}


