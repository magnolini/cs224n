package cs224n.wordaligner;

import cs224n.util.*;

import java.util.ArrayList;
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

    private static final long serialVersionUID = 1315751943476440515L;
    private CounterMap<String, String> sourceTargetCounts;
    private static Random generator = new Random();
    private static HashMap<String, CounterMap<Integer, Integer>> positions;
    private static final int numIterations = 500;

    public Alignment align(SentencePair sentencePair) {
        Alignment alignment = new Alignment();

        for (int targetIndex=0; targetIndex < sentencePair.getTargetWords().size();
             ++targetIndex) {
            String targetWord = sentencePair.getTargetWords().get(targetIndex);
            int sourceIndex = alignWord(sentencePair, targetIndex);
            if (sourceIndex < sentencePair.getSourceWords().size()) {
                alignment.addPredictedAlignment(targetIndex, sourceIndex);
            }
        }
        return alignment;
    }

    private int alignWord(SentencePair pair, int targetIndex) {
        List<String> sourceWords = pair.getSourceWords();
        sourceWords.add(NULL_WORD);
        double pmf[] = constructPMF(pair, targetIndex);
        int maxIndex = getIndexOfMaxValue(pmf);
        sourceWords.remove(NULL_WORD);
        return maxIndex;
    }

    private int getIndexOfMaxValue(double[] pmf){
        double maxValue = -1;
        int maxIndex = -1;
        for (int i=0; i<pmf.length; ++i) {
            double val = pmf[i];
            if (val > maxValue) {
                maxValue = val;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private HashMap<String, CounterMap<Integer, Integer>> clonePositions(HashMap<String, CounterMap<Integer, Integer>> positions)
    {
        HashMap<String, CounterMap<Integer, Integer>> newPositions = new HashMap<String, CounterMap<Integer, Integer>>();
        for (String k : positions.keySet()){
            newPositions.put(k, positions.get(k).deepCopy());
        }
        return newPositions;
    }

    public void train(List<SentencePair> trainingPairs) {
        sourceTargetCounts = new CounterMap<String,String>();
        positions = new HashMap<String, CounterMap<Integer, Integer>>();

        for (int i = 0; i < numIterations; ++i){  // TODO: stop with a convergence test
            CounterMap<String, String> newSourceTargetCounts = sourceTargetCounts.deepCopy();
            HashMap<String, CounterMap<Integer, Integer>> newPositions = clonePositions(positions);
            EMIteration(trainingPairs, newSourceTargetCounts, newPositions);
            sourceTargetCounts = newSourceTargetCounts;
            positions = newPositions;
        }
    }

    private CounterMap<String, String> EMIteration(List<SentencePair> trainingPairs, CounterMap<String, String> sourceTargetCounts, HashMap<String, CounterMap<Integer, Integer>> positions){
        for(SentencePair pair : trainingPairs){
            String key = makePositionKey(pair);
            for (int targetIndex=0; targetIndex<pair.getTargetWords().size(); ++targetIndex) {
                String targetWord = pair.getTargetWords().get(targetIndex);
                int sourceIndex = sampleWord(pair, targetIndex);
                String sourceWord = (sourceIndex < pair.getSourceWords().size()) ? pair.getSourceWords().get(sourceIndex) :
                        NULL_WORD;
                sourceTargetCounts.incrementCount(sourceWord, targetWord, 1);
                CounterMap<Integer, Integer> q = positions.get(key);
                if (q == null)  {
                    q = new CounterMap<Integer, Integer>();
                    positions.put(key, q);
                }
                q.incrementCount(sourceIndex, targetIndex, 1);
            }
        }
        return sourceTargetCounts;
    }

    private String makePositionKey(SentencePair pair) {
        int sourceSentenceLength = pair.getSourceWords().size();
        int targetSentenceLength = pair.getTargetWords().size();
        return sourceSentenceLength + "," + targetSentenceLength;
    }

    private int sampleWord(SentencePair pair, int targetIndex) {
        pair.getSourceWords().add(NULL_WORD);
        double[] pmf = constructPMF(pair, targetIndex);
        pair.getSourceWords().remove(NULL_WORD);
        return samplePMF(pmf);
    }

    private double[] constructPMF(SentencePair pair, int targetIndex){
        // TODO: account for NULL_WORD
        String targetWord = pair.getTargetWords().get(targetIndex);
        String key = makePositionKey(pair);
        CounterMap<Integer, Integer> q = positions.get(pair);
        if (q == null) {
            q = new CounterMap<Integer, Integer>();
        }
        double[] countsQ = new double[pair.getSourceWords().size()];
        double[] countsT = new double[pair.getSourceWords().size()];

        for (int sourceIndex=0; sourceIndex<pair.getSourceWords().size(); ++sourceIndex) {
            String sourceWord = pair.getSourceWords().get(sourceIndex);
            countsQ[sourceIndex] = q.getCount(sourceIndex, targetIndex) + 1;
            countsT[sourceIndex] = sourceTargetCounts.getCount(sourceWord, targetWord) + 1;
        }

        normalize(countsQ);
        normalize(countsT);

        double[] pmf = new double[countsQ.length];
        for (int i=0; i<pmf.length; ++i) {
            pmf[i] = countsQ[i] * countsT[i];
        }
        normalize(pmf);

        return pmf;
    }

    private void normalize(double[] arr){
        double sum = 0;
        for (int i = 0; i < arr.length; ++i){
            sum += arr[i];
        }
        for (int i = 0; i < arr.length; ++i){
            arr[i] = arr[i] / sum;
        }
    }

    private int samplePMF(double[] pmf) {
        double sum = 0;
        double[] cdf = new double[pmf.length];
        for (int i = 0; i < pmf.length; ++i) {
            sum += pmf[i];
            cdf[i] = sum;
        }
        assert (Math.abs(sum - 1.0) < 1e-05);
        double rand = generator.nextDouble();
        for (int i = 0; i < cdf.length; ++i) {
            if (rand <= cdf[i]) {
                return i;
            }
        }
        // Should never reach this
        return -10;
    }

}
