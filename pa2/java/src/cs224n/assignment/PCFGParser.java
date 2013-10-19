package cs224n.assignment;

import cs224n.ling.Tree;
import cs224n.assignment.Grammar.*;
import java.util.*;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;

    public void train(List<Tree<String>> trainTrees) {
        List<Tree<String>> annotatedTrainTrees = new ArrayList<Tree<String>>();
        int i = 0;
        for (Tree<String> tree : trainTrees) {
            annotatedTrainTrees.add(TreeAnnotations.annotateTree(tree));
        }
        lexicon = new Lexicon(annotatedTrainTrees);
        grammar = new Grammar(annotatedTrainTrees);
    }

    @Override
    public Tree<String> getBestParse(List<String> sentence) {
        return TreeAnnotations.unAnnotateTree(CKYBackTrack(CKYParse(sentence)));
    }

    private class CKYNode {

        private abstract class BackPointer {
            public abstract String getParentTerminal();
        }

        public class BinaryBackPointer extends  BackPointer {
            private CKYNode left;
            private CKYNode right;
            private BinaryRule rule;

            public BinaryBackPointer() {}

            public String getParentTerminal() {
                return rule.getParent();
            }

            public BinaryBackPointer(CKYNode left, CKYNode right, BinaryRule rule) {
                update(left, right, rule);
            }

            public CKYNode getLeftNode() {
                return left;
            }

            public CKYNode getRightNode() {
                return right;
            }

            public String getLeftTerminal() {
                return rule.getLeftChild();
            }

            public String getRightTerminal() {
                return rule.getRightChild();
            }


            public void update(CKYNode newLeft, CKYNode newRight, BinaryRule newRule) {
                left = newLeft;
                right = newRight;
                rule = newRule;
            }

            public String toString() {
                return rule.toString() + ";" + left.hashCode() + ", " + right.hashCode();
            }

        }

        public class UnaryBackPointer extends BackPointer {
            private UnaryRule rule;

            public UnaryBackPointer() {}

            public String getParentTerminal() {
                return rule.getParent();
            }

            public String getChildTerminal() {
                return rule.getChild();
            }

            public UnaryBackPointer(UnaryRule rule) {
                update(rule);
            }

            public void update(UnaryRule newRule) {
                rule = newRule;
            }

            public String toString() {
                return rule.toString() + ";";
            }
        }

        public class WordBackPointer extends BackPointer {

            private String word;

            public String getParentTerminal() {
                return word;
            }

            public WordBackPointer(String newWord) {
                word = newWord;
            }
        }

        Map<String, Double> terminalScores;
        Map<String, BackPointer> backPointers;

        public CKYNode() {
            terminalScores = new HashMap<String, Double>();
            backPointers = new HashMap<String, BackPointer>();
        }

        public BackPointer getTerminalBackPointer(String terminal) {
            return backPointers.get(terminal);
        }

        public Map<String, Double> getTerminalScores() {
            return terminalScores;
        }

        public String toString() {
            String s = "Terminals\n";
            s = s +    "---------\n";
            for (String terminal : terminalScores.keySet()) {
                s = s + terminal + "\t:\t" + terminalScores.get(terminal) + "\n";
            }

            s = s + "\n";
            s = s + "Backpointers\n";
            s = s + "------------\n";
            for (String terminal : backPointers.keySet()) {
                if (backPointers.get(terminal) != null)
                s = s + terminal + "\t:\t" + backPointers.get(terminal).toString() + "\n";
            }
            s = s + "\n";
            return s;

        }

        public void mergeTag(String word) {
            for (String tag : lexicon.getAllTags()) {
                double score = lexicon.scoreTagging(word, tag);
                if (score > 0) {
                    terminalScores.put(tag, score);
                    backPointers.put(tag,new CKYNode.WordBackPointer(word));
                }
            }
        }

        public void merge(CKYNode leftNode, CKYNode rightNode) {
            Set<BinaryRule> leftRules = new HashSet<BinaryRule>();
            Set<BinaryRule> rightRules = new HashSet<BinaryRule>();
            for (String leftTerminal : leftNode.getTerminalScores().keySet()){
                for (BinaryRule rule : grammar.getBinaryRulesByLeftChild(leftTerminal)){
                    leftRules.add(rule);
                }
            }
            for (String rightTerminal : rightNode.getTerminalScores().keySet()){
                for (BinaryRule rule : grammar.getBinaryRulesByRightChild(rightTerminal)){
                    rightRules.add(rule);
                }
            }

            leftRules.retainAll(rightRules);

            for(BinaryRule rule : leftRules){
                String leftTerminal = rule.getLeftChild();
                String rightTerminal = rule.getRightChild();
                String parentTerminal = rule.getParent();
                double currentScore = -1;
                if (terminalScores.containsKey(parentTerminal)) {
                    currentScore = terminalScores.get(parentTerminal);
                } else {
                    // Optimization. place the backpointer object once and update it in place
                    backPointers.put(parentTerminal, new BinaryBackPointer());
                }

                double ruleScore = rule.getScore() * leftNode.getTerminalScores().get(leftTerminal)
                        * rightNode.getTerminalScores().get(rightTerminal);

                if (ruleScore > currentScore) {
                    terminalScores.put(parentTerminal, ruleScore);
                    BinaryBackPointer bp = (BinaryBackPointer) backPointers.get(parentTerminal);
                    bp.update(leftNode, rightNode, rule);
                }
            }
        }


        private List<String> copySet(Set<String> str) {
            ArrayList<String> list = new ArrayList<String>();
            for (String s : str) {
                list.add(s);
            }
            return list;
        }

        public void handleUnary() {
            boolean added = true;
            while (added) {
                added = false;
                List<String> nodeTerminals = copySet(terminalScores.keySet());
                for (String term : nodeTerminals) {
                    for (UnaryRule rule : grammar.getUnaryRulesByChild(term)) {
                        String parent = rule.getParent();
                        double unaryScore = rule.getScore() * terminalScores.get(term);
                        if (!terminalScores.containsKey(parent) ||
                                unaryScore > terminalScores.get(parent)) {
                            terminalScores.put(parent, unaryScore);
                            /* optimization for memory */
                            BackPointer bp = backPointers.get(parent);
                            if (bp == null || bp instanceof BinaryBackPointer) {
                                backPointers.put(parent, new UnaryBackPointer(rule));
                            } else {
                                UnaryBackPointer ubp = (UnaryBackPointer) bp;
                                ubp.update(rule);
                            }
                            /* end */
                            added = true;
                        }
                    }
                }
            }
        }

    }

    private CKYNode[][] CKYParse(List<String> sentence) {
        int numWords = sentence.size();
        CKYNode ckyTriangle[][] = new CKYNode[numWords][numWords];

        for (int i=0; i<numWords; i++) {
            CKYNode leafNode = new CKYNode();
            leafNode.mergeTag(sentence.get(i));
            leafNode.handleUnary();
            ckyTriangle[i][i] = leafNode;
        }

        for (int diag=numWords-1; diag>0; diag--) {
            for (int i=0; i<diag; i++) {
                int j = i + (numWords-diag);
                CKYNode parentNode = new CKYNode();
                for (int split=i; split<j; split++) {
                    CKYNode leftNode = ckyTriangle[i][split];
                    CKYNode rightNode = ckyTriangle[split+1][j];
                    parentNode.merge(leftNode, rightNode);
                }
                parentNode.handleUnary();
                ckyTriangle[i][j] = parentNode;
            }
        }
        return ckyTriangle;
    }

    private Tree<String> CKYBackTrack(CKYNode[][] ckyTriangle) {

        CKYNode node = ckyTriangle[0][ckyTriangle.length-1];
        return CKYBackTrackHelper(node, "ROOT");
    }

    private Tree<String> CKYBackTrackHelper(CKYNode node, String label) {

        if (node == null) {
            return null;
        }

        Tree<String> tree = new Tree<String>(label);

        CKYNode.BackPointer bp = node.getTerminalBackPointer(label);
        if (bp == null) {
            return tree;
        }

        List<Tree<String>> children = new ArrayList<Tree<String>>();
        if (bp instanceof CKYNode.WordBackPointer) {
            Tree<String> leaf = new Tree<String>(bp.getParentTerminal());
            children.add(leaf);
        }
        else if (bp instanceof CKYNode.BinaryBackPointer) {
            CKYNode.BinaryBackPointer bbp = (CKYNode.BinaryBackPointer) bp;
            Tree<String> left = CKYBackTrackHelper(bbp.getLeftNode(), bbp.getLeftTerminal());
            Tree<String> right = CKYBackTrackHelper(bbp.getRightNode(), bbp.getRightTerminal());
            if (left != null)
                children.add(left);
            if (right != null)
                children.add(right);
        } else {
            CKYNode.UnaryBackPointer ubp = (CKYNode.UnaryBackPointer) bp;
            Tree<String> child = CKYBackTrackHelper(node, ubp.getChildTerminal());
            if (child != null)
                children.add(child);
        }
        if (!children.isEmpty())
            tree.setChildren(children);
        return tree;
    }

    void printTriangle(CKYNode[][] triangle) {
        int n = triangle.length;
        for (int i=0; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (triangle[i][j] != null) {
                    System.out.println("Node " + i + ", " + j +":");
                    System.out.println(triangle[i][j].toString());
                }
            }
        }
    }
}
