package it.unipi;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Tokenizer;
import it.unipi.index.InMemoryIndexing;
import it.unipi.index.SPIMIIndex;
import it.unipi.io.DocumentStream;
import it.unipi.io.Dumper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.PriorityQueue;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompleteTestBM25 {
    @Mock
    static DocumentStream ds;

    @InjectMocks
    InMemoryIndexing indexerSingleBlock;

    Vocabulary vocabulary;
    DocumentIndex documentIndex;

    @Before
    public void setup() {
        Constants.setScoring(ScoringType.BM25);
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
        indexerSingleBlock = new InMemoryIndexing(Vocabulary.getInstance(), Dumper.getInstance(compression), DocumentIndex.getInstance());
        new SPIMIIndex(compression, ds, indexerSingleBlock).buildIndexSPIMI(Constants.testPath);
        vocabulary = Vocabulary.getInstance();
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
            Assert.assertEquals(0.0, documentScore.score, 0.01);
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

        Assert.assertEquals(documentScore1.score, 1/(Constants.BM25_k*((1-Constants.BM25_b)+Constants.BM25_b*(4/((float)15/4)))+1)*Math.log10(2.0), 0.01);
        Assert.assertEquals(documentScore2.score, 1/(Constants.BM25_k*((1-Constants.BM25_b)+Constants.BM25_b*(4/((float)15/4)))+1)*Math.log10(2.0), 0.01);

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

        double scoreRabbit = 1/(Constants.BM25_k*((1-Constants.BM25_b)+Constants.BM25_b*(4/((float)15/4)))+1)*Math.log10(2.0);
        double scoreRecip2 = 1/(Constants.BM25_k*((1-Constants.BM25_b)+Constants.BM25_b*(4/((float)15/4)))+1)*Math.log10(2.0);
        double scoreRecip1 = 2/(Constants.BM25_k*((1-Constants.BM25_b)+Constants.BM25_b*(4/((float)15/4)))+2)*Math.log10(2.0);

        Assert.assertEquals(documentScore1.score, scoreRabbit+scoreRecip1, 0.01);
        Assert.assertEquals(documentScore2.score, scoreRabbit+scoreRecip2, 0.01);

        Assert.assertNull(results.poll());
    }

    @After
    public void flush() {
        IOUtils.deleteDirectory(Constants.testPath);
    }
}
