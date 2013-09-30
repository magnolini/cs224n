package edu.stanford.nlp.mt.decoder.feat.sparse;

import java.util.*;

import edu.stanford.nlp.mt.base.ARPALanguageModel;
import edu.stanford.nlp.mt.base.ConcreteRule;
import edu.stanford.nlp.mt.base.FeatureValue;
import edu.stanford.nlp.mt.base.Featurizable;
import edu.stanford.nlp.mt.base.InsertedStartEndToken;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.SimpleSequence;
import edu.stanford.nlp.mt.decoder.feat.DerivationFeaturizer;

/**
 * 
 * @author danielcer
 * 
 */
public class CrossPhraseBoundryFeaturizer implements
    DerivationFeaturizer<IString, String> {
  public static final String FEATURE_PREFIX = "CPB";
  public static final String PREFIX_SRC = ":src";
  public static final String PREFIX_TRG = ":trg";

  public static final int DEFAULT_SIZE = 1;
  public static final boolean DEFAULT_DO_SOURCE = true;
  public static final boolean DEFAULT_DO_TARGET = true;
  public final boolean doSource;
  public final boolean doTarget;
  public final int size;
  public static final Sequence<IString> INITIAL_PHRASE = new SimpleSequence<IString>(
      ARPALanguageModel.START_TOKEN);
  public static final Sequence<IString> FINAL_PHRASE = new SimpleSequence<IString>(
      ARPALanguageModel.END_TOKEN);

  public CrossPhraseBoundryFeaturizer(String... args) {
    size = (args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_SIZE);
    doSource = (args.length >= 2 ? Boolean.parseBoolean(args[1])
        : DEFAULT_DO_SOURCE);
    doTarget = (args.length >= 3 ? Boolean.parseBoolean(args[2])
        : DEFAULT_DO_TARGET);
  }

  public CrossPhraseBoundryFeaturizer() {
    size = DEFAULT_SIZE;
    doSource = DEFAULT_DO_SOURCE;
    doTarget = DEFAULT_DO_TARGET;
  }

  @Override
  public void initialize(int sourceInputId,
      List<ConcreteRule<IString,String>> options, Sequence<IString> foreign) {
  }

  private Sequence<IString> lSequence(Sequence<IString> phrase) {
    return phrase.subsequence(0, Math.min(size, phrase.size()));
  }

  private Sequence<IString> rSequence(Sequence<IString> phrase) {
    int phraseSz = phrase.size();
    return phrase.subsequence(Math.max(0, phraseSz - size), phraseSz);
  }

  @Override
  public List<FeatureValue<String>> featurize(
      Featurizable<IString, String> f) {
    List<FeatureValue<String>> fList = new LinkedList<FeatureValue<String>>();

    if (doTarget) {
      Sequence<IString> priorSource = (f.prior != null ? f.prior.targetPhrase
          : INITIAL_PHRASE);
      Sequence<IString> currentSource = f.targetPhrase;

      fList.add(new FeatureValue<String>(FEATURE_PREFIX + PREFIX_TRG + size
          + ":" + rSequence(priorSource).toString("_") + "|"
          + lSequence(currentSource).toString("_"), 1.0));

      if (f.done) {
        fList.add(new FeatureValue<String>(
            FEATURE_PREFIX + PREFIX_TRG + size + ":"
                + rSequence(currentSource).toString("_") + "|" + FINAL_PHRASE,
            1.0));
      }
    }

    if (doSource) {
      Sequence<IString> wrappedSource = new InsertedStartEndToken<IString>(
          f.sourceSentence, ARPALanguageModel.START_TOKEN,
          ARPALanguageModel.END_TOKEN);
      int wrappedForeignSz = wrappedSource.size();
      fList.add(new FeatureValue<String>(FEATURE_PREFIX
          + PREFIX_SRC
          + size
          + ":"
          + wrappedSource.subsequence(
              Math.max(0, f.sourcePosition + 1 - size), f.sourcePosition + 1)
              .toString("_")
          + "|"
          + wrappedSource.subsequence(f.sourcePosition + 1,
              Math.min(f.sourcePosition + 1 + size, wrappedForeignSz))
              .toString("_"), 1.0));

      if (f.done) {
        fList.add(new FeatureValue<String>(FEATURE_PREFIX
            + PREFIX_SRC
            + size
            + ":"
            + wrappedSource.subsequence(
                Math.max(0, wrappedForeignSz - 1 - size), wrappedForeignSz - 1)
                .toString("_") + "|" + ARPALanguageModel.END_TOKEN, 1.0));
      }
    }

    return fList;
  }

  public void reset() {
  }
}
