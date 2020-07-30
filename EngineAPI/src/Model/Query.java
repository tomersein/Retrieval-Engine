package Model;

public class Query {

    private String id;
    private String queryData;
    private String[] queryDataSplitted;


    /**
     * Constructor
     */
    public Query(String id, String queryData){
        this.id = id;
        this.queryData = queryData;
        queryDataSplitted = queryData.split(" ");
    }

    /**
     * Getter for query's ID
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Getter for query's words (data)
     * @return
     */
    public String getQueryData() {
        return queryData;
    }

    /**
     * Getter for query's words split by space
     * @return
     */
    public String[] getQueryDataSplitted() {
        return queryDataSplitted;
    }
}
