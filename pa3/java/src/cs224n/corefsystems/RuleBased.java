package cs224n.corefsystems;

import java.util.*;

import cs224n.coref.*;
import cs224n.util.Pair;

public class RuleBased implements CoreferenceSystem {

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		// TODO Auto-generated method stub

	}

    private boolean genderAndNumberAgree(Mention m, Mention n){
        Pair<Boolean, Boolean> genderAgreementInfo = Util.haveGenderAndAreSameGender(m, n);
        Pair<Boolean, Boolean> numberAgreementInfo = Util.haveNumberAndAreSameNumber(m, n);
        boolean genderAgree = (genderAgreementInfo.getFirst() && genderAgreementInfo.getSecond()) || !genderAgreementInfo.getFirst();
        boolean numberAgree = (numberAgreementInfo.getFirst() && numberAgreementInfo.getSecond()) || !numberAgreementInfo.getFirst();
        return genderAgree && numberAgree;
    }

    private boolean areAppositive(Mention first, Mention second, Document doc){
        if(first.sentence.equals(second.sentence) && first.parse.getLabel().equals("NP") && second.parse.getLabel().equals("NP")){
            int firstIndex = doc.indexOfMention(first);
            int secondIndex = doc.indexOfMention(second);
            if( (firstIndex == secondIndex - 1) ||
                ((firstIndex == secondIndex - 2) && doc.getMentions().get(firstIndex + 1).headToken().posTag().equals(",")) ){
                if(genderAndNumberAgree(first, second))
                    return true;
            }
        }
        return false;
    }

    private boolean match(Document doc, Mention m, Mention n) {

        // exact matching
        if (m.gloss().equals(n.gloss()))
            return true;

        // hobbs matching
        if( (Hobbs.getHobbsCandidate(doc, m) == n || Hobbs.getHobbsCandidate(doc, n) == m) && genderAndNumberAgree(m, n))
            return true;

//        if( areAppositive(m, n, doc))


        return false;
    }

    private void mergeSets(Map<Mention, Set<Mention>> map, Mention m, Mention n) {
        Set<Mention> set1 = map.get(n);
        Set<Mention> set2 = map.get(m);
        set1.addAll(set2);

        for (Mention temp : set1) {
            map.put(temp, set1);
        }
    }

    private Set<Set<Mention>> extractSet(Map<Mention, Set<Mention>> map) {
        Set<Set<Mention>> allSets = new HashSet<Set<Mention>>();
        for (Set<Mention> s : map.values()) {
            allSets.add(s);
        }
        return allSets;
    }

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
        List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();

        Map<Mention, Set<Mention> > map  = new HashMap<Mention, Set<Mention>>();

        // create singleton clusters
        for(Mention m : doc.getMentions()){
            Set<Mention> set = new HashSet<Mention>();
            set.add(m);
            map.put(m, set);
        }

        for (Mention m : doc.getMentions()){
            for (Mention n : doc.getMentions()) {
                if ( (n == m) || (doc.indexOfMention(n) > doc.indexOfMention(m)) )
                    continue;
                if (match(doc, n, m)) {
                    mergeSets(map,m,n);
                }
            }
//            System.out.println("Mention: " + m.gloss());
//            System.out.println("Sentence: " + m.sentence);
//            System.out.println("Index: " + doc.indexOfMention(m));
//            System.out.println("\n\n");
        }

        // TODO: refactor
        for(Set<Mention> a : extractSet(map)) {
            ClusteredMention c = null;
            for(Mention m : a) {
                if(c != null)
                    mentions.add(m.markCoreferent(c));
                else {
                    c = m.markSingleton();
                    mentions.add(c);
                }
            }
        }

        return mentions;
    }

}
