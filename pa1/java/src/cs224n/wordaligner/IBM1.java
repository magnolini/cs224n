package cs224n.wordaligner;

import cs224n.util.*;

import java.util.HashMap;
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
    private CounterMap<String, String> sourceTargetCounts;
    private Counter<String> targetCounts;
    private static Random generator = new Random(1);
    private static final int numIterations = 200;

    public Alignment align(SentencePair sentencePair) {
        Alignment alignment = new Alignment();
        sentencePair.getTargetWords().add(NULL_WORD);
        for (int sourceIndex = 0; sourceIndex < sentencePair.getSourceWords().size(); ++sourceIndex) {
            double maxProb = 0;
            int maxTargetIndex = -1;
             for (int targetIndex = 0; targetIndex < sentencePair.getTargetWords().size(); ++targetIndex) {
                 String targetWord = sentencePair.getTargetWords().get(targetIndex);
                 String sourceWord = sentencePair.getSourceWords().get(sourceIndex);
                double probAlign = sourceTargetCounts.getCount(sourceWord, targetWord) /
                    targetCounts.getCount(targetWord);
                if (probAlign > maxProb) {
                    maxTargetIndex = targetIndex;
                    maxProb = probAlign;
                }
            }
            sentencePair.getTargetWords().remove(NULL_WORD);
            if (maxTargetIndex < sentencePair.getTargetWords().size()) {
                alignment.addPredictedAlignment(maxTargetIndex, sourceIndex);
            }
        }
        return alignment;
    }

    public void train(List<SentencePair> trainingPairs) {
        sourceTargetCounts = null;
        targetCounts = null;
        for (int i = 0; i < numIterations; ++i){  // TODO: stop with a convergence test?
            System.out.println("EM Iteration: " + i);
            CounterMap<String, String> newSourceTargetCounts = new CounterMap<String, String>();
            Counter<String> newTargetCounts = new Counter<String>();
            EMIteration(trainingPairs, newSourceTargetCounts, newTargetCounts);
            sourceTargetCounts = newSourceTargetCounts;
            targetCounts = newTargetCounts;
        }
    }

    private void EMIteration(List<SentencePair> trainingPairs, CounterMap<String, String> newSourceTargetCounts,
                             Counter<String> newTargetCounts){
        for(SentencePair pair : trainingPairs){
            pair.getTargetWords().add(NULL_WORD);
            for (String sourceWord : pair.getSourceWords()) {
                for (String targetWord : pair.getTargetWords()) {
                    double probAlign = 1.0 / pair.getSourceWords().size();
                    if (sourceTargetCounts != null)  {
                        probAlign = sourceTargetCounts.getCount(sourceWord, targetWord) /
                            targetCounts.getCount(targetWord);
                    }
                    newSourceTargetCounts.incrementCount(sourceWord, targetWord, probAlign);
                    newTargetCounts.incrementCount(targetWord, probAlign);
                }
             }
            pair.getTargetWords().remove(NULL_WORD);
        }
    }

}
