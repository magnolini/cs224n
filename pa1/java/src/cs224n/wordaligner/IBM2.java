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

public class IBM2 implements WordAligner {
    protected class QType extends HashMap<String, CounterMap<Integer, Integer> > {}

    private static final long serialVersionUID = 1315751943476440515L;
    private CounterMap<String, String> t;  // t(e: A, B, C, f: X, Y, Z) = t(f|e)
    private static HashMap<String, CounterMap<Integer, Integer>> q;

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
        IBM1 initializer = new IBM1();
        initializer.train(trainingPairs);
        t = initializer.getT();
        q = null;

        for (int i = 0; i < numIterations; ++i){  // TODO: stop with a convergence test?
            System.out.println("IBM2: EM Iteration: " + i);
            CounterMap<String, String> c = new CounterMap<String, String>();
            QType cq = new QType();
            EMIteration(trainingPairs, c, cq);
            t = Counters.conditionalNormalize(c);
            q = normalizeCQ(cq);
        }
    }

    // receives an empty c
    private void EMIteration(List<SentencePair> trainingPairs, CounterMap<String, String> c,
                             QType cq){
        for(SentencePair pair : trainingPairs){
            pair.getSourceWords().add(NULL_WORD);
            for (int f_index = 0; f_index < pair.getTargetWords().size(); f_index++) {
                String f = pair.getTargetWords().get(f_index);
                double tqsum = 0.0;
                if (q != null) {
                    tqsum = normalizeForSentence(pair, f_index);
                }
                String qKey = makeQKey(pair);
                for (int e_index = 0; e_index < pair.getSourceWords().size(); ++e_index) {
                    String e = pair.getSourceWords().get(e_index);
                    double delta = 0.0;
                    if (q != null){
                        delta = t.getCount(e, f) * q.get(qKey).getCount(e_index, f_index)
                                / tqsum;
                    }
                    else{
                        double tsum = normalizeTForSentence(pair.getSourceWords(), f);
                        delta = t.getCount(e, f) / tsum;
                    }
                    c.incrementCount(e, f, delta);
                    if (cq.get(qKey) == null) {
                        cq.put(qKey, new CounterMap<Integer, Integer>());
                    }
                    cq.get(qKey).incrementCount(e_index, f_index, delta);
                }
            }
            pair.getSourceWords().remove(NULL_WORD);
        }
    }


    private double normalizeForSentence(SentencePair pair, int f_index) {
        double sum = 0.0;
        String qKey = makeQKey(pair);
        String f = pair.getTargetWords().get(f_index);
        for (int e_index = 0; e_index < pair.getSourceWords().size(); ++e_index) {
            String e = pair.getSourceWords().get(e_index);
            sum += t.getCount(e, f) * q.get(qKey).getCount(e_index, f_index);

        }
        return sum;
    }

    private double normalizeTForSentence(List<String> e_sentence, String f_word) {
        double sum = 0.0;
        for (String e : e_sentence) {
            sum += t.getCount(e, f_word);
        }
        return sum;
    }

    private String makeQKey(SentencePair pair) {
        int sourceSentenceLength = pair.getSourceWords().size();
        int targetSentenceLength = pair.getTargetWords().size();
        return sourceSentenceLength + "," + targetSentenceLength;
    }

    private QType normalizeCQ(QType cq) {
        QType newQ = new QType();
        for (String qKey : cq.keySet()) {
            CounterMap<Integer, Integer> cqNorm =
                    Counters.conditionalNormalize(cq.get(qKey));
            newQ.put(qKey, cqNorm);
        }
        return newQ;
    }

}
