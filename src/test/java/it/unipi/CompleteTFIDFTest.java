package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Tokenizer;
import it.unipi.index.SPIMIIndex;
import it.unipi.io.DocumentStream;
import it.unipi.model.Document;
import it.unipi.model.DocumentIndex;
import it.unipi.model.Vocabulary;
import it.unipi.scoring.DocumentScore;
import it.unipi.scoring.MaxScore;
import it.unipi.scoring.ScoringType;
import it.unipi.utils.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.PriorityQueue;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompleteTFIDFTest {

    @Mock
    static DocumentStream ds;

    Vocabulary vocabulary;
    DocumentIndex documentIndex;

    @Before
    public void setup() throws IOException {
        Constants.setPath(Constants.testPath);
        Constants.setScoring(ScoringType.TFIDF);

        IOUtils.deleteDirectory(Constants.testPath);
        IOUtils.createDirectory(Constants.testPath);

        when(ds.nextDoc()).thenReturn(
                new Document("0\tduck duck duck"),
                new Document("1\tbeij dish duck duck"),
                new Document("2\tduck duck rabbit recip"),
                new Document("3\trabbit recip recip duck"),
                null
        );
        CompressionType compression = CompressionType.BINARY;
        Constants.setCompression(compression);

        // Dumping
        new SPIMIIndex(compression, ds).buildIndex(Constants.testPath);
        vocabulary = Vocabulary.getInstance(compression);
        documentIndex = DocumentIndex.getInstance();
    }

    @Test
    public void testQueryDuck(){
        String query = "duck";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");
        Assert.assertEquals(results.size(), 4);

        for(int i=0; i<4; i++){
            DocumentScore documentScore = results.poll();
            Assert.assertNotNull(documentScore);
            Assert.assertEquals(0.0, documentScore.score, 1e-6);
        }
        Assert.assertNull(results.poll());
    }

    @Test
    public void testQueryRabbit(){
        String query = "rabbit";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");
        Assert.assertEquals(results.size(), 2);

        DocumentScore documentScore2 = results.poll();
        DocumentScore documentScore1 = results.poll();
        Assert.assertNotNull(documentScore1);
        Assert.assertNotNull(documentScore2);

        Assert.assertEquals(documentScore1.score, Math.log10(2.0), 1e-6);
        Assert.assertEquals(documentScore2.score, Math.log10(2.0), 1e-6);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testQueryRecip(){
        String query = "recip";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");
        Assert.assertEquals(results.size(), 2);

        DocumentScore documentScore1 = results.poll();
        DocumentScore documentScore2 = results.poll();
        Assert.assertNotNull(documentScore1);
        Assert.assertNotNull(documentScore2);

        Assert.assertEquals(documentScore2.score, (1+Math.log10(2.0))*Math.log10(2.0), 1e-6);
        Assert.assertEquals(documentScore1.score, Math.log10(2.0), 1e-6);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testQueryEmpty(){
        // fetching
        String query = "cat";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");
        Assert.assertNull(results);
    }

    @Test
    public void testQueryRabbitRecip(){
        String query = "rabbit recip";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");

        DocumentScore documentScore2 = results.poll();
        DocumentScore documentScore1 = results.poll();
        Assert.assertNotNull(documentScore1);
        Assert.assertNotNull(documentScore2);

        Assert.assertEquals(documentScore1.score, (1 + Math.log10(2.0)) * Math.log10(2.0) + Math.log10(2.0), 1e-6);
        Assert.assertEquals(documentScore2.score, 2 * Math.log10(2.0), 1e-6);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testResetPostingList(){
        String query = "rabbit recip";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        for(int i=0; i<2; i++) {
            double start = System.currentTimeMillis();

            PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");

            DocumentScore documentScore2 = results.poll();
            DocumentScore documentScore1 = results.poll();
            Assert.assertNotNull(documentScore1);
            Assert.assertNotNull(documentScore2);

            Assert.assertEquals(documentScore1.score,(1+Math.log10(2.0))*Math.log10(2.0)+Math.log10(2.0) , 1e-6);
            Assert.assertEquals(documentScore2.score, 2 * Math.log10(2.0), 1e-6);

            Assert.assertNull(results.poll());

            System.out.println("Iterazione: "+i+" Tempo impiegato: "+(System.currentTimeMillis()-start));
        }
    }
    @Test
    public void testConjunctiveRabbitRecip(){
        String query = "rabbit recip";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "conjunctive");

        DocumentScore documentScore2 = results.poll();
        DocumentScore documentScore1 = results.poll();
        Assert.assertNotNull(documentScore1);
        Assert.assertNotNull(documentScore2);

        Assert.assertEquals(documentScore1.score, ((1+Math.log10(2.0))*Math.log10(2.0) + Math.log10(2.0)), 1e-6);
        Assert.assertEquals(documentScore2.score, 2*Math.log10(2.0), 1e-6);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testConjunctiveEmpty(){
        String query = "rabbit cat";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "conjunctive");

        Assert.assertNull(results.poll());
    }

    @Test
    public void testResetConjunctive(){
        String query = "duck rabbit";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        for(int i=0; i<2; i++) {
            double start = System.currentTimeMillis();

            PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "conjunctive");

            DocumentScore documentScore2 = results.poll();
            DocumentScore documentScore1 = results.poll();
            Assert.assertNotNull(documentScore1);
            Assert.assertNotNull(documentScore2);

            Assert.assertEquals(documentScore1.score, Math.log10(2.0), 1e-6);
            Assert.assertEquals(documentScore2.score, Math.log10(2.0), 1e-6);

            Assert.assertNull(results.poll());

            System.out.println("Iterazione: "+i+" Tempo impiegato: "+(System.currentTimeMillis()-start));
        }
    }

    @Test
    public void testConjunctiveBeij(){
        String query = "beij";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "conjunctive");

        DocumentScore documentScore = results.poll();
        Assert.assertNotNull(documentScore);

        Assert.assertEquals(documentScore.score, Math.log10(4.0), 1e-6);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testConjunctiveRabbitDishCatBeij(){
        String query = "rabbit dish duck beij";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "conjunctive");

        Assert.assertNull(results.poll());
    }


    @After
    public void flush() {
        IOUtils.deleteDirectory(Constants.testPath);
    }
}


