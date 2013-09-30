package edu.stanford.nlp.mt.metrics;

import java.util.List;

import edu.stanford.nlp.mt.base.Sequence;

/**
 * Evaluation metric that returns a sentence-level score.
 * 
 * @author Spence Green
 *
 * @param <TK>
 * @param <FV>
 */
public interface SentenceLevelMetric<TK,FV> {

  /**
   * Return a score for this translation.
   * 
   * @param sourceId
   * @param references
   * @param referenceWeights
   * @param translation
   * @return
   */
  public double score(int sourceId, List<Sequence<TK>> references, double[] referenceWeights, Sequence<TK> translation);
  
  /**
   * Update internal state with statistics from this set of evaluation objects.
   * 
   * @param sourceId
   * @param references
   * @param translation
   */
  public void update(int sourceId, List<Sequence<TK>> references, Sequence<TK> translation);
  
  /**
   * Is this metric threadsafe.
   * 
   * @return
   */
  public boolean isThreadsafe();
}
