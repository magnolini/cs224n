package cs224n.corefsystems;

import java.util.*;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.util.Pair;

public class AllSingleton implements CoreferenceSystem {
    /**
     * Since this is a deterministic system, the train() method does nothing
     * @param trainingData The data to train off of
     */
    public void train(Collection<Pair<Document, List<Entity>>> trainingData) {}

    /**
     * Find mentions that are exact matches of each other, and mark them as coreferent.
     * @param doc The document to run coreference on
     * @return The list of clustered mentions to return
     */
    public List<ClusteredMention> runCoreference(Document doc) {
        //(variables)
        List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
        //(for each mention...)
        for(Mention m : doc.getMentions()){
            //(...else create a new singleton cluster)
            ClusteredMention newCluster = m.markSingleton();
            mentions.add(newCluster);
        }
        //(return the mentions)
        return mentions;
    }
}
