package cs224n.corefsystems;

import java.util.*;


import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.*;
import cs224n.util.Pair;

public class BetterBaseline implements CoreferenceSystem {

    // TODO: refactor
    Map<String, List<String> > commonCo = new HashMap<String, List<String>>();

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
        for(Pair<Document, List<Entity>> pair : trainingData){
            //--Get Variables
            List<Entity> clusters = pair.getSecond();
            for(Entity e : clusters){
                for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
                    String first = mentionPair.getFirst().headWord();
                    String second = mentionPair.getSecond().headWord();
                    if(!commonCo.containsKey(first)){
                        commonCo.put(first, new ArrayList<String>());
                    }
                    if(!commonCo.containsKey(second)){
                        commonCo.put(second, new ArrayList<String>());
                    }
                    commonCo.get(first).add(second);
                    commonCo.get(second).add(first);
                }
            }
        }
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
        List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
        Map<String,Entity> clusters = new HashMap<String,Entity>();
        for(Mention m : doc.getMentions()){
            String mentionString = m.gloss();
            Pronoun pronoun = Pronoun.valueOrNull(mentionString);
            boolean foundCoref = false;

            // exact match
            if(clusters.containsKey(mentionString)){
                mentions.add(m.markCoreferent(clusters.get(mentionString)));
                continue;
            }
            // head word match
            if(commonCo.containsKey(mentionString)) {
                for(String coref: commonCo.get(mentionString)) {
                    if(clusters.containsKey(coref)) {
                        mentions.add(m.markCoreferent(clusters.get(coref)));
                        foundCoref = true;
                        break;
                    }
                }
                if(foundCoref)
                    continue;
            }
            // pronoun gender match
            if (pronoun != null){
                for(String c : clusters.keySet()){
                    if(pronoun.gender == Name.gender(c)){
                        mentions.add(m.markCoreferent(clusters.get(c)));
                        foundCoref = true;
                        break;
                    }
                }
                if(foundCoref)
                    continue;
            }

            ClusteredMention newCluster = m.markSingleton();
            mentions.add(newCluster);
            clusters.put(mentionString,newCluster.entity);
        }

        return mentions;
	}
}
