package cs224n.wordaligner;

import cs224n.util.*;

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
    private CounterMap<String, String> t;  // t(e: A, B, C, f: X, Y, Z) = t(f|e)
    private static Random generator = new Random(1);
    private static final int numIterations = 20;

    public Alignment align(SentencePair sentencePair) {
        Alignment alignment = new Alignment();
        sentencePair.getSourceWords().add(NULL_WORD);
        for (int f_index = 0; f_index < sentencePair.getTargetWords().size(); f_index++) {
            String f = sentencePair.getTargetWords().get(f_index);
            int maxEIndex = -1;
            double maxEProb = -1;
            for (int e_index = 0; e_index < sentencePair.getSourceWords().size(); e_index++) {
                String e = sentencePair.getSourceWords().get(e_index);
                if (t.getCount(e, f) > maxEProb) {
                    maxEProb = t.getCount(e, f);
                    maxEIndex = e_index;
                }
            }
            if (sentencePair.getSourceWords().get(maxEIndex) != NULL_WORD) {
                alignment.addPredictedAlignment(f_index, maxEIndex);
            }
        }
        sentencePair.getSourceWords().remove(NULL_WORD);
        return alignment;
    }

    public void train(List<SentencePair> trainingPairs) {
        t = null;
        for (int i = 0; i < numIterations; ++i){  // TODO: stop with a convergence test?
            System.out.println("EM Iteration: " + i);
            CounterMap<String, String> c = new CounterMap<String, String>();
            EMIteration(trainingPairs, c);
            t = Counters.conditionalNormalize(c);
        }
    }

    // receives an empty c
    private void EMIteration(List<SentencePair> trainingPairs, CounterMap<String, String> c){
        for(SentencePair pair : trainingPairs){
            pair.getSourceWords().add(NULL_WORD);
            for (String f : pair.getTargetWords()) {
                double tsum = 0.0;
                if (t != null) {
                    tsum = normalizeForSentence(pair.getSourceWords(), f);
                }
                for (String e : pair.getSourceWords()) {
                    double delta = 1.0 / pair.getSourceWords().size(); // TODO
                    if (t != null){
                        delta = t.getCount(e, f) / tsum;
                    }
                    c.incrementCount(e, f, delta);
                }
            }
            pair.getSourceWords().remove(NULL_WORD);
        }
    }


    private double normalizeForSentence(List<String> e_sentence, String f_word) {
        double sum = 0.0;
        for (String e : e_sentence) {
            sum += t.getCount(e, f_word);
        }
        return sum;
    }

}
