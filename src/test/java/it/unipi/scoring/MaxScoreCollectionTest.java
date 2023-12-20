package it.unipi.scoring;

import it.unipi.encoding.CompressionType;
import it.unipi.encoding.Tokenizer;
import it.unipi.model.DocumentIndex;
import it.unipi.model.Vocabulary;
import it.unipi.utils.Constants;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

/**
 * This test assumes that the whole collection
 * has been previously indexed
 */
@RunWith(Parameterized.class)
public class MaxScoreCollectionTest {

    // Test over different queries
    @Parameterized.Parameters
    public static Collection<Object[]> testQueries() {
        return Arrays.asList(new Object[][] {
                {"does legionella pneumophila cause pneumonia"},
                {"pizza and hamburger and coca in costa Rica"}
        });
    }

    private static MaxScore maxScore;
    private final String testQuery;

    public MaxScoreCollectionTest(String testQuery) {
        this.testQuery = testQuery;
    }

    @BeforeClass
    public static void setUp() {
        Constants.setCompression(CompressionType.COMPRESSED);
        Constants.setPath(Path.of("./data"));

        maxScore = new MaxScore(Vocabulary.getInstance(), DocumentIndex.getInstance(), Tokenizer.getInstance());
    }

    /**
     * Test that with different number of results queried
     * the top documents are always the same
     */
    @Test
    public void testBestDocument() {
        int[] numResults = new int[] {10, 100, 1000, 10000, 100000};
        List<List<DocumentScore>> queriesScores = new ArrayList<>();

        int topK = numResults[0];

        for (int K : numResults) {
            PriorityQueue<DocumentScore> scoring = maxScore.score(
                    testQuery, K, "disjunctive");
            List<DocumentScore> topScores = new ArrayList<>();

            while (!scoring.isEmpty())
                topScores.add(0, scoring.poll());
            topScores = topScores.subList(0, topK);

            queriesScores.add(topScores);
        }

        for (int i = 0; i < numResults.length - 1; i++)
            assertEquals("k = " + numResults[i] + " and " + numResults[i+1],
                    queriesScores.get(i), queriesScores.get(i+1));
    }
}