package Model;

public class Term {

    private String word;
    private int tf; // the amount of times a word appeared in a document
    private String docOfAppearance; // the name of the doc where the term was observed at
    private boolean bigLetterStarter;
    private boolean isHeader; //checks if the term is part of the document title
    private boolean isEntity;
    private int relativeLocation;

    /**
     * Term's constructor
     *
     * @param word
     */
    public Term(String word, String doc) {
        this.word = word;
        docOfAppearance = doc;
        tf = 1;
        bigLetterStarter=setBigLetter(word);
        this.isHeader=false;
        this.isEntity=false;
        relativeLocation=0;
    }

    /**
     * consrutctor
     * @param word
     * @param doc
     * @param df
     * @param tf
     */
    public Term(String word, String doc, int df, int tf){
        this.word = word;
        docOfAppearance = doc;
        this.tf = tf;
        bigLetterStarter=setBigLetter(word);
    }

    /**
     * sets the value of the boolean field isEntity
     * @param isEntity
     */
    public void setEntity(boolean isEntity){
        this.isEntity=isEntity;
    }

    /**
     * checks if the term is an entity
     * @return
     */
    public int getEntity() {
        return isEntity ? 1 : 0;
    }

    /**
     *sets the header
     * @param isHeader
     */
    public void setHeader (boolean isHeader){
        this.isHeader=isHeader;
    }

    /**
     * checks if the term is in the header
     * @return
     */
    public int getHeader() {
        return isHeader ? 1 : 0;
    }

    /**
     * getter of relative location
     * @return relative location
     */
    public int getRelativeLocation (){
        return relativeLocation;
    }

    /**
     * setter of relative location
     * @param relativeLocation the wanted value
     */
    public void setRelativeLocation(int relativeLocation){
        this.relativeLocation=relativeLocation;
    }


    /**
     * this function sets the value of the boolean field in the term
     * @param word the string of the term
     * @return true is the word starts with big letter
     */

    private boolean setBigLetter (String word){
        if(Character.isUpperCase(word.charAt(0))){
            bigLetterStarter=true;
        }
        return false;
    }


    /**
     * Getter function, returns the word within the Term
     *
     * @return word
     */
    public String getTermWord() {
        return word;
    }

    /**
     * getter function, returns if the term starts with a big letter
     * @return
     */
    public boolean getTermStarter(){
        return bigLetterStarter;
    }


    /**
     * Getter function, returns the tf field within the term.
     *
     * @return
     */
    public int getTf() {
        return tf;
    }

    /**
     * Getter function, returns the doc field within the term.
     * @return
     */
    public String getDoc() {
        return docOfAppearance;
    }

    /**
     * The function returns true if both terms came from the same document and false otherwise
     * @param other
     * @return
     */
    public boolean areTermsFromSameDoc(Term other) {
        return (this.docOfAppearance.equals(other.getDoc()));
    }

    /**
     * Setter function, updates 'tf' field within the Term
     * @param num
     */
    public void setTf(int num){
        tf = num;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Term) {
            Term termObj = (Term) obj;
            return word.equals(termObj.getTermWord());
        }
        return false;
    }

    /**
     * setter of the word field
     * @param word
     */
    public void setWord(String word){
        this.word=word;
    }


    @Override
    public String toString() {
        return "The word: " + word + " appears " + tf + " times in the current document.";
    }
}

