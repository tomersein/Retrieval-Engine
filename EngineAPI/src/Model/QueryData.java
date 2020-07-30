package Model;

public class QueryData {
    private String queryID;
    private String docID;


    public QueryData(String queryID,String docID){
        this.queryID=queryID;
        this.docID=docID;
    }

    public String getQueryID() {
        return queryID;
    }

    public String getDocID() {
        return docID;
    }

    public void setQueryID(String queryID) {
        this.queryID = queryID;
    }

    public void setDocID(String docID) {
        this.docID = docID;
    }
}


