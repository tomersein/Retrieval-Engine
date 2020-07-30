package View;

import Model.Handler;
import Model.Indexer;
import Model.PostingFilesMerger;
import Model.QueryData;
import com.medallia.word2vec.Searcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;

import javafx.scene.Scene;
import javafx.scene.control.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class Controller {

    @FXML
    public TextField toTakeFrom; // corpus directory
    @FXML
    private CheckBox stemmingCheck;
    @FXML
    private TextField toCreateTo; //posting files directory
    @FXML
    private Button takingBrowser;
    @FXML
    private Button createBrowser;
    @FXML
    private Button loadDictionary;
    @FXML
    private Button reset;
    @FXML
    private Button displayDictionary;
    @FXML
    private Button startRunning;
    @FXML
    private Label status;
    @FXML
    public ListView<String> viewLinkedList;
    @FXML
    public ListView<String> viewEntitiesList;
    @FXML
    public ListView<String> viewRelevantDocForQuery;
    @FXML
    public ListView<String> chooseBoxList;

    //-----------PART B GUI---------
    @FXML
    private Button runSingleQuery;
    @FXML
    private Button browseQueries;
    @FXML
    private Button browseQueryFiles;
    @FXML
    private TextField queryInsert;
    @FXML
    private TextField takingQueryFilesFrom;
    @FXML
    private ComboBox boxOfEntities;
    @FXML
    private Button findEntities;
    @FXML
    private CheckBox semanticCheck;
    @FXML
    private TextField saveResultsTo;
    @FXML
    private CheckBox toSaveResultsOrNot;
    @FXML
    private Button whereToSaveResults;
    @FXML
    public TableView table = new TableView();
    @FXML
    public ListView <String> multipleResultsQuery;


    private PostingFilesMerger postingFilesMerger;
    private Handler handler;
    private String pathOfCorpus;
    private String pathOfPosting;
    private LinkedList<String> listViewLinkedString;
    private String pathOfQuery;
    private String pathOfQueryResults;


    @FXML
    private void chooseCorpusDir(ActionEvent event) throws IOException {
        Stage stage = new Stage();
        DirectoryChooser chooseDir = new DirectoryChooser();
        File chosenDir = chooseDir.showDialog(stage);
        if (chosenDir != null) {
            toTakeFrom.setText(chosenDir.getAbsolutePath());
            this.pathOfCorpus = chosenDir.getAbsolutePath();
        } else {
            toTakeFrom.setText("No directory was chosen");
        }
    }

    @FXML
    private void choosePostingDir(ActionEvent event) throws IOException {
        Stage stage = new Stage();
        DirectoryChooser chooseDir = new DirectoryChooser();
        File chosenDir = chooseDir.showDialog(stage);
        if (chosenDir == null) {
            toCreateTo.setText("No directory was chosen");
        } else {
            toCreateTo.setText(chosenDir.getAbsolutePath());
            this.pathOfPosting = chosenDir.getAbsolutePath();
        }
    }


    @FXML
    private void resetProgram(ActionEvent event) {
        postingFilesMerger = new PostingFilesMerger(pathOfPosting, pathOfCorpus);
        postingFilesMerger.resetAllPostingFiles();
        status.setText("Program is cleared");
    }

    @FXML
    private void Generate() throws FileNotFoundException, UnsupportedEncodingException {
        this.handler = new Handler(pathOfCorpus, pathOfPosting, stemmingCheck.isSelected());
        this.postingFilesMerger = new PostingFilesMerger(pathOfPosting, pathOfCorpus);
        handler.start();
        status.setText("Engine was generated");
    }


    @FXML
    public void showDictionary() throws IOException {

        Indexer tempIndexer = handler.getIndexer();
        listViewLinkedString = new LinkedList<>();
        listViewLinkedString = tempIndexer.showDictionary();
        showViewList(listViewLinkedString, viewLinkedList);
        status.setText("Dictionary is being displayed");
    }


    @FXML
    public void runSingleQuery(ActionEvent event) throws IOException, Searcher.UnknownWordException {
        String query = queryInsert.getText();
        if (query.equals("") || query.equals("Please write a valid query.")) {
            status.setText("Please write a valid query.");
        } else {
            handler = new Handler(stemmingCheck.isSelected(), pathOfPosting, semanticCheck.isSelected(), toSaveResultsOrNot.isSelected(), pathOfQueryResults);
            LinkedList<String> sortedDocs = handler.findRelevantDocumentsForSingleQuery(query);
            viewRelevantDocForQuery = new ListView<>();
            setChooseBox(sortedDocs);
            showViewList(sortedDocs, viewRelevantDocForQuery);
        }
    }


    @FXML
    private void loadDictionary() throws IOException {
        Indexer tempIndexer = handler.getIndexer();
        tempIndexer.writeDictionaryToDisk();
        status.setText("Dictionary is in the disk");
    }

    @FXML
    public void setChooseBox(LinkedList<String> docs) {
        if (docs != null) {
            LinkedList<String> docsToShow = new LinkedList<>();
            for (String doc : docs) {
                String[] temp = doc.split(" :");
                docsToShow.add(temp[0]);
            }
            chooseBoxList = new ListView<>();
            ObservableList<String> list = FXCollections.observableList(docsToShow);
            boxOfEntities.setItems(list);
        }
    }

    @FXML
//todo NEW MERGED
    public void showMostRankedEntities() throws IOException {
//need to get the string from the choose box
        handler = new Handler(stemmingCheck.isSelected(), pathOfPosting, semanticCheck.isSelected(), toSaveResultsOrNot.isSelected(), pathOfQueryResults);
        String filename = boxOfEntities.getValue().toString();
        LinkedList listOfEntities = handler.findMostRankedEntities(filename);
        viewEntitiesList = new ListView<>();
        showViewList(listOfEntities, viewEntitiesList);
        status.setText("The entities are displayed");
    }

    private void showViewList(LinkedList<String> listToDisplay, ListView<String> viewList) {
        ObservableList<String> list = FXCollections.observableList(listToDisplay);
//viewList = new ListView<>();
        viewList.setItems(list);
        Stage stage = new Stage();
        Scene scene = new Scene(viewList);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void setPathToQuery(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load");
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            this.pathOfQuery = file.getAbsolutePath();
            takingQueryFilesFrom.setText(file.getAbsolutePath());
        } else {
            takingQueryFilesFrom.setText("No file was chosen");
        }
    }

    @FXML
    public void searchMultipleQueries() throws IOException, Searcher.UnknownWordException {
        handler = new Handler(stemmingCheck.isSelected(), pathOfPosting, semanticCheck.isSelected(), toSaveResultsOrNot.isSelected(), pathOfQueryResults);
        if (!takingQueryFilesFrom.getText().isEmpty()) {
            LinkedList<QueryData> querieisResults = handler.findRelevantDocumentsForManyQueries(pathOfQuery);
            multipleResultsQuery = new ListView<>();
            showMultipleQueriesResults(querieisResults);
            status.setText("Uploading the results...");
            HashSet<String> doccToChooseBox = new HashSet<>();
            for(QueryData queryData : querieisResults){
                doccToChooseBox.add(queryData.getDocID());
            }
            LinkedList<String> chooseBoxDocs = new LinkedList<>();
            for(String docName : doccToChooseBox){
                chooseBoxDocs.add(docName);
            }
            setChooseBox(chooseBoxDocs);
        } else {
            status.setText("No file was chosen");
        }
    }


    @FXML
    public void setPathToSaveQueryResults(ActionEvent event) {
        Stage stage = new Stage();
        DirectoryChooser chooseDir = new DirectoryChooser();
        File chosenDir = chooseDir.showDialog(stage);
        if (chosenDir != null) {
            saveResultsTo.setText(chosenDir.getAbsolutePath());
            pathOfQueryResults = chosenDir.getAbsolutePath();
        } else {
            saveResultsTo.setText("No directory was chosen");
        }
    }


    @FXML
    public void showMultipleQueriesResults(LinkedList<QueryData> results){
        table = new TableView();
        TableColumn QueryID = new TableColumn("Query ID");
        TableColumn DocID = new TableColumn("Doc ID");
        table.getColumns().addAll(QueryID , DocID);
        QueryID.setCellValueFactory(
                new PropertyValueFactory<QueryData,String>("queryID")
        );
        DocID.setCellValueFactory(
                new PropertyValueFactory<QueryData,String>("docID")
        );
        ObservableList<QueryData> data = FXCollections.observableList(results);
        table.setItems(data);
        Stage stage = new Stage();
        Scene scene = new Scene(table);
        stage.setScene(scene);
        stage.show();

    }




}