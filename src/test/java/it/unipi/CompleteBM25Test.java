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
import it.unipi.utils.Constants;
import it.unipi.utils.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.PriorityQueue;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompleteBM25Test {

    @Mock
    static DocumentStream ds;

    Vocabulary vocabulary;
    DocumentIndex documentIndex;

    @Before
    public void setup() throws IOException  {

        Constants.setScoring(ScoringType.BM25);
        Constants.setPath(Constants.TEST_PATH);

        IOUtils.deleteDirectory(Constants.TEST_PATH);
        IOUtils.createDirectory(Constants.TEST_PATH);

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
        new SPIMIIndex(compression, ds).buildIndex(Constants.TEST_PATH);
        vocabulary = new Vocabulary(compression);
        documentIndex = new DocumentIndex();
    }

    @Test
    public void testQueryDuck() {
        String query = "duck";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");
        Assert.assertEquals(results.size(), 4);

        for (int i=0; i<4; i++) {
            DocumentScore documentScore = results.poll();
            Assert.assertNotNull(documentScore);
            Assert.assertEquals(0.0, documentScore.score(), 0.01);
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

        Assert.assertEquals(documentScore1.score(), 1/(Constants.BM25_k*((1-Constants.BM25_b)+Constants.BM25_b*(4/((float)15/4)))+1)*Math.log10(2.0), 0.01);
        Assert.assertEquals(documentScore2.score(), 1/(Constants.BM25_k*((1-Constants.BM25_b)+Constants.BM25_b*(4/((float)15/4)))+1)*Math.log10(2.0), 0.01);

        Assert.assertNull(results.poll());
    }

    @Test
    public void testQueryEmpty(){
        // fetching
        String query = "cat";
        MaxScore maxScore = new MaxScore(vocabulary, documentIndex, Tokenizer.getInstance());

        PriorityQueue<DocumentScore> results = maxScore.score(query, 10, "disjunctive");
        Assert.assertNull(results.poll());
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

        double scoreRabbit = 1/(Constants.BM25_k*((1-Constants.BM25_b)+Constants.BM25_b*(4/((float)15/4)))+1)*Math.log10(2.0);
        double scoreRecip2 = 1/(Constants.BM25_k*((1-Constants.BM25_b)+Constants.BM25_b*(4/((float)15/4)))+1)*Math.log10(2.0);
        double scoreRecip1 = 2/(Constants.BM25_k*((1-Constants.BM25_b)+Constants.BM25_b*(4/((float)15/4)))+2)*Math.log10(2.0);

        Assert.assertEquals(documentScore1.score(), scoreRabbit+scoreRecip1, 0.01);
        Assert.assertEquals(documentScore2.score(), scoreRabbit+scoreRecip2, 0.01);

        Assert.assertNull(results.poll());
    }

    @After
    public void flush() {
        // IOUtils.deleteDirectory(Constants.testPath);
    }
}
