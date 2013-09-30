package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;
import java.io.*;

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
    private CounterMap<String,String> targetSourceCounts;
    private Counter<String> sourceCounts;


    public Alignment align(SentencePair sentencePair) {
        Alignment alignment = new Alignment();
        sentencePair.getSourceWords().add(NULL_WORD);
        int numSourceWords = sentencePair.getSourceWords().size();
        int numTargetWords = sentencePair.getTargetWords().size();

        for (int targetIndex = 0; targetIndex < numTargetWords; targetIndex++) {
            String target = sentencePair.getTargetWords().get(targetIndex);
            int maxSourceIndex = -1;
            double maxSourceScore = 0;
            for (int srcIndex = 0; srcIndex < numSourceWords; srcIndex++) {
                String source = sentencePair.getSourceWords().get(srcIndex);
                double mutualInformation = targetSourceCounts.getCount(target, source) / sourceCounts.getCount(source);
//                System.out.println("target="+target+", source="+source+". pmi="+mutualInformation);
                if ( mutualInformation > maxSourceScore ){
                    maxSourceScore = mutualInformation;
                    maxSourceIndex = srcIndex;
                }

                // TODO: Discard null alignments?????
            }

            alignment.addPredictedAlignment(targetIndex, maxSourceIndex);

        }
        sentencePair.getSourceWords().remove(NULL_WORD);
        return alignment;
    }

    public void train(List<SentencePair> trainingPairs) {
        targetSourceCounts = new CounterMap<String,String>();
        sourceCounts = new Counter<String>();
        for(SentencePair pair : trainingPairs){
            List<String> targetWords = pair.getTargetWords();
            List<String> sourceWords = pair.getSourceWords();
            sourceWords.add(NULL_WORD);
            for(String source : sourceWords){
                for(String target : targetWords){
                    targetSourceCounts.incrementCount(target, source, 1.0);
                }
            }
            for(String source : sourceWords) {
                sourceCounts.incrementCount(source, 1.0);
            }
            sourceWords.remove(NULL_WORD);
        }

        sourceCounts = Counters.normalize(sourceCounts);
        targetSourceCounts = Counters.conditionalNormalize(targetSourceCounts);

//        try {
//            BufferedWriter bw = new BufferedWriter(new FileWriter("debug"));
//            bw.write("----------------------------------\n");
//            bw.write(sourceCounts.toString());
//            bw.write("\nSource Target Counts:\n");
//            bw.write("----------------------------------\n");
//            bw.write(targetSourceCounts.toString());
//            bw.close();
//
//        } catch (IOException e) {
//            // Handle exception
//        }
    }
}
