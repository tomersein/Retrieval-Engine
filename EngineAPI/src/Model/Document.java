package Model;

import java.util.HashSet;

public class Document {

    private String docName;
    private int maxTf;
    private int uniqueTerms;
    private int docLength;
    private HashSet<Term> potentialEntities;

    /**
     * Constructor
     *
     * @param docName
     * @param maxTf
     * @param uniqueTerms
     */
    public Document(String docName, int maxTf, int uniqueTerms, int docLength, HashSet<Term> potentialEntities) {
        this.docName = docName;
        this.maxTf = maxTf;
        this.uniqueTerms = uniqueTerms;
        this.docLength = docLength;
        this.potentialEntities = potentialEntities;
    }


    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (potentialEntities != null) {
            for (Term term : potentialEntities) {
                stringBuilder.append(term.getTermWord() + "~");
            }
        }
        return docName + "," + maxTf + "," + uniqueTerms + "," + docLength + "™" + stringBuilder.toString();
    }

    public String getDocName() {
        return docName;
    }

    public int getMaxTf() {
        return maxTf;
    }

    public int getUniqueTerms() {
        return uniqueTerms;
    }

    public int getDocLength() {
        return docLength;
    }
}