package edu.stanford.nlp.mt.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.logging.Logger;

import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LeftChildrenNodeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.mt.parser.Actions.ActionType;
import edu.stanford.nlp.util.Pair;

/**
 * Feature extractor for linear time shift-reduce parser
 * Edit this to add more features.
 * 
 * @author heeyoung
 */
public class DAGFeatureExtractor {

  // Sk : k-th item in the stack from the top (S1, S2, ...)
  // Qk : k-th item from the first element of queue (Q1, Q2, ...) Q1: first item in the queue, Q2: previous word of Q1, Q3: previous word of Q2
  // tk : k-th item in the queue (t1, t2, ...) : q1 == t1 -> this is right side feature. T3 seems not helping
  // flags
  private static final boolean useS1Word = true;
  private static final boolean useS1POS = true;
  private static final boolean useS1WordPOS = true;
  private static final boolean useS1NumChild = true;
  private static final boolean useS1LeftChildPOS = true;
  private static final boolean useS1LeftChildRel = true;
  private static final boolean useS1RightChildPOS = true;
  private static final boolean useS1RightChildRel = true;
  private static final boolean useS1PreviousTokenPOS = true;

  private static final boolean useS2Word = true;
  private static final boolean useS2POS = true;
  private static final boolean useS2WordPOS = true;
  private static final boolean useS2NumChild = true;
  private static final boolean useS2LeftChildPOS = true;
  private static final boolean useS2LeftChildRel = true;
  private static final boolean useS2RightChildPOS = true;
  private static final boolean useS2RightChildRel = true;
  private static final boolean useS2NextTokenPOS = true;

  private static final boolean useS3Word = true;
  private static final boolean useS3POS = true;
  private static final boolean useS3WordPOS = true;

  private static final boolean useQ1Word = true;
  private static final boolean useQ1POS = true;
  private static final boolean useQ1WordPOS = true;

  private static final boolean useQ2Word = true;
  private static final boolean useQ2POS = true;
  private static final boolean useQ2WordPOS = true;

  private static final boolean useQ3Word = true;
  private static final boolean useQ3POS = true;
  private static final boolean useQ3WordPOS = true;

  private static final boolean usePreAction = true;
  private static final boolean useActionType = true;  // the current action type (for labeling)

  private static final boolean useS1Q1word = true;
  private static final boolean useS1Q1POS = true;
  private static final boolean useS1Q1WordPOS = true;

  private static final boolean useQ1Q2word = true;
  private static final boolean useQ1Q2POS = true;
  private static final boolean useQ1Q2WordPOS = true;

  private static final boolean useS1S2word = true;
  private static final boolean useS1S2POS = true;
  private static final boolean useS1S2WordPOS = true;

  private static final boolean useRightFeature = true;

  public static class RightSideFeatures {

    // T1 is same as Q1

    private final boolean useT2Word;
    private final boolean useT2POS;
    private final boolean useT2WordPOS;

    private final boolean useT1T2Word;
    private final boolean useT1T2POS;
    private final boolean useT1T2WordPOS;

    public RightSideFeatures(Properties props){
      useT2Word = Boolean.parseBoolean(props.getProperty("useT2Word", "true"));
      useT2POS = Boolean.parseBoolean(props.getProperty("useT2POS", "true"));
      useT2WordPOS = Boolean.parseBoolean(props.getProperty("useT2WordPOS", "true"));

      useT1T2Word = Boolean.parseBoolean(props.getProperty("useT1T2Word", "true"));
      useT1T2POS = Boolean.parseBoolean(props.getProperty("useT1T2POS", "true"));
      useT1T2WordPOS = Boolean.parseBoolean(props.getProperty("useT1T2WordPOS", "true"));
    }
  }

  // TODO : add more flags for new features


  public static List<ObjectTuple<String>> extractActFeatures(Structure struc, int offset, RightSideFeatures rightFeatures, boolean labelRelation) {
    List<ObjectTuple<String>> features = new ArrayList<ObjectTuple<String>>();
    LinkedStack<CoreLabel> stack = struc.getStack();
    LinkedStack<CoreLabel> inputQueue = struc.getInput();
    if(stack.size()==0) return features;   // empty stack: always SHIFT
    int stackSize = stack.size();

    Object[] stackTopN = struc.getStack().peekN(3);
    CoreLabel s1 = (CoreLabel) stackTopN[0];
    CoreLabel s2 = (stackSize > 1)? (CoreLabel) stackTopN[1] : null;
    CoreLabel s3 = (stackSize > 2)? (CoreLabel) stackTopN[2] : null;
    int peekLen = inputQueue.size() - s1.get(IndexAnnotation.class) + 2;
    Object[] queueNWords = struc.getInput().peekN(peekLen);
    CoreLabel q1 = (queueNWords.length > offset - 1 && offset -1 >= 0)? (CoreLabel) queueNWords[offset-1] : null;
    CoreLabel q2 = (queueNWords.length > offset)? (CoreLabel) queueNWords[offset] : null;
    CoreLabel q3 = (queueNWords.length > offset+1)? (CoreLabel) queueNWords[offset+1] : null;

    String s1Word = (s1==null)? null : s1.get(TextAnnotation.class).toLowerCase();
    String s2Word = (s2==null)? null : s2.get(TextAnnotation.class).toLowerCase();
    String s3Word = (s3==null)? null : s3.get(TextAnnotation.class).toLowerCase();
    String q1Word = (q1==null)? null : q1.get(TextAnnotation.class).toLowerCase();
    String q2Word = (q2==null)? null : q2.get(TextAnnotation.class).toLowerCase();
    String q3Word = (q3==null)? null : q3.get(TextAnnotation.class).toLowerCase();

    String s1POS = (s1==null)? null : s1.get(PartOfSpeechAnnotation.class);
    String s2POS = (s2==null)? null : s2.get(PartOfSpeechAnnotation.class);
    String s3POS = (s3==null)? null : s3.get(PartOfSpeechAnnotation.class);
    String q1POS = (q1==null)? null : q1.get(PartOfSpeechAnnotation.class);
    String q2POS = (q2==null)? null : q2.get(PartOfSpeechAnnotation.class);
    String q3POS = (q3==null)? null : q3.get(PartOfSpeechAnnotation.class);

    if(useRightFeature){
      CoreLabel t1 = q1;
      CoreLabel t2 = null;

      if(offset > 1) {
        t2 = (CoreLabel) queueNWords[offset-2];
      }
      String t1Word = q1Word;
      String t2Word = (t2==null)? null : t2.get(TextAnnotation.class).toLowerCase();
      String t1POS = q1POS;
      String t2POS = (t2==null)? null : t2.get(PartOfSpeechAnnotation.class);

      if(rightFeatures.useT2Word && t2!=null) features.add(new ObjectTuple<String>(new String[]{t2Word, "T2Word"}));
      if(rightFeatures.useT2POS && t2!=null) features.add(new ObjectTuple<String>(new String[]{t2POS, "T2POS"}));

      if(rightFeatures.useT1T2Word && t1!=null && t2!=null) features.add(new ObjectTuple<String>(new String[]{t1Word, t2Word, "T1T2Word"}));
      if(rightFeatures.useT1T2POS && t1!=null && t2!=null) features.add(new ObjectTuple<String>(new String[]{t1POS, t2POS, "T1T2POS"}));
      if(rightFeatures.useT1T2WordPOS && t1!=null && t2!=null) features.add(new ObjectTuple<String>(new String[]{t1Word, t1POS, t2Word, t2POS, "T1T2WordPOS"}));
    }

    String preActionStr = (labelRelation)? "##"+struc.getActionTrace().peek().toString() : "##"+struc.getActionTrace().peek().action.toString();

    SortedSet<Pair<CoreLabel, String>> s1Children = null;
    SortedSet<Pair<CoreLabel, String>> s2Children = null;

    if(s1 != null) s1Children = s1.get(LeftChildrenNodeAnnotation.class);
    if(s2 != null) s2Children = s2.get(LeftChildrenNodeAnnotation.class);

    if(usePreAction) features.add(new ObjectTuple<String>(new String[]{preActionStr, "preAct"}));

    if(useS1Word && s1!=null) features.add(new ObjectTuple<String>(new String[]{s1Word, "S1Word"}));
    if(useS2Word && s2!=null) features.add(new ObjectTuple<String>(new String[]{s2Word, "S2Word"}));
    if(useS3Word && s3!=null) features.add(new ObjectTuple<String>(new String[]{s3Word, "S3Word"}));
    if(useQ1Word && q1!=null) features.add(new ObjectTuple<String>(new String[]{q1Word, "Q1Word"}));
    if(useQ2Word && q2!=null) features.add(new ObjectTuple<String>(new String[]{q2Word, "Q2Word"}));
    if(useQ3Word && q3!=null) features.add(new ObjectTuple<String>(new String[]{q3Word, "Q3Word"}));

    if(useS1POS && s1!=null) features.add(new ObjectTuple<String>(new String[]{s1POS, "S1POS"}));
    if(useS2POS && s2!=null) features.add(new ObjectTuple<String>(new String[]{s2POS, "S2POS"}));
    if(useS3POS && s3!=null) features.add(new ObjectTuple<String>(new String[]{s3POS, "S3POS"}));
    if(useQ1POS && q1!=null) features.add(new ObjectTuple<String>(new String[]{q1POS, "Q1POS"}));
    if(useQ2POS && q2!=null) features.add(new ObjectTuple<String>(new String[]{q2POS, "Q2POS"}));
    if(useQ3POS && q3!=null) features.add(new ObjectTuple<String>(new String[]{q3POS, "Q3POS"}));

    if(useS1WordPOS && s1!=null) features.add(new ObjectTuple<String>(new String[]{s1Word, s1POS, "S1WordPOS"}));
    if(useS2WordPOS && s2!=null) features.add(new ObjectTuple<String>(new String[]{s2Word, s2POS, "S2WordPOS"}));
    if(useS3WordPOS && s3!=null) features.add(new ObjectTuple<String>(new String[]{s3Word, s3POS, "S3WordPOS"}));
    if(useQ1WordPOS && q1!=null) features.add(new ObjectTuple<String>(new String[]{q1Word, q1POS, "Q1WordPOS"}));
    if(useQ2WordPOS && q2!=null) features.add(new ObjectTuple<String>(new String[]{q2Word, q2POS, "Q2WordPOS"}));
    if(useQ3WordPOS && q3!=null) features.add(new ObjectTuple<String>(new String[]{q3Word, q3POS, "Q3WordPOS"}));

    if(useS1Q1word && s1!=null && q1!=null) features.add(new ObjectTuple<String>(new String[]{s1Word, q1Word, "S1Q1Word"}));
    if(useQ1Q2word && q1!=null && q2!=null) features.add(new ObjectTuple<String>(new String[]{q1Word, q2Word, "Q1Q2Word"}));
    if(useS1S2word && s1!=null && s2!=null) features.add(new ObjectTuple<String>(new String[]{s1Word, s2Word, "S1S2Word"}));

    if(useS1Q1POS && s1!=null && q1!=null) features.add(new ObjectTuple<String>(new String[]{s1POS, q1POS, "S1Q1POS"}));
    if(useQ1Q2POS && q1!=null && q2!=null) features.add(new ObjectTuple<String>(new String[]{q1POS, q2POS, "Q1Q2POS"}));
    if(useS1S2POS && s1!=null && s2!=null) features.add(new ObjectTuple<String>(new String[]{s1POS, s2POS, "S1S2POS"}));

    if(useS1Q1WordPOS && s1!=null && q1!=null) features.add(new ObjectTuple<String>(new String[]{s1Word, s1POS, q1Word, q1POS, "S1Q1WordPOS"}));
    if(useQ1Q2WordPOS && q1!=null && q2!=null) features.add(new ObjectTuple<String>(new String[]{q1Word, q1POS, q2Word, q2POS, "Q1Q2WordPOS"}));
    if(useS1S2WordPOS && s1!=null && s2!=null) features.add(new ObjectTuple<String>(new String[]{s1Word, s1POS, s2Word, s2POS, "S1S2WordPOS"}));

    if(useS1NumChild && s1Children!=null) {
      String childrenSize = "#"+s1Children.size();
      features.add(new ObjectTuple<String>(new String[]{childrenSize, "S1ChildNum"}));
    }
    if(useS2NumChild && s2Children!=null) {
      String childrenSize = "#"+s2Children.size();
      features.add(new ObjectTuple<String>(new String[]{childrenSize, "S2ChildNum"}));
    }

    if(s1Children!=null) {
      int s1ChildNum = s1Children.size();
      if(useS1LeftChildPOS && s1ChildNum > 0) {
        String leftChildPOS = s1Children.first().first().get(PartOfSpeechAnnotation.class);
        features.add(new ObjectTuple<String>(new String[]{leftChildPOS, "S1LeftChildPOS"}));
      }
      if(useS1LeftChildRel && s1ChildNum > 0) {
        String leftChildRel = s1Children.first().second();
        features.add(new ObjectTuple<String>(new String[]{leftChildRel, "S1LeftChildRel"}));
      }
      if(useS1RightChildPOS && s1ChildNum>1) {
        String rightChildPOS = s1Children.last().first().get(PartOfSpeechAnnotation.class);
        features.add(new ObjectTuple<String>(new String[]{rightChildPOS, "S1RightChildPOS"}));
      }
      if(useS1RightChildRel && s1ChildNum>1) {
        String rightChildRel = s1Children.last().second();
        features.add(new ObjectTuple<String>(new String[]{rightChildRel, "S1RightChildRel"}));
      }
    }
    if(s2Children!=null) {
      int s2ChildNum = s2Children.size();
      if(useS2LeftChildPOS && s2ChildNum > 0) {
        String leftChildPOS = s2Children.first().first().get(PartOfSpeechAnnotation.class);
        features.add(new ObjectTuple<String>(new String[]{leftChildPOS, "S2LeftChildPOS"}));
      }
      if(useS2LeftChildRel && s2ChildNum > 0) {
        String leftChildRel = s2Children.first().second();
        features.add(new ObjectTuple<String>(new String[]{leftChildRel, "S2LeftChildRel"}));
      }
      if(useS2RightChildPOS && s2ChildNum>1) {
        String rightChildPOS = s2Children.last().first().get(PartOfSpeechAnnotation.class);
        features.add(new ObjectTuple<String>(new String[]{rightChildPOS, "S2RightChildPOS"}));
      }
      if(useS2RightChildRel && s2ChildNum>1) {
        String rightChildRel = s2Children.last().second();
        features.add(new ObjectTuple<String>(new String[]{rightChildRel, "S2RightChildRel"}));
      }
    }

    if(useS1PreviousTokenPOS && s1!=null && queueNWords[peekLen-1] != null) {
      CoreLabel sn = (CoreLabel) queueNWords[peekLen-1];
      String preTokenPOS = sn.get(PartOfSpeechAnnotation.class);
      features.add(new ObjectTuple<String>(new String[]{preTokenPOS, "S1PreTokenPOS"}));
    }
    if(useS2NextTokenPOS && s2!=null) {
      // TODO: fix this to make it more efficient
      int nextTokenIdx = s2.get(IndexAnnotation.class) + 1;
      String nextTokenPOS;
      int inputSz = struc.getInput().size();
      if (inputSz-nextTokenIdx < queueNWords.length) {
        nextTokenPOS = ((CoreLabel)queueNWords[inputSz-nextTokenIdx]).get(PartOfSpeechAnnotation.class);
      } else {
        Object[] inputArr = struc.getInput().peekN(inputSz-nextTokenIdx+1);
        nextTokenPOS = ((CoreLabel)inputArr[inputArr.length-1]).get(PartOfSpeechAnnotation.class);
      }
      features.add(new ObjectTuple<String>(new String[]{nextTokenPOS, "S2NextTokenPOS"}));

    }

    // TODO add more features here

    return features;
  }

  /** Extracting features for labelClassifier:
   *    use all features of actClassifier and one additional feature, arcDirection
   *    */
  public static List<ObjectTuple<String>> extractLabelFeatures(
      ActionType action, Datum<ActionType, ObjectTuple<String>> actDatum, Structure s, int offset) {
    List<ObjectTuple<String>> features = new ArrayList<ObjectTuple<String>>();
    features.addAll(actDatum.asFeatures());
    features.add(new ObjectTuple<String>(new String[]{"##"+action.toString(), "actionType"}));

    return features;
  }

  public static void printFeatureFlags(Logger logger, RightSideFeatures rightFeatures) {

    if(Structure.useGoldTag) logger.fine("use gold tags"); else logger.fine("use majority tagger");

    if(useS1Word) logger.fine("useS1Word on"); else logger.fine("useS1Word off");
    if(useS1POS) logger.fine("useS1POS on"); else logger.fine("useS1POS off");
    if(useS1WordPOS) logger.fine("useS1WordPOS on"); else logger.fine("useS1WordPOS off");
    if(useS1NumChild) logger.fine("useS1NumChild on"); else logger.fine("useS1NumChild off");
    if(useS1LeftChildPOS) logger.fine("useS1LeftChildPOS on"); else logger.fine("useS1LeftChildPOS off");
    if(useS1LeftChildRel) logger.fine("useS1LeftChildRel on"); else logger.fine("useS1LeftChildRel off");
    if(useS1RightChildPOS) logger.fine("useS1RightChildPOS on"); else logger.fine("useS1RightChildPOS off");
    if(useS1RightChildRel) logger.fine("useS1RightChildRel on"); else logger.fine("useS1RightChildRel off");
    if(useS1PreviousTokenPOS) logger.fine("useS1PreviousTokenPOS on"); else logger.fine("useS1PreviousTokenPOS off");
    if(useS2Word) logger.fine("useS2Word on"); else logger.fine("useS2Word off");
    if(useS2POS) logger.fine("useS2POS on"); else logger.fine("useS2POS off");
    if(useS2WordPOS) logger.fine("useS2WordPOS on"); else logger.fine("useS2WordPOS off");
    if(useS2NumChild) logger.fine("useS2NumChild on"); else logger.fine("useS2NumChild off");
    if(useS2LeftChildPOS) logger.fine("useS2LeftChildPOS on"); else logger.fine("useS2LeftChildPOS off");
    if(useS2LeftChildRel) logger.fine("useS2LeftChildRel on"); else logger.fine("useS2LeftChildRel off");
    if(useS2RightChildPOS) logger.fine("useS2RightChildPOS on"); else logger.fine("useS2RightChildPOS off");
    if(useS2RightChildRel) logger.fine("useS2RightChildRel on"); else logger.fine("useS2RightChildRel off");
    if(useS2NextTokenPOS) logger.fine("useS2NextTokenPOS on"); else logger.fine("useS2NextTokenPOS off");
    if(useS3Word) logger.fine("useS3Word on"); else logger.fine("useS3Word off");
    if(useS3POS) logger.fine("useS3POS on"); else logger.fine("useS3POS off");
    if(useS3WordPOS) logger.fine("useS3WordPOS on"); else logger.fine("useS3WordPOS off");
    if(useQ1Word) logger.fine("useQ1Word on"); else logger.fine("useQ1Word off");
    if(useQ1POS) logger.fine("useQ1POS on"); else logger.fine("useQ1POS off");
    if(useQ1WordPOS) logger.fine("useQ1WordPOS on"); else logger.fine("useQ1WordPOS off");
    if(useQ2Word) logger.fine("useQ2Word on"); else logger.fine("useQ2Word off");
    if(useQ2POS) logger.fine("useQ2POS on"); else logger.fine("useQ2POS off");
    if(useQ2WordPOS) logger.fine("useQ2WordPOS on"); else logger.fine("useQ2WordPOS off");
    if(useQ3Word) logger.fine("useQ3Word on"); else logger.fine("useQ3Word off");
    if(useQ3POS) logger.fine("useQ3POS on"); else logger.fine("useQ3POS off");
    if(useQ3WordPOS) logger.fine("useQ3WordPOS on"); else logger.fine("useQ3WordPOS off");
    if(usePreAction) logger.fine("usePreAction on"); else logger.fine("usePreAction off");
    if(useActionType) logger.fine("useActionType on"); else logger.fine("useActionType off");
    if(useS1Q1word) logger.fine("useS1Q1word on"); else logger.fine("useS1Q1word off");
    if(useS1Q1POS) logger.fine("useS1Q1POS on"); else logger.fine("useS1Q1POS off");
    if(useS1Q1WordPOS) logger.fine("useS1Q1WordPOS on"); else logger.fine("useS1Q1WordPOS off");
    if(useQ1Q2word) logger.fine("useQ1Q2word on"); else logger.fine("useQ1Q2word off");
    if(useQ1Q2POS) logger.fine("useQ1Q2POS on"); else logger.fine("useQ1Q2POS off");
    if(useQ1Q2WordPOS) logger.fine("useQ1Q2WordPOS on"); else logger.fine("useQ1Q2WordPOS off");
    if(useS1S2word) logger.fine("useS1S2word on"); else logger.fine("useS1S2word off");
    if(useS1S2POS) logger.fine("useS1S2POS on"); else logger.fine("useS1S2POS off");
    if(useS1S2WordPOS) logger.fine("useS1S2WordPOS on"); else logger.fine("useS1S2WordPOS off");

    if(useRightFeature) logger.fine("useRightFeature on"); else logger.fine("useRightFeature off");
    if(rightFeatures.useT2Word) logger.fine("useT2Word on"); else logger.fine("useT2Word off");
    if(rightFeatures.useT2POS) logger.fine("useT2POS on"); else logger.fine("useT2POS off");
    if(rightFeatures.useT2WordPOS) logger.fine("useT2WordPOS on"); else logger.fine("useT2WordPOS off");
    if(rightFeatures.useT1T2Word) logger.fine("useT1T2Word on"); else logger.fine("useT1T2Word off");
    if(rightFeatures.useT1T2POS) logger.fine("useT1T2POS on"); else logger.fine("useT1T2POS off");
    if(rightFeatures.useT1T2WordPOS) logger.fine("useT1T2WordPOS on"); else logger.fine("useT1T2WordPOS off");
  }
}
