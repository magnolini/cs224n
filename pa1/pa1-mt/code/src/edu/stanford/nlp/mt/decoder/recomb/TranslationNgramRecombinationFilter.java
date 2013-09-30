package edu.stanford.nlp.mt.decoder.recomb;

import java.util.List;

import edu.stanford.nlp.mt.base.*;
import edu.stanford.nlp.mt.decoder.util.Derivation;

/**
 * 
 * @author danielcer
 * 
 * @param <TK>
 * @param <FV>
 */
public class TranslationNgramRecombinationFilter<TK extends IString, FV>
    implements RecombinationFilter<Derivation<TK, FV>> {
  final int tokenHistoryExamined;
  final List<LanguageModel<TK>> lgModels;
  static final boolean DETAILED_DEBUG = false;

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public TranslationNgramRecombinationFilter(List<LanguageModel<TK>> lgModels,
      int maxTokenHistoryExamined) {
    if (maxTokenHistoryExamined <= 0) {
      throw new RuntimeException(
          String
              .format(
                  "Invalid token history size for TranslationNgramRecombinationFilter. Token history must be > 0, not %d.",
                  maxTokenHistoryExamined));
    }
    int highestOrder = -1;
    for (LanguageModel<TK> lm : lgModels) {
      int order = lm.order();
      if (order > highestOrder) {
        highestOrder = order;
      }
    }
    if (highestOrder < maxTokenHistoryExamined) {
      this.tokenHistoryExamined = highestOrder - 1;
    } else {
      this.tokenHistoryExamined = maxTokenHistoryExamined;
    }
    this.lgModels = lgModels;
  }

  private Sequence<TK> getMaxNgram(Derivation<TK, FV> hyp,
      LanguageModel<TK> lgModel) {
    if (hyp.featurizable == null) {
      return null;
    }
    Sequence<TK> trans = (!hyp.isDone() ? new InsertedStartToken<TK>(
        hyp.featurizable.targetPrefix, lgModel.getStartToken())
        : new InsertedStartEndToken<TK>(hyp.featurizable.targetPrefix,
            lgModel.getStartToken(), lgModel.getEndToken()));
    int transSize = trans.size();
    if (transSize <= tokenHistoryExamined) {
      return trans;
    }
    return trans.subsequence(transSize - tokenHistoryExamined, transSize);
  }

  public Sequence<TK> getNgram(Derivation<TK, FV> hyp) {
    Sequence<TK> longestRelNgram = null;
    for (LanguageModel<TK> lm : lgModels) {
      Sequence<TK> maxNgram = getMaxNgram(hyp, lm);
      if (maxNgram == null)
        return null;
      Sequence<TK> ngram = maxNgram;
      int sz = maxNgram.size();
      relPrefixSearch: for (int i = 0; i < sz; i++) {
        ngram = maxNgram.subsequence(i, sz);
        if (lm.releventPrefix(ngram)) {
          break relPrefixSearch;
        }
      }
      if (longestRelNgram == null || longestRelNgram.size() < ngram.size()) {
        longestRelNgram = ngram;
      }
    }

    return longestRelNgram;
  }

  @SuppressWarnings("unused")
  private boolean compareNgrams(RawSequence<TK> transA, RawSequence<TK> transB) {
    int aPos = transA.elements.length - tokenHistoryExamined;
    int bPos = transB.elements.length - tokenHistoryExamined;
    int aLimit = transA.elements.length;

    if (aPos < 0 || bPos < 0) {
      if (aPos != bPos)
        return false;
      aPos = bPos = 0;
    }

    while (aPos < aLimit) {
      if (transA.get(aPos++).id != transB.get(bPos++).id)
        return false;
    }

    return true;
  }

  @Override
  public boolean combinable(Derivation<TK, FV> hypA, Derivation<TK, FV> hypB) {
    if (hypA.featurizable == null && hypB.featurizable == null)
      return true;
    if (hypA.featurizable == null || hypB.featurizable == null)
      return false;

    /*
     * RawSequence<TK> transA = hypA.featurizable.partialTranslationRaw;
     * RawSequence<TK> transB = hypB.featurizable.partialTranslationRaw; return
     * compareNgrams(transA, transB);
     */

    Sequence<TK> ngramA = getNgram(hypA);
    Sequence<TK> ngramB = getNgram(hypB);
    if (DETAILED_DEBUG) {
      if (ngramA.equals(ngramB)) {
        System.err.printf("hypA: %s\n", hypA.featurizable.targetPrefix);
        System.err.printf("\tn-gram: %s\n", ngramA);
        System.err.printf("hypB: %s\n", hypB.featurizable.targetPrefix);
        System.err.printf("\tn-gram: %s\n", ngramB);
      }
    }
    return (ngramA != null && ngramA.equals(ngramB));
  }

  @Override
  public long recombinationHashCode(Derivation<TK, FV> hyp) {
    Sequence<TK> ngram = getNgram(hyp);
    if (ngram == null)
      return 0;
    return ngram.longHashCode();
  }
}
