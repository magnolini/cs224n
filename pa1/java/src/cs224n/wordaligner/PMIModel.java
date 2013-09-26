package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;

/**
 * Simple word alignment baseline model that maps source positions to target
 * positions along the diagonal of the alignment grid.
 *
 * IMPORTANT: Make sure that you read the comments in the
 * cs224n.wordaligner.WordAligner interface.
 *
 * @author Dan Klein
 * @author Spence Green
 */
public class PMIModel implements WordAligner {

    private static final long serialVersionUID = 1315751943476440515L;

    // TODO: Use arrays or Counters for collecting sufficient statistics
    // from the training data.
    private CounterMap<String,String> sourceTargetCounts;
    private Counter<String> sourceCounts;
    private Counter<String> targetCounts;


    public Alignment align(SentencePair sentencePair) {
        // Placeholder code below.
        // TODO Implement an inference algorithm for Eq.1 in the assignment
        // handout to predict alignments based on the counts you collected with train().
        Alignment alignment = new Alignment();
        int numSourceWords = sentencePair.getSourceWords().size();
        int numTargetWords = sentencePair.getTargetWords().size();

        for (int targetIndex = 0; targetIndex < numTargetWords; targetIndex++) {
            String target = sentencePair.getTargetWords().get(targetIndex);
            int maxSourceIndex = -1;
            double maxSourceScore = 0;
            double targetP = targetCounts.getCount(target) / targetCounts.totalCount();

            for (int srcIndex = 0; srcIndex < numSourceWords; srcIndex++) {
                String source = sentencePair.getSourceWords().get(srcIndex);
                double jointP = sourceTargetCounts.getCount(source, target) / sourceTargetCounts.totalCount();
                double sourceP = sourceCounts.getCount(source) / sourceCounts.totalCount();
                double pmi = jointP / (sourceP * targetP);

                if ( pmi > maxSourceScore ){
                    maxSourceScore = pmi;
                    maxSourceIndex = srcIndex;
                }

                // TODO: Discard null alignments?????
            }

            alignment.addPredictedAlignment(targetIndex, maxSourceIndex);

        }
        return alignment;
    }

    public void train(List<SentencePair> trainingPairs) {
        sourceTargetCounts = new CounterMap<String,String>();
        sourceCounts = new Counter<String>();
        targetCounts = new Counter<String>();
        for(SentencePair pair : trainingPairs){
            List<String> targetWords = pair.getTargetWords();
            List<String> sourceWords = pair.getSourceWords();
            for(String source : sourceWords){
                for(String target : targetWords){
                    // TODO: Warm-up. Your code here for collecting sufficient statistics.
                    sourceTargetCounts.incrementCount(source, target, 1.0);
                }
            }
            for(String source : sourceWords) {
                sourceCounts.incrementCount(source, 1.0);
            }
            for(String target : targetWords) {
                targetCounts.incrementCount(target, 1.0);
            }
        }

//        sourceCounts = Counters.normalize(sourceCounts);
//        targetCounts = Counters.normalize(targetCounts);
//        sourceTargetCounts = Counters.normalize(sourceTargetCounts);
    }
}
