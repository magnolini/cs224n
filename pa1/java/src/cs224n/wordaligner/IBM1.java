package cs224n.wordaligner;

import cs224n.util.*;

import java.util.ArrayList;
import java.util.Random;
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
public class IBM1 implements WordAligner {

    private static final long serialVersionUID = 1315751943476440515L;
    CounterMap<String, String> sourceTargetCounts;
    private static Random generator = new Random();
    private static final int numIterations = 500;

    public Alignment align(SentencePair sentencePair) {
        Alignment alignment = new Alignment();

        for (int targetIndex=0; targetIndex < sentencePair.getTargetWords().size();
             ++targetIndex) {
            String targetWord = sentencePair.getTargetWords().get(targetIndex);
            int sourceIndex = sample(targetWord, sentencePair.getSourceWords());
            if (sourceIndex >= 0) {
                alignment.addPredictedAlignment(targetIndex, sourceIndex);
            }
        }
        return alignment;
    }

    public void train(List<SentencePair> trainingPairs) {
        sourceTargetCounts = new CounterMap<String,String>();
        for (int i = 0; i < numIterations; ++i){  // TODO: stop with a convergence test?
            System.out.println("EM Iteration " + i);
            sourceTargetCounts = EMIteration(trainingPairs);
        }
    }

    private CounterMap<String, String> EMIteration(List<SentencePair> trainingPairs){
        CounterMap<String, String> newSourceTargetCounts = sourceTargetCounts.deepCopy();
        for(SentencePair pair : trainingPairs){
            for (int i=0; i<pair.getTargetWords().size(); ++i) {
                String targetWord = pair.getTargetWords().get(i);
                int sourceIndex = sample(targetWord, pair.getSourceWords());
                String sourceWord = (sourceIndex >= 0) ? pair.getSourceWords().get(sourceIndex)
                        : NULL_WORD;
                newSourceTargetCounts.incrementCount(sourceWord, targetWord, 1);
            }
        }
        return newSourceTargetCounts;
    }

    private int sample(String targetWord, List<String> sourceWords) {
        sourceWords.add (0,NULL_WORD);
        double cumulativeSum[] = new double[sourceWords.size()];
        double sum = 0;
        for (int i = 0; i < sourceWords.size(); ++i) {
            sum += sourceTargetCounts.getCount(sourceWords.get(i), targetWord) + 1;
            cumulativeSum[i] = sum;
        }
        double rand = generator.nextDouble()*sum;
        for (int i = 0; i < cumulativeSum.length; ++i) {
            if (rand < cumulativeSum[i]) {
                sourceWords.remove(0);
                return i-1;
            }
        }
        // Should never reach this
        return -1;
    }

}
