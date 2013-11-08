package edu.stanford.nlp.mt.decoder.feat;

import java.util.List;

import edu.stanford.nlp.mt.base.FeatureValue;
import edu.stanford.nlp.mt.base.Featurizable;
import edu.stanford.nlp.util.Generics;

/**
 * 
 * @author Gil Shotan
 * @author Rafael Ferrer
 * 
 */
public class Numeric<TK> implements
    RuleFeaturizer<TK, String> {

  public static final String FEATURE_NAME = "Numeric";
  protected static final String[] numericWords = {"0","1","2","3","4","5","6","7","8","9","one","two","three","four","five","six","seven","eight","nine","ten",
						  "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty",
						  "thirty","fourtee","fifty","sixty","seventy","eighty","ninety","hundred","thoushand","million","billion","un",
"deux","trois","quatre","cinq","six","sept","huit","neuf","dix","onze","douze","treize","quatorze","quinze","seize","dix","vingt","trente","quarante","cinquante","soixante","soixante","cent","mille","milliard"};

  protected boolean containsNumericWords(String phrase) {
    for (String numWord : numericWords) {
      if (phrase.contains(numWord)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<FeatureValue<String>> ruleFeaturize(
      Featurizable<TK, String> f) {
    if (f.targetPhrase == null || f.sourcePhrase == null) {
      return null;
    } else if (containsNumericWords(f.targetPhrase.toString()) && containsNumericWords(f.sourcePhrase.toString())) {
      List<FeatureValue<String>> features = Generics.newLinkedList();
      features.add(new FeatureValue<String>(FEATURE_NAME, 1.0));
      return features;
    }
    return null;
  }

  @Override
  public void initialize() {
  }
}
