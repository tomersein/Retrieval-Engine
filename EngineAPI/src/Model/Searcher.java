package Model;

import javafx.util.Pair;

import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class Searcher {

    private Parse queryParser;
    private boolean isStemming;
    private String path;
    private Ranker ranker;
    private HashMap<String, Double> relevantDocs;
    private DecimalFormat decimalFormat;
    private boolean isSemantic;


    /**
     * Constructor
     * @param parse
     * @param pathToPosting
     * @param isStemming
     */
    public Searcher(Parse parse, String pathToPosting, boolean isStemming, boolean isSemantic) {

        queryParser = parse;
        this.isStemming = isStemming;
        this.isSemantic = isSemantic;
        this.path = pathToPosting;
        relevantDocs = new HashMap<>();
        decimalFormat = new DecimalFormat("#.###");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
    }


    /**
     * Constructor
     * @param pathToPosting
     * @param isStemming
     */
    public Searcher(String pathToPosting, boolean isStemming){
        this.isStemming = isStemming;
        this.path = pathToPosting;
    }


    /**
     * The method receives a query, calls a private version of the method and returns a hash-map
     * where the key-set contains the relevant document's Id's and the value-set contains their ranking
     * @param query
     * @return hash-map of document's ID and his ranking
     * @throws IOException
     */
    public HashMap<String, Double> searchQuery(Query query) throws IOException, com.medallia.word2vec.Searcher.UnknownWordException {
        return runUserQuery(query);
    }


    /**
     * The method receives a query from its public version and returns the hash-map of the document Id's and the matching rankings
     * @param query
     * @return relevantDocs
     * @throws IOException
     */
    private HashMap<String, Double> runUserQuery(Query query) throws IOException, com.medallia.word2vec.Searcher.UnknownWordException {

        ArrayList<Term> queryParsed = queryParser.parseQuerys(query);
        HashSet<Document> allDocs = getDocumentsFromDisk();
        Indexer indexForRanker = new Indexer(path, isStemming);
        ranker = new Ranker(indexForRanker, path, allDocs, isSemantic);
        if(isStemming){
            for(Term term : queryParsed){
                String word = indexForRanker.stemString(term);
                term.setWord(word);
            }
        }
        relevantDocs = ranker.findRelevantDocuments(queryParsed);
        return relevantDocs;
    }


    /**
     * The function returns a hash-set of all the documents in the corpus
     *
     * @return documentsHash
     * @throws IOException
     */
    public HashSet<Document> getDocumentsFromDisk() throws IOException {
        File file = new File(path + "/documents/documents.txt");
        HashSet<Document> documentsHash = new HashSet<>();
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                String[] docInfo = stringBuilder.toString().split(",");
                int maxTf = Integer.parseInt(docInfo[1]);
                int uniqueTerms = Integer.parseInt(docInfo[2]);
                String [] secSplit = docInfo[3].split("™");
                int docLength = Integer.parseInt(secSplit[0]);
                Document tempDoc = new Document(docInfo[0], maxTf, uniqueTerms, docLength, null);
                documentsHash.add(tempDoc);
                stringBuilder = new StringBuilder();
            }
            reader.close();
        }
        return documentsHash;
    }



    /**
     * the function gets the hash map of the docs and their rank, sorts, and converts them to a linked list of string
     * @param docsBeforeSort the hash map before of the sorting
     * @return the sorted linked list
     */
    public LinkedList<String> getSortedDocsList (HashMap<String,Double> docsBeforeSort){
        LinkedList <String> sortedList = new LinkedList<>();
        if(docsBeforeSort!=null) {
            PriorityQueue<Pair> pQueue = new PriorityQueue<>(new Comparator<Pair>() {
                @Override
                public int compare(Pair o1, Pair o2) {
                    double result = (Double)o1.getValue() - (Double)o2.getValue();
                    if(result < 0){
                        return 1;
                    } else if (result > 0){
                        return -1;
                    } else{
                        return 0;
                    }
                }
            });
            for (HashMap.Entry<String, Double> entry : docsBeforeSort.entrySet()) {
                Pair <String,Double> pair = new Pair <>(entry.getKey(),entry.getValue());
                pQueue.add(pair);
            }
            int size = 50;
            while (size>0 && !pQueue.isEmpty()){
                Pair tempPair = pQueue.poll();
                sortedList.add(tempPair.getKey()+" : "+decimalFormat.format(tempPair.getValue()));
                size--;
            }
        }
        return sortedList;
    }

}