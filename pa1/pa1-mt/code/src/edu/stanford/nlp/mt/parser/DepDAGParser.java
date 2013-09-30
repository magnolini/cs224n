package edu.stanford.nlp.mt.parser;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.parser.Actions.Action;
import edu.stanford.nlp.mt.parser.Actions.ActionType;
import edu.stanford.nlp.mt.parser.DAGFeatureExtractor.RightSideFeatures;
import edu.stanford.nlp.parser.Parser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.stats.OpenAddressCounter;
import edu.stanford.nlp.tagger.common.TaggerConstants;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.DependencyScoring;
import edu.stanford.nlp.trees.DependencyScoring.Score;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

/**
 * Linear time Shift-Reduce dependency parser. Incrementally parses a sentence
 * 
 * @author heeyoung
 *
 */
public class DepDAGParser implements Parser, Serializable {
  public static final boolean DEBUG = false;

  private static final long serialVersionUID = -5534972476741917367L;

  // separate classifier for action and label for speed
  private LinearClassifier<ActionType, ObjectTuple<String>> actClassifier;
  private LinearClassifier<GrammaticalRelation, ObjectTuple<String>> labelClassifier;
  private static final boolean VERBOSE = false;

  //to reduce the total number of features for training, remove features appear less than 3 times
  private static final boolean REDUCE_FEATURES = true;

  public boolean labelRelation = true;

  public boolean extractTree = true;  // ensure parsed dependencies is tree (not DAG)

  // use the default setting
  private static RightSideFeatures rightFeatures = new RightSideFeatures(new Properties());

  public DepDAGParser() {
    this.labelRelation = true;
    this.extractTree = true;
  }
  public DepDAGParser(boolean labelRelation, boolean extractTree) {
    this.labelRelation = labelRelation;
    this.extractTree = extractTree;
  }

  @Override
  public boolean parse(List<? extends HasWord> sentence) {
    return true;  // accept everything for now.
  }

  public static DepDAGParser trainModel(
      List<Structure> rawTrainData, boolean labelRelation, boolean extractTree) {
    DepDAGParser parser = new DepDAGParser(labelRelation, extractTree);

    // to reduce the total number of features for training, remove features appear less than 3 times
    Counter<ObjectTuple<String>> featureCounter = null;
    if(REDUCE_FEATURES) featureCounter = countFeatures(rawTrainData, labelRelation);

    GeneralDataset<ActionType, ObjectTuple<String>> actTrainData = new Dataset<ActionType, ObjectTuple<String>>();
    GeneralDataset<GrammaticalRelation, ObjectTuple<String>> labelTrainData = new Dataset<GrammaticalRelation, ObjectTuple<String>>();
    extractTrainingData(rawTrainData, actTrainData, labelTrainData, featureCounter, labelRelation);

    LinearClassifierFactory<ActionType, ObjectTuple<String>> actFactory = new LinearClassifierFactory<ActionType, ObjectTuple<String>>();
    LinearClassifierFactory<GrammaticalRelation, ObjectTuple<String>> labelFactory = new LinearClassifierFactory<GrammaticalRelation, ObjectTuple<String>>();

    featureCounter = null;

    // Build a classifier
    if(labelRelation) parser.labelClassifier = labelFactory.trainClassifier(labelTrainData);
    parser.actClassifier = actFactory.trainClassifier(actTrainData);
    if(VERBOSE) {
      parser.actClassifier.dump();
      parser.labelClassifier.dump();
    }

    return parser;
  }

  private static Counter<ObjectTuple<String>> countFeatures(List<Structure> rawTrainData, boolean labelRelation) {
    Counter<ObjectTuple<String>> counter = new OpenAddressCounter<ObjectTuple<String>>();

    for(Structure struc : rawTrainData) {
      LinkedStack<Action> actions = struc.getActionTrace();
      struc.actionTrace = new LinkedStack<Action>();

      int offset = struc.input.size();
      Object[] acts = actions.peekN(actions.size());
      for(int i = acts.length-1 ; i >= 0 ; i--){
        Action act = (Action)acts[i];
        Datum<ActionType, ObjectTuple<String>> actDatum = extractActFeature(act.action, struc, null, offset, labelRelation);
        Datum<GrammaticalRelation, ObjectTuple<String>> labelDatum = extractLabelFeature(act.relation, act.action, actDatum, struc, null, offset);

        for(ObjectTuple<String> feature : labelDatum.asFeatures()) {
          counter.incrementCount(feature);
        }
        try {
          Actions.doAction(act, struc, offset);
          if(act.action==ActionType.SHIFT) offset--;
        } catch (RuntimeException e) {
          throw e;
        }
      }
      struc.dependencies = new LinkedStack<TypedDependency>();
      struc.stack = new LinkedStack<CoreLabel>();
    }
    return counter;
  }

  private static void extractTrainingData(
      List<Structure> rawTrainData,
      GeneralDataset<ActionType, ObjectTuple<String>> actTrainData,
      GeneralDataset<GrammaticalRelation, ObjectTuple<String>> labelTrainData,
      Counter<ObjectTuple<String>> featureCounter, boolean labelRelation) {

    for(Structure struc : rawTrainData) {
      LinkedStack<Action> actions = struc.getActionTrace();
      int offset = struc.input.size();
      int successfulActionCnt = 0;
      if (DEBUG) {
        System.err.printf("Input: %s\n", struc.input);
      }
      Object[] acts = actions.peekN(actions.size());
      for(int i = acts.length-1 ; i >= 0 ; i--){
        Action act = (Action)acts[i];
        Datum<ActionType, ObjectTuple<String>> actDatum = extractActFeature(act.action, struc, featureCounter, offset, labelRelation);
        Datum<GrammaticalRelation, ObjectTuple<String>> labelDatum = extractLabelFeature(act.relation, act.action, actDatum, struc, featureCounter, offset);
        if(actDatum.asFeatures().size() > 0) actTrainData.add(actDatum);
        if((act.action==ActionType.LEFT_ARC || act.action==ActionType.RIGHT_ARC)
            && labelDatum.asFeatures().size() > 0) {
          labelTrainData.add(labelDatum);
        }

        if (DEBUG) {
          System.err.printf("State:\n\tAction: %s\n", act);
          System.err.printf("\tPre-stack: %s\n", struc.stack);
        }
        if(offset < 1) throw new RuntimeException("input offset is smaller than 1!!");
        try {
          Actions.doAction(act, struc, offset);
          if(act.action==ActionType.SHIFT) offset--;
        } catch (RuntimeException e) {
          System.err.printf("Runtime exception: %s\n", e);
          System.err.printf("Actions: %s\n", actions);
          System.err.printf("Bad action: %s\n", act);
          System.err.printf("Stack state: %s\n", struc.stack);
          System.err.printf("Successful action cnt: %d\n", successfulActionCnt);
          throw e;
        }
        if (DEBUG) {
          System.err.printf("\tPost-stack: %s\n", struc.stack);
        }
        successfulActionCnt++;
      }
    }
  }

  private static Datum<ActionType, ObjectTuple<String>> extractActFeature(
      ActionType act, Structure s, Counter<ObjectTuple<String>> featureCounter, int offset, boolean labelRelation){
    // if act == null, test data
    if(offset < 2) return null;
    List<ObjectTuple<String>> features = DAGFeatureExtractor.extractActFeatures(s, offset, rightFeatures, labelRelation);
    if(featureCounter!=null) {
      Set<ObjectTuple<String>> rareFeatures = new HashSet<ObjectTuple<String>>();
      for(ObjectTuple<String> feature : features) {
        if(featureCounter.getCount(feature) < 3) rareFeatures.add(feature);
      }
      features.removeAll(rareFeatures);
    }
    return new BasicDatum<ActionType, ObjectTuple<String>>(features, act);
  }
  private static Datum<GrammaticalRelation, ObjectTuple<String>> extractLabelFeature(
      GrammaticalRelation rel, ActionType action,
      Datum<ActionType, ObjectTuple<String>> actDatum, Structure s,
      Counter<ObjectTuple<String>> featureCounter, int offset){
    // if act == null, test data
    List<ObjectTuple<String>> features = DAGFeatureExtractor.extractLabelFeatures(action, actDatum, s, offset);
    if(featureCounter!=null) {
      Set<ObjectTuple<String>> rareFeatures = new HashSet<ObjectTuple<String>>();
      for(ObjectTuple<String> feature : features) {
        if(featureCounter.getCount(feature) < 3) rareFeatures.add(feature);
      }
      features.removeAll(rareFeatures);
    }
    return new BasicDatum<GrammaticalRelation, ObjectTuple<String>>(features, rel);
  }

  // for extracting features from test data (no gold Action given)
  private static Datum<ActionType, ObjectTuple<String>> extractActFeature(Structure s, int offset, boolean labelRelation){
    return extractActFeature(null, s, null, offset, labelRelation);
  }
  private static Datum<GrammaticalRelation, ObjectTuple<String>> extractLabelFeature(
      ActionType action, Structure s, Datum<ActionType, ObjectTuple<String>> actDatum, int offset){
    return extractLabelFeature(null, action, actDatum, s, null, offset);
  }

  public LinkedStack<TypedDependency> getDependencyGraph(Structure s){
    return getDependencyGraph(s, s.input.size());
  }

  private LinkedStack<TypedDependency> getDependencyGraph(Structure s, int offset){
    parsePhrase(s, offset);
    //    s.addRoot();
    return s.dependencies;
  }

  /**
   * Get dependencies when a sentence(List<CoreLabel>) is given.
   */
  public LinkedStack<TypedDependency> getDependencyGraph(List<CoreLabel> sentence){
    Structure s = new Structure();
    for(CoreLabel w : sentence){
      s.input.push(w);
      if(s.input.size() > 1) parsePhrase(s, 2);
    }
    CoreLabel last = sentence.get(sentence.size()-1);
    if(!last.get(TextAnnotation.class).equals(TaggerConstants.EOS_WORD)) {
      CoreLabel cl = new CoreLabel();
      cl.set(TextAnnotation.class, TaggerConstants.EOS_WORD);
      cl.set(PartOfSpeechAnnotation.class, TaggerConstants.EOS_TAG);
      cl.set(IndexAnnotation.class, last.get(IndexAnnotation.class)+1);
      s.input.push(cl);
      if(s.input.size() > 1) parsePhrase(s, 2);
    }
    return s.dependencies;
  }
  /**
   * Parse phrase
   * @param s - previous structure + new input phrase
   * @param offset - the length of new input phrase
   * @param labelRelation - label relation of dependency if true
   */
  public void parsePhrase(Structure s, int offset, boolean labelRelation){
    Datum<ActionType, ObjectTuple<String>> d;
    while((d=extractActFeature(s, offset, labelRelation))!=null){
      Action nextAction;
      if(s.getStack().size()==0) nextAction = new Action(ActionType.SHIFT);
      else {
        Counter<ActionType> actionScores = actClassifier.scoresOf(d);
        nextAction = getNextAction(actionScores, d, s, offset, labelRelation);
      }
      Actions.doAction(nextAction, s, offset);
      if(nextAction.action==ActionType.SHIFT) offset--;
    }
  }
  private Action getNextAction(Counter<ActionType> actionScores, Datum<ActionType, ObjectTuple<String>> d, Structure s, int offset, boolean labelRelation) {
    ActionType bestActionType = Counters.argmax(actionScores);
    Action nextAction;

    switch(bestActionType) {
      case SHIFT:
        return new Action(bestActionType);
      case REDUCE:
        return new Action(bestActionType);
      case LEFT_ARC:
        if(s.actionTrace.size() > 0 &&
            (s.actionTrace.peek().action == ActionType.LEFT_ARC || s.actionTrace.peek().action == ActionType.RIGHT_ARC)
            || (extractTree && s.dependentsIdx.contains(s.stack.peek().get(IndexAnnotation.class)))) {
          actionScores.setCount(ActionType.LEFT_ARC, Double.NEGATIVE_INFINITY);
          return getNextAction(actionScores, d, s, offset, labelRelation);
        }
        nextAction = new Action(ActionType.LEFT_ARC);
        if(labelRelation) nextAction.relation = labelClassifier.classOf(extractLabelFeature(nextAction.action, s, d, offset));
        return nextAction;
      case RIGHT_ARC:
        CoreLabel q;
        if(offset == 1) q = s.input.peek();
        else {
          Object[] o = s.input.peekN(offset);
          q = (CoreLabel) o[offset-1];
        }

        if(s.actionTrace.size() > 0 &&
            (s.actionTrace.peek().action == ActionType.LEFT_ARC || s.actionTrace.peek().action == ActionType.RIGHT_ARC)
            || (extractTree && s.dependentsIdx.contains(q.get(IndexAnnotation.class)))) {
          actionScores.setCount(ActionType.RIGHT_ARC, Double.NEGATIVE_INFINITY);
          return getNextAction(actionScores, d, s, offset, labelRelation);
        }

        nextAction = new Action(ActionType.RIGHT_ARC);
        if(labelRelation) nextAction.relation = labelClassifier.classOf(extractLabelFeature(nextAction.action, s, d, offset));
        return nextAction;
      default:
        throw new RuntimeException("wrong action type: should not occur");
    }
  }
  private void parsePhrase(Structure s, int offset){
    parsePhrase(s, offset, this.labelRelation);
  }
  protected void parsePhrase(Structure s, List<CoreLabel> phrase, boolean labelRelation){
    int fromPreviousPhrase = (s.input.size()==0)? 0 : 1;
    for(CoreLabel w : phrase){
      s.input.push(w);
    }
    parsePhrase(s, phrase.size()+fromPreviousPhrase, labelRelation);
  }
  protected void parseToken(Structure s, CoreLabel lastToken, boolean labelRelation){
    int fromPreviousPhrase = (s.input.size()==0)? 0 : 1;
    s.input.push(lastToken);
    parsePhrase(s, 1+fromPreviousPhrase, labelRelation);
  }
  public void parseToken(Structure s, CoreLabel lastToken) {
    parseToken(s, lastToken, this.labelRelation);
  }

  /**
   * Simple sentence parser for test. Use other methods for better tokenization.
   */
  public Structure parseSentence(String sent, IncrementalTagger tagger) {
    Structure struc = new Structure();
    int seqLen = tagger.ts.leftWindow() + 1;

    int idx = 1;
    String[] toks = sent.split("\\s+");
    for(String tok : toks){
      CoreLabel w = new CoreLabel();
      w.set(TextAnnotation.class, tok);
      w.set(IndexAnnotation.class, idx++);

      int len = Math.min(seqLen, struc.input.size()+1);
      IString[] sequence = new IString[len];
      int i = sequence.length-1;
      sequence[i--] = new IString(tok);
      if(len > 1) {
        for(Object c : struc.input.peekN(len-1)) {
          CoreLabel t = (CoreLabel) c;
          sequence[i--] = new IString(t.get(TextAnnotation.class));
        }
      }
      tagger.tagWord(w, sequence);
      this.parseToken(struc, w, labelRelation);
    }
    CoreLabel w = new CoreLabel();
    w.set(TextAnnotation.class, TaggerConstants.EOS_WORD);
    w.set(PartOfSpeechAnnotation.class, TaggerConstants.EOS_TAG);
    w.set(IndexAnnotation.class, idx++);
    this.parseToken(struc, w, labelRelation);

    return struc;
  }

  /**
   * To train or test parser, use this.
   * options: -train TrainingFile, -test TestFile, -loadModel ModelFile, -storeModel fileToStoreModel, -log LogFile, -labelRelation true/false, -extractTree true/false
   * 
   */
  public static void main(String[] args) throws Exception{
    boolean doTrain = false;
    boolean doTest = true;
    boolean storeTrainedModel = true;

    boolean labelRelation = true;
    boolean extractTree = true;

    Properties props = StringUtils.argsToProperties(args);
    DepDAGParser parser = null;

    //
    // set logger
    //
    String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
    Logger logger = Logger.getLogger(DepDAGParser.class.getName());

    FileHandler fh;
    try {
      String logFileName = props.getProperty("log", "log.txt");
      logFileName.replace(".txt", "_"+ timeStamp+".txt");
      fh = new FileHandler(logFileName, false);
      logger.addHandler(fh);
      logger.setLevel(Level.FINE);
      fh.setFormatter(new SimpleFormatter());
    } catch (SecurityException e) {
      System.err.println("ERROR: cannot initialize logger!");
      throw e;
    } catch (IOException e) {
      System.err.println("ERROR: cannot initialize logger!");
      throw e;
    }

    if(props.containsKey("train")) doTrain = true;
    if(props.containsKey("test")) doTest = true;
    if(props.containsKey("labelRelation")) labelRelation = Boolean.parseBoolean(props.getProperty("labelRelation", "true"));
    if(props.containsKey("extractTree")) extractTree = Boolean.parseBoolean(props.getProperty("extractTree", "true"));

    if(REDUCE_FEATURES) logger.fine("REDUCE_FEATURES on");
    else logger.fine("REDUCE_FEATURES off");
    rightFeatures = new RightSideFeatures(props);

    MaxentTagger posTagger = null;
    try {
      posTagger = new MaxentTagger(MaxentTagger.DEFAULT_NLP_GROUP_MODEL_PATH);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    //
    //  train model
    //
    if(doTrain) {
      String trainingFile = props.getProperty("train", "/scr/heeyoung/corpus/dependencies/Stanford-11Feb2011/tb3-trunk-train-2011-01-13.conll");

      logger.info("read training data from "+trainingFile + " ...");
      List<Structure> trainData = ActionRecoverer.readTrainingData(trainingFile, posTagger);

      logger.info("train model...");
      DAGFeatureExtractor.printFeatureFlags(logger, rightFeatures);
      Date s1 = new Date();
      parser = trainModel(trainData, labelRelation, extractTree);
      logger.info((((new Date()).getTime() - s1.getTime())/ 1000F) + "seconds\n");

      if(storeTrainedModel) {
        String defaultStore = "/scr/heeyoung/mt/mtdata/parser/parserModel.ser";
        if(!props.containsKey("storeModel")) logger.info("no option -storeModel : trained model will be stored at "+defaultStore);
        String trainedModelFile = props.getProperty("storeModel", defaultStore);
        IOUtils.writeObjectToFile(parser, trainedModelFile);
      }
      logger.info("training is done");
    }

    //
    //  test model
    //
    if(doTest) {
      String testFile = props.getProperty("test", "/scr/heeyoung/corpus/dependencies/Stanford-11Feb2011/tb3-trunk-dev-2011-01-13.conll");

      if(parser==null) {
        String defaultLoadModel = "/scr/heeyoung/mt/mtdata/parser/parserModel.ser";

        if(!props.containsKey("loadModel")) logger.info("no option -loadModel : trained model will be loaded from "+defaultLoadModel);
        String trainedModelFile = props.getProperty("loadModel", defaultLoadModel);
        logger.info("load trained model...");

        Date s1 = new Date();
        parser = IOUtils.readObjectFromFile(trainedModelFile);
        logger.info((((new Date()).getTime() - s1.getTime())/ 1000F) + "seconds\n");
      }
      logger.info("read test data from "+testFile + " ...");
      List<Structure> testData = ActionRecoverer.readTrainingData(testFile, null);
      //List<Structure> testData = ActionRecoverer.readTrainingData(testFile, posTagger);

      List<Collection<TypedDependency>> goldDeps = new ArrayList<Collection<TypedDependency>>();
      List<Collection<TypedDependency>> systemDeps = new ArrayList<Collection<TypedDependency>>();

      logger.info("testing...");
      int count = 0;
      long elapsedTime = 0;
      for(Structure s : testData){
        count++;
        goldDeps.add(s.getDependencies().getAll());
        s.dependencies = new LinkedStack<TypedDependency>();
        Date startTime = new Date();
        LinkedStack<TypedDependency> graph = parser.getDependencyGraph(s);
        elapsedTime += (new Date()).getTime() - startTime.getTime();
        systemDeps.add(graph.getAll());
      }
      System.out.println("The number of sentences = "+count);
      System.out.printf("avg time per sentence: %.3f seconds\n", (elapsedTime / (count*1000F)));
      System.out.printf("Total elapsed time: %.3f seconds\n", (elapsedTime / 1000F));

      logger.info("scoring...");
      DependencyScoring goldScorer = DependencyScoring.newInstanceStringEquality(goldDeps, false);
      Score score = goldScorer.score(DependencyScoring.convertStringEquality(systemDeps));
      logger.info(score.toString());
      logger.info("done");

      //
      // example
      //
      // parse sentence. (List<CoreLabel>)
      String sent = "My dog also likes eating sausage.";
      Properties pp = new Properties();
      pp.put("annotators", "tokenize, ssplit, pos, lemma");
      StanfordCoreNLP pipeline = new StanfordCoreNLP(pp);
      Annotation document = new Annotation(sent);
      pipeline.annotate(document);
      List<CoreMap> sentences = document.get(SentencesAnnotation.class);

      List<CoreLabel> l = sentences.get(0).get(TokensAnnotation.class);
      int index = 1;
      for(CoreLabel t : l){
        t.set(IndexAnnotation.class, index++);
      }
      LinkedStack<TypedDependency> g = parser.getDependencyGraph(l);

      System.err.println(g);
    }
  }
}
